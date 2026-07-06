package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.DictionaryContextLookup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DictionaryContextLookupRepository extends JpaRepository<DictionaryContextLookup, UUID> {

    Optional<DictionaryContextLookup> findByNormalizedWordAndContextHash(String normalizedWord, String contextHash);

    boolean existsByNormalizedWordAndContextHash(String normalizedWord, String contextHash);
}
