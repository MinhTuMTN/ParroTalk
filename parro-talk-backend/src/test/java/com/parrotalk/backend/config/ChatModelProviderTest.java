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
}
