package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.TopicPracticeSentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicPracticeSentenceRepository extends JpaRepository<TopicPracticeSentence, UUID> {

    Optional<TopicPracticeSentence> findFirstByTopicIdAndVocabularyId(UUID topicId, UUID vocabularyId);

    Optional<TopicPracticeSentence> findFirstByVocabularyId(UUID vocabularyId);

    Optional<TopicPracticeSentence> findFirstByTopicIdAndTargetWord(UUID topicId, String targetWord);

    Optional<TopicPracticeSentence> findFirstByTargetWord(String targetWord);

    List<TopicPracticeSentence> findByTopicIdAndVocabularyIdIn(UUID topicId, Collection<UUID> vocabularyIds);

    List<TopicPracticeSentence> findByTopicId(UUID topicId);

    @Query("SELECT s FROM TopicPracticeSentence s WHERE LOWER(s.targetWord) = LOWER(:targetWord) AND (:topicId IS NULL OR s.topic.id = :topicId)")
    Optional<TopicPracticeSentence> findByTargetWordAndOptionalTopicId(@Param("targetWord") String targetWord, @Param("topicId") UUID topicId);
}
