package com.parrotalk.backend.controller.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
import com.parrotalk.backend.dto.admin.AdminLibrarySectionRequest;
import com.parrotalk.backend.dto.admin.AdminLibrarySectionResponse;
import com.parrotalk.backend.dto.admin.ReorderLibrarySectionsRequest;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.entity.LibrarySection;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.LibrarySectionRepository;
import com.parrotalk.backend.service.LibrarySectionAdminService;

@ExtendWith(MockitoExtension.class)
class AdminLibrarySectionControllerTest {

    @Mock
    private LibrarySectionAdminService librarySectionAdminService;

    @InjectMocks
    private AdminLibrarySectionController adminLibrarySectionController;

    private UUID sectionId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        sectionId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/admin/library/sections returns all sections")
    void getAllSections_ReturnsOkWithList() {
        AdminLibrarySectionResponse response = AdminLibrarySectionResponse.builder()
                .id(sectionId)
                .title("Tech News")
                .type(SectionType.CATEGORY)
                .categoryId(categoryId)
                .categoryName("Technology")
                .categorySlug("technology")
                .orderIndex(0)
                .isActive(true)
                .maxItems(6)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(librarySectionAdminService.getAllSections()).thenReturn(List.of(response));

        ResponseEntity<ApiResponse<List<AdminLibrarySectionResponse>>> result = adminLibrarySectionController.getAllSections();

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getResult().size());
        assertEquals("Tech News", result.getBody().getResult().get(0).getTitle());
        verify(librarySectionAdminService).getAllSections();
    }

    @Test
    @DisplayName("POST /api/admin/library/sections creates a new section")
    void createSection_ReturnsOkWithCreatedSection() {
        AdminLibrarySectionRequest request = AdminLibrarySectionRequest.builder()
                .title("BBC 6 Minute English")
                .type(SectionType.PROGRAM)
                .program("6 Minute English")
                .orderIndex(1)
                .isActive(true)
                .maxItems(8)
                .build();

        AdminLibrarySectionResponse response = AdminLibrarySectionResponse.builder()
                .id(sectionId)
                .title("BBC 6 Minute English")
                .type(SectionType.PROGRAM)
                .program("6 Minute English")
                .orderIndex(1)
                .isActive(true)
                .maxItems(8)
                .build();

        when(librarySectionAdminService.createSection(any(AdminLibrarySectionRequest.class))).thenReturn(response);

        ResponseEntity<ApiResponse<AdminLibrarySectionResponse>> result = adminLibrarySectionController.createSection(request);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("BBC 6 Minute English", result.getBody().getResult().getTitle());
        assertEquals(SectionType.PROGRAM, result.getBody().getResult().getType());
        verify(librarySectionAdminService).createSection(request);
    }

    @Test
    @DisplayName("PUT /api/admin/library/sections/{id} updates existing section")
    void updateSection_ReturnsOkWithUpdatedSection() {
        AdminLibrarySectionRequest request = AdminLibrarySectionRequest.builder()
                .title("Updated Section")
                .type(SectionType.CATEGORY)
                .categoryId(categoryId)
                .orderIndex(2)
                .isActive(true)
                .maxItems(10)
                .build();

        AdminLibrarySectionResponse response = AdminLibrarySectionResponse.builder()
                .id(sectionId)
                .title("Updated Section")
                .type(SectionType.CATEGORY)
                .categoryId(categoryId)
                .orderIndex(2)
                .isActive(true)
                .maxItems(10)
                .build();

        when(librarySectionAdminService.updateSection(eq(sectionId), any(AdminLibrarySectionRequest.class))).thenReturn(response);

        ResponseEntity<ApiResponse<AdminLibrarySectionResponse>> result = adminLibrarySectionController.updateSection(sectionId, request);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Updated Section", result.getBody().getResult().getTitle());
        verify(librarySectionAdminService).updateSection(sectionId, request);
    }

    @Test
    @DisplayName("DELETE /api/admin/library/sections/{id} deletes section")
    void deleteSection_ReturnsOk() {
        doNothing().when(librarySectionAdminService).deleteSection(sectionId);

        ResponseEntity<ApiResponse<Void>> result = adminLibrarySectionController.deleteSection(sectionId);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(librarySectionAdminService).deleteSection(sectionId);
    }

    @Test
    @DisplayName("PUT /api/admin/library/sections/reorder reorders sections")
    void reorderSections_ReturnsOk() {
        ReorderLibrarySectionsRequest request = ReorderLibrarySectionsRequest.builder()
                .items(List.of(
                        ReorderLibrarySectionsRequest.ReorderSectionItem.builder()
                                .id(sectionId)
                                .orderIndex(0)
                                .build()))
                .build();

        doNothing().when(librarySectionAdminService).reorderSections(any(ReorderLibrarySectionsRequest.class));

        ResponseEntity<ApiResponse<Void>> result = adminLibrarySectionController.reorderSections(request);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(librarySectionAdminService).reorderSections(request);
    }
}