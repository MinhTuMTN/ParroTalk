package com.parrotalk.backend.repository;

import com.parrotalk.backend.constant.VocabularyStatus;
import com.parrotalk.backend.entity.UserVocabulary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;

@Repository
public interface UserVocabularyRepository extends JpaRepository<UserVocabulary, UUID> {

    Optional<UserVocabulary> findByUserIdAndNormalizedWord(UUID userId, String normalizedWord);

    boolean existsByUserIdAndNormalizedWord(UUID userId, String normalizedWord);

    Page<UserVocabulary> findByUserId(UUID userId, Pageable pageable);

    Page<UserVocabulary> findByUserIdAndStatus(UUID userId, VocabularyStatus status, Pageable pageable);

    Page<UserVocabulary> findByUserIdAndNextReviewAtLessThanEqual(UUID userId, LocalDateTime nextReviewAt, Pageable pageable);

    @Query("""
            SELECT t.name, COUNT(DISTINCT uv.normalizedWord)
            FROM Topic t
            JOIN VocabularySense vs ON vs.topic.id = t.id AND vs.isDeleted = false
            JOIN Vocabulary v ON v.id = vs.vocabulary.id AND v.isDeleted = false
            JOIN UserVocabulary uv ON uv.normalizedWord = v.normalizedWord 
                AND uv.user.id = :userId 
                AND uv.status IN ('LEARNING', 'MASTERED') 
                AND uv.isDeleted = false
            WHERE t.isDeleted = false
            GROUP BY t.name
            """)
    List<Object[]> countLearnedWordsByTopicForUser(@org.springframework.data.repository.query.Param("userId") java.util.UUID userId);

    /** (normalizedWord, status) rows for the given user and words. Always scoped by userId. */
    @Query("""
            SELECT uv.normalizedWord, uv.status
            FROM UserVocabulary uv
            WHERE uv.user.id = :userId AND uv.normalizedWord IN :words AND uv.isDeleted = false
            """)
    List<Object[]> findStatusesByUserIdAndNormalizedWordIn(
            @org.springframework.data.repository.query.Param("userId") UUID userId,
            @org.springframework.data.repository.query.Param("words") java.util.Collection<String> words);

    @Query("""
            SELECT COUNT(DISTINCT uv.normalizedWord)
            FROM VocabularySense vs
            JOIN Vocabulary v ON v.id = vs.vocabulary.id AND v.isDeleted = false
            JOIN UserVocabulary uv ON uv.normalizedWord = v.normalizedWord
                AND uv.user.id = :userId
                AND uv.status IN ('LEARNING', 'MASTERED')
                AND uv.isDeleted = false
            WHERE vs.topic.id = :topicId AND vs.isDeleted = false
            """)
    long countLearnedWordsForTopic(
            @org.springframework.data.repository.query.Param("userId") UUID userId,
            @org.springframework.data.repository.query.Param("topicId") UUID topicId);

    @Query("""
            SELECT DISTINCT uv.normalizedWord
            FROM VocabularySense vs
            JOIN Vocabulary v ON v.id = vs.vocabulary.id AND v.isDeleted = false
            JOIN UserVocabulary uv ON uv.normalizedWord = v.normalizedWord
                AND uv.user.id = :userId
                AND uv.status IN ('LEARNING', 'MASTERED')
                AND uv.isDeleted = false
            WHERE LOWER(vs.topic.name) = LOWER(:topicName) AND vs.isDeleted = false
            """)
    List<String> findLearnedNormalizedWordsByUserIdAndTopicName(
            @org.springframework.data.repository.query.Param("userId") UUID userId,
            @org.springframework.data.repository.query.Param("topicName") String topicName);
}
