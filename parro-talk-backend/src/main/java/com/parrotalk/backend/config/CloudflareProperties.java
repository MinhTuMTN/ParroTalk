package com.parrotalk.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for Cloudflare Workers AI embeddings.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.cloudflare")
public class CloudflareProperties {

    private boolean enabled = true;

    private String accountId = "";

    private String apiToken = "";

    private String embeddingModel = "@cf/baai/bge-m3";

    private double minSimilarity = 0.30;

    private int expectedDimensions = 1024;

    private String baseUrl = "https://api.cloudflare.com/client/v4";

    public boolean isConfigured() {
        return enabled
                && accountId != null && !accountId.isBlank()
                && apiToken != null && !apiToken.isBlank()
                && embeddingModel != null && !embeddingModel.isBlank();
    }
}
