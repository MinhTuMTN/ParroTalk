package com.parrotalk.backend.dto.vocablist;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record AddVocabularyListItemRequest(
        List<String> words,
        String rawWord,
        UUID vocabularyId,
        String sourceOption,
        String userNote
) {
    public AddVocabularyListItemRequest(List<String> words, String sourceOption, String userNote) {
        this(words, null, null, sourceOption, userNote);
    }

    public List<String> resolveWords() {
        List<String> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        if (words != null && !words.isEmpty()) {
            for (String w : words) {
                if (w != null && !w.isBlank()) {
                    String trimmed = w.trim();
                    if (seen.add(trimmed.toLowerCase())) {
                        result.add(trimmed);
                    }
                }
            }
        }
        if (rawWord != null && !rawWord.isBlank()) {
            String trimmed = rawWord.trim();
            if (seen.add(trimmed.toLowerCase())) {
                result.add(trimmed);
            }
        }
        if (result.size() > 100) {
            return result.subList(0, 100);
        }
        return result;
    }
}
