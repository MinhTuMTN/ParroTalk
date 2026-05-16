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
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import com.parrotalk.backend.constant.LessonStatus;
import com.parrotalk.backend.constant.LessonVisibilityStatus;
import com.parrotalk.backend.dto.LessonWithProgressDTO;
import com.parrotalk.backend.entity.Lesson;

import jakarta.persistence.LockModeType;

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
    Optional<Lesson> findByFileHashAndOwnerId(String fileHash, UUID ownerId);

    /**
     * Find lesson by file hash and owner id is null.
     * 
     * @param fileHash File hash
     * @return Lesson
     */
    Optional<Lesson> findByFileHashAndOwnerIdIsNull(String fileHash);

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
     * Find all lessons by visibility status.
     * 
     * @param visibilityStatus Visibility status
     * @param pageable         Pageable
     * @return Page of lessons
     */
    Page<Lesson> findAllByVisibilityStatus(LessonVisibilityStatus visibilityStatus, Pageable pageable);

    /**
     * Find lesson by id and visibility status.
     * 
     * @param id               Lesson id
     * @param visibilityStatus Visibility status
     * @return Lesson
     */
    Optional<Lesson> findByIdAndVisibilityStatus(UUID id, LessonVisibilityStatus visibilityStatus);

    /**
     * Search lessons with user progress.
     * 
     * @param query    Query string
     * @param userId   User ID
     * @param pageable Pageable
     * @return Page of lessons with progress
     */
    @Query(value = """
            SELECT new com.parrotalk.backend.dto.LessonWithProgressDTO(
                l.id,
                l.title,
                COALESCE(ulp.lastProgress, 0) * 100
            )
            FROM Lesson l
            LEFT JOIN UserLessonProgress ulp
                ON ulp.lesson.id = l.id
                AND ulp.user.id = :userId
            WHERE l.visibilityStatus = com.parrotalk.backend.constant.LessonVisibilityStatus.PUBLISHED
                AND (:query IS NULL OR :query = '' OR LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')))
            """, countQuery = """
            SELECT COUNT(l.id)
            FROM Lesson l
            WHERE l.visibilityStatus = com.parrotalk.backend.constant.LessonVisibilityStatus.PUBLISHED
                AND (:query IS NULL OR :query = '' OR LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<LessonWithProgressDTO> searchPublishedLessonsWithProgress(
            @Param("query") String query,
            @Param("userId") UUID userId,
            Pageable pageable);

    /**
     * Search own lessons with user progress.
     * 
     * @param query    Query string
     * @param userId   User ID
     * @param pageable Pageable
     * @return Page of lessons with progress
     */
    @Query(value = """
            SELECT new com.parrotalk.backend.dto.LessonWithProgressDTO(
                l.id,
                l.title,
                COALESCE(ulp.lastProgress, 0) * 100
            )
            FROM Lesson l
            LEFT JOIN UserLessonProgress ulp
                ON ulp.lesson.id = l.id
                AND ulp.user.id = :userId
            WHERE l.ownerId = :userId
                AND l.visibilityStatus = com.parrotalk.backend.constant.LessonVisibilityStatus.HIDDEN
                AND (:query IS NULL OR :query = '' OR LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')))
            """, countQuery = """
            SELECT COUNT(l.id)
            FROM Lesson l
            WHERE l.ownerId = :userId
                AND l.visibilityStatus = com.parrotalk.backend.constant.LessonVisibilityStatus.HIDDEN
                AND (:query IS NULL OR :query = '' OR LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<LessonWithProgressDTO> searchOwnedHiddenLessonsWithProgress(
            @Param("query") String query,
            @Param("userId") UUID userId,
            Pageable pageable);

    /**
     * Search lessons with specification.
     * 
     * @param specification Lesson Specification
     * @param pageable      Pageable
     * @return Page of lessons
     */
    @Override
    Page<Lesson> findAll(Specification<Lesson> spec, Pageable pageable);

    /**
     * Find lesson by id.
     * 
     * @param lessonId Lesson id
     * @return Lesson
     */
    @Override
    @EntityGraph(attributePaths = { "segments", "categories" })
    Optional<Lesson> findById(UUID lessonId);

    /**
     * Find lesson by id for update.
     * 
     * @param lessonId Lesson id
     * @return Lesson
     */
    @Query("SELECT l FROM Lesson l WHERE l.id = :lessonId")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Lesson> findByIdForUpdate(UUID lessonId);

    Optional<Lesson> findByIdAndOwnerId(UUID id, UUID ownerId);
}
