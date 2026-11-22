package com.parrotalk.backend.dto.quiz;

import com.parrotalk.backend.constant.QuizAttemptStatus;
import com.parrotalk.backend.constant.QuizMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonQuizAttemptSummaryDto {
    private UUID attemptId;
    private UUID quizSetId;
    private QuizMode mode;
    private QuizAttemptStatus status;
    private Integer totalQuestions;
    private Integer answeredCount;
    private Integer correctCount;
    private Integer partialCount;
    private Integer overallScore;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
