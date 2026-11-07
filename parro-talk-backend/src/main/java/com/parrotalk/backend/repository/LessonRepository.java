package com.parrotalk.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
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
import com.parrotalk.backend.dto.SectionLessonProjection;
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
     * Find lessons by visibility status ordered by creation date descending with limit.
     * 
     * @param visibilityStatus Visibility status
     * @param limit            Limit
     * @return List of lessons
     */
    List<Lesson> findByVisibilityStatusOrderByCreatedAtDesc(LessonVisibilityStatus visibilityStatus, Limit limit);

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
                COALESCE(ulp.lastProgress, 0) * 100,
                CASE
                    WHEN l.thumbnailUrl IS NULL OR l.thumbnailUrl = '' OR l.thumbnailUrl LIKE '%default-audio-thumbnail%'
                    THEN COALESCE(
                        (SELECT MIN(cat.imageUrl) FROM l.categories cat WHERE cat.imageUrl IS NOT NULL AND cat.imageUrl <> ''),
                        l.thumbnailUrl
                    )
                    ELSE l.thumbnailUrl
                END,
                l.fileUrl,
                l.duration
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
                COALESCE(ulp.lastProgress, 0) * 100,
                CASE
                    WHEN l.thumbnailUrl IS NULL OR l.thumbnailUrl = '' OR l.thumbnailUrl LIKE '%default-audio-thumbnail%'
                    THEN COALESCE(
                        (SELECT MIN(cat.imageUrl) FROM l.categories cat WHERE cat.imageUrl IS NOT NULL AND cat.imageUrl <> ''),
                        l.thumbnailUrl
                    )
                    ELSE l.thumbnailUrl
                END,
                l.fileUrl,
                l.duration
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

    /**
     * Find lessons in progress for user.
     */
    @Query(value = """
            SELECT new com.parrotalk.backend.dto.LessonWithProgressDTO(
                l.id,
                l.title,
                ulp.lastProgress * 100,
                CASE
                    WHEN l.thumbnailUrl IS NULL OR l.thumbnailUrl = '' OR l.thumbnailUrl LIKE '%default-audio-thumbnail%'
                    THEN COALESCE(
                        (SELECT MIN(cat.imageUrl) FROM l.categories cat WHERE cat.imageUrl IS NOT NULL AND cat.imageUrl <> ''),
                        l.thumbnailUrl
                    )
                    ELSE l.thumbnailUrl
                END,
                l.fileUrl,
                l.duration
            )
            FROM UserLessonProgress ulp
            JOIN ulp.lesson l
            WHERE ulp.user.id = :userId
                AND ulp.lastProgress > 0
                AND ulp.lastProgress < 1.0
            ORDER BY ulp.updatedAt DESC
            """)
    List<LessonWithProgressDTO> findContinueLearning(
            @Param("userId") UUID userId,
            Pageable pageable);

    /**
     * Find user lessons currently processing or pending.
     */
    @Query(value = """
            SELECT new com.parrotalk.backend.dto.LessonWithProgressDTO(
                l.id,
                l.title,
                COALESCE(ulp.lastProgress, 0) * 100,
                CASE
                    WHEN l.thumbnailUrl IS NULL OR l.thumbnailUrl = '' OR l.thumbnailUrl LIKE '%default-audio-thumbnail%'
                    THEN COALESCE(
                        (SELECT MIN(cat.imageUrl) FROM l.categories cat WHERE cat.imageUrl IS NOT NULL AND cat.imageUrl <> ''),
                        l.thumbnailUrl
                    )
                    ELSE l.thumbnailUrl
                END,
                l.fileUrl,
                l.duration
            )
            FROM Lesson l
            LEFT JOIN UserLessonProgress ulp
                ON ulp.lesson.id = l.id
                AND ulp.user.id = :userId
            WHERE l.ownerId = :userId
                AND l.status IN (com.parrotalk.backend.constant.LessonStatus.PENDING, com.parrotalk.backend.constant.LessonStatus.PROCESSING)
            ORDER BY l.createdAt DESC
            """)
    List<LessonWithProgressDTO> findUserProcessing(
            @Param("userId") UUID userId,
            Pageable pageable);

    /**
     * Find user uploaded lessons that are done.
     */
    @Query(value = """
            SELECT new com.parrotalk.backend.dto.LessonWithProgressDTO(
                l.id,
                l.title,
                COALESCE(ulp.lastProgress, 0) * 100,
                CASE
                    WHEN l.thumbnailUrl IS NULL OR l.thumbnailUrl = '' OR l.thumbnailUrl LIKE '%default-audio-thumbnail%'
                    THEN COALESCE(
                        (SELECT MIN(cat.imageUrl) FROM l.categories cat WHERE cat.imageUrl IS NOT NULL AND cat.imageUrl <> ''),
                        l.thumbnailUrl
                    )
                    ELSE l.thumbnailUrl
                END,
                l.fileUrl,
                l.duration
            )
            FROM Lesson l
            LEFT JOIN UserLessonProgress ulp
                ON ulp.lesson.id = l.id
                AND ulp.user.id = :userId
            WHERE l.ownerId = :userId
                AND l.status = com.parrotalk.backend.constant.LessonStatus.DONE
            ORDER BY l.createdAt DESC
            """)
    List<LessonWithProgressDTO> findUserUploaded(
            @Param("userId") UUID userId,
            Pageable pageable);

    /**
     * Find published lessons by category.
     */
    @Query(value = """
            SELECT new com.parrotalk.backend.dto.LessonWithProgressDTO(
                l.id,
                l.title,
                COALESCE(ulp.lastProgress, 0) * 100,
                CASE
                    WHEN (l.thumbnailUrl IS NULL OR l.thumbnailUrl = '' OR l.thumbnailUrl LIKE '%default-audio-thumbnail%')
                         AND c.imageUrl IS NOT NULL AND c.imageUrl <> ''
                    THEN c.imageUrl
                    ELSE l.thumbnailUrl
                END,
                l.fileUrl,
                l.duration
            )
            FROM Lesson l
            JOIN l.categories c
            LEFT JOIN UserLessonProgress ulp
                ON ulp.lesson.id = l.id
                AND ulp.user.id = :userId
            WHERE c.id = :categoryId
                AND l.visibilityStatus = com.parrotalk.backend.constant.LessonVisibilityStatus.PUBLISHED
                AND l.status = com.parrotalk.backend.constant.LessonStatus.DONE
            ORDER BY l.createdAt DESC
            """)
    List<LessonWithProgressDTO> findByCategory(
            @Param("categoryId") UUID categoryId,
            @Param("userId") UUID userId,
            Pageable pageable);

    /**
     * Find published lessons by program.
     */
    @Query(value = """
            SELECT new com.parrotalk.backend.dto.LessonWithProgressDTO(
                l.id,
                l.title,
                COALESCE(ulp.lastProgress, 0) * 100,
                CASE
                    WHEN l.thumbnailUrl IS NULL OR l.thumbnailUrl = '' OR l.thumbnailUrl LIKE '%default-audio-thumbnail%'
                    THEN COALESCE(
                        (SELECT MIN(cat.imageUrl) FROM l.categories cat WHERE cat.imageUrl IS NOT NULL AND cat.imageUrl <> ''),
                        l.thumbnailUrl
                    )
                    ELSE l.thumbnailUrl
                END,
                l.fileUrl,
                l.duration
            )
            FROM Lesson l
            LEFT JOIN UserLessonProgress ulp
                ON ulp.lesson.id = l.id
                AND ulp.user.id = :userId
            WHERE l.program = :program
                AND l.visibilityStatus = com.parrotalk.backend.constant.LessonVisibilityStatus.PUBLISHED
                AND l.status = com.parrotalk.backend.constant.LessonStatus.DONE
            ORDER BY l.createdAt DESC
            """)
    List<LessonWithProgressDTO> findByProgram(
            @Param("program") String program,
            @Param("userId") UUID userId,
            Pageable pageable);

    /**
     * Find category lessons by slug with optional program and source filters.
     */
    @Query(value = """
            SELECT new com.parrotalk.backend.dto.LessonWithProgressDTO(
                l.id,
                l.title,
                COALESCE(ulp.lastProgress, 0) * 100,
                CASE
                    WHEN (l.thumbnailUrl IS NULL OR l.thumbnailUrl = '' OR l.thumbnailUrl LIKE '%default-audio-thumbnail%')
                         AND c.imageUrl IS NOT NULL AND c.imageUrl <> ''
                    THEN c.imageUrl
                    ELSE l.thumbnailUrl
                END,
                l.fileUrl,
                l.duration
            )
            FROM Lesson l
            JOIN l.categories c
            LEFT JOIN UserLessonProgress ulp
                ON ulp.lesson.id = l.id
                AND ulp.user.id = :userId
            WHERE c.slug = :categorySlug
                AND l.visibilityStatus = com.parrotalk.backend.constant.LessonVisibilityStatus.PUBLISHED
                AND l.status = com.parrotalk.backend.constant.LessonStatus.DONE
                AND (:program IS NULL OR :program = '' OR l.program = :program)
                AND (:source IS NULL OR :source = '' OR l.source = :source)
            """, countQuery = """
            SELECT COUNT(l.id)
            FROM Lesson l
            JOIN l.categories c
            WHERE c.slug = :categorySlug
                AND l.visibilityStatus = com.parrotalk.backend.constant.LessonVisibilityStatus.PUBLISHED
                AND l.status = com.parrotalk.backend.constant.LessonStatus.DONE
                AND (:program IS NULL OR :program = '' OR l.program = :program)
                AND (:source IS NULL OR :source = '' OR l.source = :source)
            """)
    Page<LessonWithProgressDTO> findCategoryLessonsBySlug(
            @Param("categorySlug") String categorySlug,
            @Param("program") String program,
            @Param("source") String source,
            @Param("userId") UUID userId,
            Pageable pageable);

    /**
     * Search public lessons with optional filters.
     */
    @Query(value = """
            SELECT new com.parrotalk.backend.dto.LessonWithProgressDTO(
                l.id,
                l.title,
                COALESCE(ulp.lastProgress, 0) * 100,
                CASE
                    WHEN l.thumbnailUrl IS NULL OR l.thumbnailUrl = '' OR l.thumbnailUrl LIKE '%default-audio-thumbnail%'
                    THEN COALESCE(
                        (SELECT MIN(cat.imageUrl) FROM l.categories cat WHERE (:category IS NOT NULL AND :category <> '') AND (cat.slug = :category OR cat.name = :category) AND cat.imageUrl IS NOT NULL AND cat.imageUrl <> ''),
                        (SELECT MIN(cat.imageUrl) FROM l.categories cat WHERE cat.imageUrl IS NOT NULL AND cat.imageUrl <> ''),
                        l.thumbnailUrl
                    )
                    ELSE l.thumbnailUrl
                END,
                l.fileUrl,
                l.duration
            )
            FROM Lesson l
            LEFT JOIN UserLessonProgress ulp
                ON ulp.lesson.id = l.id
                AND ulp.user.id = :userId
            WHERE l.visibilityStatus = com.parrotalk.backend.constant.LessonVisibilityStatus.PUBLISHED
                AND l.status = com.parrotalk.backend.constant.LessonStatus.DONE
                AND (:query IS NULL OR :query = '' OR LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')))
                AND (:category IS NULL OR :category = '' OR EXISTS (SELECT 1 FROM l.categories c WHERE c.slug = :category OR c.name = :category))
                AND (:program IS NULL OR :program = '' OR l.program = :program)
                AND (:source IS NULL OR :source = '' OR l.source = :source)
            """, countQuery = """
            SELECT COUNT(l.id)
            FROM Lesson l
            WHERE l.visibilityStatus = com.parrotalk.backend.constant.LessonVisibilityStatus.PUBLISHED
                AND l.status = com.parrotalk.backend.constant.LessonStatus.DONE
                AND (:query IS NULL OR :query = '' OR LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')))
                AND (:category IS NULL OR :category = '' OR EXISTS (SELECT 1 FROM l.categories c WHERE c.slug = :category OR c.name = :category))
                AND (:program IS NULL OR :program = '' OR l.program = :program)
                AND (:source IS NULL OR :source = '' OR l.source = :source)
            """)
    Page<LessonWithProgressDTO> searchPublicLessons(
            @Param("query") String query,
            @Param("category") String category,
            @Param("program") String program,
            @Param("source") String source,
            @Param("userId") UUID userId,
            Pageable pageable);

    /**
     * Batch query lessons for multiple categories without user progress (for guests).
     */
    @Query(value = """
            WITH ranked_category_lessons AS (
                SELECT 
                    CAST(c.category_id AS VARCHAR) AS section_key,
                    CAST(l.id AS VARCHAR) AS id,
                    l.title AS title,
                    0.0 AS progress,
                    CASE
                        WHEN (l.thumbnail_url IS NULL OR l.thumbnail_url = '' OR l.thumbnail_url LIKE '%default-audio-thumbnail%')
                             AND lc.image_url IS NOT NULL AND lc.image_url <> ''
                        THEN lc.image_url
                        ELSE l.thumbnail_url
                    END AS thumbnail_url,
                    l.file_url AS file_url,
                    l.duration AS duration,
                    ROW_NUMBER() OVER (PARTITION BY c.category_id ORDER BY l.created_at DESC) AS rank
                FROM lessons l
                JOIN lesson_category_lesson c ON l.id = c.lesson_id
                LEFT JOIN lesson_categories lc ON lc.id = c.category_id AND lc.is_deleted = false
                WHERE c.category_id IN (:categoryIds)
                  AND l.visibility_status = 'PUBLISHED'
                  AND l.status = 'DONE'
                  AND l.is_deleted = false
            )
            SELECT section_key, id, title, progress, thumbnail_url, file_url, duration
            FROM ranked_category_lessons
            WHERE rank <= :maxItems
            ORDER BY rank ASC
            """, nativeQuery = true)
    List<SectionLessonProjection> findBatchCategoryLessons(
            @Param("categoryIds") List<UUID> categoryIds,
            @Param("maxItems") int maxItems);

    /**
     * Batch query lessons for multiple programs without user progress (for guests).
     */
    @Query(value = """
            WITH ranked_program_lessons AS (
                SELECT 
                    l.program AS section_key,
                    CAST(l.id AS VARCHAR) AS id,
                    l.title AS title,
                    0.0 AS progress,
                    CASE
                        WHEN l.thumbnail_url IS NULL OR l.thumbnail_url = '' OR l.thumbnail_url LIKE '%default-audio-thumbnail%'
                        THEN COALESCE(
                            (SELECT lc.image_url
                             FROM lesson_category_lesson lcl
                             JOIN lesson_categories lc ON lc.id = lcl.category_id
                             WHERE lcl.lesson_id = l.id
                               AND lc.is_deleted = false
                               AND lc.image_url IS NOT NULL
                               AND lc.image_url <> ''
                             ORDER BY lc.sort_order ASC, lc.name ASC
                             LIMIT 1),
                            l.thumbnail_url
                        )
                        ELSE l.thumbnail_url
                    END AS thumbnail_url,
                    l.file_url AS file_url,
                    l.duration AS duration,
                    ROW_NUMBER() OVER (PARTITION BY l.program ORDER BY l.created_at DESC) AS rank
                FROM lessons l
                WHERE l.program IN (:programs)
                  AND l.visibility_status = 'PUBLISHED'
                  AND l.status = 'DONE'
                  AND l.is_deleted = false
            )
            SELECT section_key, id, title, progress, thumbnail_url, file_url, duration
            FROM ranked_program_lessons
            WHERE rank <= :maxItems
            ORDER BY rank ASC
            """, nativeQuery = true)
    List<SectionLessonProjection> findBatchProgramLessons(
            @Param("programs") List<String> programs,
            @Param("maxItems") int maxItems);

    /**
     * Batch query user-specific lessons (IN_PROGRESS).
     */
    @Query(value = """
            WITH in_progress_lessons AS (
                SELECT 
                    'IN_PROGRESS' AS section_key,
                    CAST(l.id AS VARCHAR) AS id,
                    l.title AS title,
                    ulp.last_progress * 100.0 AS progress,
                    CASE
                        WHEN l.thumbnail_url IS NULL OR l.thumbnail_url = '' OR l.thumbnail_url LIKE '%default-audio-thumbnail%'
                        THEN COALESCE(
                            (SELECT lc.image_url
                             FROM lesson_category_lesson lcl
                             JOIN lesson_categories lc ON lc.id = lcl.category_id
                             WHERE lcl.lesson_id = l.id
                               AND lc.is_deleted = false
                               AND lc.image_url IS NOT NULL
                               AND lc.image_url <> ''
                             ORDER BY lc.sort_order ASC, lc.name ASC
                             LIMIT 1),
                            l.thumbnail_url
                        )
                        ELSE l.thumbnail_url
                    END AS thumbnail_url,
                    l.file_url AS file_url,
                    l.duration AS duration,
                    ROW_NUMBER() OVER (ORDER BY ulp.updated_at DESC) AS rank
                FROM user_lesson_progress ulp
                JOIN lessons l ON ulp.lesson_id = l.id
                WHERE ulp.user_id = :userId
                  AND ulp.last_progress > 0 
                  AND ulp.last_progress < 1.0
                  AND l.is_deleted = false
            ),
            user_processing AS (
                SELECT 
                    'USER_PROCESSING' AS section_key,
                    CAST(l.id AS VARCHAR) AS id,
                    l.title AS title,
                    COALESCE(ulp.last_progress, 0) * 100.0 AS progress,
                    CASE
                        WHEN l.thumbnail_url IS NULL OR l.thumbnail_url = '' OR l.thumbnail_url LIKE '%default-audio-thumbnail%'
                        THEN COALESCE(
                            (SELECT lc.image_url
                             FROM lesson_category_lesson lcl
                             JOIN lesson_categories lc ON lc.id = lcl.category_id
                             WHERE lcl.lesson_id = l.id
                               AND lc.is_deleted = false
                               AND lc.image_url IS NOT NULL
                               AND lc.image_url <> ''
                             ORDER BY lc.sort_order ASC, lc.name ASC
                             LIMIT 1),
                            l.thumbnail_url
                        )
                        ELSE l.thumbnail_url
                    END AS thumbnail_url,
                    l.file_url AS file_url,
                    l.duration AS duration,
                    ROW_NUMBER() OVER (ORDER BY l.created_at DESC) AS rank
                FROM lessons l
                LEFT JOIN user_lesson_progress ulp ON ulp.lesson_id = l.id AND ulp.user_id = :userId
                WHERE l.owner_id = :userId
                  AND l.status IN ('PENDING', 'PROCESSING')
                  AND l.is_deleted = false
            ),
            user_uploaded AS (
                SELECT 
                    'USER_UPLOADED' AS section_key,
                    CAST(l.id AS VARCHAR) AS id,
                    l.title AS title,
                    COALESCE(ulp.last_progress, 0) * 100.0 AS progress,
                    CASE
                        WHEN l.thumbnail_url IS NULL OR l.thumbnail_url = '' OR l.thumbnail_url LIKE '%default-audio-thumbnail%'
                        THEN COALESCE(
                            (SELECT lc.image_url
                             FROM lesson_category_lesson lcl
                             JOIN lesson_categories lc ON lc.id = lcl.category_id
                             WHERE lcl.lesson_id = l.id
                               AND lc.is_deleted = false
                               AND lc.image_url IS NOT NULL
                               AND lc.image_url <> ''
                             ORDER BY lc.sort_order ASC, lc.name ASC
                             LIMIT 1),
                            l.thumbnail_url
                        )
                        ELSE l.thumbnail_url
                    END AS thumbnail_url,
                    l.file_url AS file_url,
                    l.duration AS duration,
                    ROW_NUMBER() OVER (ORDER BY l.created_at DESC) AS rank
                FROM lessons l
                LEFT JOIN user_lesson_progress ulp ON ulp.lesson_id = l.id AND ulp.user_id = :userId
                WHERE l.owner_id = :userId
                  AND l.status = 'DONE'
                  AND l.is_deleted = false
            )
            SELECT section_key, id, title, progress, thumbnail_url, file_url, duration FROM in_progress_lessons WHERE rank <= :maxItems
            UNION ALL
            SELECT section_key, id, title, progress, thumbnail_url, file_url, duration FROM user_processing WHERE rank <= :maxItems
            UNION ALL
            SELECT section_key, id, title, progress, thumbnail_url, file_url, duration FROM user_uploaded WHERE rank <= :maxItems
            """, nativeQuery = true)
    List<SectionLessonProjection> findBatchUserLessons(
            @Param("userId") UUID userId,
            @Param("maxItems") int maxItems);
}
