package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;

import com.parrotalk.backend.config.ChatModelProvider;
import com.parrotalk.backend.dto.ChatModelNode;

@ExtendWith(MockitoExtension.class)
class AiCoreClientTest {

    @Mock
    private ChatModelProvider chatModelProvider;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private AiCoreClient aiCoreClient;

    @BeforeEach
    void setUp() {
        aiCoreClient = new AiCoreClient(chatModelProvider);
    }

    @Test
    @DisplayName("callAndValidate with Class<T> should succeed and apply validator")
    void testCallAndValidate_WithClass_Success() {
        // Arrange
        String systemPrompt = "You are a helpful assistant.";
        String userPrompt = "Classify this text.";
        String rawResponse = "Success Response";
        Integer expectedValidatedResult = 100;

        ChatModelNode chatModelNode = new ChatModelNode(chatClient, "gpt-4o-mini", "sk-***1234");
        when(chatModelProvider.current()).thenReturn(chatModelNode);

        when(chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .entity(String.class)).thenReturn(rawResponse);

        Function<String, Integer> validator = resp -> {
            assertEquals(rawResponse, resp);
            return expectedValidatedResult;
        };

        // Act
        Integer result = aiCoreClient.callAndValidate(systemPrompt, userPrompt, String.class, validator);

        // Assert
        assertNotNull(result);
        assertEquals(expectedValidatedResult, result);
        verify(chatModelProvider, times(1)).current();
    }

    @Test
    @DisplayName("callAndValidate with Class<T> should rethrow exception when ChatClient fails")
    void testCallAndValidate_WithClass_ChatClientThrowsException() {
        // Arrange
        String systemPrompt = "You are a helpful assistant.";
        String userPrompt = "Classify this text.";

        ChatModelNode chatModelNode = new ChatModelNode(chatClient, "gpt-4o-mini", "sk-***1234");
        when(chatModelProvider.current()).thenReturn(chatModelNode);

        when(chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .entity(String.class)).thenThrow(new RuntimeException("OpenAI rate limit exceeded"));

        Function<String, Integer> validator = String::length;

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> aiCoreClient.callAndValidate(systemPrompt, userPrompt, String.class, validator)
        );

        assertEquals("OpenAI rate limit exceeded", exception.getMessage());
        verify(chatModelProvider, times(1)).current();
    }

    @Test
    @DisplayName("callAndValidate with Class<T> should rethrow exception when validator fails")
    void testCallAndValidate_WithClass_ValidatorThrowsException() {
        // Arrange
        String systemPrompt = "System";
        String userPrompt = "User";
        String rawResponse = "Invalid Data";

        ChatModelNode chatModelNode = new ChatModelNode(chatClient, "gpt-4o-mini", "sk-***1234");
        when(chatModelProvider.current()).thenReturn(chatModelNode);

        when(chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .entity(String.class)).thenReturn(rawResponse);

        Function<String, Integer> validator = resp -> {
            throw new IllegalArgumentException("Validation failed for: " + resp);
        };

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiCoreClient.callAndValidate(systemPrompt, userPrompt, String.class, validator)
        );

        assertEquals("Validation failed for: Invalid Data", exception.getMessage());
    }

    @Test
    @DisplayName("callAndValidate with ParameterizedTypeReference<T> should succeed and apply validator")
    void testCallAndValidate_WithParameterizedTypeReference_Success() {
        // Arrange
        String systemPrompt = "You are a helpful assistant.";
        String userPrompt = "List items.";
        List<String> rawList = List.of("A", "B", "C");
        int expectedSize = 3;

        ChatModelNode chatModelNode = new ChatModelNode(chatClient, "gpt-4o-mini", "sk-***1234");
        when(chatModelProvider.current()).thenReturn(chatModelNode);

        ParameterizedTypeReference<List<String>> typeRef = new ParameterizedTypeReference<>() {};
        when(chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .entity(typeRef)).thenReturn(rawList);

        Function<List<String>, Integer> validator = List::size;

        // Act
        Integer result = aiCoreClient.callAndValidate(systemPrompt, userPrompt, typeRef, validator);

        // Assert
        assertNotNull(result);
        assertEquals(expectedSize, result);
        verify(chatModelProvider, times(1)).current();
    }

    @Test
    @DisplayName("callAndValidate with ParameterizedTypeReference<T> should rethrow exception when ChatClient fails")
    void testCallAndValidate_WithParameterizedTypeReference_ChatClientThrowsException() {
        // Arrange
        String systemPrompt = "System";
        String userPrompt = "User";

        ChatModelNode chatModelNode = new ChatModelNode(chatClient, "gpt-4o-mini", "sk-***1234");
        when(chatModelProvider.current()).thenReturn(chatModelNode);

        ParameterizedTypeReference<List<String>> typeRef = new ParameterizedTypeReference<>() {};
        when(chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .entity(typeRef)).thenThrow(new RuntimeException("Network timeout"));

        Function<List<String>, Integer> validator = List::size;

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> aiCoreClient.callAndValidate(systemPrompt, userPrompt, typeRef, validator)
        );

        assertEquals("Network timeout", exception.getMessage());
    }
}
