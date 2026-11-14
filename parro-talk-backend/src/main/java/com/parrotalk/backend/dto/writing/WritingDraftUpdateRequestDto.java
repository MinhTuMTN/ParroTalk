package com.parrotalk.backend.dto.writing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WritingDraftUpdateRequestDto {

    @NotBlank(message = "Draft sentence must not be blank.")
    @Size(max = 4000, message = "Draft must not exceed 4000 characters.")
    private String userDraft;
}

