package com.parrotalk.backend.dto;

import java.util.UUID;

/**
 * Input item sent to the translation model.
 */
public record SegmentTranslationRequestItem(
        UUID segmentId,
        String text) {
}
