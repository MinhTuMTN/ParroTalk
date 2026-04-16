package com.parrotalk.backend.dto;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;

/**
 * Lesson search request.
 * 
 * @author MinhTuMTN
 */
@Data
public class LessonSearchRequest {

    /** Search query */
    private String query;

    /** Page number */
    private int page = 0;

    /** Page size */
    private int size = 10;

    /**
     * Get cache key.
     * 
     * @return Cache key
     */
    public String getCacheKey() {
        return String.join(
            "_",
            StringUtils.defaultString(query),
            String.valueOf(page),
            String.valueOf(size)
        );
    }
}
