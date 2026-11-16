package com.parrotalk.backend.dto.vocablist;

import java.util.List;
import java.util.UUID;

public record VocabularyListItemDto(
        UUID id,
        UUID vocabularyId,
        String rawWord,
        String normalizedWord,
        String commonMeaningVn,
        String dataSource,
        String sourceOption,
        String crawlStatus,
        String crawlErrorMessage,
        String userNote,
        List<UUID> selectedSenseIds,
        List<UUID> selectedIdiomIds,
        List<UUID> selectedPhrasalVerbIds,
        List<SenseOptionDto> availableSenses,
        List<PhraseOptionDto> availableIdioms,
        List<PhraseOptionDto> availablePhrasalVerbs,
        String masteryLevel,
        int correctCount,
        int wrongCount
) {
    public record SenseOptionDto(
            UUID id,
            String partOfSpeech,
            String cefrLevel,
            String definitionEn,
            String definitionVn,
            String exampleEn,
            String exampleVn,
            List<String> synonyms,
            List<String> antonyms,
            List<String> collocations,
            String topic,
            String dataSource,
            long questionCount
    ) {}

    public record PhraseOptionDto(
            UUID id,
            String phrase,
            String definitionEn,
            String definitionVn,
            String exampleEn,
            String exampleVn,
            String dataSource,
            long questionCount
    ) {}
}

