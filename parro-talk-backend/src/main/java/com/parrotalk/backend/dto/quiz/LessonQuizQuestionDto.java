package com.parrotalk.backend.dto.quiz;

import com.parrotalk.backend.constant.QuizQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonQuizQuestionDto {
    private UUID questionId;
    private Integer displayOrder;
    private QuizQuestionType questionType;
    private String questionText;
    private List<String> options;
    private boolean answered;
    private QuizAnswerResultDto answerResult;
}
