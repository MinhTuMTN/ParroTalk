package com.parrotalk.backend.repository;

import com.parrotalk.backend.constant.VocabularyStatus;
import com.parrotalk.backend.entity.UserVocabulary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserVocabularyRepository extends JpaRepository<UserVocabulary, UUID> {

    Optional<UserVocabulary> findByUserIdAndNormalizedWord(UUID userId, String normalizedWord);

    boolean existsByUserIdAndNormalizedWord(UUID userId, String normalizedWord);

    Page<UserVocabulary> findByUserId(UUID userId, Pageable pageable);

    Page<UserVocabulary> findByUserIdAndStatus(UUID userId, VocabularyStatus status, Pageable pageable);

    Page<UserVocabulary> findByUserIdAndNextReviewAtLessThanEqual(UUID userId, LocalDateTime nextReviewAt, Pageable pageable);
}
