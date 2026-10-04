package com.parrotalk.backend.dto.dictionary;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record SaveVocabularyRequest(
        String word,
        String displayWord,
        UUID dictionaryEntryId,
        UUID vocabularyId,
        UUID lessonId,
        UUID segmentId,
        Double startTime,
        Double endTime,
        String contextText,
        String note
) {
}
