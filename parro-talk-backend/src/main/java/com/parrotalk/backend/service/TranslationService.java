package com.parrotalk.backend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.dto.SegmentTranslationRequestItem;
import com.parrotalk.backend.dto.SegmentTranslationResponse;
import com.parrotalk.backend.dto.SegmentTranslationResponseItem;
import com.parrotalk.backend.entity.SegmentTranslation;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.repository.SegmentTranslationRepository;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Translates lesson segments and persists the results for later reads.
 */
@Service
@Slf4j
public class TranslationService {

    public static final String DEFAULT_TARGET_LANGUAGE = "vi";

    private static final String PROVIDER = "groq";
    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final ParameterizedTypeReference<List<SegmentTranslationResponseItem>> RESPONSE_TYPE = new ParameterizedTypeReference<>() {
    };

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final TranscriptionSegmentRepository segmentRepository;
    private final SegmentTranslationRepository translationRepository;
    private final TransactionTemplate transactionTemplate;
    private final CacheManager cacheManager;
    private final String model;

    public TranslationService(
            ChatClient.Builder chatClientBuilder,
            ObjectMapper objectMapper,
            TranscriptionSegmentRepository segmentRepository,
            SegmentTranslationRepository translationRepository,
            TransactionTemplate transactionTemplate,
            CacheManager cacheManager,
            @Value("${spring.ai.openai.chat.options.model:llama-3.3-70b-versatile}") String model) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.segmentRepository = segmentRepository;
        this.translationRepository = translationRepository;
        this.transactionTemplate = transactionTemplate;
        this.cacheManager = cacheManager;
        this.model = model;
    }

    /**
     * Translate all missing lesson segments for a target language in the
     * background.
     */
    @Async
    public void translateLessonSegmentsAsync(UUID lessonId, String targetLanguage) {
        try {
            translateMissingSegments(lessonId, targetLanguage);
        } catch (Exception e) {
            log.error("Unexpected translation failure for lesson {} and language {}", lessonId, targetLanguage, e);
        }
    }

    /**
     * Translate only segments that do not have a stored translation yet.
     */
    public void translateMissingSegments(UUID lessonId, String targetLanguage) {
        List<TranscriptionSegment> segments = segmentRepository.findByLessonIdOrderByDisplayOrderAsc(lessonId);
        if (segments.isEmpty()) {
            return;
        }

        Map<UUID, TranscriptionSegment> segmentsById = segments.stream()
                .collect(Collectors.toMap(TranscriptionSegment::getId, segment -> segment));
        Set<UUID> translatedSegmentIds = translationRepository
                .findBySegmentIdInAndTargetLanguage(segmentsById.keySet(), targetLanguage)
                .stream()
                .map(translation -> translation.getSegment().getId())
                .collect(Collectors.toSet());

        List<TranscriptionSegment> missingSegments = segments.stream()
                .filter(segment -> !translatedSegmentIds.contains(segment.getId()))
                .toList();
        if (missingSegments.isEmpty()) {
            return;
        }

        log.info("Translating {} missing segments for lesson {} to {}", missingSegments.size(), lessonId,
                targetLanguage);

        for (int start = 0; start < missingSegments.size(); start += DEFAULT_BATCH_SIZE) {
            int end = Math.min(start + DEFAULT_BATCH_SIZE, missingSegments.size());
            List<TranscriptionSegment> batch = missingSegments.subList(start, end);
            try {
                List<SegmentTranslationResponseItem> responseItems = requestTranslations(batch);
                validateResponse(batch, responseItems);
                saveBatch(batch, responseItems, targetLanguage);
                evictLessonDetailCache(lessonId);
            } catch (Exception e) {
                log.error("Failed to translate lesson {} batch {}-{} with provider {} and model {}",
                        lessonId, start, end - 1, PROVIDER, model, e);
            }
        }
    }

    /**
     * Translate one segment immediately and replace an existing stored translation
     * if present.
     */
    public SegmentTranslationResponse translateSegment(UUID segmentId, String targetLanguage) {
        TranscriptionSegment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new IllegalArgumentException("Segment not found"));

        try {
            List<SegmentTranslationResponseItem> responseItems = requestTranslations(List.of(segment));
            validateResponse(List.of(segment), responseItems);
            SegmentTranslationResponseItem responseItem = responseItems.get(0);

            transactionTemplate.executeWithoutResult(status -> {
                SegmentTranslation translation = translationRepository
                        .findBySegmentIdAndTargetLanguage(segmentId, targetLanguage)
                        .orElseGet(() -> SegmentTranslation.builder()
                                .segment(segment)
                                .targetLanguage(targetLanguage)
                                .provider(PROVIDER)
                                .model(model)
                                .build());
                translation.setTranslatedText(responseItem.translatedText().trim());
                translation.setProvider(PROVIDER);
                translation.setModel(model);
                translationRepository.save(translation);
            });

            evictLessonDetailCache(segment.getLesson().getId());
            return new SegmentTranslationResponse(targetLanguage, responseItem.translatedText().trim());
        } catch (Exception e) {
            log.error("Failed to translate segment {} with provider {} and model {}", segmentId, PROVIDER, model, e);
            throw new IllegalStateException("Failed to translate segment", e);
        }
    }

    private List<SegmentTranslationResponseItem> requestTranslations(List<TranscriptionSegment> batch)
            throws JsonProcessingException {

        List<SegmentTranslationRequestItem> requestItems = batch.stream()
                .map(segment -> new SegmentTranslationRequestItem(segment.getId(), segment.getText()))
                .toList();

        String requestJson = objectMapper.writeValueAsString(requestItems);

        return chatClient.prompt()
                .system("""
                        You are a professional English-to-Vietnamese translation engine
                        for a listening-learning application.

                        Your task is to translate each English segment into clear,
                        accurate, and natural Vietnamese.

                        Translation rules:
                        - Translate each segment independently.
                        - Preserve the original meaning exactly.
                        - Do not paraphrase or reinterpret the content.
                        - Do not add or remove information.
                        - Preserve names, numbers, times, places, and special terms exactly as written.
                        - Keep the tone simple and easy for Vietnamese learners to understand.
                        - Prefer accuracy over creativity.
                        - If a word has a direct meaning, translate it literally unless the sentence becomes unnatural.
                        - Do not use context from other segments to change the meaning of a segment.
                        - Keep each segmentId unchanged.

                        Response rules:
                        - Return ONLY valid JSON.
                        - Do not include markdown, explanations, comments, or extra text.
                        - The response must be a JSON array.
                        - Each object must contain:
                          - segmentId
                          - translatedText
                        """)
                .user("""
                        Translate the following JSON array of English segments into Vietnamese.

                        Input JSON:
                        %s
                        """.formatted(requestJson))
                .call()
                .entity(RESPONSE_TYPE);
    }

    void validateResponse(
            List<TranscriptionSegment> batch,
            List<SegmentTranslationResponseItem> responseItems) {
        if (responseItems == null) {
            throw new IllegalArgumentException("Translation response is null");
        }

        Set<UUID> expectedIds = batch.stream()
                .map(TranscriptionSegment::getId)
                .collect(Collectors.toSet());
        Set<UUID> actualIds = new HashSet<>();

        for (SegmentTranslationResponseItem item : responseItems) {
            if (item == null || item.segmentId() == null || item.translatedText() == null
                    || item.translatedText().isBlank()) {
                throw new IllegalArgumentException("Translation response contains an invalid item");
            }
            if (!expectedIds.contains(item.segmentId())) {
                throw new IllegalArgumentException("Translation response contains an unknown segment id");
            }
            if (!actualIds.add(item.segmentId())) {
                throw new IllegalArgumentException("Translation response contains duplicate segment ids");
            }
        }

        if (!actualIds.equals(expectedIds)) {
            throw new IllegalArgumentException("Translation response does not match requested segment ids");
        }
    }

    private void saveBatch(
            List<TranscriptionSegment> batch,
            List<SegmentTranslationResponseItem> responseItems,
            String targetLanguage) {
        Map<UUID, TranscriptionSegment> segmentsById = batch.stream()
                .collect(Collectors.toMap(TranscriptionSegment::getId, segment -> segment));
        Map<UUID, SegmentTranslationResponseItem> responseById = new HashMap<>();
        responseItems.forEach(item -> responseById.put(item.segmentId(), item));

        List<SegmentTranslation> translations = new ArrayList<>();
        for (TranscriptionSegment segment : batch) {
            SegmentTranslationResponseItem item = responseById.get(segment.getId());
            translations.add(SegmentTranslation.builder()
                    .segment(segmentsById.get(segment.getId()))
                    .targetLanguage(targetLanguage)
                    .translatedText(item.translatedText().trim())
                    .provider(PROVIDER)
                    .model(model)
                    .build());
        }

        transactionTemplate.executeWithoutResult(status -> translationRepository.saveAll(translations));
    }

    private void evictLessonDetailCache(UUID lessonId) {
        Cache cache = cacheManager.getCache("lessonDetailCache");
        if (cache != null) {
            cache.evict(lessonId);
        }
    }
}
