package org.example.boykisserai.service.state;

import org.example.boykisserai.config.AppProperties;
import org.example.boykisserai.domain.constant.BehaviorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoiceSessionServiceTest {

    @Mock
    private BehaviorService behaviorService;

    @Mock
    private DashboardStateService dashboardStateService;

    // AppProperties — простой конфиг-POJO, мокать его нет смысла: собираем
    // реальный объект с нужными значениями и передаём в конструктор явно.
    // @InjectMocks не подходит здесь, так как без мока/бина для AppProperties
    // он подставляет null, и любой вызов props.getBot()... роняет NPE.
    private AppProperties props;

    private VoiceSessionService voiceSessionService;

    @BeforeEach
    void setUp() {
        props = new AppProperties();
        props.getBot().setActiveDialogWindowMs(25000);

        voiceSessionService = new VoiceSessionService(behaviorService, dashboardStateService, props);
    }

    @Test
    @DisplayName("РАСПОЗНАВАНИЕ ДИНЫ: Должен переключить спикера на Дину при представлении")
    void shouldSwitchSpeakerToDinaWhenIntroduced() {
        var defaultSpeaker = voiceSessionService.resolveSpeaker("просто фраза");
        assertEquals("mabuchi", defaultSpeaker.id());

        var dinaSpeaker = voiceSessionService.resolveSpeaker("привет кот, это дина!");
        assertEquals("dina", dinaSpeaker.id(), "ID спикера должен стать 'dina'");
        assertEquals("Дина", dinaSpeaker.name(), "Имя спикера должно стать 'Дина'");
    }

    @Test
    @DisplayName("ВОЗВРАЩЕНИЕ МАБУЧИ: Должен вернуть профиль Создателя")
    void shouldSwitchSpeakerBackToMabuchi() {
        voiceSessionService.resolveSpeaker("я дина");

        var mabuchiSpeaker = voiceSessionService.resolveSpeaker("кот, я вернулся, это мабучи");
        assertEquals("mabuchi", mabuchiSpeaker.id(), "ID спикера должен снова стать 'mabuchi'");
        assertEquals("Мабучи", mabuchiSpeaker.name(), "Имя спикера должно стать 'Мабучи'");
    }

    @Test
    @DisplayName("КОМАНДЫ СНА: Должен распознавать любые вариации просьбы помолчать")
    void shouldRecognizeSleepCommands() {
        assertTrue(voiceSessionService.isSleepCommand("кот помолчи пожалуйста"));
        assertTrue(voiceSessionService.isSleepCommand("заткнись"));
        assertTrue(voiceSessionService.isSleepCommand("закрой рот"));
        assertTrue(voiceSessionService.isSleepCommand("хватит болтать"));
        assertFalse(voiceSessionService.isSleepCommand("привет как дела"));
    }

    @Test
    @DisplayName("КОМАНДЫ ПРОБУЖДЕНИЯ: Должен понимать 'я пришел', 'говори', 'хватит молчать'")
    void shouldRecognizeWakeCommands() {
        assertTrue(voiceSessionService.isWakeCommand("всё, я пришёл, говори"));
        assertTrue(voiceSessionService.isWakeCommand("хватит молчать"));
        assertTrue(voiceSessionService.isWakeCommand("кот проснись"));
        assertTrue(voiceSessionService.isWakeCommand("я тут"));
        assertTrue(voiceSessionService.isWakeCommand("разговаривай"));
        assertFalse(voiceSessionService.isWakeCommand("как погода"));
    }

    @Test
    @DisplayName("УПРАВЛЕНИЕ РЕЖИМАМИ: Должен переключать BehaviorService в SILENT и NORMAL")
    void shouldSwitchBehaviorModes() {
        voiceSessionService.setSilentMode();
        verify(behaviorService).setBehavior(BehaviorType.SILENT);

        voiceSessionService.setNormalMode();
        verify(behaviorService).setBehavior(BehaviorType.NORMAL);
    }

    @Test
    @DisplayName("ФИЛЬТР МИКРОФОНА: Должен пропускать речь при наличии триггер-слов и пробуждении")
    void shouldAllowVoiceWhenWakeWordIsPresent() {
        assertTrue(voiceSessionService.canProcessVoice("привет как дела"));
        assertTrue(voiceSessionService.canProcessVoice("кот что думаешь"));
        assertTrue(voiceSessionService.canProcessVoice("бойкиссер слушай"));
        assertTrue(voiceSessionService.canProcessVoice("здарова"));
        assertTrue(voiceSessionService.canProcessVoice("я пришел говори"));
    }

    @Test
    @DisplayName("ТАЙМЕР ДАШБОРДА: Должен обновлять время окна диалога в DashboardStateService")
    void shouldUpdateDashboardTimerOnSessionRenew() {
        voiceSessionService.updateSessionTimer();
        verify(dashboardStateService, times(1)).setDialogWindowExpireTime(anyLong());
    }
}