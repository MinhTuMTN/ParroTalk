package com.parrotalk.backend.service.cms;

import com.parrotalk.backend.constant.CmsItemStatus;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.admin.AdminCategoryCreateRequest;
import com.parrotalk.backend.dto.admin.AdminCategoryDto;
import com.parrotalk.backend.dto.admin.AdminCategoryReorderRequest;
import com.parrotalk.backend.dto.admin.AdminCategoryUpdateRequest;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.entity.cms.AuditLog;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.mapper.admin.AdminCategoryMapper;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.cms.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final AdminCategoryMapper categoryMapper;
    private final AuditLogRepository auditLogRepository;

    public static String generateSlug(String input) {
        if (input == null || input.isBlank()) {
            throw new ParroTalkException("Cannot generate valid slug from name", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        String replaced = input.replace('đ', 'd').replace('Đ', 'd');
        String normalized = Normalizer.normalize(replaced, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.isBlank()) {
            throw new ParroTalkException("Cannot generate valid slug from name", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        return slug;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminCategoryDto> searchCategories(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sortOrder").ascending().and(Sort.by("createdAt").descending()));

        Page<Category> categoryPage;
        if (query != null && !query.isBlank()) {
            categoryPage = categoryRepository.searchByKeyword(query.trim(), pageable);
        } else {
            categoryPage = categoryRepository.findAll(pageable);
        }

        Map<UUID, Long> lessonsCountMap = getLessonsCountMap();
        List<AdminCategoryDto> content = categoryPage.getContent().stream().map(c -> {
            AdminCategoryDto dto = categoryMapper.toDto(c);
            dto.setLessonsCount(lessonsCountMap.getOrDefault(c.getId(), 0L));
            return dto;
        }).collect(Collectors.toList());

        return PageResponse.<AdminCategoryDto>builder()
                .content(content)
                .page(categoryPage.getNumber())
                .size(categoryPage.getSize())
                .totalPages(categoryPage.getTotalPages())
                .totalElements(categoryPage.getTotalElements())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminCategoryDto getCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ParroTalkException("Category not found", "NOT_FOUND", HttpStatus.NOT_FOUND));
        AdminCategoryDto dto = categoryMapper.toDto(category);
        dto.setLessonsCount(categoryRepository.countLessonsByCategoryId(category.getId()));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<AdminCategoryDto> getCategoryTree() {
        List<Category> allCategories = categoryRepository.findAll(
                Sort.by("sortOrder").ascending().and(Sort.by("name").ascending())
        );
        Map<UUID, Long> lessonsCountMap = getLessonsCountMap();

        Map<UUID, AdminCategoryDto> dtoMap = new HashMap<>();
        List<AdminCategoryDto> roots = new ArrayList<>();

        for (Category c : allCategories) {
            AdminCategoryDto dto = categoryMapper.toDto(c);
            dto.setLessonsCount(lessonsCountMap.getOrDefault(c.getId(), 0L));
            dto.setChildren(new ArrayList<>());
            dtoMap.put(dto.getId(), dto);
        }

        for (Category c : allCategories) {
            AdminCategoryDto dto = dtoMap.get(c.getId());
            if (c.getParentCategoryId() == null) {
                roots.add(dto);
            } else {
                AdminCategoryDto parentDto = dtoMap.get(c.getParentCategoryId());
                if (parentDto != null) {
                    parentDto.getChildren().add(dto);
                } else {
                    roots.add(dto); // fallback if parent missing
                }
            }
        }

        return roots;
    }

    @Transactional
    public AdminCategoryDto createCategory(AdminCategoryCreateRequest request) {
        String slug = (request.getSlug() == null || request.getSlug().isBlank())
                ? generateSlug(request.getName())
                : request.getSlug().trim();

        if (categoryRepository.existsBySlug(slug)) {
            throw new ParroTalkException("Slug already exists", "CONFLICT", HttpStatus.CONFLICT);
        }

        Category parent = null;
        String path = "/";
        if (request.getParentCategoryId() != null) {
            parent = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new ParroTalkException("Parent category not found", "NOT_FOUND", HttpStatus.NOT_FOUND));
            path = parent.getPath() != null ? parent.getPath() : "/" + parent.getId() + "/";
        }

        Category category = Category.builder()
                .name(request.getName())
                .nameVi(request.getNameVi())
                .slug(slug)
                .description(request.getDescription())
                .icon(request.getIcon())
                .color(request.getColor())
                .imageUrl(request.getImageUrl())
                .parentCategoryId(request.getParentCategoryId())
                .sortOrder(request.getSortOrder())
                .status(request.getStatus())
                .build();

        category = categoryRepository.save(category);

        // Update path after save to get the UUID
        category.setPath(path + category.getId() + "/");
        category = categoryRepository.save(category);

        logAudit("Category", category.getId(), "CREATE");

        AdminCategoryDto dto = categoryMapper.toDto(category);
        dto.setLessonsCount(0L);
        return dto;
    }

    @Transactional
    public AdminCategoryDto updateCategory(UUID id, AdminCategoryUpdateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ParroTalkException("Category not found", "NOT_FOUND", HttpStatus.NOT_FOUND));

        String slug = (request.getSlug() == null || request.getSlug().isBlank())
                ? generateSlug(request.getName())
                : request.getSlug().trim();

        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ParroTalkException("Slug already exists", "CONFLICT", HttpStatus.CONFLICT);
        }

        String parentBasePath = "/";
        if (request.getParentCategoryId() != null) {
            if (request.getParentCategoryId().equals(id)) {
                throw new ParroTalkException("Category cannot be its own parent", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
            }
            Category parent = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new ParroTalkException("Parent category not found", "NOT_FOUND", HttpStatus.NOT_FOUND));

            if (parent.getPath() != null && parent.getPath().contains("/" + id + "/")) {
                throw new ParroTalkException("Cannot create a cycle in category hierarchy", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
            }
            parentBasePath = parent.getPath() != null ? parent.getPath() : "/" + parent.getId() + "/";
        }

        String oldPath = category.getPath();
        String newPath = parentBasePath + category.getId() + "/";

        category.setParentCategoryId(request.getParentCategoryId());
        if (!Objects.equals(oldPath, newPath)) {
            category.setPath(newPath);
            if (oldPath != null && !oldPath.isBlank()) {
                List<Category> descendants = categoryRepository.findByPathStartingWith(oldPath);
                for (Category child : descendants) {
                    if (!child.getId().equals(id) && child.getPath() != null && child.getPath().startsWith(oldPath)) {
                        child.setPath(newPath + child.getPath().substring(oldPath.length()));
                        categoryRepository.save(child);
                    }
                }
            }
        }

        category.setName(request.getName());
        category.setNameVi(request.getNameVi());
        category.setSlug(slug);
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        category.setImageUrl(request.getImageUrl());
        category.setSortOrder(request.getSortOrder());
        category.setStatus(request.getStatus());

        category = categoryRepository.save(category);

        logAudit("Category", category.getId(), "UPDATE");

        AdminCategoryDto dto = categoryMapper.toDto(category);
        dto.setLessonsCount(categoryRepository.countLessonsByCategoryId(category.getId()));
        return dto;
    }

    @Transactional
    public AdminCategoryDto updateCategoryStatus(UUID id, CmsItemStatus status) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ParroTalkException("Category not found", "NOT_FOUND", HttpStatus.NOT_FOUND));

        category.setStatus(status);
        category = categoryRepository.save(category);

        logAudit("Category", category.getId(), "UPDATE_STATUS");

        AdminCategoryDto dto = categoryMapper.toDto(category);
        dto.setLessonsCount(categoryRepository.countLessonsByCategoryId(category.getId()));
        return dto;
    }

    @Transactional
    public void reorderCategories(AdminCategoryReorderRequest request) {
        List<UUID> orderedIds = request.getOrderedIds();
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new ParroTalkException("orderedIds cannot be empty", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }

        Set<UUID> uniqueRequestedIds = new HashSet<>(orderedIds);
        if (uniqueRequestedIds.size() != orderedIds.size()) {
            throw new ParroTalkException("Duplicate category IDs in reorder request", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }

        List<Category> siblings = request.getParentCategoryId() == null
                ? categoryRepository.findByParentCategoryIdIsNullOrderBySortOrderAsc()
                : categoryRepository.findByParentCategoryIdOrderBySortOrderAsc(request.getParentCategoryId());

        Map<UUID, Category> siblingMap = siblings.stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        if (!siblingMap.keySet().equals(uniqueRequestedIds)) {
            throw new ParroTalkException(
                    "orderedIds must match all siblings of the specified parent category",
                    "VALIDATION_ERROR",
                    HttpStatus.BAD_REQUEST
            );
        }

        List<Category> toSave = new ArrayList<>(orderedIds.size());
        for (int i = 0; i < orderedIds.size(); i++) {
            Category sibling = siblingMap.get(orderedIds.get(i));
            sibling.setSortOrder(i);
            toSave.add(sibling);
        }

        categoryRepository.saveAll(toSave);
        UUID auditTargetId = request.getParentCategoryId() != null ? request.getParentCategoryId() : orderedIds.get(0);
        logAudit("Category", auditTargetId, "REORDER");
    }

    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ParroTalkException("Category not found", "NOT_FOUND", HttpStatus.NOT_FOUND));

        long childCount = categoryRepository.countByParentCategoryId(id);
        if (childCount > 0) {
            throw new ParroTalkException(
                    "Cannot delete category with " + childCount + " subcategories. Move or delete subcategories first.",
                    "CONFLICT",
                    HttpStatus.CONFLICT
            );
        }

        long lessonsCount = categoryRepository.countLessonsByCategoryId(id);
        if (lessonsCount > 0) {
            throw new ParroTalkException(
                    "Cannot delete category used by " + lessonsCount + " lessons",
                    "CONFLICT",
                    HttpStatus.CONFLICT
            );
        }

        categoryRepository.delete(category);
        logAudit("Category", id, "DELETE");
    }

    private Map<UUID, Long> getLessonsCountMap() {
        List<CategoryRepository.CategoryLessonCountProjection> counts = categoryRepository.countLessonsAllCategories();
        if (counts == null) {
            return Map.of();
        }
        return counts.stream().collect(Collectors.toMap(
                CategoryRepository.CategoryLessonCountProjection::getCategoryId,
                CategoryRepository.CategoryLessonCountProjection::getLessonsCount,
                (a, b) -> a
        ));
    }

    private void logAudit(String entityName, UUID entityId, String action) {
        AuditLog log = AuditLog.builder()
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .build();
        auditLogRepository.save(log);
    }
}
