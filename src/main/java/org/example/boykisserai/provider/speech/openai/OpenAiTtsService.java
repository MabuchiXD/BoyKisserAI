package org.example.boykisserai.provider.speech.openai;

import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.config.AppProperties;
import org.example.boykisserai.provider.speech.SpeechState;
import org.example.boykisserai.provider.speech.TextToSpeechService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.nio.file.Files;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.tts.provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiTtsService implements TextToSpeechService {

    private final AppProperties props;
    private final WebClient webClient;

    public OpenAiTtsService(AppProperties props) {
        this.props = props;
        this.webClient = WebClient.builder().baseUrl(props.getOpenai().getBaseUrl()).build();
    }

    @Override
    public void speak(String text) {
        if (text == null || text.isBlank()) return;

        String cleanText = text.replaceAll("[^a-zA-Zа-яА-Я0-9\\s,.!?-]", "").trim();
        if (cleanText.isEmpty()) return;

        File audioFile = new File("openai_voice.wav");
        String voice = props.getOpenai().getVoice();

        try {
            SpeechState.isSpeaking = true;
            log.info("[OpenAI TTS] Синтез речи голосом [{}]...", voice);

            Map<String, Object> bodyMap = Map.of(
                    "model", "tts-1",
                    "input", cleanText,
                    "voice", voice.toLowerCase().trim(),
                    "response_format", "wav",
                    "speed", props.getOpenai().getTtsSpeed()
            );

            // WebClient сам сериализует Map в JSON через Jackson — не нужно
            // вручную вызывать objectMapper.writeValueAsString(...), как это
            // требовалось при ручной сборке HttpRequest.
            byte[] audioBytes = webClient.post()
                    .uri("/v1/audio/speech")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getOpenai().getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(bodyMap)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            if (audioBytes != null && audioBytes.length > 0) {
                Files.write(audioFile.toPath(), audioBytes);
                playWavAudioStream(audioFile);
            } else {
                log.error("[OpenAI TTS Error] Пустой ответ от API");
            }

        } catch (Exception e) {
            log.error("[OpenAI TTS Error] {}", e.getMessage(), e);
        } finally {
            SpeechState.isSpeaking = false;
        }
    }

    private void playWavAudioStream(File audioFile) {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat baseFormat = in.getFormat();
            AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );

            try (AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, in);
                 SourceDataLine line = AudioSystem.getSourceDataLine(decodedFormat)) {

                line.open(decodedFormat);
                line.start();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = din.read(buffer, 0, buffer.length)) != -1) {
                    line.write(buffer, 0, bytesRead);
                }
                line.drain();
                line.stop();
            }
        } catch (Exception e) {
            log.error("[OpenAI TTS Playback Error] Ошибка воспроизведения: {}", e.getMessage(), e);
        }
    }
}