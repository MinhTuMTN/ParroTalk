package com.parrotalk.backend.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

/**
 * The four independent Writing Assessment criteria used to evaluate learner drafts.
 */
@Getter
@RequiredArgsConstructor
public enum WritingAssessmentCriterion {

    TASK_FULFILLMENT(
            "Task Fulfillment & Idea Development",
            "Hoàn thành yêu cầu và phát triển ý",
            0.25
    ),
    COHERENCE_COHESION(
            "Coherence & Cohesion",
            "Tính mạch lạc và liên kết",
            0.25
    ),
    LEXICAL_RESOURCE(
            "Lexical Resource",
            "Vốn từ vựng",
            0.25
    ),
    GRAMMATICAL_RANGE_ACCURACY(
            "Grammatical Range & Accuracy",
            "Độ đa dạng và chính xác ngữ pháp",
            0.25
    );

    private final String nameEn;
    private final String labelVn;
    private final double defaultWeight;

    public static WritingAssessmentCriterion fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Assessment criterion key cannot be blank");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "TASK_FULFILLMENT", "TASK_FULFILLMENT_IDEA_DEVELOPMENT" -> TASK_FULFILLMENT;
            case "COHERENCE_COHESION", "COHERENCE_AND_COHESION" -> COHERENCE_COHESION;
            case "LEXICAL_RESOURCE" -> LEXICAL_RESOURCE;
            case "GRAMMATICAL_RANGE_ACCURACY", "GRAMMATICAL_RANGE_AND_ACCURACY" -> GRAMMATICAL_RANGE_ACCURACY;
            default -> throw new IllegalArgumentException("Unknown assessment criterion: " + raw);
        };
    }
}
