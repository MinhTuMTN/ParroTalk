package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.collection.spi.PersistentSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.parrotalk.backend.config.AppSetting;
import com.parrotalk.backend.constant.LessonVisibilityStatus;
import com.parrotalk.backend.constant.TranslationStatus;
import com.parrotalk.backend.dto.LessonResponse;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.mapper.CategoryMapperImpl;
import com.parrotalk.backend.mapper.LessonMapperImpl;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.repository.SegmentTranslationRepository;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;

class LessonServiceTranslationStatusTest {

    private LessonRepository lessonRepository;
    private LessonService lessonService;
    private LessonMapperImpl lessonMapper;

    @BeforeEach
    void setUp() {
        lessonRepository = mock(LessonRepository.class);
        lessonMapper = new LessonMapperImpl();
        lessonService = new LessonService(
                lessonRepository,
                mock(SseService.class),
                lessonMapper,
                new CategoryMapperImpl(),
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

    @Test
    void searchAdminLessonsMapsCategoriesAndFallbackThumbnailWhenThumbnailIsNull() {
        UUID lessonId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Daily Life")
                .slug("daily-life")
                .imageUrl("https://cdn.example.com/daily-life.png")
                .sortOrder(1)
                .build();
        Lesson lesson = Lesson.builder()
                .id(lessonId)
                .title("How does the speaker spend free time?")
                .fileUrl("https://cdn.example.com/audio.mp3")
                .thumbnailUrl(null)
                .duration(21)
                .totalSegments(5)
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .categories(Set.of(category))
                .build();

        Page<Lesson> page = new PageImpl<>(List.of(lesson), PageRequest.of(0, 10), 1);
        when(lessonRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Lesson>>any(), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<LessonResponse> result = lessonService.searchAdminLessons(
                null, LessonVisibilityStatus.PUBLISHED, categoryId, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        LessonResponse dto = result.getContent().get(0);
        assertEquals("https://cdn.example.com/daily-life.png", dto.getThumbnailUrl());
        assertEquals(5, dto.getTotalSegments());
        assertEquals(1, dto.getCategories().size());
        assertEquals("Daily Life", dto.getCategories().iterator().next().getName());
    }

    @Test
    void resolveThumbnailUrlAndMapCategoriesSkipUninitializedPersistentSetWithoutLazyException() {
        Lesson lessonWithUninitializedCategories = Lesson.builder()
                .id(UUID.randomUUID())
                .title("Detached Lesson")
                .fileUrl("https://cdn.example.com/audio.mp3")
                .thumbnailUrl(null)
                .categories(new PersistentSet<>())
                .build();

        String resolved = assertDoesNotThrow(
                () -> lessonMapper.resolveThumbnailUrl(lessonWithUninitializedCategories));
        List<String> names = assertDoesNotThrow(
                () -> lessonMapper.mapCategories(lessonWithUninitializedCategories.getCategories()));

        assertNull(resolved);
        assertEquals(List.of(), names);
    }
}
