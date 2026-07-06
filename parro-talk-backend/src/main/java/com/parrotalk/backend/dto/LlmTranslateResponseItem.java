package com.parrotalk.backend.dto;

/**
 * Response item for LLM translation.
 * 
 * @param index The index of the segment in the batch.
 * @param text  The translated text.
 */
public record LlmTranslateResponseItem(int index, String text) {
}
