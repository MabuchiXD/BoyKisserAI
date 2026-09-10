package org.example.boykisserai.service.relationship;

import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.domain.constant.RelationshipType;
import org.example.boykisserai.domain.entity.UserEntity;
import org.example.boykisserai.repository.UserRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RelationshipService {

    private final UserRepository userRepository;

    public RelationshipService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public RelationshipType getRelationshipType(UserEntity user) {
        if (user.isCreator()) {
            return RelationshipType.CREATOR;
        }

        int trust = user.getTrustLevel();
        if (trust >= 90) return RelationshipType.BELOVED;
        if (trust >= 70) return RelationshipType.FRIEND;
        if (trust >= 40) return RelationshipType.NEUTRAL;
        if (trust >= 15) return RelationshipType.SUSPICIOUS;
        return RelationshipType.HOSTILE;
    }

    public void adjustTrust(UserEntity user, int delta) {
        if (user.isCreator()) return;

        int newTrust = Math.max(0, Math.min(100, user.getTrustLevel() + delta));
        user.setTrustLevel(newTrust);
        userRepository.save(user);
        log.info("[Relationship] Доверие пользователя {} изменилось: {}/100 ({})",
                user.getDisplayName(), newTrust, getRelationshipType(user).getTitle());
    }
}