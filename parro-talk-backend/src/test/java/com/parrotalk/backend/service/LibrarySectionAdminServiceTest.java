package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
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

import com.parrotalk.backend.constant.SectionType;
import com.parrotalk.backend.dto.admin.AdminLibrarySectionRequest;
import com.parrotalk.backend.dto.admin.AdminLibrarySectionResponse;
import com.parrotalk.backend.dto.admin.ReorderLibrarySectionsRequest;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.entity.LibrarySection;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.LibrarySectionRepository;

@ExtendWith(MockitoExtension.class)
class LibrarySectionAdminServiceTest {

    @Mock
    private LibrarySectionRepository librarySectionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private LibrarySectionAdminService service;

    private UUID categoryId;
    private UUID sectionId;
    private Category mockCategory;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        sectionId = UUID.randomUUID();
        mockCategory = Category.builder()
                .id(categoryId)
                .name("Technology")
                .slug("technology")
                .build();
    }

    @Test
    @DisplayName("createSection: throws BAD_REQUEST when CATEGORY type has null categoryId")
    void createSection_CategoryNullId_ThrowsException() {
        AdminLibrarySectionRequest request = AdminLibrarySectionRequest.builder()
                .title("Tech")
                .type(SectionType.CATEGORY)
                .categoryId(null)
                .build();

        ParroTalkException ex = assertThrows(ParroTalkException.class, () -> service.createSection(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatusCode());
    }

    @Test
    @DisplayName("createSection: throws NOT_FOUND when category does not exist")
    void createSection_CategoryNotFound_ThrowsException() {
        AdminLibrarySectionRequest request = AdminLibrarySectionRequest.builder()
                .title("Tech")
                .type(SectionType.CATEGORY)
                .categoryId(categoryId)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        ParroTalkException ex = assertThrows(ParroTalkException.class, () -> service.createSection(request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatusCode());
    }

    @Test
    @DisplayName("createSection: throws BAD_REQUEST when PROGRAM type has blank program")
    void createSection_ProgramBlank_ThrowsException() {
        AdminLibrarySectionRequest request = AdminLibrarySectionRequest.builder()
                .title("BBC")
                .type(SectionType.PROGRAM)
                .program("   ")
                .build();

        ParroTalkException ex = assertThrows(ParroTalkException.class, () -> service.createSection(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatusCode());
    }

    @Test
    @DisplayName("createSection: auto computes orderIndex when not specified")
    void createSection_AutoComputesOrderIndex() {
        AdminLibrarySectionRequest request = AdminLibrarySectionRequest.builder()
                .title("TED Talks")
                .type(SectionType.PROGRAM)
                .program("TED")
                .orderIndex(null)
                .build();

        LibrarySection existing = LibrarySection.builder()
                .id(UUID.randomUUID())
                .title("First Section")
                .orderIndex(5)
                .build();

        when(librarySectionRepository.findAllByOrderByOrderIndexAsc()).thenReturn(List.of(existing));
        when(librarySectionRepository.save(any(LibrarySection.class))).thenAnswer(i -> {
            LibrarySection s = i.getArgument(0);
            s.setId(sectionId);
            return s;
        });

        AdminLibrarySectionResponse response = service.createSection(request);

        assertNotNull(response);
        assertEquals(6, response.getOrderIndex());
        assertEquals("TED Talks", response.getTitle());
    }

    @Test
    @DisplayName("updateSection: throws NOT_FOUND when section does not exist")
    void updateSection_NotFound_ThrowsException() {
        AdminLibrarySectionRequest request = AdminLibrarySectionRequest.builder()
                .title("Tech")
                .type(SectionType.CATEGORY)
                .categoryId(categoryId)
                .build();

        when(librarySectionRepository.findById(sectionId)).thenReturn(Optional.empty());

        ParroTalkException ex = assertThrows(ParroTalkException.class, () -> service.updateSection(sectionId, request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatusCode());
    }

    @Test
    @DisplayName("deleteSection: throws NOT_FOUND when section does not exist")
    void deleteSection_NotFound_ThrowsException() {
        when(librarySectionRepository.findById(sectionId)).thenReturn(Optional.empty());

        ParroTalkException ex = assertThrows(ParroTalkException.class, () -> service.deleteSection(sectionId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatusCode());
    }

    @Test
    @DisplayName("reorderSections: throws BAD_REQUEST when items list is empty")
    void reorderSections_EmptyItems_ThrowsException() {
        ReorderLibrarySectionsRequest request = ReorderLibrarySectionsRequest.builder()
                .items(Collections.emptyList())
                .build();

        ParroTalkException ex = assertThrows(ParroTalkException.class, () -> service.reorderSections(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatusCode());
    }
}