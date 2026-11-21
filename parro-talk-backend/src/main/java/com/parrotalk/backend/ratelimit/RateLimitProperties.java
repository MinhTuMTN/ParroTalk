package com.parrotalk.backend.ratelimit;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Externalized configuration properties for tiered Redis token bucket rate limiting.
 */
@Component
@ConfigurationProperties(prefix = "application.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private static final Map<RateLimitTier, TierConfig> DEFAULT_TIERS = createDefaultTiers();

    /** Master switch to enable or disable rate limiting. */
    private boolean enabled = true;

    /** Whether to trust reverse-proxy headers such as X-Forwarded-For and X-Real-IP. */
    private boolean trustProxyHeaders = true;

    /** Whether to trust CF-Connecting-IP (enable only when origin is strictly behind Cloudflare). */
    private boolean trustCloudflareHeader = false;

    /** Number of trusted reverse-proxy hops appending to X-Forwarded-For (used from right to left). */
    private int trustedProxyHops = 1;

    /** Per-tier overrides keyed by {@link RateLimitTier#getConfigKey()}. */
    private Map<String, TierConfig> tiers = new HashMap<>();

    public TierConfig resolveTier(RateLimitTier tier) {
        TierConfig configured = tiers != null ? tiers.get(tier.getConfigKey()) : null;
        if (configured != null && configured.isValid()) {
            return configured;
        }
        return DEFAULT_TIERS.get(tier);
    }

    private static Map<RateLimitTier, TierConfig> createDefaultTiers() {
        Map<RateLimitTier, TierConfig> defaults = new EnumMap<>(RateLimitTier.class);
        defaults.put(RateLimitTier.AUTH, new TierConfig(10, 10, 60, false));
        defaults.put(RateLimitTier.AUTH_FORGOT_PASSWORD, new TierConfig(5, 5, 3600, false));
        defaults.put(RateLimitTier.AUTH_RESEND_VERIFICATION, new TierConfig(5, 5, 900, false));
        defaults.put(RateLimitTier.AI_HEAVY, new TierConfig(10, 10, 60, true));
        defaults.put(RateLimitTier.AI_LIGHT, new TierConfig(30, 30, 60, true));
        defaults.put(RateLimitTier.GENERAL_PUBLIC, new TierConfig(60, 60, 60, true));
        defaults.put(RateLimitTier.GENERAL_AUTHENTICATED, new TierConfig(120, 120, 60, true));
        return defaults;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TierConfig {
        /** Maximum burst capacity (tokens). */
        private long capacity;

        /** Number of tokens refilled per refill period. */
        private long refillTokens;

        /** Refill period in seconds. */
        private long refillPeriodSeconds;

        /** Whether ROLE_ADMIN bypasses this tier. */
        private boolean adminBypass;

        public boolean isValid() {
            return capacity > 0 && refillTokens > 0 && refillPeriodSeconds > 0;
        }
    }
}
