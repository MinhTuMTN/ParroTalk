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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.security.config.Customizer;

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
    @Value("${application.security.public-endpoints:/api/auth/**}")
    private String[] publicEndpoints;

    /** Jwt authentication filter */
    private final JwtAuthenticationFilter jwtAuthFilter;

    /** Authentication provider */
    private final AuthenticationProvider authenticationProvider;

    /** Jwt authentication entry point */
    private final JwtAuthenticationEntryPoint authEntryPoint;

    /**
     * Security filter chain.
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the {@link SecurityFilterChain} to use
     * @throws Exception if an error occurs
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Disable CSRF
        http.csrf(AbstractHttpConfigurer::disable)
                // Enable CORS
                .cors(Customizer.withDefaults())
                // Enable exception handling
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authEntryPoint))
                // Authorize HTTP requests
                .authorizeHttpRequests(auth -> auth
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
