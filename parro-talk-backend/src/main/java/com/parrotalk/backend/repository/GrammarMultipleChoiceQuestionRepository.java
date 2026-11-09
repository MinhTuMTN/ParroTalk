package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.GrammarMultipleChoiceQuestion;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GrammarMultipleChoiceQuestionRepository extends JpaRepository<GrammarMultipleChoiceQuestion, UUID> {

    List<GrammarMultipleChoiceQuestion> findBySenseIdIn(Collection<UUID> senseIds);

    List<GrammarMultipleChoiceQuestion> findByIdiomIdIn(Collection<UUID> idiomIds);

    List<GrammarMultipleChoiceQuestion> findByPhrasalVerbIdIn(Collection<UUID> phrasalVerbIds);

    long countBySenseId(UUID senseId);

    long countByIdiomId(UUID idiomId);

    long countByPhrasalVerbId(UUID phrasalVerbId);

    @org.springframework.data.jpa.repository.Query("SELECT q.sense.id, COUNT(q) FROM GrammarMultipleChoiceQuestion q WHERE q.sense.id IN :senseIds GROUP BY q.sense.id")
    List<Object[]> countGroupedBySenseIdIn(@org.springframework.data.repository.query.Param("senseIds") Collection<UUID> senseIds);

    @org.springframework.data.jpa.repository.Query("SELECT q.idiom.id, COUNT(q) FROM GrammarMultipleChoiceQuestion q WHERE q.idiom.id IN :idiomIds GROUP BY q.idiom.id")
    List<Object[]> countGroupedByIdiomIdIn(@org.springframework.data.repository.query.Param("idiomIds") Collection<UUID> idiomIds);

    @org.springframework.data.jpa.repository.Query("SELECT q.phrasalVerb.id, COUNT(q) FROM GrammarMultipleChoiceQuestion q WHERE q.phrasalVerb.id IN :phrasalVerbIds GROUP BY q.phrasalVerb.id")
    List<Object[]> countGroupedByPhrasalVerbIdIn(@org.springframework.data.repository.query.Param("phrasalVerbIds") Collection<UUID> phrasalVerbIds);

    boolean existsByContentHash(String contentHash);
}

