package org.example.boykisserai.provider.speech.yandex;

import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.config.AppProperties;
import org.example.boykisserai.provider.speech.SpeechState;
import org.example.boykisserai.provider.speech.TextToSpeechService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.tts.provider", havingValue = "yandex")
public class YandexTtsService implements TextToSpeechService {

    private static final int SAMPLE_RATE = 48000;

    private final AppProperties props;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public YandexTtsService(AppProperties props) {
        this.props = props;
    }

    @Override
    public void speak(String text) {
        if (text == null || text.isBlank()) return;

        String cleanText = text.replace("@", "").replaceAll("[^a-zA-Zа-яА-ЯёЁ0-9\\s,.!?-]", "").trim();
        if (cleanText.isEmpty()) return;

        cleanText = cleanText.replaceAll("(?i)\\bвсе\\b", "вс+ё")
                .replaceAll("(?i)\\bвсё\\b", "вс+ё");

        String voice = props.getYandex().getVoice();
        String emotion = props.getYandex().getEmotion();
        double pitchShift = props.getYandex().getPitchShift();

        try {
            SpeechState.isSpeaking = true;
            log.info("[YandexSpeechKit] Озвучка [{} | {} | shift: {}]...", voice, emotion, pitchShift);

            String bodyParams = "text=" + URLEncoder.encode(cleanText, StandardCharsets.UTF_8)
                    + "&lang=ru-RU"
                    + "&voice=" + URLEncoder.encode(voice, StandardCharsets.UTF_8)
                    + "&emotion=" + URLEncoder.encode(emotion, StandardCharsets.UTF_8)
                    + "&speed=" + URLEncoder.encode(props.getYandex().getSpeed(), StandardCharsets.UTF_8)
                    + "&format=lpcm"
                    + "&sampleRateHertz=" + SAMPLE_RATE;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://tts.api.cloud.yandex.net/speech/v1/tts:synthesize"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Api-Key " + props.getYandex().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(bodyParams))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200 && response.body().length > 0) {
                byte[] pcmData = response.body();

                if (Math.abs(pitchShift) > 0.01) {
                    pcmData = applyPitchShiftInMemory(pcmData, pitchShift);
                }

                playPcmDirectly(pcmData);
            } else {
                log.error("[YandexTTS Error 1] Код ошибки: {}", response.statusCode());
            }

        } catch (Exception e) {
            log.error("[YandexTTS Error 2] {}", e.getMessage(), e);
        } finally {
            SpeechState.isSpeaking = false;
        }
    }

    //Тональность голоса кота
    private byte[] applyPitchShiftInMemory(byte[] pcmData, double shiftSteps) {
        double factor = Math.pow(2.0, shiftSteps / 12.0);
        int claimedRate = (int) Math.round(SAMPLE_RATE * factor);

        short[] samples = bytesToShorts(pcmData);
        int outputLength = (int) Math.round(samples.length * ((double) SAMPLE_RATE / claimedRate));
        short[] resampled = new short[Math.max(outputLength, 1)];

        double step = (double) samples.length / resampled.length;
        for (int i = 0; i < resampled.length; i++) {
            double srcPos = i * step;
            int idx0 = (int) srcPos;
            int idx1 = Math.min(idx0 + 1, samples.length - 1);
            double frac = srcPos - idx0;

            if (idx0 >= samples.length) {
                resampled[i] = samples[samples.length - 1];
            } else {
                resampled[i] = (short) (samples[idx0] * (1 - frac) + samples[idx1] * frac);
            }
        }

        return shortsToBytes(resampled);
    }

    private short[] bytesToShorts(byte[] bytes) {
        short[] shorts = new short[bytes.length / 2];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = (short) ((bytes[i * 2 + 1] << 8) | (bytes[i * 2] & 0xff));
        }
        return shorts;
    }

    private byte[] shortsToBytes(short[] shorts) {
        byte[] bytes = new byte[shorts.length * 2];
        for (int i = 0; i < shorts.length; i++) {
            bytes[i * 2] = (byte) (shorts[i] & 0xff);
            bytes[i * 2 + 1] = (byte) ((shorts[i] >> 8) & 0xff);
        }
        return bytes;
    }

    // Раньше я записывал сырой звук на диск, применял питч через файл, и только потом проигрывал,
    // из-за двух обращений к диску и процесса питча задержка озвучки была ощутимой.
    // Сейчас всё делается в памяти без создания файла и сразу проигрывается.
    private void playPcmDirectly(byte[] pcmData) {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        try (AudioInputStream ais = new AudioInputStream(
                new ByteArrayInputStream(pcmData), format, pcmData.length / 2L);
             SourceDataLine line = AudioSystem.getSourceDataLine(format)) {

            line.open(format);
            line.start();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = ais.read(buffer, 0, buffer.length)) != -1) {
                line.write(buffer, 0, bytesRead);
            }
            line.drain();
            line.stop();
        } catch (Exception e) {
            log.error("[YandexTTS Playback Error] {}", e.getMessage());
        }
    }
}