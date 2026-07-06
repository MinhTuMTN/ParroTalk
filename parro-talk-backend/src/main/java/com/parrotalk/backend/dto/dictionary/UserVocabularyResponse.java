package com.parrotalk.backend.dto.dictionary;

import com.parrotalk.backend.constant.VocabularyDifficulty;
import com.parrotalk.backend.constant.VocabularyStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserVocabularyResponse(
        UUID id,
        String normalizedWord,
        String displayWord,
        String note,
        VocabularyStatus status,
        VocabularyDifficulty difficulty,
        int reviewCount,
        LocalDateTime lastReviewedAt,
        LocalDateTime nextReviewAt,
        LocalDateTime createdAt,
        List<VocabularyOccurrenceResponse> occurrences
) {
}
