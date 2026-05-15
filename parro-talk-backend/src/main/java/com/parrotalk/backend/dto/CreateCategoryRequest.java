package com.parrotalk.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Create Category Request DTO.
 * 
 * @author MinhTuMTN
 */
@Getter
@Setter
public class CreateCategoryRequest {

    /** Category name */
    @NotBlank(message = "Category name is required")
    private String name;
}
