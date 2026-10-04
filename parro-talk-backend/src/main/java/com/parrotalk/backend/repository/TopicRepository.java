package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {
    Optional<Topic> findByName(String name);
    List<Topic> findByNameIn(java.util.Collection<String> names);

    @Query("""
            SELECT t.id, t.name, COUNT(DISTINCT v.id)
            FROM Topic t
            LEFT JOIN VocabularySense vs ON vs.topic.id = t.id AND vs.isDeleted = false
            LEFT JOIN Vocabulary v ON v.id = vs.vocabulary.id AND v.isDeleted = false
            WHERE t.isDeleted = false
            GROUP BY t.id, t.name
            ORDER BY t.name
            """)
    List<Object[]> findTopicsWithCounts();

    @Query("""
            SELECT COUNT(DISTINCT v.id)
            FROM VocabularySense vs
            JOIN Vocabulary v ON v.id = vs.vocabulary.id AND v.isDeleted = false
            WHERE vs.topic.id = :topicId AND vs.isDeleted = false
            """)
    long countDistinctVocabulariesByTopicId(@org.springframework.data.repository.query.Param("topicId") UUID topicId);
}
