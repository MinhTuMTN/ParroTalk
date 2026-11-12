package com.parrotalk.backend.service;

import java.util.function.Function;

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
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public <T, R> R callAndValidate(
            String systemPrompt, 
            String userPrompt, 
            Class<T> responseType,
            Function<T, R> validator) {
        
        ChatModelNode node = chatModelProvider.current();
        try {
            T response = node.chatClient().prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .entity(responseType);
                    
            return validator.apply(response);
        } catch (Exception e) {
            chatModelProvider.next();
            log.warn("AI call or validation failed (model: {}). Rotating key and retrying... Error: {}", node.modelName(), e.getMessage());
            throw e;
        }
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
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public <T, R> R callAndValidate(
            String systemPrompt, 
            String userPrompt, 
            ParameterizedTypeReference<T> responseType,
            Function<T, R> validator) {
        
        ChatModelNode node = chatModelProvider.current();
        try {
            T response = node.chatClient().prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .entity(responseType);
                    
            return validator.apply(response);
        } catch (Exception e) {
            chatModelProvider.next();
            log.warn("AI call or validation failed (model: {}). Rotating key and retrying... Error: {}", node.modelName(), e.getMessage());
            throw e;
        }
    }
}
