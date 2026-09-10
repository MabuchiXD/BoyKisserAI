package org.example.boykisserai.provider.speech.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.config.AppProperties;
import org.example.boykisserai.provider.speech.SpeechToTextService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayOutputStream;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.stt.provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiSttService implements SpeechToTextService {

    private final AppProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiSttService(AppProperties props) {
        this.props = props;
    }

    @Override
    public String recognizeSpeech(byte[] pcmAudioBytes) {
        if (pcmAudioBytes == null || pcmAudioBytes.length == 0) return "";

        try {
            log.info("[OpenAI Whisper] Распознавание речи через Whisper API...");

            byte[] wavBytes = wrapPcmToWav(pcmAudioBytes, 48000);

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("model", "whisper-1");
            builder.part("language", "ru");
            builder.part("file", new ByteArrayResource(wavBytes) {
                @Override
                public String getFilename() {
                    return "speech.wav";
                }
            }).header(HttpHeaders.CONTENT_TYPE, "audio/wav");

            WebClient client = WebClient.builder().baseUrl(props.getOpenai().getBaseUrl()).build();

            String jsonResponse = client.post()
                    .uri("/v1/audio/transcriptions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getOpenai().getApiKey())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (jsonResponse != null) {
                JsonNode root = objectMapper.readTree(jsonResponse);
                if (root.has("text")) {
                    String recognizedText = root.get("text").asText().trim();

                    // Фильтр фантомных галлюцинаций Whisper
                    String lower = recognizedText.toLowerCase();
                    if (lower.contains("субтитр") ||
                            lower.contains("дубровск") ||
                            lower.contains("спасибо за просмотр") ||
                            lower.contains("продолжение следует") ||
                            lower.contains("подпишитесь") ||
                            lower.contains("подписывайтесь") ||
                            lower.equals(".") || lower.equals("!") || lower.equals("?")) {
                        log.debug("[Whisper Filter] Срезана фантомная галлюцинация тишины: \"{}\"", recognizedText);
                        return "";
                    }

                    log.info("[Whisper OK] Распознано: \"{}\"", recognizedText);
                    return recognizedText;
                }
            }

        } catch (Exception e) {
            log.error("[Whisper Error] Ошибка распознавания: {}", e.getMessage());
        }

        return "";
    }

    private byte[] wrapPcmToWav(byte[] pcmData, int sampleRate) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long totalDataLen = pcmData.length + 36;
        byte[] header = new byte[44];
        long byteRate = sampleRate * 2;

        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
        header[20] = 1; header[21] = 0;
        header[22] = 1; header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = 2; header[33] = 0;
        header[34] = 16; header[35] = 0;
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (pcmData.length & 0xff);
        header[41] = (byte) ((pcmData.length >> 8) & 0xff);
        header[42] = (byte) ((pcmData.length >> 16) & 0xff);
        header[43] = (byte) ((pcmData.length >> 24) & 0xff);

        out.write(header);
        out.write(pcmData);
        return out.toByteArray();
    }
}