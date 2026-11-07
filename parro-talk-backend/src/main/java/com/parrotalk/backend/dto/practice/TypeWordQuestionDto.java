package com.parrotalk.backend.dto.practice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for Type the Word (active recall practice with hints).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TypeWordQuestionDto {
    private UUID id;
    private String targetWord;
    private String promptDefinition;
    private String definitionEn;
    private String definitionVn;
    private String firstLetter;
    private Integer characterCount;
    private String partOfSpeech;
    private String phonetic;
    private String audioUrl;
    private String exampleWithMask;
    private String exampleSentence;
}
