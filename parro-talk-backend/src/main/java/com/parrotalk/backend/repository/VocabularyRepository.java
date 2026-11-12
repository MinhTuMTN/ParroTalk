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
public interface VocabularyRepository extends JpaRepository<Vocabulary, UUID>, JpaSpecificationExecutor<Vocabulary>, VocabularyRepositoryCustom {
    
    Optional<Vocabulary> findByNormalizedWord(String normalizedWord);
    List<Vocabulary> findByNormalizedWordIn(List<String> normalizedWords);

    @EntityGraph(attributePaths = {"senses"})
    @Query("SELECT v FROM Vocabulary v WHERE v.normalizedWord IN :normalizedWords")
    List<Vocabulary> findByNormalizedWordInWithSenses(@Param("normalizedWords") java.util.Collection<String> normalizedWords);

    @Query(value = "SELECT word FROM vocabularies ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<String> findRandomWords(@Param("limit") int limit);

    @EntityGraph(attributePaths = {"senses"})
    @Query("SELECT v FROM Vocabulary v WHERE v.id = :id")
    Optional<Vocabulary> findByIdWithGraph(@Param("id") UUID id);

    /**
     * Page of distinct vocabulary ids that have at least one live sense in the given topic.
     * Ordered deterministically (word, then id) so pages never overlap or skip rows.
     */
    @Query(value = """
            SELECT v.id FROM Vocabulary v
            WHERE EXISTS (SELECT 1 FROM VocabularySense s WHERE s.vocabulary = v AND s.topic.id = :topicId)
            ORDER BY v.normalizedWord ASC, v.id ASC
            """,
            countQuery = """
            SELECT COUNT(v) FROM Vocabulary v
            WHERE EXISTS (SELECT 1 FROM VocabularySense s WHERE s.vocabulary = v AND s.topic.id = :topicId)
            """)
    org.springframework.data.domain.Page<UUID> findIdsByTopicId(
            @Param("topicId") UUID topicId,
            org.springframework.data.domain.Pageable pageable);

    /** Loads vocabularies with senses in one query; pronunciations are batch-loaded (@BatchSize). */
    @EntityGraph(attributePaths = {"senses"})
    @Query("SELECT v FROM Vocabulary v WHERE v.id IN :ids")
    List<Vocabulary> findAllByIdWithSenses(@Param("ids") java.util.Collection<UUID> ids);

}
