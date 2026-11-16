package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.UserVocabularyListProgress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserVocabularyListProgressRepository extends JpaRepository<UserVocabularyListProgress, UUID> {

    List<UserVocabularyListProgress> findByUserIdAndVocabularyListId(UUID userId, UUID listId);

    Optional<UserVocabularyListProgress> findByUserIdAndListItemId(UUID userId, UUID listItemId);
}

