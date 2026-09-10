package org.example.boykisserai.service.prompt;

import org.example.boykisserai.domain.constant.BehaviorType;
import org.example.boykisserai.domain.constant.EmotionType;
import org.example.boykisserai.domain.constant.PersonalityPhase;
import org.example.boykisserai.domain.constant.RelationshipType;
import org.example.boykisserai.domain.entity.UserEntity;
import org.example.boykisserai.service.memory.MemoryService;
import org.example.boykisserai.service.state.BehaviorService;
import org.example.boykisserai.service.state.DashboardStateService;
import org.example.boykisserai.service.relationship.RelationshipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PromptServiceTest {

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private MemoryService memoryService;

    @Mock
    private DashboardStateService dashboardStateService;

    @Mock
    private BehaviorService behaviorService;

    @InjectMocks
    private PromptService promptService;

    @BeforeEach
    void setUp() {
        // По умолчанию поиск третьего лица возвращает пустой Optional (чтобы не было NPE)
        when(memoryService.findTargetUserInMessage(anyString(), any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("КРОСС-ПЛАТФОРМЕННОЕ МНЕНИЕ: Бот подтягивает досье и факты Создателя, когда о нем спрашивают в Discord")
    void shouldInjectTargetUserDossierWhenAskedAboutAnotherPerson() {
        UserEntity speakerAsata = new UserEntity("DISCORD", "349937009399300096", "AsataEnot", false);
        UserEntity targetCreator = new UserEntity("LOCAL", "mabuchi", "Мабучи", true);

        when(relationshipService.getRelationshipType(speakerAsata)).thenReturn(RelationshipType.NEUTRAL);
        when(relationshipService.getRelationshipType(targetCreator)).thenReturn(RelationshipType.CREATOR);
        when(dashboardStateService.getCurrentPhase()).thenReturn(PersonalityPhase.CYNIC_MEME);
        when(dashboardStateService.getCurrentEmotion()).thenReturn(EmotionType.JOY);
        when(behaviorService.getCurrentBehavior()).thenReturn(BehaviorType.NORMAL);

        when(memoryService.findTargetUserInMessage("что думаешь о Мабучи?", speakerAsata))
                .thenReturn(Optional.of(targetCreator));
        when(memoryService.getUserFacts(targetCreator)).thenReturn(List.of("Пишет на Java"));
        when(memoryService.getUserBlunders(targetCreator)).thenReturn(List.of("Случайно удалил прод"));

        String prompt = promptService.buildSystemPrompt(speakerAsata, "что думаешь о Мабучи?");

        assertTrue(prompt.contains("ТЕБЯ СПРАШИВАЮТ О ДРУГОМ ЧЕЛОВЕКЕ"));
        assertTrue(prompt.contains("МАБУЧИ"));
        assertTrue(prompt.contains("Пишет на Java"));
        assertTrue(prompt.contains("Случайно удалил прод"));
    }

    @Test
    @DisplayName("ПОСЛЕВКУСИЕ 1 (Бычка после сна): При выходе из сна в состоянии IRRITATION бот сохраняет злость")
    void shouldRetainIrritationMoodWhenWokenUp() {
        UserEntity user = new UserEntity("DISCORD", "123", "AsataEnot", false);

        when(relationshipService.getRelationshipType(user)).thenReturn(RelationshipType.SUSPICIOUS);
        when(dashboardStateService.getCurrentPhase()).thenReturn(PersonalityPhase.CYNIC_MEME);
        when(dashboardStateService.getCurrentEmotion()).thenReturn(EmotionType.IRRITATION);
        when(behaviorService.getCurrentBehavior()).thenReturn(BehaviorType.NORMAL);

        String prompt = promptService.buildSystemPrompt(user, "ладно, говори");

        assertTrue(prompt.contains("IRRITATION"));
        assertTrue(prompt.contains("ТЫ ЗОЛ / РАЗДРАЖЕН"));
        assertTrue(prompt.contains("Быкани в ответ"));
    }

    @Test
    @DisplayName("ПОСЛЕВКУСИЕ 2 (Жалоба Создателю): Если кота разозлил чужак, при обращении Мабучи кот жалуется")
    void shouldInjectRecentDramaWhenTalkingToCreatorAfterConflict() {
        UserEntity creator = new UserEntity("LOCAL", "mabuchi", "Мабучи", true);

        when(relationshipService.getRelationshipType(creator)).thenReturn(RelationshipType.CREATOR);
        when(dashboardStateService.getCurrentPhase()).thenReturn(PersonalityPhase.CYNIC_MEME);
        when(dashboardStateService.getCurrentEmotion()).thenReturn(EmotionType.IRRITATION);
        when(dashboardStateService.getActiveDramaForCreator())
                .thenReturn("Пользователь AsataEnot в DISCORD выбесил тебя фразой: 'ты еблан'");
        when(behaviorService.getCurrentBehavior()).thenReturn(BehaviorType.NORMAL);

        String prompt = promptService.buildSystemPrompt(creator, "Привет, кот! Как дела?");

        assertTrue(prompt.contains("СВЕЖИЙ КОНФЛИКТ"));
        assertTrue(prompt.contains("AsataEnot"));
        assertTrue(prompt.contains("Пожалуйся Мабучи"));
    }

    @Test
    @DisplayName("ПРАВИЛО TWITCH: Промпт требует назвать зрителя по нику вслух БЕЗ знака @")
    void shouldRequireSpeakingNicknameWithoutAtSymbolForTwitch() {
        UserEntity twitchViewer = new UserEntity("TWITCH", "9988", "Sanya228", false);

        when(relationshipService.getRelationshipType(twitchViewer)).thenReturn(RelationshipType.NEUTRAL);
        when(dashboardStateService.getCurrentPhase()).thenReturn(PersonalityPhase.CYNIC_MEME);
        when(dashboardStateService.getCurrentEmotion()).thenReturn(EmotionType.JOY);
        when(behaviorService.getCurrentBehavior()).thenReturn(BehaviorType.NORMAL);

        String prompt = promptService.buildSystemPrompt(twitchViewer, "Кот привет");

        assertTrue(prompt.contains("ПРАВИЛО СТРИМА TWITCH"));
        assertTrue(prompt.contains("Sanya228"));
        assertTrue(prompt.contains("БЕЗ знака @"));
    }

    @Test
    @DisplayName("Системный промпт содержит правила токсик-режима и JSON-схему ответа при фазе DEMON_TOXIC")
    void shouldIncludeDemonToxicRulesWhenPhaseIsActive() {
        UserEntity user = new UserEntity("DISCORD", "123", "AsataEnot", false);

        when(relationshipService.getRelationshipType(user)).thenReturn(RelationshipType.NEUTRAL);
        when(dashboardStateService.getCurrentPhase()).thenReturn(PersonalityPhase.DEMON_TOXIC);
        when(dashboardStateService.getCurrentEmotion()).thenReturn(EmotionType.JOY);
        when(behaviorService.getCurrentBehavior()).thenReturn(BehaviorType.NORMAL);

        String prompt = promptService.buildSystemPrompt(user, "привет");

        assertTrue(prompt.contains("ДЕМОНА"));
        // Формат ответа теперь JSON: было "MOOD:" в квадратных скобках,
        // стало поле "emotion" в JSON-схеме — проверяем актуальный маркер формата.
        assertTrue(prompt.contains("\"emotion\""));
    }

    @Test
    @DisplayName("Промпт для Дины содержит теплое отношение и статус девушки Создателя")
    void shouldGenerateSpecialPromptForDina() {
        UserEntity dina = new UserEntity("LOCAL", "dina", "Дина", false);

        when(relationshipService.getRelationshipType(dina)).thenReturn(RelationshipType.BELOVED);
        when(dashboardStateService.getCurrentPhase()).thenReturn(PersonalityPhase.CYNIC_MEME);
        when(dashboardStateService.getCurrentEmotion()).thenReturn(EmotionType.JOY);
        when(behaviorService.getCurrentBehavior()).thenReturn(BehaviorType.NORMAL);

        String prompt = promptService.buildSystemPrompt(dina, "Привет кот!");

        assertTrue(prompt.contains("Девушка Мабучи"));
    }

    @Test
    @DisplayName("Промпт подтягивает факты и компромат пользователя из памяти")
    void shouldInjectUserFactsAndBlunders() {
        UserEntity user = new UserEntity("DISCORD", "123", "AsataEnot", false);

        when(relationshipService.getRelationshipType(user)).thenReturn(RelationshipType.FRIEND);
        when(dashboardStateService.getCurrentPhase()).thenReturn(PersonalityPhase.CYNIC_MEME);
        when(dashboardStateService.getCurrentEmotion()).thenReturn(EmotionType.JOY);
        when(behaviorService.getCurrentBehavior()).thenReturn(BehaviorType.NORMAL);
        when(memoryService.getUserFacts(user)).thenReturn(List.of("Любит аниме"));
        when(memoryService.getUserBlunders(user)).thenReturn(List.of("Назвал бота глупым"));

        String prompt = promptService.buildSystemPrompt(user, "как дела?");

        assertTrue(prompt.contains("Любит аниме"));
        assertTrue(prompt.contains("Назвал бота глупым"));
    }
}