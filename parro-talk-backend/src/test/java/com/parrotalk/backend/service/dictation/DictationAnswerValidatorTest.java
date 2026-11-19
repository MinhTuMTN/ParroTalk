package com.parrotalk.backend.service.dictation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.parrotalk.backend.dto.dictation.BlankAnswerItem;
import com.parrotalk.backend.dto.dictation.BlankResultItem;
import com.parrotalk.backend.dto.dictation.BlankSlotDto;
import com.parrotalk.backend.service.dictation.DictationAnswerValidator.SegmentValidationResult;

class DictationAnswerValidatorTest {

    private final DictationTokenizationService tokenizationService = new DictationTokenizationService();
    private final DictationAnswerValidator validator = new DictationAnswerValidator(tokenizationService);

    @Test
    void acceptsCaseWhitespaceCurlyApostropheAndOuterPunctuationVariants() {
        assertTrue(validator.isBlankAnswerCorrect("  DON’T! ", "don't", null));
        assertTrue(validator.isBlankAnswerCorrect(" \"Environment.\" ", "environment", null));
        assertTrue(validator.isBlankAnswerCorrect("I'm", "I’m", null));
    }

    @Test
    void allowsUnambiguousContractionsWithoutApostropheButRejectsAmbiguousPairs() {
        // Unambiguous contractions allowed without apostrophe
        assertTrue(validator.isBlankAnswerCorrect("dont", "don't", null));
        assertTrue(validator.isBlankAnswerCorrect("didnt", "didn't", null));
        assertTrue(validator.isBlankAnswerCorrect("isnt", "isn't", null));
        assertTrue(validator.isBlankAnswerCorrect("wouldnt", "wouldn't", null));

        // Ambiguous contractions MUST NOT match stripped form
        assertFalse(validator.isBlankAnswerCorrect("its", "it's", null));
        assertFalse(validator.isBlankAnswerCorrect("were", "we're", null));
        assertFalse(validator.isBlankAnswerCorrect("well", "we'll", null));
        assertFalse(validator.isBlankAnswerCorrect("hell", "he'll", null));
        assertFalse(validator.isBlankAnswerCorrect("ill", "I'll", null));
        assertFalse(validator.isBlankAnswerCorrect("shell", "she'll", null));
        assertFalse(validator.isBlankAnswerCorrect("shed", "she'd", null));
    }

    @Test
    void acceptsHyphenAndNumberVariantsAndAcceptableAnswers() {
        assertTrue(validator.isBlankAnswerCorrect("coworker", "co-worker", null));
        assertTrue(validator.isBlankAnswerCorrect("co worker", "co-worker", null));
        assertTrue(validator.isBlankAnswerCorrect("co-worker", "co-worker", null));

        assertTrue(validator.isBlankAnswerCorrect("fifteen", "15", null));
        assertTrue(validator.isBlankAnswerCorrect("15", "fifteen", null));
        assertTrue(validator.isBlankAnswerCorrect("twenty five", "25", null));
        assertTrue(validator.isBlankAnswerCorrect("25", "twenty-five", null));

        assertTrue(validator.isBlankAnswerCorrect("colour", "color", List.of("colour")));
        assertFalse(validator.isBlankAnswerCorrect("hue", "color", List.of("colour")));
    }

    @Test
    void validatesEachBlankIndependentlyByWordIndexForRepeatedWordsAndConcealsWrongExpectedAnswer() {
        String text = "The team checked the report before the meeting.";
        // wordIndex: 0=The, 1=team, 2=checked, 3=the, 4=report, 5=before, 6=the, 7=meeting
        List<BlankSlotDto> blanks = List.of(
                BlankSlotDto.builder().wordIndex(2).charLength(7).build(),
                BlankSlotDto.builder().wordIndex(3).charLength(3).build(),
                BlankSlotDto.builder().wordIndex(6).charLength(3).build(),
                BlankSlotDto.builder().wordIndex(7).charLength(7).build());

        SegmentValidationResult partial = validator.validateSegmentBlanks(text, blanks, List.of(
                BlankAnswerItem.builder().wordIndex(2).answer("checked").build(),
                BlankAnswerItem.builder().wordIndex(3).answer("the").build(),
                BlankAnswerItem.builder().wordIndex(6).answer("a").build(),
                BlankAnswerItem.builder().wordIndex(7).answer("WRONG").build()));

        assertFalse(partial.isAllCorrect());
        assertEquals(2, partial.getCorrectCount());
        assertEquals(4, partial.getTotalBlanks());
        assertEquals(0.5, partial.getAccuracy());

        List<BlankResultItem> results = partial.getResults();
        assertTrue(results.get(0).isCorrect());
        assertEquals("checked", results.get(0).getExpectedAnswer());
        assertTrue(results.get(1).isCorrect());
        assertEquals("the", results.get(1).getExpectedAnswer());
        assertFalse(results.get(2).isCorrect());
        assertNull(results.get(2).getExpectedAnswer());
        assertFalse(results.get(3).isCorrect());
        assertNull(results.get(3).getExpectedAnswer());

        SegmentValidationResult allPassed = validator.validateSegmentBlanks(text, blanks, List.of(
                BlankAnswerItem.builder().wordIndex(2).answer("checked").build(),
                BlankAnswerItem.builder().wordIndex(3).answer("the").build(),
                BlankAnswerItem.builder().wordIndex(6).answer("the").build(),
                BlankAnswerItem.builder().wordIndex(7).answer("meeting").build()));

        assertTrue(allPassed.isAllCorrect());
        assertEquals(1.0, allPassed.getAccuracy());
        assertEquals("meeting", allPassed.getResults().get(3).getExpectedAnswer());
    }
}
