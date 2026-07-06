package com.parrotalk.backend.dto.dictionary;

import com.parrotalk.backend.constant.VocabularyDifficulty;
import com.parrotalk.backend.constant.VocabularyStatus;

import java.time.LocalDateTime;

public record UpdateVocabularyRequest(
        VocabularyStatus status,
        VocabularyDifficulty difficulty,
        String note,
        LocalDateTime nextReviewAt
) {
}
