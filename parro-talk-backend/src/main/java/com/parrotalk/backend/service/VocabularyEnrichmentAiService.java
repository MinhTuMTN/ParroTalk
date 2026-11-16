package com.parrotalk.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.constant.UserNotificationType;
import com.parrotalk.backend.constant.VocabularyCrawlStatus;
import com.parrotalk.backend.constant.VocabularyDataSource;
import com.parrotalk.backend.entity.Idiom;
import com.parrotalk.backend.entity.PhrasalVerb;
import com.parrotalk.backend.entity.SenseAntonym;
import com.parrotalk.backend.entity.SenseCollocation;
import com.parrotalk.backend.entity.SenseSynonym;
import com.parrotalk.backend.entity.Topic;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularyList;
import com.parrotalk.backend.entity.VocabularyListItem;
import com.parrotalk.backend.entity.VocabularyPronunciation;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.repository.TopicRepository;
import com.parrotalk.backend.repository.VocabularyListItemRepository;
import com.parrotalk.backend.repository.VocabularyListRepository;
import com.parrotalk.backend.repository.VocabularyRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class VocabularyEnrichmentAiService {

    private static final Set<String> AUTO_SELECT_CEFR_LEVELS = Set.of("A1", "A2", "B1", "B2");

    private static final String ENRICH_SYSTEM_PROMPT = """
            You are an expert English-Vietnamese lexicographer and Cambridge CEFR specialist.
            Given raw crawled dictionary data (from Oxford Learner's Dictionaries and/or Merriam-Webster Thesaurus/Dictionary),
            or a word that was not found in external dictionaries:
            1. Provide a concise, natural Vietnamese common meaning (`commonMeaningVn`).
            2. For each sense:
               - Keep or assign an accurate `partOfSpeech` and `cefrLevel` (A1, A2, B1, B2, C1, or C2).
               - Provide clear `definitionEn` and natural Vietnamese `definitionVn`.
               - Provide a natural `exampleEn` containing the word and its Vietnamese translation `exampleVn`.
               - Align relevant synonyms and antonyms from `mwSenseSynAntGroups` to the matching sense so each sense only gets synonyms/antonyms that match its specific meaning.
               - Preserve or suggest up to 5 natural `collocations` and a domain `topic`.
            3. For each idiom and phrasalVerb:
               - Provide `definitionVn` and `exampleVn` corresponding to `definitionEn` and `exampleEn`.
            4. If the input has no senses (status=NOT_FOUND), generate 1-3 accurate senses for the English word/phrase.

            Output ONLY a valid JSON object matching AiEnrichmentResponse with no markdown fences.
            """;

    private final AiCoreClient aiCoreClient;
    private final VocabularyRepository vocabularyRepository;
    private final TopicRepository topicRepository;
    private final VocabularyListItemRepository listItemRepository;
    private final VocabularyListRepository listRepository;
    private final UserNotificationService userNotificationService;
    private final ObjectMapper objectMapper;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiEnrichedSenseItem {
        private int index;
        private String partOfSpeech;
        private String cefrLevel;
        private String definitionEn;
        private String definitionVn;
        private String exampleEn;
        private String exampleVn;
        private List<String> synonyms;
        private List<String> antonyms;
        private List<String> collocations;
        private String topic;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiEnrichedPhraseItem {
        private String phrase;
        private String definitionEn;
        private String definitionVn;
        private String exampleEn;
        private String exampleVn;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiEnrichmentResponse {
        private String commonMeaningVn;
        private List<String> wordForms;
        private List<AiEnrichedSenseItem> senses;
        private List<AiEnrichedPhraseItem> idioms;
        private List<AiEnrichedPhraseItem> phrasalVerbs;
    }

    @Transactional
    public void processCrawlResultMessage(String messageJson) {
        try {
            JsonNode root = objectMapper.readTree(messageJson);
            String word = root.path("word").asText("").trim();
            String status = root.path("status").asText("NOT_FOUND");
            String rawDataSource = root.path("dataSource").asText("NOT_FOUND");
            String listItemIdStr = root.path("listItemId").asText(null);
            String listIdStr = root.path("listId").asText(null);
            JsonNode payload = root.path("payload");

            VocabularyListItem listItem = (listItemIdStr != null && !listItemIdStr.isBlank())
                    ? listItemRepository.findById(UUID.fromString(listItemIdStr)).orElse(null)
                    : null;

            if (word.isEmpty() && listItem != null) {
                word = listItem.getRawWord();
            }
            if (word.isEmpty()) {
                log.warn("Received crawl result with blank word: {}", messageJson);
                return;
            }

            if ("FAILED".equalsIgnoreCase(status)) {
                handleFailedCrawl(listItem, root.path("error").asText("Crawler error"));
                return;
            }

            VocabularyDataSource resolvedSource = resolveDataSource(status, rawDataSource);
            AiEnrichmentResponse enriched = callAiToEnrich(word, status, payload);

            Vocabulary savedVocab = upsertVocabulary(word, resolvedSource, payload, enriched);

            if (listItem != null) {
                listItem.setVocabulary(savedVocab);
                listItem.setCrawlStatus(VocabularyCrawlStatus.COMPLETED);
                listItem.setCrawlErrorMessage(null);
                listItem.setSelectedSenseIds(selectDefaultSenseIds(savedVocab.getSenses()));
                listItem.setSelectedIdiomIds(
                        savedVocab.getIdioms().stream().map(Idiom::getId).filter(java.util.Objects::nonNull).toList()
                );
                listItem.setSelectedPhrasalVerbIds(
                        savedVocab.getPhrasalVerbs().stream().map(PhrasalVerb::getId).filter(java.util.Objects::nonNull).toList()
                );
                listItemRepository.save(listItem);

                VocabularyList vocabList = listItem.getVocabularyList();
                User owner = vocabList != null ? vocabList.getOwner() : null;
                UUID refListId = vocabList != null ? vocabList.getId()
                        : (listIdStr != null && !listIdStr.isBlank() ? UUID.fromString(listIdStr) : null);

                if (owner != null) {
                    userNotificationService.notifyUser(
                            owner,
                            UserNotificationType.VOCAB_CRAWL_COMPLETED,
                            "Đã thu thập xong từ '" + savedVocab.getWord() + "'",
                            "Từ '" + savedVocab.getWord() + "' đã được thu thập (" + resolvedSource.name()
                                    + ") và bổ sung " + savedVocab.getSenses().size() + " nghĩa vào danh sách của bạn.",
                            refListId,
                            Map.of(
                                    "word", savedVocab.getWord(),
                                    "vocabularyId", savedVocab.getId() != null ? savedVocab.getId().toString() : "",
                                    "listItemId", listItem.getId() != null ? listItem.getId().toString() : "",
                                    "dataSource", resolvedSource.name(),
                                    "senseCount", savedVocab.getSenses().size()
                            )
                    );
                }
            }
        } catch (Exception ex) {
            log.error("Failed to process crawl result message: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Failed to process crawl result message", ex);
        }
    }

    private void handleFailedCrawl(VocabularyListItem listItem, String errorMsg) {
        if (listItem == null) {
            return;
        }
        listItem.setCrawlStatus(VocabularyCrawlStatus.FAILED);
        listItem.setCrawlErrorMessage(errorMsg);
        listItemRepository.save(listItem);

        VocabularyList vocabList = listItem.getVocabularyList();
        if (vocabList != null && vocabList.getOwner() != null) {
            userNotificationService.notifyUser(
                    vocabList.getOwner(),
                    UserNotificationType.VOCAB_CRAWL_FAILED,
                    "Không thể thu thập từ '" + listItem.getRawWord() + "'",
                    "Quá trình thu thập từ '" + listItem.getRawWord() + "' gặp lỗi: " + errorMsg,
                    vocabList.getId(),
                    Map.of("word", listItem.getRawWord(), "error", errorMsg)
            );
        }
    }

    private VocabularyDataSource resolveDataSource(String status, String rawDataSource) {
        if ("NOT_FOUND".equalsIgnoreCase(status) || "NOT_FOUND".equalsIgnoreCase(rawDataSource)) {
            return VocabularyDataSource.AI_GENERATED;
        }
        try {
            return VocabularyDataSource.valueOf(rawDataSource);
        } catch (Exception ignored) {
            return VocabularyDataSource.OXFORD_MW_HYBRID;
        }
    }

    private AiEnrichmentResponse callAiToEnrich(String word, String status, JsonNode payload) {
        try {
            String userPrompt = objectMapper.writeValueAsString(Map.of(
                    "word", word,
                    "crawlStatus", status,
                    "rawPayload", payload
            ));
            return aiCoreClient.callAndValidate(
                    ENRICH_SYSTEM_PROMPT,
                    userPrompt,
                    AiEnrichmentResponse.class,
                    resp -> {
                        if (resp == null) {
                            throw new IllegalArgumentException("AI returned null enrichment response");
                        }
                        return resp;
                    }
            );
        } catch (Exception ex) {
            log.warn("AI enrichment fallback triggered for word '{}': {}", word, ex.getMessage());
            return buildFallbackEnrichmentFromPayload(word, payload);
        }
    }

    private AiEnrichmentResponse buildFallbackEnrichmentFromPayload(String word, JsonNode payload) {
        List<AiEnrichedSenseItem> senses = new ArrayList<>();
        JsonNode rawSenses = payload.path("senses");
        if (rawSenses.isArray() && !rawSenses.isEmpty()) {
            int idx = 0;
            for (JsonNode s : rawSenses) {
                String defVi = s.hasNonNull("definitionVi") && !s.path("definitionVi").asText().isBlank()
                        ? s.path("definitionVi").asText()
                        : (s.hasNonNull("definitionVn") && !s.path("definitionVn").asText().isBlank()
                                ? s.path("definitionVn").asText()
                                : s.path("definitionEn").asText(word));
                String exVi = s.hasNonNull("exampleVi") && !s.path("exampleVi").asText().isBlank()
                        ? s.path("exampleVi").asText()
                        : s.path("exampleVn").asText("");

                senses.add(AiEnrichedSenseItem.builder()
                        .index(idx++)
                        .partOfSpeech(s.path("partOfSpeech").asText("noun"))
                        .cefrLevel(s.path("cefrLevel").asText("B1"))
                        .definitionEn(s.path("definitionEn").asText(word))
                        .definitionVn(defVi)
                        .exampleEn(s.path("exampleEn").asText(""))
                        .exampleVn(exVi)
                        .synonyms(readStringList(s.path("synonyms")))
                        .antonyms(readStringList(s.path("antonyms")))
                        .collocations(readStringList(s.path("collocations")))
                        .topic(s.path("topic").asText(null))
                        .build());
            }
        } else {
            senses.add(AiEnrichedSenseItem.builder()
                    .index(0)
                    .partOfSpeech("noun")
                    .cefrLevel("B1")
                    .definitionEn(word)
                    .definitionVn(word)
                    .exampleEn("This is an example of " + word + ".")
                    .exampleVn("")
                    .build());
        }
        return AiEnrichmentResponse.builder()
                .commonMeaningVn(word)
                .senses(senses)
                .idioms(List.of())
                .phrasalVerbs(List.of())
                .build();
    }

    private Vocabulary upsertVocabulary(
            String word,
            VocabularyDataSource resolvedSource,
            JsonNode payload,
            AiEnrichmentResponse enriched) throws Exception {
        String normalizedWord = word.trim().toLowerCase();
        List<Vocabulary> existingList = vocabularyRepository.findByNormalizedWordIn(List.of(normalizedWord));
        Vocabulary vocab = existingList.isEmpty() ? new Vocabulary() : existingList.get(0);

        vocab.setWord(word.trim());
        vocab.setNormalizedWord(normalizedWord);
        if (vocab.getId() == null || vocab.getDataSource() == null) {
            vocab.setDataSource(resolvedSource);
        }
        if ((vocab.getCommonMeaningVn() == null || vocab.getCommonMeaningVn().isBlank())
                && enriched.getCommonMeaningVn() != null && !enriched.getCommonMeaningVn().isBlank()) {
            vocab.setCommonMeaningVn(enriched.getCommonMeaningVn().trim());
        }

        List<String> wordForms = readStringList(payload.path("wordForms"));
        if (wordForms.isEmpty() && enriched.getWordForms() != null) {
            wordForms = enriched.getWordForms();
        }
        if (!wordForms.isEmpty()) {
            vocab.setWordFormsJson(objectMapper.writeValueAsString(wordForms));
        }

        // Pronunciations (merge by accent without clearing existing rows)
        JsonNode rawProns = payload.path("pronunciations");
        if (rawProns.isArray() && !rawProns.isEmpty()) {
            Map<String, VocabularyPronunciation> existingByAccent = new java.util.HashMap<>();
            for (VocabularyPronunciation existing : vocab.getPronunciations()) {
                if (existing.getAccent() != null) {
                    existingByAccent.put(existing.getAccent().toUpperCase(), existing);
                }
            }
            for (JsonNode p : rawProns) {
                String accent = p.path("accent").asText("US").trim();
                String ipa = p.path("ipa").asText("");
                String audioUrl = p.path("audioUrl").asText("");
                VocabularyPronunciation existing = existingByAccent.get(accent.toUpperCase());
                if (existing != null) {
                    if (!ipa.isBlank()) existing.setIpa(ipa);
                    if (!audioUrl.isBlank()) existing.setAudioUrl(audioUrl);
                } else {
                    VocabularyPronunciation pron = VocabularyPronunciation.builder()
                            .vocabulary(vocab)
                            .accent(accent)
                            .ipa(ipa)
                            .audioUrl(audioUrl)
                            .build();
                    vocab.getPronunciations().add(pron);
                    existingByAccent.put(accent.toUpperCase(), pron);
                }
            }
        }

        // Senses (append only non-duplicate senses so existing IDs stay intact)
        JsonNode rawSenses = payload.path("senses");
        if (enriched.getSenses() != null && !enriched.getSenses().isEmpty()) {
            Set<String> existingDefs = new java.util.HashSet<>();
            for (VocabularySense existingSense : vocab.getSenses()) {
                if (existingSense.getDefinitionEn() != null) {
                    existingDefs.add(existingSense.getDefinitionEn().trim().toLowerCase());
                }
            }
            for (int i = 0; i < enriched.getSenses().size(); i++) {
                AiEnrichedSenseItem item = enriched.getSenses().get(i);
                String defKey = item.getDefinitionEn() != null ? item.getDefinitionEn().trim().toLowerCase() : "";
                if (!defKey.isEmpty() && existingDefs.contains(defKey)) {
                    continue;
                }
                if (!defKey.isEmpty()) {
                    existingDefs.add(defKey);
                }
                JsonNode rawSenseNode = (rawSenses.isArray() && i < rawSenses.size()) ? rawSenses.get(i) : null;
                boolean hadCefrInRaw = rawSenseNode != null && !rawSenseNode.path("cefrLevel").asText("").isBlank();

                String cefr = (item.getCefrLevel() != null && !item.getCefrLevel().isBlank())
                        ? item.getCefrLevel().trim().toUpperCase()
                        : "B1";

                VocabularySense sense = VocabularySense.builder()
                        .vocabulary(vocab)
                        .partOfSpeech(item.getPartOfSpeech() != null ? item.getPartOfSpeech() : "noun")
                        .cefrLevel(cefr)
                        .isAiCefr(!hadCefrInRaw)
                        .dataSource(resolvedSource)
                        .definitionEn(item.getDefinitionEn())
                        .definitionVn(item.getDefinitionVn())
                        .exampleEn(item.getExampleEn())
                        .exampleVn(item.getExampleVn())
                        .build();

                if (item.getTopic() != null && !item.getTopic().isBlank()) {
                    String tName = item.getTopic().trim();
                    Topic topic = topicRepository.findByName(tName).orElse(null);
                    if (topic == null) {
                        Topic newTopic = new Topic();
                        newTopic.setName(tName);
                        topic = topicRepository.save(newTopic);
                    }
                    sense.setTopic(topic);
                }

                Set<String> mergedSyns = new LinkedHashSet<>();
                if (rawSenseNode != null) {
                    mergedSyns.addAll(readStringList(rawSenseNode.path("synonyms")));
                }
                if (item.getSynonyms() != null) {
                    mergedSyns.addAll(item.getSynonyms());
                }
                for (String syn : mergedSyns) {
                    if (syn != null && !syn.isBlank()) {
                        sense.getSynonyms().add(SenseSynonym.builder().sense(sense).synonymWord(syn.trim()).build());
                    }
                }

                Set<String> mergedAnts = new LinkedHashSet<>();
                if (rawSenseNode != null) {
                    mergedAnts.addAll(readStringList(rawSenseNode.path("antonyms")));
                }
                if (item.getAntonyms() != null) {
                    mergedAnts.addAll(item.getAntonyms());
                }
                for (String ant : mergedAnts) {
                    if (ant != null && !ant.isBlank()) {
                        sense.getAntonyms().add(SenseAntonym.builder().sense(sense).antonymWord(ant.trim()).build());
                    }
                }

                Set<String> mergedCols = new LinkedHashSet<>();
                if (rawSenseNode != null) {
                    mergedCols.addAll(readStringList(rawSenseNode.path("collocations")));
                }
                if (item.getCollocations() != null) {
                    mergedCols.addAll(item.getCollocations());
                }
                for (String col : mergedCols) {
                    if (col != null && !col.isBlank()) {
                        sense.getCollocations().add(SenseCollocation.builder().sense(sense).collocationWord(col.trim()).build());
                    }
                }

                vocab.getSenses().add(sense);
            }
        }

        // Idioms (append only new phrases)
        if (enriched.getIdioms() != null && !enriched.getIdioms().isEmpty()) {
            Set<String> existingIdioms = new java.util.HashSet<>();
            for (Idiom existingIdm : vocab.getIdioms()) {
                if (existingIdm.getPhrase() != null) {
                    existingIdioms.add(existingIdm.getPhrase().trim().toLowerCase());
                }
            }
            for (AiEnrichedPhraseItem idm : enriched.getIdioms()) {
                if (idm.getPhrase() == null || idm.getPhrase().isBlank()) {
                    continue;
                }
                String phraseKey = idm.getPhrase().trim().toLowerCase();
                if (!existingIdioms.add(phraseKey)) {
                    continue;
                }
                vocab.getIdioms().add(Idiom.builder()
                        .vocabulary(vocab)
                        .phrase(idm.getPhrase().trim())
                        .definition(idm.getDefinitionEn())
                        .definitionVn(idm.getDefinitionVn())
                        .example(idm.getExampleEn())
                        .exampleVn(idm.getExampleVn())
                        .dataSource(resolvedSource)
                        .build());
            }
        }

        // Phrasal Verbs (append only new phrases)
        if (enriched.getPhrasalVerbs() != null && !enriched.getPhrasalVerbs().isEmpty()) {
            Set<String> existingPvs = new java.util.HashSet<>();
            for (PhrasalVerb existingPv : vocab.getPhrasalVerbs()) {
                if (existingPv.getPhrase() != null) {
                    existingPvs.add(existingPv.getPhrase().trim().toLowerCase());
                }
            }
            for (AiEnrichedPhraseItem pv : enriched.getPhrasalVerbs()) {
                if (pv.getPhrase() == null || pv.getPhrase().isBlank()) {
                    continue;
                }
                String phraseKey = pv.getPhrase().trim().toLowerCase();
                if (!existingPvs.add(phraseKey)) {
                    continue;
                }
                vocab.getPhrasalVerbs().add(PhrasalVerb.builder()
                        .vocabulary(vocab)
                        .phrase(pv.getPhrase().trim())
                        .definition(pv.getDefinitionEn())
                        .definitionVn(pv.getDefinitionVn())
                        .example(pv.getExampleEn())
                        .exampleVn(pv.getExampleVn())
                        .dataSource(resolvedSource)
                        .build());
            }
        }

        return vocabularyRepository.save(vocab);
    }

    public static List<UUID> selectDefaultSenseIds(List<VocabularySense> senses) {
        if (senses == null || senses.isEmpty()) {
            return List.of();
        }
        List<UUID> preferred = senses.stream()
                .filter(s -> s.getId() != null && s.getCefrLevel() != null
                        && AUTO_SELECT_CEFR_LEVELS.contains(s.getCefrLevel().trim().toUpperCase()))
                .map(VocabularySense::getId)
                .toList();
        if (!preferred.isEmpty()) {
            return preferred;
        }
        return senses.stream()
                .map(VocabularySense::getId)
                .filter(java.util.Objects::nonNull)
                .limit(3)
                .toList();
    }

    private static List<String> readStringList(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        for (JsonNode n : arrayNode) {
            String txt = (n.isObject() && n.has("word"))
                    ? n.path("word").asText("").trim()
                    : n.asText("").trim();
            if (!txt.isEmpty()) {
                list.add(txt);
            }
        }
        return list;
    }
}

