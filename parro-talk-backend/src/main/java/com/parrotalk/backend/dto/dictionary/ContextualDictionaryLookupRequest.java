package com.parrotalk.backend.dto.dictionary;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ContextualDictionaryLookupRequest(
        @NotBlank String word,
        @NotBlank String contextText,
        UUID lessonId,
        UUID segmentId
) {
}
