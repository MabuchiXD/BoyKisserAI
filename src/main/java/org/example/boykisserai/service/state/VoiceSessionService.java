package org.example.boykisserai.service.state;

import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.config.AppProperties;
import org.example.boykisserai.domain.constant.BehaviorType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VoiceSessionService {

    private final BehaviorService behaviorService;
    private final DashboardStateService dashboardStateService;
    private final AppProperties props;

    private volatile long lastVoiceInteractionTime = 0;
    private String currentLocalUserId = "mabuchi";
    private String currentLocalUserName = "Мабучи";

    public record SpeakerInfo(String id, String name) {}

    public VoiceSessionService(BehaviorService behaviorService,
                               DashboardStateService dashboardStateService,
                               AppProperties props) {
        this.behaviorService = behaviorService;
        this.dashboardStateService = dashboardStateService;
        this.props = props;
    }

    public boolean isSleepCommand(String lowerInput) {
        return lowerInput.contains("помолчи") ||
                lowerInput.contains("заткнись") ||
                lowerInput.contains("тишина") ||
                lowerInput.contains("усни") ||
                lowerInput.contains("закрой рот") ||
                lowerInput.contains("не мешай") ||
                lowerInput.contains("хватит болтать");
    }

    public boolean isWakeCommand(String lowerInput) {
        return lowerInput.contains("проснись") ||
                lowerInput.contains("очнись") ||
                lowerInput.contains("говори") ||
                lowerInput.contains("разговаривай") ||
                lowerInput.contains("болтай") ||
                lowerInput.contains("хватит молчать") ||
                lowerInput.contains("не молчи") ||
                lowerInput.contains("включайся") ||
                lowerInput.contains("я пришел") ||
                lowerInput.contains("я пришёл") ||
                lowerInput.contains("я вернулся") ||
                lowerInput.contains("я тут") ||
                lowerInput.contains("я здесь") ||
                isApologyCommand(lowerInput);
    }

    // Раньше бот молчал если послать его и извиниться, потому пришлось сделать так, что извинения тоже заставляют его говорить
    public boolean isApologyCommand(String lowerInput) {
        return lowerInput.contains("извини") ||
                lowerInput.contains("прости") ||
                lowerInput.contains("сорян") ||
                lowerInput.contains("был не прав") ||
                lowerInput.contains("не обижайся") ||
                lowerInput.contains("каюсь") ||
                lowerInput.contains("зря быканул");
    }

    public void setSilentMode() {
        behaviorService.setBehavior(BehaviorType.SILENT);
        this.lastVoiceInteractionTime = 0;
        log.info("[VoiceSession] Переведен в режим тишины (SILENT)");
    }

    public void setNormalMode() {
        behaviorService.setBehavior(BehaviorType.NORMAL);
        this.lastVoiceInteractionTime = System.currentTimeMillis();
        log.info("[VoiceSession] Пробужден в стандартный режим (NORMAL)");
    }

    public boolean shouldRespond() {
        return behaviorService.shouldRespond();
    }

    public SpeakerInfo resolveSpeaker(String lowerInput) {
        if (lowerInput.contains("это дина") || lowerInput.contains("я дина") || lowerInput.contains("говорит дина") || lowerInput.contains("с тобой дина")) {
            currentLocalUserId = "dina";
            currentLocalUserName = "Дина";
            log.info("[Speaker Switch] За микрофон села ДИНА!");
        } else if (lowerInput.contains("это мабучи") || lowerInput.contains("я мабучи") || lowerInput.contains("я вернулся") || lowerInput.contains("это я")) {
            currentLocalUserId = "mabuchi";
            currentLocalUserName = "Мабучи";
            log.info("[Speaker Switch] За микрофон вернулся МАБУЧИ!");
        }
        return new SpeakerInfo(currentLocalUserId, currentLocalUserName);
    }

    public boolean canProcessVoice(String lowerInput) {
        boolean hasWakeWord = lowerInput.contains("бойкиссер") ||
                lowerInput.contains("кот") ||
                lowerInput.contains("слушай") ||
                lowerInput.contains("бот") ||
                lowerInput.contains("привет") ||
                lowerInput.contains("хай") ||
                lowerInput.contains("здарова") ||
                lowerInput.contains("ку") ||
                isWakeCommand(lowerInput);

        long activeDialogWindowMs = props.getBot().getActiveDialogWindowMs();
        boolean isWithinActiveDialog = (System.currentTimeMillis() - lastVoiceInteractionTime) < activeDialogWindowMs;

        return hasWakeWord || isWithinActiveDialog;
    }

    public void updateSessionTimer() {
        this.lastVoiceInteractionTime = System.currentTimeMillis();
        long activeDialogWindowMs = props.getBot().getActiveDialogWindowMs();
        dashboardStateService.setDialogWindowExpireTime(lastVoiceInteractionTime + activeDialogWindowMs);
    }
}