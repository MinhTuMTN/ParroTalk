package com.parrotalk.backend.service;

import org.springframework.stereotype.Component;

import com.parrotalk.backend.config.PasswordResetProperties;
import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.exception.AuthException;
import com.parrotalk.backend.ratelimit.RateLimitProperties;
import com.parrotalk.backend.ratelimit.RateLimitProperties.TierConfig;
import com.parrotalk.backend.ratelimit.RateLimitResult;
import com.parrotalk.backend.ratelimit.RateLimitTier;
import com.parrotalk.backend.ratelimit.RedisTokenBucketRateLimiter;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis-backed rate limiter for the forgot-password endpoint.
 * Delegates to the shared {@link RedisTokenBucketRateLimiter} so IP and email keys
 * use the same atomic token bucket engine and fail-open behavior as the rest of the API.
 *
 * @author MinhTuMTN
 */
@Component
@Slf4j
public class ForgotPasswordRateLimiter {

    private final RedisTokenBucketRateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;
    private final PasswordResetProperties passwordResetProperties;

    public ForgotPasswordRateLimiter(
            RedisTokenBucketRateLimiter rateLimiter,
            RateLimitProperties rateLimitProperties,
            PasswordResetProperties passwordResetProperties) {
        this.rateLimiter = rateLimiter;
        this.rateLimitProperties = rateLimitProperties;
        this.passwordResetProperties = passwordResetProperties;
    }

    /**
     * Check whether the given key has exceeded the forgot-password rate limit.
     *
     * @param key an identifier such as a client IP or email address
     * @throws AuthException with {@link ErrorCode#RATE_LIMIT_EXCEEDED} and {@link RateLimitResult} if exceeded
     */
    public void checkRateLimit(String key) {
        if (!rateLimitProperties.isEnabled()) {
            return;
        }

        TierConfig tierConfig = resolveEffectiveConfig();
        String bucketKey = RateLimitTier.AUTH_FORGOT_PASSWORD.getConfigKey() + ":" + key;
        RateLimitResult result = rateLimiter.tryConsume(bucketKey, tierConfig);

        if (!result.allowed()) {
            log.warn("Forgot-password rate limit exceeded for key: {}",
                    RedisTokenBucketRateLimiter.maskKey(bucketKey));
            throw new AuthException(ErrorCode.RATE_LIMIT_EXCEEDED, result);
        }
    }

    private TierConfig resolveEffectiveConfig() {
        TierConfig configured = rateLimitProperties.resolveTier(RateLimitTier.AUTH_FORGOT_PASSWORD);
        int legacyMax = passwordResetProperties != null ? passwordResetProperties.getMaxRequestsPerHour() : 0;
        if (legacyMax > 0 && rateLimitProperties.getTiers() != null
                && !rateLimitProperties.getTiers().containsKey(RateLimitTier.AUTH_FORGOT_PASSWORD.getConfigKey())) {
            return new TierConfig(legacyMax, legacyMax, configured.getRefillPeriodSeconds(), false);
        }
        return configured;
    }
}
