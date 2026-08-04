package com.parrotalk.backend.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Utility for pagination bounds and safe page requests.
 *
 * @author MinhTuMTN
 */
public final class PageableUtils {

    /** Maximum page size accepted from clients. */
    public static final int MAX_PAGE_SIZE = 100;

    private PageableUtils() {
    }

    /**
     * Build a page request bounded to a safe size.
     *
     * @param page Requested page index
     * @param size Requested page size
     * @param sort Sort configuration
     * @return Sanitized {@link Pageable}
     */
    public static Pageable createPageRequest(int page, int size, Sort sort) {
        return PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                sort);
    }
}
