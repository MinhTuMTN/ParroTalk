package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.VocabularyListItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VocabularyListItemRepository extends JpaRepository<VocabularyListItem, UUID> {

    List<VocabularyListItem> findByVocabularyListIdOrderByDisplayOrderAscCreatedAtAsc(UUID listId);

    Optional<VocabularyListItem> findByVocabularyListIdAndNormalizedWord(UUID listId, String normalizedWord);
}

