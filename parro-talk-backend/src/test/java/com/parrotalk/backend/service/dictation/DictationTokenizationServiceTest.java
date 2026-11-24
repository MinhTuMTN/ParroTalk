package com.parrotalk.backend.service.dictation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.parrotalk.backend.service.dictation.DictationTokenizationService.DictationToken;

class DictationTokenizationServiceTest {

    private final DictationTokenizationService tokenizationService = new DictationTokenizationService();

    @Test
    void tokenizesWordsContractionsHyphensAndPunctuationWithStableIndices() {
        String text = "Don't worry, my co-worker said I’m fine!";
        List<DictationToken> tokens = tokenizationService.tokenize(text);

        List<DictationToken> words = tokens.stream().filter(DictationToken::isWord).toList();
        assertEquals(List.of("Don't", "worry", "my", "co-worker", "said", "I’m", "fine"),
                words.stream().map(DictationToken::getText).toList());
        assertEquals(List.of("don't", "worry", "my", "co-worker", "said", "i'm", "fine"),
                words.stream().map(DictationToken::getNormalized).toList());

        for (int i = 0; i < words.size(); i++) {
            assertEquals(i, words.get(i).getWordIndex());
            assertEquals(words.get(i).getText(), text.substring(words.get(i).getCharStart(), words.get(i).getCharEnd()));
        }

        List<DictationToken> nonWords = tokens.stream().filter(t -> !t.isWord()).toList();
        assertFalse(nonWords.isEmpty());
        for (DictationToken nonWord : nonWords) {
            assertEquals(-1, nonWord.getWordIndex());
            assertEquals(nonWord.getText(), text.substring(nonWord.getCharStart(), nonWord.getCharEnd()));
        }

        for (int i = 0; i < tokens.size(); i++) {
            assertEquals(i, tokens.get(i).getTokenIndex());
        }
    }

    @Test
    void extractWordTokensMatchesWordTokensAndHandlesEmptyOrPunctuationOnly() {
        assertTrue(tokenizationService.tokenize(null).isEmpty());
        assertTrue(tokenizationService.tokenize("").isEmpty());
        assertTrue(tokenizationService.extractWordTokens("... ! ?").isEmpty());

        List<DictationToken> nonWordOnly = tokenizationService.tokenize("...");
        assertEquals(1, nonWordOnly.size());
        assertFalse(nonWordOnly.get(0).isWord());
        assertEquals(-1, nonWordOnly.get(0).getWordIndex());
    }
}
