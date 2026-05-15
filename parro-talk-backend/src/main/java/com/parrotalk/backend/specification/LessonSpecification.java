package com.parrotalk.backend.specification;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import com.parrotalk.backend.entity.Lesson;

/**
 * Lesson Specification.
 * 
 * @author MinhTuMTN
 */
public class LessonSpecification {

    /**
     * Get specification for finding lessons by name.
     *
     * @param keyword Keyword
     * @return Specification
     */
    public static Specification<Lesson> hasTitleLike(String keyword) {
        return (root, query, cb) -> {
            // If keyword is blank, return empty specification
            if (StringUtils.isBlank(keyword)) {
                return cb.conjunction();
            }

            // Convert keyword to lowercase and add wildcard
            String pattern = "%" + keyword.toLowerCase() + "%";

            return cb.like(cb.lower(root.get("title")), pattern);
        };
    }
}
