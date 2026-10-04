package com.parrotalk.backend.dto.admin.vocab;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminVocabularyAiRefineRequestDto {
    private String word;
    private List<SenseDto> senses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SenseDto {
        private String partOfSpeech;
        private String cefrLevel;
        private String definitionEn;
        private String definitionVn;
        private String exampleEn;
        private String exampleVn;
        private List<String> synonyms;
        private List<String> antonyms;
        private List<String> collocations;
    }
}
