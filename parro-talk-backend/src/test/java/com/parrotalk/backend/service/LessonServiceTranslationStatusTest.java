package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.parrotalk.backend.config.AppSetting;
import com.parrotalk.backend.constant.TranslationStatus;
import com.parrotalk.backend.mapper.CategoryMapper;
import com.parrotalk.backend.mapper.LessonMapper;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.repository.SegmentTranslationRepository;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;

class LessonServiceTranslationStatusTest {

    private LessonService lessonService;

    @BeforeEach
    void setUp() {
        lessonService = new LessonService(
                mock(LessonRepository.class),
                mock(SseService.class),
                mock(LessonMapper.class),
                mock(CategoryMapper.class),
                mock(TranscriptionSegmentRepository.class),
                mock(SegmentTranslationRepository.class),
                mock(CategoryRepository.class),
                mock(TranslationService.class),
                mock(AppSetting.class));
    }

    @Test
    void resolveTranslationStatusReturnsNotStartedWhenNoTranslationsExist() {
        assertEquals(TranslationStatus.NOT_STARTED, lessonService.resolveTranslationStatus(0, 10));
    }

    @Test
    void resolveTranslationStatusReturnsPartialWhenSomeTranslationsExist() {
        assertEquals(TranslationStatus.PARTIAL, lessonService.resolveTranslationStatus(3, 10));
    }

    @Test
    void resolveTranslationStatusReturnsCompletedWhenAllTranslationsExist() {
        assertEquals(TranslationStatus.COMPLETED, lessonService.resolveTranslationStatus(10, 10));
    }
}
