package com.parrotalk.backend.ratelimit;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.parrotalk.backend.ratelimit.RateLimitProperties.TierConfig;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Global HTTP filter applying baseline rate limiting across {@code /api/**} endpoints.
 * Runs after {@code JwtAuthenticationFilter} so authenticated users are keyed by User ID.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final RedisTokenBucketRateLimiter rateLimiter;
    private final RateLimitHttpSupport httpSupport;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        boolean authenticated = httpSupport.isAuthenticated();
        RateLimitTier tier = authenticated
                ? RateLimitTier.GENERAL_AUTHENTICATED
                : RateLimitTier.GENERAL_PUBLIC;
        TierConfig config = properties.resolveTier(tier);

        if (config.isAdminBypass() && httpSupport.isAdmin()) {
            filterChain.doFilter(request, response);
            return;
        }

        String subjectKey = httpSupport.resolveSubjectKey(request, RateLimitKeyStrategy.USER_OR_IP);
        String bucketKey = tier.getConfigKey() + ":" + subjectKey;

        RateLimitResult result = rateLimiter.tryConsume(bucketKey, config);
        if (!result.allowed()) {
            log.warn("Global rate limit exceeded: tier={}, subject={}, path={}",
                    tier.getConfigKey(),
                    RedisTokenBucketRateLimiter.maskKey(bucketKey),
                    request.getRequestURI());
            httpSupport.writeTooManyRequestsResponse(response, result);
            return;
        }

        httpSupport.applyHeaders(response, result);
        filterChain.doFilter(request, response);
    }
}
