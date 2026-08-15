package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.DictionaryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link DictionaryEntry}.
 *
 * @author MinhTuMTN
 */
@Repository
public interface DictionaryEntryRepository extends JpaRepository<DictionaryEntry, UUID> {

    /** Find dictionary entry by normalized word. **/
    Optional<DictionaryEntry> findByNormalizedWordAndLanguage(String normalizedWord, String language);

    /** Query list of unique topics with word count. **/
    @Query("SELECT d.topic, COUNT(d) FROM DictionaryEntry d WHERE d.topic IS NOT NULL AND d.topic <> '' GROUP BY d.topic ORDER BY COUNT(d) DESC")
    List<Object[]> findTopicsWithCounts();

    /** Search vocabulary by keyword, topic, CEFR level, and part of speech. **/
    @Query("SELECT d FROM DictionaryEntry d WHERE " +
           "(:topic IS NULL OR :topic = '' OR LOWER(d.topic) = LOWER(:topic)) AND " +
           "(:cefrLevel IS NULL OR :cefrLevel = '' OR LOWER(d.cefrLevel) = LOWER(:cefrLevel)) AND " +
           "(:partOfSpeech IS NULL OR :partOfSpeech = '' OR LOWER(d.partOfSpeech) LIKE LOWER(CONCAT('%', :partOfSpeech, '%'))) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(d.displayWord) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(d.commonMeaningVi) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(CAST(d.definitionsJson AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(CAST(d.idiomsJson AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(CAST(d.collocationsJson AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(CAST(d.phrasalVerbsJson AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<DictionaryEntry> searchVocabularies(
            @Param("keyword") String keyword,
            @Param("topic") String topic,
            @Param("cefrLevel") String cefrLevel,
            @Param("partOfSpeech") String partOfSpeech,
            Pageable pageable);

    /** Find random dictionary entries for practice questions. **/
    @Query("SELECT d FROM DictionaryEntry d WHERE " +
           "(:topic IS NULL OR :topic = '' OR LOWER(d.topic) = LOWER(:topic)) AND " +
           "(:cefrLevel IS NULL OR :cefrLevel = '' OR LOWER(d.cefrLevel) = LOWER(:cefrLevel)) " +
           "ORDER BY FUNCTION('RANDOM')")
    Page<DictionaryEntry> findRandomEntries(
            @Param("topic") String topic,
            @Param("cefrLevel") String cefrLevel,
            Pageable pageable);
}
