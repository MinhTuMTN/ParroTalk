package com.parrotalk.backend.dto.admin;

import com.parrotalk.backend.constant.CmsItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AdminCategoryUpdateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Slug is required")
    private String slug;

    private String description;

    private String icon;

    private String color;

    private String imageUrl;

    private UUID parentCategoryId;

    @NotNull(message = "Sort order is required")
    @Min(value = 0, message = "Sort order must be >= 0")
    private Integer sortOrder;

    @NotNull(message = "Status is required")
    private CmsItemStatus status;
}
