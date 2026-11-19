package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;

import com.parrotalk.backend.config.ChatModelProvider;
import com.parrotalk.backend.config.ChatModelProviderRegistry;
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
                .responseEntity(String.class)).thenReturn(new ResponseEntity<>(null, rawResponse));

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
                .responseEntity(String.class)).thenThrow(new RuntimeException("OpenAI rate limit exceeded"));

        Function<String, Integer> validator = String::length;

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> aiCoreClient.callAndValidate(systemPrompt, userPrompt, String.class, validator)
        );

        assertEquals("OpenAI rate limit exceeded", exception.getMessage());
        verify(chatModelProvider, times(3)).current();
        verify(chatModelProvider, times(3)).markFailed(eq(chatModelNode), any(RuntimeException.class));
    }

    @Test
    @DisplayName("callAndValidate should rotate past >3 restricted keys and succeed on healthy key")
    void testCallAndValidate_RotatesPastMoreThanThreeRestrictedKeys() {
        ChatClient badClient1 = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        ChatClient badClient2 = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        ChatClient badClient3 = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        ChatClient goodClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);

        ChatModelNode node1 = new ChatModelNode(badClient1, "qwen/qwen3.8-27b", "gsk_...0001");
        ChatModelNode node2 = new ChatModelNode(badClient2, "qwen/qwen3.8-27b", "gsk_...0002");
        ChatModelNode node3 = new ChatModelNode(badClient3, "qwen/qwen3.8-27b", "gsk_...0003");
        ChatModelNode node4 = new ChatModelNode(goodClient, "qwen/qwen3.8-27b", "gsk_...0004");

        when(chatModelProvider.size()).thenReturn(4);
        when(chatModelProvider.current()).thenReturn(node1, node2, node3, node4);

        RuntimeException restricted = new RuntimeException(
                "400: Organization has been restricted. Please reach out to support if you believe this was in error.");
        when(badClient1.prompt().system("sys").user("usr").call().responseEntity(String.class)).thenThrow(restricted);
        when(badClient2.prompt().system("sys").user("usr").call().responseEntity(String.class)).thenThrow(restricted);
        when(badClient3.prompt().system("sys").user("usr").call().responseEntity(String.class)).thenThrow(restricted);
        when(goodClient.prompt().system("sys").user("usr").call().responseEntity(String.class))
                .thenReturn(new ResponseEntity<>(null, "OK"));

        String result = aiCoreClient.callAndValidate("sys", "usr", String.class, Function.identity());

        assertEquals("OK", result);
        verify(chatModelProvider).markFailed(node1, restricted);
        verify(chatModelProvider).markFailed(node2, restricted);
        verify(chatModelProvider).markFailed(node3, restricted);
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
                .responseEntity(String.class)).thenReturn(new ResponseEntity<>(null, rawResponse));

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
                .responseEntity(typeRef)).thenReturn(new ResponseEntity<>(null, rawList));

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
                .responseEntity(typeRef)).thenThrow(new RuntimeException("Network timeout"));

        Function<List<String>, Integer> validator = List::size;

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> aiCoreClient.callAndValidate(systemPrompt, userPrompt, typeRef, validator)
        );

        assertEquals("Network timeout", exception.getMessage());
    }

    @Test
    @DisplayName("callAndValidate with provider chain should use primary gemini provider when healthy")
    void testCallAndValidate_WithProviderChain_UsesGeminiFirst() {
        ChatModelProvider geminiProvider = mock(ChatModelProvider.class);
        ChatModelProvider groqProvider = mock(ChatModelProvider.class);
        ChatClient geminiClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);

        when(geminiProvider.size()).thenReturn(1);
        when(geminiProvider.current()).thenReturn(new ChatModelNode(geminiClient, "gemini-3.5-flash-lite", "AIza...1111"));
        when(geminiClient.prompt().system("sys").user("usr").call().responseEntity(String.class))
                .thenReturn(new ResponseEntity<>(null, "gemini-ok"));

        ChatModelProviderRegistry registry = new ChatModelProviderRegistry(
                Map.of("groq", groqProvider, "gemini", geminiProvider), "groq");
        AiCoreClient multiClient = new AiCoreClient(groqProvider, registry);

        String result = multiClient.callAndValidate(
                List.of("gemini", "groq"), "sys", "usr", String.class, Function.identity());

        assertEquals("gemini-ok", result);
        verifyNoInteractions(groqProvider);
    }

    @Test
    @DisplayName("callAndValidate with provider chain should fall back to groq when gemini exhausts all attempts")
    void testCallAndValidate_WithProviderChain_FallsBackToGroqOnGeminiFailure() {
        ChatModelProvider geminiProvider = mock(ChatModelProvider.class);
        ChatModelProvider groqProvider = mock(ChatModelProvider.class);
        ChatClient geminiClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        ChatClient groqClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);

        ChatModelNode geminiNode = new ChatModelNode(geminiClient, "gemini-3.5-flash-lite", "AIza...1111");
        ChatModelNode groqNode = new ChatModelNode(groqClient, "qwen/qwen3.8-27b", "gsk_...1111");

        when(geminiProvider.size()).thenReturn(2);
        when(geminiProvider.current()).thenReturn(geminiNode);
        when(geminiClient.prompt().system("sys").user("usr").call().responseEntity(String.class))
                .thenThrow(new RuntimeException("429 Too Many Requests"));

        when(groqProvider.size()).thenReturn(1);
        when(groqProvider.current()).thenReturn(groqNode);
        when(groqClient.prompt().system("sys").user("usr").call().responseEntity(String.class))
                .thenReturn(new ResponseEntity<>(null, "groq-fallback-ok"));

        ChatModelProviderRegistry registry = new ChatModelProviderRegistry(
                Map.of("groq", groqProvider, "gemini", geminiProvider), "groq");
        AiCoreClient multiClient = new AiCoreClient(groqProvider, registry);

        String result = multiClient.callAndValidate(
                List.of("gemini", "groq"), "sys", "usr", String.class, Function.identity());

        assertEquals("groq-fallback-ok", result);
        verify(geminiProvider, times(1)).markFailed(eq(geminiNode), any(RuntimeException.class));
        verify(groqProvider, times(1)).current();
    }

    @Test
    @DisplayName("callAndValidate with provider chain should rethrow last exception when all providers fail")
    void testCallAndValidate_WithProviderChain_RethrowsWhenAllProvidersFail() {
        ChatModelProvider geminiProvider = mock(ChatModelProvider.class);
        ChatModelProvider groqProvider = mock(ChatModelProvider.class);
        ChatClient geminiClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        ChatClient groqClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);

        when(geminiProvider.size()).thenReturn(1);
        when(geminiProvider.healthyCount()).thenReturn(0);
        when(geminiProvider.current()).thenReturn(new ChatModelNode(geminiClient, "gemini-3.5-flash-lite", "AIza...1111"));
        when(geminiClient.prompt().system("sys").user("usr").call().responseEntity(String.class))
                .thenThrow(new RuntimeException("Gemini 429"));

        when(groqProvider.size()).thenReturn(1);
        when(groqProvider.current()).thenReturn(new ChatModelNode(groqClient, "qwen/qwen3.8-27b", "gsk_...1111"));
        when(groqClient.prompt().system("sys").user("usr").call().responseEntity(String.class))
                .thenThrow(new RuntimeException("Groq 503"));

        ChatModelProviderRegistry registry = new ChatModelProviderRegistry(
                Map.of("groq", groqProvider, "gemini", geminiProvider), "groq");
        AiCoreClient multiClient = new AiCoreClient(groqProvider, registry);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> multiClient.callAndValidate(
                List.of("gemini", "groq"), "sys", "usr", String.class, Function.identity()));
        assertEquals("Groq 503", ex.getMessage());
    }

    @Test
    @DisplayName("callAndValidate with ParameterizedTypeReference and provider chain should fall back cleanly")
    void testCallAndValidate_WithParameterizedTypeReference_ProviderChain() {
        ChatModelProvider geminiProvider = mock(ChatModelProvider.class);
        ChatModelProvider groqProvider = mock(ChatModelProvider.class);
        ChatClient geminiClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        ChatClient groqClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        ParameterizedTypeReference<List<String>> typeRef = new ParameterizedTypeReference<>() {};

        when(geminiProvider.size()).thenReturn(1);
        when(geminiProvider.healthyCount()).thenReturn(0);
        when(geminiProvider.current()).thenReturn(new ChatModelNode(geminiClient, "gemini-3.5-flash-lite", "AIza...1111"));
        when(geminiClient.prompt().system("sys").user("usr").call().responseEntity(typeRef))
                .thenThrow(new RuntimeException("Gemini unavailable"));

        when(groqProvider.size()).thenReturn(1);
        when(groqProvider.current()).thenReturn(new ChatModelNode(groqClient, "qwen/qwen3.8-27b", "gsk_...1111"));
        when(groqClient.prompt().system("sys").user("usr").call().responseEntity(typeRef))
                .thenReturn(new ResponseEntity<>(null, List.of("ok")));

        ChatModelProviderRegistry registry = new ChatModelProviderRegistry(
                Map.of("groq", groqProvider, "gemini", geminiProvider), "groq");
        AiCoreClient multiClient = new AiCoreClient(groqProvider, registry);

        Integer size = multiClient.callAndValidate(
                List.of("gemini", "groq"), "sys", "usr", typeRef, List::size);
        assertEquals(1, size);
    }

    @Test
    @DisplayName("callAndValidate with provider chain should skip unconfigured gemini provider and use groq")
    void testCallAndValidate_WithProviderChain_SkipsUnconfiguredProvider() {
        ChatModelProvider emptyGeminiProvider = mock(ChatModelProvider.class);
        ChatModelProvider groqProvider = mock(ChatModelProvider.class);
        ChatClient groqClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);

        when(emptyGeminiProvider.size()).thenReturn(0);
        when(groqProvider.size()).thenReturn(1);
        when(groqProvider.current()).thenReturn(new ChatModelNode(groqClient, "qwen/qwen3.8-27b", "gsk_...1111"));
        when(groqClient.prompt().system("sys").user("usr").call().responseEntity(String.class))
                .thenReturn(new ResponseEntity<>(null, "groq-ok"));

        ChatModelProviderRegistry registry = new ChatModelProviderRegistry(
                Map.of("groq", groqProvider, "gemini", emptyGeminiProvider), "groq");
        AiCoreClient multiClient = new AiCoreClient(groqProvider, registry);

        String result = multiClient.callAndValidate(
                List.of("gemini", "groq"), "sys", "usr", String.class, Function.identity());

        assertEquals("groq-ok", result);
        verify(emptyGeminiProvider, never()).current();
    }

    @Test
    @DisplayName("4-arg callAndValidate should auto-fallback across providers when registry has multiple providers")
    void testCallAndValidate_DefaultChainFallsBackAcrossProviders() {
        ChatModelProvider geminiVocabProvider = mock(ChatModelProvider.class);
        ChatModelProvider groqProvider = mock(ChatModelProvider.class);
        ChatClient geminiClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        ChatClient groqClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);

        ChatModelNode geminiNode = new ChatModelNode(geminiClient, "gemini-3.1-flash-lite", "AIza...1111");
        ChatModelNode groqNode = new ChatModelNode(groqClient, "openai/gpt-oss-120b", "gsk_...1111");

        when(geminiVocabProvider.size()).thenReturn(1);
        when(geminiVocabProvider.healthyCount()).thenReturn(0);
        when(geminiVocabProvider.current()).thenReturn(geminiNode);
        when(geminiClient.prompt().system("sys").user("usr").call().responseEntity(String.class))
                .thenThrow(new RuntimeException("429 rate limit"));

        when(groqProvider.size()).thenReturn(1);
        when(groqProvider.current()).thenReturn(groqNode);
        when(groqClient.prompt().system("sys").user("usr").call().responseEntity(String.class))
                .thenReturn(new ResponseEntity<>(null, "groq-auto-fallback-ok"));

        ChatModelProviderRegistry registry = new ChatModelProviderRegistry(
                Map.of("gemini-vocab", geminiVocabProvider, "groq", groqProvider), "gemini-vocab");
        AiCoreClient multiClient = new AiCoreClient(geminiVocabProvider, registry);

        String result = multiClient.callAndValidate("sys", "usr", String.class, Function.identity());
        assertEquals("groq-auto-fallback-ok", result);
    }

    @Test
    @DisplayName("Should cap attempts on primary provider to 2 when a fallback provider is available")
    void testCallAndValidate_CapsAttemptsBeforeFallback() {
        ChatModelProvider geminiProvider = mock(ChatModelProvider.class);
        ChatModelProvider groqProvider = mock(ChatModelProvider.class);
        ChatClient geminiClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        ChatClient groqClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);

        ChatModelNode geminiNode = new ChatModelNode(geminiClient, "gemini-3.5-flash-lite", "AIza...1111");
        ChatModelNode groqNode = new ChatModelNode(groqClient, "openai/gpt-oss-120b", "gsk_...1111");

        when(geminiProvider.size()).thenReturn(6);
        when(geminiProvider.healthyCount()).thenReturn(4);
        when(geminiProvider.current()).thenReturn(geminiNode);
        when(geminiClient.prompt().system("sys").user("usr").call().responseEntity(String.class))
                .thenThrow(new RuntimeException("503: null"));

        when(groqProvider.size()).thenReturn(1);
        when(groqProvider.current()).thenReturn(groqNode);
        when(groqClient.prompt().system("sys").user("usr").call().responseEntity(String.class))
                .thenReturn(new ResponseEntity<>(null, "groq-fast-fallback"));

        ChatModelProviderRegistry registry = new ChatModelProviderRegistry(
                Map.of("gemini", geminiProvider, "groq", groqProvider), "gemini");
        AiCoreClient multiClient = new AiCoreClient(geminiProvider, registry);

        String result = multiClient.callAndValidate(
                List.of("gemini", "groq"), "sys", "usr", String.class, Function.identity());

        assertEquals("groq-fast-fallback", result);
        verify(geminiProvider, times(2)).current();
    }
}
