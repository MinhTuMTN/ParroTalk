package com.parrotalk.backend.dto.dictation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-blank validation result returned in {@code DraftSegmentResponse}.
 * {@code expectedAnswer} is only populated when the blank is correct or the overall segment passed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlankResultItem {
    private int wordIndex;
    private boolean correct;
    private String expectedAnswer;
}
