package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.CategoryStatus;
import com.parrotalk.backend.dto.CategoryResponse;
import com.parrotalk.backend.dto.CreateCategoryRequest;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.ReorderCategoryItemRequest;
import com.parrotalk.backend.dto.UpdateCategoryRequest;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.mapper.CategoryMapper;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.specification.CategorySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Category Service.
 * 
 * @author MinhTuMTN
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final Pattern NON_ASCII_PATTERN = Pattern.compile("[^\\p{ASCII}]");
    private static final Pattern NON_SLUG_PATTERN = Pattern.compile("[^a-z0-9]+");
    private static final List<String> ALLOWED_SORT_FIELDS = List.of("displayOrder", "createdAt", "name");

    /** Category repository. */
    private final CategoryRepository categoryRepository;

    /** Lesson repository. */
    private final LessonRepository lessonRepository;

    /** Category mapper. */
    private final CategoryMapper categoryMapper;

    /**
     * Search public active categories by name.
     * 
     * @param query Query
     * @param page  Page number
     * @param size  Page size
     * @return Page response of categories
     */
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> searchCategories(
            String query,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "displayOrder"));
        Page<Category> categories = categoryRepository.findAll(
                CategorySpecification.keywordContains(query).and(CategorySpecification.hasStatus(CategoryStatus.ACTIVE)),
                pageable);
        return toPageResponse(categories);
    }

    /**
     * Search/filter categories for Admin.
     *
     * @param keyword Keyword search
     * @param status Category status filter
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param direction Sort direction
     * @return Page response of categories
     */
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getAdminCategories(
            String keyword,
            CategoryStatus status,
            int page,
            int size,
            String sortBy,
            Sort.Direction direction) {
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "displayOrder";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, safeSortBy));
        Page<Category> categories = categoryRepository.findAll(
                CategorySpecification.keywordContains(keyword).and(CategorySpecification.hasStatus(status)),
                pageable);
        return toPageResponse(categories);
    }

    /**
     * Get category detail.
     *
     * @param id Category ID
     * @return Category response
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(UUID id) {
        return categoryMapper.toCategoryResponse(findCategory(id));
    }

    /**
     * Create category.
     * 
     * @param request Create category request
     * @return Category
     */
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String normalizedName = normalizeRequiredText(request.getName(), "Category name is required");
        String slug = resolveSlug(request.getSlug(), normalizedName);

        if (categoryRepository.existsBySlugIgnoreCase(slug)) {
            throw new ParroTalkException("Category slug already exists", HttpStatus.CONFLICT);
        }

        Category category = Category.builder()
                .name(normalizedName)
                .slug(slug)
                .description(normalizeOptionalText(request.getDescription()))
                .iconName(normalizeOptionalText(request.getIconName()))
                .iconUrl(normalizeOptionalText(request.getIconUrl()))
                .color(normalizeOptionalText(request.getColor()))
                .status(request.getStatus() != null ? request.getStatus() : CategoryStatus.ACTIVE)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : nextDisplayOrder())
                .build();

        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    /**
     * Update category.
     *
     * @param id Category ID
     * @param request Update category request
     * @return Category response
     */
    @Transactional
    public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request) {
        Category category = findCategory(id);
        String normalizedName = normalizeRequiredText(request.getName(), "Category name is required");
        String slug = resolveSlug(request.getSlug(), normalizedName);

        if (categoryRepository.existsBySlugIgnoreCaseAndIdNot(slug, id)) {
            throw new ParroTalkException("Category slug already exists", HttpStatus.CONFLICT);
        }

        category.setName(normalizedName);
        category.setSlug(slug);
        category.setDescription(normalizeOptionalText(request.getDescription()));
        category.setIconName(normalizeOptionalText(request.getIconName()));
        category.setIconUrl(normalizeOptionalText(request.getIconUrl()));
        category.setColor(normalizeOptionalText(request.getColor()));
        category.setStatus(request.getStatus() != null ? request.getStatus() : category.getStatus());
        category.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : category.getDisplayOrder());

        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    /**
     * Soft delete category by deactivating it.
     *
     * @param id Category ID
     */
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = findCategory(id);
        long lessonCount = lessonRepository.countByCategories_Id(id);

        category.setStatus(CategoryStatus.INACTIVE);
        categoryRepository.save(category);

        if (lessonCount == 0) {
            categoryRepository.delete(category);
        }
    }

    /**
     * Update category status.
     *
     * @param id Category ID
     * @param status New status
     * @return Category response
     */
    @Transactional
    public CategoryResponse updateStatus(UUID id, CategoryStatus status) {
        Category category = findCategory(id);
        category.setStatus(status);
        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    /**
     * Reorder categories.
     *
     * @param reorderItems Reorder items
     * @return Updated category responses
     */
    @Transactional
    public List<CategoryResponse> reorderCategories(List<ReorderCategoryItemRequest> reorderItems) {
        return reorderItems.stream()
                .map(item -> {
                    Category category = findCategory(item.getId());
                    category.setDisplayOrder(item.getDisplayOrder());
                    return categoryMapper.toCategoryResponse(categoryRepository.save(category));
                })
                .collect(Collectors.toList());
    }

    private Category findCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ParroTalkException("Category not found", HttpStatus.NOT_FOUND));
    }

    private PageResponse<CategoryResponse> toPageResponse(Page<Category> categories) {
        return PageResponse.<CategoryResponse>builder()
                .content(categories.getContent().stream().map(categoryMapper::toCategoryResponse).collect(Collectors.toList()))
                .page(categories.getNumber())
                .size(categories.getSize())
                .totalPages(categories.getTotalPages())
                .totalElements(categories.getTotalElements())
                .build();
    }

    private int nextDisplayOrder() {
        return categoryRepository.findMaxDisplayOrder() + 1;
    }

    private String normalizeRequiredText(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new ParroTalkException(errorMessage, HttpStatus.BAD_REQUEST);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String resolveSlug(String requestedSlug, String name) {
        String slugSource = StringUtils.hasText(requestedSlug) ? requestedSlug : name;
        String slug = slugify(slugSource);
        if (!StringUtils.hasText(slug)) {
            throw new ParroTalkException("Category slug is invalid", HttpStatus.BAD_REQUEST);
        }
        return slug;
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String ascii = NON_ASCII_PATTERN.matcher(normalized).replaceAll("");
        String slug = NON_SLUG_PATTERN.matcher(ascii.toLowerCase(Locale.ENGLISH)).replaceAll("-");
        return slug.replaceAll("(^-+|-+$)", "");
    }
}
