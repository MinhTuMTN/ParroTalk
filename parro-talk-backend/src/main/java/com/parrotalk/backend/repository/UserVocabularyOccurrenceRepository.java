package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.UserVocabularyOccurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserVocabularyOccurrenceRepository extends JpaRepository<UserVocabularyOccurrence, UUID> {

    List<UserVocabularyOccurrence> findByUserVocabularyIdOrderByCreatedAtDesc(UUID userVocabularyId);

    List<UserVocabularyOccurrence> findByLessonIdAndSegmentId(UUID lessonId, UUID segmentId);

    boolean existsByUserVocabularyIdAndLessonIdAndSegmentIdAndWord(UUID userVocabularyId, UUID lessonId, UUID segmentId, String word);
}
