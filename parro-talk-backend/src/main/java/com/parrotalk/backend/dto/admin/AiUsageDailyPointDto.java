package com.parrotalk.backend.dto.admin;

import java.time.LocalDate;

public record AiUsageDailyPointDto(LocalDate date, long requests, long inputTokens, long outputTokens, long totalTokens) {
}
