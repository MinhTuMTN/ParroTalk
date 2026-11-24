package com.parrotalk.backend.dto.dictation;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single blank slot in a dictation segment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlankSlotDto {

    /**
     * Zero-based index among word tokens (only {@code isWord == true} tokens are counted).
     */
    private int wordIndex;

    /**
     * Internal token length (ignored in JSON serialization so API responses never leak missing word length).
     */
    @JsonIgnore
    private int charLength;

    /**
     * Optional alternative acceptable answers normalized/display forms if different from the surface word.
     */
    private List<String> acceptableAnswers;
}
