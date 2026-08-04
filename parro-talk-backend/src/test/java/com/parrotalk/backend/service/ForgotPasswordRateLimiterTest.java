package com.parrotalk.backend.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.parrotalk.backend.config.PasswordResetProperties;
import com.parrotalk.backend.exception.AuthException;

class ForgotPasswordRateLimiterTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ForgotPasswordRateLimiter limiter;
    private Map<String, Long> redisStore;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        redisStore = new HashMap<>();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.increment(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Long count = redisStore.getOrDefault(key, 0L) + 1;
            redisStore.put(key, count);
            return count;
        });

        PasswordResetProperties props = new PasswordResetProperties();
        props.setMaxRequestsPerHour(5);
        limiter = new ForgotPasswordRateLimiter(redisTemplate, props);
    }

    @Test
    @DisplayName("should allow requests under the limit")
    void allowUnderLimit() {
        String key = "ip:192.168.1.1";

        // 5 requests should all pass
        for (int i = 0; i < 5; i++) {
            assertThatCode(() -> limiter.checkRateLimit(key)).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("should block the 6th request within the same window")
    void blockOverLimit() {
        String key = "ip:192.168.1.2";

        for (int i = 0; i < 5; i++) {
            limiter.checkRateLimit(key);
        }

        assertThatThrownBy(() -> limiter.checkRateLimit(key))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Too many requests");
    }

    @Test
    @DisplayName("should set key expiration on first request")
    void setExpirationOnFirstRequest() {
        String key = "ip:192.168.1.3";
        limiter.checkRateLimit(key);

        verify(redisTemplate).expire(eq("rate_limit:forgot_password:" + key), any(Duration.class));
    }
}
