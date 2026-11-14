package com.parrotalk.backend.service;

import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.parrotalk.backend.config.ChatModelProvider;
import com.parrotalk.backend.dto.ChatModelNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Generic AI client service providing structured prompt execution,
 * automatic retries, and post-processing validation.
 *
 * <p>Each failed attempt rotates to the next API key/model, so the retry backoff is
 * intentionally short: waiting is pointless when the next attempt uses a different key.
 *
 * @author MinhTuMTN
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiCoreClient {

    private final ChatModelProvider chatModelProvider;

    /**
     * Executes an AI prompt with the given system and user prompts, parses the output
     * into the specified target class, and applies a custom validator/mapper.
     *
     * @param <T> Target class type representing the structured LLM response entity
     * @param <R> Return type produced by the validator/mapper function
     * @param systemPrompt System instructions for the AI model
     * @param userPrompt User input/prompt context
     * @param responseType Class reference of the expected LLM response entity
     * @param validator Mapping and validation function applied to the LLM response
     * @return Transformed and validated result
     */
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 300, multiplier = 2.0)
    )
    public <T, R> R callAndValidate(
            String systemPrompt,
            String userPrompt,
            Class<T> responseType,
            Function<T, R> validator) {

        ChatModelNode node = chatModelProvider.current();
        return execute(node, responseType.getSimpleName(),
                () -> prompt(node, systemPrompt, userPrompt).responseEntity(responseType),
                validator);
    }

    /**
     * Executes an AI prompt with generic return types (e.g. {@code List<T>}, {@code Map<K, V>}),
     * parses the output using {@link ParameterizedTypeReference}, and applies a custom validator/mapper.
     *
     * @param <T> Generic type reference of the structured LLM response
     * @param <R> Return type produced by the validator/mapper function
     * @param systemPrompt System instructions for the AI model
     * @param userPrompt User input/prompt context
     * @param responseType {@link ParameterizedTypeReference} describing the generic response type
     * @param validator Mapping and validation function applied to the LLM response
     * @return Transformed and validated result
     */
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 300, multiplier = 2.0)
    )
    public <T, R> R callAndValidate(
            String systemPrompt,
            String userPrompt,
            ParameterizedTypeReference<T> responseType,
            Function<T, R> validator) {

        ChatModelNode node = chatModelProvider.current();
        return execute(node, responseType.getType().getTypeName(),
                () -> prompt(node, systemPrompt, userPrompt).responseEntity(responseType),
                validator);
    }

    private ChatClient.CallResponseSpec prompt(ChatModelNode node, String systemPrompt, String userPrompt) {
        return node.chatClient().prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call();
    }

    private <T, R> R execute(
            ChatModelNode node,
            String target,
            Supplier<ResponseEntity<ChatResponse, T>> call,
            Function<T, R> validator) {

        long startNs = System.nanoTime();
        try {
            ResponseEntity<ChatResponse, T> result = call.get();
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            logUsage(node, target, elapsedMs, result.response());
            return validator.apply(result.entity());
        } catch (Exception e) {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            chatModelProvider.next();
            log.warn("AI call or validation failed after {} ms (model: {}, key: {}). Rotating key and retrying... Error: {}",
                    elapsedMs, node.modelName(), node.maskedApiKey(), e.getMessage());
            throw e;
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
