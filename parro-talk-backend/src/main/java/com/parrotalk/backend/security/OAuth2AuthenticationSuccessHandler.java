package com.parrotalk.backend.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.parrotalk.backend.config.FrontendProperties;
import com.parrotalk.backend.dto.TokenPair;
import com.parrotalk.backend.service.TokenService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final FrontendProperties frontendProperties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        if (!(authentication.getPrincipal() instanceof ParroTalkOidcUser principal)) {
            throw new ServletException("Unexpected OAuth2 principal type");
        }

        TokenPair tokenPair = tokenService.issueTokens(principal.getUser());
        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendProperties.getBaseUrl())
                .path("/oauth/success")
                .queryParam("accessToken", tokenPair.accessToken())
                .queryParam("refreshToken", tokenPair.refreshToken())
                .build()
                .encode()
                .toUriString();

        invalidateSession(request);
        response.sendRedirect(redirectUrl);
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
