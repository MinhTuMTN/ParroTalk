package com.parrotalk.backend.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.parrotalk.backend.config.PasswordResetProperties;
import com.parrotalk.backend.exception.AuthException;
import com.parrotalk.backend.ratelimit.RateLimitProperties;
import com.parrotalk.backend.ratelimit.RateLimitProperties.TierConfig;
import com.parrotalk.backend.ratelimit.RateLimitResult;
import com.parrotalk.backend.ratelimit.RedisTokenBucketRateLimiter;

class ForgotPasswordRateLimiterTest {

    private RedisTokenBucketRateLimiter tokenBucketRateLimiter;
    private RateLimitProperties rateLimitProperties;
    private PasswordResetProperties passwordResetProperties;
    private ForgotPasswordRateLimiter limiter;

    @BeforeEach
    void setUp() {
        tokenBucketRateLimiter = mock(RedisTokenBucketRateLimiter.class);
        rateLimitProperties = new RateLimitProperties();
        rateLimitProperties.setEnabled(true);

        passwordResetProperties = new PasswordResetProperties();
        passwordResetProperties.setMaxRequestsPerHour(5);

        limiter = new ForgotPasswordRateLimiter(
                tokenBucketRateLimiter,
                rateLimitProperties,
                passwordResetProperties);
    }

    @Test
    @DisplayName("should allow requests when token bucket permits")
    void allowUnderLimit() {
        String key = "ip:192.168.1.1";
        when(tokenBucketRateLimiter.tryConsume(eq("auth-forgot-password:" + key), any(TierConfig.class)))
                .thenReturn(new RateLimitResult(true, 5, 4, 1700003600L, 0L));

        assertThatCode(() -> limiter.checkRateLimit(key)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should throw AuthException when token bucket rejects")
    void blockOverLimit() {
        String key = "ip:192.168.1.2";
        when(tokenBucketRateLimiter.tryConsume(eq("auth-forgot-password:" + key), any(TierConfig.class)))
                .thenReturn(new RateLimitResult(false, 5, 0, 1700003600L, 720L));

        assertThatThrownBy(() -> limiter.checkRateLimit(key))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Too many requests");
    }

    @Test
    @DisplayName("should skip rate limiting when master switch is disabled")
    void skipWhenDisabled() {
        rateLimitProperties.setEnabled(false);

        assertThatCode(() -> limiter.checkRateLimit("ip:192.168.1.3")).doesNotThrowAnyException();
        verify(tokenBucketRateLimiter, never()).tryConsume(any(), any());
    }
}
