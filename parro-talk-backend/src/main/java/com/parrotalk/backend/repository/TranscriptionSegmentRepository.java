package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.TranscriptionSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TranscriptionSegmentRepository extends JpaRepository<TranscriptionSegment, UUID> {

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
