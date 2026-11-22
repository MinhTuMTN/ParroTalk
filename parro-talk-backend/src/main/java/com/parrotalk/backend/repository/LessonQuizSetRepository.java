package com.parrotalk.backend.repository;

import com.parrotalk.backend.constant.QuizMode;
import com.parrotalk.backend.entity.LessonQuizSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonQuizSetRepository extends JpaRepository<LessonQuizSet, UUID> {

    Optional<LessonQuizSet> findFirstByLessonIdAndModeAndActiveTrueOrderByGeneratedAtDesc(
            UUID lessonId,
            QuizMode mode);

    List<LessonQuizSet> findByLessonIdAndActiveTrue(UUID lessonId);

    Optional<LessonQuizSet> findFirstByLessonIdAndModeOrderByGeneratedAtDesc(
            UUID lessonId,
            QuizMode mode);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE LessonQuizSet s SET s.active = false WHERE s.lesson.id = :lessonId AND s.mode = :mode AND s.active = true")
    int deactivateActiveSets(@Param("lessonId") UUID lessonId, @Param("mode") QuizMode mode);
}
