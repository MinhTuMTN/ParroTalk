package com.parrotalk.backend.dto.vocablist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVocabularyListRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        String targetCefr,
        String visibility
) {
    public CreateVocabularyListRequest(String title, String description, String visibility) {
        this(title, description, null, visibility);
    }
}
