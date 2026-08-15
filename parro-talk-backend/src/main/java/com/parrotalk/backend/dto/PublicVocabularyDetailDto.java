package com.parrotalk.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO for public vocabulary detail.
 * 
 * @author MinhTuMTN
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicVocabularyDetailDto {
    private java.util.UUID id;
    private String word;
    private String commonMeaningVi;
    private String partOfSpeech;
    private String phonetic;
    private String cefrLevel;
    private String topic;
    private String audioUsUrl;
    private String audioUkUrl;
    
    private List<String> definitions;
    private List<String> examples;
    private List<String> synonyms;
    private List<String> antonyms;
    private List<String> idioms;
    private List<String> collocations;
    private List<String> phrasalVerbs;
    private Boolean isSaved;
}
