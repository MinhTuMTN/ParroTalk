package com.parrotalk.backend.dto.admin;



public record AiUsageByFeatureDto(String feature, long requests, long inputTokens, long outputTokens, long totalTokens, double sharePercent) {
}
