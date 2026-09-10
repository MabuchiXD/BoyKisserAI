package org.example.boykisserai.service.memory;

import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.config.AppProperties;
import org.example.boykisserai.domain.constant.MemoryType;
import org.example.boykisserai.domain.entity.MemoryEntity;
import org.example.boykisserai.domain.entity.UserEntity;
import org.example.boykisserai.domain.model.Message;
import org.example.boykisserai.repository.MemoryRepository;
import org.example.boykisserai.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class MemoryService {

    private final UserRepository userRepository;
    private final MemoryRepository memoryRepository;
    private final StringRedisTemplate redisTemplate;
    private final AppProperties props;

    private static final String CREATOR_CANONICAL_PLATFORM = "LOCAL";
    private static final String CREATOR_CANONICAL_EXTERNAL_ID = "mabuchi";

    //TTL без входящих сообщений теперь 4 часа неактивности
    private static final Duration SHORT_TERM_MEMORY_TTL = Duration.ofHours(4);

    public MemoryService(UserRepository userRepository,
                         MemoryRepository memoryRepository,
                         StringRedisTemplate redisTemplate,
                         AppProperties props) {
        this.userRepository = userRepository;
        this.memoryRepository = memoryRepository;
        this.redisTemplate = redisTemplate;
        this.props = props;
    }

    public UserEntity getOrCreateUser(String platform, String externalId, String displayName) {
        String creatorId = props.getBot().getCreatorDiscordId();
        boolean isCreatorIdentity = "LOCAL".equalsIgnoreCase(platform) ||
                (creatorId != null && creatorId.equalsIgnoreCase(externalId)) ||
                "mabuchi".equalsIgnoreCase(externalId) ||
                "mabuchi".equalsIgnoreCase(displayName);

        String lookupPlatform = isCreatorIdentity ? CREATOR_CANONICAL_PLATFORM : platform;
        String lookupExternalId = isCreatorIdentity ? CREATOR_CANONICAL_EXTERNAL_ID : externalId;

        return userRepository.findByPlatformAndExternalId(lookupPlatform, lookupExternalId)
                .map(existingUser -> {
                    if (displayName != null && !displayName.equals(existingUser.getDisplayName())) {
                        existingUser.setDisplayName(displayName);
                        return userRepository.save(existingUser);
                    }
                    return existingUser;
                })
                .orElseGet(() -> userRepository.save(
                        new UserEntity(lookupPlatform, lookupExternalId, displayName, isCreatorIdentity)));
    }

    public Optional<UserEntity> findTargetUserInMessage(String message, UserEntity speaker) {
        if (message == null || message.isBlank()) return Optional.empty();
        String lower = message.toLowerCase();

        for (UserEntity u : userRepository.findAll()) {
            if (speaker != null && u.getId() != null && u.getId().equals(speaker.getId())) {
                continue;
            }

            String name = u.getDisplayName().toLowerCase();
            String id = u.getExternalId().toLowerCase();

            if (lower.contains(name) || lower.contains(id) ||
                    (u.isCreator() && (lower.contains("мабучи") || lower.contains("создател") || lower.contains("хозяин"))) ||
                    ("dina".equalsIgnoreCase(u.getExternalId()) && (lower.contains("дина") || lower.contains("дине") || lower.contains("дину")))) {
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }

    public void saveFact(UserEntity user, String fact, MemoryType type) {
        if (fact == null || fact.isBlank()) return;

        String cleanFact = fact.trim();
        String normalizedNew = normalize(cleanFact);

        boolean alreadyExists = memoryRepository.findByUser(user).stream()
                .anyMatch(m -> isSimilar(normalize(m.getFact()), normalizedNew));

        if (alreadyExists) {
            log.debug("[MemoryService] Похожий факт уже есть в БД, пропуск: {}", cleanFact);
            return;
        }

        MemoryType finalType = (type != null) ? type : MemoryType.FACT;
        MemoryEntity memory = new MemoryEntity(user, cleanFact, finalType);
        memoryRepository.save(memory);
        log.info("[MemoryService] Сохранено в БД [{}]: {}", finalType, cleanFact);
    }

    private String normalize(String text) {
        return text.toLowerCase()
                .replaceAll("[^a-zA-Zа-яА-Я0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isSimilar(String existing, String incoming) {
        if (existing.equals(incoming)) return true;
        if (existing.contains(incoming) || incoming.contains(existing)) return true;

        String[] wordsIncoming = incoming.split(" ");
        int matches = 0;
        for (String word : wordsIncoming) {
            if (word.length() >= 4 && existing.contains(word.substring(0, Math.min(word.length(), 6)))) {
                matches++;
            }
        }
        return matches > 0 && (double) matches / wordsIncoming.length >= 0.5;
    }

    public List<String> getUserFacts(UserEntity user) {
        return memoryRepository.findTop5ByUserAndTypeOrderByCreatedAtDesc(user, MemoryType.FACT).stream()
                .map(MemoryEntity::getFact)
                .toList();
    }

    public List<String> getUserBlunders(UserEntity user) {
        return memoryRepository.findTop5ByUserAndTypeOrderByCreatedAtDesc(user, MemoryType.BLUNDER).stream()
                .map(MemoryEntity::getFact)
                .toList();
    }

    public void saveShortTermMessage(String keyId, String role, String content) {
        String key = "chat:history:" + keyId;
        redisTemplate.opsForList().rightPush(key, role + ":::" + content);
        redisTemplate.opsForList().trim(key, -15, -1);
        // Раньше TTL вообще не истекал, а сейчас он продлевается при входящем сообщении и истекает при долгом перерыве
        redisTemplate.expire(key, SHORT_TERM_MEMORY_TTL);
    }

    public List<Message> getShortTermHistory(String keyId) {
        String key = "chat:history:" + keyId;
        List<String> rawMessages = redisTemplate.opsForList().range(key, 0, -1);
        List<Message> history = new ArrayList<>();

        if (rawMessages != null) {
            for (String raw : rawMessages) {
                String[] parts = raw.split(":::", 2);
                if (parts.length == 2) {
                    history.add(new Message(parts[0], parts[1]));
                }
            }
        }
        return history;
    }

    public void clearShortTermHistory(String keyId) {
        String key = "chat:history:" + keyId;
        redisTemplate.delete(key);
        log.info("[MemoryService] Краткосрочная память Redis очищена для: {}", keyId);
    }

    /**
     * Явный, осознанный сброс ВСЕЙ краткосрочной памяти — для всех пользователей
     * сразу, не только Создателя. Предназначен для ручного вызова (например,
     * кнопкой "Новый стрим" на дашборде), а не для автоматического запуска при
     * старте приложения — раньше именно это (жёсткий сброс "mabuchi" при каждом
     * старте) было источником проблемы: рестарт процесса посреди стрима стирал
     * контекст в неподходящий момент.
     *
     * Внимание: redisTemplate.keys(...) сканирует все ключи по паттерну — при
     * очень большом количестве ключей это может быть небыстрой операцией.
     * Для масштаба этого проекта (один локальный Redis, несколько активных
     * собеседников) это не проблема, но стоит иметь в виду, если база вырастет.
     */
    public void clearAllShortTermHistory() {
        Set<String> keys = redisTemplate.keys("chat:history:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("[MemoryService] Краткосрочная память Redis очищена для всех ({} ключей)", keys.size());
        }
    }
}