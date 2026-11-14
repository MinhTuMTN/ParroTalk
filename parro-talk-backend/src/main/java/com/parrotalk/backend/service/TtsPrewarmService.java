package com.parrotalk.backend.service;

import com.parrotalk.backend.config.RabbitMQConfig;
import com.parrotalk.backend.dto.writing.WritingSessionResponseDto;
import com.parrotalk.backend.dto.writing.WritingSessionResponseDto.WritingCompleteSentenceDto;
import com.parrotalk.backend.dto.writing.WritingSessionResponseDto.WritingLexicalItemDto;
import com.parrotalk.backend.entity.TtsAudioCache;
import com.parrotalk.backend.repository.TtsAudioCacheRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asynchronously prewarms Edge-TTS disk cache entries after a Writing Assistant session
 * is created so word, phrase, example, and sentence audio play immediately on click.
 */
@Service
@Slf4j
public class TtsPrewarmService {

    private static final int CORE_THREADS = 2;
    private static final int MAX_THREADS = 2;
    private static final int QUEUE_CAPACITY = 200;
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);

    private final WritingTtsService writingTtsService;
    private final boolean enabled;
    private final int maxClips;
    private final ExecutorService executor;
    private final boolean ownsExecutor;
    private RabbitTemplate rabbitTemplate;
    private TtsAudioCacheRepository ttsAudioCacheRepository;
    private String prewarmMode = "rabbitmq";

    @Autowired(required = false)
    void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Autowired(required = false)
    void setTtsAudioCacheRepository(TtsAudioCacheRepository ttsAudioCacheRepository) {
        this.ttsAudioCacheRepository = ttsAudioCacheRepository;
    }

    @Value("${app.tts.prewarm.mode:rabbitmq}")
    void setPrewarmMode(String prewarmMode) {
        this.prewarmMode = prewarmMode != null ? prewarmMode : "rabbitmq";
    }

    @Autowired
    public TtsPrewarmService(
            WritingTtsService writingTtsService,
            @Value("${app.tts.prewarm.enabled:true}") boolean enabled,
            @Value("${app.tts.prewarm.max-clips:40}") int maxClips) {
        this(writingTtsService, enabled, maxClips, createDefaultExecutor(), true);
    }

    TtsPrewarmService(
            WritingTtsService writingTtsService,
            boolean enabled,
            int maxClips,
            ExecutorService executor) {
        this(writingTtsService, enabled, maxClips, executor, false);
    }

    private TtsPrewarmService(
            WritingTtsService writingTtsService,
            boolean enabled,
            int maxClips,
            ExecutorService executor,
            boolean ownsExecutor) {
        this.writingTtsService = writingTtsService;
        this.enabled = enabled;
        this.maxClips = Math.max(0, maxClips);
        this.executor = executor;
        this.ownsExecutor = ownsExecutor;
    }

    private static ExecutorService createDefaultExecutor() {
        return new ThreadPoolExecutor(
                CORE_THREADS,
                MAX_THREADS,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                runnable -> {
                    Thread thread = new Thread(runnable, "tts-prewarm-" + THREAD_COUNTER.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * Enqueues TTS synthesis tasks in strict priority order:
     * 1) VOCABULARY + EXPRESSION terms (US & UK)
     * 2) COLLOCATION terms (US & UK)
     * 3) Example sentences & COMPLETE_SENTENCE items (US & UK)
     * External dictionary audio URLs are skipped, duplicates are removed, and total clips are capped at {@code maxClips}.
     */
    public void prewarmSession(WritingSessionResponseDto session) {
        if (!enabled || maxClips <= 0 || session == null || writingTtsService == null) {
            return;
        }
        try {
            List<PrewarmClip> clips = collectPrioritizedClips(session);
            if (clips.isEmpty()) {
                return;
            }
            if ("rabbitmq".equalsIgnoreCase(prewarmMode) && rabbitTemplate != null) {
                publishBatchToRabbitMq(clips);
                return;
            }
            if (executor == null) {
                return;
            }
            for (PrewarmClip clip : clips) {
                try {
                    executor.execute(() -> synthesizeSafely(clip.text(), clip.accent()));
                } catch (RejectedExecutionException rejected) {
                    log.debug("TTS prewarm queue is full (capacity {}); skipping remaining {} clips",
                            QUEUE_CAPACITY, clips.size());
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("TTS prewarm scheduling skipped due to error: {}", e.getMessage());
        }
    }

    /**
     * Enqueues a single on-demand synthesis job to RabbitMQ when local fallback synthesis is disabled.
     */
    public void enqueueSingleSynthesis(String text, String accent, String requestedVoice) {
        if (!enabled || rabbitTemplate == null || writingTtsService == null) {
            return;
        }
        try {
            String cleaned = writingTtsService.cleanAndValidateText(text);
            String normAccent = writingTtsService.normalizeAccent(accent);
            String voice = writingTtsService.resolveVoice(normAccent, requestedVoice);
            String cacheKey = writingTtsService.computeCacheKey(cleaned, normAccent, voice);
            Map<String, Object> item = new HashMap<>();
            item.put("cacheKey", cacheKey);
            item.put("text", cleaned);
            item.put("accent", normAccent);
            item.put("voice", voice);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.TTS_SYNTHESIS_QUEUE,
                    Map.of("items", List.of(item))
            );
        } catch (Exception e) {
            log.debug("On-demand RabbitMQ TTS enqueue skipped: {}", e.getMessage());
        }
    }

    private void publishBatchToRabbitMq(List<PrewarmClip> clips) {
        List<Map<String, Object>> candidates = new ArrayList<>(clips.size());
        List<String> candidateKeys = new ArrayList<>(clips.size());
        for (PrewarmClip clip : clips) {
            String normAccent = writingTtsService.normalizeAccent(clip.accent());
            String voice = writingTtsService.resolveVoice(normAccent);
            String cacheKey = writingTtsService.computeCacheKey(clip.text(), normAccent, voice);
            Map<String, Object> payloadItem = new HashMap<>();
            payloadItem.put("cacheKey", cacheKey);
            payloadItem.put("text", clip.text());
            payloadItem.put("accent", normAccent);
            payloadItem.put("voice", voice);
            candidates.add(payloadItem);
            candidateKeys.add(cacheKey);
        }

        Set<String> alreadyCachedKeys = new HashSet<>();
        if (ttsAudioCacheRepository != null && !candidateKeys.isEmpty()) {
            for (TtsAudioCache cached : ttsAudioCacheRepository.findAllByCacheKeyIn(candidateKeys)) {
                if (cached.getCacheKey() != null) {
                    alreadyCachedKeys.add(cached.getCacheKey());
                }
            }
        }

        List<Map<String, Object>> uncachedItems = new ArrayList<>();
        for (Map<String, Object> item : candidates) {
            if (!alreadyCachedKeys.contains(String.valueOf(item.get("cacheKey")))) {
                uncachedItems.add(item);
            }
        }
        if (uncachedItems.isEmpty()) {
            return;
        }

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.TTS_SYNTHESIS_QUEUE,
                Map.of("items", uncachedItems)
        );
        log.debug("Published {} uncached TTS clips to RabbitMQ {}", uncachedItems.size(), RabbitMQConfig.TTS_SYNTHESIS_QUEUE);
    }

    List<PrewarmClip> collectPrioritizedClips(WritingSessionResponseDto session) {
        List<PrewarmClip> ordered = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // Priority 1: VOCABULARY + EXPRESSION terms
        addLexicalTermClips(session.getVocabulary(), ordered, seen);
        addLexicalTermClips(session.getExpressions(), ordered, seen);

        // Priority 2: COLLOCATION terms
        addLexicalTermClips(session.getCollocations(), ordered, seen);

        // Priority 3: Examples across lexical items + COMPLETE_SENTENCE items
        addLexicalExampleClips(session.getVocabulary(), ordered, seen);
        addLexicalExampleClips(session.getExpressions(), ordered, seen);
        addLexicalExampleClips(session.getCollocations(), ordered, seen);
        addCompleteSentenceClips(session.getCompleteSentences(), ordered, seen);

        return ordered;
    }

    private void addLexicalTermClips(
            List<WritingLexicalItemDto> items,
            List<PrewarmClip> ordered,
            Set<String> seen) {
        if (items == null) {
            return;
        }
        for (WritingLexicalItemDto item : items) {
            if (ordered.size() >= maxClips) {
                return;
            }
            if (item == null || item.getText() == null || item.getText().isBlank()) {
                continue;
            }
            if (!isExternalDictionaryUrl(item.getAudioUsUrl())) {
                addClipIfRoom(item.getText(), "US", ordered, seen);
            }
            if (!isExternalDictionaryUrl(item.getAudioUkUrl())) {
                addClipIfRoom(item.getText(), "UK", ordered, seen);
            }
        }
    }

    private void addLexicalExampleClips(
            List<WritingLexicalItemDto> items,
            List<PrewarmClip> ordered,
            Set<String> seen) {
        if (items == null) {
            return;
        }
        for (WritingLexicalItemDto item : items) {
            if (ordered.size() >= maxClips) {
                return;
            }
            if (item == null || item.getExampleEn() == null || item.getExampleEn().isBlank()) {
                continue;
            }
            if (!isExternalDictionaryUrl(item.getExampleAudioUsUrl())) {
                addClipIfRoom(item.getExampleEn(), "US", ordered, seen);
            }
            if (!isExternalDictionaryUrl(item.getExampleAudioUkUrl())) {
                addClipIfRoom(item.getExampleEn(), "UK", ordered, seen);
            }
        }
    }

    private void addCompleteSentenceClips(
            List<WritingCompleteSentenceDto> sentences,
            List<PrewarmClip> ordered,
            Set<String> seen) {
        if (sentences == null) {
            return;
        }
        for (WritingCompleteSentenceDto sentence : sentences) {
            if (ordered.size() >= maxClips) {
                return;
            }
            if (sentence == null || sentence.getSentenceEn() == null || sentence.getSentenceEn().isBlank()) {
                continue;
            }
            if (!isExternalDictionaryUrl(sentence.getAudioUsUrl())) {
                addClipIfRoom(sentence.getSentenceEn(), "US", ordered, seen);
            }
            if (!isExternalDictionaryUrl(sentence.getAudioUkUrl())) {
                addClipIfRoom(sentence.getSentenceEn(), "UK", ordered, seen);
            }
        }
    }

    private void addClipIfRoom(
            String rawText,
            String accent,
            List<PrewarmClip> ordered,
            Set<String> seen) {
        if (ordered.size() >= maxClips || rawText == null) {
            return;
        }
        String normalizedText = rawText.trim().replaceAll("\\s+", " ");
        if (normalizedText.isEmpty()) {
            return;
        }
        String dedupKey = accent + "\u0000" + normalizedText;
        if (seen.add(dedupKey)) {
            ordered.add(new PrewarmClip(normalizedText, accent));
        }
    }

    private boolean isExternalDictionaryUrl(String url) {
        return url != null && !url.isBlank() && !url.startsWith("/api/writing/tts");
    }

    private void synthesizeSafely(String text, String accent) {
        try {
            writingTtsService.synthesize(text, accent);
        } catch (Exception e) {
            log.debug("TTS prewarm failed for accent={} text='{}': {}", accent, text, e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        if (ownsExecutor && executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    record PrewarmClip(String text, String accent) {
    }
}
