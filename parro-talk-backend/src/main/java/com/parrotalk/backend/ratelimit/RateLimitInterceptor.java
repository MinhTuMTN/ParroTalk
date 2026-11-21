package com.parrotalk.backend.ratelimit;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.parrotalk.backend.ratelimit.RateLimitProperties.TierConfig;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring MVC interceptor enforcing {@link RateLimit} annotations declared on controller
 * methods or classes for sensitive or expensive endpoints.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitProperties properties;
    private final RedisTokenBucketRateLimiter rateLimiter;
    private final RateLimitHttpSupport httpSupport;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!properties.isEnabled() || !(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit annotation = resolveAnnotation(handlerMethod);
        if (annotation == null) {
            return true;
        }

        RateLimitTier tier = annotation.tier();
        TierConfig config = properties.resolveTier(tier);

        if (config.isAdminBypass() && httpSupport.isAdmin()) {
            return true;
        }

        String subjectKey = httpSupport.resolveSubjectKey(request, annotation.keyStrategy());
        String resourceSuffix = annotation.resource().isBlank() ? "" : ":" + annotation.resource().trim();
        String bucketKey = tier.getConfigKey() + resourceSuffix + ":" + subjectKey;

        RateLimitResult result = rateLimiter.tryConsume(bucketKey, config);
        if (!result.allowed()) {
            log.warn("Endpoint rate limit exceeded: tier={}, subject={}, path={}",
                    tier.getConfigKey(),
                    RedisTokenBucketRateLimiter.maskKey(bucketKey),
                    request.getRequestURI());
            httpSupport.writeTooManyRequestsResponse(response, result);
            return false;
        }

        httpSupport.applyHeaders(response, result);
        return true;
    }

    private RateLimit resolveAnnotation(HandlerMethod handlerMethod) {
        RateLimit methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), RateLimit.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RateLimit.class);
    }
}
