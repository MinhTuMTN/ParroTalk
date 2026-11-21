package com.parrotalk.backend.ai;

/** Well-known feature tags for AI usage attribution. */
public final class AiFeature {
    public static final String UNKNOWN = "UNKNOWN";
    public static final String CUSTOM_LIST_QUESTION_BATCH = "CUSTOM_LIST_QUESTION_BATCH";

    public static final String ADMIN_VOCABULARY_AI = "ADMIN_VOCABULARY_AI";
    public static final String VOCABULARY_ENRICHMENT = "VOCABULARY_ENRICHMENT";
    public static final String WRITING_ASSISTANT = "WRITING_ASSISTANT";
    public static final String TOPIC_PRACTICE_SENTENCE = "TOPIC_PRACTICE_SENTENCE";
    public static final String LESSON_CATEGORY_CLASSIFICATION = "LESSON_CATEGORY_CLASSIFICATION";
    public static final String TRANSLATION = "TRANSLATION";
    public static final String VOCABULARY_PRACTICE = "VOCABULARY_PRACTICE";

    private AiFeature() {
    }
}
