package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.WritingItemValidationStatus;
import com.parrotalk.backend.dto.writing.GrammarIssueDto;
import com.parrotalk.backend.dto.writing.GrammarValidationResultDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight, zero-container-overhead structural and orthographic validation service
 * for AI-generated writing items and learner sentence drafts.
 */
@Service
public class GrammarValidationService {

    private static final Pattern REPEATED_WORD_PATTERN =
            Pattern.compile("(?i)\\b([a-z]{2,})\\s+\\1\\b");
    private static final Pattern DOUBLE_SPACE_PATTERN =
            Pattern.compile(" {2,}");

    /**
     * Validates a complete English sentence using deterministic structural and orthographic rules.
     */
    public GrammarValidationResultDto validateSentence(String sentence) {
        if (sentence == null || sentence.trim().length() < 6) {
            return GrammarValidationResultDto.builder()
                    .valid(false)
                    .status(WritingItemValidationStatus.REJECTED)
                    .summaryVn("Câu quá ngắn hoặc trống, vui lòng nhập đầy đủ một câu tiếng Anh hoàn chỉnh.")
                    .issues(List.of(GrammarIssueDto.builder()
                            .ruleId("TOO_SHORT")
                            .message("Sentence is empty or too short.")
                            .messageVn("Câu quá ngắn hoặc trống.")
                            .offset(0)
                            .length(sentence == null ? 0 : sentence.length())
                            .replacements(List.of())
                            .build()))
                    .build();
        }

        String trimmed = sentence.trim();
        List<GrammarIssueDto> issues = new ArrayList<>();

        // 1. Check first alphabetical character is uppercase
        char firstChar = trimmed.charAt(0);
        if (Character.isLetter(firstChar) && !Character.isUpperCase(firstChar)) {
            issues.add(GrammarIssueDto.builder()
                    .ruleId("UPPERCASE_START")
                    .message("Sentence should start with an uppercase letter.")
                    .messageVn("Chữ cái đầu câu nên viết hoa.")
                    .offset(0)
                    .length(1)
                    .replacements(List.of(String.valueOf(Character.toUpperCase(firstChar))))
                    .build());
        }

        // 2. Check terminal punctuation (. ! ? " ')
        char lastChar = trimmed.charAt(trimmed.length() - 1);
        if (lastChar != '.' && lastChar != '!' && lastChar != '?' && lastChar != '"' && lastChar != '\'') {
            issues.add(GrammarIssueDto.builder()
                    .ruleId("TERMINAL_PUNCTUATION")
                    .message("Sentence should end with proper punctuation (., !, or ?).")
                    .messageVn("Cuối câu cần có dấu chấm câu (., ! hoặc ?).")
                    .offset(trimmed.length() - 1)
                    .length(1)
                    .replacements(List.of(lastChar + "."))
                    .build());
        }

        // 3. Check repeated adjacent words (e.g. "drive drive")
        Matcher repeatedMatcher = REPEATED_WORD_PATTERN.matcher(trimmed);
        while (repeatedMatcher.find()) {
            String word = repeatedMatcher.group(1);
            issues.add(GrammarIssueDto.builder()
                    .ruleId("REPEATED_WORD")
                    .message("Repeated word detected: '" + word + "'.")
                    .messageVn("Phát hiện lặp từ liên tiếp: '" + word + "'.")
                    .offset(repeatedMatcher.start())
                    .length(repeatedMatcher.end() - repeatedMatcher.start())
                    .replacements(List.of(word))
                    .build());
        }

        // 4. Check balanced parentheses/brackets
        if (!hasBalancedBrackets(trimmed)) {
            issues.add(GrammarIssueDto.builder()
                    .ruleId("UNBALANCED_BRACKETS")
                    .message("Unbalanced parentheses or brackets in sentence.")
                    .messageVn("Dấu ngoặc trong câu chưa được đóng/mở cân xứng.")
                    .offset(0)
                    .length(trimmed.length())
                    .replacements(List.of())
                    .build());
        }

        // 5. Check extra consecutive spaces
        Matcher spaceMatcher = DOUBLE_SPACE_PATTERN.matcher(trimmed);
        if (spaceMatcher.find()) {
            issues.add(GrammarIssueDto.builder()
                    .ruleId("MULTIPLE_SPACES")
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

    /**
     * Validates a learner's draft sentence and checks which suggested vocabulary/collocation items were used.
     */
    public GrammarValidationResultDto validateUserDraft(String draftSentence, List<String> suggestedTerms) {
        GrammarValidationResultDto baseResult = validateSentence(draftSentence);
        if (baseResult.getStatus() == WritingItemValidationStatus.REJECTED) {
            return baseResult;
        }

        String lowerDraft = draftSentence.toLowerCase(Locale.ROOT);
        List<String> matchedTerms = new ArrayList<>();
        if (suggestedTerms != null) {
            for (String term : suggestedTerms) {
                if (term != null && !term.isBlank()) {
                    String cleanTerm = term.trim().toLowerCase(Locale.ROOT);
                    // Check exact phrase or stem match (first 5 chars for inflected verbs)
                    if (lowerDraft.contains(cleanTerm)
                            || (cleanTerm.length() >= 5 && !cleanTerm.contains(" ")
                                    && lowerDraft.contains(cleanTerm.substring(0, cleanTerm.length() - 1)))) {
                        matchedTerms.add(term.trim());
                    }
                }
            }
        }

        String keywordNote = matchedTerms.isEmpty()
                ? " Bạn có thể thử áp dụng thêm các từ vựng và collocations gợi ý ở trên vào câu."
                : " Bạn đã vận dụng tốt: " + String.join(", ", matchedTerms) + ".";

        return GrammarValidationResultDto.builder()
                .valid(baseResult.isValid())
                .status(baseResult.getStatus())
                .summaryVn(baseResult.getSummaryVn() + keywordNote)
                .issues(baseResult.getIssues())
                .build();
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

    private boolean hasBalancedBrackets(String text) {
        int parens = 0;
        int brackets = 0;
        for (int i = 0; i < text.length(); i++) {
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
}

