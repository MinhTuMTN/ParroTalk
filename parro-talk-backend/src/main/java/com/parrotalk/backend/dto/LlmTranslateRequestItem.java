package com.parrotalk.backend.dto;

/**
 * Request item for LLM translation.
 * 
 * @param index The index of the segment in the batch.
 * @param text  The text to translate.
 */
public record LlmTranslateRequestItem(int index, String text) {
}
