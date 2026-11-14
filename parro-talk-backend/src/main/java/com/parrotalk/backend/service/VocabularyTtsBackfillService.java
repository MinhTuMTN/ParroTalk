package com.parrotalk.backend.service;

import com.parrotalk.backend.config.RabbitMQConfig;
import com.parrotalk.backend.entity.TtsAudioCache;
import com.parrotalk.backend.entity.VocabularyPronunciation;
import com.parrotalk.backend.repository.TtsAudioCacheRepository;
import com.parrotalk.backend.repository.VocabularyPronunciationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backfills blocked/missing {@link VocabularyPronunciation#getAudioUrl()} entries
 * using {@code tts_audio_cache} and the RabbitMQ Edge-TTS + Cloudinary worker.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyTtsBackfillService {

    private final VocabularyPronunciationRepository pronunciationRepository;
    private final WritingTtsService writingTtsService;
    private final TtsAudioCacheRepository ttsAudioCacheRepository;
    private RabbitTemplate rabbitTemplate;

    private final Map<String, Set<UUID>> pendingByCacheKey = new ConcurrentHashMap<>();

    @Autowired(required = false)
    void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public record BackfillStatusDto(long remainingNeedingTts, int pendingInMemory) {
    }

    public record BackfillTriggerResultDto(
            int scanned,
            int updatedFromCache,
            int enqueuedToRabbitMq,
            long remainingNeedingTts
    ) {
    }

    @Transactional(readOnly = true)
    public BackfillStatusDto getStatus() {
        return new BackfillStatusDto(
                pronunciationRepository.countPronunciationsNeedingTts(),
                pendingByCacheKey.size()
        );
    }

    @Transactional
    public BackfillTriggerResultDto triggerBackfill(int batchSize, int maxItems) {
        int safeBatchSize = Math.max(1, Math.min(batchSize, 100));
        int safeMaxItems = Math.max(1, Math.min(maxItems, 5000));

        List<VocabularyPronunciation> targets = pronunciationRepository.findPronunciationsNeedingTts(
                PageRequest.of(0, safeMaxItems)
        );
        if (targets.isEmpty()) {
            return new BackfillTriggerResultDto(0, 0, 0, 0);
        }

        List<CandidateItem> candidates = new ArrayList<>(targets.size());
        Set<String> allCacheKeys = new HashSet<>();
        for (VocabularyPronunciation p : targets) {
            if (p.getVocabulary() == null || p.getVocabulary().getWord() == null) {
                continue;
            }
            try {
                String cleanedWord = writingTtsService.cleanAndValidateText(p.getVocabulary().getWord());
                String normAccent = writingTtsService.normalizeAccent(p.getAccent());
                String voice = writingTtsService.resolveVoice(normAccent);
                String cacheKey = writingTtsService.computeCacheKey(cleanedWord, normAccent, voice);
                candidates.add(new CandidateItem(p.getId(), cacheKey, cleanedWord, normAccent, voice));
                allCacheKeys.add(cacheKey);
            } catch (Exception e) {
                log.debug("Skipping pronunciation {} during TTS backfill: {}", p.getId(), e.getMessage());
            }
        }

        Map<String, String> cachedUrlsByKey = new HashMap<>();
        if (!allCacheKeys.isEmpty()) {
            for (TtsAudioCache cached : ttsAudioCacheRepository.findAllByCacheKeyIn(allCacheKeys)) {
                if (cached.getCacheKey() != null && cached.getCloudinaryUrl() != null && !cached.getCloudinaryUrl().isBlank()) {
                    cachedUrlsByKey.put(cached.getCacheKey(), cached.getCloudinaryUrl());
                }
            }
        }

        int updatedFromCache = 0;
        Map<String, Map<String, Object>> uncachedByKey = new HashMap<>();
        for (CandidateItem item : candidates) {
            String cachedUrl = cachedUrlsByKey.get(item.cacheKey());
            if (cachedUrl != null) {
                updatedFromCache += pronunciationRepository.updateAudioUrlById(item.pronunciationId(), cachedUrl);
                continue;
            }
            pendingByCacheKey
                    .computeIfAbsent(item.cacheKey(), ignored -> ConcurrentHashMap.newKeySet())
                    .add(item.pronunciationId());
            uncachedByKey.putIfAbsent(item.cacheKey(), Map.of(
                    "cacheKey", item.cacheKey(),
                    "text", item.word(),
                    "accent", item.accent(),
                    "voice", item.voice()
            ));
        }

        int enqueuedCount = 0;
        if (!uncachedByKey.isEmpty() && rabbitTemplate != null) {
            List<Map<String, Object>> toPublish = new ArrayList<>(uncachedByKey.values());
            for (int i = 0; i < toPublish.size(); i += safeBatchSize) {
                List<Map<String, Object>> chunk = toPublish.subList(i, Math.min(i + safeBatchSize, toPublish.size()));
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE,
                        RabbitMQConfig.TTS_SYNTHESIS_QUEUE,
                        Map.of("items", chunk)
                );
                enqueuedCount += chunk.size();
            }
            log.info("Vocabulary TTS backfill: updatedFromCache={}, enqueuedClips={}", updatedFromCache, enqueuedCount);
        } else if (!uncachedByKey.isEmpty()) {
            log.warn("RabbitTemplate unavailable; {} uncached vocabulary TTS clips were not enqueued.", uncachedByKey.size());
        }

        long remaining = pronunciationRepository.countPronunciationsNeedingTts();
        return new BackfillTriggerResultDto(targets.size(), updatedFromCache, enqueuedCount, remaining);
    }

    /**
     * Updates any pending {@link VocabularyPronunciation} rows once RabbitMQ returns a Cloudinary URL.
     */
    @Transactional
    public void onTtsResult(String cacheKey, String cloudinaryUrl) {
        if (cacheKey == null || cacheKey.isBlank() || cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
            return;
        }
        Set<UUID> pronunciationIds = pendingByCacheKey.remove(cacheKey);
        if (pronunciationIds == null || pronunciationIds.isEmpty()) {
            return;
        }
        int updated = 0;
        for (UUID id : pronunciationIds) {
            updated += pronunciationRepository.updateAudioUrlById(id, cloudinaryUrl);
        }
        if (updated > 0) {
            log.debug("Updated {} vocabulary pronunciations for cacheKey={}", updated, cacheKey);
        }
    }

    private record CandidateItem(UUID pronunciationId, String cacheKey, String word, String accent, String voice) {
    }
}
