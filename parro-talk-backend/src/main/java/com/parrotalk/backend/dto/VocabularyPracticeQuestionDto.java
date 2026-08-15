package com.parrotalk.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO for vocabulary practice question.
 * 
 * @author MinhTuMTN
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyPracticeQuestionDto {
    private java.util.UUID id;
    private String questionType;
    private String prompt;
    private List<String> options;
    private String correctAnswer;
    private String wordHint;
    private Integer charCount;
    private String audioUrl;
    private String explanation;
    private java.util.UUID wordId;
}
