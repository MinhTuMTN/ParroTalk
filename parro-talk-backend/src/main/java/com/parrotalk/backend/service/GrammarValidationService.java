package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.WritingItemValidationStatus;
import com.parrotalk.backend.dto.writing.GrammarIssueDto;
import com.parrotalk.backend.dto.writing.GrammarValidationResultDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic structural, orthographic, and inflection-aware lexical validation service
 * for AI-generated writing items and learner sentence drafts.
 *
 * <p>Minimum lengths: {@link #MIN_SENTENCE_LENGTH} (6) for complete AI reference sentences,
 * and {@link #MIN_DRAFT_LENGTH} (3) for learner drafts aligned with the frontend minimum.
 */
@Service
public class GrammarValidationService {

    public static final int MIN_SENTENCE_LENGTH = 6;
    public static final int MIN_DRAFT_LENGTH = 3;

    private static final Pattern REPEATED_WORD_PATTERN =
            Pattern.compile("(?i)\\b([a-z]{2,})\\s+\\1\\b");
    private static final Pattern DOUBLE_SPACE_PATTERN =
            Pattern.compile(" {2,}");
    private static final Pattern WORD_TOKEN_PATTERN =
            Pattern.compile("[A-Za-z0-9]+(?:['’][A-Za-z0-9]+)?");
    private static final Pattern PATTERN_SLOT_REGEX =
            Pattern.compile("\\[[^\\]]+\\]");

    private static final Set<String> REPEATED_WORD_ALLOWLIST = Set.of(
            "had", "that", "can", "do", "did", "is", "was", "has", "will"
    );

    private static final Set<String> LOWERCASE_START_ALLOWLIST = Set.of(
            "iPhone", "iPhones", "iPad", "iPads", "iPod", "iMac", "iOS", "iTunes",
            "eBay", "eBook", "eBooks", "eCommerce", "macOS", "e.g.", "i.e."
    );

    private static final Set<String> ABBREVIATIONS_BEFORE_DOT = Set.of(
            "e.g", "i.e", "etc", "vs", "mr", "mrs", "ms", "dr", "prof", "u.s", "u.k"
    );

    private static final Set<String> PHRASE_GAP_PLACEHOLDERS = Set.of(
            "sb", "somebody", "someone", "sth", "something", "one's", "oneself",
            "doing", "v-ing", "v", "v-inf"
    );

    private static final Map<String, Set<String>> IRREGULAR_VERBS = buildIrregularVerbMap();

    /**
     * Validates a complete English sentence using deterministic structural and orthographic rules.
     */
    public GrammarValidationResultDto validateSentence(String sentence) {
        return validateTextWithMinLength(sentence, MIN_SENTENCE_LENGTH);
    }

    /**
     * Validates a learner's draft sentence (minimum 3 characters, aligned with frontend)
     * and evaluates suggested vocabulary, expressions, collocations, and sentence patterns.
     */
    public GrammarValidationResultDto validateUserDraft(String draftSentence, List<String> suggestedTerms) {
        GrammarValidationResultDto baseResult = validateTextWithMinLength(draftSentence, MIN_DRAFT_LENGTH);
        if (baseResult.getStatus() == WritingItemValidationStatus.REJECTED) {
            return baseResult;
        }

        DraftTermEvaluation evaluation = evaluateSuggestedTerms(draftSentence, suggestedTerms);
        String keywordNote = evaluation.usedTerms().isEmpty()
                ? " Bạn có thể thử áp dụng thêm các từ vựng và collocations gợi ý ở trên vào câu."
                : " Bạn đã vận dụng tốt: " + String.join(", ", evaluation.usedTerms()) + ".";

        return GrammarValidationResultDto.builder()
                .valid(baseResult.isValid())
                .status(baseResult.getStatus())
                .summaryVn(baseResult.getSummaryVn() + keywordNote)
                .issues(baseResult.getIssues())
                .usedTerms(evaluation.usedTerms())
                .missingTerms(evaluation.missingTerms())
                .build();
    }

    /**
     * Evaluates suggested lexical items and sentence patterns against a learner draft using
     * word-boundary, inflection-aware, phrase-gap, and pattern-anchor matching.
     */
    public DraftTermEvaluation evaluateSuggestedTerms(String draftSentence, List<String> suggestedTerms) {
        if (suggestedTerms == null || suggestedTerms.isEmpty()) {
            return new DraftTermEvaluation(List.of(), List.of());
        }
        List<String> draftTokens = extractNormalizedTokens(draftSentence);
        Set<String> seenTerms = new LinkedHashSet<>();
        List<String> usedTerms = new ArrayList<>();
        List<String> missingTerms = new ArrayList<>();

        for (String rawTerm : suggestedTerms) {
            if (rawTerm == null || rawTerm.isBlank()) {
                continue;
            }
            String trimmedTerm = rawTerm.trim();
            if (!seenTerms.add(trimmedTerm.toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (matchesSuggestedTerm(trimmedTerm, draftTokens)) {
                usedTerms.add(trimmedTerm);
            } else {
                missingTerms.add(trimmedTerm);
            }
        }
        return new DraftTermEvaluation(usedTerms, missingTerms);
    }

    /**
     * Validates a generated lexical item (vocabulary, expression, collocation, or sentence pattern).
     */
    public WritingItemValidationStatus validateLexicalItem(String contentEn, String meaningVn) {
        if (contentEn == null || contentEn.isBlank() || contentEn.trim().length() > 300) {
            return WritingItemValidationStatus.REJECTED;
        }
        if (meaningVn == null || meaningVn.isBlank()) {
            return WritingItemValidationStatus.NEEDS_REVIEW;
        }
        return WritingItemValidationStatus.APPROVED;
    }

    public record DraftTermEvaluation(List<String> usedTerms, List<String> missingTerms) {}

    private GrammarValidationResultDto validateTextWithMinLength(String text, int minLength) {
        int contentStart = text == null ? 0 : findContentStart(text);
        int contentEnd = text == null ? 0 : findContentEnd(text, contentStart);
        int trimmedLen = contentEnd - contentStart;

        if (text == null || trimmedLen < minLength) {
            return GrammarValidationResultDto.builder()
                    .valid(false)
                    .status(WritingItemValidationStatus.REJECTED)
                    .summaryVn("Câu quá ngắn hoặc trống, vui lòng nhập đầy đủ một câu tiếng Anh hoàn chỉnh.")
                    .issues(List.of(GrammarIssueDto.builder()
                            .ruleId("TOO_SHORT")
                            .type("GRAMMAR")
                            .message("Sentence is empty or too short.")
                            .messageVn("Câu quá ngắn hoặc trống.")
                            .offset(0)
                            .length(text == null ? 0 : text.length())
                            .replacements(List.of())
                            .build()))
                    .build();
        }

        List<GrammarIssueDto> issues = new ArrayList<>();
        List<int[]> sentenceSpans = splitSentenceSpans(text, contentStart, contentEnd);

        for (int[] span : sentenceSpans) {
            checkSentenceStartCapitalization(text, span[0], span[1], issues);
            checkTerminalPunctuation(text, span[0], span[1], issues);
        }

        // 3. Repeated adjacent words (computed directly on raw text for exact UI offsets)
        Matcher repeatedMatcher = REPEATED_WORD_PATTERN.matcher(text);
        repeatedMatcher.region(contentStart, contentEnd);
        while (repeatedMatcher.find()) {
            String word = repeatedMatcher.group(1);
            if (REPEATED_WORD_ALLOWLIST.contains(word.toLowerCase(Locale.ROOT))) {
                continue;
            }
            issues.add(GrammarIssueDto.builder()
                    .ruleId("REPEATED_WORD")
                    .type("WORD_CHOICE")
                    .message("Repeated word detected: '" + word + "'.")
                    .messageVn("Phát hiện lặp từ liên tiếp: '" + word + "'.")
                    .offset(repeatedMatcher.start())
                    .length(repeatedMatcher.end() - repeatedMatcher.start())
                    .replacements(List.of(word))
                    .build());
        }

        // 4. Balanced parentheses and brackets
        if (!hasBalancedBrackets(text, contentStart, contentEnd)) {
            issues.add(GrammarIssueDto.builder()
                    .ruleId("UNBALANCED_BRACKETS")
                    .type("PUNCTUATION")
                    .message("Unbalanced parentheses or brackets in sentence.")
                    .messageVn("Dấu ngoặc trong câu chưa được đóng/mở cân xứng.")
                    .offset(contentStart)
                    .length(trimmedLen)
                    .replacements(List.of())
                    .build());
        }

        // 5. Extra consecutive spaces inside content bounds
        Matcher spaceMatcher = DOUBLE_SPACE_PATTERN.matcher(text);
        spaceMatcher.region(contentStart, contentEnd);
        if (spaceMatcher.find()) {
            issues.add(GrammarIssueDto.builder()
                    .ruleId("MULTIPLE_SPACES")
                    .type("PUNCTUATION")
                    .message("Multiple consecutive spaces found.")
                    .messageVn("Có nhiều khoảng trắng liên tiếp thừa trong câu.")
                    .offset(spaceMatcher.start())
                    .length(spaceMatcher.end() - spaceMatcher.start())
                    .replacements(List.of(" "))
                    .build());
        }

        boolean valid = issues.isEmpty();
        WritingItemValidationStatus status = valid
                ? WritingItemValidationStatus.APPROVED
                : WritingItemValidationStatus.NEEDS_REVIEW;
        String summaryVn = valid
                ? "Cấu trúc câu đạt chuẩn quy tắc chính tả và dấu câu cơ bản."
                : "Phát hiện " + issues.size() + " điểm cần lưu ý về hình thức/chính tả câu.";

        return GrammarValidationResultDto.builder()
                .valid(valid)
                .status(status)
                .summaryVn(summaryVn)
                .issues(issues)
                .build();
    }

    private void checkSentenceStartCapitalization(String text, int spanStart, int spanEnd, List<GrammarIssueDto> issues) {
        int idx = spanStart;
        while (idx < spanEnd && isOpeningPunctuation(text.charAt(idx))) {
            idx++;
        }
        if (idx >= spanEnd) {
            return;
        }
        char firstChar = text.charAt(idx);
        if (!Character.isLetter(firstChar) || Character.isUpperCase(firstChar)) {
            return;
        }

        int tokenEnd = idx;
        while (tokenEnd < spanEnd && !Character.isWhitespace(text.charAt(tokenEnd))) {
            tokenEnd++;
        }
        String rawToken = text.substring(idx, tokenEnd);
        String strippedToken = stripTrailingPunctuation(rawToken);
        if (LOWERCASE_START_ALLOWLIST.contains(rawToken) || LOWERCASE_START_ALLOWLIST.contains(strippedToken)) {
            return;
        }

        issues.add(GrammarIssueDto.builder()
                .ruleId("UPPERCASE_START")
                .type("PUNCTUATION")
                .message("Sentence should start with an uppercase letter.")
                .messageVn("Chữ cái đầu câu nên viết hoa.")
                .offset(idx)
                .length(1)
                .replacements(List.of(String.valueOf(Character.toUpperCase(firstChar))))
                .build());
    }

    private void checkTerminalPunctuation(String text, int spanStart, int spanEnd, List<GrammarIssueDto> issues) {
        int inspectIdx = spanEnd - 1;
        while (inspectIdx > spanStart && isClosingQuoteOrBracket(text.charAt(inspectIdx))) {
            inspectIdx--;
        }
        char terminalChar = text.charAt(inspectIdx);
        if (terminalChar != '.' && terminalChar != '!' && terminalChar != '?') {
            issues.add(GrammarIssueDto.builder()
                    .ruleId("TERMINAL_PUNCTUATION")
                    .type("PUNCTUATION")
                    .message("Sentence should end with proper punctuation (., !, or ?).")
                    .messageVn("Cuối câu cần có dấu chấm câu (., ! hoặc ?).")
                    .offset(inspectIdx)
                    .length(1)
                    .replacements(List.of(terminalChar + "."))
                    .build());
        }
    }

    private List<int[]> splitSentenceSpans(String text, int start, int end) {
        List<int[]> spans = new ArrayList<>();
        int sentenceStart = skipWhitespace(text, start, end);
        int i = sentenceStart;

        while (i < end) {
            char c = text.charAt(i);
            if (isSentenceTerminator(text, i, sentenceStart, end)) {
                int spanEnd = i + 1;
                while (spanEnd < end && isClosingQuoteOrBracket(text.charAt(spanEnd))) {
                    spanEnd++;
                }
                if (spanEnd >= end || Character.isWhitespace(text.charAt(spanEnd))) {
                    spans.add(new int[]{sentenceStart, spanEnd});
                    sentenceStart = skipWhitespace(text, spanEnd, end);
                    i = sentenceStart;
                    continue;
                }
            }
            i++;
        }

        if (sentenceStart < end) {
            int trailingEnd = end;
            while (trailingEnd > sentenceStart && Character.isWhitespace(text.charAt(trailingEnd - 1))) {
                trailingEnd--;
            }
            if (trailingEnd > sentenceStart) {
                spans.add(new int[]{sentenceStart, trailingEnd});
            }
        }

        if (spans.isEmpty() && start < end) {
            spans.add(new int[]{start, end});
        }
        return spans;
    }

    private boolean isSentenceTerminator(String text, int idx, int spanStart, int end) {
        char c = text.charAt(idx);
        if (c == '!' || c == '?') {
            return true;
        }
        if (c != '.') {
            return false;
        }
        // Do not split decimal numbers like 6.5
        if (idx > spanStart && idx + 1 < end
                && Character.isDigit(text.charAt(idx - 1))
                && Character.isDigit(text.charAt(idx + 1))) {
            return false;
        }
        // Do not split ellipsis ("...") on intermediate dots
        if (idx + 1 < end && text.charAt(idx + 1) == '.') {
            return false;
        }
        // Check common abbreviations ending in "."
        int wordStart = idx - 1;
        while (wordStart >= spanStart && !Character.isWhitespace(text.charAt(wordStart))
                && !isOpeningPunctuation(text.charAt(wordStart))) {
            wordStart--;
        }
        String tokenBeforeDot = text.substring(wordStart + 1, idx).toLowerCase(Locale.ROOT);
        if (ABBREVIATIONS_BEFORE_DOT.contains(tokenBeforeDot)) {
            return false;
        }
        return idx + 1 >= end
                || Character.isWhitespace(text.charAt(idx + 1))
                || isClosingQuoteOrBracket(text.charAt(idx + 1));
    }

    private boolean matchesSuggestedTerm(String term, List<String> draftTokens) {
        if (draftTokens.isEmpty()) {
            return false;
        }
        if (term.indexOf('[') >= 0 && term.indexOf(']') >= 0) {
            return matchesSentencePattern(term, draftTokens);
        }
        List<String> termTokens = extractPhraseTokensWithPlaceholders(term);
        if (termTokens.isEmpty()) {
            return false;
        }
        if (termTokens.size() == 1 && !PHRASE_GAP_PLACEHOLDERS.contains(termTokens.get(0))) {
            String single = termTokens.get(0);
            for (String draftToken : draftTokens) {
                if (wordsMatchWithInflection(single, draftToken)) {
                    return true;
                }
            }
            return false;
        }
        return matchesTokenSequenceWithGaps(termTokens, draftTokens, 0, 0, true);
    }

    private boolean matchesSentencePattern(String patternText, List<String> draftTokens) {
        String normalized = PATTERN_SLOT_REGEX.matcher(patternText).replaceAll(" __SLOT__ ");
        normalized = normalized.replace('+', ' ');
        Matcher m = Pattern.compile("__SLOT__|[A-Za-z0-9]+(?:['’][A-Za-z0-9]+)?").matcher(normalized);
        List<String> anchorTokens = new ArrayList<>();
        boolean hasSlot = false;
        while (m.find()) {
            String token = m.group();
            if ("__SLOT__".equals(token)) {
                hasSlot = true;
            } else {
                anchorTokens.add(token.toLowerCase(Locale.ROOT).replace('’', '\''));
            }
        }
        if (anchorTokens.isEmpty()) {
            return false;
        }
        int tokenIdx = 0;
        for (int aIdx = 0; aIdx < anchorTokens.size(); aIdx++) {
            String anchor = anchorTokens.get(aIdx);
            boolean matched = false;
            int searchLimit = (aIdx == 0 || hasSlot) ? draftTokens.size() : Math.min(draftTokens.size(), tokenIdx + 3);
            for (int j = tokenIdx; j < searchLimit; j++) {
                if (wordsMatchWithInflection(anchor, draftTokens.get(j))) {
                    tokenIdx = j + 1;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesTokenSequenceWithGaps(
            List<String> phraseTokens,
            List<String> draftTokens,
            int phraseIdx,
            int draftIdx,
            boolean firstLiteralSearch) {

        if (phraseIdx >= phraseTokens.size()) {
            return true;
        }

        String expected = phraseTokens.get(phraseIdx);
        if (PHRASE_GAP_PLACEHOLDERS.contains(expected)) {
            // Trailing placeholder (e.g. "prevent sb from sth") requires at least one remaining token
            if (phraseIdx == phraseTokens.size() - 1) {
                return draftIdx < draftTokens.size();
            }
            int maxGap = expected.equals("one's") || expected.equals("oneself") ? 3 : 5;
            int minGap = 1;
            for (int gap = minGap; gap <= maxGap && draftIdx + gap < draftTokens.size(); gap++) {
                if (matchesTokenSequenceWithGaps(phraseTokens, draftTokens, phraseIdx + 1, draftIdx + gap, false)) {
                    return true;
                }
            }
            return false;
        }

        if (firstLiteralSearch) {
            for (int start = draftIdx; start < draftTokens.size(); start++) {
                if (wordsMatchWithInflection(expected, draftTokens.get(start))) {
                    if (matchesTokenSequenceWithGaps(phraseTokens, draftTokens, phraseIdx + 1, start + 1, false)) {
                        return true;
                    }
                }
            }
            return false;
        }

        if (draftIdx < draftTokens.size() && wordsMatchWithInflection(expected, draftTokens.get(draftIdx))) {
            return matchesTokenSequenceWithGaps(phraseTokens, draftTokens, phraseIdx + 1, draftIdx + 1, false);
        }
        return false;
    }

    private boolean wordsMatchWithInflection(String lemma, String draftToken) {
        if (lemma.equals(draftToken)) {
            return true;
        }
        Set<String> irregularForms = IRREGULAR_VERBS.get(lemma);
        if (irregularForms != null && irregularForms.contains(draftToken)) {
            return true;
        }
        return buildRegularInflections(lemma).contains(draftToken);
    }

    private Set<String> buildRegularInflections(String base) {
        Set<String> forms = new HashSet<>();
        forms.add(base);
        forms.add(base + "s");
        forms.add(base + "es");
        forms.add(base + "ed");
        forms.add(base + "ing");

        if (base.endsWith("e") && base.length() > 2) {
            forms.add(base + "d");
            if (!base.endsWith("ee") && !base.endsWith("ye") && !base.endsWith("oe")) {
                forms.add(base.substring(0, base.length() - 1) + "ing");
            }
        }
        if (base.endsWith("ie") && base.length() > 2) {
            forms.add(base.substring(0, base.length() - 2) + "ying");
        }
        if (base.endsWith("y") && base.length() > 2 && !isVowel(base.charAt(base.length() - 2))) {
            String stem = base.substring(0, base.length() - 1);
            forms.add(stem + "ies");
            forms.add(stem + "ied");
            forms.add(stem + "ier");
            forms.add(stem + "iest");
        }
        if (shouldAllowDoubledConsonant(base)) {
            char last = base.charAt(base.length() - 1);
            forms.add(base + last + "ed");
            forms.add(base + last + "ing");
            forms.add(base + last + "er");
            forms.add(base + last + "est");
        }
        return forms;
    }

    private boolean shouldAllowDoubledConsonant(String base) {
        int len = base.length();
        if (len < 3) {
            return false;
        }
        char c1 = base.charAt(len - 3);
        char c2 = base.charAt(len - 2);
        char c3 = base.charAt(len - 1);
        return !isVowel(c1) && isVowel(c2) && !isVowel(c3)
                && c3 != 'w' && c3 != 'x' && c3 != 'y';
    }

    private boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    private List<String> extractNormalizedTokens(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        Matcher matcher = WORD_TOKEN_PATTERN.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group().toLowerCase(Locale.ROOT).replace('’', '\''));
        }
        return tokens;
    }

    private List<String> extractPhraseTokensWithPlaceholders(String phrase) {
        String cleaned = phrase.replace("v-ing", "vingplaceholder").replace("v-inf", "vinfplaceholder");
        List<String> raw = extractNormalizedTokens(cleaned);
        List<String> result = new ArrayList<>(raw.size());
        for (String token : raw) {
            if ("vingplaceholder".equals(token)) {
                result.add("v-ing");
            } else if ("vinfplaceholder".equals(token)) {
                result.add("v-inf");
            } else {
                result.add(token);
            }
        }
        return result;
    }

    private int findContentStart(String text) {
        int i = 0;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        return i;
    }

    private int findContentEnd(String text, int start) {
        int i = text.length();
        while (i > start && Character.isWhitespace(text.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    private int skipWhitespace(String text, int idx, int end) {
        while (idx < end && Character.isWhitespace(text.charAt(idx))) {
            idx++;
        }
        return idx;
    }

    private boolean isOpeningPunctuation(char c) {
        return c == '"' || c == '\'' || c == '“' || c == '‘' || c == '(' || c == '[';
    }

    private boolean isClosingQuoteOrBracket(char c) {
        return c == '"' || c == '\'' || c == '”' || c == '’' || c == ')' || c == ']';
    }

    private String stripTrailingPunctuation(String token) {
        int end = token.length();
        while (end > 0) {
            char c = token.charAt(end - 1);
            if (c == ',' || c == ';' || c == ':' || c == '!' || c == '?'
                    || (c == '.' && !"e.g.".equals(token) && !"i.e.".equals(token))
                    || isClosingQuoteOrBracket(c)) {
                end--;
            } else {
                break;
            }
        }
        return token.substring(0, end);
    }

    private boolean hasBalancedBrackets(String text, int start, int end) {
        int parens = 0;
        int brackets = 0;
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            if (c == '(') parens++;
            else if (c == ')') parens--;
            else if (c == '[') brackets++;
            else if (c == ']') brackets--;
            if (parens < 0 || brackets < 0) {
                return false;
            }
        }
        return parens == 0 && brackets == 0;
    }

    private static Map<String, Set<String>> buildIrregularVerbMap() {
        Map<String, Set<String>> map = new HashMap<>();
        map.put("be", Set.of("is", "am", "are", "was", "were", "been", "being"));
        map.put("have", Set.of("has", "had", "having"));
        map.put("do", Set.of("does", "did", "done", "doing"));
        map.put("go", Set.of("goes", "went", "gone", "going"));
        map.put("drive", Set.of("drives", "drove", "driven", "driving"));
        map.put("take", Set.of("takes", "took", "taken", "taking"));
        map.put("make", Set.of("makes", "made", "making"));
        map.put("give", Set.of("gives", "gave", "given", "giving"));
        map.put("get", Set.of("gets", "got", "gotten", "getting"));
        map.put("come", Set.of("comes", "came", "coming"));
        map.put("see", Set.of("sees", "saw", "seen", "seeing"));
        map.put("know", Set.of("knows", "knew", "known", "knowing"));
        map.put("think", Set.of("thinks", "thought", "thinking"));
        map.put("feel", Set.of("feels", "felt", "feeling"));
        map.put("find", Set.of("finds", "found", "finding"));
        map.put("become", Set.of("becomes", "became", "becoming"));
        map.put("begin", Set.of("begins", "began", "begun", "beginning"));
        map.put("break", Set.of("breaks", "broke", "broken", "breaking"));
        map.put("bring", Set.of("brings", "brought", "bringing"));
        map.put("build", Set.of("builds", "built", "building"));
        map.put("buy", Set.of("buys", "bought", "buying"));
        map.put("choose", Set.of("chooses", "chose", "chosen", "choosing"));
        map.put("draw", Set.of("draws", "drew", "drawn", "drawing"));
        map.put("drink", Set.of("drinks", "drank", "drunk", "drinking"));
        map.put("eat", Set.of("eats", "ate", "eaten", "eating"));
        map.put("fall", Set.of("falls", "fell", "fallen", "falling"));
        map.put("forbid", Set.of("forbids", "forbade", "forbidden", "forbidding"));
        map.put("forget", Set.of("forgets", "forgot", "forgotten", "forgetting"));
        map.put("grow", Set.of("grows", "grew", "grown", "growing"));
        map.put("hear", Set.of("hears", "heard", "hearing"));
        map.put("hold", Set.of("holds", "held", "holding"));
        map.put("keep", Set.of("keeps", "kept", "keeping"));
        map.put("lead", Set.of("leads", "led", "leading"));
        map.put("leave", Set.of("leaves", "left", "leaving"));
        map.put("lose", Set.of("loses", "lost", "losing"));
        map.put("mean", Set.of("means", "meant", "meaning"));
        map.put("meet", Set.of("meets", "met", "meeting"));
        map.put("pay", Set.of("pays", "paid", "paying"));
        map.put("put", Set.of("puts", "putting"));
        map.put("read", Set.of("reads", "reading"));
        map.put("ride", Set.of("rides", "rode", "ridden", "riding"));
        map.put("rise", Set.of("rises", "rose", "risen", "rising"));
        map.put("run", Set.of("runs", "ran", "running"));
        map.put("say", Set.of("says", "said", "saying"));
        map.put("sell", Set.of("sells", "sold", "selling"));
        map.put("send", Set.of("sends", "sent", "sending"));
        map.put("set", Set.of("sets", "setting"));
        map.put("show", Set.of("shows", "showed", "shown", "showing"));
        map.put("speak", Set.of("speaks", "spoke", "spoken", "speaking"));
        map.put("spend", Set.of("spends", "spent", "spending"));
        map.put("stand", Set.of("stands", "stood", "standing"));
        map.put("teach", Set.of("teaches", "taught", "teaching"));
        map.put("tell", Set.of("tells", "told", "telling"));
        map.put("understand", Set.of("understands", "understood", "understanding"));
        map.put("wear", Set.of("wears", "wore", "worn", "wearing"));
        map.put("win", Set.of("wins", "won", "winning"));
        map.put("write", Set.of("writes", "wrote", "written", "writing"));
        map.put("can", Set.of("could"));
        map.put("will", Set.of("would"));
        map.put("may", Set.of("might"));
        map.put("shall", Set.of("should"));
        return map;
    }
}
