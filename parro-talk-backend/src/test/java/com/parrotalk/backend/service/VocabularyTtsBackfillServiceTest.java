package com.parrotalk.backend.service;

import com.parrotalk.backend.config.RabbitMQConfig;
import com.parrotalk.backend.entity.TtsAudioCache;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularyPronunciation;
import com.parrotalk.backend.repository.TtsAudioCacheRepository;
import com.parrotalk.backend.repository.VocabularyPronunciationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VocabularyTtsBackfillServiceTest {

    private VocabularyPronunciationRepository pronunciationRepository;
    private WritingTtsService writingTtsService;
    private TtsAudioCacheRepository ttsAudioCacheRepository;
    private RabbitTemplate rabbitTemplate;
    private VocabularyTtsBackfillService service;

    @BeforeEach
    void setUp() {
        pronunciationRepository = mock(VocabularyPronunciationRepository.class);
        writingTtsService = new WritingTtsService("uploads/tts", "python", 8L, 2);
        ttsAudioCacheRepository = mock(TtsAudioCacheRepository.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        service = new VocabularyTtsBackfillService(
                pronunciationRepository, writingTtsService, ttsAudioCacheRepository);
        service.setRabbitTemplate(rabbitTemplate);
    }

    @Test
    @DisplayName("triggerBackfill updates cached items immediately, enqueues uncached items, and onTtsResult updates DB")
    void triggerBackfill_UpdatesFromCacheAndEnqueuesUncached() {
        UUID cachedPronId = UUID.randomUUID();
        UUID uncachedPronId = UUID.randomUUID();
        Vocabulary vocab = Vocabulary.builder().word("social").normalizedWord("social").build();

        VocabularyPronunciation cachedPron = VocabularyPronunciation.builder()
                .id(cachedPronId)
                .vocabulary(vocab)
                .accent("US")
                .audioUrl("https://dictionary.cambridge.org/media/english/us_pron/s/soc/socia/social.mp3")
                .build();
        VocabularyPronunciation uncachedPron = VocabularyPronunciation.builder()
                .id(uncachedPronId)
                .vocabulary(vocab)
                .accent("UK")
                .audioUrl("https://dictionary.cambridge.org/media/english/uk_pron/s/soc/socia/social.mp3")
                .build();

        String usCacheKey = writingTtsService.computeCacheKey("social", "US", WritingTtsService.VOICE_US);
        String ukCacheKey = writingTtsService.computeCacheKey("social", "UK", WritingTtsService.VOICE_UK);
        String usCloudinaryUrl = "https://res.cloudinary.com/demo/video/upload/social_us.mp3";
        String ukCloudinaryUrl = "https://res.cloudinary.com/demo/video/upload/social_uk.mp3";

        when(pronunciationRepository.findPronunciationsNeedingTts(any(Pageable.class)))
                .thenReturn(List.of(cachedPron, uncachedPron));
        when(ttsAudioCacheRepository.findAllByCacheKeyIn(anyCollection()))
                .thenReturn(List.of(TtsAudioCache.builder()
                        .cacheKey(usCacheKey)
                        .cloudinaryUrl(usCloudinaryUrl)
                        .accent("US")
                        .voice(WritingTtsService.VOICE_US)
                        .build()));
        when(pronunciationRepository.updateAudioUrlById(cachedPronId, usCloudinaryUrl)).thenReturn(1);
        when(pronunciationRepository.updateAudioUrlById(uncachedPronId, ukCloudinaryUrl)).thenReturn(1);
        when(pronunciationRepository.countPronunciationsNeedingTts()).thenReturn(1L);

        VocabularyTtsBackfillService.BackfillTriggerResultDto result = service.triggerBackfill(25, 100);

        assertEquals(2, result.scanned());
        assertEquals(1, result.updatedFromCache());
        assertEquals(1, result.enqueuedToRabbitMq());
        assertEquals(1L, result.remainingNeedingTts());

        verify(pronunciationRepository).updateAudioUrlById(cachedPronId, usCloudinaryUrl);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE),
                eq(RabbitMQConfig.TTS_SYNTHESIS_QUEUE),
                any(Map.class)
        );

        service.onTtsResult(ukCacheKey, ukCloudinaryUrl);
        verify(pronunciationRepository).updateAudioUrlById(uncachedPronId, ukCloudinaryUrl);
    }
}
