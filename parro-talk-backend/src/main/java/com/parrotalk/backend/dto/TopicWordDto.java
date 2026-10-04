package com.parrotalk.backend.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A word row on the topic detail page.
 * {@code status} is NEW / LEARNING / MASTERED for authenticated users who saved the word,
 * and {@code null} for guests or words the user has not saved.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicWordDto {
    private UUID id;
    private String word;
    private String commonMeaningVi;
    private String partOfSpeech;
    private String phonetic;
    private String cefrLevel;
    private String audioUsUrl;
    private String audioUkUrl;
    private String status;
}
