package com.parrotalk.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;

import com.parrotalk.backend.entity.LessonStatus;
import java.time.LocalDateTime;
import java.util.UUID;

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
    private String youtubeUrl;
    private String mediaType;
    private String sourceType;
    private LocalDateTime createdAt;
}
