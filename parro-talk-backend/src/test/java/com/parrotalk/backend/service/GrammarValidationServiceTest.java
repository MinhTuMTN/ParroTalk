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

    @Test
    @DisplayName("Inflection-aware and word-boundary matching matches inflected terms without substring false positives")
    void testValidateUserDraft_InflectionAwareAndWordBoundaryMatching() {
        GrammarValidationResultDto result = validationService.validateUserDraft(
                "Alcohol impaired his driving ability and she went home early, preventing others from causing harm.",
                List.of("impair", "drive", "go", "air", "prevent sb from doing sth", "[Subject] + prevented + [O]")
        );

        assertNotNull(result.getUsedTerms());
        assertNotNull(result.getMissingTerms());
        assertTrue(result.getUsedTerms().contains("impair"), "Should match 'impaired'");
        assertTrue(result.getUsedTerms().contains("drive"), "Should match 'driving'");
        assertTrue(result.getUsedTerms().contains("go"), "Should match irregular 'went'");
        assertTrue(result.getUsedTerms().contains("prevent sb from doing sth"), "Should match phrase gap");
        assertFalse(result.getUsedTerms().contains("air"), "Should NOT match 'air' inside 'impaired'");
        assertTrue(result.getMissingTerms().contains("air"));
        assertTrue(result.getMissingTerms().contains("[Subject] + prevented + [O]"));
    }

    @Test
    @DisplayName("Sentence patterns match anchor tokens and allowlist avoids false positives on camelCase or valid doubled words")
    void testValidateUserDraft_SentencePatternAndAllowlists() {
        GrammarValidationResultDto result = validationService.validateUserDraft(
                "iPhone had had great reviews (even in tests). She should not drive because it is unsafe.",
                List.of("[Subject] + should not + [V-inf] + because + [Clause].")
        );

        assertTrue(result.isValid(), "Expected valid draft without false positives, issues=" + result.getIssues());
        assertEquals(WritingItemValidationStatus.APPROVED, result.getStatus());
        assertTrue(result.getUsedTerms().contains("[Subject] + should not + [V-inf] + because + [Clause]."));
    }

    @Test
    @DisplayName("Draft validation enforces MIN_DRAFT_LENGTH = 3 while validateSentence requires MIN_SENTENCE_LENGTH = 6")
    void testValidateUserDraft_MinDraftLengthThree() {
        GrammarValidationResultDto draftHi = validationService.validateUserDraft("Hi.", List.of());
        assertNotEquals(WritingItemValidationStatus.REJECTED, draftHi.getStatus());
        assertTrue(draftHi.isValid());

        GrammarValidationResultDto draftShort = validationService.validateUserDraft("Hi", List.of());
        assertEquals(WritingItemValidationStatus.REJECTED, draftShort.getStatus());
        assertFalse(draftShort.isValid());
    }

    @Test
    @DisplayName("Multi-sentence draft reports raw text offsets without trim shift")
    void testValidateUserDraft_MultiSentenceAndRawOffsets() {
        GrammarValidationResultDto result = validationService.validateUserDraft(
                "  First sentence. second sentence.",
                List.of()
        );

        assertFalse(result.isValid());
        assertTrue(result.getIssues().stream()
                .anyMatch(i -> "UPPERCASE_START".equals(i.getRuleId()) && i.getOffset() == 18));
    }
}

