package com.parrotalk.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Youtube Upload Request.
 * 
 * @author MinhTuMTN
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeUploadRequest {
    @NotBlank(message = "Video URL is required")
    private String url;

    @NotBlank(message = "Title is required")
    private String title;
}
