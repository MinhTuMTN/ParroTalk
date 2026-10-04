package com.parrotalk.backend.dto.admin.vocab;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminVocabularyAiCommonMeaningRequestDto {

    @NotBlank(message = "Word is required")
    private String word;

}
