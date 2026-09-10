package org.example.boykisserai.service;

import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.domain.constant.CharacterAction;
import org.example.boykisserai.domain.constant.EmotionType;
import org.example.boykisserai.domain.entity.UserEntity;
import org.example.boykisserai.service.ai.AiService;
import org.example.boykisserai.service.ai.NeuroImpactParser;
import org.example.boykisserai.service.memory.FactExtractorService;
import org.example.boykisserai.service.memory.MemoryService;
import org.example.boykisserai.service.relationship.ToxicDefenseService;
import org.example.boykisserai.service.state.DashboardStateService;
import org.example.boykisserai.service.state.VoiceSessionService;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class CharacterService {

    private final VoiceSessionService voiceSession;
    private final MemoryService memoryService;
    private final FactExtractorService factExtractor;
    private final AiService aiService;
    private final NeuroImpactParser impactParser;
    private final CharacterOutputService outputService;
    private final DashboardStateService dashboardStateService;
    private final ToxicDefenseService toxicDefenseService;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    private final ReentrantLock dialogueLock = new ReentrantLock(true);

    public CharacterService(VoiceSessionService voiceSession,
                            MemoryService memoryService,
                            FactExtractorService factExtractor,
                            AiService aiService,
                            NeuroImpactParser impactParser,
                            CharacterOutputService outputService,
                            DashboardStateService dashboardStateService,
                            ToxicDefenseService toxicDefenseService) {
        this.voiceSession = voiceSession;
        this.memoryService = memoryService;
        this.factExtractor = factExtractor;
        this.aiService = aiService;
        this.impactParser = impactParser;
        this.outputService = outputService;
        this.dashboardStateService = dashboardStateService;
        this.toxicDefenseService = toxicDefenseService;
    }

    public String chat(String externalId, String displayName, String platform, String userInput) {
        if (userInput == null || userInput.isBlank()) return "";
        String lowerInput = userInput.toLowerCase();

        boolean isWake = voiceSession.isWakeCommand(lowerInput);
        boolean isSleep = voiceSession.isSleepCommand(lowerInput);

        if (isWake) {
            voiceSession.setNormalMode();
        } else if (!voiceSession.shouldRespond()) {
            return "";
        }

        String finalExternalId = externalId;
        String finalDisplayName = displayName;

        if ("LOCAL".equalsIgnoreCase(platform) || "LOCAL_MIC".equalsIgnoreCase(platform)) {
            var speaker = voiceSession.resolveSpeaker(lowerInput);
            finalExternalId = speaker.id();
            finalDisplayName = speaker.name();

            if (!voiceSession.canProcessVoice(lowerInput)) {
                log.debug("[Local Filter] Пропущено: \"{}\"", userInput);
                return "";
            }
        }

        dialogueLock.lock();
        try {
            UserEntity user = memoryService.getOrCreateUser(platform, finalExternalId, finalDisplayName);
            dashboardStateService.setCurrentActiveUser(user);

            var state = toxicDefenseService.evaluate(user, userInput);
            if (state == ToxicDefenseService.DefenseState.SHOULD_IGNORE) {
                return "";
            }

            String currentTime = timeFormat.format(new Date());
            dashboardStateService.logUserMessage(currentTime, finalDisplayName, platform, userInput);

            log.info("[Boykisser] Генерация ответа для {} ({})...", finalDisplayName, platform);
            String rawReply = aiService.generateReply(user, userInput);
            var impact = impactParser.parse(rawReply);

            if (!user.isCreator() && (impact.trustDelta() < 0 || impact.emotion() == EmotionType.IRRITATION)) {
                dashboardStateService.recordDrama(finalDisplayName, platform, userInput, impact.reason());
            }

            memoryService.saveShortTermMessage(finalExternalId, "user", userInput);
            memoryService.saveShortTermMessage(finalExternalId, "assistant", impact.cleanText());
            factExtractor.extractAndSaveFacts(user, userInput);

            log.info("[Boykisser AI]: \"{}\" (Emotion: {}, Trust: {})", impact.cleanText(), impact.emotion(), impact.trustDelta());

            outputService.present(user, platform, finalDisplayName, userInput, impact);

            if (user.isCreator() || "dina".equalsIgnoreCase(finalExternalId)) {
                dashboardStateService.markDramaAsVented();
            }

            if (isSleep || impact.action() == CharacterAction.SLEEP) {
                voiceSession.setSilentMode();
                log.info("[CharacterService] Бот переведен в спящий режим (ACTION: SLEEP)");
            } else if (impact.action() == CharacterAction.WAKE) {
                voiceSession.setNormalMode();
            }

            if (state == ToxicDefenseService.DefenseState.TRIGGER_KICK) {
                toxicDefenseService.lockUserInIgnore(finalExternalId);
                log.warn("[ToxicDefense] Пользователь {} заблокирован в игнор!", finalDisplayName);
            }

            if ("LOCAL".equalsIgnoreCase(platform) || "LOCAL_MIC".equalsIgnoreCase(platform)) {
                voiceSession.updateSessionTimer();
            }

            return impact.cleanText();

        } finally {
            dialogueLock.unlock();
        }
    }
}