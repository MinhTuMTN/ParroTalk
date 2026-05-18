package com.parrotalk.backend.service.ai;

import com.parrotalk.backend.dto.CategoryAiSuggestDescriptionRequest;
import com.parrotalk.backend.dto.CategoryAiSuggestDescriptionResponse;

/**
 * Abstraction for category AI assistance.
 * Implementations can use Spring AI, OpenAI, or another provider without changing controllers.
 */
public interface CategoryAiService {

    /**
     * Suggest a category description based on admin input.
     *
     * @param request Suggestion request
     * @return Suggested category description response
     */
    CategoryAiSuggestDescriptionResponse suggestDescription(CategoryAiSuggestDescriptionRequest request);
}
