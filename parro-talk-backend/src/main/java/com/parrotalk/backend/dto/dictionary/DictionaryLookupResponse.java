package com.parrotalk.backend.dto.dictionary;

import java.util.UUID;

public record DictionaryLookupResponse(
        UUID id,
        String word,
        String normalizedWord,
        String language,
        String partOfSpeech,
        String phonetic,
        String definitionsJson,
        String examplesJson,
        String synonymsJson,
        String antonymsJson,
        String source,
        String cefrLevel,
        String audioUkUrl,
        String audioUsUrl,
        String commonMeaningVi,
        boolean cacheHit
) {
}
