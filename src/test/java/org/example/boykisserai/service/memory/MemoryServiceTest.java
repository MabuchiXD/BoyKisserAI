package org.example.boykisserai.service.memory;

import org.example.boykisserai.config.AppProperties;
import org.example.boykisserai.domain.constant.MemoryType;
import org.example.boykisserai.domain.entity.MemoryEntity;
import org.example.boykisserai.domain.entity.UserEntity;
import org.example.boykisserai.domain.model.Message;
import org.example.boykisserai.repository.MemoryRepository;
import org.example.boykisserai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MemoryRepository memoryRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    private static final String CREATOR_DISCORD_ID = "349937009399300096";

    private MemoryService memoryService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getBot().setCreatorDiscordId(CREATOR_DISCORD_ID);

        memoryService = new MemoryService(userRepository, memoryRepository, redisTemplate, props);
    }

    @Test
    @DisplayName("РАЗДЕЛЕНИЕ ТИПОВ: Должен сохранять и FACT, и BLUNDER с правильными типами")
    void shouldSaveDifferentMemoryTypesCorrectly() {
        UserEntity user = new UserEntity("LOCAL", "mabuchi", "Мабучи", true);

        when(memoryRepository.findByUser(user)).thenReturn(List.of());

        memoryService.saveFact(user, "Работает Java-разработчиком", MemoryType.FACT);
        memoryService.saveFact(user, "Случайно снес базу данных на проде", MemoryType.BLUNDER);

        ArgumentCaptor<MemoryEntity> captor = ArgumentCaptor.forClass(MemoryEntity.class);
        verify(memoryRepository, times(2)).save(captor.capture());

        List<MemoryEntity> saved = captor.getAllValues();
        assertEquals(MemoryType.FACT, saved.get(0).getType());
        assertEquals("Работает Java-разработчиком", saved.get(0).getFact());

        assertEquals(MemoryType.BLUNDER, saved.get(1).getType());
        assertEquals("Случайно снес базу данных на проде", saved.get(1).getFact());
    }

    @Test
    @DisplayName("ДЕДУПЛИКАЦИЯ: Не должен сохранять дубликаты одинаковых или похожих фактов")
    void shouldDeduplicateSimilarFacts() {
        UserEntity user = new UserEntity("LOCAL", "mabuchi", "Мабучи", true);
        MemoryEntity existing = new MemoryEntity(user, "Любит онигири с тунцом", MemoryType.FACT);

        when(memoryRepository.findByUser(user)).thenReturn(List.of(existing));

        memoryService.saveFact(user, "Любит онигири", MemoryType.FACT);

        verify(memoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("ВЫБОРКА TOP-5: Должен раздельно возвращать факты и компромат из базы")
    void shouldRetrieveTop5FactsAndBlundersSeparately() {
        UserEntity user = new UserEntity("DISCORD", "123", "AsataEnot", false);

        MemoryEntity fact = new MemoryEntity(user, "Любит играть в Доту", MemoryType.FACT);
        MemoryEntity blunder = new MemoryEntity(user, "Слил мид со счетом 0-10", MemoryType.BLUNDER);

        when(memoryRepository.findTop5ByUserAndTypeOrderByCreatedAtDesc(user, MemoryType.FACT))
                .thenReturn(List.of(fact));
        when(memoryRepository.findTop5ByUserAndTypeOrderByCreatedAtDesc(user, MemoryType.BLUNDER))
                .thenReturn(List.of(blunder));

        List<String> facts = memoryService.getUserFacts(user);
        List<String> blunders = memoryService.getUserBlunders(user);

        assertEquals(1, facts.size());
        assertEquals("Любит играть в Доту", facts.get(0));

        assertEquals(1, blunders.size());
        assertEquals("Слил мид со счетом 0-10", blunders.get(0));
    }

    @Test
    @DisplayName("АКТИВНОСТЬ: Сохранение сообщения должно продлевать TTL ключа на 4 часа")
    void shouldPersistMessageAndRefreshTtlOnActivity() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        memoryService.saveShortTermMessage("mabuchi", "user", "привет!");

        verify(listOperations).rightPush("chat:history:mabuchi", "user:::привет!");
        verify(listOperations).trim("chat:history:mabuchi", -15, -1);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(redisTemplate).expire(eq("chat:history:mabuchi"), ttlCaptor.capture());
        assertEquals(Duration.ofHours(4), ttlCaptor.getValue());
    }

    @Test
    @DisplayName("АКТИВНОСТЬ: Каждое новое сообщение продлевает TTL заново, не только первое")
    void shouldRefreshTtlOnEveryMessageNotJustFirst() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        memoryService.saveShortTermMessage("mabuchi", "user", "привет!");
        memoryService.saveShortTermMessage("mabuchi", "assistant", "здарова!");
        memoryService.saveShortTermMessage("mabuchi", "user", "как дела?");

        // TTL должен обновляться на КАЖДЫЙ вызов — именно это и делает диалог
        // "неистекающим" на практике, пока сообщения продолжают приходить.
        verify(redisTemplate, times(3)).expire(eq("chat:history:mabuchi"), eq(Duration.ofHours(4)));
    }

    @Test
    @DisplayName("НЕАКТИВНОСТЬ: Если ключ истёк по TTL (Redis сам его удалил), история должна вернуться пустой, а не упасть")
    void shouldReturnEmptyHistoryWhenKeyExpiredDueToInactivity() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        // Симулируем реальный эффект TTL: после 4+ часов тишины Redis удаляет
        // ключ сам — range() на несуществующий ключ вернёт null.
        when(listOperations.range("chat:history:mabuchi", 0, -1)).thenReturn(null);

        List<Message> history = memoryService.getShortTermHistory("mabuchi");

        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    @DisplayName("НЕАКТИВНОСТЬ: Если ключ давно истёк и не пересоздавался, старые сообщения не восстанавливаются из ниоткуда")
    void shouldNotFabricateHistoryAfterExpiry() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("chat:history:mabuchi", 0, -1)).thenReturn(List.of());

        List<Message> history = memoryService.getShortTermHistory("mabuchi");

        assertTrue(history.isEmpty());
    }

    @Test
    @DisplayName("ЧТЕНИЕ ИСТОРИИ: Должен корректно разбирать формат role:::content обратно в сообщения")
    void shouldParseRawRedisEntriesBackIntoMessages() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("chat:history:mabuchi", 0, -1)).thenReturn(
                List.of("user:::привет!", "assistant:::здарова, братан!"));

        List<Message> history = memoryService.getShortTermHistory("mabuchi");

        assertEquals(2, history.size());
        assertEquals("user", history.get(0).role());
        assertEquals("привет!", history.get(0).content());
        assertEquals("assistant", history.get(1).role());
        assertEquals("здарова, братан!", history.get(1).content());
    }

    @Test
    @DisplayName("СБРОС ВСЕХ: clearAllShortTermHistory должен удалить все ключи chat:history:*")
    void shouldClearAllShortTermHistoryKeys() {
        Set<String> keys = Set.of("chat:history:mabuchi", "chat:history:112233");
        when(redisTemplate.keys("chat:history:*")).thenReturn(keys);

        memoryService.clearAllShortTermHistory();

        verify(redisTemplate).delete(keys);
    }

    @Test
    @DisplayName("СБРОС ВСЕХ: Не должен звать delete(), если подходящих ключей вообще нет")
    void shouldNotCallDeleteWhenNoShortTermKeysExist() {
        when(redisTemplate.keys("chat:history:*")).thenReturn(Set.of());

        memoryService.clearAllShortTermHistory();

        verify(redisTemplate, never()).delete(anySet());
    }

    @Test
    @DisplayName("СОЗДАНИЕ ПОЛЬЗОВАТЕЛЯ: Должен создать нового пользователя, если такого ещё нет в БД")
    void shouldCreateNewUserWhenNotFound() {
        when(userRepository.findByPlatformAndExternalId("DISCORD", "112233")).thenReturn(Optional.empty());
        UserEntity saved = new UserEntity("DISCORD", "112233", "Gleb", false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(saved);

        UserEntity result = memoryService.getOrCreateUser("DISCORD", "112233", "Gleb");

        assertEquals("Gleb", result.getDisplayName());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("ИДЕНТИЧНОСТЬ СОЗДАТЕЛЯ: Сообщение от Создателя через Discord должно попадать в ту же запись, что и через LOCAL/микрофон")
    void shouldCanonicalizeCreatorIdentityAcrossPlatforms() {
        UserEntity canonicalCreator = new UserEntity("LOCAL", "mabuchi", "Мабучи", true);
        when(userRepository.findByPlatformAndExternalId("LOCAL", "mabuchi"))
                .thenReturn(Optional.of(canonicalCreator));

        // Пришло сообщение от Создателя, но с платформы DISCORD и его реальным
        // discord id — регрессионный тест на баг, который сам
        // нашёл вручную: раньше это создавало ВТОРОГО, независимого пользователя
        // с пустой памятью вместо той же самой канонической записи.
        UserEntity result = memoryService.getOrCreateUser("DISCORD", CREATOR_DISCORD_ID, "Мабучи");

        assertEquals(canonicalCreator, result);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("ИДЕНТИЧНОСТЬ ОБЫЧНОГО ПОЛЬЗОВАТЕЛЯ: Разные платформы у НЕ-Создателя остаются разными записями")
    void shouldKeepSeparateIdentitiesForRegularUserAcrossPlatforms() {
        when(userRepository.findByPlatformAndExternalId("TWITCH", "998877")).thenReturn(Optional.empty());
        UserEntity saved = new UserEntity("TWITCH", "998877", "Sanya228", false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(saved);

        // Обычный зритель — платформа и externalId используются как есть,
        // Два разных человека с похожим ником на разных платформах не должны случайно превратиться в одного
        memoryService.getOrCreateUser("TWITCH", "998877", "Sanya228");

        verify(userRepository).findByPlatformAndExternalId("TWITCH", "998877");
        verify(userRepository, never()).findByPlatformAndExternalId("LOCAL", "mabuchi");
    }
}