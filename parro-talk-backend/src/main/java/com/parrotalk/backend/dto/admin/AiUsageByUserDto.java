package com.parrotalk.backend.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record AiUsageByUserDto(UUID userId, String email, String displayName, boolean deleted, long requests, long inputTokens, long outputTokens, long totalTokens, Instant lastUsedAt) {
}
