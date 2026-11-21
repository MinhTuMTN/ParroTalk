package com.parrotalk.backend.ratelimit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.entity.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Shared HTTP utilities for subject key resolution, admin bypass checks,
 * rate limit response headers, and 429 JSON responses.
 */
@Component
@RequiredArgsConstructor
public class RateLimitHttpSupport {

    public static final String HEADER_RATE_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    public boolean isAdmin() {
        if (!isAuthenticated()) {
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (ROLE_ADMIN.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public String resolveSubjectKey(HttpServletRequest request, RateLimitKeyStrategy strategy) {
        if (strategy == RateLimitKeyStrategy.USER_OR_IP && isAuthenticated()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Object principal = authentication.getPrincipal();
            if (principal instanceof User user && user.getId() != null) {
                return "user:" + user.getId();
            }
            if (authentication.getName() != null && !authentication.getName().isBlank()) {
                return "user:" + authentication.getName();
            }
        }
        return "ip:" + clientIpResolver.resolve(request);
    }

    public void applyHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader(HEADER_RATE_LIMIT, String.valueOf(result.limit()));
        response.setHeader(HEADER_RATE_LIMIT_REMAINING, String.valueOf(result.remaining()));
        response.setHeader(HEADER_RATE_LIMIT_RESET, String.valueOf(result.resetEpochSeconds()));
        if (!result.allowed() && result.retryAfterSeconds() > 0) {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(result.retryAfterSeconds()));
        }
    }

    public void writeTooManyRequestsResponse(HttpServletResponse response, RateLimitResult result) throws IOException {
        applyHeaders(response, result);
        ErrorCode errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .errorCode(errorCode.name())
                .message(errorCode.getMessage())
                .build();

        objectMapper.writeValue(response.getWriter(), body);
    }
}
