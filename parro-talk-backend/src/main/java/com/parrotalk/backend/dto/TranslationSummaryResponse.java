package com.parrotalk.backend.dto;

import com.parrotalk.backend.constant.TranslationStatus;

/**
 * Aggregated translation state exposed with lesson detail.
 */
public record TranslationSummaryResponse(
        String targetLanguage,
        TranslationStatus status,
        long translatedCount,
        int totalSegments) {
}
