package com.parrotalk.backend.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.entity.*;
import com.parrotalk.backend.repository.TopicRepository;
import com.parrotalk.backend.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class VocabularyImporterService {

    private final VocabularyRepository vocabularyRepository;
    private final TopicRepository topicRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public void importFromJson(String filePath) {
        log.info("Starting vocabulary import from: {}", filePath);
        long startTime = System.currentTimeMillis();

        AtomicInteger totalProcessed = new AtomicInteger(0);
        AtomicInteger totalInserted = new AtomicInteger(0);
        AtomicInteger totalUpdated = new AtomicInteger(0);
        AtomicInteger totalSkipped = new AtomicInteger(0);
        AtomicInteger totalFailed = new AtomicInteger(0);
        List<String> failedWords = Collections.synchronizedList(new ArrayList<>());

        // Pre-load all topics to avoid concurrent creation unique constraint violations
        ConcurrentHashMap<String, Topic> globalTopicCache = new ConcurrentHashMap<>();
        List<Topic> allTopics = topicRepository.findAll();
        for (Topic t : allTopics) {
            globalTopicCache.put(t.getName(), t);
        }
        // Extract all topics from JSON in a single quick pass to prevent concurrent insertion deadlocks
        Set<String> newTopics = ConcurrentHashMap.newKeySet();
        try (JsonParser parser = objectMapper.getFactory().createParser(new File(filePath))) {
            if (parser.nextToken() == JsonToken.START_ARRAY) {
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    JsonNode node = objectMapper.readTree(parser);
                    JsonNode topicsNode = node.path("Topic");
                    if (topicsNode.isArray()) {
                        for (JsonNode topicNode : topicsNode) {
                            String topicName = topicNode.asText(null);
                            if (topicName != null && !topicName.trim().isEmpty()) {
                                String finalTopicName = topicName.trim();
                                if (!globalTopicCache.containsKey(finalTopicName)) {
                                    newTopics.add(finalTopicName);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to pre-scan JSON file for topics", e);
            return;
        }

        if (!newTopics.isEmpty()) {
            log.info("Found {} new topics. Saving to database...", newTopics.size());
            List<Topic> topicsToSave = new ArrayList<>();
            for (String name : newTopics) {
                Topic t = new Topic();
                t.setName(name);
                topicsToSave.add(t);
            }
            topicRepository.saveAll(topicsToSave);
            
            // Reload cache
            globalTopicCache.clear();
            allTopics = topicRepository.findAll();
            for (Topic t : allTopics) {
                globalTopicCache.put(t.getName(), t);
            }
        }
        log.info("Loaded {} existing topics into cache.", globalTopicCache.size());
        // Executor with CallerRunsPolicy to throttle JSON parsing when DB is slow
        int nThreads = 20; // Increased to 20 threads to max out processing speed with the custom Hikari pool size (50)
        ExecutorService executor = new ThreadPoolExecutor(
                nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        try (JsonParser parser = objectMapper.getFactory().createParser(new File(filePath))) {
            
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                log.error("Expected JSON array at root");
                return;
            }

            List<JsonNode> batch = new ArrayList<>();
            final int BATCH_SIZE = 10;

            while (parser.nextToken() != JsonToken.END_ARRAY) {
                JsonNode node = objectMapper.readTree(parser);
                batch.add(node);

                if (batch.size() >= BATCH_SIZE) {
                    List<JsonNode> batchToProcess = new ArrayList<>(batch);
                    batch.clear();
                    
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        BatchResult result = processBatch(batchToProcess, globalTopicCache);
                        totalProcessed.addAndGet(batchToProcess.size());
                        totalInserted.addAndGet(result.inserted);
                        totalUpdated.addAndGet(result.updated);
                        totalSkipped.addAndGet(result.skipped);
                        totalFailed.addAndGet(result.failed);
                        if (!result.failedWords.isEmpty()) {
                            failedWords.addAll(result.failedWords);
                        }
                        int currentProcessed = totalProcessed.get();
                        if (currentProcessed % 1000 == 0 || currentProcessed % 1000 < BATCH_SIZE) {
                            long elapsedSecs = (System.currentTimeMillis() - startTime) / 1000;
                            long speed = elapsedSecs > 0 ? currentProcessed / elapsedSecs : currentProcessed;
                            log.info("Processed {} items (Inserted: {}, Skipped: {}, Failed: {}). Speed: {} items/sec",
                                    currentProcessed, totalInserted.get(), totalSkipped.get(), totalFailed.get(), speed);
                        }
                    }, executor);
                    futures.add(future);
                }
            }

            // Process remaining items in the last batch
            if (!batch.isEmpty()) {
                List<JsonNode> batchToProcess = new ArrayList<>(batch);
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    BatchResult result = processBatch(batchToProcess, globalTopicCache);
                    totalProcessed.addAndGet(batchToProcess.size());
                    totalInserted.addAndGet(result.inserted);
                    totalUpdated.addAndGet(result.updated);
                    totalSkipped.addAndGet(result.skipped);
                    totalFailed.addAndGet(result.failed);
                    if (!result.failedWords.isEmpty()) {
                        failedWords.addAll(result.failedWords);
                    }
                }, executor);
                futures.add(future);
            }

            // Wait for all batches to finish
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            long durationMs = System.currentTimeMillis() - startTime;
            log.info("Import completed in {} ms.", durationMs);
            log.info("Total Processed: {}, Inserted: {}, Updated: {}, Skipped: {}, Failed: {}", 
                    totalProcessed.get(), totalInserted.get(), totalUpdated.get(), totalSkipped.get(), totalFailed.get());
            
            if (!failedWords.isEmpty()) {
                log.warn("Failed words: {}", failedWords);
            }

        } catch (Exception e) {
            log.error("Failed to read JSON file or parser error", e);
        } finally {
            executor.shutdown();
        }
    }

    private BatchResult processBatch(List<JsonNode> batch, ConcurrentHashMap<String, Topic> globalTopicCache) {
        try {
            return transactionTemplate.execute(status -> processNodes(batch, globalTopicCache));
        } catch (Exception e) {
            log.warn("Batch failed (possibly constraint violation). Falling back to item-by-item processing to identify errors. Reason: {}", e.getMessage());
            BatchResult result = new BatchResult();
            for (JsonNode node : batch) {
                try {
                    BatchResult singleResult = transactionTemplate.execute(status -> processNodes(List.of(node), globalTopicCache));
                    if (singleResult != null) {
                        result.inserted += singleResult.inserted;
                        result.updated += singleResult.updated;
                        result.skipped += singleResult.skipped;
                        result.failed += singleResult.failed;
                        result.failedWords.addAll(singleResult.failedWords);
                    }
                } catch (Exception ex) {
                    String word = node.path("Word").asText(null);
                    log.error("Error processing word '{}': {}", word, ex.getMessage());
                    result.failed++;
                    if (word != null && !word.trim().isEmpty()) {
                        result.failedWords.add(word);
                    }
                }
            }
            return result;
        }
    }

    private BatchResult processNodes(List<JsonNode> nodes, ConcurrentHashMap<String, Topic> globalTopicCache) {
        BatchResult result = new BatchResult();
        
        List<String> normalizedWords = new ArrayList<>();
        for (JsonNode node : nodes) {
            String word = node.path("Word").asText(null);
            if (word != null && !word.trim().isEmpty()) {
                normalizedWords.add(word.trim().toLowerCase());
            }
        }

        Map<String, Vocabulary> existingVocabMap = new HashMap<>();
        if (!normalizedWords.isEmpty()) {
            List<Vocabulary> existingVocabs = vocabularyRepository.findByNormalizedWordIn(normalizedWords);
            for (Vocabulary v : existingVocabs) {
                existingVocabMap.put(v.getNormalizedWord(), v);
            }
        }

        for (JsonNode node : nodes) {
            String word = node.path("Word").asText(null);
            if (word == null || word.trim().isEmpty()) {
                result.skipped++;
                continue;
            }
            
            String normalizedWord = word.trim().toLowerCase();
            Vocabulary vocabulary = existingVocabMap.get(normalizedWord);
            
            if (vocabulary != null) {
                result.skipped++;
                continue;
            } else {
                vocabulary = new Vocabulary();
                vocabulary.setNormalizedWord(normalizedWord);
                // Put it in map to handle duplicates within the same batch
                existingVocabMap.put(normalizedWord, vocabulary);
                result.inserted++;
            }

            vocabulary.setWord(word.trim());
            vocabulary.setCommonMeaningVn(node.path("common_meaning_vn").asText(null));



            // Pronunciation
            JsonNode pronNode = node.path("Pronunciation");
            if (pronNode.isObject()) {
                if (pronNode.has("UK")) {
                    JsonNode uk = pronNode.get("UK");
                    VocabularyPronunciation p = new VocabularyPronunciation();
                    p.setVocabulary(vocabulary);
                    p.setAccent("UK");
                    p.setIpa(uk.path("IPA").asText(null));
                    p.setAudioUrl(uk.path("Audio").asText(null));
                    vocabulary.getPronunciations().add(p);
                }
                if (pronNode.has("US")) {
                    JsonNode us = pronNode.get("US");
                    VocabularyPronunciation p = new VocabularyPronunciation();
                    p.setVocabulary(vocabulary);
                    p.setAccent("US");
                    p.setIpa(us.path("IPA").asText(null));
                    p.setAudioUrl(us.path("Audio").asText(null));
                    vocabulary.getPronunciations().add(p);
                }
            }

            // Senses
            JsonNode sensesNode = node.path("Senses");
            if (sensesNode.isArray()) {
                for (JsonNode sNode : sensesNode) {
                    VocabularySense sense = new VocabularySense();
                    sense.setVocabulary(vocabulary);
                    sense.setPartOfSpeech(sNode.path("POS").asText(null));
                    sense.setCefrLevel(sNode.path("CEFR").asText(null));
                    sense.setIsAiCefr(sNode.path("is_ai_cefr").asBoolean(false));
                    sense.setDefinitionEn(sNode.path("Definition_EN").asText(null));
                    sense.setDefinitionVn(sNode.path("Definition_VN").asText(null));
                    sense.setExampleEn(sNode.path("example").asText(null));
                    sense.setExampleVn(sNode.path("example_vn").asText(null));

                    // Synonyms
                    JsonNode synNode = sNode.path("Synonyms");
                    if (synNode.isArray()) {
                        for (JsonNode syn : synNode) {
                            String synWord = syn.path("synonym").asText(null);
                            if (synWord != null && !synWord.trim().isEmpty()) {
                                SenseSynonym synonym = new SenseSynonym();
                                synonym.setSense(sense);
                                synonym.setSynonymWord(synWord.trim());
                                synonym.setSynonymVn(syn.path("synonym_vn").asText(null));
                                sense.getSynonyms().add(synonym);
                            }
                        }
                    }

                    // Antonyms
                    JsonNode antNode = sNode.path("Antonyms");
                    if (antNode.isArray()) {
                        for (JsonNode ant : antNode) {
                            String antWord = ant.path("antonym").asText(null);
                            if (antWord != null && !antWord.trim().isEmpty()) {
                                SenseAntonym antonym = new SenseAntonym();
                                antonym.setSense(sense);
                                antonym.setAntonymWord(antWord.trim());
                                antonym.setAntonymVn(ant.path("antonym_vn").asText(null));
                                sense.getAntonyms().add(antonym);
                            }
                        }
                    }

                    // Collocations
                    JsonNode colNode = sNode.path("Collocations");
                    if (colNode.isArray()) {
                        for (JsonNode col : colNode) {
                            String colWord = col.path("collocation").asText(null);
                            if (colWord != null && !colWord.trim().isEmpty()) {
                                SenseCollocation collocation = new SenseCollocation();
                                collocation.setSense(sense);
                                collocation.setCollocationWord(colWord.trim());
                                collocation.setCollocationVn(col.path("collocation_vn").asText(null));
                                sense.getCollocations().add(collocation);
                            }
                        }
                    }

                    vocabulary.getSenses().add(sense);
                }
            }

            // Idioms
            JsonNode idiomsNode = node.path("Idioms");
            if (idiomsNode.isArray()) {
                for (JsonNode iNode : idiomsNode) {
                    String phrase = iNode.path("phrase").asText(null);
                    if (phrase != null && !phrase.trim().isEmpty()) {
                        Idiom idiom = new Idiom();
                        idiom.setVocabulary(vocabulary);
                        idiom.setPhrase(phrase.trim());
                        idiom.setDefinition(iNode.path("definition").asText(null));
                        idiom.setDefinitionVn(iNode.path("definition_vn").asText(null));
                        idiom.setExample(iNode.path("example").asText(null));
                        idiom.setExampleVn(iNode.path("example_vn").asText(null));
                        vocabulary.getIdioms().add(idiom);
                    }
                }
            }

            // Phrasal Verbs
            JsonNode pvNode = node.path("Phrasal_Verbs");
            if (pvNode.isArray()) {
                for (JsonNode pNode : pvNode) {
                    String phrase = pNode.path("phrase").asText(null);
                    if (phrase != null && !phrase.trim().isEmpty()) {
                        PhrasalVerb pv = new PhrasalVerb();
                        pv.setVocabulary(vocabulary);
                        pv.setPhrase(phrase.trim());
                        pv.setDefinition(pNode.path("definition").asText(null));
                        pv.setDefinitionVn(pNode.path("definition_vn").asText(null));
                        pv.setExample(pNode.path("example").asText(null));
                        pv.setExampleVn(pNode.path("example_vn").asText(null));
                        vocabulary.getPhrasalVerbs().add(pv);
                    }
                }
            }

            vocabularyRepository.save(vocabulary);
        }
        return result;
    }

    private static class BatchResult {
        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;
        List<String> failedWords = new ArrayList<>();
    }
}
