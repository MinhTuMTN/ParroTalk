package com.parrotalk.backend.dto;

/**
 * Translation exposed to API clients for one segment.
 */
public record SegmentTranslationResponse(
        String targetLanguage,
        String translatedText) {
}
