package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.Vocabulary;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VocabularyRepositoryImpl implements VocabularyRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public List<Vocabulary> findRandomVocabularies(String topic, String cefrLevel, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT v.* FROM vocabularies v " +
                "WHERE v.is_deleted = false AND EXISTS (SELECT 1 FROM vocabulary_senses s " +
                "LEFT JOIN vocabulary_topics t ON s.topic_id = t.id " +
                "WHERE s.vocabulary_id = v.id AND s.is_deleted = false"
        );

        if (topic != null && !topic.trim().isEmpty()) {
            sql.append(" AND LOWER(t.name) = LOWER(:topic)");
        }
        if (cefrLevel != null && !cefrLevel.trim().isEmpty()) {
            sql.append(" AND s.cefr_level = :cefrLevel");
        }
        sql.append(") ORDER BY RANDOM() LIMIT :limit");

        var query = em.createNativeQuery(sql.toString(), Vocabulary.class);

        if (topic != null && !topic.trim().isEmpty()) {
            query.setParameter("topic", topic.trim());
        }
        if (cefrLevel != null && !cefrLevel.trim().isEmpty()) {
            query.setParameter("cefrLevel", cefrLevel.trim());
        }
        query.setParameter("limit", limit);

        return query.getResultList();
    }
}
