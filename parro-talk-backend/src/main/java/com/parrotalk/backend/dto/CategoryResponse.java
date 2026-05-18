package com.parrotalk.backend.dto;

import com.parrotalk.backend.constant.CategoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String iconName;
    private String iconUrl;
    private String thumbnailUrl;
    private String color;
    private CategoryStatus status;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
