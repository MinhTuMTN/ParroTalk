package com.parrotalk.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parrotalk.backend.entity.SegmentTranslation;

@Repository
public interface SegmentTranslationRepository extends JpaRepository<SegmentTranslation, UUID> {

    List<SegmentTranslation> findBySegmentIdInAndTargetLanguage(Collection<UUID> segmentIds, String targetLanguage);

    Optional<SegmentTranslation> findBySegmentIdAndTargetLanguage(UUID segmentId, String targetLanguage);

    void deleteBySegmentIdInAndTargetLanguage(Collection<UUID> segmentIds, String targetLanguage);

    void deleteBySegmentLessonId(UUID lessonId);

    long countBySegmentLessonIdAndTargetLanguage(UUID lessonId, String targetLanguage);
}
