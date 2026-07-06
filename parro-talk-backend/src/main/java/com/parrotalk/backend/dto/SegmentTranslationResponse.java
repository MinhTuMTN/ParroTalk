package com.parrotalk.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Translation exposed to API clients for one segment.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SegmentTranslationResponse {
        private String targetLanguage;
        private String translatedText;
}
