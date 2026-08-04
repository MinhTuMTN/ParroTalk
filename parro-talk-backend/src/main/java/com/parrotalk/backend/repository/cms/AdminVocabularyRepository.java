package com.parrotalk.backend.repository.cms;

import com.parrotalk.backend.entity.cms.AdminVocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminVocabularyRepository extends JpaRepository<AdminVocabulary, UUID>, JpaSpecificationExecutor<AdminVocabulary> {
    
    Optional<AdminVocabulary> findByWordAndPartOfSpeechAndCefrLevel(String word, String partOfSpeech, String cefrLevel);
    
    boolean existsByWordAndPartOfSpeechAndCefrLevel(String word, String partOfSpeech, String cefrLevel);

    @org.springframework.data.jpa.repository.Query(value = "SELECT word FROM admin_vocabularies ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    java.util.List<String> findRandomWords(@org.springframework.data.repository.query.Param("limit") int limit);
}
