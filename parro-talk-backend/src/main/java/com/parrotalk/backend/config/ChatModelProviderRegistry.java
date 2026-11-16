package com.parrotalk.backend.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Registry of {@link ChatModelProvider} pools keyed by provider name (e.g. {@code groq}, {@code gemini}).
 * Keeps the default provider pointing to the primary {@link ChatModelProvider} bean so existing
 * listeners and callers share the same rotation/quarantine state.
 */
@Component
public class ChatModelProviderRegistry {

    private final String defaultProviderName;
    private final ChatModelProvider defaultProvider;
    private final Map<String, ChatModelProvider> providers;

    @Autowired
    public ChatModelProviderRegistry(OpenAIProperties properties, ChatModelProvider defaultProvider) {
        this.defaultProviderName = normalizeName(properties.getDefaultProvider(), "groq");
        this.defaultProvider = defaultProvider;

        Map<String, ChatModelProvider> map = new LinkedHashMap<>();
        map.put(this.defaultProviderName, defaultProvider);

        if (properties.getProviders() != null) {
            for (String rawName : properties.getProviders().keySet()) {
                if (rawName == null || rawName.isBlank()) {
                    continue;
                }
                String normalized = rawName.trim().toLowerCase(Locale.ROOT);
                if (normalized.equals(this.defaultProviderName)) {
                    continue;
                }
                map.put(normalized, new ChatModelProvider(properties.resolveProvider(rawName)));
            }
        }
        this.providers = Collections.unmodifiableMap(map);
    }

    public ChatModelProviderRegistry(Map<String, ChatModelProvider> providers, String defaultProviderName) {
        this.defaultProviderName = normalizeName(defaultProviderName, "groq");
        Map<String, ChatModelProvider> map = new LinkedHashMap<>();
        if (providers != null) {
            providers.forEach((k, v) -> {
                if (k != null && !k.isBlank() && v != null) {
                    map.put(k.trim().toLowerCase(Locale.ROOT), v);
                }
            });
        }
        this.defaultProvider = map.get(this.defaultProviderName);
        this.providers = Collections.unmodifiableMap(map);
    }

    public ChatModelProvider getDefaultProvider() {
        return defaultProvider;
    }

    public String getDefaultProviderName() {
        return defaultProviderName;
    }

    public Map<String, ChatModelProvider> getAllProviders() {
        return providers;
    }

    public Optional<ChatModelProvider> getProvider(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(providers.get(providerName.trim().toLowerCase(Locale.ROOT)));
    }

    private static String normalizeName(String name, String fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
