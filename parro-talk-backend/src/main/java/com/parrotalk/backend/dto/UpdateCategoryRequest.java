package com.parrotalk.backend.dto;

import com.parrotalk.backend.constant.CategoryStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Update Category Request DTO.
 */
@Getter
@Setter
public class UpdateCategoryRequest {

    /** Category name. */
    @NotBlank(message = "Category name is required")
    private String name;

    /** SEO/user-friendly slug. */
    private String slug;

    /** Category description. */
    private String description;

    /** Icon name from the frontend icon set. */
    private String iconName;

    /** Icon URL for custom category icon. */
    private String iconUrl;

    /** Category display color. */
    private String color;

    /** Category status. */
    private CategoryStatus status;

    /** Display order. */
    private Integer displayOrder;
}
