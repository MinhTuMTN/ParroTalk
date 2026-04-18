package com.parrotalk.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.parrotalk.backend.constant.LessonStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonResponse {
    private UUID id;
    private LessonStatus status;
    private int progress;
    private String currentStep;
    private String fileUrl;
    private String mediaType;
    private String sourceType;
    private LocalDateTime createdAt;
    private String title;
    private String description;
    private Integer duration;
    private Set<CategoryResponse> categories;
    private List<TranscriptionResponse> segments;
}
