package com.parrotalk.backend.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Generates stable hashes for contextual dictionary cache keys.
 */
@Component
public class ContextHashGenerator {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final String DELIMITER = "|";

    /**
     * Generate a SHA-256 hash from normalized word and normalized context.
     *
     * @param normalizedWord normalized lookup word
     * @param contextText sentence or cropped context around the word
     * @return 64-character lowercase SHA-256 hex hash
     */
    public String generate(String normalizedWord, String contextText) {
        String input = safeNormalize(normalizedWord) + DELIMITER + safeNormalize(contextText);
        return sha256(input);
    }

    private String safeNormalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return WHITESPACE.matcher(value.trim().toLowerCase(Locale.ROOT)).replaceAll(" ");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
