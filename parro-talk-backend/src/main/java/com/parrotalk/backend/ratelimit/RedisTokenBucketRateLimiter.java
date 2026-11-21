package com.parrotalk.backend.ratelimit;

import java.time.Clock;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.parrotalk.backend.ratelimit.RateLimitProperties.TierConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * Atomic Redis-backed Token Bucket rate limiter using a Lua script.
 * Fails open if Redis is unavailable so traffic is not disrupted by cache outages.
 */
@Component
@Slf4j
public class RedisTokenBucketRateLimiter {

    private static final String KEY_PREFIX = "rate_limit:";

    private static final String TOKEN_BUCKET_LUA_SCRIPT = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_tokens = tonumber(ARGV[2])
            local refill_period_ms = tonumber(ARGV[3])
            local now_ms = tonumber(ARGV[4])
            local requested = tonumber(ARGV[5])

            local refill_rate_per_ms = refill_tokens / refill_period_ms
            local data = redis.call('HMGET', key, 'tokens', 'last_refill_ms')
            local tokens = tonumber(data[1])
            local last_refill_ms = tonumber(data[2])

            if tokens == nil or last_refill_ms == nil then
                tokens = capacity
                last_refill_ms = now_ms
            else
                local elapsed_ms = math.max(0, now_ms - last_refill_ms)
                tokens = math.min(capacity, tokens + (elapsed_ms * refill_rate_per_ms))
                last_refill_ms = math.max(last_refill_ms, now_ms)
            end

            local allowed = 0
            local retry_after_ms = 0

            if tokens >= requested then
                allowed = 1
                tokens = tokens - requested
            else
                allowed = 0
                local deficit = requested - tokens
                retry_after_ms = math.ceil(deficit / refill_rate_per_ms)
            end

            local remaining = math.max(0, math.floor(tokens))
            local tokens_to_full = math.max(0, capacity - tokens)
            local time_to_full_ms = math.ceil(tokens_to_full / refill_rate_per_ms)
            local reset_epoch_sec = math.ceil((now_ms + time_to_full_ms) / 1000)
            local retry_after_sec = math.ceil(retry_after_ms / 1000)
            local ttl_ms = math.max(refill_period_ms * 2, math.ceil((capacity / refill_rate_per_ms) * 2))

            redis.call('HSET', key, 'tokens', tostring(tokens), 'last_refill_ms', tostring(last_refill_ms))
            redis.call('PEXPIRE', key, ttl_ms)

            return {allowed, remaining, reset_epoch_sec, retry_after_sec}
            """;

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;
    @SuppressWarnings("rawtypes")
    private final DefaultRedisScript<List> tokenBucketScript;

    @Autowired
    public RedisTokenBucketRateLimiter(StringRedisTemplate redisTemplate) {
        this(redisTemplate, Clock.systemUTC());
    }

    RedisTokenBucketRateLimiter(StringRedisTemplate redisTemplate, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
        this.tokenBucketScript = new DefaultRedisScript<>();
        this.tokenBucketScript.setScriptText(TOKEN_BUCKET_LUA_SCRIPT);
        this.tokenBucketScript.setResultType(List.class);
    }

    /**
     * Attempts to consume 1 token from the bucket identified by {@code bucketKey}.
     *
     * @param bucketKey unique bucket identifier without global prefix (e.g. {@code ai-heavy:user:123})
     * @param config    token bucket configuration
     * @return evaluation result; fails open on Redis errors
     */
    public RateLimitResult tryConsume(String bucketKey, TierConfig config) {
        return tryConsume(bucketKey, config, 1L);
    }

    /**
     * Attempts to consume {@code tokens} from the bucket identified by {@code bucketKey}.
     */
    public RateLimitResult tryConsume(String bucketKey, TierConfig config, long tokens) {
        if (config == null || !config.isValid()) {
            return RateLimitResult.failOpen(1L, clock);
        }

        long requested = Math.max(1L, Math.min(tokens, config.getCapacity()));
        String redisKey = KEY_PREFIX + bucketKey;
        long nowMs = clock.millis();
        long refillPeriodMs = config.getRefillPeriodSeconds() * 1000L;

        try {
            @SuppressWarnings("unchecked")
            List<Object> response = redisTemplate.execute(
                    tokenBucketScript,
                    Collections.singletonList(redisKey),
                    String.valueOf(config.getCapacity()),
                    String.valueOf(config.getRefillTokens()),
                    String.valueOf(refillPeriodMs),
                    String.valueOf(nowMs),
                    String.valueOf(requested));

            if (response == null || response.size() < 4) {
                log.warn("Rate limiter received empty response from Redis for key '{}'; failing open", maskKey(bucketKey));
                return RateLimitResult.failOpen(config.getCapacity(), clock);
            }

            boolean allowed = toLong(response.get(0)) == 1L;
            long remaining = Math.max(0L, toLong(response.get(1)));
            long resetEpochSeconds = toLong(response.get(2));
            long retryAfterSeconds = allowed ? 0L : Math.max(1L, toLong(response.get(3)));

            return new RateLimitResult(
                    allowed,
                    config.getCapacity(),
                    remaining,
                    resetEpochSeconds,
                    retryAfterSeconds);
        } catch (Exception ex) {
            log.warn("Redis rate limiter unavailable for key '{}'; failing open. Reason: {}",
                    maskKey(bucketKey), ex.getMessage());
            return RateLimitResult.failOpen(config.getCapacity(), clock);
        }
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    /**
     * Safely masks sensitive portions of rate limit keys (emails, IPv4, IPv6, user IDs) for logging.
     */
    public static String maskKey(String key) {
        if (key == null || key.length() <= 4) {
            return "***";
        }

        String[] markers = { ":email:", ":ip:", ":user:", "email:", "ip:", "user:" };
        for (String marker : markers) {
            int idx = key.indexOf(marker);
            if (idx >= 0) {
                String prefix = key.substring(0, idx + marker.length());
                String value = key.substring(idx + marker.length());
                return prefix + maskValue(value);
            }
        }

        return key.substring(0, Math.min(6, key.length())) + "***";
    }

    private static String maskValue(String value) {
        if (value == null || value.isEmpty()) {
            return "***";
        }
        int atIdx = value.indexOf('@');
        if (atIdx >= 0) {
            if (atIdx == 0) {
                return value.length() > 1 ? "***" + value : "***";
            }
            String local = value.substring(0, atIdx);
            String maskedLocal = local.length() > 2
                    ? local.substring(0, 2) + "***"
                    : local.substring(0, 1) + "***";
            return maskedLocal + value.substring(atIdx);
        }
        int dot = value.indexOf('.');
        if (dot > 0) {
            return value.substring(0, dot) + ".***";
        }
        int colon = value.indexOf(':');
        if (colon > 0) {
            return value.substring(0, colon) + ":***";
        }
        if (value.length() > 8) {
            return value.substring(0, 8) + "***";
        }
        return "***";
    }
}
