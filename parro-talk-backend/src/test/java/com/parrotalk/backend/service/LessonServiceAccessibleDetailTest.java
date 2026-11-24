package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.config.AppSetting;
import com.parrotalk.backend.constant.CacheNames;
import com.parrotalk.backend.constant.DictationMode;
import com.parrotalk.backend.constant.LessonStatus;
import com.parrotalk.backend.constant.LessonVisibilityStatus;
import com.parrotalk.backend.constant.TranslationLanguage;
import com.parrotalk.backend.constant.TranslationStatus;
import com.parrotalk.backend.dto.LessonResponse;
import com.parrotalk.backend.dto.dictation.BlankSlotDto;
import com.parrotalk.backend.dto.dictation.SegmentBlanksPayload;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.mapper.CategoryMapperImpl;
import com.parrotalk.backend.mapper.LessonMapperImpl;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.repository.SegmentTranslationRepository;
import com.parrotalk.backend.repository.SegmentTranslationRepository.SegmentTranslationProjection;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;
import com.parrotalk.backend.service.dictation.DictationBlankGenerator;
import com.parrotalk.backend.util.SegmentBlanksCodec;

class LessonServiceAccessibleDetailTest {

    private AnnotationConfigApplicationContext context;
    private LessonRepository lessonRepository;
    private TranscriptionSegmentRepository transcriptionSegmentRepository;
    private SegmentTranslationRepository segmentTranslationRepository;
    private DictationBlankGenerator blankGenerator;
    private SegmentBlanksCodec segmentBlanksCodec;
    private CacheManager cacheManager;
    private LessonService lessonService;

    @Configuration
    @EnableCaching
    static class TestCacheConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CacheNames.LESSON_DETAIL_CACHE);
        }

        @Bean
        LessonRepository lessonRepository() {
            return mock(LessonRepository.class);
        }

        @Bean
        TranscriptionSegmentRepository transcriptionSegmentRepository() {
            return mock(TranscriptionSegmentRepository.class);
        }

        @Bean
        SegmentTranslationRepository segmentTranslationRepository() {
            return mock(SegmentTranslationRepository.class);
        }

        @Bean
        DictationBlankGenerator blankGenerator() {
            return mock(DictationBlankGenerator.class);
        }

        @Bean
        SegmentBlanksCodec segmentBlanksCodec() {
            return new SegmentBlanksCodec(new ObjectMapper());
        }

        @Bean
        LessonService lessonService(
                LessonRepository lessonRepository,
                TranscriptionSegmentRepository transcriptionSegmentRepository,
                SegmentTranslationRepository segmentTranslationRepository,
                DictationBlankGenerator blankGenerator,
                SegmentBlanksCodec segmentBlanksCodec) {
            return new LessonService(
                    lessonRepository,
                    mock(SseService.class),
                    new LessonMapperImpl(),
                    new CategoryMapperImpl(),
                    transcriptionSegmentRepository,
                    segmentTranslationRepository,
                    mock(CategoryRepository.class),
                    mock(TranslationService.class),
                    blankGenerator,
                    segmentBlanksCodec,
                    mock(AppSetting.class));
        }
    }

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestCacheConfig.class);
        lessonRepository = context.getBean(LessonRepository.class);
        transcriptionSegmentRepository = context.getBean(TranscriptionSegmentRepository.class);
        segmentTranslationRepository = context.getBean(SegmentTranslationRepository.class);
        blankGenerator = context.getBean(DictationBlankGenerator.class);
        segmentBlanksCodec = context.getBean(SegmentBlanksCodec.class);
        cacheManager = context.getBean(CacheManager.class);
        lessonService = context.getBean(LessonService.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void publishedDoneLessonIsCachedViaCacheableAndSkipsCountQuery() {
        UUID lessonId = UUID.randomUUID();
        UUID seg1Id = UUID.randomUUID();
        UUID seg2Id = UUID.randomUUID();

        Lesson lesson = Lesson.builder()
                .id(lessonId)
                .title("Public Lesson")
                .fileUrl("https://cdn.example.com/audio.mp3")
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .status(LessonStatus.DONE)
                .build();

        String blanksJson = segmentBlanksCodec.serialize(SegmentBlanksPayload.builder()
                .v(1)
                .wordCount(4)
                .suitable(true)
                .levels(Map.of(DictationMode.EASY,
                        List.of(BlankSlotDto.builder().wordIndex(0).acceptableAnswers(List.of("hello")).build())))
                .build());

        TranscriptionSegment seg1 = TranscriptionSegment.builder()
                .id(seg1Id)
                .text("Hello world today now")
                .startTime(0.0)
                .endTime(2.0)
                .displayOrder(0)
                .blanksJson(blanksJson)
                .lesson(lesson)
                .build();
        TranscriptionSegment seg2 = TranscriptionSegment.builder()
                .id(seg2Id)
                .text("Second sentence here now")
                .startTime(2.0)
                .endTime(4.0)
                .displayOrder(1)
                .blanksJson(blanksJson)
                .lesson(lesson)
                .build();

        SegmentTranslationProjection proj1 = projection(seg1Id, "vi", "Xin chao");
        lesson.setSegments(List.of(seg1, seg2));

        when(lessonRepository.findByIdAndVisibilityStatus(lessonId, LessonVisibilityStatus.PUBLISHED))
                .thenReturn(Optional.of(lesson));
        when(segmentTranslationRepository.findTranslationsBySegmentIdsAndTargetLanguage(
                List.of(seg1Id, seg2Id), TranslationLanguage.VIETNAMESE.getCode()))
                .thenReturn(List.of(proj1));

        LessonResponse first = lessonService.getAccessibleLessonDetail(lessonId, null);
        LessonResponse second = lessonService.getAccessibleLessonDetail(lessonId, null);

        assertSame(first, second);
        assertEquals(lessonId, first.getId());
        assertEquals(2, first.getSegments().size());
        assertEquals("Xin chao", first.getSegments().get(0).getTranslation().getTranslatedText());
        assertNull(first.getSegments().get(1).getTranslation());
        assertEquals(TranslationStatus.PARTIAL, first.getTranslationSummary().status());
        assertEquals(1L, first.getTranslationSummary().translatedCount());
        assertEquals(2, first.getTranslationSummary().totalSegments());

        verify(lessonRepository, times(1)).findByIdAndVisibilityStatus(lessonId, LessonVisibilityStatus.PUBLISHED);
        verify(transcriptionSegmentRepository, never()).findByLessonIdOrderByDisplayOrderAsc(any());
        verify(segmentTranslationRepository, never()).countBySegmentLessonIdAndTargetLanguage(any(), any());
        verify(transcriptionSegmentRepository, never()).saveAll(anyList());

        Cache cache = cacheManager.getCache(CacheNames.LESSON_DETAIL_CACHE);
        assertNotNull(cache.get(lessonId, LessonResponse.class));
    }

    @Test
    void hiddenOwnedLessonUsesSingleCombinedQueryAndIsNotCached() {
        UUID lessonId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).build();

        Lesson hiddenLesson = Lesson.builder()
                .id(lessonId)
                .ownerId(ownerId)
                .title("Private Lesson")
                .fileUrl("https://cdn.example.com/private.mp3")
                .visibilityStatus(LessonVisibilityStatus.HIDDEN)
                .status(LessonStatus.DONE)
                .build();

        when(lessonRepository.findAccessibleByIdAndOwnerId(lessonId, ownerId))
                .thenReturn(Optional.of(hiddenLesson));
        when(transcriptionSegmentRepository.findByLessonIdOrderByDisplayOrderAsc(lessonId))
                .thenReturn(List.of());

        LessonResponse result = lessonService.getAccessibleLessonDetail(lessonId, owner);

        assertEquals("Private Lesson", result.getTitle());
        verify(lessonRepository).findAccessibleByIdAndOwnerId(lessonId, ownerId);
        verify(lessonRepository, never()).findByIdAndVisibilityStatus(any(), any());
        verify(lessonRepository, never()).findByIdAndOwnerId(any(), any());

        Cache cache = cacheManager.getCache(CacheNames.LESSON_DETAIL_CACHE);
        assertNull(cache.get(lessonId, LessonResponse.class));
    }

    @Test
    void unauthorizedUserGets404WhenLessonNotAccessible() {
        UUID lessonId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        User otherUser = User.builder().id(otherUserId).build();

        when(lessonRepository.findAccessibleByIdAndOwnerId(lessonId, otherUserId))
                .thenReturn(Optional.empty());

        ParroTalkException ex = assertThrows(
                ParroTalkException.class,
                () -> lessonService.getAccessibleLessonDetail(lessonId, otherUser));
        assertEquals(404, ex.getHttpStatusCode().value());
    }

    @Test
    void missingBlanksAreGeneratedAndSavedWhenNull() {
        UUID lessonId = UUID.randomUUID();
        UUID segId = UUID.randomUUID();
        Lesson lesson = Lesson.builder()
                .id(lessonId)
                .title("Legacy Lesson")
                .fileUrl("https://cdn.example.com/legacy.mp3")
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .status(LessonStatus.DONE)
                .build();
        TranscriptionSegment segment = TranscriptionSegment.builder()
                .id(segId)
                .text("Generate blanks for this")
                .startTime(0.0)
                .endTime(1.5)
                .displayOrder(0)
                .blanksJson(null)
                .lesson(lesson)
                .build();

        SegmentBlanksPayload generatedPayload = SegmentBlanksPayload.builder()
                .v(1)
                .wordCount(4)
                .suitable(true)
                .levels(Map.of(DictationMode.EASY, List.of()))
                .build();

        lesson.setSegments(List.of(segment));

        when(lessonRepository.findByIdAndVisibilityStatus(lessonId, LessonVisibilityStatus.PUBLISHED))
                .thenReturn(Optional.of(lesson));
        when(blankGenerator.generateForSegments(List.of("Generate blanks for this")))
                .thenReturn(List.of(generatedPayload));
        when(segmentTranslationRepository.findTranslationsBySegmentIdsAndTargetLanguage(
                eq(List.of(segId)), eq(TranslationLanguage.VIETNAMESE.getCode())))
                .thenReturn(List.of());

        LessonResponse result = lessonService.getAccessibleLessonDetail(lessonId, null);

        assertEquals(1, result.getSegments().size());
        assertEquals(4, result.getSegments().get(0).getWordCount());
        verify(transcriptionSegmentRepository).saveAll(List.of(segment));
    }

    private static SegmentTranslationProjection projection(UUID segmentId, String targetLanguage, String translatedText) {
        return new SegmentTranslationProjection() {
            @Override
            public UUID getSegmentId() {
                return segmentId;
            }

            @Override
            public String getTargetLanguage() {
                return targetLanguage;
            }

            @Override
            public String getTranslatedText() {
                return translatedText;
            }
        };
    }
}
