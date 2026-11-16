package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.VocabularyListShare;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VocabularyListShareRepository extends JpaRepository<VocabularyListShare, UUID> {

    List<VocabularyListShare> findBySharedWithUserIdOrderByCreatedAtDesc(UUID userId);

    List<VocabularyListShare> findByVocabularyListIdOrderByCreatedAtDesc(UUID listId);

    Optional<VocabularyListShare> findByVocabularyListIdAndSharedWithUserId(UUID listId, UUID userId);

    boolean existsByVocabularyListIdAndSharedWithUserId(UUID listId, UUID userId);
}

