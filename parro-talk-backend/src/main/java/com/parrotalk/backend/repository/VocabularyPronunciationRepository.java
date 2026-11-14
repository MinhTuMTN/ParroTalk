package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.VocabularyPronunciation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VocabularyPronunciationRepository extends JpaRepository<VocabularyPronunciation, UUID> {

    @Query("""
            SELECT p FROM VocabularyPronunciation p
            JOIN FETCH p.vocabulary v
            WHERE p.audioUrl IS NULL
               OR TRIM(p.audioUrl) = ''
               OR LOWER(p.audioUrl) LIKE '%dictionary.cambridge.org%'
            ORDER BY p.id ASC
            """)
    List<VocabularyPronunciation> findPronunciationsNeedingTts(Pageable pageable);

    @Query("""
            SELECT COUNT(p) FROM VocabularyPronunciation p
            WHERE p.audioUrl IS NULL
               OR TRIM(p.audioUrl) = ''
               OR LOWER(p.audioUrl) LIKE '%dictionary.cambridge.org%'
            """)
    long countPronunciationsNeedingTts();

    @Modifying
    @Query("UPDATE VocabularyPronunciation p SET p.audioUrl = :audioUrl WHERE p.id = :id")
    int updateAudioUrlById(@Param("id") UUID id, @Param("audioUrl") String audioUrl);
}
