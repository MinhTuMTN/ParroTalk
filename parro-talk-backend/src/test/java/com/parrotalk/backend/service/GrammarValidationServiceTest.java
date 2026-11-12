package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.WritingItemValidationStatus;
import com.parrotalk.backend.dto.writing.GrammarValidationResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GrammarValidationServiceTest {

    private GrammarValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new GrammarValidationService();
    }

    @Test
    @DisplayName("Valid sentence passes structural validation with APPROVED status")
    void testValidateSentence_ValidSentence_Approved() {
        GrammarValidationResultDto result = validationService.validateSentence(
                "People who are drunk must not drive because alcohol impairs their vision and concentration.");

        assertTrue(result.isValid());
        assertEquals(WritingItemValidationStatus.APPROVED, result.getStatus());
        assertTrue(result.getIssues().isEmpty());
    }

    @Test
    @DisplayName("Blank or too short sentence is REJECTED")
    void testValidateSentence_BlankOrShort_Rejected() {
        GrammarValidationResultDto resultBlank = validationService.validateSentence("   ");
        assertFalse(resultBlank.isValid());
        assertEquals(WritingItemValidationStatus.REJECTED, resultBlank.getStatus());

        GrammarValidationResultDto resultShort = validationService.validateSentence("Hi.");
        assertFalse(resultShort.isValid());
        assertEquals(WritingItemValidationStatus.REJECTED, resultShort.getStatus());
    }

    @Test
    @DisplayName("Sentence missing uppercase start or terminal punctuation gets NEEDS_REVIEW with issues")
    void testValidateSentence_MissingCapitalOrPunctuation_NeedsReview() {
        GrammarValidationResultDto result = validationService.validateSentence(
                "drunk drivers should not drive because it is dangerous");

        assertFalse(result.isValid());
        assertEquals(WritingItemValidationStatus.NEEDS_REVIEW, result.getStatus());
        assertEquals(2, result.getIssues().size());
        assertTrue(result.getIssues().stream().anyMatch(i -> "UPPERCASE_START".equals(i.getRuleId())));
        assertTrue(result.getIssues().stream().anyMatch(i -> "TERMINAL_PUNCTUATION".equals(i.getRuleId())));
    }

    @Test
    @DisplayName("Sentence with repeated adjacent words or unbalanced brackets gets NEEDS_REVIEW")
    void testValidateSentence_RepeatedWordsAndUnbalancedBrackets_NeedsReview() {
        GrammarValidationResultDto result = validationService.validateSentence(
                "People should not (drive drive under the influence.");

        assertFalse(result.isValid());
        assertEquals(WritingItemValidationStatus.NEEDS_REVIEW, result.getStatus());
        assertTrue(result.getIssues().stream().anyMatch(i -> "REPEATED_WORD".equals(i.getRuleId())));
        assertTrue(result.getIssues().stream().anyMatch(i -> "UNBALANCED_BRACKETS".equals(i.getRuleId())));
    }

    @Test
    @DisplayName("Draft check verifies suggested vocabulary usage and reports matched keywords")
    void testValidateUserDraft_ChecksSuggestedKeywords() {
        GrammarValidationResultDto result = validationService.validateUserDraft(
                "Drunk people should not drive because blood alcohol concentration can impair their vision.",
                List.of("impair", "blood alcohol concentration", "concentration"));

        assertTrue(result.isValid());
        assertEquals(WritingItemValidationStatus.APPROVED, result.getStatus());
        assertTrue(result.getSummaryVn().contains("impair"));
    }

    @Test
    @DisplayName("Validate vocabulary or phrase item status")
    void testValidateVocabularyOrPhrase() {
        assertEquals(WritingItemValidationStatus.APPROVED,
                validationService.validateLexicalItem("drive under the influence", "lái xe khi say rượu"));
        assertEquals(WritingItemValidationStatus.REJECTED,
                validationService.validateLexicalItem("", "nghĩa trống"));
        assertEquals(WritingItemValidationStatus.NEEDS_REVIEW,
                validationService.validateLexicalItem("impair", ""));
    }
}

