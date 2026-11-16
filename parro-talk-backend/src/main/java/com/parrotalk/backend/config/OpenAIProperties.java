package com.parrotalk.backend.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "application.ai")
@Data
public class OpenAIProperties {
    private String defaultProvider = "groq";
    private List<String> apiKeys = new ArrayList<>();
    private String baseUrl;
    private List<String> model = new ArrayList<>();
    private boolean exhaustModelsPerKey = false;
    private Map<String, ProviderProperties> providers = new LinkedHashMap<>();

    /**
     * Per-request HTTP timeout (seconds). Keep it short so a stuck/slow key fails fast
     * and {@code AiCoreClient} can rotate to the next key instead of hanging.
     */
    private int timeoutSeconds = 30;

    /**
     * Retries performed silently inside the OpenAI SDK (it honours Groq's Retry-After on 429,
     * which can block a request for 10-20s+). Defaults to 0 because the application already
     * retries with key/model rotation via {@code @Retryable}.
     */
    private int sdkMaxRetries = 0;

    @Data
    public static class ProviderProperties {
        private List<String> apiKeys = new ArrayList<>();
        private String baseUrl;
        private List<String> model = new ArrayList<>();
        private Boolean exhaustModelsPerKey;
        private Integer timeoutSeconds;
        private Integer sdkMaxRetries;
    }

    /**
     * Resolves an effective {@link OpenAIProperties} snapshot for the given provider name.
     * Falls back to top-level properties when {@code providerName} matches {@code defaultProvider}
     * and no explicit provider entry is configured (preserving legacy config and unit tests).
     */
    public OpenAIProperties resolveProvider(String providerName) {
        OpenAIProperties resolved = new OpenAIProperties();
        resolved.setDefaultProvider(defaultProvider);
        resolved.setTimeoutSeconds(timeoutSeconds);
        resolved.setSdkMaxRetries(sdkMaxRetries);
        resolved.setExhaustModelsPerKey(exhaustModelsPerKey);

        if (providerName == null || providerName.isBlank()) {
            return resolved;
        }

        ProviderProperties matched = findProviderProperties(providerName.trim());
        if (matched != null) {
            resolved.setApiKeys(matched.getApiKeys() != null ? new ArrayList<>(matched.getApiKeys()) : new ArrayList<>());
            resolved.setBaseUrl(matched.getBaseUrl());
            resolved.setModel(matched.getModel() != null ? new ArrayList<>(matched.getModel()) : new ArrayList<>());
            if (matched.getExhaustModelsPerKey() != null) {
                resolved.setExhaustModelsPerKey(matched.getExhaustModelsPerKey());
            }
            if (matched.getTimeoutSeconds() != null) {
                resolved.setTimeoutSeconds(matched.getTimeoutSeconds());
            }
            if (matched.getSdkMaxRetries() != null) {
                resolved.setSdkMaxRetries(matched.getSdkMaxRetries());
            }
            return resolved;
        }

        if (defaultProvider != null && defaultProvider.trim().equalsIgnoreCase(providerName.trim())) {
            resolved.setApiKeys(apiKeys != null ? new ArrayList<>(apiKeys) : new ArrayList<>());
            resolved.setBaseUrl(baseUrl);
            resolved.setModel(model != null ? new ArrayList<>(model) : new ArrayList<>());
        }
        return resolved;
    }

    private ProviderProperties findProviderProperties(String providerName) {
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        ProviderProperties exact = providers.get(providerName);
        if (exact != null) {
            return exact;
        }
        String target = providerName.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, ProviderProperties> entry : providers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().toLowerCase(Locale.ROOT).equals(target)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
