package com.parrotalk.backend.service.dictation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import lombok.Value;

/**
 * Deterministic tokenizer for English dictation segments.
 * Ensures 1-to-1 word index parity between blank generation, answer validation, and frontend rendering.
 */
@Service
public class DictationTokenizationService {

    /**
     * Matches words with internal ASCII or curly apostrophes and hyphens (e.g. {@code don't}, {@code don’t},
     * {@code I'm}, {@code co-worker}). Non-matching spans are emitted as non-word tokens.
     */
    public static final Pattern WORD_PATTERN = Pattern.compile("([a-zA-Z0-9]+(?:[-'\\u2019][a-zA-Z0-9]+)*)");

    /**
     * Tokenizes a segment text into ordered word and non-word tokens.
     *
     * @param text raw segment text
     * @return immutable list of tokens with stable {@code tokenIndex} and {@code wordIndex}
     */
    public List<DictationToken> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        List<DictationToken> tokens = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(text);

        int cursor = 0;
        int tokenIndex = 0;
        int wordIndex = 0;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            if (start > cursor) {
                String nonWord = text.substring(cursor, start);
                tokens.add(new DictationToken(
                        tokenIndex++,
                        -1,
                        nonWord,
                        normalizeTokenText(nonWord),
                        false,
                        cursor,
                        start));
            }

            String word = matcher.group(1);
            tokens.add(new DictationToken(
                    tokenIndex++,
                    wordIndex++,
                    word,
                    normalizeTokenText(word),
                    true,
                    start,
                    end));
            cursor = end;
        }

        if (cursor < text.length()) {
            String trailing = text.substring(cursor);
            tokens.add(new DictationToken(
                    tokenIndex,
                    -1,
                    trailing,
                    normalizeTokenText(trailing),
                    false,
                    cursor,
                    text.length()));
        }

        return Collections.unmodifiableList(tokens);
    }

    /**
     * Extracts only the word tokens ({@code isWord == true}) in order.
     */
    public List<DictationToken> extractWordTokens(String text) {
        List<DictationToken> all = tokenize(text);
        List<DictationToken> words = new ArrayList<>();
        for (DictationToken token : all) {
            if (token.isWord()) {
                words.add(token);
            }
        }
        return Collections.unmodifiableList(words);
    }

    /**
     * Normalizes curly apostrophes to ASCII {@code '} and lowercases using {@link Locale#ROOT}.
     */
    public static String normalizeTokenText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u2019', '\'')
                .replace('\u2018', '\'')
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Immutable representation of a token in a dictation segment.
     */
    @Value
    public static class DictationToken {
        int tokenIndex;
        int wordIndex;
        String text;
        String normalized;
        boolean word;
        int charStart;
        int charEnd;

        public boolean isWord() {
            return word;
        }
    }
}
