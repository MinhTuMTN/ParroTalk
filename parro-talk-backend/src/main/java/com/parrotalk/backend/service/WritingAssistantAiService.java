package com.parrotalk.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.parrotalk.backend.constant.DifficultyScale;
import com.parrotalk.backend.constant.WritingMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI service for analyzing a learner's Vietnamese or English idea and generating
 * level-calibrated vocabulary, expressions, collocations, sentence patterns,
 * combination guide, and complete reference sentences in a single LLM request.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WritingAssistantAiService {

    private final AiCoreClient aiCoreClient;

    private static final String SUGGESTION_SYSTEM_PROMPT = """
            You are an expert English writing coach for Vietnamese learners.
            Mode: SUGGESTION MODE (Fast Lexical & Pattern Hints ONLY — DO NOT reveal complete sentences or lengthy explanations).

            CRITICAL LEVEL CALIBRATION RULES:
            - CEFR A1 / A2 or IELTS 4.0 - 4.5: Use ONLY basic everyday words and simple clauses (because, and, so, should not, cannot). No academic jargon.
            - CEFR B1 / B2 or IELTS 5.0 - 6.5: Use natural intermediate vocabulary, standard collocations, and compound/complex clauses.
            - CEFR C1 / C2 or IELTS 7.0 - 8.0: Use precise academic/formal vocabulary, strong collocations, and advanced syntactic structures.

            OUTPUT REQUIREMENTS (Single compact JSON Object — keep Vietnamese strings ultra-concise, max 6-10 words):
            1. detectedLanguage: "VI", "EN", or "MIXED".
            2. coreIdeaSummaryVn: 1 short Vietnamese sentence summarizing the core idea.
            3. vocabulary: 4 key words calibrated to the target level:
               - word (English lemma)
               - phonetic (standard IPA pronunciation, e.g. "/ˌkɒm.bɪˈneɪ.ʃən/")
               - partOfSpeech ("noun", "verb", "adjective", "adverb")
               - cefrLevel ("A1".."C2")
               - meaningVn (short Vietnamese meaning, 2-5 words)
               - usageContextVn (brief grammar/collocation note, 3-6 words)
            4. expressions: 2 idiomatic phrases or phrasal verbs (fields: phrase, phonetic, cefrLevel, meaningVn, usageContextVn).
            5. collocations: 3 natural word partnerships (fields: phrase, phonetic, cefrLevel, meaningVn, usageContextVn).
            6. sentencePatterns: 2 sentence templates with bracketed slots (e.g., "[Subject] + should not + [V-inf] + because + [Clause].") with short meaningVn and usageContextVn. NEVER output full completed sentences.

            DO NOT include combinationGuideVn, completeSentences, explanationVn, exampleEn, or exampleVn.
            Output ONLY valid JSON with no markdown fences.
            """;

    private static final String DRAFT_REVIEW_SYSTEM_PROMPT = """
            You are an expert English grammar checker and writing coach for Vietnamese learners.
            Task: Strictly review a learner's English draft sentence/paragraph against their core idea, proficiency level, and suggested target items.

            STRICT EVALUATION RULES:
            1. Judge grammar, spelling, word choice, and punctuation accurately and strictly. Catch real errors such as subject-verb disagreement ("She go" -> "She went"), wrong tense ("go ... yesterday"), missing/wrong articles or prepositions, fragment/run-on clauses, and misspellings.
            2. Do NOT flag valid US or UK spelling/grammar variants (e.g., color/colour, organize/organise, canceled/cancelled, gotten/got, optional Oxford comma) and do NOT invent errors if the draft is already grammatical and natural.
            3. If the draft has no errors, return "issues": [] and set "score" high (90-100).
            4. Evaluate each item in "suggestedItems" (including vocabulary words, collocations, expressions, and bracketed sentence patterns like "[Subject] + should not + [V-inf]") in ANY valid grammatical inflection, tense, plural, or filled-slot form.
            5. Keep all "explanationVn", "note", and "overallVn" concise and natural in Vietnamese.

            OUTPUT REQUIREMENTS (Single compact JSON Object, no markdown fences):
            - correctedSentence: string (the grammatically corrected natural version of the learner's draft)
            - issues: array of objects, each with:
              - type: "GRAMMAR" | "SPELLING" | "WORD_CHOICE" | "PUNCTUATION"
              - original: string (exact problematic substring from the learner's draft)
              - suggestion: string (recommended replacement)
              - explanationVn: string (clear concise explanation in Vietnamese)
            - score: integer 0-100 (overall quality score based on accuracy, naturalness, and idea clarity)
            - usedItems: array of objects matching suggestedItems, each with:
              - text: string (suggested item text)
              - used: boolean (true if used in the draft, including inflections or filled pattern slots)
              - correctUsage: boolean (true if used both present AND grammatically/semantically accurate)
              - note: string (brief Vietnamese note on usage or null/empty if straightforward)
            - overallVn: string (1 short Vietnamese overall feedback sentence)
            """;

    private static final String GUIDANCE_SYSTEM_PROMPT = """
            You are an expert English writing coach and IELTS/CEFR curriculum designer for Vietnamese learners.
            Mode: GUIDANCE MODE (Comprehensive Step-by-Step Writing Guidance & Model Sentences).

            CRITICAL LEVEL CALIBRATION RULES:
            - CEFR A1 / A2 or IELTS 4.0 - 4.5: Use ONLY basic, everyday vocabulary and simple sentence structures (Subject + Verb + Object, simple conjunctions like 'because', 'and', 'so', modal verbs 'should not', 'cannot'). NEVER use academic or C1/C2 jargon.
            - CEFR B1 / B2 or IELTS 5.0 - 6.5: Use clear, natural intermediate vocabulary, standard collocations, and compound/complex sentences (relative clauses 'who/which', cause-effect linkers 'because/since/as a result').
            - CEFR C1 / C2 or IELTS 7.0 - 8.0: Use precise, sophisticated academic/formal vocabulary, strong collocations, and advanced syntactic structures (participial clauses, nominalization, parallel formal clauses).

            OUTPUT REQUIREMENTS (Single JSON Object - keep all Vietnamese strings concise, max 12-16 words per field):
            1. detectedLanguage: "VI", "EN", or "MIXED".
            2. coreIdeaSummaryVn: 1 short Vietnamese sentence summarizing the core idea.
            3. combinationGuideVn: 3 short numbered steps in Vietnamese on how to combine the suggested vocabulary, collocations, and patterns.
            4. vocabulary: 4 key words calibrated to the target level:
               - word (English lemma)
               - phonetic (standard IPA pronunciation, e.g. "/ˌkɒm.bɪˈneɪ.ʃən/")
               - partOfSpeech ("noun", "verb", "adjective", "adverb")
               - cefrLevel ("A1".."C2")
               - meaningVn (short Vietnamese meaning, 2-5 words)
               - explanationVn (1 brief Vietnamese phrase on why it fits)
               - usageContextVn (short grammar/register note, 3-8 words)
               - exampleEn (1 short illustrative sentence <= 12 words)
               - exampleVn (Vietnamese translation of exampleEn)
            5. expressions: 2 idiomatic phrases or phrasal verbs relevant to the idea (fields: phrase, phonetic, cefrLevel, meaningVn, explanationVn, usageContextVn, exampleEn, exampleVn).
            6. collocations: 3 natural word partnerships needed for this idea (fields: phrase, phonetic, cefrLevel, meaningVn, explanationVn, usageContextVn, exampleEn, exampleVn).
            7. sentencePatterns: 2 sentence templates using bracketed slots (e.g., "[Subject] + should not + [V-inf] + because + [Clause].") with short meaningVn, explanationVn, and usageContextVn. NEVER put the full completed sentence inside sentencePatterns.
            8. completeSentences: 2 complete English sentences expressing the full idea at the target level (one "Natural / Standard" and one "Formal / IELTS Writing"), each with:
               - tone ("Natural / Standard" or "Formal / IELTS Writing")
               - sentenceEn (complete English sentence)
               - translationVn (Vietnamese translation)
               - grammarBreakdownVn (concise 1-sentence clause/grammar breakdown)
               - whyItWorksVn (concise 1-sentence note on why it works at this level)

            Output ONLY a valid JSON object matching the schema, with no markdown code fences or extra commentary.
            """;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiWritingAnalysisPayload {
        @JsonProperty("detectedLanguage")
        private String detectedLanguage;

        @JsonProperty("coreIdeaSummaryVn")
        private String coreIdeaSummaryVn;

        @JsonProperty("combinationGuideVn")
        private String combinationGuideVn;

        @JsonProperty("vocabulary")
        @Builder.Default
        private List<AiLexicalItem> vocabulary = new ArrayList<>();

        @JsonProperty("expressions")
        @Builder.Default
        private List<AiLexicalItem> expressions = new ArrayList<>();

        @JsonProperty("collocations")
        @Builder.Default
        private List<AiLexicalItem> collocations = new ArrayList<>();

        @JsonProperty("sentencePatterns")
        @Builder.Default
        private List<AiPatternItem> sentencePatterns = new ArrayList<>();

        @JsonProperty("completeSentences")
        @Builder.Default
        private List<AiCompleteSentenceItem> completeSentences = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiLexicalItem {
        @JsonProperty("word")
        private String word;

        @JsonProperty("phrase")
        private String phrase;

        @JsonProperty("phonetic")
        private String phonetic;

        @JsonProperty("partOfSpeech")
        private String partOfSpeech;

        @JsonProperty("cefrLevel")
        private String cefrLevel;

        @JsonProperty("meaningVn")
        private String meaningVn;

        @JsonProperty("explanationVn")
        private String explanationVn;

        @JsonProperty("usageContextVn")
        private String usageContextVn;

        @JsonProperty("exampleEn")
        private String exampleEn;

        @JsonProperty("exampleVn")
        private String exampleVn;

        public String resolvedText() {
            if (word != null && !word.isBlank()) {
                return word.trim();
            }
            if (phrase != null && !phrase.isBlank()) {
                return phrase.trim();
            }
            return "";
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiPatternItem {
        @JsonProperty("pattern")
        private String pattern;

        @JsonProperty("meaningVn")
        private String meaningVn;

        @JsonProperty("explanationVn")
        private String explanationVn;

        @JsonProperty("usageContextVn")
        private String usageContextVn;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiCompleteSentenceItem {
        @JsonProperty("tone")
        private String tone;

        @JsonProperty("sentenceEn")
        private String sentenceEn;

        @JsonProperty("translationVn")
        private String translationVn;

        @JsonProperty("grammarBreakdownVn")
        private String grammarBreakdownVn;

        @JsonProperty("whyItWorksVn")
        private String whyItWorksVn;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiSuggestionPayload {
        @JsonProperty("detectedLanguage")
        private String detectedLanguage;

        @JsonProperty("coreIdeaSummaryVn")
        private String coreIdeaSummaryVn;

        @JsonProperty("vocabulary")
        @Builder.Default
        private List<AiSuggestionLexicalItem> vocabulary = new ArrayList<>();

        @JsonProperty("expressions")
        @Builder.Default
        private List<AiSuggestionLexicalItem> expressions = new ArrayList<>();

        @JsonProperty("collocations")
        @Builder.Default
        private List<AiSuggestionLexicalItem> collocations = new ArrayList<>();

        @JsonProperty("sentencePatterns")
        @Builder.Default
        private List<AiSuggestionPatternItem> sentencePatterns = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiSuggestionLexicalItem {
        @JsonProperty("word")
        private String word;

        @JsonProperty("phrase")
        private String phrase;

        @JsonProperty("phonetic")
        private String phonetic;

        @JsonProperty("partOfSpeech")
        private String partOfSpeech;

        @JsonProperty("cefrLevel")
        private String cefrLevel;

        @JsonProperty("meaningVn")
        private String meaningVn;

        @JsonProperty("usageContextVn")
        private String usageContextVn;

        public AiLexicalItem toFullItem() {
            return AiLexicalItem.builder()
                    .word(word)
                    .phrase(phrase)
                    .phonetic(phonetic)
                    .partOfSpeech(partOfSpeech)
                    .cefrLevel(cefrLevel)
                    .meaningVn(meaningVn)
                    .usageContextVn(usageContextVn)
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiSuggestionPatternItem {
        @JsonProperty("pattern")
        private String pattern;

        @JsonProperty("meaningVn")
        private String meaningVn;

        @JsonProperty("usageContextVn")
        private String usageContextVn;

        public AiPatternItem toFullPattern() {
            return AiPatternItem.builder()
                    .pattern(pattern)
                    .meaningVn(meaningVn)
                    .usageContextVn(usageContextVn)
                    .build();
        }
    }

    /**
     * Executes a single structured AI call to analyze the learner's idea at the specified difficulty level.
     * Uses a lightweight JSON schema for SUGGESTION mode and the full JSON schema for GUIDANCE mode.
     */
    public AiWritingAnalysisPayload analyzeIdea(
            String inputText,
            WritingMode mode,
            DifficultyScale scaleType,
            String targetLevel) {

        String levelDescriptor = scaleType + " " + targetLevel + " (" + buildLevelGuidance(scaleType, targetLevel) + ")";
        log.info("Calling AI for Writing Assistant: mode={}, level={} {}", mode, scaleType, targetLevel);

        if (mode == WritingMode.SUGGESTION) {
            String userPrompt = String.format("""
                    Learner Input Idea: "%s"
                    Target Proficiency Level: %s

                    Return ONLY the lightweight Suggestion Mode JSON (detectedLanguage, coreIdeaSummaryVn, vocabulary, expressions, collocations, sentencePatterns) calibrated to %s.
                    """,
                    inputText.trim(),
                    levelDescriptor,
                    scaleType + " " + targetLevel);

            return aiCoreClient.callAndValidate(
                    SUGGESTION_SYSTEM_PROMPT,
                    userPrompt,
                    AiSuggestionPayload.class,
                    raw -> {
                        if (raw == null) {
                            throw new IllegalArgumentException("AI returned null suggestion payload");
                        }
                        if (raw.getVocabulary() == null || raw.getVocabulary().isEmpty()) {
                            throw new IllegalArgumentException("AI returned empty vocabulary list");
                        }
                        if (raw.getSentencePatterns() == null || raw.getSentencePatterns().isEmpty()) {
                            throw new IllegalArgumentException("AI returned empty sentencePatterns list");
                        }
                        return AiWritingAnalysisPayload.builder()
                                .detectedLanguage(raw.getDetectedLanguage() != null && !raw.getDetectedLanguage().isBlank()
                                        ? raw.getDetectedLanguage() : "VI")
                                .coreIdeaSummaryVn(raw.getCoreIdeaSummaryVn())
                                .vocabulary(raw.getVocabulary().stream().map(AiSuggestionLexicalItem::toFullItem).toList())
                                .expressions(raw.getExpressions() == null ? List.of()
                                        : raw.getExpressions().stream().map(AiSuggestionLexicalItem::toFullItem).toList())
                                .collocations(raw.getCollocations() == null ? List.of()
                                        : raw.getCollocations().stream().map(AiSuggestionLexicalItem::toFullItem).toList())
                                .sentencePatterns(raw.getSentencePatterns().stream().map(AiSuggestionPatternItem::toFullPattern).toList())
                                .completeSentences(List.of())
                                .build();
                    }
            );
        }

        String userPrompt = String.format("""
                Learner Input Idea: "%s"
                Target Proficiency Level: %s

                Analyze this idea and return the full Guidance Mode JSON object (detectedLanguage, coreIdeaSummaryVn, combinationGuideVn, vocabulary, expressions, collocations, sentencePatterns, completeSentences) strictly calibrated to %s.
                """,
                inputText.trim(),
                levelDescriptor,
                scaleType + " " + targetLevel);

        return aiCoreClient.callAndValidate(
                GUIDANCE_SYSTEM_PROMPT,
                userPrompt,
                AiWritingAnalysisPayload.class,
                raw -> {
                    if (raw == null) {
                        throw new IllegalArgumentException("AI returned null writing analysis payload");
                    }
                    if (raw.getVocabulary() == null || raw.getVocabulary().isEmpty()) {
                        throw new IllegalArgumentException("AI returned empty vocabulary list");
                    }
                    if (raw.getSentencePatterns() == null || raw.getSentencePatterns().isEmpty()) {
                        throw new IllegalArgumentException("AI returned empty sentencePatterns list");
                    }
                    if (raw.getCompleteSentences() == null || raw.getCompleteSentences().isEmpty()) {
                        throw new IllegalArgumentException("AI returned empty completeSentences list in GUIDANCE mode");
                    }
                    if (raw.getDetectedLanguage() == null || raw.getDetectedLanguage().isBlank()) {
                        raw.setDetectedLanguage("VI");
                    }
                    if (raw.getExpressions() == null) {
                        raw.setExpressions(new ArrayList<>());
                    }
                    if (raw.getCollocations() == null) {
                        raw.setCollocations(new ArrayList<>());
                    }
                    return raw;
                }
        );
    }

    /**
     * Reviews a learner's English draft using the configured LLM for grammar, spelling,
     * word choice, punctuation, corrected sentence, score, and suggested-item usage.
     */
    public AiDraftReviewPayload reviewDraft(
            String draft,
            String coreIdeaSummaryVn,
            DifficultyScale scaleType,
            String targetLevel,
            List<String> suggestedItems) {
        if (draft == null || draft.isBlank()) {
            throw new IllegalArgumentException("Draft cannot be blank for AI review");
        }

        DifficultyScale resolvedScale = scaleType != null ? scaleType : DifficultyScale.CEFR;
        String resolvedLevel = (targetLevel != null && !targetLevel.isBlank()) ? targetLevel.trim() : "B1";
        String levelDescriptor = resolvedScale + " " + resolvedLevel + " (" + buildLevelGuidance(resolvedScale, resolvedLevel) + ")";
        String coreIdea = (coreIdeaSummaryVn != null && !coreIdeaSummaryVn.isBlank())
                ? coreIdeaSummaryVn.trim()
                : "(not provided)";
        String suggestedList = (suggestedItems == null || suggestedItems.isEmpty())
                ? "(none)"
                : String.join(", ", suggestedItems);

        String userPrompt = String.format("""
                Learner Draft: "%s"
                Core Idea (Vietnamese): "%s"
                Target Proficiency Level: %s
                Suggested Items to Check: [%s]

                Strictly evaluate grammar, spelling, word choice, punctuation, overall score (0-100), correctedSentence, and usage of each suggested item. Return ONLY the JSON object.
                """,
                draft.trim(),
                coreIdea,
                levelDescriptor,
                suggestedList);

        return aiCoreClient.callAndValidate(
                DRAFT_REVIEW_SYSTEM_PROMPT,
                userPrompt,
                AiDraftReviewPayload.class,
                raw -> {
                    if (raw == null) {
                        throw new IllegalArgumentException("AI returned null draft review payload");
                    }
                    List<AiDraftIssue> normalizedIssues = new ArrayList<>();
                    if (raw.getIssues() != null) {
                        for (AiDraftIssue issue : raw.getIssues()) {
                            if (issue == null) {
                                continue;
                            }
                            issue.setType(normalizeIssueType(issue.getType()));
                            normalizedIssues.add(issue);
                        }
                    }
                    raw.setIssues(normalizedIssues);

                    List<AiUsedItemEvaluation> normalizedUsed = new ArrayList<>();
                    if (raw.getUsedItems() != null) {
                        for (AiUsedItemEvaluation item : raw.getUsedItems()) {
                            if (item != null && item.getText() != null && !item.getText().isBlank()) {
                                normalizedUsed.add(item);
                            }
                        }
                    }
                    raw.setUsedItems(normalizedUsed);

                    if (raw.getScore() != null) {
                        raw.setScore(Math.max(0, Math.min(100, raw.getScore())));
                    }
                    if ((raw.getCorrectedSentence() == null || raw.getCorrectedSentence().isBlank())
                            && normalizedIssues.isEmpty()) {
                        raw.setCorrectedSentence(draft.trim());
                    }
                    if (raw.getOverallVn() == null) {
                        raw.setOverallVn("");
                    }
                    return raw;
                }
        );
    }

    private String normalizeIssueType(String type) {
        if (type == null || type.isBlank()) {
            return "GRAMMAR";
        }
        String upper = type.trim().toUpperCase();
        return switch (upper) {
            case "GRAMMAR", "SPELLING", "WORD_CHOICE", "PUNCTUATION" -> upper;
            default -> "GRAMMAR";
        };
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiDraftReviewPayload {
        @JsonProperty("correctedSentence")
        private String correctedSentence;

        @JsonProperty("issues")
        @Builder.Default
        private List<AiDraftIssue> issues = new ArrayList<>();

        @JsonProperty("score")
        private Integer score;

        @JsonProperty("usedItems")
        @Builder.Default
        private List<AiUsedItemEvaluation> usedItems = new ArrayList<>();

        @JsonProperty("overallVn")
        private String overallVn;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiDraftIssue {
        @JsonProperty("type")
        private String type;

        @JsonProperty("original")
        private String original;

        @JsonProperty("suggestion")
        private String suggestion;

        @JsonProperty("explanationVn")
        private String explanationVn;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiUsedItemEvaluation {
        @JsonProperty("text")
        private String text;

        @JsonProperty("used")
        private Boolean used;

        @JsonProperty("correctUsage")
        private Boolean correctUsage;

        @JsonProperty("note")
        private String note;
    }

    private String buildLevelGuidance(DifficultyScale scaleType, String targetLevel) {
        if (scaleType == DifficultyScale.CEFR) {
            return switch (targetLevel.toUpperCase()) {
                case "A1", "A2" -> "Beginner/Elementary: use simple everyday words, short clauses, basic connectors like because/and/so; avoid complex academic vocabulary";
                case "B1", "B2" -> "Intermediate/Upper-Intermediate: use natural common collocations, clear cause-and-effect clauses, moderate lexical variety";
                case "C1", "C2" -> "Advanced/Mastery: use precise academic/formal vocabulary, strong idiomatic collocations, sophisticated complex structures";
                default -> "Standard natural English";
            };
        }
        try {
            double band = Double.parseDouble(targetLevel);
            if (band <= 4.5) {
                return "Elementary IELTS (Band 4.0-4.5): keep vocabulary simple and familiar, focus on clear basic sentence structures without academic jargon";
            } else if (band <= 6.0) {
                return "Intermediate IELTS (Band 5.0-6.0): use clear topic vocabulary, standard collocations, and accurate compound/complex sentences";
            } else {
                return "Advanced IELTS (Band 6.5-8.0): use natural academic collocations, precise lexical resource, and high-scoring grammatical range";
            }
        } catch (NumberFormatException e) {
            return "Standard IELTS Writing level";
        }
    }
}

