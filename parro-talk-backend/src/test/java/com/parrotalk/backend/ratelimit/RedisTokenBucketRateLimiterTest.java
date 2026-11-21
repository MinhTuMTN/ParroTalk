package com.parrotalk.backend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import com.parrotalk.backend.ratelimit.RateLimitProperties.TierConfig;

class RedisTokenBucketRateLimiterTest {

    private StringRedisTemplate redisTemplate;
    private RedisTokenBucketRateLimiter rateLimiter;
    private TierConfig config;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC);
        rateLimiter = new RedisTokenBucketRateLimiter(redisTemplate, fixedClock);
        config = new TierConfig(10, 10, 60, true);
    }

    @Test
    @DisplayName("should return allowed result and pass expected KEYS/ARGV to Lua script")
    @SuppressWarnings("unchecked")
    void returnsAllowedWhenPermitted() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(1L, 9L, 1_700_000_006L, 0L));

        RateLimitResult result = rateLimiter.tryConsume("ai-heavy:user:123", config);

        assertThat(result.allowed()).isTrue();
        assertThat(result.limit()).isEqualTo(10L);
        assertThat(result.remaining()).isEqualTo(9L);
        assertThat(result.resetEpochSeconds()).isEqualTo(1_700_000_006L);
        assertThat(result.retryAfterSeconds()).isZero();

        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(List.of("rate_limit:ai-heavy:user:123")),
                eq("10"),
                eq("10"),
                eq("60000"),
                eq("1700000000000"),
                eq("1"));
    }

    @Test
    @DisplayName("should return rejected result with retryAfterSeconds when Lua script denies request")
    @SuppressWarnings("unchecked")
    void returnsRejectedWhenDenied() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(0L, 0L, 1_700_000_060L, 6L));

        RateLimitResult result = rateLimiter.tryConsume("auth:ip:1.2.3.4", config);

        assertThat(result.allowed()).isFalse();
        assertThat(result.limit()).isEqualTo(10L);
        assertThat(result.remaining()).isZero();
        assertThat(result.retryAfterSeconds()).isEqualTo(6L);
    }

    @Test
    @DisplayName("should fail open with deterministic clock when Redis throws connection exception")
    @SuppressWarnings("unchecked")
    void failsOpenOnRedisException() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any(), any()))
                .thenThrow(new RedisConnectionFailureException("Redis down"));

        RateLimitResult result = rateLimiter.tryConsume("general-public:ip:1.2.3.4", config);

        assertThat(result.allowed()).isTrue();
        assertThat(result.limit()).isEqualTo(10L);
        assertThat(result.resetEpochSeconds()).isEqualTo(1_700_000_000L);
        assertThat(result.retryAfterSeconds()).isZero();
    }

    @Test
    @DisplayName("should fail open when Redis returns null")
    @SuppressWarnings("unchecked")
    void failsOpenOnNullResponse() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(null);

        RateLimitResult result = rateLimiter.tryConsume("general-public:ip:1.2.3.4", config);

        assertThat(result.allowed()).isTrue();
        assertThat(result.limit()).isEqualTo(10L);
    }

    @Test
    @DisplayName("should safely mask emails, leading-@ values, IPv4, IPv6, and user IDs without throwing")
    void masksKeysSafely() {
        assertThat(RedisTokenBucketRateLimiter.maskKey("general-public:ip:@")).isEqualTo("general-public:ip:***");
        assertThat(RedisTokenBucketRateLimiter.maskKey("auth-forgot-password:email:@domain.com"))
                .isEqualTo("auth-forgot-password:email:***@domain.com");
        assertThat(RedisTokenBucketRateLimiter.maskKey("auth-forgot-password:email:alice@example.com"))
                .isEqualTo("auth-forgot-password:email:al***@example.com");
        assertThat(RedisTokenBucketRateLimiter.maskKey("general-public:ip:203.0.113.5"))
                .isEqualTo("general-public:ip:203.***");
        assertThat(RedisTokenBucketRateLimiter.maskKey("general-public:ip:2001:db8:85a3::8a2e"))
                .isEqualTo("general-public:ip:2001:***");
        assertThat(RedisTokenBucketRateLimiter.maskKey("ai-heavy:user:12345678-90ab-cdef"))
                .isEqualTo("ai-heavy:user:12345678***");
    }
}
