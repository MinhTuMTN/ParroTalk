package com.parrotalk.backend.dto.vocablist;

import java.time.LocalDateTime;
import java.util.UUID;

public record VocabularyListSummaryDto(
        UUID id,
        String title,
        String description,
        String targetCefr,
        String visibility,
        String status,
        String shareToken,
        String ownerName,
        boolean isOwner,
        int wordCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public VocabularyListSummaryDto(
            UUID id,
            String title,
            String description,
            String visibility,
            String status,
            String shareToken,
            String ownerName,
            boolean isOwner,
            int wordCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, title, description, null, visibility, status, shareToken, ownerName, isOwner, wordCount, createdAt, updatedAt);
    }
}
