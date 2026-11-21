package com.parrotalk.backend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    @Test
    @DisplayName("should ignore CF-Connecting-IP by default and use rightmost untrusted hop from X-Forwarded-For")
    void ignoresCloudflareByDefaultAndUsesRightmostHop() {
        RateLimitProperties props = new RateLimitProperties();
        props.setTrustProxyHeaders(true);
        props.setTrustCloudflareHeader(false);
        props.setTrustedProxyHops(1);
        ClientIpResolver resolver = new ClientIpResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("CF-Connecting-IP", "198.51.100.99");
        request.addHeader("X-Forwarded-For", "1.1.1.1, 203.0.113.20");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.20");
    }

    @Test
    @DisplayName("should honor trustedProxyHops when multiple proxies append to X-Forwarded-For")
    void honorsTrustedProxyHops() {
        RateLimitProperties props = new RateLimitProperties();
        props.setTrustProxyHeaders(true);
        props.setTrustedProxyHops(2);
        ClientIpResolver resolver = new ClientIpResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.1, 203.0.113.20, 10.0.0.5");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.20");
    }

    @Test
    @DisplayName("should trust CF-Connecting-IP only when trustCloudflareHeader is explicitly enabled")
    void trustsCloudflareHeaderWhenEnabled() {
        RateLimitProperties props = new RateLimitProperties();
        props.setTrustProxyHeaders(true);
        props.setTrustCloudflareHeader(true);
        ClientIpResolver resolver = new ClientIpResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("CF-Connecting-IP", "203.0.113.10");
        request.addHeader("X-Forwarded-For", "203.0.113.20");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("should reject malformed or oversized proxy headers and fall back to remoteAddr")
    void rejectsMalformedHeadersAndFallsBackToRemoteAddr() {
        RateLimitProperties props = new RateLimitProperties();
        props.setTrustProxyHeaders(true);
        props.setTrustCloudflareHeader(true);
        ClientIpResolver resolver = new ClientIpResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.7");
        request.addHeader("CF-Connecting-IP", "@");
        request.addHeader("X-Forwarded-For", "not-an-ip, " + "a".repeat(60));
        request.addHeader("X-Real-IP", "unknown");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.7");
    }

    @Test
    @DisplayName("should ignore proxy headers and use remoteAddr when trustProxyHeaders is false")
    void ignoresProxyHeadersWhenUntrusted() {
        RateLimitProperties props = new RateLimitProperties();
        props.setTrustProxyHeaders(false);
        props.setTrustCloudflareHeader(true);
        ClientIpResolver resolver = new ClientIpResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.7");
        request.addHeader("CF-Connecting-IP", "203.0.113.10");
        request.addHeader("X-Forwarded-For", "203.0.113.20");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.7");
    }

    @Test
    @DisplayName("should return unknown for null request")
    void returnsUnknownForNullRequest() {
        ClientIpResolver resolver = new ClientIpResolver(new RateLimitProperties());
        assertThat(resolver.resolve(null)).isEqualTo("unknown");
    }
}
