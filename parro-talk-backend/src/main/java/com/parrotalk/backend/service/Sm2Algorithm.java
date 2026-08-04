package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.Sm2Rating;
import com.parrotalk.backend.entity.UserVocabulary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Implementation of the SuperMemo-2 (SM-2) algorithm.
 */
@Service
public class Sm2Algorithm {

    /**
     * Updates the SM-2 properties of the given user vocabulary item based on the rating.
     * 
     * @param vocab the vocabulary item to update
     * @param rating the user's rating of how well they remembered the item
     */
    public void calculateNextReview(UserVocabulary vocab, Sm2Rating rating) {
        int repetitions = vocab.getRepetitions();
        int interval = vocab.getIntervalDays();
        double easeFactor = vocab.getEaseFactor();

        int q = rating.getValue(); // 0: Again, 1: Hard, 2: Good, 3: Easy
        
        // Maps our 0-3 rating to standard SM-2 0-5 rating.
        // Again -> 0, Hard -> 3, Good -> 4, Easy -> 5
        int sm2Quality;
        switch (rating) {
            case EASY:
                sm2Quality = 5;
                break;
            case GOOD:
                sm2Quality = 4;
                break;
            case HARD:
                sm2Quality = 3;
                break;
            case AGAIN:
            default:
                sm2Quality = 0;
                break;
        }

        if (sm2Quality >= 3) {
            // Correct response
            if (repetitions == 0) {
                interval = 1;
            } else if (repetitions == 1) {
                interval = 6;
            } else {
                interval = (int) Math.round(interval * easeFactor);
            }
            repetitions++;
            vocab.setCorrectCount(vocab.getCorrectCount() + 1);
        } else {
            // Incorrect response
            repetitions = 0;
            interval = 1;
            vocab.setWrongCount(vocab.getWrongCount() + 1);
        }

        easeFactor = easeFactor + (0.1 - (5 - sm2Quality) * (0.08 + (5 - sm2Quality) * 0.02));
        if (easeFactor < 1.3) {
            easeFactor = 1.3;
        }

        vocab.setRepetitions(repetitions);
        vocab.setIntervalDays(interval);
        vocab.setEaseFactor(easeFactor);
        vocab.setLastReviewedAt(LocalDateTime.now());
        vocab.setNextReviewAt(LocalDateTime.now().plusDays(interval));
    }
}
