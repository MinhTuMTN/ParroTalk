package com.parrotalk.backend.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Frontend URLs used by CORS and OAuth redirects.
 */
@Component
@ConfigurationProperties(prefix = "application.frontend")
@Data
public class FrontendProperties {

    private String baseUrl = "http://localhost:3000";

    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:3000"));
}
