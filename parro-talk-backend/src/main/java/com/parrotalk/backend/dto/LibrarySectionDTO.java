package com.parrotalk.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

import com.parrotalk.backend.constant.SectionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibrarySectionDTO {
    private UUID id;
    private String title;
    private SectionType type;
    private String categoryId;
    private String categoryName;
    private String categorySlug;
    private String program;
    private List<LessonWithProgressDTO> items;
}
