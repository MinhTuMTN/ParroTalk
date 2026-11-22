package com.parrotalk.backend.dto.quiz;

import com.parrotalk.backend.constant.QuizMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuizAttemptRequest {
    private QuizMode mode;
    @Builder.Default
    private boolean forceNew = false;
}
