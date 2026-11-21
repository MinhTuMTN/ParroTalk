package com.parrotalk.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.constant.McqQuestionCategory;
import com.parrotalk.backend.constant.QuestionGenerationSource;
import com.parrotalk.backend.constant.UserNotificationType;
import com.parrotalk.backend.constant.VocabularyListStatus;
import com.parrotalk.backend.entity.GrammarMultipleChoiceQuestion;
import com.parrotalk.backend.entity.Idiom;
import com.parrotalk.backend.entity.PhrasalVerb;
import com.parrotalk.backend.entity.SenseAntonym;
import com.parrotalk.backend.entity.SenseCollocation;
import com.parrotalk.backend.entity.SenseSynonym;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularyList;
import com.parrotalk.backend.entity.VocabularyListItem;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.repository.GrammarMultipleChoiceQuestionRepository;
import com.parrotalk.backend.repository.IdiomRepository;
import com.parrotalk.backend.repository.PhrasalVerbRepository;
import com.parrotalk.backend.repository.VocabularyListItemRepository;
import com.parrotalk.backend.repository.VocabularyListRepository;
import com.parrotalk.backend.repository.VocabularySenseRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomListQuestionBatchService {

    private static final int MIN_QUESTIONS_PER_SENSE = 5;
    private static final int MIN_QUESTIONS_PER_PHRASE = 2;
    private static final ExecutorService BATCH_EXECUTOR = Executors.newFixedThreadPool(1);

    @Lazy
    @Autowired
    private CustomListQuestionBatchService self;

    private static final String MCQ_SYSTEM_PROMPT = """
            You are a senior Cambridge English assessment designer creating high-quality 4-option multiple-choice questions for Vietnamese learners.
            Follow the requested question categories strictly:
            - CONTEXTUAL_CLOZE: Fill the blank (`____`) in a natural, context-rich sentence testing the exact target sense.
            - CONFUSING_NUANCE: Test subtle lexical distinction against 3 near-synonyms or easily confused words.
            - WORD_FORM: Test morphological word family (noun/verb/adjective/adverb forms of the headword).
            - GRAMMAR_PATTERN: Test grammatical structure tied to the word's part of speech (verb patterns to-V/V-ing, tense/agreement, countable/uncountable quantifiers, prepositions).
            - COLLOCATION_PREPOSITION: Test natural collocation or preposition pairing.
            - SYNONYM_IN_CONTEXT: Present a full sentence with the target word highlighted as **word**, and ask which option is CLOSEST in meaning in that context.
            - ANTONYM_IN_CONTEXT: Present a full sentence with the target word highlighted as **word**, and ask which option is OPPOSITE in meaning in that context.
            - IDIOM_PHRASAL_VERB: Test the target idiom or phrasal verb in realistic communication (particle completion or contextual meaning).

            Requirements:
            1. Each question MUST have `options` as an array of 4 distinct strings, and `correctAnswer` MUST exactly match one option.
            2. `difficulty` MUST be one of: A1, A2, B1, B2, C1, C2.
            3. Provide clear `explanationEn` and natural Vietnamese `explanationVn` explaining why the correct answer is right and distractors are wrong.
            4. NEVER duplicate any sentence from EXISTING QUESTIONS.
            5. Output ONLY valid JSON matching `{ "questions": [ ... ] }` with no markdown fences.
            """;

    private final AiCoreClient aiCoreClient;
    private final GrammarMultipleChoiceQuestionRepository mcqRepository;
    private final VocabularyListRepository listRepository;
    private final VocabularyListItemRepository listItemRepository;
    private final VocabularySenseRepository senseRepository;
    private final IdiomRepository idiomRepository;
    private final PhrasalVerbRepository phrasalVerbRepository;
    private final UserNotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiMcqItem {
        private String questionCategory;
        private String grammarTopic;
        private String difficulty;
        private String questionText;
        private List<String> options;
        private String correctAnswer;
        private String completedSentence;
        private String explanationEn;
        private String explanationVn;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiMcqBatchResponse {
        private List<AiMcqItem> questions;
    }

    public Set<McqQuestionCategory> determineRequiredCategories(VocabularySense sense) {
        Set<McqQuestionCategory> categories = new LinkedHashSet<>();
        categories.add(McqQuestionCategory.CONTEXTUAL_CLOZE);
        categories.add(McqQuestionCategory.CONFUSING_NUANCE);
        categories.add(McqQuestionCategory.WORD_FORM);
        categories.add(McqQuestionCategory.GRAMMAR_PATTERN);

        if (sense.getCollocations() != null && !sense.getCollocations().isEmpty()) {
            categories.add(McqQuestionCategory.COLLOCATION_PREPOSITION);
        }
        if (sense.getSynonyms() != null && !sense.getSynonyms().isEmpty()) {
            categories.add(McqQuestionCategory.SYNONYM_IN_CONTEXT);
        }
        if (sense.getAntonyms() != null && !sense.getAntonyms().isEmpty()) {
            categories.add(McqQuestionCategory.ANTONYM_IN_CONTEXT);
        }
        return categories;
    }

    private CustomListQuestionBatchService proxy() {
        return self != null ? self : this;
    }

    public void triggerFinalizeBatchAsync(UUID listId, UUID userId) {
        CompletableFuture.runAsync(() -> runFinalizeBatchSync(listId, userId), BATCH_EXECUTOR)
                .exceptionally(ex -> {
                    log.error("Unhandled async error finalizing custom list {}: {}", listId, ex.getMessage(), ex);
                    return null;
                });
    }

    public int runFinalizeBatchSync(UUID listId, UUID userId) {
        try (var ignored = com.parrotalk.backend.ai.AiUsageContext.open(
                userId, com.parrotalk.backend.ai.AiFeature.CUSTOM_LIST_QUESTION_BATCH)) {
            return doRunFinalizeBatchSync(listId, userId);
        }
    }

    private int doRunFinalizeBatchSync(UUID listId, UUID userId) {
        List<Set<UUID>> targets = proxy().loadFinalizeTargetsAndMarkGenerating(listId);
        if (targets == null) {
            return 0;
        }
        Set<UUID> selectedSenseIds = targets.get(0);
        Set<UUID> selectedIdiomIds = targets.get(1);
        Set<UUID> selectedPvIds = targets.get(2);

        int totalGenerated = 0;
        boolean hadError = false;

        for (UUID senseId : selectedSenseIds) {
            try {
                int generated = proxy().processSenseFinalize(listId, userId, senseId);
                totalGenerated += generated;
                pauseBetweenBatchCalls(generated);
            } catch (Exception ex) {
                hadError = true;
                log.error("Failed finalizing sense {} for list {}: {}", senseId, listId, ex.getMessage(), ex);
            }
        }

        for (UUID idiomId : selectedIdiomIds) {
            try {
                int generated = proxy().processIdiomFinalize(listId, userId, idiomId);
                totalGenerated += generated;
                pauseBetweenBatchCalls(generated);
            } catch (Exception ex) {
                hadError = true;
                log.error("Failed finalizing idiom {} for list {}: {}", idiomId, listId, ex.getMessage(), ex);
            }
        }

        for (UUID pvId : selectedPvIds) {
            try {
                int generated = proxy().processPhrasalVerbFinalize(listId, userId, pvId);
                totalGenerated += generated;
                pauseBetweenBatchCalls(generated);
            } catch (Exception ex) {
                hadError = true;
                log.error("Failed finalizing phrasal verb {} for list {}: {}", pvId, listId, ex.getMessage(), ex);
            }
        }

        proxy().completeFinalizeStatusAndNotify(listId, totalGenerated, hadError);
        return totalGenerated;
    }

    private void pauseBetweenBatchCalls(int generated) {
        if (generated <= 0) {
            return;
        }
        try {
            Thread.sleep(500L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    public List<Set<UUID>> loadFinalizeTargetsAndMarkGenerating(UUID listId) {
        VocabularyList list = listRepository.findById(listId).orElse(null);
        if (list == null) {
            return null;
        }
        list.setStatus(VocabularyListStatus.GENERATING_QUESTIONS);
        listRepository.save(list);

        List<VocabularyListItem> items = listItemRepository.findByVocabularyListIdOrderByDisplayOrderAscCreatedAtAsc(listId);
        Set<UUID> selectedSenseIds = new LinkedHashSet<>();
        Set<UUID> selectedIdiomIds = new LinkedHashSet<>();
        Set<UUID> selectedPvIds = new LinkedHashSet<>();

        for (VocabularyListItem item : items) {
            selectedSenseIds.addAll(item.getSelectedSenseIds());
            selectedIdiomIds.addAll(item.getSelectedIdiomIds());
            selectedPvIds.addAll(item.getSelectedPhrasalVerbIds());
        }
        return List.of(selectedSenseIds, selectedIdiomIds, selectedPvIds);
    }

    @Transactional
    public int processSenseFinalize(UUID listId, UUID userId, UUID senseId) {
        long existingCount = mcqRepository.countBySenseId(senseId);
        if (existingCount >= MIN_QUESTIONS_PER_SENSE) {
            return 0;
        }
        VocabularySense sense = senseRepository.findById(senseId).orElse(null);
        if (sense == null) {
            return 0;
        }
        int needed = (int) Math.max(1, MIN_QUESTIONS_PER_SENSE - existingCount);
        return generateForSenseInternal(sense, needed, QuestionGenerationSource.USER_LIST_FINALIZE_AI, listId, userId);
    }

    @Transactional
    public int processIdiomFinalize(UUID listId, UUID userId, UUID idiomId) {
        long existingCount = mcqRepository.countByIdiomId(idiomId);
        if (existingCount >= MIN_QUESTIONS_PER_PHRASE) {
            return 0;
        }
        Idiom idiom = idiomRepository.findById(idiomId).orElse(null);
        if (idiom == null) {
            return 0;
        }
        int needed = (int) Math.max(1, MIN_QUESTIONS_PER_PHRASE - existingCount);
        return generateForIdiomInternal(idiom, needed, QuestionGenerationSource.USER_LIST_FINALIZE_AI, listId, userId);
    }

    @Transactional
    public int processPhrasalVerbFinalize(UUID listId, UUID userId, UUID pvId) {
        long existingCount = mcqRepository.countByPhrasalVerbId(pvId);
        if (existingCount >= MIN_QUESTIONS_PER_PHRASE) {
            return 0;
        }
        PhrasalVerb pv = phrasalVerbRepository.findById(pvId).orElse(null);
        if (pv == null) {
            return 0;
        }
        int needed = (int) Math.max(1, MIN_QUESTIONS_PER_PHRASE - existingCount);
        return generateForPhrasalVerbInternal(pv, needed, QuestionGenerationSource.USER_LIST_FINALIZE_AI, listId, userId);
    }

    @Transactional
    public void completeFinalizeStatusAndNotify(UUID listId, int totalGenerated, boolean hadError) {
        VocabularyList list = listRepository.findById(listId).orElse(null);
        if (list == null) {
            return;
        }
        list.setStatus(hadError && totalGenerated == 0 ? VocabularyListStatus.PARTIAL_ERROR : VocabularyListStatus.ACTIVE);
        listRepository.save(list);

        if (list.getOwner() != null) {
            notificationService.notifyUser(
                    list.getOwner(),
                    UserNotificationType.LIST_QUESTIONS_READY,
                    "Bộ câu hỏi cho danh sách '" + list.getTitle() + "' đã sẵn sàng!",
                    "Hệ thống đã chuẩn bị xong ngân hàng câu hỏi trắc nghiệm (tạo mới " + totalGenerated + " câu hỏi AI) cho danh sách của bạn.",
                    list.getId(),
                    Map.of(
                            "listId", list.getId().toString(),
                            "generatedCount", totalGenerated,
                            "hadError", hadError,
                            "generationSource", QuestionGenerationSource.USER_LIST_FINALIZE_AI.name()
                    )
            );
        }
    }

    @Transactional
    public int generateMoreForSenseSync(UUID listId, UUID userId, UUID senseId, int count) {
        VocabularySense sense = senseRepository.findById(senseId).orElse(null);
        if (sense == null) {
            return 0;
        }
        int bounded = Math.max(1, Math.min(count, 6));
        return generateForSenseInternal(
                sense,
                bounded,
                QuestionGenerationSource.USER_LIST_MORE_AI,
                listId,
                userId
        );
    }

    @Transactional
    public int generateMoreForIdiomSync(UUID listId, UUID userId, UUID idiomId, int count) {
        Idiom idiom = idiomRepository.findById(idiomId).orElse(null);
        if (idiom == null) {
            return 0;
        }
        int bounded = Math.max(1, Math.min(count, 4));
        return generateForIdiomInternal(
                idiom,
                bounded,
                QuestionGenerationSource.USER_LIST_MORE_AI,
                listId,
                userId
        );
    }

    @Transactional
    public int generateMoreForPhrasalVerbSync(UUID listId, UUID userId, UUID pvId, int count) {
        PhrasalVerb pv = phrasalVerbRepository.findById(pvId).orElse(null);
        if (pv == null) {
            return 0;
        }
        int bounded = Math.max(1, Math.min(count, 4));
        return generateForPhrasalVerbInternal(
                pv,
                bounded,
                QuestionGenerationSource.USER_LIST_MORE_AI,
                listId,
                userId
        );
    }

    private int generateForSenseInternal(
            VocabularySense sense,
            int countToRequest,
            QuestionGenerationSource generationSource,
            UUID listId,
            UUID userId) {
        Vocabulary vocab = sense.getVocabulary();
        List<GrammarMultipleChoiceQuestion> existing = sense.getId() != null
                ? mcqRepository.findBySenseIdIn(List.of(sense.getId()))
                : List.of();

        Set<McqQuestionCategory> requiredCategories = determineRequiredCategories(sense);
        List<String> existingSentences = existing.stream()
                .map(GrammarMultipleChoiceQuestion::getCompletedSentence)
                .filter(s -> s != null && !s.isBlank())
                .limit(12)
                .toList();

        List<String> syns = sense.getSynonyms() != null
                ? sense.getSynonyms().stream().map(SenseSynonym::getSynonymWord).toList()
                : List.of();
        List<String> ants = sense.getAntonyms() != null
                ? sense.getAntonyms().stream().map(SenseAntonym::getAntonymWord).toList()
                : List.of();
        List<String> cols = sense.getCollocations() != null
                ? sense.getCollocations().stream().map(SenseCollocation::getCollocationWord).toList()
                : List.of();

        String userPrompt = String.format("""
                Target Word: %s
                Word Forms (if any): %s
                Part of Speech: %s
                CEFR Level: %s
                Sense Definition (EN): %s
                Sense Definition (VN): %s
                Example (EN): %s
                Synonyms: %s
                Antonyms: %s
                Collocations: %s
                Requested Categories to Cover: %s
                Count to Generate: %d
                EXISTING QUESTIONS (DO NOT DUPLICATE): %s
                """,
                vocab != null ? vocab.getWord() : "",
                vocab != null && vocab.getWordFormsJson() != null ? vocab.getWordFormsJson() : "[]",
                sense.getPartOfSpeech(),
                sense.getCefrLevel() != null ? sense.getCefrLevel() : "B1",
                sense.getDefinitionEn(),
                sense.getDefinitionVn() != null ? sense.getDefinitionVn() : "",
                sense.getExampleEn() != null ? sense.getExampleEn() : "",
                syns,
                ants,
                cols,
                requiredCategories,
                countToRequest,
                existingSentences
        );

        try {
            AiMcqBatchResponse response = com.parrotalk.backend.ai.AiUsageContext.withFeature(
                userId,
                com.parrotalk.backend.ai.AiFeature.CUSTOM_LIST_QUESTION_BATCH,
                () -> aiCoreClient.callAndValidate(
                    MCQ_SYSTEM_PROMPT,
                    userPrompt,
                    AiMcqBatchResponse.class,
                    raw -> {
                        if (raw == null || raw.getQuestions() == null || raw.getQuestions().isEmpty()) {
                            throw new IllegalArgumentException("AI returned empty MCQ batch");
                        }
                        return raw;
                    }
            ));
            return persistQuestions(
                    response.getQuestions(),
                    vocab,
                    sense,
                    null,
                    null,
                    generationSource,
                    listId,
                    userId
            );
        } catch (Exception ex) {
            log.warn("AI MCQ generation failed for sense {}: {}", sense.getId(), ex.getMessage());
            return 0;
        }
    }

    private int generateForIdiomInternal(
            Idiom idiom,
            int countToRequest,
            QuestionGenerationSource generationSource,
            UUID listId,
            UUID userId) {
        Vocabulary vocab = idiom.getVocabulary();
        List<GrammarMultipleChoiceQuestion> existing = idiom.getId() != null
                ? mcqRepository.findByIdiomIdIn(List.of(idiom.getId()))
                : List.of();

        String userPrompt = String.format("""
                Target Idiom: %s
                Headword: %s
                Definition (EN): %s
                Definition (VN): %s
                Example: %s
                Requested Category: IDIOM_PHRASAL_VERB
                Count to Generate: %d
                EXISTING QUESTIONS (DO NOT DUPLICATE): %s
                """,
                idiom.getPhrase(),
                vocab != null ? vocab.getWord() : "",
                idiom.getDefinition(),
                idiom.getDefinitionVn() != null ? idiom.getDefinitionVn() : "",
                idiom.getExample() != null ? idiom.getExample() : "",
                countToRequest,
                existing.stream().map(GrammarMultipleChoiceQuestion::getCompletedSentence).toList()
        );

        try {
            AiMcqBatchResponse response = com.parrotalk.backend.ai.AiUsageContext.withFeature(
                userId,
                com.parrotalk.backend.ai.AiFeature.CUSTOM_LIST_QUESTION_BATCH,
                () -> aiCoreClient.callAndValidate(
                    MCQ_SYSTEM_PROMPT,
                    userPrompt,
                    AiMcqBatchResponse.class,
                    raw -> {
                        if (raw == null || raw.getQuestions() == null || raw.getQuestions().isEmpty()) {
                            throw new IllegalArgumentException("AI returned empty idiom MCQ batch");
                        }
                        return raw;
                    }
            ));
            return persistQuestions(
                    response.getQuestions(),
                    vocab,
                    null,
                    idiom,
                    null,
                    generationSource,
                    listId,
                    userId
            );
        } catch (Exception ex) {
            log.warn("AI MCQ generation failed for idiom {}: {}", idiom.getId(), ex.getMessage());
            return 0;
        }
    }

    private int generateForPhrasalVerbInternal(
            PhrasalVerb pv,
            int countToRequest,
            QuestionGenerationSource generationSource,
            UUID listId,
            UUID userId) {
        Vocabulary vocab = pv.getVocabulary();
        List<GrammarMultipleChoiceQuestion> existing = pv.getId() != null
                ? mcqRepository.findByPhrasalVerbIdIn(List.of(pv.getId()))
                : List.of();

        String userPrompt = String.format("""
                Target Phrasal Verb: %s
                Headword: %s
                Definition (EN): %s
                Definition (VN): %s
                Example: %s
                Requested Category: IDIOM_PHRASAL_VERB
                Count to Generate: %d
                EXISTING QUESTIONS (DO NOT DUPLICATE): %s
                """,
                pv.getPhrase(),
                vocab != null ? vocab.getWord() : "",
                pv.getDefinition(),
                pv.getDefinitionVn() != null ? pv.getDefinitionVn() : "",
                pv.getExample() != null ? pv.getExample() : "",
                countToRequest,
                existing.stream().map(GrammarMultipleChoiceQuestion::getCompletedSentence).toList()
        );

        try {
            AiMcqBatchResponse response = com.parrotalk.backend.ai.AiUsageContext.withFeature(
                userId,
                com.parrotalk.backend.ai.AiFeature.CUSTOM_LIST_QUESTION_BATCH,
                () -> aiCoreClient.callAndValidate(
                    MCQ_SYSTEM_PROMPT,
                    userPrompt,
                    AiMcqBatchResponse.class,
                    raw -> {
                        if (raw == null || raw.getQuestions() == null || raw.getQuestions().isEmpty()) {
                            throw new IllegalArgumentException("AI returned empty phrasal verb MCQ batch");
                        }
                        return raw;
                    }
            ));
            return persistQuestions(
                    response.getQuestions(),
                    vocab,
                    null,
                    null,
                    pv,
                    generationSource,
                    listId,
                    userId
            );
        } catch (Exception ex) {
            log.warn("AI MCQ generation failed for phrasal verb {}: {}", pv.getId(), ex.getMessage());
            return 0;
        }
    }

    private int persistQuestions(
            List<AiMcqItem> items,
            Vocabulary vocab,
            VocabularySense sense,
            Idiom idiom,
            PhrasalVerb pv,
            QuestionGenerationSource generationSource,
            UUID listId,
            UUID userId) {
        int savedCount = 0;
        for (AiMcqItem item : items) {
            if (item.getQuestionText() == null || item.getQuestionText().isBlank()
                    || item.getOptions() == null || item.getOptions().size() < 4
                    || item.getCorrectAnswer() == null || item.getCorrectAnswer().isBlank()) {
                continue;
            }
            List<String> options = item.getOptions().subList(0, 4);
            int correctIdx = options.indexOf(item.getCorrectAnswer().trim());
            if (correctIdx < 0) {
                for (int i = 0; i < options.size(); i++) {
                    if (options.get(i).equalsIgnoreCase(item.getCorrectAnswer().trim())) {
                        correctIdx = i;
                        break;
                    }
                }
            }
            if (correctIdx < 0) {
                continue;
            }

            String completed = (item.getCompletedSentence() != null && !item.getCompletedSentence().isBlank())
                    ? item.getCompletedSentence().trim()
                    : item.getQuestionText().replace("____", options.get(correctIdx)).trim();

            String contentHash = computeContentHash(completed, options.get(correctIdx));
            if (mcqRepository.existsByContentHash(contentHash)) {
                continue;
            }

            McqQuestionCategory category = resolveCategory(item.getQuestionCategory(), idiom != null || pv != null);
            String difficulty = resolveDifficulty(item.getDifficulty(), sense != null ? sense.getCefrLevel() : "B2");

            try {
                String optionsJson = objectMapper.writeValueAsString(options);
                GrammarMultipleChoiceQuestion entity = GrammarMultipleChoiceQuestion.builder()
                        .vocabulary(vocab)
                        .sense(sense)
                        .idiom(idiom)
                        .phrasalVerb(pv)
                        .grammarTopic(item.getGrammarTopic() != null && !item.getGrammarTopic().isBlank()
                                ? item.getGrammarTopic().trim()
                                : category.name().toLowerCase())
                        .questionCategory(category)
                        .generationSource(generationSource)
                        .difficulty(difficulty)
                        .questionText(item.getQuestionText().trim())
                        .optionsJson(optionsJson)
                        .correctIndex((short) correctIdx)
                        .correctAnswer(options.get(correctIdx))
                        .completedSentence(completed)
                        .explanationEn(item.getExplanationEn() != null ? item.getExplanationEn().trim() : completed)
                        .explanationVn(item.getExplanationVn() != null ? item.getExplanationVn().trim() : "")
                        .contentHash(contentHash)
                        .sourceListId(listId)
                        .createdByUserId(userId)
                        .build();

                proxy().saveSingleMcq(entity);
                savedCount++;
            } catch (Exception ex) {
                log.warn("Skipping invalid or duplicate MCQ item: {}", ex.getMessage());
            }
        }
        return savedCount;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSingleMcq(GrammarMultipleChoiceQuestion entity) {
        mcqRepository.save(entity);
    }

    private static McqQuestionCategory resolveCategory(String raw, boolean isPhrase) {
        if (isPhrase) {
            return McqQuestionCategory.IDIOM_PHRASAL_VERB;
        }
        if (raw != null && !raw.isBlank()) {
            try {
                return McqQuestionCategory.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return McqQuestionCategory.CONTEXTUAL_CLOZE;
    }

    private static String resolveDifficulty(String raw, String fallbackCefr) {
        Set<String> valid = Set.of("A1", "A2", "B1", "B2", "C1", "C2");
        if (raw != null && valid.contains(raw.trim().toUpperCase())) {
            return raw.trim().toUpperCase();
        }
        if (fallbackCefr != null && valid.contains(fallbackCefr.trim().toUpperCase())) {
            return fallbackCefr.trim().toUpperCase();
        }
        return "B1";
    }

    public static String computeContentHash(String completedSentence, String correctAnswer) {
        try {
            String payload = completedSentence.trim().toLowerCase() + "::" + correctAnswer.trim().toLowerCase();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}

