package com.parrotalk.backend.service.cms;

import com.parrotalk.backend.constant.CmsItemStatus;
import com.parrotalk.backend.dto.admin.AdminCategoryCreateRequest;
import com.parrotalk.backend.dto.admin.AdminCategoryDto;
import com.parrotalk.backend.dto.admin.AdminCategoryReorderRequest;
import com.parrotalk.backend.dto.admin.AdminCategoryUpdateRequest;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.exception.ParroTalkException;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.List;
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
        createRequest.setNameVi("Danh mục thử nghiệm");
        createRequest.setSlug("test-category");
        createRequest.setStatus(CmsItemStatus.ACTIVE);
        createRequest.setSortOrder(1);

        category = Category.builder()
                .name("Test Category")
                .nameVi("Danh mục thử nghiệm")
                .slug("test-category")
                .status(CmsItemStatus.ACTIVE)
                .sortOrder(1)
                .build();
        category.setId(categoryId);
        category.setPath("/" + categoryId + "/");

        categoryDto = new AdminCategoryDto();
        categoryDto.setId(categoryId);
        categoryDto.setName("Test Category");
        categoryDto.setNameVi("Danh mục thử nghiệm");
        categoryDto.setSlug("test-category");
    }

    @Test
    void generateSlug_VietnameseCharacters_NormalizedCorrectly() {
        assertEquals("du-lich-di-chuyen", AdminCategoryService.generateSlug("Du lịch & Di chuyển"));
        assertEquals("doi-song-hang-ngay", AdminCategoryService.generateSlug("Đời sống hằng ngày"));
        assertEquals("cong-nghe-ai", AdminCategoryService.generateSlug("  Công nghệ / AI  "));
    }

    @Test
    void createCategory_Success() {
        when(categoryRepository.existsBySlug(createRequest.getSlug())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toDto(any(Category.class))).thenReturn(categoryDto);

        AdminCategoryDto result = categoryService.createCategory(createRequest);

        assertNotNull(result);
        assertEquals(categoryDto.getName(), result.getName());
        assertEquals("Danh mục thử nghiệm", result.getNameVi());

        // Verify path was updated properly (called save twice)
        verify(categoryRepository, times(2)).save(any(Category.class));
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    void createCategory_BlankSlug_AutoGeneratesVietnameseSafeSlug() {
        createRequest.setName("Du lịch & Di chuyển");
        createRequest.setSlug("   ");
        when(categoryRepository.existsBySlug("du-lich-di-chuyen")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(categoryId);
            }
            return c;
        });
        when(categoryMapper.toDto(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            AdminCategoryDto dto = new AdminCategoryDto();
            dto.setId(c.getId());
            dto.setName(c.getName());
            dto.setSlug(c.getSlug());
            return dto;
        });

        AdminCategoryDto result = categoryService.createCategory(createRequest);

        assertEquals("du-lich-di-chuyen", result.getSlug());
    }

    @Test
    void createCategory_SlugExists_ThrowsConflict() {
        when(categoryRepository.existsBySlug(createRequest.getSlug())).thenReturn(true);

        ParroTalkException ex = assertThrows(ParroTalkException.class, () ->
                categoryService.createCategory(createRequest)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatusCode());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void getCategoryTree_PopulatesBatchLessonsCountAndHierarchy() {
        UUID rootId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();

        Category root = Category.builder()
                .name("Root")
                .slug("root")
                .sortOrder(0)
                .status(CmsItemStatus.ACTIVE)
                .path("/" + rootId + "/")
                .build();
        root.setId(rootId);

        Category child = Category.builder()
                .name("Child")
                .slug("child")
                .parentCategoryId(rootId)
                .sortOrder(0)
                .status(CmsItemStatus.ACTIVE)
                .path("/" + rootId + "/" + childId + "/")
                .build();
        child.setId(childId);

        AdminCategoryDto rootDto = AdminCategoryDto.builder().id(rootId).name("Root").slug("root").build();
        AdminCategoryDto childDto = AdminCategoryDto.builder().id(childId).name("Child").slug("child").parentCategoryId(rootId).build();

        when(categoryRepository.findAll(any(Sort.class))).thenReturn(List.of(root, child));
        when(categoryMapper.toDto(root)).thenReturn(rootDto);
        when(categoryMapper.toDto(child)).thenReturn(childDto);

        CategoryRepository.CategoryLessonCountProjection rootCount = mock(CategoryRepository.CategoryLessonCountProjection.class);
        when(rootCount.getCategoryId()).thenReturn(rootId);
        when(rootCount.getLessonsCount()).thenReturn(3L);

        CategoryRepository.CategoryLessonCountProjection childCount = mock(CategoryRepository.CategoryLessonCountProjection.class);
        when(childCount.getCategoryId()).thenReturn(childId);
        when(childCount.getLessonsCount()).thenReturn(7L);

        when(categoryRepository.countLessonsAllCategories()).thenReturn(List.of(rootCount, childCount));

        List<AdminCategoryDto> tree = categoryService.getCategoryTree();

        assertEquals(1, tree.size());
        assertEquals(3L, tree.get(0).getLessonsCount());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals(7L, tree.get(0).getChildren().get(0).getLessonsCount());
        verify(categoryRepository, times(1)).countLessonsAllCategories();
        verify(categoryRepository, never()).countLessonsByCategoryId(any());
    }

    @Test
    void updateCategory_MoveToRoot_UpdatesDescendantPaths() {
        UUID parentId = UUID.randomUUID();
        UUID nodeId = UUID.randomUUID();
        UUID grandChildId = UUID.randomUUID();

        String oldNodePath = "/" + parentId + "/" + nodeId + "/";
        Category node = Category.builder()
                .name("Node")
                .slug("node")
                .parentCategoryId(parentId)
                .path(oldNodePath)
                .sortOrder(0)
                .status(CmsItemStatus.ACTIVE)
                .build();
        node.setId(nodeId);

        Category grandChild = Category.builder()
                .name("GrandChild")
                .slug("grandchild")
                .parentCategoryId(nodeId)
                .path(oldNodePath + grandChildId + "/")
                .sortOrder(0)
                .status(CmsItemStatus.ACTIVE)
                .build();
        grandChild.setId(grandChildId);

        AdminCategoryUpdateRequest req = new AdminCategoryUpdateRequest();
        req.setName("Node Updated");
        req.setNameVi("Nút");
        req.setSlug("");
        req.setParentCategoryId(null);
        req.setSortOrder(0);
        req.setStatus(CmsItemStatus.ACTIVE);

        when(categoryRepository.findById(nodeId)).thenReturn(Optional.of(node));
        when(categoryRepository.existsBySlugAndIdNot("node-updated", nodeId)).thenReturn(false);
        when(categoryRepository.findByPathStartingWith(oldNodePath)).thenReturn(List.of(node, grandChild));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryMapper.toDto(any(Category.class))).thenReturn(new AdminCategoryDto());
        when(categoryRepository.countLessonsByCategoryId(nodeId)).thenReturn(2L);

        categoryService.updateCategory(nodeId, req);

        assertNull(node.getParentCategoryId());
        assertEquals("/" + nodeId + "/", node.getPath());
        assertEquals("/" + nodeId + "/" + grandChildId + "/", grandChild.getPath());
        assertEquals("node-updated", node.getSlug());
        assertEquals("Nút", node.getNameVi());
        verify(categoryRepository).save(grandChild);
    }

    @Test
    void updateCategory_CycleDetected_ThrowsBadRequest() {
        UUID childId = UUID.randomUUID();
        Category child = Category.builder()
                .name("Child")
                .slug("child")
                .parentCategoryId(categoryId)
                .path("/" + categoryId + "/" + childId + "/")
                .build();
        child.setId(childId);

        AdminCategoryUpdateRequest req = new AdminCategoryUpdateRequest();
        req.setName("Test Category");
        req.setSlug("test-category");
        req.setParentCategoryId(childId);
        req.setSortOrder(0);
        req.setStatus(CmsItemStatus.ACTIVE);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsBySlugAndIdNot("test-category", categoryId)).thenReturn(false);
        when(categoryRepository.findById(childId)).thenReturn(Optional.of(child));

        ParroTalkException ex = assertThrows(ParroTalkException.class, () ->
                categoryService.updateCategory(categoryId, req)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatusCode());
    }

    @Test
    void updateCategoryStatus_Success() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.countLessonsByCategoryId(categoryId)).thenReturn(4L);
        when(categoryMapper.toDto(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            return AdminCategoryDto.builder()
                    .id(c.getId())
                    .status(c.getStatus())
                    .build();
        });

        AdminCategoryDto result = categoryService.updateCategoryStatus(categoryId, CmsItemStatus.INACTIVE);

        assertEquals(CmsItemStatus.INACTIVE, result.getStatus());
        assertEquals(4L, result.getLessonsCount());
        verify(auditLogRepository).save(any());
    }

    @Test
    void reorderCategories_ValidSiblings_UpdatesSortOrders() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Category c1 = Category.builder().name("A").slug("a").sortOrder(0).build();
        c1.setId(id1);
        Category c2 = Category.builder().name("B").slug("b").sortOrder(1).build();
        c2.setId(id2);

        when(categoryRepository.findByParentCategoryIdIsNullOrderBySortOrderAsc()).thenReturn(List.of(c1, c2));

        AdminCategoryReorderRequest req = AdminCategoryReorderRequest.builder()
                .parentCategoryId(null)
                .orderedIds(List.of(id2, id1))
                .build();

        categoryService.reorderCategories(req);

        assertEquals(0, c2.getSortOrder());
        assertEquals(1, c1.getSortOrder());
        verify(categoryRepository).saveAll(anyList());
        verify(auditLogRepository).save(any());
    }

    @Test
    void reorderCategories_MismatchedSiblings_ThrowsBadRequest() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Category c1 = Category.builder().name("A").slug("a").sortOrder(0).build();
        c1.setId(id1);

        when(categoryRepository.findByParentCategoryIdIsNullOrderBySortOrderAsc()).thenReturn(List.of(c1));

        AdminCategoryReorderRequest req = AdminCategoryReorderRequest.builder()
                .parentCategoryId(null)
                .orderedIds(List.of(id1, id2))
                .build();

        ParroTalkException ex = assertThrows(ParroTalkException.class, () ->
                categoryService.reorderCategories(req)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatusCode());
        verify(categoryRepository, never()).saveAll(anyList());
    }

    @Test
    void deleteCategory_Success() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.countByParentCategoryId(categoryId)).thenReturn(0L);
        when(categoryRepository.countLessonsByCategoryId(categoryId)).thenReturn(0L);

        categoryService.deleteCategory(categoryId);

        verify(categoryRepository, times(1)).delete(category);
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    void deleteCategory_HasChildren_ThrowsConflict() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.countByParentCategoryId(categoryId)).thenReturn(2L);

        ParroTalkException ex = assertThrows(ParroTalkException.class, () ->
                categoryService.deleteCategory(categoryId)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatusCode());
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteCategory_InUse_ThrowsConflict() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.countByParentCategoryId(categoryId)).thenReturn(0L);
        when(categoryRepository.countLessonsByCategoryId(categoryId)).thenReturn(5L);

        ParroTalkException ex = assertThrows(ParroTalkException.class, () ->
                categoryService.deleteCategory(categoryId)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatusCode());
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
