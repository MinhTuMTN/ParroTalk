package com.parrotalk.backend.dto.practice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for Fill in the Blank practice questions (Hybrid AI flow).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FillBlankQuestionDto {
    private UUID id;
    private String word;
    private String targetForm;
    private String sentenceWithBlank;
    private String completedSentence;
    private String sentenceVn;
    private String grammarContextTip;
    private String explanation;
    private String source; // "DATABASE" | "AI_GENERATED" | "DICTIONARY_FALLBACK"
    private String partOfSpeech;
    private String cefrLevel;
    private String definitionEn;
    private String definitionVn;
    private String audioUrl;
    private List<String> acceptableVariants;
}
