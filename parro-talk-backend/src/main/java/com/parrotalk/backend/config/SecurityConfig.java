package com.parrotalk.backend.config;

import com.parrotalk.backend.security.JwtAuthenticationEntryPoint;
import com.parrotalk.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.security.config.Customizer;

import com.parrotalk.backend.security.CustomOAuth2UserService;
import com.parrotalk.backend.security.OAuth2AuthenticationFailureHandler;
import com.parrotalk.backend.security.OAuth2AuthenticationSuccessHandler;

/**
 * Security config.
 * 
 * @author MinhTuMTN
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    /** Public endpoints */
    @Value("${application.security.public-endpoints}")
    private String[] publicEndpoints;

    /** Jwt authentication filter */
    private final JwtAuthenticationFilter jwtAuthFilter;

    /** Authentication provider */
    private final AuthenticationProvider authenticationProvider;

    /** Jwt authentication entry point */
    private final JwtAuthenticationEntryPoint authEntryPoint;

    private final CustomOAuth2UserService customOAuth2UserService;

    private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    private final OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;

    /**
     * Security filter chain.
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the {@link SecurityFilterChain} to use
     * @throws Exception if an error occurs
     */
    @Bean
    @Order(1)
    SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/oauth2/**", "/login/oauth2/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/authorization/**").permitAll()
                        .requestMatchers("/login/oauth2/code/**").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .baseUri("/oauth2/authorization"))
                        .redirectionEndpoint(endpoint -> endpoint
                                .baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOAuth2UserService))
                        .successHandler(oauth2AuthenticationSuccessHandler)
                        .failureHandler(oauth2AuthenticationFailureHandler));

        return http.build();
    }

    /**
     * Security filter chain for JWT-protected application endpoints.
     */
    @Bean
    @Order(2)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        // Disable CSRF
        http.csrf(AbstractHttpConfigurer::disable)
                // Enable CORS
                .cors(Customizer.withDefaults())
                // Enable exception handling
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authEntryPoint))
                // Authorize HTTP requests
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers(publicEndpoints).permitAll()
                        .anyRequest().authenticated())
                // Disable session management due to JWT authentication
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Set authentication provider
                .authenticationProvider(authenticationProvider)
                // Add JWT authentication filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
