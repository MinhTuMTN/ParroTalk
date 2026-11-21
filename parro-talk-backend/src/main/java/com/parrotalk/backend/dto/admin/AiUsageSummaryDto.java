package com.parrotalk.backend.dto.admin;

import java.time.Instant;

public record AiUsageSummaryDto(Instant from, Instant to, long totalRequests, long inputTokens, long outputTokens, long totalTokens, long activeUsers, double avgTokensPerRequest) {
}
