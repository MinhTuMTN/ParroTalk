package com.parrotalk.backend.ratelimit;

/**
 * Strategy for resolving the subject identifier used in rate limit keys.
 */
public enum RateLimitKeyStrategy {
    /**
     * Use authenticated user ID when available; otherwise fall back to client IP.
     */
    USER_OR_IP,

    /**
     * Always identify requests by client IP address.
     */
    IP_ONLY
}
