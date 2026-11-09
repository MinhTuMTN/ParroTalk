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
        if (words != null && !words.isEmpty()) {
            for (String w : words) {
                if (w != null && !w.isBlank()) {
                    result.add(w.trim());
                }
            }
        }
        if (rawWord != null && !rawWord.isBlank()) {
            result.add(rawWord.trim());
        }
        return result;
    }
}
