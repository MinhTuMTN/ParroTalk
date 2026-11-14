package com.parrotalk.backend.service;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.parrotalk.backend.config.ChatModelProvider;
import com.parrotalk.backend.config.ChatModelProviderRegistry;
import com.parrotalk.backend.dto.ChatModelNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Generic AI client service providing structured prompt execution,
 * automatic key/model rotation on failure, optional multi-provider fallback,
 * and post-processing validation.
 *
 * <p>Each failed attempt quarantines bad/rate-limited keys when applicable and
 * rotates to the next healthy API key/model.
 *
 * @author MinhTuMTN
 */
@Component
@Slf4j
public class AiCoreClient {

    private static final int DEFAULT_MIN_ATTEMPTS = 3;
    private static final int DEFAULT_MAX_ATTEMPTS = 15;
    private static final long SAME_KEY_BACKOFF_MS = 200L;

    private final ChatModelProvider chatModelProvider;
    @Nullable
    private final ChatModelProviderRegistry providerRegistry;

    public AiCoreClient(ChatModelProvider chatModelProvider) {
        this(chatModelProvider, new ChatModelProviderRegistry(
                chatModelProvider != null ? Map.of("groq", chatModelProvider) : Map.of(),
                "groq"));
    }

    @Autowired
    public AiCoreClient(
            ChatModelProvider chatModelProvider,
            @Autowired(required = false) @Nullable ChatModelProviderRegistry providerRegistry) {
        this.chatModelProvider = chatModelProvider;
        this.providerRegistry = providerRegistry;
    }

    /**
     * Executes an AI prompt with the given system and user prompts on the default provider,
     * parses the output into the specified target class, and applies a custom validator/mapper.
     */
    public <T, R> R callAndValidate(
            String systemPrompt,
            String userPrompt,
            Class<T> responseType,
            Function<T, R> validator) {

        return executeWithRotation(
                chatModelProvider,
                responseType.getSimpleName(),
                node -> prompt(node, systemPrompt, userPrompt).responseEntity(responseType),
                validator);
    }

    /**
     * Executes an AI prompt using the specified ordered provider chain (falling back to the next
     * provider when all attempts on the current provider fail or when a provider has no keys).
     */
    public <T, R> R callAndValidate(
            List<String> providerOrder,
            String systemPrompt,
            String userPrompt,
            Class<T> responseType,
            Function<T, R> validator) {

        return executeWithProviderFallback(
                providerOrder,
                responseType.getSimpleName(),
                node -> prompt(node, systemPrompt, userPrompt).responseEntity(responseType),
                validator);
    }

    /**
     * Executes an AI prompt with generic return types (e.g. {@code List<T>}, {@code Map<K, V>})
     * on the default provider, parses the output using {@link ParameterizedTypeReference},
     * and applies a custom validator/mapper.
     */
    public <T, R> R callAndValidate(
            String systemPrompt,
            String userPrompt,
            ParameterizedTypeReference<T> responseType,
            Function<T, R> validator) {

        return executeWithRotation(
                chatModelProvider,
                responseType.getType().getTypeName(),
                node -> prompt(node, systemPrompt, userPrompt).responseEntity(responseType),
                validator);
    }

    /**
     * Executes an AI prompt with generic return types using the specified ordered provider chain.
     */
    public <T, R> R callAndValidate(
            List<String> providerOrder,
            String systemPrompt,
            String userPrompt,
            ParameterizedTypeReference<T> responseType,
            Function<T, R> validator) {

        return executeWithProviderFallback(
                providerOrder,
                responseType.getType().getTypeName(),
                node -> prompt(node, systemPrompt, userPrompt).responseEntity(responseType),
                validator);
    }

    private ChatClient.CallResponseSpec prompt(ChatModelNode node, String systemPrompt, String userPrompt) {
        return node.chatClient().prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call();
    }

    private <T, R> R executeWithProviderFallback(
            List<String> providerOrder,
            String target,
            Function<ChatModelNode, ResponseEntity<ChatResponse, T>> call,
            Function<T, R> validator) {

        if (providerOrder == null || providerOrder.isEmpty()) {
            return executeWithRotation(chatModelProvider, target, call, validator);
        }

        RuntimeException lastException = null;
        for (int i = 0; i < providerOrder.size(); i++) {
            String providerName = providerOrder.get(i);
            ChatModelProvider provider = providerRegistry != null
                    ? providerRegistry.getProvider(providerName).orElse(null)
                    : null;
            if (provider == null || provider.size() == 0) {
                log.debug("Skipping AI provider '{}' for [{}] (not configured or no API keys).",
                        providerName, target);
                continue;
            }
            final int nextProviderIdx = i + 1;
            try {
                return executeWithRotation(
                        provider,
                        target,
                        call,
                        validator,
                        () -> hasConfiguredProviderAfter(providerOrder, nextProviderIdx));
            } catch (RuntimeException e) {
                lastException = e;
                log.warn("AI provider '{}' exhausted for [{}], trying next provider if available. Error: {}",
                        providerName, target, e.getMessage());
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        throw new IllegalStateException("No AI chat models/API keys are configured for providers: " + providerOrder);
    }

    private boolean hasConfiguredProviderAfter(List<String> providerOrder, int startIndex) {
        if (providerRegistry == null) {
            return false;
        }
        for (int i = startIndex; i < providerOrder.size(); i++) {
            ChatModelProvider next = providerRegistry.getProvider(providerOrder.get(i)).orElse(null);
            if (next != null && next.size() > 0) {
                return true;
            }
        }
        return false;
    }

    private <T, R> R executeWithRotation(
            ChatModelProvider provider,
            String target,
            Function<ChatModelNode, ResponseEntity<ChatResponse, T>> call,
            Function<T, R> validator) {
        return executeWithRotation(provider, target, call, validator, () -> false);
    }

    private <T, R> R executeWithRotation(
            ChatModelProvider provider,
            String target,
            Function<ChatModelNode, ResponseEntity<ChatResponse, T>> call,
            Function<T, R> validator,
            BooleanSupplier hasFallbackProvider) {

        int maxAttempts = Math.max(DEFAULT_MIN_ATTEMPTS, Math.min(provider.size(), DEFAULT_MAX_ATTEMPTS));
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            ChatModelNode node = provider.current();
            try {
                return execute(node, target, () -> call.apply(node), validator);
            } catch (RuntimeException e) {
                lastException = e;
                provider.markFailed(node, e);
                if (provider.healthyCount() == 0 && hasFallbackProvider.getAsBoolean()) {
                    log.warn("AI call failed (model: {}, key: {}) and no healthy keys remain; falling back immediately. Error: {}",
                            node.modelName(), node.maskedApiKey(), e.getMessage());
                    break;
                }
                boolean hasMoreAttempts = attempt < maxAttempts;
                log.warn("AI call or validation failed on attempt {}/{} (model: {}, key: {}). {} Error: {}",
                        attempt, maxAttempts, node.modelName(), node.maskedApiKey(),
                        hasMoreAttempts ? "Rotating key and retrying..." : "No more attempts.",
                        e.getMessage());
                if (hasMoreAttempts && provider.size() <= 1) {
                    sleepQuietly(SAME_KEY_BACKOFF_MS * attempt);
                }
            }
        }
        throw lastException;
    }

    private <T, R> R execute(
            ChatModelNode node,
            String target,
            Supplier<ResponseEntity<ChatResponse, T>> call,
            Function<T, R> validator) {

        long startNs = System.nanoTime();
        ResponseEntity<ChatResponse, T> result = call.get();
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
        logUsage(node, target, elapsedMs, result.response());
        return validator.apply(result.entity());
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void logUsage(ChatModelNode node, String target, long elapsedMs, ChatResponse response) {
        Integer promptTokens = null;
        Integer completionTokens = null;
        if (response != null && response.getMetadata() != null) {
            Usage usage = response.getMetadata().getUsage();
            if (usage != null) {
                promptTokens = usage.getPromptTokens();
                completionTokens = usage.getCompletionTokens();
            }
        }
        log.info("AI call [{}] done in {} ms (model: {}, key: {}, promptTokens: {}, completionTokens: {})",
                target, elapsedMs, node.modelName(), node.maskedApiKey(), promptTokens, completionTokens);
    }
}
