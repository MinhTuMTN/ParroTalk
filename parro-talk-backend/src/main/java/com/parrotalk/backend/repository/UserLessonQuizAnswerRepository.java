package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.UserLessonQuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserLessonQuizAnswerRepository extends JpaRepository<UserLessonQuizAnswer, UUID> {

    List<UserLessonQuizAnswer> findByAttemptId(UUID attemptId);

    Optional<UserLessonQuizAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);

    boolean existsByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);
}
