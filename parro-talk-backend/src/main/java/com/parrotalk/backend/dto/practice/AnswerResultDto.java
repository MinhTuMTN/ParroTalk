package com.parrotalk.backend.dto.practice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResultDto {
    private boolean correct;
    private String explanation;
    private String correctAnswer;
    private Integer xpEarned;
}
