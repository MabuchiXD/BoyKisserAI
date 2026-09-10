package org.example.boykisserai.provider.speech.yandex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.config.AppProperties;
import org.example.boykisserai.provider.speech.SpeechToTextService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.stt.provider", havingValue = "yandex")
public class YandexSttService implements SpeechToTextService {

    private final AppProperties props;
    private final WebClient webClient = WebClient.create("https://stt.api.cloud.yandex.net");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public YandexSttService(AppProperties props) {
        this.props = props;
    }

    @Override
    public String recognizeSpeech(byte[] pcmAudioBytes) {
        if (pcmAudioBytes == null || pcmAudioBytes.length == 0) return "";

        try {
            log.info("[YandexSTT] Отправка записанной речи на сервер Яндекса...");

            String jsonResponse = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/speech/v1/stt:recognize")
                            .queryParam("lang", "ru-RU")
                            .queryParam("topic", "general")
                            .queryParam("format", "lpcm")
                            .queryParam("sampleRateHertz", "48000")
                            .build())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header("Authorization", "Api-Key " + props.getYandex().getApiKey())
                    .bodyValue(pcmAudioBytes)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (jsonResponse != null) {
                JsonNode root = objectMapper.readTree(jsonResponse);
                if (root.has("result")) {
                    String recognizedText = root.get("result").asText();
                    log.info("[YandexSTT OK] Распознано: \"{}\"", recognizedText);
                    return recognizedText;
                }
            }

        } catch (Exception e) {
            log.error("[YandexSTT Error] Ошибка распознавания речи: {}", e.getMessage());
        }

        return "";
    }
}