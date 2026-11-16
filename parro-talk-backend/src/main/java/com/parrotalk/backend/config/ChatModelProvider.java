package com.parrotalk.backend.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private final Set<String> disabledKeys = ConcurrentHashMap.newKeySet();
    private final Map<String, String> lastErrors = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastErrorTimes = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastCheckedTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> lastLatencyMs = new ConcurrentHashMap<>();
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
        if (properties.isExhaustModelsPerKey()) {
            // Iterate apiKey outer, model inner so one key exhausts its models before rotating keys.
            for (String apiKey : properties.getApiKeys()) {
                for (String modelName : properties.getModel()) {
                    addNodeIfValid(result, properties, apiKey, modelName);
                }
            }
        } else {
            // Iterate model outer, apiKey inner so next() switches API key first.
            for (String modelName : properties.getModel()) {
                for (String apiKey : properties.getApiKeys()) {
                    addNodeIfValid(result, properties, apiKey, modelName);
                }
            }
        }
        return result;
    }

    private static void addNodeIfValid(
            List<ChatModelNode> target,
            OpenAIProperties properties,
            String apiKey,
            String modelName) {
        if (modelName == null || modelName.isBlank() || apiKey == null || apiKey.isBlank()) {
            return;
        }
        String trimmedModel = modelName.trim();
        String trimmedKey = apiKey.trim();
        target.add(new ChatModelNode(
                ChatClient.builder(createChatModel(properties, trimmedKey, trimmedModel)).build(),
                trimmedModel,
                maskedApiKey(trimmedKey)));
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
            String maskedKey = node.maskedApiKey();
            String msg = rootMessage(error);
            lastErrors.put(maskedKey, msg);
            lastErrorTimes.put(maskedKey, clock.instant());

            Duration quarantine = classifyQuarantineDuration(error);
            if (!quarantine.isZero()) {
                Instant until = clock.instant().plus(quarantine);
                quarantinedKeys.put(maskedKey, until);
                log.warn("Quarantined AI key {} for {} due to error: {}",
                        maskedKey, quarantine, msg);
            }
        }
        return next();
    }

    public boolean isQuarantined(ChatModelNode node) {
        if (node == null || node.maskedApiKey() == null) {
            return false;
        }
        if (disabledKeys.contains(node.maskedApiKey())) {
            return true;
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

    public List<ChatModelNode> getNodes() {
        return nodes;
    }

    public int getCurrentIndex() {
        return current.get();
    }

    public boolean isDisabled(String maskedApiKey) {
        return maskedApiKey != null && disabledKeys.contains(maskedApiKey);
    }

    public void setDisabled(String maskedApiKey, boolean disabled) {
        if (maskedApiKey == null || maskedApiKey.isBlank()) {
            return;
        }
        if (disabled) {
            disabledKeys.add(maskedApiKey);
            ChatModelNode active = nodes.isEmpty() ? null : nodes.get(current.get());
            if (active != null && maskedApiKey.equals(active.maskedApiKey())) {
                next();
            }
        } else {
            disabledKeys.remove(maskedApiKey);
        }
    }

    public void resetKey(String maskedApiKey) {
        if (maskedApiKey == null || maskedApiKey.isBlank()) {
            return;
        }
        quarantinedKeys.remove(maskedApiKey);
        disabledKeys.remove(maskedApiKey);
        lastErrors.remove(maskedApiKey);
        lastErrorTimes.remove(maskedApiKey);
    }

    public void resetAll() {
        quarantinedKeys.clear();
        disabledKeys.clear();
        lastErrors.clear();
        lastErrorTimes.clear();
    }

    public Instant getQuarantinedUntil(String maskedApiKey) {
        if (maskedApiKey == null) {
            return null;
        }
        Instant until = quarantinedKeys.get(maskedApiKey);
        if (until != null && !clock.instant().isBefore(until)) {
            quarantinedKeys.remove(maskedApiKey, until);
            return null;
        }
        return until;
    }

    public String getLastError(String maskedApiKey) {
        return maskedApiKey != null ? lastErrors.get(maskedApiKey) : null;
    }

    public Instant getLastErrorAt(String maskedApiKey) {
        return maskedApiKey != null ? lastErrorTimes.get(maskedApiKey) : null;
    }

    public Instant getLastCheckedAt(String maskedApiKey) {
        return maskedApiKey != null ? lastCheckedTimes.get(maskedApiKey) : null;
    }

    public Long getLastLatencyMs(String maskedApiKey) {
        return maskedApiKey != null ? lastLatencyMs.get(maskedApiKey) : null;
    }

    public void recordCheckSuccess(String maskedApiKey, long latencyMs) {
        if (maskedApiKey == null) {
            return;
        }
        Instant now = clock.instant();
        quarantinedKeys.remove(maskedApiKey);
        lastErrors.remove(maskedApiKey);
        lastErrorTimes.remove(maskedApiKey);
        lastCheckedTimes.put(maskedApiKey, now);
        lastLatencyMs.put(maskedApiKey, latencyMs);
    }

    public void recordCheckFailure(ChatModelNode node, Throwable error, long latencyMs) {
        if (node == null || node.maskedApiKey() == null) {
            return;
        }
        String maskedKey = node.maskedApiKey();
        Instant now = clock.instant();
        String msg = rootMessage(error);
        lastCheckedTimes.put(maskedKey, now);
        lastLatencyMs.put(maskedKey, latencyMs);
        lastErrors.put(maskedKey, msg);
        lastErrorTimes.put(maskedKey, now);

        Duration quarantine = classifyQuarantineDuration(error);
        if (!quarantine.isZero()) {
            quarantinedKeys.put(maskedKey, now.plus(quarantine));
        }
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
