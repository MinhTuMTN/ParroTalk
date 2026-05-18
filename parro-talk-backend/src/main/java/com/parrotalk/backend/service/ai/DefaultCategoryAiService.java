package com.parrotalk.backend.service.ai;

import com.parrotalk.backend.dto.CategoryAiSuggestDescriptionRequest;
import com.parrotalk.backend.dto.CategoryAiSuggestDescriptionResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Default category AI service abstraction.
 *
 * This project does not currently include Spring AI dependencies/configuration. The prompt builder is kept here
 * so a provider-backed implementation can reuse the same contract and replace the deterministic fallback later.
 */
@Service
public class DefaultCategoryAiService implements CategoryAiService {

    @Override
    public CategoryAiSuggestDescriptionResponse suggestDescription(CategoryAiSuggestDescriptionRequest request) {
        String categoryName = request.getCategoryName().trim();
        String targetLearner = StringUtils.hasText(request.getTargetLearner())
                ? request.getTargetLearner().trim()
                : "English learners";
        String language = StringUtils.hasText(request.getLanguage()) ? request.getLanguage().trim() : "en";

        // Provider integration point: send buildJsonPrompt(...) to Spring AI/OpenAI and parse valid JSON response.
        buildJsonPrompt(categoryName, targetLearner, language);

        return CategoryAiSuggestDescriptionResponse.builder()
                .suggestedDescription(buildFallbackDescription(categoryName, targetLearner, language))
                .build();
    }

    private String buildJsonPrompt(String categoryName, String targetLearner, String language) {
        return """
                You are helping an admin of an English listening dictation app create a category.
                Return ONLY valid JSON with this exact schema: {"suggestedDescription":"string"}.
                The description must be natural, easy to understand, and suitable for organizing English listening lessons, audio, or video.
                Do not mention implementation details. Do not hard-code examples unrelated to the given category.
                Category name: %s
                Target learner: %s
                Output language: %s
                """.formatted(categoryName, targetLearner, language);
    }

    private String buildFallbackDescription(String categoryName, String targetLearner, String language) {
        if ("vi".equalsIgnoreCase(language) || "vietnamese".equalsIgnoreCase(language)) {
            return "Các bài nghe tiếng Anh về " + categoryName
                    + ", được chọn để giúp " + targetLearner
                    + " luyện nghe, ghi chính tả và mở rộng vốn từ trong ngữ cảnh tự nhiên.";
        }

        return "English listening and dictation lessons about " + categoryName
                + ", curated to help " + targetLearner
                + " improve comprehension, vocabulary, and confidence with natural audio and video content.";
    }
}
