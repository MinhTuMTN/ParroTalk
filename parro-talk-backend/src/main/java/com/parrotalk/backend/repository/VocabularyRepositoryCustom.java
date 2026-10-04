package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.Vocabulary;
import java.util.List;

public interface VocabularyRepositoryCustom {
    List<Vocabulary> findRandomVocabularies(String topic, String cefrLevel, int limit);
}
