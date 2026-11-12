package com.parrotalk.backend.repository;

import com.parrotalk.backend.constant.DifficultyScale;
import com.parrotalk.backend.entity.WritingSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WritingSessionRepository extends JpaRepository<WritingSession, UUID> {

    Page<WritingSession> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"generatedItems", "generatedItems.linkedVocabulary", "generatedItems.linkedSense"})
    Optional<WritingSession> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT s.id FROM WritingSession s
            WHERE s.user.id = :userId
              AND LOWER(s.inputText) = LOWER(:inputText)
              AND s.scaleType = :scaleType
              AND s.targetLevel = :targetLevel
              AND s.mode = :mode
            ORDER BY s.createdAt DESC
            LIMIT 1
            """)
    Optional<UUID> findLatestSessionIdByInputAndLevel(
            @Param("userId") UUID userId,
            @Param("inputText") String inputText,
            @Param("scaleType") DifficultyScale scaleType,
            @Param("targetLevel") String targetLevel,
            @Param("mode") com.parrotalk.backend.constant.WritingMode mode);

    @Modifying
    @Query("UPDATE WritingSession s SET s.mode = :mode WHERE s.id = :id")
    void updateSessionMode(@Param("id") UUID id, @Param("mode") com.parrotalk.backend.constant.WritingMode mode);
}

