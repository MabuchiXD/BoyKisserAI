package org.example.boykisserai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.boykisserai.domain.constant.MemoryType;

import java.time.LocalDateTime;

@Entity
@Table(name = "memories")
@Getter
@Setter
@NoArgsConstructor
public class MemoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 1000)
    private String fact;

    // Тип памяти: FACT (обычный факт) или BLUNDER (компромат/косяк)
    @Enumerated(EnumType.STRING)
    private MemoryType type = MemoryType.FACT;

    private LocalDateTime createdAt = LocalDateTime.now();

    public MemoryEntity(UserEntity user, String fact, MemoryType type) {
        this.user = user;
        this.fact = fact;
        this.type = type != null ? type : MemoryType.FACT;
    }
}