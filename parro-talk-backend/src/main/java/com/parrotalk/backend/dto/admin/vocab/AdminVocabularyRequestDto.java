package com.parrotalk.backend.dto.admin.vocab;

import jakarta.validation.constraints.NotBlank;
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
public class AdminVocabularyRequestDto {
    @NotBlank(message = "Word is required")
    private String word;
    private String commonMeaningVn;
    private List<AdminVocabularyPronunciationRequestDto> pronunciations;
    private List<AdminVocabularySenseRequestDto> senses;
    private List<AdminExpressionRequestDto> idioms;
    private List<AdminExpressionRequestDto> phrasalVerbs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminVocabularyPronunciationRequestDto {
        private String region;
        private String ipa;
        private String audioUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminVocabularySenseRequestDto {
        private String partOfSpeech;
        private String cefrLevel;
        private Boolean isAiCefr;
        private String definitionEn;
        private String definitionVn;
        private String exampleEn;
        private String exampleVn;
        /** Preferred: single canonical topic UUID */
        private UUID topicId;
        /** Alternative: canonical topic name string */
        private String topic;
        /** Legacy: list of topic names – only topics.get(0) is used */
        private List<String> topics;
        private List<String> synonyms;
        private List<String> antonyms;
        private List<String> collocations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminExpressionRequestDto {
        private String text;
        private String meaningEn;
        private String meaningVn;
    }
}
