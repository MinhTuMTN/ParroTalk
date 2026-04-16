package com.parrotalk.backend.specification;

import com.parrotalk.backend.entity.Lesson;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import com.parrotalk.backend.entity.Category;

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
    public static Specification<Lesson> findLessonsByName(String keyword) {
        return (root, query, criticalBuilder) -> {
            // If keyword is blank, return empty specification
            if (StringUtils.isBlank(keyword)) {
                return criticalBuilder.conjunction();
            }

            // Distinct result
            query.distinct(true);

            // Convert keyword to lowercase and add wildcard
            String pattern = "%" + keyword.toLowerCase() + "%";

            // Join category
            Join<Lesson, Category> categoryJoin = root.join("categories", JoinType.LEFT);

            // Lesson Predicate
            Predicate lessonPredicate = criticalBuilder.like(
                    criticalBuilder.lower(root.get("title")), pattern);

            // Category Predicate
            Predicate categoryPredicate = criticalBuilder.like(
                    criticalBuilder.lower(categoryJoin.get("name")), pattern);

            // Combine predicates with OR operator
            return criticalBuilder.or(lessonPredicate, categoryPredicate);
        };
    }
}
