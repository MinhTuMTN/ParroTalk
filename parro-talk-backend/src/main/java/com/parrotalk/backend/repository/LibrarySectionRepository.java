package com.parrotalk.backend.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parrotalk.backend.entity.LibrarySection;

import java.util.List;
import java.util.UUID;

@Repository
public interface LibrarySectionRepository extends JpaRepository<LibrarySection, UUID> {
    
    @EntityGraph(attributePaths = {"category"})
    List<LibrarySection> findAllByOrderByOrderIndexAsc();

    @EntityGraph(attributePaths = {"category"})
    List<LibrarySection> findByIsActiveTrueOrderByOrderIndexAsc();
}
