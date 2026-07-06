package com.parrotalk.backend.dto.dictionary;

import java.time.LocalDateTime;
import java.util.UUID;

public record VocabularyOccurrenceResponse(
        UUID id,
        UUID lessonId,
        UUID segmentId,
        String word,
        Double startTime,
        Double endTime,
        String contextText,
        LocalDateTime createdAt
) {
}
