package org.example.boykisserai.service.relationship;

import org.example.boykisserai.domain.entity.UserEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ToxicDefenseService {

    private final RelationshipService relationshipService;

    public enum DefenseState {
        NORMAL,          // Обычный диалог
        SHOULD_IGNORE,   // Полный игнор (бот молчит)
        TRIGGER_KICK,    // Пора сгенерировать уникальный финальный посыл
        APOLOGY_ACCEPTED // Извинение принято
    }

    private final Map<String, Integer> toxicStrikeCount = new ConcurrentHashMap<>();
    private final Map<String, Boolean> ignoredUsers = new ConcurrentHashMap<>();

    public ToxicDefenseService(RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    public DefenseState evaluate(UserEntity user, String userInput) {
        if (user.isCreator()) return DefenseState.NORMAL;

        String lower = userInput.toLowerCase();
        String userId = user.getExternalId();

        // 1. Проверка на извинения
        if (isApology(lower)) {
            ignoredUsers.remove(userId);
            toxicStrikeCount.remove(userId);
            relationshipService.adjustTrust(user, 15);
            return DefenseState.APOLOGY_ACCEPTED;
        }

        // 2. Если уже в игноре — молчим
        if (ignoredUsers.getOrDefault(userId, false)) {
            System.out.println("[ToxicDefense] Забаненный " + user.getDisplayName() + " проигнорирован.");
            return DefenseState.SHOULD_IGNORE;
        }

        // 3. Если доверие на нуле — считаем страйки
        if (user.getTrustLevel() <= 0) {
            int strikes = toxicStrikeCount.getOrDefault(userId, 0) + 1;
            toxicStrikeCount.put(userId, strikes);

            // На 3-й страйк запускаем финальный посыл от ИИ
            if (strikes >= 3) {
                return DefenseState.TRIGGER_KICK;
            }
        } else {
            toxicStrikeCount.remove(userId);
        }

        return DefenseState.NORMAL;
    }

    public void lockUserInIgnore(String userId) {
        ignoredUsers.put(userId, true);
    }

    public boolean isUserIgnored(String userId) {
        return ignoredUsers.getOrDefault(userId, false);
    }

    private boolean isApology(String text) {
        return text.contains("прости") ||
                text.contains("извини") ||
                text.contains("сорян") ||
                text.contains("был не прав") ||
                text.contains("не обижайся") ||
                text.contains("каюсь") ||
                text.contains("зря быканул");
    }
}