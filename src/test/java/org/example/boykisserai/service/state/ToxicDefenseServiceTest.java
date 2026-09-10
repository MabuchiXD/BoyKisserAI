package org.example.boykisserai.service.state;

import org.example.boykisserai.domain.entity.UserEntity;
import org.example.boykisserai.service.relationship.RelationshipService;
import org.example.boykisserai.service.relationship.ToxicDefenseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToxicDefenseServiceTest {

    @Mock
    private RelationshipService relationshipService;

    @InjectMocks
    private ToxicDefenseService toxicDefenseService;

    @Test
    @DisplayName("СЦЕНАРИЙ ИСКУПЛЕНИЯ: Асата токсичит -> попадает в игнор -> извиняется -> получает +15 доверия и разбан!")
    void shouldHandleFullTrollingAndRedemptionCycle() {
        UserEntity asata = new UserEntity("DISCORD", "349937009399300096", "AsataEnot", false);
        asata.setTrustLevel(0); // Доверие на нуле после серии оскорблений

        // 1. Асата пишет 1-й и 2-й наезд при нулевом доверии -> бот пока еще отвечает (генерит контент!)
        assertEquals(ToxicDefenseService.DefenseState.NORMAL, toxicDefenseService.evaluate(asata, "ты лох"));
        assertEquals(ToxicDefenseService.DefenseState.NORMAL, toxicDefenseService.evaluate(asata, "пошел нахуй"));

        // 2. На 3-й наезд подряд срабатывает финальный рейдж-кик TRIGGER_KICK!
        assertEquals(ToxicDefenseService.DefenseState.TRIGGER_KICK, toxicDefenseService.evaluate(asata, "заткнись урод"));

        // Блокируем в игнор
        toxicDefenseService.lockUserInIgnore(asata.getExternalId());
        assertTrue(toxicDefenseService.isUserIgnored(asata.getExternalId()), "Асата должен быть в игноре");

        // 3. Асата пытается писать обычные фразы в бане -> бот ЖЕЛЕЗНО ИГНОРИРУЕТ (SHOULD_IGNORE)
        assertEquals(ToxicDefenseService.DefenseState.SHOULD_IGNORE, toxicDefenseService.evaluate(asata, "эй ты тут?"));
        assertEquals(ToxicDefenseService.DefenseState.SHOULD_IGNORE, toxicDefenseService.evaluate(asata, "ответь мне"));

        // 4. АСАТА ИЗВИНЯЕТСЯ: "Блин, кот, извини меня, пожалуйста, я зря быканул"
        var apologyState = toxicDefenseService.evaluate(asata, "кот, извини меня пожалуйста, я зря быканул");

        // 5. ПРОВЕРЯЕМ РАЗБАН:
        assertEquals(ToxicDefenseService.DefenseState.APOLOGY_ACCEPTED, apologyState, "Извинение должно быть принято!");
        assertFalse(toxicDefenseService.isUserIgnored(asata.getExternalId()), "Бан должен быть снят!");

        // Проверяем, что за искреннее извинение начислено +15 к доверию!
        verify(relationshipService, times(1)).adjustTrust(asata, 15);
    }

    @Test
    @DisplayName("ИММУНИТЕТ: Создателя (Мабучи) нельзя заблокировать в игнор ни при каких условиях")
    void creatorShouldNeverBeIgnored() {
        UserEntity creator = new UserEntity("LOCAL", "mabuchi", "Мабучи", true);
        creator.setTrustLevel(0);

        for (int i = 0; i < 10; i++) {
            assertEquals(ToxicDefenseService.DefenseState.NORMAL, toxicDefenseService.evaluate(creator, "заткнись"));
        }
        assertFalse(toxicDefenseService.isUserIgnored(creator.getExternalId()));
    }
}