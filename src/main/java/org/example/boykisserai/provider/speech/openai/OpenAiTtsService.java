package org.example.boykisserai.provider.speech.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.config.AppProperties;
import org.example.boykisserai.provider.speech.SpeechState;
import org.example.boykisserai.provider.speech.TextToSpeechService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sound.sampled.*;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.tts.provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiTtsService implements TextToSpeechService {

    private final AppProperties props;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiTtsService(AppProperties props) {
        this.props = props;
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

            String jsonPayload = objectMapper.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getOpenai().getBaseUrl() + "/v1/audio/speech"))
                    .header("Authorization", "Bearer " + props.getOpenai().getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200 && response.body().length > 0) {
                Files.write(audioFile.toPath(), response.body());
                playWavAudioStream(audioFile);
            } else {
                String errorBody = new String(response.body(), StandardCharsets.UTF_8);
                log.error("[OpenAI TTS Error] Код: {}, Детали: {}", response.statusCode(), errorBody);
            }

        } catch (Exception e) {
            log.error("[OpenAI TTS Error] {}", e.getMessage());
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
            log.error("[OpenAI TTS Playback Error] Ошибка воспроизведения: {}", e.getMessage());
        }
    }
}