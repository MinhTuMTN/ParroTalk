package com.parrotalk.backend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.config.ChatModelProvider;
import com.parrotalk.backend.dto.ChatModelNode;
import com.parrotalk.backend.dto.LlmTranslateRequestItem;
import com.parrotalk.backend.dto.LlmTranslateResponseItem;
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

    private static final String PROVIDER = "groq";
    private static final int DEFAULT_BATCH_SIZE = 25;
    private static final int MAX_RETRIES = 3;

    private static final String SYSTEM_PROMPT = """
                You are an English-to-Vietnamese translator for a listening app.
                Rules: translate each segment independently and literally; keep names,
                numbers, times, places unchanged; do not merge context between segments;
                do not add/remove meaning. Keep tone simple for learners.
                Output ONLY a JSON array, no markdown/comments and do not include any reasoning or code blocks.
                Each item: {"index":<int>,"text":"<translation>"}
            """;

    private static final ParameterizedTypeReference<List<LlmTranslateResponseItem>> LLM_RESPONSE_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final ChatModelProvider chatModelProvider;
    private final ObjectMapper objectMapper;
    private final TranscriptionSegmentRepository segmentRepository;
    private final SegmentTranslationRepository translationRepository;
    private final TransactionTemplate transactionTemplate;
    private final CacheManager cacheManager;

    public TranslationService(ChatModelProvider chatModelProvider, ObjectMapper objectMapper,
            TranscriptionSegmentRepository segmentRepository, SegmentTranslationRepository translationRepository,
            TransactionTemplate transactionTemplate, CacheManager cacheManager) {
        this.chatModelProvider = chatModelProvider;
        this.objectMapper = objectMapper;
        this.segmentRepository = segmentRepository;
        this.translationRepository = translationRepository;
        this.transactionTemplate = transactionTemplate;
        this.cacheManager = cacheManager;
    }

    /**
     * Translate only segments that do not have a stored translation yet.
     * 
     * @param lessonId Lesson ID
     * @param targetLanguage Target language
     */
    @Retryable(
            maxAttempts = MAX_RETRIES,
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            listeners = "translationRetryListener",
            retryFor = Exception.class)
    public void translateMissingSegments(UUID lessonId, String targetLanguage) {
        List<TranscriptionSegment> segments = segmentRepository.findByLessonIdOrderByDisplayOrderAsc(lessonId);
        if (segments.isEmpty()) {
            return;
        }

        Map<UUID, TranscriptionSegment> segmentsById =
                segments.stream().collect(Collectors.toMap(TranscriptionSegment::getId, segment -> segment));
        Set<UUID> translatedSegmentIds = translationRepository
                .findBySegmentIdInAndTargetLanguage(segmentsById.keySet(), targetLanguage)
                .stream()
                .map(translation -> translation.getSegment().getId())
                .collect(Collectors.toSet());

        List<TranscriptionSegment> missingSegments =
                segments.stream().filter(segment -> !translatedSegmentIds.contains(segment.getId())).toList();
        if (missingSegments.isEmpty()) {
            return;
        }

        log.info(
            "Translating {} missing segments for lesson {} to {}",
            missingSegments.size(),
            lessonId,
            targetLanguage);


        ChatModelNode chatModelNode = chatModelProvider.current();
        for (int start = 0; start < missingSegments.size(); start += DEFAULT_BATCH_SIZE) {
            int end = Math.min(start + DEFAULT_BATCH_SIZE, missingSegments.size());
            List<TranscriptionSegment> batch = missingSegments.subList(start, end);
            List<SegmentTranslationResponseItem> responseItems = requestTranslations(batch, lessonId, chatModelNode);
            validateResponse(batch, responseItems);
            saveBatch(batch, responseItems, targetLanguage, chatModelNode.modelName());
            evictLessonDetailCache(lessonId);
        }
    }

    /**
     * Translate one segment immediately and replace an existing stored translation if present.
     * 
     * @param segmentId The ID of the segment to translate.
     * @param targetLanguage The target language for translation.
     * @return The translated segment.
     */
    public SegmentTranslationResponse translateOneSegment(UUID segmentId, String targetLanguage) {
        TranscriptionSegment segment = segmentRepository
                .findById(segmentId)
                .orElseThrow(() -> new IllegalArgumentException("Segment not found"));

        ChatModelNode chatModelNode = chatModelProvider.current();
        String modelName = chatModelNode.modelName();

        try {
            List<SegmentTranslationResponseItem> responseItems =
                    requestTranslations(List.of(segment), segment.getLesson().getId(), chatModelNode);
            validateResponse(List.of(segment), responseItems);
            SegmentTranslationResponseItem responseItem = responseItems.get(0);

            transactionTemplate.executeWithoutResult(status -> {
                SegmentTranslation translation = translationRepository
                        .findBySegmentIdAndTargetLanguage(segmentId, targetLanguage)
                        .orElseGet(
                                () -> SegmentTranslation
                                        .builder()
                                        .segment(segment)
                                        .targetLanguage(targetLanguage)
                                        .provider(PROVIDER)
                                        .model(modelName)
                                        .build());
                translation.setTranslatedText(responseItem.translatedText().trim());
                translation.setProvider(PROVIDER);
                translation.setModel(modelName);
                translationRepository.save(translation);
            });

            evictLessonDetailCache(segment.getLesson().getId());
            return new SegmentTranslationResponse(targetLanguage, responseItem.translatedText().trim());
        } catch (Exception e) {
            log
                    .error(
                            "Failed to translate segment {} with provider {} and model {}",
                            segmentId,
                            PROVIDER,
                            modelName,
                            e);
            throw new IllegalStateException("Failed to translate segment", e);
        }
    }

    private List<SegmentTranslationResponseItem> requestTranslations(List<TranscriptionSegment> batch, UUID lessonId,
            ChatModelNode chatModelNode) {

        List<LlmTranslateRequestItem> requestItems = IntStream
                .range(0, batch.size())
                .mapToObj(i -> new LlmTranslateRequestItem(i, batch.get(i).getText()))
                .toList();

        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(requestItems);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize translation request items to JSON", e);
        }

        return chatModelNode
                .chatClient()
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(requestJson)
                .call()
                .entity(LLM_RESPONSE_LIST_TYPE)
                .stream()
                .map(r -> new SegmentTranslationResponseItem(batch.get(r.index()).getId(), r.text()))
                .toList();
    }

    void validateResponse(List<TranscriptionSegment> batch, List<SegmentTranslationResponseItem> responseItems) {
        if (responseItems == null) {
            throw new IllegalArgumentException("Translation response is null");
        }

        Set<UUID> expectedIds = batch.stream().map(TranscriptionSegment::getId).collect(Collectors.toSet());
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

    private void saveBatch(List<TranscriptionSegment> batch, List<SegmentTranslationResponseItem> responseItems,
            String targetLanguage, String modelName) {
        Map<UUID, TranscriptionSegment> segmentsById =
                batch.stream().collect(Collectors.toMap(segment -> segment.getId(), segment -> segment));
        Map<UUID, SegmentTranslationResponseItem> responseById = new HashMap<>();
        responseItems.forEach(item -> responseById.put(item.segmentId(), item));

        List<SegmentTranslation> translations = new ArrayList<>();
        for (TranscriptionSegment segment : batch) {
            SegmentTranslationResponseItem item = responseById.get(segment.getId());
            translations
                    .add(
                            SegmentTranslation
                                    .builder()
                                    .segment(segmentsById.get(segment.getId()))
                                    .targetLanguage(targetLanguage)
                                    .translatedText(item.translatedText().trim())
                                    .provider(PROVIDER)
                                    .model(modelName)
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
