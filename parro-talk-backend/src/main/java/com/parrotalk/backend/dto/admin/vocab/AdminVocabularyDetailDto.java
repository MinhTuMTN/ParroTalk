package com.parrotalk.backend.dto.admin.vocab;

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
public class AdminVocabularyDetailDto {
    private UUID id;
    private String word;
    private String commonMeaningVn;
    private List<AdminVocabularyPronunciationDto> pronunciations;
    private List<AdminVocabularySenseDto> senses;
    private List<AdminExpressionDto> idioms;
    private List<AdminExpressionDto> phrasalVerbs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminVocabularyPronunciationDto {
        private UUID id;
        private String region;
        private String ipa;
        private String audioUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminVocabularySenseDto {
        private UUID id;
        private String partOfSpeech;
        private String cefrLevel;
        private Boolean isAiCefr;
        private String definitionEn;
        private String definitionVn;
        private String exampleEn;
        private String exampleVn;
        /** Single canonical topic name (null if unset) */
        private String topic;
        /** Single canonical topic UUID (null if unset) */
        private UUID topicId;
        /** Backward-compat list – always contains at most one element */
        private List<String> topics;
        private List<String> synonyms;
        private List<String> antonyms;
        private List<String> collocations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminExpressionDto {
        private UUID id;
        private String text;
        private String meaningEn;
        private String meaningVn;
    }
}
