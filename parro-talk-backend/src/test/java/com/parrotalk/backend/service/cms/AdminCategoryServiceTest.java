package com.parrotalk.backend.service.cms;

import com.parrotalk.backend.constant.CmsItemStatus;
import com.parrotalk.backend.dto.admin.AdminCategoryCreateRequest;
import com.parrotalk.backend.dto.admin.AdminCategoryDto;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.mapper.admin.AdminCategoryMapper;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.cms.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AdminCategoryMapper categoryMapper;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AdminCategoryService categoryService;

    private AdminCategoryCreateRequest createRequest;
    private Category category;
    private AdminCategoryDto categoryDto;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        
        createRequest = new AdminCategoryCreateRequest();
        createRequest.setName("Test Category");
        createRequest.setSlug("test-category");
        createRequest.setStatus(CmsItemStatus.ACTIVE);
        createRequest.setSortOrder(1);

        category = Category.builder()
                .name("Test Category")
                .slug("test-category")
                .status(CmsItemStatus.ACTIVE)
                .sortOrder(1)
                .build();
        category.setId(categoryId);
        category.setPath("/" + categoryId + "/");

        categoryDto = new AdminCategoryDto();
        categoryDto.setId(categoryId);
        categoryDto.setName("Test Category");
        categoryDto.setSlug("test-category");
    }

    @Test
    void createCategory_Success() {
        // Arrange
        when(categoryRepository.existsBySlug(createRequest.getSlug())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toDto(any(Category.class))).thenReturn(categoryDto);

        // Act
        AdminCategoryDto result = categoryService.createCategory(createRequest);

        // Assert
        assertNotNull(result);
        assertEquals(categoryDto.getName(), result.getName());
        
        // Verify path was updated properly (called save twice)
        verify(categoryRepository, times(2)).save(any(Category.class));
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    void createCategory_SlugExists_ThrowsException() {
        // Arrange
        when(categoryRepository.existsBySlug(createRequest.getSlug())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.createCategory(createRequest);
        });
        
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void deleteCategory_Success() {
        // Arrange
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.countLessonsByCategoryId(categoryId)).thenReturn(0L);

        // Act
        categoryService.deleteCategory(categoryId);

        // Assert
        verify(categoryRepository, times(1)).delete(category);
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    void deleteCategory_InUse_ThrowsException() {
        // Arrange
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.countLessonsByCategoryId(categoryId)).thenReturn(5L);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            categoryService.deleteCategory(categoryId);
        });
        
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
