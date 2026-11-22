package com.parrotalk.backend.repository;

import com.parrotalk.backend.constant.QuizAttemptStatus;
import com.parrotalk.backend.entity.UserLessonQuizAttempt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserLessonQuizAttemptRepository extends JpaRepository<UserLessonQuizAttempt, UUID> {

    Optional<UserLessonQuizAttempt> findByIdAndUserIdAndLessonId(UUID id, UUID userId, UUID lessonId);

    Optional<UserLessonQuizAttempt> findFirstByUserIdAndLessonIdAndStatusOrderByStartedAtDesc(
            UUID userId,
            UUID lessonId,
            QuizAttemptStatus status);

    List<UserLessonQuizAttempt> findByUserIdAndLessonIdAndStatusOrderByCompletedAtDesc(
            UUID userId,
            UUID lessonId,
            QuizAttemptStatus status,
            Pageable pageable);

    List<UserLessonQuizAttempt> findByUserIdAndLessonIdOrderByStartedAtDesc(
            UUID userId,
            UUID lessonId,
            Pageable pageable);
}
