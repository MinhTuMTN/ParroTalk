package com.parrotalk.backend.util;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalizes words for dictionary lookup without changing their linguistic meaning.
 */
@Component
public class WordNormalizer {

    private static final Pattern LEADING_OR_TRAILING_PUNCTUATION = Pattern.compile("^[\\p{Punct}]+|[\\p{Punct}]+$");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    /**
     * Normalize a user-selected word for stable dictionary lookup.
     *
     * <p>This intentionally keeps contractions such as {@code don't} and {@code it's}
     * as single lexical forms. It also avoids stemming/lemmatization because dictionary
     * lookup should not silently change the transcript text selected by the user.</p>
     *
     * @param word raw word from transcript or UI
     * @return normalized lookup key, or an empty string when input is null/blank
     */
    public String normalize(String word) {
        if (word == null || word.isBlank()) {
            return "";
        }

        String normalized = word.trim()
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201B', '\'')
                .replace('\u0060', '\'')
                .toLowerCase(Locale.ROOT);

        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFKC);
        normalized = LEADING_OR_TRAILING_PUNCTUATION.matcher(normalized).replaceAll("");
        normalized = COMBINING_MARKS.matcher(normalized).replaceAll("");

        return normalized.trim();
    }
}
