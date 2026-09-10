package org.example.boykisserai.service.state;

import org.example.boykisserai.domain.constant.BehaviorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BehaviorServiceTest {

    private BehaviorService behaviorService;

    @BeforeEach
    void setUp() {
        behaviorService = new BehaviorService();
    }

    @Test
    @DisplayName("По умолчанию бот должен находиться в режиме NORMAL и отвечать на сообщения")
    void shouldBeInNormalModeByDefault() {
        assertEquals(BehaviorType.NORMAL, behaviorService.getCurrentBehavior());
        assertTrue(behaviorService.shouldRespond(), "В режиме NORMAL бот должен отвечать");
    }

    @Test
    @DisplayName("РЕЖИМ ТИШИНЫ: В режиме SILENT и SLEEPING бот должен полностью молчать")
    void shouldNotRespondWhenSilentOrSleeping() {
        // 1. Включаем тишину
        behaviorService.setBehavior(BehaviorType.SILENT);
        assertFalse(behaviorService.shouldRespond(), "В режиме SILENT shouldRespond() должен быть false!");

        // 2. Включаем сон
        behaviorService.setBehavior(BehaviorType.SLEEPING);
        assertFalse(behaviorService.shouldRespond(), "В режиме SLEEPING shouldRespond() должен быть false!");
    }

    @Test
    @DisplayName("В режимах QUIET и CHATTY бот должен продолжать отвечать")
    void shouldRespondInQuietAndChattyModes() {
        behaviorService.setBehavior(BehaviorType.QUIET);
        assertTrue(behaviorService.shouldRespond(), "В режиме QUIET бот должен отвечать");

        behaviorService.setBehavior(BehaviorType.CHATTY);
        assertTrue(behaviorService.shouldRespond(), "В режиме CHATTY бот должен отвечать");
    }

    @Test
    @DisplayName("Парсер команд должен переключать режимы по фразам пользователя")
    void shouldSwitchBehaviorsOnUserCommands() {
        // Команда тишины
        behaviorService.processUserCommands("кот помолчи пожалуйста");
        assertEquals(BehaviorType.SILENT, behaviorService.getCurrentBehavior());
        assertFalse(behaviorService.shouldRespond());

        // Команда пробуждения
        behaviorService.processUserCommands("снова разговаривай");
        assertEquals(BehaviorType.NORMAL, behaviorService.getCurrentBehavior());
        assertTrue(behaviorService.shouldRespond());

        // Команда краткости
        behaviorService.processUserCommands("будь тише и говори поменьше");
        assertEquals(BehaviorType.QUIET, behaviorService.getCurrentBehavior());

        // Команда болтливости
        behaviorService.processUserCommands("давай поболтаем");
        assertEquals(BehaviorType.CHATTY, behaviorService.getCurrentBehavior());
    }
}