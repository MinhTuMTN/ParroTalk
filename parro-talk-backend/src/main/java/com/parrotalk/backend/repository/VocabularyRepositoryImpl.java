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
        return queryRandomVocabularies(topic, cefrLevel, limit, null);
    }

    @Override
    public List<Vocabulary> findRandomVocabularies(String topic, String cefrLevel, int limit, List<String> excludeWords) {
        if (excludeWords == null || excludeWords.isEmpty()) {
            return findRandomVocabularies(topic, cefrLevel, limit);
        }
        List<String> cleanedExcludes = excludeWords.stream()
                .filter(w -> w != null && !w.trim().isEmpty())
                .map(w -> w.trim().toLowerCase())
                .distinct()
                .toList();
        if (cleanedExcludes.isEmpty()) {
            return findRandomVocabularies(topic, cefrLevel, limit);
        }

        List<Vocabulary> unlearned = new java.util.ArrayList<>(queryRandomVocabularies(topic, cefrLevel, limit, cleanedExcludes));
        if (unlearned.size() < limit) {
            List<Vocabulary> fallbackPool = queryRandomVocabularies(topic, cefrLevel, limit, null);
            java.util.Set<String> seenWords = new java.util.HashSet<>();
            for (Vocabulary v : unlearned) {
                if (v.getNormalizedWord() != null) {
                    seenWords.add(v.getNormalizedWord().toLowerCase());
                } else if (v.getWord() != null) {
                    seenWords.add(v.getWord().toLowerCase());
                }
            }
            for (Vocabulary v : fallbackPool) {
                if (unlearned.size() >= limit) {
                    break;
                }
                String key = v.getNormalizedWord() != null ? v.getNormalizedWord().toLowerCase()
                        : (v.getWord() != null ? v.getWord().toLowerCase() : "");
                if (!seenWords.contains(key)) {
                    seenWords.add(key);
                    unlearned.add(v);
                }
            }
        }
        return unlearned;
    }

    @SuppressWarnings("unchecked")
    private List<Vocabulary> queryRandomVocabularies(String topic, String cefrLevel, int limit, List<String> excludeWords) {
        StringBuilder sql = new StringBuilder(
                "SELECT v.* FROM vocabularies v " +
                "WHERE v.is_deleted = false"
        );

        if (excludeWords != null && !excludeWords.isEmpty()) {
            sql.append(" AND LOWER(v.normalized_word) NOT IN (:excludeWords)");
        }

        sql.append(" AND EXISTS (SELECT 1 FROM vocabulary_senses s " +
                "LEFT JOIN vocabulary_topics t ON s.topic_id = t.id " +
                "WHERE s.vocabulary_id = v.id AND s.is_deleted = false"
        );

        if (topic != null && !topic.trim().isEmpty()) {
            sql.append(" AND (LOWER(t.name) = LOWER(:topic) OR LOWER(t.name) LIKE LOWER(CONCAT('%', :topic, '%')))");
        }
        if (cefrLevel != null && !cefrLevel.trim().isEmpty()) {
            sql.append(" AND s.cefr_level = :cefrLevel");
        }
        sql.append(") ORDER BY RANDOM() LIMIT :limit");

        var query = em.createNativeQuery(sql.toString(), Vocabulary.class);

        if (excludeWords != null && !excludeWords.isEmpty()) {
            query.setParameter("excludeWords", excludeWords);
        }
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
