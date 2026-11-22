package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.LessonQuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonQuizQuestionRepository extends JpaRepository<LessonQuizQuestion, UUID> {

    List<LessonQuizQuestion> findByQuizSetIdOrderByDisplayOrderAsc(UUID quizSetId);

    Optional<LessonQuizQuestion> findByIdAndQuizSetId(UUID id, UUID quizSetId);
}
