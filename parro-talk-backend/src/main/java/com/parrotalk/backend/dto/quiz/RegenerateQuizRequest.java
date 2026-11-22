package com.parrotalk.backend.dto.quiz;

import com.parrotalk.backend.constant.QuizMode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegenerateQuizRequest {
    @NotNull(message = "Quiz mode is required.")
    private QuizMode mode;
}
