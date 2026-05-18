package com.parrotalk.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Reorder categories request DTO.
 */
@Getter
@Setter
public class ReorderCategoriesRequest {

    /** Categories to reorder. */
    @Valid
    @NotEmpty(message = "Categories are required")
    private List<ReorderCategoryItemRequest> categories;
}
