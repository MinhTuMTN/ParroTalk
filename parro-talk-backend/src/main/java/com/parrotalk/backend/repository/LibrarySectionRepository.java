package com.parrotalk.backend.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.parrotalk.backend.entity.LibrarySection;

import java.util.List;
import java.util.UUID;

/**
 * Library Section Repository.
 * 
 * @author MinhTuMTN
 */
public interface LibrarySectionRepository extends JpaRepository<LibrarySection, UUID> {

    /**
     * Find all library sections ordered by their order index in ascending order (Admin).
     */
    @EntityGraph(attributePaths = {"category"})
    List<LibrarySection> findAllByOrderByOrderIndexAsc();

    /**
     * Find all active library sections ordered by their order index in ascending order.
     */
    @EntityGraph(attributePaths = {"category"})
    List<LibrarySection> findByIsActiveTrueOrderByOrderIndexAsc();
}
