package com.parrotalk.backend.specification;

import com.parrotalk.backend.constant.CategoryStatus;
import com.parrotalk.backend.entity.Category;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Category specifications for admin search/filtering.
 */
public final class CategorySpecification {

    private CategorySpecification() {
    }

    /**
     * Search by keyword in name, slug, or description.
     *
     * @param keyword Search keyword
     * @return Category specification
     */
    public static Specification<Category> keywordContains(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }

            String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likeKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("slug")), likeKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likeKeyword));
        };
    }

    /**
     * Filter by category status.
     *
     * @param status Category status
     * @return Category specification
     */
    public static Specification<Category> hasStatus(CategoryStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(root.get("status"), status);
        };
    }
}
