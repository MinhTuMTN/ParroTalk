package com.parrotalk.backend.dto.admin;

import java.util.UUID;

import com.parrotalk.backend.constant.SectionType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLibrarySectionRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Section type is required")
    private SectionType type;

    private UUID categoryId;

    private String program;

    private Integer orderIndex;

    private Boolean isActive;

    @Min(value = 1, message = "Max items must be at least 1")
    private Integer maxItems;
}
