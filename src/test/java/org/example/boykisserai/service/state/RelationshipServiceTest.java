package org.example.boykisserai.service.state;

import org.example.boykisserai.domain.constant.RelationshipType;
import org.example.boykisserai.domain.entity.UserEntity;
import org.example.boykisserai.repository.UserRepository;
import org.example.boykisserai.service.relationship.RelationshipService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RelationshipServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RelationshipService relationshipService;

    @Test
    @DisplayName("Создатель всегда имеет статус CREATOR и иммунитет к снижению доверия")
    void creatorShouldAlwaysHaveCreatorStatusAndImmunity() {
        UserEntity creator = new UserEntity("LOCAL", "mabuchi", "Мабучи", true);

        assertEquals(RelationshipType.CREATOR, relationshipService.getRelationshipType(creator));

        // Пытаемся отнять 50 доверия у Создателя
        relationshipService.adjustTrust(creator, -50);

        assertEquals(100, creator.getTrustLevel(), "Уровень доверия Создателя не должен падать ниже 100");
    }

    @Test
    @DisplayName("Корректный расчет статусов отношений по шкале доверия")
    void shouldCalculateCorrectRelationshipTiers() {
        UserEntity user = new UserEntity("DISCORD", "123", "User", false);

        user.setTrustLevel(95);
        assertEquals(RelationshipType.BELOVED, relationshipService.getRelationshipType(user));

        user.setTrustLevel(75);
        assertEquals(RelationshipType.FRIEND, relationshipService.getRelationshipType(user));

        user.setTrustLevel(50);
        assertEquals(RelationshipType.NEUTRAL, relationshipService.getRelationshipType(user));

        user.setTrustLevel(25);
        assertEquals(RelationshipType.SUSPICIOUS, relationshipService.getRelationshipType(user));

        user.setTrustLevel(5);
        assertEquals(RelationshipType.HOSTILE, relationshipService.getRelationshipType(user));
    }

    @Test
    @DisplayName("Уровень доверия не должен превышать 100 и падать ниже 0")
    void shouldClampTrustBetweenZeroAndHundred() {
        UserEntity user = new UserEntity("DISCORD", "123", "User", false);
        user.setTrustLevel(90);

        relationshipService.adjustTrust(user, 30);
        assertEquals(100, user.getTrustLevel());

        relationshipService.adjustTrust(user, -150);
        assertEquals(0, user.getTrustLevel());

        verify(userRepository, org.mockito.Mockito.atLeastOnce()).save(user);
    }
}