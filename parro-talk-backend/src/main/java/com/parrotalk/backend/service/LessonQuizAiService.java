package com.parrotalk.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.parrotalk.backend.constant.QuizGradingStatus;
import com.parrotalk.backend.constant.QuizMode;
import com.parrotalk.backend.constant.QuizQuestionType;
import com.parrotalk.backend.entity.TranscriptionSegment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AI service for generating listening comprehension quizzes from lesson transcripts
 * and semantically grading fill-in-the-blank answers.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LessonQuizAiService {

    static final int MAX_TRANSCRIPT_CHARS = 12_000;
    private static final List<String> QUIZ_PROVIDER_ORDER = List.of("groq", "gemini", "gemini-vocab");

    private final AiCoreClient aiCoreClient;

    private static final String GENERATE_SYSTEM_PROMPT = """
            You are an expert English listening comprehension teacher and curriculum designer for Vietnamese learners.
            Task: Given an indexed English lesson transcript, generate a high-quality listening comprehension quiz.

            CRITICAL RULES:
            1. Every question MUST test genuine comprehension of ideas, facts, cause-effect, speaker intent, or key details in the transcript (NOT trivial rote transcription or grammar trivia).
            2. Return EXACTLY the requested number of questions and follow the requested mode:
               - MULTIPLE_CHOICE: every question has questionType="MULTIPLE_CHOICE", 4 plausible options in "options", "correctIndex" (0..3), "modelAnswer" equal to options[correctIndex], "explanationVn" in clear concise Vietnamese, and "evidenceSegmentIndex" (integer index from the transcript).
               - FILL_IN_THE_BLANK: every question has questionType="FILL_IN_THE_BLANK", "questionText" containing "___" testing comprehension of a key idea/fact/detail (NOT mechanical dictation of random function words), "options": [], "correctIndex": null, "modelAnswer" (short natural English answer 1-5 words), "explanationVn" in concise Vietnamese, and "evidenceSegmentIndex" (integer index from the transcript).
               - MIXED: mix roughly 50% MULTIPLE_CHOICE and 50% FILL_IN_THE_BLANK (e.g. for 5 questions: 3 MULTIPLE_CHOICE + 2 FILL_IN_THE_BLANK; for 3 questions: 2 MULTIPLE_CHOICE + 1 FILL_IN_THE_BLANK; for 8: 4 + 4; for 10: 5 + 5).
            3. NEVER output UUIDs. For evidence, return ONLY "evidenceSegmentIndex" (the integer inside [index] from the transcript).
            4. Spread questions across different parts of the listening passage (beginning, middle, end) when possible.
            5. Output ONLY a valid JSON object matching the schema, with no markdown code fences or commentary.
            """;

    private static final String GRADE_SYSTEM_PROMPT = """
            You are an expert English listening comprehension grader for Vietnamese learners.
            Task: Grade the learner's fill-in-the-blank comprehension answer semantically against the question, model answer, and transcript evidence.

            GRADING RULES:
            1. Focus on meaning and comprehension of the listening passage, NOT exact word-for-word matching.
            2. Accept valid synonyms, equivalent phrasing, acceptable grammatical inflections, or very minor spelling typos as CORRECT (score 85-100).
            3. Award PARTIAL (score 40-84) if the learner captured part of the meaning or a somewhat imprecise/incomplete idea.
            4. Award INCORRECT (score 0-39) if the answer is wrong, contradicts the evidence, or is irrelevant.
            5. Provide concise, helpful Vietnamese feedback in "feedback" (1-2 short sentences explaining why it is correct/partial/incorrect).
            6. Output ONLY a valid JSON object with fields: "status" ("CORRECT" | "PARTIAL" | "INCORRECT"), "score" (integer 0..100), "feedback" (Vietnamese string). No markdown fences.
            """;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiQuizGenerationPayload {
        @JsonProperty("questions")
        @Builder.Default
        private List<AiQuizQuestionItem> questions = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiQuizQuestionItem {
        @JsonProperty("questionType")
        private String questionType;

        @JsonProperty("questionText")
        private String questionText;

        @JsonProperty("options")
        @Builder.Default
        private List<String> options = new ArrayList<>();

        @JsonProperty("correctIndex")
        private Integer correctIndex;

        @JsonProperty("modelAnswer")
        private String modelAnswer;

        @JsonProperty("explanationVn")
        private String explanationVn;

        @JsonProperty("evidenceSegmentIndex")
        private Integer evidenceSegmentIndex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiGradePayload {
        @JsonProperty("status")
        private String status;

        @JsonProperty("score")
        private Integer score;

        @JsonProperty("feedback")
        private String feedback;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedQuestion {
        private int displayOrder;
        private QuizQuestionType questionType;
        private String questionText;
        private List<String> options;
        private Short correctIndex;
        private String modelAnswer;
        private String explanationVn;
        private TranscriptionSegment evidenceSegment;
        private String evidenceText;
        private Double evidenceStartTime;
        private Double evidenceEndTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradingResult {
        private QuizGradingStatus status;
        private int score;
        private String feedback;
    }

    /**
     * Generates listening comprehension questions from indexed transcript segments.
     */
    public List<GeneratedQuestion> generateQuestions(
            String lessonTitle,
            List<TranscriptionSegment> segments,
            QuizMode mode,
            int targetCount) {
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("Transcript segments cannot be empty");
        }
        int safeTargetCount = Math.max(1, targetCount);
        String formattedTranscript = formatTranscriptForPrompt(segments, MAX_TRANSCRIPT_CHARS);
        String title = (lessonTitle != null && !lessonTitle.isBlank()) ? lessonTitle.trim() : "Untitled Lesson";

        String modeGuidance = switch (mode) {
            case MULTIPLE_CHOICE -> "All " + safeTargetCount + " questions MUST have questionType='MULTIPLE_CHOICE' with exactly 4 options.";
            case FILL_IN_THE_BLANK -> "All " + safeTargetCount + " questions MUST have questionType='FILL_IN_THE_BLANK' with '___' in questionText.";
            case MIXED -> {
                int mcqCount = (safeTargetCount + 1) / 2;
                int fibCount = safeTargetCount - mcqCount;
                yield "Generate EXACTLY " + mcqCount + " MULTIPLE_CHOICE and " + fibCount + " FILL_IN_THE_BLANK questions (total " + safeTargetCount + ").";
            }
        };

        String userPrompt = String.format("""
                Lesson Title: "%s"
                Quiz Mode: %s
                Target Question Count: %d
                Mode Distribution Requirement: %s

                Indexed Transcript Segments:
                %s

                Return ONLY JSON: { "questions": [ { "questionType": "...", "questionText": "...", "options": [...], "correctIndex": 0, "modelAnswer": "...", "explanationVn": "...", "evidenceSegmentIndex": 0 } ] }
                """,
                title,
                mode.name(),
                safeTargetCount,
                modeGuidance,
                formattedTranscript);

        AiQuizGenerationPayload payload = aiCoreClient.callAndValidate(
                QUIZ_PROVIDER_ORDER,
                GENERATE_SYSTEM_PROMPT,
                userPrompt,
                AiQuizGenerationPayload.class,
                raw -> validateGenerationPayload(raw, mode, safeTargetCount));

        List<GeneratedQuestion> result = new ArrayList<>(payload.getQuestions().size());
        for (int i = 0; i < payload.getQuestions().size(); i++) {
            AiQuizQuestionItem item = payload.getQuestions().get(i);
            QuizQuestionType qType = parseQuestionType(item.getQuestionType(), mode);

            int rawIdx = item.getEvidenceSegmentIndex() != null ? item.getEvidenceSegmentIndex() : 0;
            int clampedIdx = Math.max(0, Math.min(segments.size() - 1, rawIdx));
            TranscriptionSegment evidence = segments.get(clampedIdx);

            List<String> normalizedOptions = null;
            Short normalizedCorrectIndex = null;
            String normalizedModelAnswer;

            if (qType == QuizQuestionType.MULTIPLE_CHOICE) {
                normalizedOptions = item.getOptions().stream()
                        .map(opt -> opt == null ? "" : opt.trim())
                        .toList();
                int idx = Math.max(0, Math.min(3, item.getCorrectIndex()));
                normalizedCorrectIndex = (short) idx;
                normalizedModelAnswer = normalizedOptions.get(idx);
            } else {
                normalizedModelAnswer = item.getModelAnswer().trim();
            }

            result.add(GeneratedQuestion.builder()
                    .displayOrder(i + 1)
                    .questionType(qType)
                    .questionText(item.getQuestionText().trim())
                    .options(normalizedOptions)
                    .correctIndex(normalizedCorrectIndex)
                    .modelAnswer(normalizedModelAnswer)
                    .explanationVn(item.getExplanationVn() != null ? item.getExplanationVn().trim() : "")
                    .evidenceSegment(evidence)
                    .evidenceText(evidence.getText())
                    .evidenceStartTime(evidence.getStartTime())
                    .evidenceEndTime(evidence.getEndTime())
                    .build());
        }
        return result;
    }

    /**
     * Grades a fill-in-the-blank answer using exact normalized fast-path first, then semantic AI grading.
     */
    public GradingResult gradeFillInTheBlank(
            String questionText,
            String modelAnswer,
            String explanationVn,
            String evidenceText,
            String userAnswer) {
        String trimmedUser = userAnswer == null ? "" : userAnswer.trim();
        if (trimmedUser.isEmpty()) {
            return GradingResult.builder()
                    .status(QuizGradingStatus.INCORRECT)
                    .score(0)
                    .feedback("Bạn chưa nhập câu trả lời." + formatExplanationSuffix(explanationVn))
                    .build();
        }

        String normalizedUser = normalizeAnswerForExactMatch(trimmedUser);
        String normalizedModel = normalizeAnswerForExactMatch(modelAnswer);
        if (!normalizedModel.isEmpty() && normalizedUser.equals(normalizedModel)) {
            return GradingResult.builder()
                    .status(QuizGradingStatus.CORRECT)
                    .score(100)
                    .feedback("Chính xác!" + formatExplanationSuffix(explanationVn))
                    .build();
        }

        String userPrompt = String.format("""
                Question: "%s"
                Model Answer: "%s"
                Transcript Evidence: "%s"
                Learner Answer: "%s"

                Evaluate semantic accuracy and return ONLY JSON: { "status": "CORRECT|PARTIAL|INCORRECT", "score": 0..100, "feedback": "..." }.
                """,
                questionText != null ? questionText.trim() : "",
                modelAnswer != null ? modelAnswer.trim() : "",
                evidenceText != null ? evidenceText.trim() : "",
                trimmedUser);

        AiGradePayload grade = aiCoreClient.callAndValidate(
                QUIZ_PROVIDER_ORDER,
                GRADE_SYSTEM_PROMPT,
                userPrompt,
                AiGradePayload.class,
                raw -> {
                    if (raw == null || raw.getScore() == null) {
                        throw new IllegalArgumentException("AI returned invalid grading payload");
                    }
                    return raw;
                });

        int clampedScore = Math.max(0, Math.min(100, grade.getScore()));
        QuizGradingStatus resolvedStatus = statusForScore(clampedScore);
        String feedback = (grade.getFeedback() != null && !grade.getFeedback().isBlank())
                ? grade.getFeedback().trim()
                : defaultFeedbackForStatus(resolvedStatus);

        return GradingResult.builder()
                .status(resolvedStatus)
                .score(clampedScore)
                .feedback(feedback)
                .build();
    }

    /**
     * Formats indexed segments into prompt lines and compacts intelligently if length exceeds maxChars,
     * preserving beginning, middle, and end segments evenly.
     */
    String formatTranscriptForPrompt(List<TranscriptionSegment> segments, int maxChars) {
        List<String> lines = new ArrayList<>(segments.size());
        for (int i = 0; i < segments.size(); i++) {
            TranscriptionSegment seg = segments.get(i);
            String text = seg.getText() == null ? "" : seg.getText().trim().replaceAll("\\s+", " ");
            lines.add(String.format(Locale.US, "[%d] (%.1fs - %.1fs): %s",
                    i, seg.getStartTime(), seg.getEndTime(), text));
        }

        String joined = String.join("\n", lines);
        if (joined.length() <= maxChars) {
            return joined;
        }

        int total = lines.size();
        int keepHead = Math.min(total, Math.max(1, total / 8));
        int keepTail = Math.min(total - keepHead, Math.max(1, total / 8));

        // Trim head/tail sizes if needed so both head and tail always fit within maxChars
        int middleStart = keepHead;
        int middleEnd = total - keepTail;
        while (keepHead > 1 || keepTail > 1) {
            int headTailLen = estimateRangeLength(lines, 0, keepHead)
                    + estimateRangeLength(lines, total - keepTail, total)
                    + 16;
            if (headTailLen <= maxChars * 0.75) {
                break;
            }
            if (keepHead >= keepTail && keepHead > 1) {
                keepHead--;
            } else if (keepTail > 1) {
                keepTail--;
            }
        }
        middleStart = keepHead;
        middleEnd = total - keepTail;

        List<Integer> selectedIndices = new ArrayList<>();
        for (int i = 0; i < middleStart; i++) {
            selectedIndices.add(i);
        }

        int middleCount = middleEnd - middleStart;
        int headLen = estimateRangeLength(lines, 0, middleStart);
        int tailLen = estimateRangeLength(lines, middleEnd, total);
        int budgetForMiddle = Math.max(0, maxChars - headLen - tailLen - 32);

        if (middleCount > 0 && budgetForMiddle > 0) {
            int avgLineLen = Math.max(40, joined.length() / Math.max(1, total));
            int targetMiddleLines = Math.min(middleCount, Math.max(1, budgetForMiddle / avgLineLen));
            double step = (double) middleCount / targetMiddleLines;
            int prevAdded = -1;
            int usedMiddle = 0;
            for (int k = 0; k < targetMiddleLines; k++) {
                int idx = middleStart + (int) Math.floor(k * step);
                if (idx < middleEnd && idx > prevAdded) {
                    int lineLen = lines.get(idx).length() + 1;
                    if (usedMiddle + lineLen > budgetForMiddle) {
                        break;
                    }
                    selectedIndices.add(idx);
                    usedMiddle += lineLen;
                    prevAdded = idx;
                }
            }
        }

        for (int i = middleEnd; i < total; i++) {
            selectedIndices.add(i);
        }

        StringBuilder sb = new StringBuilder();
        int lastIdx = -1;
        for (int idx : selectedIndices) {
            if (lastIdx != -1 && idx > lastIdx + 1) {
                String marker = "...\n";
                if (sb.length() + marker.length() <= maxChars) {
                    sb.append(marker);
                }
            }
            String line = lines.get(idx) + "\n";
            if (sb.length() + line.length() > maxChars) {
                if (sb.length() < maxChars - 10) {
                    sb.append(line, 0, maxChars - sb.length());
                }
                break;
            }
            sb.append(line);
            lastIdx = idx;
        }
        return sb.toString().trim();
    }

    private int estimateRangeLength(List<String> lines, int start, int end) {
        int sum = 0;
        for (int i = start; i < end; i++) {
            sum += lines.get(i).length() + 1;
        }
        return sum;
    }

    private AiQuizGenerationPayload validateGenerationPayload(
            AiQuizGenerationPayload raw,
            QuizMode mode,
            int targetCount) {
        if (raw == null || raw.getQuestions() == null || raw.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("AI returned empty quiz questions");
        }
        if (raw.getQuestions().size() != targetCount) {
            throw new IllegalArgumentException(
                    "Expected " + targetCount + " quiz questions, got " + raw.getQuestions().size());
        }

        int mcqCount = 0;
        int fibCount = 0;

        for (AiQuizQuestionItem item : raw.getQuestions()) {
            if (item == null) {
                throw new IllegalArgumentException("Quiz question item cannot be null");
            }
            if (item.getQuestionText() == null || item.getQuestionText().isBlank()) {
                throw new IllegalArgumentException("Question text cannot be blank");
            }
            QuizQuestionType qType = parseQuestionType(item.getQuestionType(), mode);
            if (mode == QuizMode.MULTIPLE_CHOICE && qType != QuizQuestionType.MULTIPLE_CHOICE) {
                throw new IllegalArgumentException("Expected MULTIPLE_CHOICE question in MULTIPLE_CHOICE mode");
            }
            if (mode == QuizMode.FILL_IN_THE_BLANK && qType != QuizQuestionType.FILL_IN_THE_BLANK) {
                throw new IllegalArgumentException("Expected FILL_IN_THE_BLANK question in FILL_IN_THE_BLANK mode");
            }

            if (qType == QuizQuestionType.MULTIPLE_CHOICE) {
                mcqCount++;
                if (item.getOptions() == null || item.getOptions().size() != 4) {
                    throw new IllegalArgumentException("MULTIPLE_CHOICE question must have exactly 4 options");
                }
                for (String opt : item.getOptions()) {
                    if (opt == null || opt.isBlank()) {
                        throw new IllegalArgumentException("MULTIPLE_CHOICE options cannot contain blank entries");
                    }
                }
                if (item.getCorrectIndex() == null || item.getCorrectIndex() < 0 || item.getCorrectIndex() > 3) {
                    throw new IllegalArgumentException("MULTIPLE_CHOICE correctIndex must be between 0 and 3");
                }
            } else {
                fibCount++;
                if (!item.getQuestionText().contains("___")) {
                    throw new IllegalArgumentException("FILL_IN_THE_BLANK questionText must contain '___'");
                }
                if (item.getModelAnswer() == null || item.getModelAnswer().isBlank()) {
                    throw new IllegalArgumentException("FILL_IN_THE_BLANK modelAnswer cannot be blank");
                }
            }

            if (item.getEvidenceSegmentIndex() == null) {
                throw new IllegalArgumentException("evidenceSegmentIndex is required");
            }
        }

        if (mode == QuizMode.MIXED && targetCount >= 2 && (mcqCount == 0 || fibCount == 0)) {
            throw new IllegalArgumentException("MIXED mode must include both MULTIPLE_CHOICE and FILL_IN_THE_BLANK questions");
        }

        return raw;
    }

    private QuizQuestionType parseQuestionType(String rawType, QuizMode mode) {
        if (rawType == null || rawType.isBlank()) {
            if (mode == QuizMode.MULTIPLE_CHOICE) {
                return QuizQuestionType.MULTIPLE_CHOICE;
            }
            if (mode == QuizMode.FILL_IN_THE_BLANK) {
                return QuizQuestionType.FILL_IN_THE_BLANK;
            }
            throw new IllegalArgumentException("questionType is required in MIXED mode");
        }
        try {
            return QuizQuestionType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported questionType: " + rawType);
        }
    }

    static String normalizeAnswerForExactMatch(String input) {
        if (input == null) {
            return "";
        }
        return input.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}“”‘’]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    static QuizGradingStatus statusForScore(int score) {
        if (score >= 85) {
            return QuizGradingStatus.CORRECT;
        }
        if (score >= 40) {
            return QuizGradingStatus.PARTIAL;
        }
        return QuizGradingStatus.INCORRECT;
    }

    private String formatExplanationSuffix(String explanationVn) {
        if (explanationVn == null || explanationVn.isBlank()) {
            return "";
        }
        return " " + explanationVn.trim();
    }

    private String defaultFeedbackForStatus(QuizGradingStatus status) {
        return switch (status) {
            case CORRECT -> "Chính xác! Câu trả lời của bạn thể hiện đúng ý của bài nghe.";
            case PARTIAL -> "Gần đúng! Câu trả lời đã nắm được một phần ý nhưng còn thiếu hoặc chưa thật chính xác.";
            case INCORRECT -> "Chưa chính xác. Hãy nghe lại đoạn dẫn chứng để nắm rõ ý chính.";
        };
    }
}
