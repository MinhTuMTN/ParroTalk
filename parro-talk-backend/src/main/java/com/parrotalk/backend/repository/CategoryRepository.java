package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.Category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Category Repository.
 * 
 * @author MinhTuMTN
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {

    /**
     * Find category by name ignore case.
     * 
     * @param name Category name
     * @return Optional of Category
     */
    Optional<Category> findByNameIgnoreCase(String name);

    /**
     * Find categories by name containing ignore case.
     * 
     * @param name     Category name
     * @param pageable Pageable
     * @return Page of Category
     */
    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find category by slug ignore case.
     *
     * @param slug Category slug
     * @return Optional of Category
     */
    Optional<Category> findBySlugIgnoreCase(String slug);

    /**
     * Check if slug exists.
     *
     * @param slug Category slug
     * @return true if slug exists
     */
    boolean existsBySlugIgnoreCase(String slug);

    /**
     * Check if slug exists on another category.
     *
     * @param slug Category slug
     * @param id Current category id
     * @return true if slug exists on another category
     */
    boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);

    /**
     * Get the greatest display order.
     *
     * @return Maximum display order
     */
    @Query("SELECT COALESCE(MAX(c.displayOrder), 0) FROM Category c")
    Integer findMaxDisplayOrder();
}
