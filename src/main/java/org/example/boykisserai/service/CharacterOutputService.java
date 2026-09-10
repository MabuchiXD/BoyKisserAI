package org.example.boykisserai.service;

import org.example.boykisserai.domain.entity.UserEntity;
import org.example.boykisserai.provider.speech.TextToSpeechService;
import org.example.boykisserai.service.ai.NeuroImpactParser.ImpactResult;
import org.example.boykisserai.service.animation.AnimationService;
import org.example.boykisserai.service.state.DashboardStateService;
import org.example.boykisserai.service.state.EmotionService;
import org.example.boykisserai.service.relationship.RelationshipService;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class CharacterOutputService {

    private final AnimationService animationService;
    private final TextToSpeechService ttsService;
    private final EmotionService emotionService;
    private final RelationshipService relationshipService;
    private final DashboardStateService dashboardStateService;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public CharacterOutputService(AnimationService animationService,
                                  TextToSpeechService ttsService,
                                  EmotionService emotionService,
                                  RelationshipService relationshipService,
                                  DashboardStateService dashboardStateService) {
        this.animationService = animationService;
        this.ttsService = ttsService;
        this.emotionService = emotionService;
        this.relationshipService = relationshipService;
        this.dashboardStateService = dashboardStateService;
    }

    public void present(UserEntity user, String platform, String displayName, String userInput, ImpactResult impact) {
        if (impact.trustDelta() != 0) {
            relationshipService.adjustTrust(user, impact.trustDelta());
        }

        if (!dashboardStateService.isForceEmotionOverride()) {
            emotionService.setEmotion(impact.emotion());
            dashboardStateService.setCurrentEmotion(impact.emotion());
        }

        String currentTime = timeFormat.format(new Date());
        dashboardStateService.setCurrentSpeaker(displayName);
        dashboardStateService.setCurrentPlatform(platform);
        dashboardStateService.setTrustLevel(user.getTrustLevel());
        dashboardStateService.setRelationship(relationshipService.getRelationshipType(user).getTitle());

        // Передаем action().name() в лог дашборда!
        dashboardStateService.logAiResponse(currentTime, platform, impact.cleanText(),
                impact.emotion().name(), impact.trustDelta(), impact.reason(), impact.action().name());

        long speechDurationMs = Math.max(1500, impact.cleanText().length() * 70L);
        animationService.simulateSpeaking(speechDurationMs);
        ttsService.speak(impact.cleanText());
    }
}