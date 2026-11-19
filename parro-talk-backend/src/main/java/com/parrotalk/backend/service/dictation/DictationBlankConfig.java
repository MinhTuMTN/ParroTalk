package com.parrotalk.backend.service.dictation;

import java.util.Map;

import com.parrotalk.backend.constant.DictationMode;

/**
 * Centralized configuration for Fill-in-the-Blanks blank counts, signal weights, and structural constraints.
 */
public final class DictationBlankConfig {

    public static final int SCHEMA_VERSION = 1;
    public static final double MIN_VISIBLE_WORD_RATIO = 0.40;
    public static final double REPETITION_PENALTY = 0.35;

    private static final Map<DictationMode, ModeWeights> WEIGHTS = Map.of(
            DictationMode.EASY, new ModeWeights(0.30, 0.30, 0.20, 0.05, 0.15),
            DictationMode.MEDIUM, new ModeWeights(0.25, 0.25, 0.20, 0.15, 0.15),
            DictationMode.HARD, new ModeWeights(0.15, 0.15, 0.20, 0.35, 0.15));

    private DictationBlankConfig() {
    }

    public static ModeWeights weightsFor(DictationMode mode) {
        return WEIGHTS.getOrDefault(mode, WEIGHTS.get(DictationMode.MEDIUM));
    }

    /**
     * Target blank range for a sentence of {@code wordCount} words at the given mode.
     *
     * <ul>
     *   <li>{@code N <= 1}: 0 blanks (unsuitable)</li>
     *   <li>{@code 2 <= N <= 4}: Easy 1, Medium 1, Hard {@code min(N - 1, 2)}</li>
     *   <li>{@code 5 <= N <= 7}: Easy 1, Medium 2, Hard 3..4</li>
     *   <li>{@code 8 <= N <= 12}: Easy 1..2, Medium 3..4, Hard 5..7</li>
     *   <li>{@code 13 <= N <= 18}: Easy 2..3, Medium 4..6, Hard 7..10</li>
     *   <li>{@code N >= 19}: Easy 3..4, Medium 6..8, Hard 9..12</li>
     * </ul>
     */
    public static BlankRange resolveBlankRange(int wordCount, DictationMode mode) {
        if (wordCount <= 1 || mode == null || mode == DictationMode.FULL) {
            return new BlankRange(0, 0);
        }

        if (wordCount < 5) {
            int maxByVisibility = Math.max(1, wordCount - 1);
            return switch (mode) {
                case EASY, MEDIUM -> new BlankRange(1, Math.min(1, maxByVisibility));
                case HARD -> new BlankRange(1, Math.min(wordCount - 1, 2));
                case FULL -> new BlankRange(0, 0);
            };
        }

        BlankRange raw;
        if (wordCount <= 7) {
            raw = switch (mode) {
                case EASY -> new BlankRange(1, 1);
                case MEDIUM -> new BlankRange(2, 2);
                case HARD -> new BlankRange(3, 4);
                case FULL -> new BlankRange(0, 0);
            };
        } else if (wordCount <= 12) {
            raw = switch (mode) {
                case EASY -> new BlankRange(1, 2);
                case MEDIUM -> new BlankRange(3, 4);
                case HARD -> new BlankRange(5, 7);
                case FULL -> new BlankRange(0, 0);
            };
        } else if (wordCount <= 18) {
            raw = switch (mode) {
                case EASY -> new BlankRange(2, 3);
                case MEDIUM -> new BlankRange(4, 6);
                case HARD -> new BlankRange(7, 10);
                case FULL -> new BlankRange(0, 0);
            };
        } else {
            raw = switch (mode) {
                case EASY -> new BlankRange(3, 4);
                case MEDIUM -> new BlankRange(6, 8);
                case HARD -> new BlankRange(9, 12);
                case FULL -> new BlankRange(0, 0);
            };
        }

        int maxAllowedVisible = (int) Math.floor(wordCount * (1.0 - MIN_VISIBLE_WORD_RATIO));
        int clampedMax = Math.min(raw.max(), Math.max(1, maxAllowedVisible));
        int clampedMin = Math.min(raw.min(), clampedMax);
        return new BlankRange(clampedMin, clampedMax);
    }

    public static int maxConsecutiveBlanks(DictationMode mode) {
        return switch (mode) {
            case EASY -> 1;
            case MEDIUM -> 1;
            case HARD -> 2;
            case FULL -> 0;
        };
    }

    public static int fallbackMaxConsecutiveBlanks(DictationMode mode) {
        return switch (mode) {
            case EASY -> 1;
            case MEDIUM, HARD -> 2;
            case FULL -> 0;
        };
    }

    public record BlankRange(int min, int max) {
    }

    public record ModeWeights(double wPos, double wSem, double wCefr, double wListen, double wLearn) {
    }
}
