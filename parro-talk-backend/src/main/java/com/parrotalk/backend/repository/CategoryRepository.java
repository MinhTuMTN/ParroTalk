package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.Category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Category Repository.
 * 
 * @author MinhTuMTN
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

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
}

