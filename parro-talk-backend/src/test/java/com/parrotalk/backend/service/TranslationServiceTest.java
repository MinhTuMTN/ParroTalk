package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.dto.SegmentTranslationResponseItem;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.repository.SegmentTranslationRepository;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;

class TranslationServiceTest {

    private TranslationService translationService;
    private UUID firstId;
    private UUID secondId;
    private List<TranscriptionSegment> batch;

    @BeforeEach
    void setUp() {
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        when(chatClientBuilder.build()).thenReturn(mock(ChatClient.class));

        translationService = new TranslationService(
                chatClientBuilder,
                new ObjectMapper(),
                mock(TranscriptionSegmentRepository.class),
                mock(SegmentTranslationRepository.class),
                mock(TransactionTemplate.class),
                mock(CacheManager.class),
                "llama-3.3-70b-versatile");

        firstId = UUID.randomUUID();
        secondId = UUID.randomUUID();
        batch = List.of(
                TranscriptionSegment.builder().id(firstId).text("Hello").build(),
                TranscriptionSegment.builder().id(secondId).text("Good morning").build());
    }

    @Test
    void validateResponseAcceptsMatchingItems() {
        List<SegmentTranslationResponseItem> response = List.of(
                new SegmentTranslationResponseItem(firstId, "Xin chao"),
                new SegmentTranslationResponseItem(secondId, "Chao buoi sang"));

        assertDoesNotThrow(() -> translationService.validateResponse(batch, response));
    }

    @Test
    void validateResponseRejectsMissingItems() {
        List<SegmentTranslationResponseItem> response = List.of(
                new SegmentTranslationResponseItem(firstId, "Xin chao"));

        assertThrows(IllegalArgumentException.class,
                () -> translationService.validateResponse(batch, response));
    }

    @Test
    void validateResponseRejectsUnknownIds() {
        List<SegmentTranslationResponseItem> response = List.of(
                new SegmentTranslationResponseItem(firstId, "Xin chao"),
                new SegmentTranslationResponseItem(UUID.randomUUID(), "Khong hop le"));

        assertThrows(IllegalArgumentException.class,
                () -> translationService.validateResponse(batch, response));
    }

    @Test
    void validateResponseRejectsDuplicateIds() {
        List<SegmentTranslationResponseItem> response = List.of(
                new SegmentTranslationResponseItem(firstId, "Xin chao"),
                new SegmentTranslationResponseItem(firstId, "Xin chao lan nua"));

        assertThrows(IllegalArgumentException.class,
                () -> translationService.validateResponse(batch, response));
    }

    @Test
    void validateResponseRejectsBlankTranslations() {
        List<SegmentTranslationResponseItem> response = List.of(
                new SegmentTranslationResponseItem(firstId, "Xin chao"),
                new SegmentTranslationResponseItem(secondId, " "));

        assertThrows(IllegalArgumentException.class,
                () -> translationService.validateResponse(batch, response));
    }
}
