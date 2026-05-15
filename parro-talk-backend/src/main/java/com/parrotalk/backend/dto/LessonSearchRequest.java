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

    /** Search query alias */
    private String search;

    /** Page number */
    private int page = 0;

    /** Page size */
    private int size = 10;

    /** Page size alias */
    private Integer limit;

    /**
     * Get search query.
     * 
     * @return Search query
     */
    public String getQuery() {
        return StringUtils.isNotBlank(query) ? query : search;
    }

    /**
     * Get page size.
     * 
     * @return Page size
     */
    public int getSize() {
        return limit != null && limit > 0 ? limit : size;
    }

    /**
     * Get cache key.
     * 
     * @return Cache key
     */
    public String getCacheKey() {
        return String.join(
                "_",
                StringUtils.defaultString(getQuery()),
                String.valueOf(page),
                String.valueOf(getSize()));
    }
}
