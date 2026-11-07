package com.parrotalk.backend.dto.practice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for Learn Words (interactive 3D flashcards with reversible EN ↔ VI translations).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LearnWordDto {
    private UUID id;
    private String word;
    private String phonetic;
    private String phoneticUs;
    private String phoneticUk;
    private String audioUrl;
    private String audioUrlUs;
    private String audioUrlUk;
    private String partOfSpeech;
    private String cefrLevel;
    private String topic;
    private String definitionEn;
    private String definitionVn;
    private String exampleEn;
    private String exampleVn;
    private List<String> synonyms;
    private List<String> antonyms;
    private List<String> collocations;
}
