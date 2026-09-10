package org.example.boykisserai.service.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.boykisserai.domain.constant.CharacterAction;
import org.example.boykisserai.domain.constant.EmotionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

//Парсит эмоции и состояние персонажа на основе JSON-а поступившего от ИИ
@Component
public class NeuroImpactParser {

    private static final Logger log = LoggerFactory.getLogger(NeuroImpactParser.class);

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Модель иногда пишет "чтото", "какойто" слитно и без дефиса, поэтому принудительно поставил дефис
    // для грамматически верного "что-то", "какой-то".
    private static final Pattern HYPHEN_FIX_PATTERN = Pattern.compile(
            "(?iu)\\b(кто|что|кем|чем|кому|чему|кого|чего|как|где|куда|откуда|когда|почему|зачем|какой|какая|какое|какие|каком|каких|каким)(то|либо|нибудь)\\b"
    );

    // "коекто", "коечто" слитно -> "кое-кто", "кое-что"
    private static final Pattern KOE_FIX_PATTERN = Pattern.compile(
            "(?iu)\\bкое(кто|что|кого|чего|кому|чему|кем|чем|как|где|куда)\\b"
    );

    public record ImpactResult(
            String cleanText,
            EmotionType emotion,
            int trustDelta,
            String reason,
            CharacterAction action
    ) {}

    //На случай если ИИ выдаст вообще не то название модуля, которое от него требуется
    private record NeuroImpactDto(
            String text,
            String emotion,
            @JsonProperty("trust_delta") Integer trustDelta,
            String reason,
            String action
    ) {}

    public ImpactResult parse(String rawReply) {
        if (rawReply == null || rawReply.isBlank()) {
            return fallback("");
        }

        String rawTrimmed = rawReply.trim();
        String json = extractJsonBlock(rawTrimmed);

        if (json != null) {
            try {
                NeuroImpactDto dto = mapper.readValue(json, NeuroImpactDto.class);
                String cleanText = autoCorrectRussianGrammar(dto.text() != null ? dto.text().trim() : "");
                return new ImpactResult(
                        cleanText,
                        parseEmotionSafe(dto.emotion()),
                        dto.trustDelta() != null ? dto.trustDelta() : 0,
                        dto.reason() != null && !dto.reason().isBlank() ? dto.reason() : "Обычный ответ",
                        parseActionSafe(dto.action())
                );
            } catch (Exception e) {
                log.warn("Не удалось распарсить JSON-ответ модели, использую fallback. Причина: {}", e.getMessage());
            }
        }

        // Модель (особенно локальная qwen через Ollama) может проигнорировать
        // формат и вернуть просто текст — в этом случае не пытаемся угадать
        // структуру, отдаём текст как обычную реплику с нейтральными метаданными.
        return fallback(rawTrimmed);
    }

    // ИИ может выдать ответ с текстом до содержания JSON-а, а так он читает только то, что внутри полей предполагаемого JSON-а
    private String extractJsonBlock(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start == -1 || end == -1 || end <= start) {
            return null;
        }
        return text.substring(start, end + 1);
    }

    private ImpactResult fallback(String text) {
        return new ImpactResult(autoCorrectRussianGrammar(text), EmotionType.JOY, 0, "Не удалось извлечь метаданные ответа", CharacterAction.NONE);
    }

    private String autoCorrectRussianGrammar(String text) {
        if (text == null || text.isEmpty()) return "";
        String fixed = HYPHEN_FIX_PATTERN.matcher(text).replaceAll("$1-$2");
        return KOE_FIX_PATTERN.matcher(fixed).replaceAll("кое-$1");
    }

    private EmotionType parseEmotionSafe(String val) {
        if (val == null) return EmotionType.JOY;
        try {
            return EmotionType.valueOf(val.trim().toUpperCase());
        } catch (Exception e) {
            return EmotionType.JOY;
        }
    }

    private CharacterAction parseActionSafe(String val) {
        if (val == null) return CharacterAction.NONE;
        try {
            return CharacterAction.valueOf(val.trim().toUpperCase());
        } catch (Exception e) {
            return CharacterAction.NONE;
        }
    }
}