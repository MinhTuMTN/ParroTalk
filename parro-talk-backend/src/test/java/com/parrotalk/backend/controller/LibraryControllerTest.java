package com.parrotalk.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.parrotalk.backend.constant.SectionType;
import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.LessonWithProgressDTO;
import com.parrotalk.backend.dto.LibrarySectionDTO;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.LibraryService;

@ExtendWith(MockitoExtension.class)
class LibraryControllerTest {

    @Mock
    private LibraryService libraryService;

    @InjectMocks
    private LibraryController libraryController;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .fullName("Test User")
                .build();
    }

    @Test
    @DisplayName("GET /api/library/sections returns sections")
    void getLibrarySections_ReturnsOkWithSections() {
        LibrarySectionDTO section = LibrarySectionDTO.builder()
                .id(UUID.randomUUID())
                .title("Featured Topics")
                .type(SectionType.CATEGORY)
                .items(List.of(new LessonWithProgressDTO(UUID.randomUUID(), "Lesson 1", 50.0, "thumb.jpg", "url.mp3", 300)))
                .build();

        when(libraryService.getLibrarySections(mockUser.getId())).thenReturn(List.of(section));

        ResponseEntity<ApiResponse<List<LibrarySectionDTO>>> response = libraryController.getLibrarySections(mockUser);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getResult().size());
        assertEquals("Featured Topics", response.getBody().getResult().get(0).getTitle());
        verify(libraryService).getLibrarySections(mockUser.getId());
    }

    @Test
    @DisplayName("GET /api/library/categories/{slug}/lessons returns page of lessons")
    void getCategoryLessons_ReturnsOkWithPage() {
        LessonWithProgressDTO lesson = new LessonWithProgressDTO(UUID.randomUUID(), "Lesson A", 0.0, "thumb.jpg", "url.mp3", 200);
        PageResponse<LessonWithProgressDTO> pageResponse = PageResponse.<LessonWithProgressDTO>builder()
                .content(List.of(lesson))
                .page(0)
                .size(12)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(libraryService.getCategoryLessons(eq("technology"), any(), any(), eq("latest"), eq(0), eq(12), any()))
                .thenReturn(pageResponse);

        ResponseEntity<ApiResponse<PageResponse<LessonWithProgressDTO>>> response =
                libraryController.getCategoryLessons("technology", null, null, "latest", 0, 12, null);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getResult().getTotalElements());
        assertEquals("Lesson A", response.getBody().getResult().getContent().get(0).getTitle());
        verify(libraryService).getCategoryLessons(eq("technology"), any(), any(), eq("latest"), eq(0), eq(12), any());
    }

    @Test
    @DisplayName("GET /api/library/search returns search results")
    void searchPublicLessons_ReturnsOkWithPage() {
        LessonWithProgressDTO lesson = new LessonWithProgressDTO(UUID.randomUUID(), "Search Match", 75.0, "thumb.jpg", "url.mp3", 180);
        PageResponse<LessonWithProgressDTO> pageResponse = PageResponse.<LessonWithProgressDTO>builder()
                .content(List.of(lesson))
                .page(0)
                .size(12)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(libraryService.searchPublicLessons(eq("query"), any(), any(), any(), eq("latest"), eq(0), eq(12), eq(mockUser)))
                .thenReturn(pageResponse);

        ResponseEntity<ApiResponse<PageResponse<LessonWithProgressDTO>>> response =
                libraryController.searchPublicLessons("query", null, null, null, "latest", 0, 12, mockUser);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getResult().getTotalElements());
        assertEquals("Search Match", response.getBody().getResult().getContent().get(0).getTitle());
        verify(libraryService).searchPublicLessons(eq("query"), any(), any(), any(), eq("latest"), eq(0), eq(12), eq(mockUser));
    }
}