package com.parrotalk.backend.dto.writing;

import com.parrotalk.backend.constant.DifficultyScale;
import com.parrotalk.backend.constant.WritingMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WritingAnalyzeRequestDto {

    @NotBlank(message = "Input text must not be blank.")
    @Size(min = 3, max = 1000, message = "Input text must be between 3 and 1000 characters.")
    private String inputText;

    @NotNull(message = "Writing mode is required (SUGGESTION or GUIDANCE).")
    private WritingMode mode;

    @NotNull(message = "Difficulty scale is required (CEFR or IELTS).")
    private DifficultyScale scaleType;

    @NotBlank(message = "Target level is required (e.g. B2 or 6.5).")
    private String targetLevel;
}

