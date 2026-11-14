package com.parrotalk.backend.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.openai.models.ReasoningEffort;
import com.parrotalk.backend.dto.ChatModelNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ChatModelProvider {

    private static final Duration PERMANENT_KEY_QUARANTINE = Duration.ofHours(6);
    private static final Duration RATE_LIMIT_COOLDOWN = Duration.ofSeconds(30);

    private final List<ChatModelNode> nodes;
    private final AtomicInteger current = new AtomicInteger();
    private final Map<String, Instant> quarantinedKeys = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public ChatModelProvider(OpenAIProperties properties) {
        this(buildNodes(properties.resolveProvider(properties.getDefaultProvider())), Clock.systemUTC());
    }

    ChatModelProvider(List<ChatModelNode> nodes, Clock clock) {
        this.nodes = List.copyOf(nodes);
        this.clock = clock;
    }

    private static List<ChatModelNode> buildNodes(OpenAIProperties properties) {
        List<ChatModelNode> result = new ArrayList<>();
        // Iterate model outer, apiKey inner so next() always switches API key first.
        for (String modelName : properties.getModel()) {
            if (modelName == null || modelName.isBlank()) {
                continue;
            }
            String trimmedModel = modelName.trim();
            for (String apiKey : properties.getApiKeys()) {
                if (apiKey == null || apiKey.isBlank()) {
                    continue;
                }
                String trimmedKey = apiKey.trim();
                result.add(new ChatModelNode(
                        ChatClient.builder(createChatModel(properties, trimmedKey, trimmedModel)).build(),
                        trimmedModel,
                        maskedApiKey(trimmedKey)));
            }
        }
        return result;
    }

    private static OpenAiChatModel createChatModel(OpenAIProperties properties, String apiKey, String modelName) {
        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder()
                .apiKey(apiKey)
                .baseUrl(properties.getBaseUrl())
                .model(modelName)
                .maxTokens(4096)
                .temperature(0.1)
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .maxRetries(properties.getSdkMaxRetries());

        if ("qwen/qwen3.8-27b".equals(modelName)) {
            options.reasoningEffort(ReasoningEffort.NONE.asString());
        }

        return OpenAiChatModel.builder()
                .options(options.build())
                .build();
    }

    private static String maskedApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 4)
            return apiKey;
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }

    public ChatModelNode current() {
        ensureConfigured();
        int idx = current.get();
        ChatModelNode candidate = nodes.get(idx);
        if (!isQuarantined(candidate)) {
            return candidate;
        }
        for (int offset = 1; offset < nodes.size(); offset++) {
            int nextIdx = Math.floorMod(idx + offset, nodes.size());
            ChatModelNode nextCandidate = nodes.get(nextIdx);
            if (!isQuarantined(nextCandidate)) {
                current.set(nextIdx);
                return nextCandidate;
            }
        }
        return candidate;
    }

    public ChatModelNode next() {
        ensureConfigured();
        int startIdx = current.get();
        for (int offset = 1; offset <= nodes.size(); offset++) {
            int nextIdx = Math.floorMod(startIdx + offset, nodes.size());
            ChatModelNode candidate = nodes.get(nextIdx);
            if (!isQuarantined(candidate)) {
                current.set(nextIdx);
                return candidate;
            }
        }
        int fallbackIdx = Math.floorMod(startIdx + 1, nodes.size());
        current.set(fallbackIdx);
        return nodes.get(fallbackIdx);
    }

    /**
     * Records a failure on the given node, quarantining its API key when the error indicates
     * a restricted/invalid key or rate limit, and advances rotation to the next available node.
     */
    public ChatModelNode markFailed(ChatModelNode node, Throwable error) {
        if (node != null && node.maskedApiKey() != null) {
            Duration quarantine = classifyQuarantineDuration(error);
            if (!quarantine.isZero()) {
                Instant until = clock.instant().plus(quarantine);
                quarantinedKeys.put(node.maskedApiKey(), until);
                log.warn("Quarantined AI key {} for {} due to error: {}",
                        node.maskedApiKey(), quarantine, rootMessage(error));
            }
        }
        return next();
    }

    public boolean isQuarantined(ChatModelNode node) {
        if (node == null || node.maskedApiKey() == null) {
            return false;
        }
        Instant until = quarantinedKeys.get(node.maskedApiKey());
        if (until == null) {
            return false;
        }
        if (clock.instant().isBefore(until)) {
            return true;
        }
        quarantinedKeys.remove(node.maskedApiKey(), until);
        return false;
    }

    public int healthyCount() {
        int count = 0;
        for (ChatModelNode node : nodes) {
            if (!isQuarantined(node)) {
                count++;
            }
        }
        return count;
    }

    public int size() {
        return nodes.size();
    }

    private void ensureConfigured() {
        if (nodes.isEmpty()) {
            throw new IllegalStateException("No AI chat models/API keys are configured.");
        }
    }

    private static Duration classifyQuarantineDuration(Throwable error) {
        Throwable cursor = error;
        for (int depth = 0; cursor != null && depth < 10; depth++) {
            String msg = cursor.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase(Locale.ROOT);
                if (lower.contains("organization has been restricted")
                        || lower.contains("invalid_api_key")
                        || lower.contains("invalid api key")
                        || lower.contains("api_key_invalid")
                        || lower.contains("permission_denied")
                        || lower.contains("incorrect api key")
                        || lower.contains("account deactivated")
                        || lower.contains("api key expired")
                        || lower.contains("401")) {
                    return PERMANENT_KEY_QUARANTINE;
                }
                if (lower.contains("429")
                        || lower.contains("rate_limit_exceeded")
                        || lower.contains("rate limit")
                        || lower.contains("too many requests")) {
                    return RATE_LIMIT_COOLDOWN;
                }
            }
            cursor = cursor.getCause();
        }
        return Duration.ZERO;
    }

    private static String rootMessage(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        return error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
    }

}
