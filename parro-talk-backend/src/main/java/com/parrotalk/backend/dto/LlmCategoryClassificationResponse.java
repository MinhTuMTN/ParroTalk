package com.parrotalk.backend.dto;

import java.util.List;

/**
 * Structured LLM response for lesson category classification.
 * 
 * @author MinhTuMTN
 */
public record LlmCategoryClassificationResponse(

    /** List of category codes */
    List<String> categoryCodes
) {
}
