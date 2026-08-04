package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.Sm2Rating;
import com.parrotalk.backend.entity.UserVocabulary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Sm2AlgorithmTest {

    private final Sm2Algorithm sm2 = new Sm2Algorithm();

    @Test
    void testCalculateNextReview_FirstTimeGood() {
        UserVocabulary vocab = new UserVocabulary();
        vocab.setIntervalDays(0);
        vocab.setRepetitions(0);
        vocab.setEaseFactor(2.5);

        sm2.calculateNextReview(vocab, Sm2Rating.GOOD);

        assertEquals(1, vocab.getIntervalDays());
        assertEquals(1, vocab.getRepetitions());
        assertEquals(2.5, vocab.getEaseFactor());
    }

    @Test
    void testCalculateNextReview_SecondTimeGood() {
        UserVocabulary vocab = new UserVocabulary();
        vocab.setIntervalDays(1);
        vocab.setRepetitions(1);
        vocab.setEaseFactor(2.5);

        sm2.calculateNextReview(vocab, Sm2Rating.GOOD);

        assertEquals(6, vocab.getIntervalDays());
        assertEquals(2, vocab.getRepetitions());
        assertEquals(2.5, vocab.getEaseFactor());
    }

    @Test
    void testCalculateNextReview_ThirdTimeGood() {
        UserVocabulary vocab = new UserVocabulary();
        vocab.setIntervalDays(6);
        vocab.setRepetitions(2);
        vocab.setEaseFactor(2.5);

        sm2.calculateNextReview(vocab, Sm2Rating.GOOD);

        assertEquals(15, vocab.getIntervalDays());
        assertEquals(3, vocab.getRepetitions());
        assertEquals(2.5, vocab.getEaseFactor());
    }

    @Test
    void testCalculateNextReview_Again_ResetsRepetitions() {
        UserVocabulary vocab = new UserVocabulary();
        vocab.setIntervalDays(15);
        vocab.setRepetitions(3);
        vocab.setEaseFactor(2.5);

        sm2.calculateNextReview(vocab, Sm2Rating.AGAIN);

        assertEquals(1, vocab.getIntervalDays());
        assertEquals(0, vocab.getRepetitions());
        assertTrue(vocab.getEaseFactor() < 2.5); // Ease factor decreases
    }
}
