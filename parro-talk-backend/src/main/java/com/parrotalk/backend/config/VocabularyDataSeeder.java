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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Seeder component for initializing vocabulary data from CSV.
 *
 * @author MinhTuMTN
 */
@Component
public class VocabularyDataSeeder implements CommandLineRunner {

    /** Logger instance **/
    private static final Logger log = LoggerFactory.getLogger(VocabularyDataSeeder.class);

    /** JdbcTemplate for database operations **/
    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructor for VocabularyDataSeeder.
     *
     * @param jdbcTemplate JdbcTemplate bean
     */
    public VocabularyDataSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Executes the seed runner when application starts.
     *
     * @param args Command line arguments
     * @throws Exception if seeding fails
     */
    @Override
    public void run(String... args) throws Exception {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dictionary_entries", Integer.class);
        if (count != null && count > 0) {
            log.info("Dictionary entries already exist ({} records). Skipping seed.", count);
            return;
        }

        log.info("Starting vocabulary data seeding...");

        try {
            ClassPathResource resource = new ClassPathResource("db/data/vocab_final_vn_categorized.csv");
            if (!resource.exists()) {
                log.warn("vocab_final_vn_categorized.csv not found in db/data. Skipping seed.");
                return;
            }

            try (InputStream is = getBOMInputStream(resource.getInputStream());
                 Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                         .setHeader()
                         .setSkipHeaderRecord(true)
                         .setIgnoreHeaderCase(true)
                         .setTrim(true)
                         .build())) {

                List<Object[]> batchArgs = new ArrayList<>();
                Set<String> seenWords = new HashSet<>();
                int batchSize = 500;
                int totalInserted = 0;
                Timestamp now = Timestamp.from(Instant.now());

                for (CSVRecord record : csvParser) {
                    String word = getValue(record, "Word", "word");
                    if (word == null || word.isBlank()) continue;

                    String normalizedWord = word.toLowerCase().trim();
                    if (!seenWords.add(normalizedWord)) {
                        // Skip duplicate words in the CSV
                        continue;
                    }

                    String cefrLevel = getValue(record, "CEFR", "cefr_level");
                    String pos = getValue(record, "POS", "part_of_speech", "parts_of_speech");
                    String phonetic = getValue(record, "IPA", "ipa", "ipa_uk");
                    String audioUkUrl = getValue(record, "Audio_UK", "audio_uk_url");
                    String audioUsUrl = getValue(record, "Audio_US", "audio_us_url");
                    String commonMeaningVi = getValue(record, "Common_Meaning_VN", "common_meaning_vi");
                    String definitionsJson = sanitizeJson(getValue(record, "Definitions", "definitions_json", "meanings_json"));
                    String examplesJson = sanitizeJson(getValue(record, "examples_json", "Examples"));
                    String synonymsJson = sanitizeJson(getValue(record, "Synonyms", "synonyms_json"));
                    String antonymsJson = sanitizeJson(getValue(record, "Antonyms", "antonyms_json"));

                    String topic = getValue(record, "Topic", "topic");
                    String collocationsJson = sanitizeJson(getValue(record, "Collocations", "collocations_json"));
                    String idiomsJson = sanitizeJson(getValue(record, "Idioms", "idioms_json"));
                    String phrasalVerbsJson = sanitizeJson(getValue(record, "Phrasal_Verbs", "phrasal_verbs_json"));

                    batchArgs.add(new Object[]{
                            UUID.randomUUID(),
                            word,
                            normalizedWord,
                            "en",
                            pos,
                            phonetic,
                            cefrLevel,
                            audioUkUrl,
                            audioUsUrl,
                            commonMeaningVi,
                            definitionsJson,
                            examplesJson,
                            synonymsJson,
                            antonymsJson,
                            topic,
                            collocationsJson,
                            idiomsJson,
                            phrasalVerbsJson,
                            "System Seed", now, now, false
                    });

                    if (batchArgs.size() >= batchSize) {
                        totalInserted += safeInsertBatch(batchArgs);
                        batchArgs.clear();
                        if (totalInserted % 5000 == 0) {
                            log.info("Seeded {} vocabulary records so far...", totalInserted);
                        }
                    }
                }

                if (!batchArgs.isEmpty()) {
                    totalInserted += safeInsertBatch(batchArgs);
                }

                log.info("Vocabulary data seeded successfully. Total inserted: {}", totalInserted);
            }
        } catch (Exception e) {
            log.error("Failed to seed vocabulary data", e);
        }
    }

    /**
     * Safely retrieves string value from CSV record using candidate header keys.
     *
     * @param record CSV record instance
     * @param possibleKeys List of candidate header names
     * @return String value if mapped and present, null otherwise
     */
    private String getValue(CSVRecord record, String... possibleKeys) {
        for (String key : possibleKeys) {
            if (record.isMapped(key)) {
                String val = record.get(key);
                if (val != null && !val.isBlank()) {
                    return val;
                }
            }
        }
        return null;
    }

    /**
     * Sanitizes raw input string into valid JSON array or object string.
     *
     * @param rawJson Raw string from CSV field
     * @return Valid JSON string representation or "[]" default
     */
    private String sanitizeJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank() || rawJson.equalsIgnoreCase("null") || rawJson.equalsIgnoreCase("nan")) {
            return "[]";
        }
        String trimmed = rawJson.trim();
        if (!trimmed.startsWith("[") && !trimmed.startsWith("{")) {
            return "[]";
        }
        // Fix Python single-quoted list format e.g. ['item1', 'item2']
        if (trimmed.startsWith("[") && trimmed.contains("'") && !trimmed.contains("\"")) {
            trimmed = trimmed.replace("'", "\"");
        }
        return trimmed;
    }

    /**
     * Wraps InputStream to detect and strip UTF-8 Byte Order Mark (BOM).
     *
     * @param is Original InputStream
     * @return Cleaned InputStream without BOM
     * @throws Exception if stream reading fails
     */
    private InputStream getBOMInputStream(InputStream is) throws Exception {
        PushbackInputStream pb = new PushbackInputStream(is, 3);
        byte[] bom = new byte[3];
        int n = pb.read(bom, 0, bom.length);
        if (n == 3 && (bom[0] == (byte) 0xEF) && (bom[1] == (byte) 0xBB) && (bom[2] == (byte) 0xBF)) {
            // BOM found and stripped
        } else if (n > 0) {
            pb.unread(bom, 0, n);
        }
        return pb;
    }

    /**
     * Safely executes batch insert with ON CONFLICT DO NOTHING and row-by-row fallback if batch fails.
     *
     * @param batchArgs Batch parameters
     * @return Number of successfully inserted records
     */
    private int safeInsertBatch(List<Object[]> batchArgs) {
        String sql = "INSERT INTO dictionary_entries (id, display_word, normalized_word, language, part_of_speech, phonetic, cefr_level, audio_uk_url, audio_us_url, common_meaning_vi, definitions_json, examples_json, synonyms_json, antonyms_json, topic, collocations_json, idioms_json, phrasal_verbs_json, source, created_at, updated_at, is_deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?, ?, ?) " +
                "ON CONFLICT (normalized_word, language) DO NOTHING";
        try {
            jdbcTemplate.batchUpdate(sql, batchArgs);
            return batchArgs.size();
        } catch (Exception e) {
            log.warn("Batch insert failed for {} records, falling back to row-by-row insert. Error: {}", batchArgs.size(), e.getMessage());
            int successCount = 0;
            for (Object[] row : batchArgs) {
                try {
                    jdbcTemplate.update(sql, row);
                    successCount++;
                } catch (Exception ex) {
                    log.error("Failed to insert word '{}': {}", row[1], ex.getMessage());
                }
            }
            return successCount;
        }
    }
}
