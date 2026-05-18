package com.parrotalk.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Single category reorder item request DTO.
 */
@Getter
@Setter
public class ReorderCategoryItemRequest {

    /** Category ID. */
    @NotNull(message = "Category id is required")
    private UUID id;

    /** New display order. */
    @NotNull(message = "Display order is required")
    @Min(value = 0, message = "Display order must be greater than or equal to 0")
    private Integer displayOrder;
}
