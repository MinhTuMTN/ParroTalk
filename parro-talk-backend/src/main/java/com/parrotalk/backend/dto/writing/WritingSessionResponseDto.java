package com.parrotalk.backend.dto.writing;

import com.parrotalk.backend.constant.DifficultyScale;
import com.parrotalk.backend.constant.WritingItemType;
import com.parrotalk.backend.constant.WritingItemValidationStatus;
import com.parrotalk.backend.constant.WritingMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WritingSessionResponseDto {

    private UUID id;
    private String inputText;
    private String inputLanguage;
    private WritingMode mode;
    private DifficultyScale scaleType;
    private String targetLevel;
    private String coreIdeaSummaryVn;
    private String combinationGuideVn;
    private String userDraft;
    private GrammarValidationResultDto draftValidation;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<WritingLexicalItemDto> vocabulary = new ArrayList<>();

    @Builder.Default
    private List<WritingLexicalItemDto> expressions = new ArrayList<>();

    @Builder.Default
    private List<WritingLexicalItemDto> collocations = new ArrayList<>();

    @Builder.Default
    private List<WritingPatternItemDto> sentencePatterns = new ArrayList<>();

    @Builder.Default
    private List<WritingCompleteSentenceDto> completeSentences = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WritingLexicalItemDto {
        private UUID id;
        private WritingItemType itemType;
        private String text;
        private String normalizedText;
        private String partOfSpeech;
        private String cefrLevel;
        private String meaningVn;
        private String explanationVn;
        private String usageContextVn;
        private String exampleEn;
        private String exampleVn;
        private boolean inDictionary;
        private UUID linkedVocabularyId;
        private UUID linkedSenseId;
        private String dictionaryWord;
        private WritingItemValidationStatus validationStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WritingPatternItemDto {
        private UUID id;
        private String pattern;
        private String meaningVn;
        private String explanationVn;
        private String usageContextVn;
        private WritingItemValidationStatus validationStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WritingCompleteSentenceDto {
        private UUID id;
        private String tone;
        private String sentenceEn;
        private String translationVn;
        private String grammarBreakdownVn;
        private String whyItWorksVn;
        private WritingItemValidationStatus validationStatus;
        private GrammarValidationResultDto grammarCheck;
    }
}

