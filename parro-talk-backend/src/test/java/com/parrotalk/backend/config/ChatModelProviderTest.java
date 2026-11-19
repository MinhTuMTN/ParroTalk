package com.parrotalk.backend.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import com.parrotalk.backend.dto.ChatModelNode;

class ChatModelProviderTest {

    private static class MutableClock extends Clock {
        private final AtomicReference<Instant> current;

        MutableClock(Instant initial) {
            this.current = new AtomicReference<>(initial);
        }

        void advanceSeconds(long seconds) {
            current.updateAndGet(t -> t.plusSeconds(seconds));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current.get();
        }
    }

    @Test
    @DisplayName("markFailed should quarantine restricted key for 6 hours and skip it on current/next")
    void markFailed_QuarantinesRestrictedKey() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-14T09:00:00Z"));
        ChatModelNode node1 = new ChatModelNode(mock(ChatClient.class), "qwen/qwen3.8-27b", "gsk_...0001");
        ChatModelNode node2 = new ChatModelNode(mock(ChatClient.class), "qwen/qwen3.8-27b", "gsk_...0002");
        ChatModelNode node3 = new ChatModelNode(mock(ChatClient.class), "qwen/qwen3.8-27b", "gsk_...0003");

        ChatModelProvider provider = new ChatModelProvider(List.of(node1, node2, node3), clock);

        assertEquals(node1, provider.current());
        assertEquals(3, provider.healthyCount());

        ChatModelNode afterFail = provider.markFailed(
                node1,
                new RuntimeException("400: Organization has been restricted. Please reach out to support."));

        assertEquals(node2, afterFail);
        assertTrue(provider.isQuarantined(node1));
        assertEquals(2, provider.healthyCount());

        // Rotating through remaining healthy nodes should skip node1
        assertEquals(node3, provider.next());
        assertEquals(node2, provider.next());
        assertEquals(node2, provider.current());

        // After 6 hours + 1 second, node1 becomes healthy again
        clock.advanceSeconds(6 * 3600 + 1);
        assertFalse(provider.isQuarantined(node1));
        assertEquals(3, provider.healthyCount());
    }

    @Test
    @DisplayName("markFailed should cooldown rate-limited key for 30 seconds")
    void markFailed_CooldownsRateLimitedKey() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-14T09:00:00Z"));
        ChatModelNode node1 = new ChatModelNode(mock(ChatClient.class), "qwen/qwen3.8-27b", "gsk_...0001");
        ChatModelNode node2 = new ChatModelNode(mock(ChatClient.class), "qwen/qwen3.8-27b", "gsk_...0002");

        ChatModelProvider provider = new ChatModelProvider(List.of(node1, node2), clock);

        provider.markFailed(node1, new RuntimeException("429 Too Many Requests: rate limit exceeded"));
        assertTrue(provider.isQuarantined(node1));
        assertEquals(node2, provider.current());

        clock.advanceSeconds(31);
        assertFalse(provider.isQuarantined(node1));
    }

    @Test
    @DisplayName("next should still rotate if all keys are quarantined")
    void next_AllQuarantined_RotatesFallback() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-14T09:00:00Z"));
        ChatModelNode node1 = new ChatModelNode(mock(ChatClient.class), "qwen/qwen3.8-27b", "gsk_...0001");
        ChatModelNode node2 = new ChatModelNode(mock(ChatClient.class), "qwen/qwen3.8-27b", "gsk_...0002");

        ChatModelProvider provider = new ChatModelProvider(List.of(node1, node2), clock);

        provider.markFailed(node1, new RuntimeException("400: Organization has been restricted"));
        provider.markFailed(node2, new RuntimeException("400: Organization has been restricted"));

        assertEquals(0, provider.healthyCount());
        assertEquals(node1, provider.current());
        assertEquals(node2, provider.next());
    }

    @Test
    @DisplayName("OpenAIProperties constructor should interleave keys before models and ignore blanks")
    void constructor_InterleavesKeysFirstAndIgnoresBlanks() {
        OpenAIProperties props = new OpenAIProperties();
        props.setBaseUrl("https://api.groq.com/openai/v1");
        props.setApiKeys(List.of("gsk_key_1111", " ", "gsk_key_2222"));
        props.setModel(List.of("model-a", "model-b"));

        ChatModelProvider provider = new ChatModelProvider(props);

        assertEquals(4, provider.size());
        assertEquals("model-a", provider.current().modelName());
        assertEquals("gsk_...1111", provider.current().maskedApiKey());

        ChatModelNode second = provider.next();
        assertEquals("model-a", second.modelName());
        assertEquals("gsk_...2222", second.maskedApiKey());
    }

    @Test
    @DisplayName("resolveProvider should return provider-specific config with inherited defaults and fallback to top-level for defaultProvider")
    void resolveProvider_ResolvesConfiguredAndLegacyProperties() {
        OpenAIProperties props = new OpenAIProperties();
        props.setDefaultProvider("groq");
        props.setTimeoutSeconds(45);
        props.setSdkMaxRetries(1);

        OpenAIProperties.ProviderProperties groq = new OpenAIProperties.ProviderProperties();
        groq.setBaseUrl("https://api.groq.com/openai/v1");
        groq.setApiKeys(List.of("gsk_1111"));
        groq.setModel(List.of("qwen/qwen3.8-27b"));

        OpenAIProperties.ProviderProperties gemini = new OpenAIProperties.ProviderProperties();
        gemini.setBaseUrl("https://generativelanguage.googleapis.com/v1beta/openai");
        gemini.setApiKeys(List.of("AIza_1111", "AIza_2222"));
        gemini.setModel(List.of("gemini-3.5-flash-lite"));

        props.getProviders().put("groq", groq);
        props.getProviders().put("gemini", gemini);

        OpenAIProperties resolvedGemini = props.resolveProvider("gemini");
        assertEquals("https://generativelanguage.googleapis.com/v1beta/openai", resolvedGemini.getBaseUrl());
        assertEquals(List.of("AIza_1111", "AIza_2222"), resolvedGemini.getApiKeys());
        assertEquals(List.of("gemini-3.5-flash-lite"), resolvedGemini.getModel());
        assertEquals(45, resolvedGemini.getTimeoutSeconds());
        assertEquals(1, resolvedGemini.getSdkMaxRetries());

        OpenAIProperties missing = props.resolveProvider("unknown");
        assertTrue(missing.getApiKeys().isEmpty());
        assertTrue(missing.getModel().isEmpty());
    }

    @Test
    @DisplayName("ChatModelProviderRegistry should isolate groq and gemini pools and expose default provider")
    void registry_BuildsIndependentProviderPools() {
        OpenAIProperties props = new OpenAIProperties();
        props.setDefaultProvider("groq");

        OpenAIProperties.ProviderProperties groq = new OpenAIProperties.ProviderProperties();
        groq.setBaseUrl("https://api.groq.com/openai/v1");
        groq.setApiKeys(List.of("gsk_key_1111"));
        groq.setModel(List.of("qwen/qwen3.8-27b"));

        OpenAIProperties.ProviderProperties gemini = new OpenAIProperties.ProviderProperties();
        gemini.setBaseUrl("https://generativelanguage.googleapis.com/v1beta/openai");
        gemini.setApiKeys(List.of("AIza_key_1111", "AIza_key_2222"));
        gemini.setModel(List.of("gemini-3.5-flash-lite"));

        props.getProviders().put("groq", groq);
        props.getProviders().put("gemini", gemini);

        ChatModelProvider defaultProvider = new ChatModelProvider(props);
        ChatModelProviderRegistry registry = new ChatModelProviderRegistry(props, defaultProvider);

        assertSame(defaultProvider, registry.getDefaultProvider());
        assertSame(defaultProvider, registry.getProvider("groq").orElseThrow());
        assertEquals("qwen/qwen3.8-27b", defaultProvider.current().modelName());

        ChatModelProvider geminiProvider = registry.getProvider("GEMINI").orElseThrow();
        assertEquals(2, geminiProvider.size());
        assertEquals("gemini-3.5-flash-lite", geminiProvider.current().modelName());
        assertEquals("AIza...1111", geminiProvider.current().maskedApiKey());

        geminiProvider.markFailed(geminiProvider.current(), new RuntimeException("429 rate limit exceeded"));
        assertEquals(1, geminiProvider.healthyCount());
        assertEquals(1, defaultProvider.healthyCount());
    }

    @Test
    @DisplayName("resolveProvider should support per-provider timeout/retry overrides and legacy top-level fallback")
    void resolveProvider_SupportsOverridesAndLegacyTopLevelFallback() {
        OpenAIProperties props = new OpenAIProperties();
        props.setDefaultProvider("groq");
        props.setTimeoutSeconds(30);
        props.setSdkMaxRetries(0);
        props.setBaseUrl("https://api.groq.com/openai/v1");
        props.setApiKeys(List.of("gsk_legacy_1"));
        props.setModel(List.of("qwen/qwen3.8-27b"));

        OpenAIProperties resolvedLegacy = props.resolveProvider("groq");
        assertEquals("https://api.groq.com/openai/v1", resolvedLegacy.getBaseUrl());
        assertEquals(List.of("gsk_legacy_1"), resolvedLegacy.getApiKeys());
        assertEquals(List.of("qwen/qwen3.8-27b"), resolvedLegacy.getModel());

        OpenAIProperties.ProviderProperties gemini = new OpenAIProperties.ProviderProperties();
        gemini.setBaseUrl("https://generativelanguage.googleapis.com/v1beta/openai");
        gemini.setApiKeys(List.of("AIza_1"));
        gemini.setModel(List.of("gemini-3.5-flash-lite"));
        gemini.setTimeoutSeconds(15);
        gemini.setSdkMaxRetries(2);
        props.getProviders().put("gemini", gemini);

        OpenAIProperties resolvedGemini = props.resolveProvider("gemini");
        assertEquals(15, resolvedGemini.getTimeoutSeconds());
        assertEquals(2, resolvedGemini.getSdkMaxRetries());
    }

    @Test
    @DisplayName("exhaustModelsPerKey should exhaust all models on key1 before moving to key2")
    void constructor_ExhaustModelsPerKey_OrdersKeyOuterModelInner() {
        OpenAIProperties props = new OpenAIProperties();
        props.setBaseUrl("https://generativelanguage.googleapis.com/v1beta/openai");
        props.setApiKeys(List.of("AIza_key_1111", "AIza_key_2222"));
        props.setModel(List.of("gemini-3.1-flash-lite", "gemma-4-31b-it"));
        props.setExhaustModelsPerKey(true);

        ChatModelProvider provider = new ChatModelProvider(props);
        assertEquals(4, provider.size());
        assertEquals("AIza...1111", provider.current().maskedApiKey());
        assertEquals("gemini-3.1-flash-lite", provider.current().modelName());

        ChatModelNode second = provider.next();
        assertEquals("AIza...1111", second.maskedApiKey());
        assertEquals("gemma-4-31b-it", second.modelName());

        ChatModelNode third = provider.next();
        assertEquals("AIza...2222", third.maskedApiKey());
        assertEquals("gemini-3.1-flash-lite", third.modelName());
    }

    @Test
    @DisplayName("setDisabled, recordCheckFailure, recordCheckSuccess, and resetKey manage runtime key states")
    void runtimeMonitoringAndToggleControls() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-17T00:00:00Z"));
        ChatModelNode node1 = new ChatModelNode(mock(ChatClient.class), "gemini-3.1-flash-lite", "AIza...1111");
        ChatModelNode node2 = new ChatModelNode(mock(ChatClient.class), "gemini-3.1-flash-lite", "AIza...2222");

        ChatModelProvider provider = new ChatModelProvider(List.of(node1, node2), clock);

        provider.setDisabled("AIza...1111", true);
        assertTrue(provider.isDisabled("AIza...1111"));
        assertTrue(provider.isQuarantined(node1));
        assertEquals(node2, provider.current());

        provider.resetKey("AIza...1111");
        assertFalse(provider.isDisabled("AIza...1111"));
        assertFalse(provider.isQuarantined(node1));

        provider.recordCheckFailure(node1, new RuntimeException("429 rate limit exceeded"), 120L);
        assertEquals("429 rate limit exceeded", provider.getLastError("AIza...1111"));
        assertEquals(120L, provider.getLastLatencyMs("AIza...1111"));
        assertNotNull(provider.getQuarantinedUntil("AIza...1111"));

        provider.recordCheckSuccess("AIza...1111", 85L);
        assertNull(provider.getLastError("AIza...1111"));
        assertNull(provider.getQuarantinedUntil("AIza...1111"));
        assertEquals(85L, provider.getLastLatencyMs("AIza...1111"));
    }

    @Test
    @DisplayName("markFailed should apply 30s cooldown on 503 and Request failed timeouts")
    void markFailed_AppliesShortCooldownOn503AndTimeout() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-17T00:00:00Z"));
        ChatModelNode node1 = new ChatModelNode(mock(ChatClient.class), "gemini-3.5-flash-lite", "AIza...1111");
        ChatModelNode node2 = new ChatModelNode(mock(ChatClient.class), "gemini-3.5-flash-lite", "AQ.A...o1eg");
        ChatModelNode node3 = new ChatModelNode(mock(ChatClient.class), "gemini-3.5-flash-lite", "AIza...2222");

        ChatModelProvider provider = new ChatModelProvider(List.of(node1, node2, node3), clock);

        provider.markFailed(node1, new RuntimeException("503: null"));
        assertTrue(provider.isQuarantined(node1));

        provider.markFailed(node2, new RuntimeException("Request failed"));
        assertTrue(provider.isQuarantined(node2));
        assertEquals(1, provider.healthyCount());
        assertEquals(node3, provider.current());

        clock.advanceSeconds(31);
        assertFalse(provider.isQuarantined(node1));
        assertFalse(provider.isQuarantined(node2));
        assertEquals(3, provider.healthyCount());
    }
}
