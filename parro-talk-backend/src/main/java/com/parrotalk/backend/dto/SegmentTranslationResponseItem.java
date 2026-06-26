package com.parrotalk.backend.dto;

import java.util.UUID;

/**
 * Output item returned by the translation model.
 */
public record SegmentTranslationResponseItem(
        UUID segmentId,
        String translatedText) {
}
