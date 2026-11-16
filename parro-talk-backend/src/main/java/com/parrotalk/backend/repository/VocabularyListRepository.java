package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.VocabularyList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VocabularyListRepository extends JpaRepository<VocabularyList, UUID> {

    List<VocabularyList> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Optional<VocabularyList> findByShareToken(String shareToken);
}

