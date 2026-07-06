package com.parrotalk.backend.dto.dictionary;

import java.math.BigDecimal;
import java.util.UUID;

public record ContextualDictionaryLookupResponse(
        UUID id,
        String word,
        String normalizedWord,
        String contextHash,
        String meaningVi,
        String shortMeaningVi,
        String explanationVi,
        String partOfSpeech,
        BigDecimal confidence,
        String source,
        boolean cacheHit
) {
}
