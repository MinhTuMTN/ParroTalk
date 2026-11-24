package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, UUID> {

    /**
     * Atomically insert or update user lesson completion progress in PostgreSQL.
     *
     * @param userId   User id
     * @param newScore Score of the newly completed lesson
     * @param now      Activity timestamp
     * @return Number of affected rows
     */
    @Modifying
    @Query(value = """
            INSERT INTO user_progress (
                user_id,
                total_lessons_completed,
                total_score,
                avg_score,
                last_activity_date,
                created_at,
                updated_at,
                is_deleted
            ) VALUES (
                :userId,
                1,
                :newScore,
                :newScore,
                :now,
                :now,
                :now,
                false
            )
            ON CONFLICT (user_id) DO UPDATE SET
                total_lessons_completed = CASE
                    WHEN user_progress.is_deleted = true THEN 1
                    ELSE user_progress.total_lessons_completed + 1
                END,
                total_score = CASE
                    WHEN user_progress.is_deleted = true THEN EXCLUDED.total_score
                    ELSE user_progress.total_score + EXCLUDED.total_score
                END,
                avg_score = CASE
                    WHEN user_progress.is_deleted = true THEN EXCLUDED.avg_score
                    ELSE (user_progress.total_score + EXCLUDED.total_score) / (user_progress.total_lessons_completed + 1)
                END,
                last_activity_date = EXCLUDED.last_activity_date,
                updated_at = EXCLUDED.updated_at,
                is_deleted = false
            """, nativeQuery = true)
    int upsertLessonCompletion(
            @Param("userId") UUID userId,
            @Param("newScore") double newScore,
            @Param("now") LocalDateTime now);
}
