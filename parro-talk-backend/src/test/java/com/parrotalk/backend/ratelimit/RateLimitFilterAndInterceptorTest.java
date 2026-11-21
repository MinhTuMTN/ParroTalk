package com.parrotalk.backend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.AuthException;
import com.parrotalk.backend.exception.GlobalExceptionHandler;
import com.parrotalk.backend.ratelimit.RateLimitProperties.TierConfig;

class RateLimitFilterAndInterceptorTest {

    private RateLimitProperties properties;
    private RedisTokenBucketRateLimiter rateLimiter;
    private RateLimitHttpSupport httpSupport;
    private RateLimitFilter filter;
    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setTrustProxyHeaders(true);

        rateLimiter = mock(RedisTokenBucketRateLimiter.class);
        ClientIpResolver ipResolver = new ClientIpResolver(properties);
        httpSupport = new RateLimitHttpSupport(ipResolver, new ObjectMapper());
        filter = new RateLimitFilter(properties, rateLimiter, httpSupport);
        interceptor = new RateLimitInterceptor(properties, rateLimiter, httpSupport);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Global filter returns 429 JSON and rate limit headers when public client exceeds quota")
    void globalFilterBlocksPublicRequestWith429() throws Exception {
        when(rateLimiter.tryConsume(eq("general-public:ip:203.0.113.5"), any(TierConfig.class)))
                .thenReturn(new RateLimitResult(false, 60, 0, 1_700_000_060L, 12L));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/library");
        request.addHeader("X-Forwarded-For", "203.0.113.5");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader(RateLimitHttpSupport.HEADER_RATE_LIMIT)).isEqualTo("60");
        assertThat(response.getHeader(RateLimitHttpSupport.HEADER_RATE_LIMIT_REMAINING)).isEqualTo("0");
        assertThat(response.getHeader(RateLimitHttpSupport.HEADER_RATE_LIMIT_RESET)).isEqualTo("1700000060");
        assertThat(response.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("12");
        assertThat(response.getContentAsString()).contains(ErrorCode.RATE_LIMIT_EXCEEDED.name());
        assertThat(response.getContentAsString()).contains("\"code\":1015");
    }

    @Test
    @DisplayName("Global filter protects public SSE connections against connection flooding")
    void globalFilterEnforcesPublicSseConnections() throws Exception {
        when(rateLimiter.tryConsume(eq("general-public:ip:203.0.113.8"), any(TierConfig.class)))
                .thenReturn(new RateLimitResult(false, 60, 0, 1_700_000_060L, 10L));

        MockHttpServletRequest sseRequest = new MockHttpServletRequest("GET", "/api/lessons/sse/123");
        sseRequest.setRemoteAddr("203.0.113.8");
        MockHttpServletResponse sseResponse = new MockHttpServletResponse();
        filter.doFilter(sseRequest, sseResponse, new MockFilterChain());

        assertThat(sseResponse.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("Global filter bypasses ROLE_ADMIN, OPTIONS preflight, and non-/api/ paths")
    void globalFilterBypassesAdminOptionsAndNonApi() throws Exception {
        authenticateAs(Role.ADMIN);

        MockHttpServletRequest adminRequest = new MockHttpServletRequest("GET", "/api/lessons/mine");
        MockHttpServletResponse adminResponse = new MockHttpServletResponse();
        filter.doFilter(adminRequest, adminResponse, new MockFilterChain());
        assertThat(adminResponse.getStatus()).isEqualTo(200);

        SecurityContextHolder.clearContext();
        MockHttpServletRequest optionsRequest = new MockHttpServletRequest("OPTIONS", "/api/library");
        MockHttpServletResponse optionsResponse = new MockHttpServletResponse();
        filter.doFilter(optionsRequest, optionsResponse, new MockFilterChain());
        assertThat(optionsResponse.getStatus()).isEqualTo(200);

        MockHttpServletRequest actuatorRequest = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse actuatorResponse = new MockHttpServletResponse();
        filter.doFilter(actuatorRequest, actuatorResponse, new MockFilterChain());
        assertThat(actuatorResponse.getStatus()).isEqualTo(200);

        verify(rateLimiter, never()).tryConsume(any(), any());
    }

    @Test
    @DisplayName("Interceptor bypasses ROLE_ADMIN on AI_HEAVY but enforces AUTH tier even for ROLE_ADMIN")
    void interceptorAdminBypassDependsOnTier() throws Exception {
        authenticateAs(Role.ADMIN);
        SampleController controller = new SampleController();

        Method aiMethod = SampleController.class.getMethod("aiEndpoint");
        HandlerMethod aiHandler = new HandlerMethod(controller, aiMethod);
        MockHttpServletRequest aiReq = new MockHttpServletRequest("POST", "/api/writing/analyze");
        MockHttpServletResponse aiRes = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(aiReq, aiRes, aiHandler)).isTrue();
        verify(rateLimiter, never()).tryConsume(any(), any());

        when(rateLimiter.tryConsume(eq("auth:ip:10.1.1.9"), any(TierConfig.class)))
                .thenReturn(new RateLimitResult(false, 10, 0, 1_700_000_060L, 15L));

        Method authMethod = SampleController.class.getMethod("authEndpoint");
        HandlerMethod authHandler = new HandlerMethod(controller, authMethod);
        MockHttpServletRequest authReq = new MockHttpServletRequest("POST", "/api/auth/login");
        authReq.setRemoteAddr("10.1.1.9");
        MockHttpServletResponse authRes = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(authReq, authRes, authHandler)).isFalse();
        assertThat(authRes.getStatus()).isEqualTo(429);
        assertThat(authRes.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("15");
    }

    @Test
    @DisplayName("Interceptor isolates buckets when @RateLimit specifies resource suffix")
    void interceptorIsolatesResourceBuckets() throws Exception {
        when(rateLimiter.tryConsume(eq("auth:refresh:ip:10.1.1.10"), any(TierConfig.class)))
                .thenReturn(new RateLimitResult(true, 10, 9, 1_700_000_006L, 0L));

        SampleController controller = new SampleController();
        HandlerMethod refreshHandler = new HandlerMethod(controller, SampleController.class.getMethod("refreshEndpoint"));
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/refresh");
        req.setRemoteAddr("10.1.1.10");
        MockHttpServletResponse res = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(req, res, refreshHandler)).isTrue();
        verify(rateLimiter).tryConsume(eq("auth:refresh:ip:10.1.1.10"), any(TierConfig.class));
    }

    @Test
    @DisplayName("Interceptor keys authenticated regular users by User ID")
    void interceptorKeysAuthenticatedUserById() throws Exception {
        UUID userId = authenticateAs(Role.USER);
        when(rateLimiter.tryConsume(eq("ai-heavy:user:" + userId), any(TierConfig.class)))
                .thenReturn(new RateLimitResult(true, 10, 9, 1_700_000_006L, 0L));

        SampleController controller = new SampleController();
        HandlerMethod aiHandler = new HandlerMethod(controller, SampleController.class.getMethod("aiEndpoint"));
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/writing/analyze");
        MockHttpServletResponse res = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(req, res, aiHandler)).isTrue();
        assertThat(res.getHeader(RateLimitHttpSupport.HEADER_RATE_LIMIT)).isEqualTo("10");
        assertThat(res.getHeader(RateLimitHttpSupport.HEADER_RATE_LIMIT_REMAINING)).isEqualTo("9");
    }

    @Test
    @DisplayName("GlobalExceptionHandler attaches Retry-After and X-RateLimit headers for AuthException with RateLimitResult")
    void globalExceptionHandlerAttachesRateLimitHeaders() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        RateLimitResult result = new RateLimitResult(false, 5, 0, 1_700_003_600L, 720L);
        AuthException ex = new AuthException(ErrorCode.RATE_LIMIT_EXCEEDED, result);

        ResponseEntity<ApiResponse<Void>> response = handler.handleAuthException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("720");
        assertThat(response.getHeaders().getFirst(RateLimitHttpSupport.HEADER_RATE_LIMIT)).isEqualTo("5");
        assertThat(response.getHeaders().getFirst(RateLimitHttpSupport.HEADER_RATE_LIMIT_REMAINING)).isEqualTo("0");
        assertThat(response.getHeaders().getFirst(RateLimitHttpSupport.HEADER_RATE_LIMIT_RESET)).isEqualTo("1700003600");
    }

    private UUID authenticateAs(Role role) {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .email(role.name().toLowerCase() + "@parrotalk.test")
                .fullName("Test User")
                .role(role)
                .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
        return id;
    }

    static class SampleController {
        @RateLimit(tier = RateLimitTier.AI_HEAVY)
        public void aiEndpoint() {
        }

        @RateLimit(tier = RateLimitTier.AUTH, keyStrategy = RateLimitKeyStrategy.IP_ONLY)
        public void authEndpoint() {
        }

        @RateLimit(tier = RateLimitTier.AUTH, keyStrategy = RateLimitKeyStrategy.IP_ONLY, resource = "refresh")
        public void refreshEndpoint() {
        }
    }
}
