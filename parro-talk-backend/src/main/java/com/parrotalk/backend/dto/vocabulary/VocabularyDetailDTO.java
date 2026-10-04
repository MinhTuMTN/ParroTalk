package com.parrotalk.backend.dto.vocabulary;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VocabularyDetailDTO {
    private String word;
    private String commonMeaningVn;
    private List<String> topics;
    private List<PronunciationDTO> pronunciations;
    private List<SenseDTO> senses;
    private List<IdiomDTO> idioms;
    private List<PhrasalVerbDTO> phrasalVerbs;

    @Data
    @Builder
    public static class PronunciationDTO {
        private String accent;
        private String ipa;
        private String audioUrl;
    }

    @Data
    @Builder
    public static class SenseDTO {
        private String partOfSpeech;
        private String cefrLevel;
        private boolean isAiCefr;
        private String definitionEn;
        private String definitionVn;
        private String exampleEn;
        private String exampleVn;
        /** Canonical topic name for this sense */
        private String topic;
        /** Canonical topic UUID for this sense */
        private java.util.UUID topicId;
        private List<SynonymDTO> synonyms;
        private List<AntonymDTO> antonyms;
        private List<CollocationDTO> collocations;
    }

    @Data
    @Builder
    public static class SynonymDTO {
        private String synonymWord;
        private String synonymVn;
    }

    @Data
    @Builder
    public static class AntonymDTO {
        private String antonymWord;
        private String antonymVn;
    }

    @Data
    @Builder
    public static class CollocationDTO {
        private String collocationWord;
        private String collocationVn;
    }

    @Data
    @Builder
    public static class IdiomDTO {
        private String phrase;
        private String definition;
        private String definitionVn;
        private String example;
        private String exampleVn;
    }

    @Data
    @Builder
    public static class PhrasalVerbDTO {
        private String phrase;
        private String definition;
        private String definitionVn;
        private String example;
        private String exampleVn;
    }
}
