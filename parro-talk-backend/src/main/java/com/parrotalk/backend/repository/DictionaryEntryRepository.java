package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.DictionaryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DictionaryEntryRepository extends JpaRepository<DictionaryEntry, UUID> {

    Optional<DictionaryEntry> findByNormalizedWordAndLanguage(String normalizedWord, String language);

    boolean existsByNormalizedWordAndLanguage(String normalizedWord, String language);
}
