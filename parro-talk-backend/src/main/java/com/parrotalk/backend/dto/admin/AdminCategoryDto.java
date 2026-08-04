package com.parrotalk.backend.dto.admin;

import com.parrotalk.backend.constant.CmsItemStatus;
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
public class AdminCategoryDto {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String icon;
    private String color;
    private String imageUrl;
    private UUID parentCategoryId;
    private Integer sortOrder;
    private CmsItemStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private long lessonsCount;
    private java.util.List<AdminCategoryDto> children;
}
