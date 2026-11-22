package com.parrotalk.backend.dto.quiz;

import com.parrotalk.backend.constant.QuizGradingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswerResultDto {
    private UUID questionId;
    private String userAnswer;
    private Integer selectedIndex;
    private QuizGradingStatus status;
    private Integer score;
    private String feedback;
    private String modelAnswer;
    private Integer correctIndex;
    private String explanationVn;
    private UUID evidenceSegmentId;
    private String evidenceText;
    private Double evidenceStartTime;
    private Double evidenceEndTime;
}
