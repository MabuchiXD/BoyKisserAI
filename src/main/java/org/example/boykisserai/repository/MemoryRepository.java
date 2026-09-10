package org.example.boykisserai.repository;

import org.example.boykisserai.domain.constant.MemoryType;
import org.example.boykisserai.domain.entity.MemoryEntity;
import org.example.boykisserai.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemoryRepository extends JpaRepository<MemoryEntity, Long> {

    // Найти ВСЕ воспоминания пользователя (нужно для проверки на дубликаты)
    List<MemoryEntity> findByUser(UserEntity user);

    // Найти TOP-5 воспоминаний конкретного типа (FACT или BLUNDER)
    List<MemoryEntity> findTop5ByUserAndTypeOrderByCreatedAtDesc(UserEntity user, MemoryType type);
}