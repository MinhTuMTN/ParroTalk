package com.parrotalk.backend.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an endpoint-specific rate limit tier evaluated by {@link RateLimitInterceptor}.
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Rate limit tier to enforce.
     */
    RateLimitTier tier();

    /**
     * Strategy used to build the subject key for this endpoint.
     */
    RateLimitKeyStrategy keyStrategy() default RateLimitKeyStrategy.USER_OR_IP;

    /**
     * Optional resource suffix to isolate buckets within the same tier.
     */
    String resource() default "";
}
