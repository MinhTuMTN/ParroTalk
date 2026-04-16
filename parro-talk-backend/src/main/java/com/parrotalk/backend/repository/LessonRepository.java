package com.parrotalk.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.parrotalk.backend.constant.LessonStatus;
import com.parrotalk.backend.entity.Lesson;

/**
 * Lesson repository.
 * 
 * @author MinhTuMTN
 */
@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    /**
     * Find lesson by file hash.
     * 
     * @param fileHash File hash
     * @return Lesson
     */
    Optional<Lesson> findByFileHash(String fileHash);

    List<Lesson> findAllByOwnerId(UUID ownerId);

    Page<Lesson> findAllByStatusAndCreatedAtBefore(LessonStatus status, LocalDateTime createdAt, Pageable pageable);

    List<Lesson> findAllByStatusAndCreatedAtBefore(LessonStatus status, LocalDateTime createdAt);

    @Query("""
                SELECT DISTINCT l
                FROM Lesson l
                WHERE
                    (
                        :query IS NULL
                        OR LOWER(CAST(l.title AS string)) LIKE LOWER(CAST(:query AS string))
                        OR LOWER(CAST(l.description AS string)) LIKE LOWER(CAST(:query AS string))
                    )
            """)
    Page<Lesson> searchLessons(@org.springframework.data.repository.query.Param("query") String query,
            @org.springframework.data.repository.query.Param("categoryId") UUID categoryId, Pageable pageable);

    List<Lesson> findAllByTitleIsNull();

}
