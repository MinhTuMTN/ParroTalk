package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.PracticeQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PracticeQuestionRepository extends JpaRepository<PracticeQuestion, UUID> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"userVocabulary", "userVocabulary.dictionaryEntry"})
    List<PracticeQuestion> findBySessionId(UUID sessionId);
    
}
