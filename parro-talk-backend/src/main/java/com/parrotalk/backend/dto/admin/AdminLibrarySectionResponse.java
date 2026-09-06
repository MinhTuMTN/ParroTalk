package com.parrotalk.backend.dto.admin;

import java.time.LocalDateTime;
import java.util.UUID;

import com.parrotalk.backend.constant.SectionType;

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
public class AdminLibrarySectionResponse {

    private UUID id;
    private String title;
    private SectionType type;
    private UUID categoryId;
    private String categoryName;
    private String categorySlug;
    private String program;
    private Integer orderIndex;
    private Boolean isActive;
    private Integer maxItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
