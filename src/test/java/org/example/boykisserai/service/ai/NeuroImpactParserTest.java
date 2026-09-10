package org.example.boykisserai.service.ai;

import org.example.boykisserai.domain.constant.CharacterAction;
import org.example.boykisserai.domain.constant.EmotionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NeuroImpactParserTest {

    private NeuroImpactParser parser;

    @BeforeEach
    void setUp() {
        parser = new NeuroImpactParser();
    }

    @Test
    @DisplayName("ОСНОВНОЙ СЛУЧАЙ: Должен распарсить валидный JSON-ответ модели")
    void shouldParseValidJsonResponse() {
        String aiResponse = """
                {"text": "Ты вернулся, заебись! Чем займёмся?", "emotion": "JOY", "trust_delta": 2, "reason": "Пользователь вернулся", "action": "WAKE"}
                """;

        var impact = parser.parse(aiResponse);

        assertEquals(CharacterAction.WAKE, impact.action());
        assertEquals(EmotionType.JOY, impact.emotion());
        assertEquals(2, impact.trustDelta());
        assertEquals("Ты вернулся, заебись! Чем займёмся?", impact.cleanText());
    }

    @Test
    @DisplayName("JSON В MARKDOWN-ОБЁРТКЕ: Должен вытащить объект, даже если модель обернула его в ```json ... ```")
    void shouldExtractJsonWrappedInMarkdown() {
        String aiResponse = """
                Вот мой ответ:
                ```json
                {"text": "Да пошёл ты сам, молчу.", "emotion": "IRRITATION", "trust_delta": -5, "reason": "Приказали свалить", "action": "SLEEP"}
                ```
                """;

        var impact = parser.parse(aiResponse);

        assertEquals(CharacterAction.SLEEP, impact.action());
        assertEquals(EmotionType.IRRITATION, impact.emotion());
        assertEquals(-5, impact.trustDelta());
        assertEquals("Да пошёл ты сам, молчу.", impact.cleanText());
    }

    @Test
    @DisplayName("ГРАММАТИКА: Должен склеивать через дефис слитные 'чтото', 'коечто' в тексте ответа")
    void shouldFixRussianHyphenGrammarInText() {
        String aiResponse = """
                {"text": "Расскажи мне чтото интересное, у меня коечто есть для тебя", "emotion": "INTEREST", "trust_delta": 0, "reason": "Обычный ответ", "action": "NONE"}
                """;

        var impact = parser.parse(aiResponse);

        assertEquals("Расскажи мне что-то интересное, у меня кое-что есть для тебя", impact.cleanText());
    }

    @Test
    @DisplayName("НЕИЗВЕСТНАЯ ЭМОЦИЯ/ACTION: Должен деградировать в дефолт, если модель прислала невалидное значение поля")
    void shouldFallbackToDefaultsForInvalidEnumValues() {
        String aiResponse = """
                {"text": "Обычный ответ", "emotion": "НЕСУЩЕСТВУЮЩАЯ_ЭМОЦИЯ", "trust_delta": 1, "reason": "Тест", "action": "ЧЕПУХА"}
                """;

        var impact = parser.parse(aiResponse);

        assertEquals(EmotionType.JOY, impact.emotion());
        assertEquals(CharacterAction.NONE, impact.action());
        assertEquals(1, impact.trustDelta());
        assertEquals("Обычный ответ", impact.cleanText());
    }

    @Test
    @DisplayName("ФОЛЛБЭК: Обычный текст без JSON вообще (например, локальная модель проигнорировала формат)")
    void shouldHandleFallbackGracefully() {
        String rawText = "Просто обычный ответ без каких-либо мета-данных";
        var impact = parser.parse(rawText);

        assertEquals(CharacterAction.NONE, impact.action());
        assertEquals(EmotionType.JOY, impact.emotion());
        assertEquals(0, impact.trustDelta());
        assertEquals(rawText, impact.cleanText());
    }

    @Test
    @DisplayName("ФОЛЛБЭК: Битый JSON (например, обрезанный ответ) не должен ронять парсинг исключением")
    void shouldFallbackGracefullyOnMalformedJson() {
        String brokenJson = "{\"text\": \"Ты перебил меня на полусл";

        var impact = parser.parse(brokenJson);

        assertEquals(CharacterAction.NONE, impact.action());
        assertEquals(EmotionType.JOY, impact.emotion());
        assertEquals(0, impact.trustDelta());
    }
}