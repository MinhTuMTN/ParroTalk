package com.parrotalk.backend.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScoringService {

    /**
     * Scores the user answer against the correct answer.
     * Returns a score from 0 to 100.
     */
    public int calculateScore(String userAnswer, String correctAnswer) {
        if (userAnswer == null || correctAnswer == null) {
            return 0;
        }

        List<String> userWords = preprocess(userAnswer);
        List<String> correctWords = preprocess(correctAnswer);

        if (correctWords.isEmpty()) {
            return userWords.isEmpty() ? 100 : 0;
        }

        long matchCount = 0;
        // Simple word-by-word comparison based on position or existence?
        // For dictation, sequence matters. Let's do a simple comparison for now.
        int minLength = Math.min(userWords.size(), correctWords.size());
        for (int i = 0; i < minLength; i++) {
            if (userWords.get(i).equals(correctWords.get(i))) {
                matchCount++;
            }
        }

        return (int) ((matchCount * 100) / correctWords.size());
    }

    public boolean isCorrect(int score) {
        return score >= 90; // Threshold for "correct"
    }

    private List<String> preprocess(String text) {
        return Arrays.stream(text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .split("\\s+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
