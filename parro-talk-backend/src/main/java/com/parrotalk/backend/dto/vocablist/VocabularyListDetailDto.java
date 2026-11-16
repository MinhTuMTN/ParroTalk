package com.parrotalk.backend.dto.vocablist;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VocabularyListDetailDto(
        UUID id,
        String title,
        String description,
        String targetCefr,
        String visibility,
        String status,
        String shareToken,
        UUID clonedFromListId,
        String ownerName,
        boolean isOwner,
        List<SharedUserDto> sharedUsers,
        List<VocabularyListItemDto> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public VocabularyListDetailDto(
            UUID id,
            String title,
            String description,
            String visibility,
            String status,
            String shareToken,
            UUID clonedFromListId,
            String ownerName,
            boolean isOwner,
            List<SharedUserDto> sharedUsers,
            List<VocabularyListItemDto> items,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, title, description, null, visibility, status, shareToken, clonedFromListId, ownerName, isOwner, sharedUsers, items, createdAt, updatedAt);
    }

    public record SharedUserDto(
            UUID userId,
            String email,
            String username,
            String fullName,
            String role
    ) {}
}
