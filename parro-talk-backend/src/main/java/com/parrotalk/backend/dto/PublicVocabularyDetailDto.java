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
    
    // Clean string lists for general usage
    private List<String> definitions;
    private List<String> examples;
    private List<String> synonyms;
    private List<String> antonyms;
    private List<String> idioms;
    private List<String> collocations;
    private List<String> phrasalVerbs;
    private Boolean isSaved;

    // Structured senses matching modern Vocabulary Detail UI
    private List<SenseItemDto> senses;
    private List<PhrasalVerbItemDto> structuredPhrasalVerbs;
    private List<IdiomItemDto> structuredIdioms;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SenseItemDto {
        private String id;
        private String orderNumber;
        private String title;
        private String partOfSpeech;
        private String countability;
        private String cefrLevel;
        private String topicTag;
        private String definitionEn;
        private String definitionVn;
        private String usageContextNote;
        private String exampleEn;
        private String exampleVn;
        private List<String> synonyms;
        private List<String> collocations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhrasalVerbItemDto {
        private String phrasalVerb;
        private String register;
        private String definitionEn;
        private String definitionVn;
        private String exampleEn;
        private String exampleVn;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdiomItemDto {
        private String idiom;
        private String relatedSenseNote;
        private String definitionEn;
        private String definitionVn;
        private String exampleEn;
        private String exampleVn;
    }
}
