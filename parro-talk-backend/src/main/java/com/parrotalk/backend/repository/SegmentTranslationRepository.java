package com.parrotalk.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.parrotalk.backend.entity.SegmentTranslation;

@Repository
public interface SegmentTranslationRepository extends JpaRepository<SegmentTranslation, UUID> {

    interface SegmentTranslationProjection {
        UUID getSegmentId();

        String getTargetLanguage();

        String getTranslatedText();
    }

    List<SegmentTranslation> findBySegmentIdInAndTargetLanguage(Collection<UUID> segmentIds, String targetLanguage);

    /**
     * Find lightweight translation projections for a collection of segment IDs and target language.
     *
     * @param segmentIds     Segment IDs
     * @param targetLanguage Target language code
     * @return List of segment translation projections
     */
    @Query("""
            SELECT st.segment.id AS segmentId,
                   st.targetLanguage AS targetLanguage,
                   st.translatedText AS translatedText
            FROM SegmentTranslation st
            WHERE st.segment.id IN :segmentIds
              AND st.targetLanguage = :targetLanguage
            """)
    List<SegmentTranslationProjection> findTranslationsBySegmentIdsAndTargetLanguage(
            @Param("segmentIds") Collection<UUID> segmentIds,
            @Param("targetLanguage") String targetLanguage);

    Optional<SegmentTranslation> findBySegmentIdAndTargetLanguage(UUID segmentId, String targetLanguage);

    void deleteBySegmentIdInAndTargetLanguage(Collection<UUID> segmentIds, String targetLanguage);

    void deleteBySegmentLessonId(UUID lessonId);

    long countBySegmentLessonIdAndTargetLanguage(UUID lessonId, String targetLanguage);
}
