package com.parrotalk.backend.dto.quiz;

import com.parrotalk.backend.constant.QuizAttemptStatus;
import com.parrotalk.backend.constant.QuizMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonQuizAttemptResponse {
    private UUID attemptId;
    private UUID lessonId;
    private UUID quizSetId;
    private QuizMode mode;
    private QuizAttemptStatus status;
    private Integer totalQuestions;
    private Integer answeredCount;
    private Integer correctCount;
    private Integer partialCount;
    private Integer overallScore;
    private Integer regenerateCooldownSeconds;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<LessonQuizQuestionDto> questions;
}
