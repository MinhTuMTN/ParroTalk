package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.Vocabulary;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VocabularyRepository extends JpaRepository<Vocabulary, UUID>, JpaSpecificationExecutor<Vocabulary> {
    
    Optional<Vocabulary> findByNormalizedWord(String normalizedWord);
    List<Vocabulary> findByNormalizedWordIn(List<String> normalizedWords);

    @Query(value = "SELECT word FROM vocabularies ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<String> findRandomWords(@Param("limit") int limit);

    @EntityGraph(attributePaths = {"senses"})
    @Query("SELECT v FROM Vocabulary v WHERE v.id = :id")
    Optional<Vocabulary> findByIdWithGraph(@Param("id") UUID id);

    @Query("SELECT DISTINCT v FROM Vocabulary v LEFT JOIN v.senses s LEFT JOIN s.topic t WHERE " +
           "(:keyword IS NULL OR LOWER(v.normalizedWord) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:topic IS NULL OR LOWER(t.name) = LOWER(:topic)) AND " +
           "(:cefrLevel IS NULL OR s.cefrLevel = :cefrLevel) AND " +
           "(:partOfSpeech IS NULL OR s.partOfSpeech = :partOfSpeech)")
    org.springframework.data.domain.Page<Vocabulary> searchVocabularies(
            @Param("keyword") String keyword,
            @Param("topic") String topic,
            @Param("cefrLevel") String cefrLevel,
            @Param("partOfSpeech") String partOfSpeech,
            org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT DISTINCT v.* FROM vocabularies v " +
                   "JOIN vocabulary_senses s ON s.vocabulary_id = v.id " +
                   "LEFT JOIN vocabulary_topics t ON s.topic_id = t.id " +
                   "WHERE (:topic IS NULL OR LOWER(t.name) = LOWER(:topic)) AND " +
                   "(:cefrLevel IS NULL OR s.cefr_level = :cefrLevel) " +
                   "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Vocabulary> findRandomVocabularies(@Param("topic") String topic, @Param("cefrLevel") String cefrLevel, @Param("limit") int limit);
}
