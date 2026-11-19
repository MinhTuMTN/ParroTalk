package com.parrotalk.backend.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.parrotalk.backend.config.CloudflareProperties;
import com.parrotalk.backend.constant.CacheNames;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Client for embedding search queries via Cloudflare Workers AI (@cf/baai/bge-m3).
 */
@Slf4j
@Service
public class CloudflareEmbeddingClient {

    private final RestClient restClient;
    private final CloudflareProperties properties;
    private final ObjectProvider<CacheManager> cacheManagerProvider;

    public CloudflareEmbeddingClient(
            RestClient.Builder restClientBuilder,
            CloudflareProperties properties,
            ObjectProvider<CacheManager> cacheManagerProvider) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
        this.cacheManagerProvider = cacheManagerProvider;
    }

    /**
     * Embed a query string and serialize it as a pgvector literal (e.g. {@code [0.1,0.2,...]}).
     * Returns {@link Optional#empty()} if Cloudflare is unconfigured or fails so callers can fallback.
     */
    public Optional<String> embedQueryToVectorLiteral(String query) {
        if (query == null || query.isBlank() || !properties.isConfigured()) {
            return Optional.empty();
        }

        String trimmedQuery = query.trim();
        String cacheKey = buildCacheKey(trimmedQuery);
        Optional<String> cached = readFromCache(cacheKey);
        if (cached.isPresent()) {
            return cached;
        }

        try {
            String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
            String uri = String.format("%s/accounts/%s/ai/run/%s",
                    baseUrl,
                    properties.getAccountId().trim(),
                    properties.getEmbeddingModel().trim());

            CloudflareEmbedResponse response = restClient.post()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiToken().trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "text", List.of(trimmedQuery),
                            "truncate_inputs", true))
                    .retrieve()
                    .body(CloudflareEmbedResponse.class);

            Optional<String> vectorLiteral = extractAndFormatVector(response);
            vectorLiteral.ifPresent(val -> writeToCache(cacheKey, val));
            return vectorLiteral;
        } catch (Exception ex) {
            log.warn("Cloudflare embedding request failed ({}), falling back to LIKE search", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> extractAndFormatVector(CloudflareEmbedResponse response) {
        if (response == null || !Boolean.TRUE.equals(response.getSuccess()) || response.getResult() == null) {
            log.warn("Cloudflare embedding returned unsuccessful payload");
            return Optional.empty();
        }
        List<List<Double>> data = response.getResult().getData();
        if (data == null || data.size() != 1 || data.get(0) == null) {
            log.warn("Cloudflare embedding returned unexpected vector count");
            return Optional.empty();
        }

        List<Double> vector = data.get(0);
        int expectedDims = properties.getExpectedDimensions();
        if (vector.size() != expectedDims) {
            log.warn("Cloudflare embedding dimension mismatch: expected={}, got={}", expectedDims, vector.size());
            return Optional.empty();
        }

        StringBuilder sb = new StringBuilder(expectedDims * 10 + 2);
        sb.append('[');
        for (int i = 0; i < vector.size(); i++) {
            Double value = vector.get(i);
            if (value == null || !Double.isFinite(value)) {
                log.warn("Cloudflare embedding contains non-finite value at index {}", i);
                return Optional.empty();
            }
            if (i > 0) {
                sb.append(',');
            }
            sb.append(value);
        }
        sb.append(']');
        return Optional.of(sb.toString());
    }

    private String buildCacheKey(String query) {
        String normalized = query.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return properties.getEmbeddingModel().trim() + ":" + normalized;
    }

    private Optional<String> readFromCache(String cacheKey) {
        try {
            CacheManager manager = cacheManagerProvider.getIfAvailable();
            if (manager == null) {
                return Optional.empty();
            }
            Cache cache = manager.getCache(CacheNames.SEMANTIC_QUERY_EMBEDDING_CACHE);
            if (cache == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(cache.get(cacheKey, String.class));
        } catch (Exception ex) {
            log.debug("Semantic embedding cache read skipped: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private void writeToCache(String cacheKey, String value) {
        try {
            CacheManager manager = cacheManagerProvider.getIfAvailable();
            if (manager == null) {
                return;
            }
            Cache cache = manager.getCache(CacheNames.SEMANTIC_QUERY_EMBEDDING_CACHE);
            if (cache != null) {
                cache.put(cacheKey, value);
            }
        } catch (Exception ex) {
            log.debug("Semantic embedding cache write skipped: {}", ex.getMessage());
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CloudflareEmbedResponse {
        private Boolean success;
        private CloudflareEmbedResult result;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CloudflareEmbedResult {
        private List<List<Double>> data;
    }
}
