package org.example.boykisserai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.boykisserai.domain.constant.RelationshipType;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"platform", "externalId"})
})
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Платформа: "DISCORD", "LOCAL", "TWITCH"
    @Column(nullable = false)
    private String platform;

    // Уникальный ID на платформе (Discord ID, Twitch ID или "mabuchi_local")
    @Column(nullable = false)
    private String externalId;

    // Текущее отображаемое имя (Ник в Discord, ник на Twitch)
    @Column(nullable = false)
    private String displayName;

    // Уровень доверия (0 - 100). По умолчанию 50
    private int trustLevel = 50;

    // Флаг создателя (Да, я ЧСВ)
    private boolean isCreator = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    public UserEntity(String platform, String externalId, String displayName, boolean isCreator) {
        this.platform = platform;
        this.externalId = externalId;
        this.displayName = displayName;
        this.isCreator = isCreator;
        this.trustLevel = isCreator ? 100 : 50;
    }
}