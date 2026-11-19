package com.parrotalk.backend.dto.dictation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User-submitted answer for a single blank identified by its {@code wordIndex}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlankAnswerItem {
    private int wordIndex;
    private String answer;
}
