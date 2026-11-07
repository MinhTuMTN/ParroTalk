package com.parrotalk.backend.dto.practice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for Multiple Choice practice questions with distractors and explanation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MultipleChoiceQuestionDto {
    private UUID id;
    private String question;
    private String targetWord;
    private List<String> options;
    private String correctAnswer;
    private Integer correctIndex;
    private String phonetic;
    private String partOfSpeech;
    private String sentence;
    private String explanation;
    private String audioUrl;
}
