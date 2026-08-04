package com.parrotalk.backend.repository;

import com.parrotalk.backend.constant.CmsItemStatus;
import com.parrotalk.backend.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    Optional<Category> findBySlug(String slug);

    List<Category> findByParentCategoryId(UUID parentId);

    /**
     * Find all categories whose path starts with the given prefix.
     * Useful for fetching a subtree.
     */
    List<Category> findByPathStartingWith(String pathPrefix);

    @Query("SELECT COUNT(l) FROM Category c JOIN c.lessons l WHERE c.id = :categoryId")
    long countLessonsByCategoryId(@Param("categoryId") UUID categoryId);

    Optional<Category> findByNameIgnoreCase(String name);

    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
