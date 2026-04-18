package com.parrotalk.backend.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.parrotalk.backend.constant.Difficulty;

/**
 * Scoring utility class.
 * 
 * @author MinhTuMTN
 */
public class ScoringUtil {

    /** Maximum score */
    private static final int MAX_SCORE = 10;

    /** Minimum score */
    private static final int MIN_SCORE = 4;

    /**
     * Check if the user answer is correct.
     * 
     * @param userAnswer    User answer
     * @param correctAnswer Correct answer
     * @return True if the answer is correct, false otherwise
     */
    public static boolean checkAnswerIsCorrect(String userAnswer, String correctAnswer) {
        if (userAnswer == null || correctAnswer == null) {
            return false;
        }

        List<String> userWords = preprocess(userAnswer);
        List<String> correctWords = preprocess(correctAnswer);

        int matchCount = 0;
        int minSize = Math.min(userWords.size(), correctWords.size());

        for (int i = 0; i < minSize; i++) {
            if (userWords.get(i).equals(correctWords.get(i))) {
                matchCount++;
            }
        }

        double accuracy = (double) matchCount / correctWords.size();

        return accuracy >= 0.8;
    }

    /**
     * Calculate score for a sentence.
     * 
     * @param sentence    Sentence
     * @param hintWords   Number of hints used
     * @param replayCount Number of replays
     * @return Score
     */
    public static int calculateScore(
            String sentence,
            int hintWords,
            int replayCount) {
        Difficulty difficulty = SentenceUtil.getSentenceDifficulty(sentence);

        int hintPenalty = calculateHintPenalty(hintWords);
        int replayPenalty = calculateReplayPenalty(difficulty, replayCount);

        int score = MAX_SCORE - hintPenalty - replayPenalty;

        return Math.max(score, MIN_SCORE);
    }

    /**
     * Preprocess the text to remove punctuation and convert to lowercase.
     * 
     * @param text Text to preprocess
     * @return Preprocessed text
     */
    private static List<String> preprocess(String text) {
        return Arrays.stream(text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .split("\\s+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Calculate hint penalty.
     * ceil(hintWords / 2)
     * 
     * @param hintWords Number of hints used
     * @return Hint penalty
     */
    private static int calculateHintPenalty(int hintWords) {
        if (hintWords <= 0)
            return 0;
        return (int) Math.ceil(hintWords / 2.0);
    }

    /**
     * Calculate replay penalty.
     * Replay penalty logic:
     * SHORT -> free=1, step=2
     * MEDIUM -> free=2, step=2
     * LONG -> free=3, step=3
     *
     * penalty = floor((replay - free) / step) + 1
     * only if replay > free
     * 
     * @param difficulty  Sentence difficulty
     * @param replayCount Number of replays
     * @return Replay penalty
     */
    private static int calculateReplayPenalty(Difficulty difficulty, int replayCount) {
        int freeReplay;
        int step;

        switch (difficulty) {
            case SHORT -> {
                freeReplay = 1;
                step = 2;
            }
            case MEDIUM -> {
                freeReplay = 2;
                step = 2;
            }
            case LONG -> {
                freeReplay = 3;
                step = 3;
            }
            default -> {
                freeReplay = 2;
                step = 2;
            }
        }

        if (replayCount <= freeReplay) {
            return 0;
        }

        return ((replayCount - freeReplay) / step) + 1;
    }

}
