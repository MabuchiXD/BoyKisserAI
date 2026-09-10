package org.example.boykisserai.service.state;

import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.domain.constant.BehaviorType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BehaviorService {

    private BehaviorType currentBehavior = BehaviorType.NORMAL;

    public BehaviorType getCurrentBehavior() {
        return currentBehavior;
    }

    public void setBehavior(BehaviorType behavior) {
        this.currentBehavior = behavior;
        log.info("[BehaviorService] Режим поведения изменен на: {}", behavior);
    }

    public void processUserCommands(String userMessage) {
        String lower = userMessage.toLowerCase();
        if (lower.contains("помолчи") || lower.contains("тишина")) {
            setBehavior(BehaviorType.SILENT);
        } else if (lower.contains("говори поменьше") || lower.contains("будь тише")) {
            setBehavior(BehaviorType.QUIET);
        } else if (lower.contains("поболтаем") || lower.contains("говори больше")) {
            setBehavior(BehaviorType.CHATTY);
        } else if (lower.contains("снова разговаривай") || lower.contains("общайся")) {
            setBehavior(BehaviorType.NORMAL);
        }
    }

    public boolean shouldRespond() {
        return currentBehavior != BehaviorType.SILENT && currentBehavior != BehaviorType.SLEEPING;
    }
}