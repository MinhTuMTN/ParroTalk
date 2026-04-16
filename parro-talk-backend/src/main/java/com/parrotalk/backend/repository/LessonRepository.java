package com.parrotalk.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.parrotalk.backend.constant.LessonStatus;
import com.parrotalk.backend.entity.Lesson;

/**
 * Lesson repository.
 * 
 * @author MinhTuMTN
 */
@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID>, JpaSpecificationExecutor<Lesson> {

    /**
     * Find lesson by file hash.
     * 
     * @param fileHash File hash
     * @return Lesson
     */
    Optional<Lesson> findByFileHash(String fileHash);

    /**
     * Find all lessons by owner ID.
     * 
     * @param ownerId Owner ID
     * @return List of lessons
     */
    List<Lesson> findAllByOwnerId(UUID ownerId);

    Page<Lesson> findAllByStatusAndCreatedAtBefore(LessonStatus status, LocalDateTime createdAt, Pageable pageable);

    List<Lesson> findAllByStatusAndCreatedAtBefore(LessonStatus status, LocalDateTime createdAt);

    List<Lesson> findAllByTitleIsNull();

    /**
     * Search lessons with specification.
     * 
     * @param specification Lesson Specification
     * @param pageable      Pageable
     * @return Page of lessons
     */
    @Override
    @EntityGraph(attributePaths = { "categories" })
    Page<Lesson> findAll(Specification<Lesson> spec, Pageable pageable);
}
