package com.parrotalk.backend.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.parrotalk.backend.config.PasswordResetProperties;
import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.exception.AuthException;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis-backed rate limiter for the forgot-password endpoint.
 * Tracks request counts per key (IP address or email) using Redis increment.
 *
 * @author MinhTuMTN
 */
@Component
@Slf4j
public class ForgotPasswordRateLimiter {

    private static final String KEY_PREFIX = "rate_limit:forgot_password:";
    private static final long WINDOW_SECONDS = 3600; // 1 hour

    private final StringRedisTemplate redisTemplate;
    private final int maxRequests;

    public ForgotPasswordRateLimiter(StringRedisTemplate redisTemplate, PasswordResetProperties properties) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = properties.getMaxRequestsPerHour();
    }

    /**
     * Check whether the given key has exceeded the rate limit.
     *
     * @param key an identifier such as a client IP or email address
     * @throws AuthException with {@link ErrorCode#RATE_LIMIT_EXCEEDED} if the limit is exceeded
     */
    public void checkRateLimit(String key) {
        String redisKey = KEY_PREFIX + key;

        Long count = redisTemplate.opsForValue().increment(redisKey);

        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(WINDOW_SECONDS));
        }

        if (count != null && count > maxRequests) {
            log.warn("Rate limit exceeded for key: {}", maskKey(key));
            throw new AuthException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }

    /** Mask the key for safe logging (e.g. email → ab***@domain.com). */
    private String maskKey(String key) {
        if (key.contains("@")) {
            int atIdx = key.indexOf('@');
            String local = key.substring(0, atIdx);
            String masked = local.length() > 2
                    ? local.substring(0, 2) + "***"
                    : local.substring(0, 1) + "***";
            return masked + key.substring(atIdx);
        }
        // IP addresses — show first octet only
        int dot = key.indexOf('.');
        return dot > 0 ? key.substring(0, dot) + ".***" : key;
    }
}
