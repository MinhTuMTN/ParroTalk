package com.parrotalk.backend.ratelimit;

import java.time.Clock;
import java.time.Instant;

/**
 * Outcome of a token bucket rate limit evaluation.
 */
public record RateLimitResult(
        boolean allowed,
        long limit,
        long remaining,
        long resetEpochSeconds,
        long retryAfterSeconds) {

    public static RateLimitResult failOpen(long limit) {
        return failOpen(limit, Clock.systemUTC());
    }

    public static RateLimitResult failOpen(long limit, Clock clock) {
        long epochSecond = clock != null ? clock.instant().getEpochSecond() : Instant.now().getEpochSecond();
        return new RateLimitResult(
                true,
                Math.max(1L, limit),
                Math.max(0L, limit - 1L),
                epochSecond,
                0L);
    }
}
