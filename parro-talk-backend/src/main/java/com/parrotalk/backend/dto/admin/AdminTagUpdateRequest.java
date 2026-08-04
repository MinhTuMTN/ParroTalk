package com.parrotalk.backend.dto.admin;

import com.parrotalk.backend.constant.CmsItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminTagUpdateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Slug is required")
    private String slug;

    private String color;

    private String description;

    @NotNull(message = "Status is required")
    private CmsItemStatus status;
}
