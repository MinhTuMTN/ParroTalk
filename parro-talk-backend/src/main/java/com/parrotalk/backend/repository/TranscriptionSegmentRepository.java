package com.parrotalk.backend.repository;

import com.parrotalk.backend.dto.SegmentSubmissionProjection;
import com.parrotalk.backend.entity.TranscriptionSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TranscriptionSegmentRepository extends JpaRepository<TranscriptionSegment, UUID> {

    /**
     * Find lightweight segment and lesson metadata when the segment belongs to an active lesson.
     *
     * @param segmentId Segment id
     * @param lessonId  Lesson id
     * @return Optional segment submission projection
     */
    @Query("""
            SELECT ts.id AS id,
                   ts.text AS text,
                   ts.blanksJson AS blanksJson,
                   l.totalSegments AS lessonTotalSegments
            FROM TranscriptionSegment ts
            JOIN ts.lesson l
            WHERE ts.id = :segmentId
              AND l.id = :lessonId
            """)
    Optional<SegmentSubmissionProjection> findSubmissionTarget(
            @Param("segmentId") UUID segmentId,
            @Param("lessonId") UUID lessonId);

    /**
     * Count active transcription segments belonging to a lesson.
     *
     * @param lessonId Lesson id
     * @return Segment count
     */
    long countByLessonId(UUID lessonId);

    /**
     * Find active transcription segment IDs belonging to an active lesson.
     *
     * @param lessonId Lesson id
     * @return List of segment ids
     */
    @Query("SELECT ts.id FROM TranscriptionSegment ts JOIN ts.lesson l WHERE l.id = :lessonId")
    List<UUID> findIdsByLessonId(@Param("lessonId") UUID lessonId);

    /**
     * Find transcription segments by lesson id.
     * 
     * @param lessonId Lesson id
     * @return List of transcription segments
     */
    List<TranscriptionSegment> findByLessonId(UUID lessonId);

    /**
     * Find transcription segments by lesson id and order by display order
     * ascending.
     * 
     * @param lessonId Lesson id
     * @return List of transcription segments
     */
    List<TranscriptionSegment> findByLessonIdOrderByDisplayOrderAsc(UUID lessonId);

    /**
     * Delete transcription segments by lesson id.
     * 
     * @param lessonId Lesson id
     */
    void deleteByLessonId(UUID lessonId);
}
