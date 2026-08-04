package com.parrotalk.backend.dto.admin;

import com.parrotalk.backend.constant.CmsItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminTagCreateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String slug; // If empty, auto-generate in service

    private String color;

    private String description;

    @NotNull(message = "Status is required")
    private CmsItemStatus status;
}
