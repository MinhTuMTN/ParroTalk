package com.parrotalk.backend.ratelimit;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the client IP address from an incoming HTTP request, optionally trusting
 * reverse-proxy headers when {@code application.rate-limit.trust-proxy-headers} is enabled.
 * Validates extracted IP strings to prevent header injection and Redis key bloat.
 */
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private static final String HEADER_CF_CONNECTING_IP = "CF-Connecting-IP";
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";
    private static final String UNKNOWN_IP = "unknown";
    private static final int MAX_IP_LENGTH = 45;
    private static final Pattern VALID_IP_CHARS = Pattern.compile("^[0-9a-fA-F:.]+$");

    private final RateLimitProperties properties;

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN_IP;
        }

        if (properties.isTrustProxyHeaders()) {
            if (properties.isTrustCloudflareHeader()) {
                String cfIp = normalizeIp(request.getHeader(HEADER_CF_CONNECTING_IP));
                if (cfIp != null) {
                    return cfIp;
                }
            }

            String xffIp = extractFromXForwardedFor(request.getHeader(HEADER_X_FORWARDED_FOR));
            if (xffIp != null) {
                return xffIp;
            }

            String realIp = normalizeIp(request.getHeader(HEADER_X_REAL_IP));
            if (realIp != null) {
                return realIp;
            }
        }

        String remoteAddr = normalizeIp(request.getRemoteAddr());
        return remoteAddr != null ? remoteAddr : UNKNOWN_IP;
    }

    /**
     * Resolves the client IP from {@code X-Forwarded-For} by walking from right to left
     * according to {@code trustedProxyHops}, preventing spoofing via client-prepended entries.
     */
    private String extractFromXForwardedFor(String xff) {
        if (xff == null || xff.isBlank()) {
            return null;
        }

        String[] parts = xff.split(",");
        List<String> validIps = new ArrayList<>(parts.length);
        for (String part : parts) {
            String normalized = normalizeIp(part);
            if (normalized != null) {
                validIps.add(normalized);
            }
        }
        if (validIps.isEmpty()) {
            return null;
        }

        int hops = Math.max(1, properties.getTrustedProxyHops());
        int targetIndex = Math.max(0, validIps.size() - hops);
        return validIps.get(targetIndex);
    }

    private String normalizeIp(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()
                || trimmed.length() > MAX_IP_LENGTH
                || UNKNOWN_IP.equalsIgnoreCase(trimmed)) {
            return null;
        }
        if (!VALID_IP_CHARS.matcher(trimmed).matches()) {
            return null;
        }
        if (!trimmed.contains(".") && !trimmed.contains(":")) {
            return null;
        }
        return trimmed;
    }
}
