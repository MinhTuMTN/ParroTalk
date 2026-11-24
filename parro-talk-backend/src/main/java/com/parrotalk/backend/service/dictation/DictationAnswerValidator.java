package com.parrotalk.backend.service.dictation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.parrotalk.backend.dto.dictation.BlankAnswerItem;
import com.parrotalk.backend.dto.dictation.BlankResultItem;
import com.parrotalk.backend.dto.dictation.BlankSlotDto;
import com.parrotalk.backend.service.dictation.DictationTokenizationService.DictationToken;

import lombok.Builder;
import lombok.Value;

/**
 * Validates learner answers for Fill-in-the-Blanks dictation mode.
 * Handles apostrophes safely (without collapsing ambiguous pairs such as {@code its}/{@code it's}),
 * hyphen variants, canonical number-word equivalence, and optional acceptable alternatives.
 */
@Service
public class DictationAnswerValidator {

    private static final Pattern OUTER_PUNCTUATION = Pattern.compile("^[^a-zA-Z0-9']+|[^a-zA-Z0-9']+$");
    private static final Pattern MULTI_WHITESPACE = Pattern.compile("\\s+");

    /**
     * Contractions whose un-apostrophed form does not collide with another distinct English word.
     * Ambiguous contractions ({@code it's}/{@code its}, {@code we're}/{@code were}, {@code we'll}/{@code well},
     * {@code he'll}/{@code hell}, {@code I'll}/{@code ill}, {@code she'll}/{@code shell}, {@code she'd}/{@code shed},
     * {@code we'd}/{@code wed}) are intentionally excluded.
     */
    private static final Set<String> UNAMBIGUOUS_CONTRACTIONS = Set.of(
            "don't",
            "doesn't",
            "didn't",
            "isn't",
            "aren't",
            "wasn't",
            "weren't",
            "haven't",
            "hasn't",
            "hadn't",
            "wouldn't",
            "couldn't",
            "shouldn't");

    private static final Map<String, String> UNAMBIGUOUS_STRIPPED_TO_CANONICAL = buildUnambiguousMap();

    private static final Map<String, String> NUMBER_WORDS = buildNumberWords();

    private static final Set<String> VALID_TENS = Set.of(
            "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety");

    private static final Set<String> VALID_ONES = Set.of(
            "one", "two", "three", "four", "five", "six", "seven", "eight", "nine");

    private static Map<String, String> buildUnambiguousMap() {
        Map<String, String> map = new HashMap<>();
        for (String contraction : UNAMBIGUOUS_CONTRACTIONS) {
            map.put(contraction.replace("'", ""), contraction);
        }
        return Map.copyOf(map);
    }

    private static Map<String, String> buildNumberWords() {
        Map<String, String> map = new HashMap<>();
        map.put("0", "zero");
        map.put("1", "one");
        map.put("2", "two");
        map.put("3", "three");
        map.put("4", "four");
        map.put("5", "five");
        map.put("6", "six");
        map.put("7", "seven");
        map.put("8", "eight");
        map.put("9", "nine");
        map.put("10", "ten");
        map.put("11", "eleven");
        map.put("12", "twelve");
        map.put("13", "thirteen");
        map.put("14", "fourteen");
        map.put("15", "fifteen");
        map.put("16", "sixteen");
        map.put("17", "seventeen");
        map.put("18", "eighteen");
        map.put("19", "nineteen");
        map.put("20", "twenty");
        map.put("30", "thirty");
        map.put("40", "forty");
        map.put("50", "fifty");
        map.put("60", "sixty");
        map.put("70", "seventy");
        map.put("80", "eighty");
        map.put("90", "ninety");
        map.put("100", "hundred");
        map.put("1000", "thousand");
        return Map.copyOf(map);
    }

    private final DictationTokenizationService tokenizationService;

    public DictationAnswerValidator(DictationTokenizationService tokenizationService) {
        this.tokenizationService = tokenizationService;
    }

    /**
     * Checks whether a single user blank submission matches the expected word (or any acceptable variant).
     */
    public boolean isBlankAnswerCorrect(String userAnswer, String expectedWord, List<String> acceptableAnswers) {
        String normalizedUser = normalizeInput(userAnswer);
        if (normalizedUser.isEmpty()) {
            return false;
        }

        List<String> expectedCandidates = new ArrayList<>();
        if (expectedWord != null && !expectedWord.isBlank()) {
            expectedCandidates.add(expectedWord);
        }
        if (acceptableAnswers != null) {
            for (String alt : acceptableAnswers) {
                if (alt != null && !alt.isBlank()) {
                    expectedCandidates.add(alt);
                }
            }
        }

        for (String candidate : expectedCandidates) {
            if (matchesCandidate(normalizedUser, normalizeInput(candidate))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates all blanks for a segment independently by {@code wordIndex}.
     * {@code expectedAnswer} in {@link BlankResultItem} is revealed only when the individual blank is correct
     * or when all blanks in the segment passed.
     */
    public SegmentValidationResult validateSegmentBlanks(
            String segmentText,
            List<BlankSlotDto> blanks,
            List<BlankAnswerItem> submittedAnswers) {
        if (blanks == null || blanks.isEmpty()) {
            return SegmentValidationResult.builder()
                    .allCorrect(false)
                    .correctCount(0)
                    .totalBlanks(0)
                    .accuracy(0.0)
                    .results(List.of())
                    .build();
        }

        // 1. Index expected word tokens from the reference segment text by wordIndex
        List<DictationToken> wordTokens = tokenizationService.extractWordTokens(segmentText);
        Map<Integer, DictationToken> tokenByWordIndex = new HashMap<>();
        for (DictationToken token : wordTokens) {
            tokenByWordIndex.put(token.getWordIndex(), token);
        }

        // 2. Index user-submitted answers by wordIndex for O(1) lookup per blank slot
        Map<Integer, String> submittedByWordIndex = new HashMap<>();
        if (submittedAnswers != null) {
            for (BlankAnswerItem item : submittedAnswers) {
                if (item != null) {
                    submittedByWordIndex.put(item.getWordIndex(), item.getAnswer());
                }
            }
        }

        // 3. Evaluate each blank independently against the expected word and acceptable variants
        List<Boolean> correctnessFlags = new ArrayList<>(blanks.size());
        List<String> expectedWords = new ArrayList<>(blanks.size());
        int correctCount = 0;

        for (BlankSlotDto slot : blanks) {
            DictationToken expectedToken = tokenByWordIndex.get(slot.getWordIndex());
            String expectedWord = expectedToken != null ? expectedToken.getText() : "";
            String submitted = submittedByWordIndex.get(slot.getWordIndex());
            boolean correct = isBlankAnswerCorrect(submitted, expectedWord, slot.getAcceptableAnswers());
            correctnessFlags.add(correct);
            expectedWords.add(expectedWord);
            if (correct) {
                correctCount++;
            }
        }

        // 4. Build per-blank result items; reveal expectedAnswer only if the blank passed or all blanks passed
        boolean allCorrect = correctCount == blanks.size();
        List<BlankResultItem> results = new ArrayList<>(blanks.size());
        for (int i = 0; i < blanks.size(); i++) {
            BlankSlotDto slot = blanks.get(i);
            boolean correct = correctnessFlags.get(i);
            results.add(BlankResultItem.builder()
                    .wordIndex(slot.getWordIndex())
                    .correct(correct)
                    .expectedAnswer((correct || allCorrect) ? expectedWords.get(i) : null)
                    .build());
        }

        // 5. Compute accuracy rounded to 4 decimal places
        double accuracy = Math.round(((double) correctCount / blanks.size()) * 10000.0) / 10000.0;
        return SegmentValidationResult.builder()
                .allCorrect(allCorrect)
                .correctCount(correctCount)
                .totalBlanks(blanks.size())
                .accuracy(accuracy)
                .results(results)
                .build();
    }

    private boolean matchesCandidate(String userNorm, String expectedNorm) {
        if (expectedNorm.isEmpty()) {
            return false;
        }
        if (userNorm.equals(expectedNorm)) {
            return true;
        }

        if (matchesUnambiguousContraction(userNorm, expectedNorm)) {
            return true;
        }

        if (matchesHyphenVariant(userNorm, expectedNorm)) {
            return true;
        }

        return matchesNumberEquivalent(userNorm, expectedNorm);
    }

    private boolean matchesUnambiguousContraction(String userNorm, String expectedNorm) {
        if (UNAMBIGUOUS_CONTRACTIONS.contains(expectedNorm)
                && userNorm.equals(expectedNorm.replace("'", ""))) {
            return true;
        }
        String canonicalFromUser = UNAMBIGUOUS_STRIPPED_TO_CANONICAL.get(userNorm);
        return canonicalFromUser != null && canonicalFromUser.equals(expectedNorm);
    }

    private boolean matchesHyphenVariant(String userNorm, String expectedNorm) {
        if (!expectedNorm.contains("-") && !userNorm.contains("-")) {
            return false;
        }
        Set<String> expectedVariants = expandHyphenVariants(expectedNorm);
        Set<String> userVariants = expandHyphenVariants(userNorm);
        for (String variant : userVariants) {
            if (expectedVariants.contains(variant)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> expandHyphenVariants(String value) {
        Set<String> variants = new HashSet<>();
        variants.add(value);
        variants.add(value.replace("-", ""));
        variants.add(MULTI_WHITESPACE.matcher(value.replace("-", " ")).replaceAll(" ").trim());
        variants.add(value.replace(" ", "-"));
        variants.add(value.replace(" ", ""));
        return variants;
    }

    private boolean matchesNumberEquivalent(String userNorm, String expectedNorm) {
        String userCanonical = toCanonicalNumber(userNorm);
        String expectedCanonical = toCanonicalNumber(expectedNorm);
        return userCanonical != null && userCanonical.equals(expectedCanonical);
    }

    private String toCanonicalNumber(String value) {
        if (NUMBER_WORDS.containsKey(value)) {
            return NUMBER_WORDS.get(value);
        }
        if (NUMBER_WORDS.containsValue(value)) {
            return value;
        }
        String hyphenNormalized = value.replace(' ', '-');
        if (isCompoundNumberWord(hyphenNormalized)) {
            return hyphenNormalized;
        }
        Integer parsed = parseSimpleNumeric(value);
        if (parsed != null) {
            String word = numberToWords(parsed);
            if (word != null) {
                return word;
            }
        }
        return parseCompoundWordNumber(hyphenNormalized);
    }

    private Integer parseSimpleNumeric(String value) {
        if (!value.matches("\\d{1,3}")) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String numberToWords(int n) {
        if (n < 0 || n > 99) {
            return NUMBER_WORDS.get(String.valueOf(n));
        }
        if (n <= 20 || n % 10 == 0) {
            return NUMBER_WORDS.get(String.valueOf(n));
        }
        int tens = (n / 10) * 10;
        int ones = n % 10;
        String tensWord = NUMBER_WORDS.get(String.valueOf(tens));
        String onesWord = NUMBER_WORDS.get(String.valueOf(ones));
        if (tensWord == null || onesWord == null) {
            return null;
        }
        return tensWord + "-" + onesWord;
    }

    private boolean isCompoundNumberWord(String value) {
        return parseCompoundWordNumber(value) != null;
    }

    private String parseCompoundWordNumber(String value) {
        String[] parts = value.split("-");
        if (parts.length != 2) {
            return null;
        }
        String tens = parts[0];
        String ones = parts[1];
        boolean validTens = VALID_TENS.contains(tens);
        boolean validOnes = VALID_ONES.contains(ones);
        return (validTens && validOnes) ? (tens + "-" + ones) : null;
    }

    String normalizeInput(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.replace('\u2019', '\'')
                .replace('\u2018', '\'')
                .trim()
                .toLowerCase(Locale.ROOT);
        normalized = MULTI_WHITESPACE.matcher(normalized).replaceAll(" ");
        normalized = OUTER_PUNCTUATION.matcher(normalized).replaceAll("");
        return normalized.trim();
    }

    @Value
    @Builder
    public static class SegmentValidationResult {
        boolean allCorrect;
        int correctCount;
        int totalBlanks;
        double accuracy;
        List<BlankResultItem> results;
    }
}
