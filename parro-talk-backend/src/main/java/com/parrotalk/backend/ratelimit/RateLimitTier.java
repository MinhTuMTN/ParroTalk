package com.parrotalk.backend.ratelimit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Named rate limit tiers configured under {@code application.rate-limit.tiers}.
 */
@Getter
@RequiredArgsConstructor
public enum RateLimitTier {
    AUTH("auth"),
    AUTH_FORGOT_PASSWORD("auth-forgot-password"),
    AUTH_RESEND_VERIFICATION("auth-resend-verification"),
    AI_HEAVY("ai-heavy"),
    AI_LIGHT("ai-light"),
    GENERAL_PUBLIC("general-public"),
    GENERAL_AUTHENTICATED("general-authenticated");

    private final String configKey;
}
