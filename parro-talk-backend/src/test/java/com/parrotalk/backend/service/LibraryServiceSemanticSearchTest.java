package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.parrotalk.backend.config.CloudflareProperties;
import com.parrotalk.backend.dto.LessonWithProgressDTO;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.LessonRepository;

@ExtendWith(MockitoExtension.class)
class LibraryServiceSemanticSearchTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private LibraryCacheService libraryCacheService;

    @Mock
    private CloudflareEmbeddingClient cloudflareEmbeddingClient;

    private CloudflareProperties cloudflareProperties;
    private LibraryService libraryService;

    @BeforeEach
    void setUp() {
        cloudflareProperties = new CloudflareProperties();
        cloudflareProperties.setMinSimilarity(0.30);
        Executor directExecutor = Runnable::run;
        libraryService = new LibraryService(
                lessonRepository,
                categoryRepository,
                libraryCacheService,
                directExecutor,
                cloudflareEmbeddingClient,
                cloudflareProperties);
    }

    @Test
    void searchPublicLessonsSemantic_blankQuerySkipsCloudflareAndDelegatesToBrowse() {
        LessonWithProgressDTO dto = new LessonWithProgressDTO(UUID.randomUUID(), "Browse Lesson", 0.0, null, "a.mp3", 120);
        Page<LessonWithProgressDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 12), 1);
        when(lessonRepository.searchPublicLessons(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<LessonWithProgressDTO> res = libraryService.searchPublicLessonsSemantic(
                "   ", null, null, null, "latest", 0, 12, null);

        assertEquals(1, res.getTotalElements());
        assertEquals("Browse Lesson", res.getContent().get(0).getTitle());
        verifyNoInteractions(cloudflareEmbeddingClient);
    }

    @Test
    void searchPublicLessonsSemantic_embedsQueryAndMapsRows() {
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("u@example.com").build();
        when(cloudflareEmbeddingClient.embedQueryToVectorLiteral("nấu ăn"))
                .thenReturn(Optional.of("[0.1,0.2]"));

        Object[] row = new Object[] { lessonId, "Preserving traditional recipes", 50.0, null, "audio.mp3", 360 };
        Page<Object[]> rawPage = new PageImpl<>(Collections.singletonList(row), PageRequest.of(0, 12), 1);
        when(lessonRepository.searchPublicLessonsSemantic(
                eq("[0.1,0.2]"),
                eq(0.30),
                eq("food"),
                eq(""),
                eq(""),
                eq(userId),
                eq("relevance"),
                any(Pageable.class)))
                .thenReturn(rawPage);

        PageResponse<LessonWithProgressDTO> res = libraryService.searchPublicLessonsSemantic(
                " nấu ăn ", "food", null, null, "relevance", 0, 12, user);

        assertEquals(1, res.getTotalElements());
        LessonWithProgressDTO item = res.getContent().get(0);
        assertEquals(lessonId, item.getId());
        assertEquals("Preserving traditional recipes", item.getTitle());
        assertEquals(50, item.getProgress());
        assertEquals(LessonWithProgressDTO.DEFAULT_THUMBNAIL_URL, item.getThumbnailUrl());
        assertEquals(360, item.getDuration());
    }

    @Test
    void searchPublicLessonsSemantic_fallsBackToLikeWithSafeSortWhenEmbeddingEmpty() {
        when(cloudflareEmbeddingClient.embedQueryToVectorLiteral("weather")).thenReturn(Optional.empty());
        Page<LessonWithProgressDTO> page = new PageImpl<>(List.of(), PageRequest.of(0, 12), 0);
        when(lessonRepository.searchPublicLessons(eq("weather"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        libraryService.searchPublicLessonsSemantic("weather", null, null, null, "relevance", 0, 12, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(lessonRepository).searchPublicLessons(
                eq("weather"), isNull(), isNull(), isNull(), isNull(), pageableCaptor.capture());
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), pageableCaptor.getValue().getSort());
    }
}
