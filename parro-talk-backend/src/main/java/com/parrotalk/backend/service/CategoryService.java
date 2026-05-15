package com.parrotalk.backend.service;

import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.parrotalk.backend.dto.CategoryResponse;
import com.parrotalk.backend.dto.CreateCategoryRequest;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.mapper.CategoryMapper;
import com.parrotalk.backend.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

/**
 * Category Service.
 * 
 * @author MinhTuMTN
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    /** Category repository */
    private final CategoryRepository categoryRepository;

    /** Category mapper */
    private final CategoryMapper categoryMapper;

    /**
     * Search categories by name.
     * 
     * @param query Query
     * @param page  Page number
     * @param size  Page size
     * @return Page response of categories
     */
    public PageResponse<CategoryResponse> searchCategories(
        String query,
        int page,
        int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Category> categories = categoryRepository.findByNameContainingIgnoreCase(query, pageable);
        return PageResponse.<CategoryResponse>builder()
            .content(categories.getContent().stream().map(categoryMapper::toCategoryResponse).collect(Collectors.toList()))
            .page(categories.getNumber())
            .size(categories.getSize())
            .totalPages(categories.getTotalPages())
            .totalElements(categories.getTotalElements())
            .build();
    }

    /**
     * Create category.
     * 
     * @param request Create category request
     * @return Category
     */
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String normalizedName = request.getName().trim();

        // Check if category already exists
        Category category = categoryRepository.findByNameIgnoreCase(normalizedName)
                .orElseGet(() -> {
                    return categoryRepository.save(Category.builder().name(normalizedName).build());
                });

        return categoryMapper.toCategoryResponse(category);
    }
}
