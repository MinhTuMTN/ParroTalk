package com.parrotalk.backend.dto;

import com.parrotalk.backend.constant.CategoryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Update Category status request DTO.
 */
@Getter
@Setter
public class UpdateCategoryStatusRequest {

    /** Category status. */
    @NotNull(message = "Status is required")
    private CategoryStatus status;
}
