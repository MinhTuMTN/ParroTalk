package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.Vocabulary;
import java.util.List;
import java.util.Optional;

public interface VocabularyRepositoryCustom {
    List<Vocabulary> findRandomVocabularies(String topic, String cefrLevel, int limit);

    default List<Vocabulary> findRandomVocabularies(String topic, String cefrLevel, int limit, List<String> excludeWords) {
        return findRandomVocabularies(topic, cefrLevel, limit);
    }

    Optional<Vocabulary> findFirstByWordForm(String normalizedForm);
}
