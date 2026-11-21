package com.parrotalk.backend.dto.admin;



public record AiUsageByModelDto(String provider, String model, long requests, long inputTokens, long outputTokens, long totalTokens, double sharePercent) {
}
