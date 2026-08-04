package com.parrotalk.backend.config;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class VocabularyDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(VocabularyDataSeeder.class);
    private final JdbcTemplate jdbcTemplate;

    public VocabularyDataSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dictionary_entries", Integer.class);
        if (count != null && count > 0) {
            log.info("Dictionary entries already exist. Skipping seed.");
            return;
        }

        log.info("Starting vocabulary data seeding...");

        try {
            ClassPathResource resource = new ClassPathResource("db/data/vocabulary.csv");
            if (!resource.exists()) {
                log.warn("vocabulary.csv not found in db/data. Skipping seed.");
                return;
            }

            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {

                List<Object[]> batchArgs = new ArrayList<>();
                int batchSize = 500;
                Timestamp now = Timestamp.from(Instant.now());

                for (CSVRecord record : csvParser) {
                    // Csv structure expected: word, cefr_level, part_of_speech, ipa, audio_uk_url, audio_us_url, common_meaning_vi, meanings_json
                    String word = record.get("word");
                    String cefrLevel = record.isMapped("cefr_level") ? record.get("cefr_level") : null;
                    String pos = record.isMapped("part_of_speech") ? record.get("part_of_speech") : (record.isMapped("parts_of_speech") ? record.get("parts_of_speech") : null);
                    String phonetic = record.isMapped("ipa") ? record.get("ipa") : (record.isMapped("ipa_uk") ? record.get("ipa_uk") : null);
                    String audioUkUrl = record.isMapped("audio_uk_url") ? record.get("audio_uk_url") : null;
                    String audioUsUrl = record.isMapped("audio_us_url") ? record.get("audio_us_url") : null;
                    String commonMeaningVi = record.isMapped("common_meaning_vi") ? record.get("common_meaning_vi") : null;
                    String meaningsJson = record.isMapped("meanings_json") ? record.get("meanings_json") : "[]";

                    batchArgs.add(new Object[]{
                            UUID.randomUUID(),
                            word,
                            word.toLowerCase(),
                            "en",
                            pos,
                            phonetic,
                            cefrLevel,
                            audioUkUrl,
                            audioUsUrl,
                            commonMeaningVi,
                            meaningsJson != null && !meaningsJson.isEmpty() ? meaningsJson : "[]",
                            "[]", "[]", "[]", "System Seed", now, now, false
                    });

                    if (batchArgs.size() >= batchSize) {
                        insertBatch(batchArgs);
                        batchArgs.clear();
                    }
                }

                if (!batchArgs.isEmpty()) {
                    insertBatch(batchArgs);
                }

                log.info("Vocabulary data seeded successfully.");
            }
        } catch (Exception e) {
            log.error("Failed to seed vocabulary data", e);
        }
    }

    private void insertBatch(List<Object[]> batchArgs) {
        String sql = "INSERT INTO dictionary_entries (id, display_word, normalized_word, language, part_of_speech, phonetic, cefr_level, audio_uk_url, audio_us_url, common_meaning_vi, definitions_json, examples_json, synonyms_json, antonyms_json, source, created_at, updated_at, is_deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}
