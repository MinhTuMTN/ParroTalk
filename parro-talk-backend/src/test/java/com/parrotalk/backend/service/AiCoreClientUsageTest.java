package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.parrotalk.backend.ai.AiUsageContext;
import com.parrotalk.backend.ai.AiUsageRecorder;
import com.parrotalk.backend.config.ChatModelProvider;
import com.parrotalk.backend.dto.ChatModelNode;
import com.parrotalk.backend.entity.User;

class AiCoreClientUsageTest {

    private ChatModelProvider provider;
    private ChatClient chatClient;
    private AiUsageRecorder recorder;
    private AiCoreClient client;

    @BeforeEach
    void setUp() {
        provider = mock(ChatModelProvider.class);
        chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        recorder = mock(AiUsageRecorder.class);
        client = new AiCoreClient(provider, null, recorder);
        when(provider.current()).thenReturn(new ChatModelNode(chatClient, "m-1", "k***"));
        when(provider.size()).thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        AiUsageContext.clear();
    }

    private ChatResponse response(Integer in, Integer out) {
        ChatResponseMetadata md = in == null ? ChatResponseMetadata.builder().build()
                : ChatResponseMetadata.builder().usage(new DefaultUsage(in, out)).build();
        return new ChatResponse(java.util.List.of(new Generation(new AssistantMessage("x"))), md);
    }

    private void stubCall(ChatResponse r) {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().responseEntity(String.class))
                .thenReturn(new ResponseEntity<>(r, "ok"));
    }

    @Test
    void recordsTokensModelProviderAndFeatureUnknown() {
        stubCall(response(10, 5));
        assertEquals(2, client.callAndValidate("s", "u", String.class, String::length));
        verify(recorder).record(isNull(), eq("default"), eq("m-1"), eq("UNKNOWN"), eq(10), eq(5), eq("OK"));
    }

    @Test
    void recordsEvenWhenValidatorFails() {
        stubCall(response(10, 5));
        assertThrows(RuntimeException.class, () -> client.callAndValidate("s", "u", String.class, x -> {
            throw new IllegalStateException("bad");
        }));
        verify(recorder, atLeastOnce()).record(any(), any(), any(), any(), eq(10), eq(5), any());
    }

    @Test
    void noRecordWhenCallThrows() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().responseEntity(String.class))
                .thenThrow(new RuntimeException("boom"));
        assertThrows(RuntimeException.class, () -> client.callAndValidate("s", "u", String.class, String::length));
        verifyNoInteractions(recorder);
    }

    @Test
    void recorderThrowingDoesNotBreakResult() {
        stubCall(response(1, 1));
        doThrow(new RuntimeException("x")).when(recorder).record(any(), any(), any(), any(), anyInt(), anyInt(), any());
        assertEquals(2, client.callAndValidate("s", "u", String.class, String::length));
    }

    @Test
    void nullOrZeroUsageSkipped() {
        stubCall(response(null, null));
        client.callAndValidate("s", "u", String.class, String::length);
        stubCall(response(0, 0));
        client.callAndValidate("s", "u", String.class, String::length);
        verifyNoInteractions(recorder);
    }

    @Test
    void userAndFeatureFromContext() {
        stubCall(response(3, 4));
        UUID id = UUID.randomUUID();
        try (var s = AiUsageContext.open(id, "F")) {
            client.callAndValidate("s", "u", String.class, String::length);
        }
        verify(recorder).record(eq(id), any(), any(), eq("F"), eq(3), eq(4), any());
    }

    @Test
    void userFromSecurityContext() {
        stubCall(response(3, 4));
        User user = new User();
        UUID id = UUID.randomUUID();
        user.setId(id);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, java.util.List.of()));
        client.callAndValidate("s", "u", String.class, String::length);
        verify(recorder).record(eq(id), any(), any(), eq("UNKNOWN"), eq(3), eq(4), any());
    }

    @Test
    void anonymousPrincipalYieldsNullUser() {
        stubCall(response(3, 4));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, java.util.List.of()));
        client.callAndValidate("s", "u", String.class, String::length);
        verify(recorder).record(isNull(), any(), any(), any(), eq(3), eq(4), any());
    }

    @Test
    void featureOnlyScopeFallsBackToSecurityContextUser() {
        stubCall(response(3, 4));
        User user = new User();
        UUID id = UUID.randomUUID();
        user.setId(id);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, java.util.List.of()));
        try (var s = AiUsageContext.open(null, "F")) {
            client.callAndValidate("s", "u", String.class, String::length);
        }
        verify(recorder).record(eq(id), any(), any(), eq("F"), eq(3), eq(4), any());
    }

    @Test
    void withFeatureKeepsOuterUserAndRestores() {
        UUID id = UUID.randomUUID();
        try (var s = AiUsageContext.open(id, "OUTER")) {
            String f = AiUsageContext.withFeature("INNER", () -> {
                assertEquals(id, AiUsageContext.current().userId());
                return AiUsageContext.current().feature();
            });
            assertEquals("INNER", f);
            assertEquals("OUTER", AiUsageContext.current().feature());
        }
        assertNull(AiUsageContext.current());
        assertThrows(IllegalStateException.class, () -> AiUsageContext.withFeature("X", () -> {
            throw new IllegalStateException();
        }));
        assertNull(AiUsageContext.current());
    }
}
