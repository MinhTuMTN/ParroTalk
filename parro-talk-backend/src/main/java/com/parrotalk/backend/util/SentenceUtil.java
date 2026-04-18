package com.parrotalk.backend.util;

import com.parrotalk.backend.constant.Difficulty;

/**
 * Sentence utility class.
 * 
 * @author MinhTuMTN
 */
public class SentenceUtil {

    /**
     * Get sentence difficulty.
     * 
     * @param sentence Sentence
     * @return Sentence difficulty
     */
    public static Difficulty getSentenceDifficulty(String sentence) {
        if (sentence == null || sentence.isBlank()) {
            return Difficulty.SHORT;
        }

        int wordCount = sentence.trim().split("\\s+").length;

        if (wordCount <= 7) {
            return Difficulty.SHORT;
        } else if (wordCount <= 12) {
            return Difficulty.MEDIUM;
        } else {
            return Difficulty.LONG;
        }
    }
}
