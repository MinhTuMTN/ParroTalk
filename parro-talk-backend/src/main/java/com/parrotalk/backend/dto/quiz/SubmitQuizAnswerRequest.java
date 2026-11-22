package com.parrotalk.backend.dto.quiz;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitQuizAnswerRequest {
    @NotNull(message = "Question ID is required.")
    private UUID questionId;
    private Integer selectedIndex;
    private String userAnswer;
}
