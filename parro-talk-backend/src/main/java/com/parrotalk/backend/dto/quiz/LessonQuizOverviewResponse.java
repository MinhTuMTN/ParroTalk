package com.parrotalk.backend.dto.quiz;

import com.parrotalk.backend.constant.QuizMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonQuizOverviewResponse {
    private UUID lessonId;
    private Integer recommendedQuestionCount;
    private LessonQuizAttemptSummaryDto activeAttempt;
    private List<QuizMode> cachedModes;
    private Map<QuizMode, Integer> regenerateCooldownSeconds;
    private List<LessonQuizAttemptSummaryDto> history;
}
