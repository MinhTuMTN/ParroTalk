package com.parrotalk.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.constant.DifficultyScale;
import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.constant.WritingItemType;
import com.parrotalk.backend.constant.WritingItemValidationStatus;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.writing.GrammarIssueDto;
import com.parrotalk.backend.dto.writing.GrammarValidationResultDto;
import com.parrotalk.backend.dto.writing.WritingAnalyzeRequestDto;
import com.parrotalk.backend.dto.writing.WritingSessionResponseDto;
import com.parrotalk.backend.dto.writing.WritingSessionSummaryDto;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularyPronunciation;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.entity.WritingGeneratedItem;
import com.parrotalk.backend.entity.WritingSession;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.VocabularyRepository;
import com.parrotalk.backend.repository.WritingSessionRepository;
import com.parrotalk.backend.service.WritingAssistantAiService.AiDraftIssue;
import com.parrotalk.backend.service.WritingAssistantAiService.AiDraftReviewPayload;
import com.parrotalk.backend.service.WritingAssistantAiService.AiUsedItemEvaluation;
import com.parrotalk.backend.util.WordNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.transaction.support.TransactionTemplate;

/**
 * Core service orchestrating Writing Assistant analysis, read-only dictionary linking,
 * grammar validation, and session history persistence.
 */
@Service
@Slf4j
public class WritingAssistantService {

    private static final Set<String> VALID_CEFR_LEVELS = Set.of("A1", "A2", "B1", "B2", "C1", "C2");
    private static final Set<String> VALID_IELTS_BANDS = Set.of("4.0", "4.5", "5.0", "5.5", "6.0", "6.5", "7.0", "7.5", "8.0");

    private final WritingAssistantAiService aiService;
    private final VocabularyRepository vocabularyRepository;
    private final WritingSessionRepository sessionRepository;
    private final WordNormalizer wordNormalizer;
    private final GrammarValidationService grammarValidationService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final WritingTtsService writingTtsService;
    @Nullable
    private final TtsPrewarmService ttsPrewarmService;

    public WritingAssistantService(
            WritingAssistantAiService aiService,
            VocabularyRepository vocabularyRepository,
            WritingSessionRepository sessionRepository,
            WordNormalizer wordNormalizer,
            GrammarValidationService grammarValidationService,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            WritingTtsService writingTtsService) {
        this(aiService, vocabularyRepository, sessionRepository, wordNormalizer,
                grammarValidationService, objectMapper, transactionTemplate, writingTtsService, null);
    }

    @Autowired
    public WritingAssistantService(
            WritingAssistantAiService aiService,
            VocabularyRepository vocabularyRepository,
            WritingSessionRepository sessionRepository,
            WordNormalizer wordNormalizer,
            GrammarValidationService grammarValidationService,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            WritingTtsService writingTtsService,
            @Autowired(required = false) @Nullable TtsPrewarmService ttsPrewarmService) {
        this.aiService = aiService;
        this.vocabularyRepository = vocabularyRepository;
        this.sessionRepository = sessionRepository;
        this.wordNormalizer = wordNormalizer;
        this.grammarValidationService = grammarValidationService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.writingTtsService = writingTtsService;
        this.ttsPrewarmService = ttsPrewarmService;
    }

    /**
     * Analyzes the learner's idea with a single AI request (or serves from existing session cache),
     * links existing official Vocabulary/Sense entries in batch (read-only), validates generated items,
     * and saves the WritingSession inside a short transaction so DB connections are never held during AI I/O.
     */
    public WritingSessionResponseDto analyzeAndSave(WritingAnalyzeRequestDto request, User user) {
        long startNs = System.nanoTime();
        if (user == null || user.getId() == null) {
            throw new ParroTalkException("Authentication is required.", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

        String normalizedLevel = normalizeAndValidateLevel(request.getScaleType(), request.getTargetLevel());
        String trimmedInput = request.getInputText().trim();

        // 1. Fast path: check if user already analyzed the exact same input + scale + level + mode
        Optional<UUID> cachedIdOpt = sessionRepository.findLatestSessionIdByInputAndLevel(
                user.getId(), trimmedInput, request.getScaleType(), normalizedLevel, request.getMode());
        if (cachedIdOpt.isPresent()) {
            UUID cachedId = cachedIdOpt.get();
            WritingSessionResponseDto cachedDto = executeInTransaction(() -> {
                WritingSession existing = sessionRepository.findByIdAndUserId(cachedId, user.getId())
                        .orElse(null);
                if (existing == null) {
                    return null;
                }
                return mapSessionToDto(existing, resolveStoredDraftValidation(existing));
            });
            if (cachedDto != null) {
                long cachedMs = (System.nanoTime() - startNs) / 1_000_000;
                log.info("Served cached WritingSession {} for user {} in {} ms", cachedId, user.getId(), cachedMs);
                return cachedDto;
            }
        }

        // 2. Call AI outside DB transaction so Hikari connection is not held during network I/O
        long aiStartNs = System.nanoTime();
        WritingAssistantAiService.AiWritingAnalysisPayload aiPayload;
        try {
            aiPayload = aiService.analyzeIdea(trimmedInput, request.getMode(), request.getScaleType(), normalizedLevel);
        } catch (Exception e) {
            log.error("Writing Assistant AI analysis failed for user {}: {}", user.getId(), e.getMessage(), e);
            throw new ParroTalkException(
                    "Không thể phân tích câu lúc này. Vui lòng thử lại sau giây lát.",
                    "AI_ANALYSIS_FAILED",
                    HttpStatus.BAD_GATEWAY
            );
        }
        long aiElapsedMs = (System.nanoTime() - aiStartNs) / 1_000_000;

        // 3. Persist and link dictionary entries in a fast, bounded DB transaction
        long dbStartNs = System.nanoTime();
        WritingSessionResponseDto responseDto = executeInTransaction(() -> {
            Map<String, Vocabulary> dictionaryMap = batchLookupVocabularies(aiPayload);

            WritingSession session = WritingSession.builder()
                    .user(user)
                    .inputText(trimmedInput)
                    .inputLanguage(aiPayload.getDetectedLanguage() != null ? aiPayload.getDetectedLanguage() : "VI")
                    .mode(request.getMode())
                    .scaleType(request.getScaleType())
                    .targetLevel(normalizedLevel)
                    .coreIdeaSummaryVn(aiPayload.getCoreIdeaSummaryVn())
                    .combinationGuideVn(aiPayload.getCombinationGuideVn())
                    .responseJson(serializeJsonSafely(aiPayload))
                    .build();

            int order = 0;
            List<WritingGeneratedItem> items = new ArrayList<>();

            for (WritingAssistantAiService.AiLexicalItem v : aiPayload.getVocabulary()) {
                String text = v.resolvedText();
                if (text.isEmpty()) continue;
                items.add(buildLexicalEntity(session, WritingItemType.VOCABULARY, v, text, dictionaryMap, order++));
            }

            for (WritingAssistantAiService.AiLexicalItem expr : aiPayload.getExpressions()) {
                String text = expr.resolvedText();
                if (text.isEmpty()) continue;
                items.add(buildLexicalEntity(session, WritingItemType.EXPRESSION, expr, text, dictionaryMap, order++));
            }

            for (WritingAssistantAiService.AiLexicalItem col : aiPayload.getCollocations()) {
                String text = col.resolvedText();
                if (text.isEmpty()) continue;
                items.add(buildLexicalEntity(session, WritingItemType.COLLOCATION, col, text, dictionaryMap, order++));
            }

            for (WritingAssistantAiService.AiPatternItem pat : aiPayload.getSentencePatterns()) {
                if (pat.getPattern() == null || pat.getPattern().isBlank()) continue;
                WritingItemValidationStatus status = grammarValidationService.validateLexicalItem(
                        pat.getPattern(), pat.getMeaningVn());
                items.add(WritingGeneratedItem.builder()
                        .session(session)
                        .itemType(WritingItemType.SENTENCE_PATTERN)
                        .contentEn(pat.getPattern().trim())
                        .meaningVn(pat.getMeaningVn())
                        .explanationVn(pat.getExplanationVn())
                        .usageContextVn(pat.getUsageContextVn())
                        .validationStatus(status)
                        .displayOrder(order++)
                        .build());
            }

            for (WritingAssistantAiService.AiCompleteSentenceItem sent : aiPayload.getCompleteSentences()) {
                if (sent.getSentenceEn() == null || sent.getSentenceEn().isBlank()) continue;
                String sentenceText = sent.getSentenceEn().trim();
                GrammarValidationResultDto validation = grammarValidationService.validateSentence(sent.getSentenceEn());
                items.add(WritingGeneratedItem.builder()
                        .session(session)
                        .itemType(WritingItemType.COMPLETE_SENTENCE)
                        .contentEn(sentenceText)
                        .audioUrl(writingTtsService.buildTtsEndpointUrl(sentenceText, "US"))
                        .partOfSpeech(sent.getTone() != null ? sent.getTone().trim() : "Natural")
                        .meaningVn(sent.getTranslationVn())
                        .explanationVn(sent.getGrammarBreakdownVn())
                        .usageContextVn(sent.getWhyItWorksVn())
                        .validationStatus(validation.getStatus())
                        .validationNotes(validation.getSummaryVn())
                        .displayOrder(order++)
                        .build());
            }

            session.setGeneratedItems(items);
            WritingSession saved = sessionRepository.save(session);
            return mapSessionToDto(saved, null);
        });
        long dbElapsedMs = (System.nanoTime() - dbStartNs) / 1_000_000;
        long totalMs = (System.nanoTime() - startNs) / 1_000_000;
        log.info("Writing Assistant analysis completed in {} ms (AI: {} ms, DB: {} ms)", totalMs, aiElapsedMs, dbElapsedMs);

        if (ttsPrewarmService != null) {
            ttsPrewarmService.prewarmSession(responseDto);
        }

        return responseDto;
    }

    private <T> T executeInTransaction(java.util.function.Supplier<T> supplier) {
        if (transactionTemplate != null) {
            return transactionTemplate.execute(status -> supplier.get());
        }
        return supplier.get();
    }

    /**
     * Returns paginated session summaries for the authenticated user.
     */
    @Transactional(readOnly = true)
    public PageResponse<WritingSessionSummaryDto> getUserSessions(User user, int page, int size) {
        if (user == null || user.getId() == null) {
            throw new ParroTalkException("Authentication is required.", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));

        Page<WritingSession> sessionPage = sessionRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(safePage, safeSize));

        List<WritingSessionSummaryDto> content = sessionPage.getContent().stream()
                .map(s -> WritingSessionSummaryDto.builder()
                        .id(s.getId())
                        .inputText(s.getInputText())
                        .inputLanguage(s.getInputLanguage())
                        .mode(s.getMode())
                        .scaleType(s.getScaleType())
                        .targetLevel(s.getTargetLevel())
                        .coreIdeaSummaryVn(s.getCoreIdeaSummaryVn())
                        .hasUserDraft(s.getUserDraft() != null && !s.getUserDraft().isBlank())
                        .createdAt(s.getCreatedAt())
                        .build())
                .toList();

        return PageResponse.<WritingSessionSummaryDto>builder()
                .content(content)
                .page(sessionPage.getNumber())
                .size(sessionPage.getSize())
                .totalElements(sessionPage.getTotalElements())
                .totalPages(sessionPage.getTotalPages())
                .build();
    }

    /**
     * Retrieves a specific WritingSession owned by the user, reusing persisted draft validation
     * when available instead of recomputing or invoking AI on read.
     */
    @Transactional(readOnly = true)
    public WritingSessionResponseDto getSessionDetail(UUID sessionId, User user) {
        WritingSession session = findOwnedSession(sessionId, user);
        return mapSessionToDto(session, resolveStoredDraftValidation(session));
    }

    /**
     * Saves and validates the learner's drafted sentence for a session using deterministic rules
     * plus AI draft review, keeping database transactions short and off the AI network call.
     */
    public WritingSessionResponseDto updateUserDraft(UUID sessionId, String userDraft, User user) {
        String normalizedDraft = userDraft == null ? "" : userDraft.trim();

        DraftReviewContext context = executeInTransaction(() -> {
            WritingSession session = findOwnedSession(sessionId, user);
            return new DraftReviewContext(
                    session.getCoreIdeaSummaryVn(),
                    session.getScaleType(),
                    session.getTargetLevel(),
                    extractSuggestedTerms(session)
            );
        });

        GrammarValidationResultDto ruleResult =
                grammarValidationService.validateUserDraft(normalizedDraft, context.suggestedTerms());

        AiDraftReviewPayload aiReview = null;
        boolean aiUnavailable = false;
        if (ruleResult.getStatus() != WritingItemValidationStatus.REJECTED && hasText(normalizedDraft)) {
            try {
                aiReview = aiService.reviewDraft(
                        normalizedDraft,
                        context.coreIdeaSummaryVn(),
                        context.scaleType(),
                        context.targetLevel(),
                        context.suggestedTerms()
                );
                if (aiReview == null) {
                    aiUnavailable = true;
                }
            } catch (Exception e) {
                aiUnavailable = true;
                log.warn("AI draft review unavailable for session {}, falling back to rule validation: {}",
                        sessionId, e.getMessage());
            }
        }

        GrammarValidationResultDto validation =
                mergeDraftValidation(normalizedDraft, ruleResult, aiReview, context.suggestedTerms(), aiUnavailable);

        return executeInTransaction(() -> {
            WritingSession session = findOwnedSession(sessionId, user);
            session.setUserDraft(normalizedDraft);
            session.setGrammarCheckJson(serializeJsonSafely(validation));
            WritingSession saved = sessionRepository.save(session);
            return mapSessionToDto(saved, validation);
        });
    }

    /**
     * Soft-deletes a session owned by the user.
     */
    @Transactional
    public void deleteSession(UUID sessionId, User user) {
        WritingSession session = findOwnedSession(sessionId, user);
        sessionRepository.delete(session);
    }

    private WritingSession findOwnedSession(UUID sessionId, User user) {
        if (user == null || user.getId() == null) {
            throw new ParroTalkException("Authentication is required.", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        return sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ParroTalkException(
                        "Không tìm thấy phiên luyện viết.",
                        "WRITING_SESSION_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));
    }

    private List<String> extractSuggestedTerms(WritingSession session) {
        if (session.getGeneratedItems() == null) {
            return List.of();
        }
        return session.getGeneratedItems().stream()
                .filter(i -> i.getItemType() == WritingItemType.VOCABULARY
                        || i.getItemType() == WritingItemType.EXPRESSION
                        || i.getItemType() == WritingItemType.COLLOCATION
                        || i.getItemType() == WritingItemType.SENTENCE_PATTERN)
                .map(WritingGeneratedItem::getContentEn)
                .filter(t -> t != null && !t.isBlank())
                .toList();
    }

    private String normalizeAndValidateLevel(DifficultyScale scaleType, String targetLevel) {
        if (scaleType == null || targetLevel == null || targetLevel.isBlank()) {
            throw new ParroTalkException(
                    "Vui lòng chọn thang trình độ (CEFR hoặc IELTS) và mức độ cụ thể.",
                    ErrorCode.VALIDATION_ERROR.name(),
                    HttpStatus.BAD_REQUEST
            );
        }
        String normalized = targetLevel.trim().toUpperCase(Locale.ROOT);
        if (scaleType == DifficultyScale.CEFR) {
            if (!VALID_CEFR_LEVELS.contains(normalized)) {
                throw new ParroTalkException(
                        "Trình độ CEFR không hợp lệ. Chỉ chấp nhận: A1, A2, B1, B2, C1, C2.",
                        ErrorCode.VALIDATION_ERROR.name(),
                        HttpStatus.BAD_REQUEST
                );
            }
            return normalized;
        } else {
            if (!VALID_IELTS_BANDS.contains(normalized)) {
                throw new ParroTalkException(
                        "Trình độ IELTS không hợp lệ. Chỉ chấp nhận từ 4.0 đến 8.0 (bước 0.5).",
                        ErrorCode.VALIDATION_ERROR.name(),
                        HttpStatus.BAD_REQUEST
                );
            }
            return normalized;
        }
    }

    private Map<String, Vocabulary> batchLookupVocabularies(WritingAssistantAiService.AiWritingAnalysisPayload payload) {
        Set<String> normalizedCandidates = new LinkedHashSet<>();
        collectNormalizedCandidates(payload.getVocabulary(), normalizedCandidates);
        collectNormalizedCandidates(payload.getExpressions(), normalizedCandidates);
        collectNormalizedCandidates(payload.getCollocations(), normalizedCandidates);

        if (normalizedCandidates.isEmpty()) {
            return Map.of();
        }

        List<Vocabulary> matchedVocabs = vocabularyRepository.findByNormalizedWordInWithSenses(normalizedCandidates);
        if (matchedVocabs == null || matchedVocabs.isEmpty()) {
            return Map.of();
        }

        Map<String, Vocabulary> result = new HashMap<>();
        for (Vocabulary v : matchedVocabs) {
            if (v.getNormalizedWord() != null) {
                result.put(v.getNormalizedWord().toLowerCase(Locale.ROOT), v);
            }
        }
        return result;
    }

    private void collectNormalizedCandidates(List<WritingAssistantAiService.AiLexicalItem> items, Set<String> target) {
        if (items == null) return;
        for (WritingAssistantAiService.AiLexicalItem item : items) {
            String normalized = wordNormalizer.normalize(item.resolvedText());
            if (!normalized.isEmpty()) {
                target.add(normalized);
            }
        }
    }

    private WritingGeneratedItem buildLexicalEntity(
            WritingSession session,
            WritingItemType itemType,
            WritingAssistantAiService.AiLexicalItem item,
            String text,
            Map<String, Vocabulary> dictionaryMap,
            int order) {

        String normalized = wordNormalizer.normalize(text);
        Vocabulary linkedVocab = dictionaryMap.get(normalized);
        VocabularySense linkedSense = findBestMatchingSense(linkedVocab, item.getPartOfSpeech());
        WritingItemValidationStatus status = grammarValidationService.validateLexicalItem(text, item.getMeaningVn());

        String phonetic = item.getPhonetic();
        String audioUrl;

        DictionaryPronunciations pron = extractDictionaryPronunciations(linkedVocab);

        if (hasText(pron.usIpa())) {
            phonetic = pron.usIpa();
        } else if (!hasText(phonetic)) {
            phonetic = hasText(pron.ukIpa()) ? pron.ukIpa() : pron.fallbackIpa();
        }

        if (hasText(pron.usAudioUrl())) {
            audioUrl = pron.usAudioUrl();
        } else if (hasText(pron.ukAudioUrl())) {
            audioUrl = pron.ukAudioUrl();
        } else {
            audioUrl = pron.fallbackAudioUrl();
        }

        if (!hasText(audioUrl)) {
            audioUrl = writingTtsService.buildTtsEndpointUrl(text, "US");
        }

        return WritingGeneratedItem.builder()
                .session(session)
                .itemType(itemType)
                .contentEn(text)
                .normalizedText(normalized)
                .phonetic(phonetic)
                .audioUrl(audioUrl)
                .partOfSpeech(item.getPartOfSpeech())
                .cefrLevel(item.getCefrLevel())
                .meaningVn(item.getMeaningVn())
                .explanationVn(item.getExplanationVn())
                .usageContextVn(item.getUsageContextVn())
                .exampleEn(item.getExampleEn())
                .exampleVn(item.getExampleVn())
                .linkedVocabulary(linkedVocab)
                .linkedSense(linkedSense)
                .validationStatus(status)
                .displayOrder(order)
                .build();
    }

    private VocabularySense findBestMatchingSense(Vocabulary vocab, String partOfSpeech) {
        if (vocab == null || vocab.getSenses() == null || vocab.getSenses().isEmpty()) {
            return null;
        }
        if (partOfSpeech != null && !partOfSpeech.isBlank()) {
            String targetPos = partOfSpeech.trim().toLowerCase(Locale.ROOT);
            for (VocabularySense sense : vocab.getSenses()) {
                if (sense.getPartOfSpeech() != null
                        && sense.getPartOfSpeech().trim().toLowerCase(Locale.ROOT).contains(targetPos)) {
                    return sense;
                }
            }
        }
        return vocab.getSenses().get(0);
    }

    private WritingSessionResponseDto mapSessionToDto(
            WritingSession session,
            GrammarValidationResultDto draftValidation) {

        List<WritingSessionResponseDto.WritingLexicalItemDto> vocabulary = new ArrayList<>();
        List<WritingSessionResponseDto.WritingLexicalItemDto> expressions = new ArrayList<>();
        List<WritingSessionResponseDto.WritingLexicalItemDto> collocations = new ArrayList<>();
        List<WritingSessionResponseDto.WritingPatternItemDto> sentencePatterns = new ArrayList<>();
        List<WritingSessionResponseDto.WritingCompleteSentenceDto> completeSentences = new ArrayList<>();

        if (session.getGeneratedItems() != null) {
            for (WritingGeneratedItem item : session.getGeneratedItems()) {
                switch (item.getItemType()) {
                    case VOCABULARY -> vocabulary.add(mapLexicalDto(item));
                    case EXPRESSION -> expressions.add(mapLexicalDto(item));
                    case COLLOCATION -> collocations.add(mapLexicalDto(item));
                    case SENTENCE_PATTERN -> sentencePatterns.add(
                            WritingSessionResponseDto.WritingPatternItemDto.builder()
                                    .id(item.getId())
                                    .pattern(item.getContentEn())
                                    .meaningVn(item.getMeaningVn())
                                    .explanationVn(item.getExplanationVn())
                                    .usageContextVn(item.getUsageContextVn())
                                    .validationStatus(item.getValidationStatus())
                                    .build()
                    );
                    case COMPLETE_SENTENCE -> {
                        String audioUsUrl = hasText(item.getAudioUrl())
                                ? item.getAudioUrl()
                                : writingTtsService.buildTtsEndpointUrl(item.getContentEn(), "US");
                        String audioUkUrl = writingTtsService.buildTtsEndpointUrl(item.getContentEn(), "UK");
                        completeSentences.add(
                                WritingSessionResponseDto.WritingCompleteSentenceDto.builder()
                                        .id(item.getId())
                                        .tone(item.getPartOfSpeech())
                                        .sentenceEn(item.getContentEn())
                                        .translationVn(item.getMeaningVn())
                                        .grammarBreakdownVn(item.getExplanationVn())
                                        .whyItWorksVn(item.getUsageContextVn())
                                        .audioUsUrl(audioUsUrl)
                                        .audioUkUrl(audioUkUrl)
                                        .validationStatus(item.getValidationStatus())
                                        .grammarCheck(buildPersistedSentenceGrammarCheck(item))
                                        .build()
                        );
                    }
                }
            }
        }

        return WritingSessionResponseDto.builder()
                .id(session.getId())
                .inputText(session.getInputText())
                .inputLanguage(session.getInputLanguage())
                .mode(session.getMode())
                .scaleType(session.getScaleType())
                .targetLevel(session.getTargetLevel())
                .coreIdeaSummaryVn(session.getCoreIdeaSummaryVn())
                .combinationGuideVn(session.getCombinationGuideVn())
                .userDraft(session.getUserDraft())
                .draftValidation(draftValidation)
                .createdAt(session.getCreatedAt())
                .vocabulary(vocabulary)
                .expressions(expressions)
                .collocations(collocations)
                .sentencePatterns(sentencePatterns)
                .completeSentences(completeSentences)
                .build();
    }

    private WritingSessionResponseDto.WritingLexicalItemDto mapLexicalDto(WritingGeneratedItem item) {
        Vocabulary linkedVocab = item.getLinkedVocabulary();
        VocabularySense linkedSense = item.getLinkedSense();

        DictionaryPronunciations pron = extractDictionaryPronunciations(linkedVocab);

        String phonetic = item.getPhonetic();
        if (!hasText(phonetic)) {
            phonetic = hasText(pron.usIpa()) ? pron.usIpa()
                    : (hasText(pron.ukIpa()) ? pron.ukIpa() : pron.fallbackIpa());
        }

        String audioUrl = item.getAudioUrl();
        String audioUsUrl = pron.usAudioUrl();
        String audioUkUrl = pron.ukAudioUrl();

        if (!hasText(audioUsUrl)) {
            audioUsUrl = writingTtsService.buildTtsEndpointUrl(item.getContentEn(), "US");
        }
        if (!hasText(audioUkUrl)) {
            audioUkUrl = writingTtsService.buildTtsEndpointUrl(item.getContentEn(), "UK");
        }
        if (!hasText(audioUrl)) {
            audioUrl = hasText(pron.fallbackAudioUrl())
                    ? pron.fallbackAudioUrl()
                    : (hasText(audioUsUrl) ? audioUsUrl : audioUkUrl);
        }

        String exampleAudioUsUrl = hasText(item.getExampleEn())
                ? writingTtsService.buildTtsEndpointUrl(item.getExampleEn(), "US")
                : null;
        String exampleAudioUkUrl = hasText(item.getExampleEn())
                ? writingTtsService.buildTtsEndpointUrl(item.getExampleEn(), "UK")
                : null;

        return WritingSessionResponseDto.WritingLexicalItemDto.builder()
                .id(item.getId())
                .itemType(item.getItemType())
                .text(item.getContentEn())
                .normalizedText(item.getNormalizedText())
                .phonetic(phonetic)
                .audioUrl(audioUrl)
                .audioUsUrl(audioUsUrl)
                .audioUkUrl(audioUkUrl)
                .exampleAudioUsUrl(exampleAudioUsUrl)
                .exampleAudioUkUrl(exampleAudioUkUrl)
                .partOfSpeech(item.getPartOfSpeech())
                .cefrLevel(item.getCefrLevel())
                .meaningVn(item.getMeaningVn())
                .explanationVn(item.getExplanationVn())
                .usageContextVn(item.getUsageContextVn())
                .exampleEn(item.getExampleEn())
                .exampleVn(item.getExampleVn())
                .inDictionary(linkedVocab != null)
                .linkedVocabularyId(linkedVocab != null ? linkedVocab.getId() : null)
                .linkedSenseId(linkedSense != null ? linkedSense.getId() : null)
                .dictionaryWord(linkedVocab != null ? linkedVocab.getWord() : null)
                .validationStatus(item.getValidationStatus())
                .build();
    }

    private GrammarValidationResultDto buildPersistedSentenceGrammarCheck(WritingGeneratedItem item) {
        WritingItemValidationStatus status = item.getValidationStatus() != null
                ? item.getValidationStatus()
                : WritingItemValidationStatus.APPROVED;
        return GrammarValidationResultDto.builder()
                .valid(status != WritingItemValidationStatus.REJECTED)
                .status(status)
                .summaryVn(item.getValidationNotes())
                .build();
    }

    private GrammarValidationResultDto resolveStoredDraftValidation(WritingSession session) {
        if (session == null || !hasText(session.getUserDraft())) {
            return null;
        }
        GrammarValidationResultDto persisted = deserializeGrammarCheckSafely(session.getGrammarCheckJson());
        if (persisted != null && persisted.getStatus() != null) {
            return persisted;
        }
        return grammarValidationService.validateUserDraft(session.getUserDraft(), extractSuggestedTerms(session));
    }

    private GrammarValidationResultDto mergeDraftValidation(
            String draft,
            GrammarValidationResultDto ruleResult,
            AiDraftReviewPayload aiReview,
            List<String> suggestedTerms,
            boolean aiUnavailable) {
        if (aiReview == null) {
            List<GrammarIssueDto> ruleIssues = ruleResult.getIssues() != null
                    ? new ArrayList<>(ruleResult.getIssues())
                    : new ArrayList<>();
            String baseSummary = ruleResult.getSummaryVn() != null ? ruleResult.getSummaryVn() : "";
            String finalSummary = aiUnavailable && hasText(draft)
                    ? (baseSummary.isBlank() ? "AI review unavailable." : baseSummary + " (AI review unavailable.)")
                    : baseSummary;

            return GrammarValidationResultDto.builder()
                    .valid(ruleResult.isValid())
                    .status(ruleResult.getStatus())
                    .summaryVn(finalSummary)
                    .issues(ruleIssues)
                    .correctedSentence(ruleResult.getCorrectedSentence())
                    .usedTerms(ruleResult.getUsedTerms())
                    .missingTerms(ruleResult.getMissingTerms())
                    .score(ruleResult.getScore())
                    .build();
        }

        List<GrammarIssueDto> aiIssues = new ArrayList<>();
        if (aiReview.getIssues() != null) {
            for (AiDraftIssue aiIssue : aiReview.getIssues()) {
                if (aiIssue == null) {
                    continue;
                }
                int[] span = locateAiIssueSpan(draft, aiIssue.getOriginal(), aiIssues);
                String issueType = normalizeAiIssueType(aiIssue.getType());
                String explanation = hasText(aiIssue.getExplanationVn())
                        ? aiIssue.getExplanationVn().trim()
                        : "Cần chỉnh sửa ngữ pháp hoặc diễn đạt cho tự nhiên hơn.";
                List<String> replacements = hasText(aiIssue.getSuggestion())
                        ? List.of(aiIssue.getSuggestion().trim())
                        : List.of();

                aiIssues.add(GrammarIssueDto.builder()
                        .ruleId("AI_" + issueType)
                        .type(issueType)
                        .message(hasText(aiIssue.getOriginal()) ? aiIssue.getOriginal().trim() : explanation)
                        .messageVn(explanation)
                        .offset(span[0])
                        .length(span[1])
                        .replacements(replacements)
                        .build());
            }
        }

        List<GrammarIssueDto> mergedIssues = new ArrayList<>(aiIssues);
        if (ruleResult.getIssues() != null) {
            for (GrammarIssueDto ruleIssue : ruleResult.getIssues()) {
                // Only deduplicate deterministic rule issues against located AI issues (never against each other).
                if (!overlapsExistingIssue(ruleIssue, aiIssues, draft.length())) {
                    mergedIssues.add(ruleIssue);
                }
            }
        }

        boolean hasBlockingIssue = mergedIssues.stream()
                .anyMatch(issue -> "GRAMMAR".equalsIgnoreCase(issue.getType())
                        || "SPELLING".equalsIgnoreCase(issue.getType()));
        boolean rejectedByLength = ruleResult.getStatus() == WritingItemValidationStatus.REJECTED;
        boolean valid = !rejectedByLength && !hasBlockingIssue;
        WritingItemValidationStatus status = rejectedByLength
                ? WritingItemValidationStatus.REJECTED
                : (valid ? WritingItemValidationStatus.APPROVED : WritingItemValidationStatus.NEEDS_REVIEW);

        MergedTerms mergedTerms = mergeUsedAndMissingTerms(
                suggestedTerms, ruleResult, aiReview.getUsedItems());

        String summaryVn = hasText(aiReview.getOverallVn())
                ? aiReview.getOverallVn().trim()
                : ruleResult.getSummaryVn();

        String correctedSentence = hasText(aiReview.getCorrectedSentence())
                ? aiReview.getCorrectedSentence().trim()
                : (valid ? draft : null);

        return GrammarValidationResultDto.builder()
                .valid(valid)
                .status(status)
                .summaryVn(summaryVn)
                .issues(mergedIssues)
                .correctedSentence(correctedSentence)
                .usedTerms(mergedTerms.usedTerms())
                .missingTerms(mergedTerms.missingTerms())
                .score(aiReview.getScore())
                .build();
    }

    private String normalizeAiIssueType(String type) {
        if (!hasText(type)) {
            return "GRAMMAR";
        }
        return switch (type.trim().toUpperCase(Locale.ROOT)) {
            case "GRAMMAR", "SPELLING", "WORD_CHOICE", "PUNCTUATION" -> type.trim().toUpperCase(Locale.ROOT);
            default -> "GRAMMAR";
        };
    }

    private int[] locateAiIssueSpan(String draft, String original, List<GrammarIssueDto> alreadyMapped) {
        if (!hasText(draft) || !hasText(original)) {
            return new int[]{0, 0};
        }
        String needle = original.trim();
        int idx = findNextNonCollidingOccurrence(draft, needle, alreadyMapped, false);
        if (idx < 0) {
            idx = findNextNonCollidingOccurrence(draft, needle, alreadyMapped, true);
        }
        if (idx >= 0) {
            return new int[]{idx, Math.min(needle.length(), draft.length() - idx)};
        }
        return new int[]{0, 0};
    }

    private int findNextNonCollidingOccurrence(
            String haystack,
            String needle,
            List<GrammarIssueDto> alreadyMapped,
            boolean ignoreCase) {
        String source = ignoreCase ? haystack.toLowerCase(Locale.ROOT) : haystack;
        String target = ignoreCase ? needle.toLowerCase(Locale.ROOT) : needle;
        int fromIndex = 0;
        int firstMatch = -1;
        while (fromIndex <= source.length() - target.length()) {
            int found = source.indexOf(target, fromIndex);
            if (found < 0) {
                break;
            }
            if (firstMatch < 0) {
                firstMatch = found;
            }
            int end = found + target.length();
            boolean overlaps = false;
            for (GrammarIssueDto existing : alreadyMapped) {
                int exStart = existing.getOffset();
                int exEnd = exStart + existing.getLength();
                if (found < exEnd && exStart < end) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                return found;
            }
            fromIndex = found + 1;
        }
        return firstMatch;
    }

    private boolean overlapsExistingIssue(
            GrammarIssueDto ruleIssue,
            List<GrammarIssueDto> existingIssues,
            int draftLength) {
        int rStart = ruleIssue.getOffset();
        int rEnd = rStart + Math.max(1, ruleIssue.getLength());
        for (GrammarIssueDto existing : existingIssues) {
            // Unlocated AI diagnostics (length <= 0) carry draft-level guidance without an exact span; never suppress rules.
            if (existing.getLength() <= 0) {
                continue;
            }
            if ("TERMINAL_PUNCTUATION".equals(ruleIssue.getRuleId())
                    && "PUNCTUATION".equalsIgnoreCase(existing.getType())
                    && existing.getOffset() + existing.getLength() >= Math.max(0, draftLength - 2)) {
                return true;
            }
            int eStart = existing.getOffset();
            int eEnd = eStart + Math.max(1, existing.getLength());
            if (rStart < eEnd && eStart < rEnd
                    && (existing.getType() == null
                    || ruleIssue.getType() == null
                    || existing.getType().equalsIgnoreCase(ruleIssue.getType())
                    || "REPEATED_WORD".equals(ruleIssue.getRuleId()))) {
                return true;
            }
        }
        return false;
    }

    private MergedTerms mergeUsedAndMissingTerms(
            List<String> suggestedTerms,
            GrammarValidationResultDto ruleResult,
            List<AiUsedItemEvaluation> aiUsedItems) {
        Set<String> usedLower = new LinkedHashSet<>();
        if (ruleResult.getUsedTerms() != null) {
            for (String term : ruleResult.getUsedTerms()) {
                if (hasText(term)) {
                    usedLower.add(term.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        if (aiUsedItems != null) {
            for (AiUsedItemEvaluation eval : aiUsedItems) {
                if (eval != null && hasText(eval.getText()) && Boolean.TRUE.equals(eval.getUsed())) {
                    usedLower.add(eval.getText().trim().toLowerCase(Locale.ROOT));
                }
            }
        }

        List<String> usedTerms = new ArrayList<>();
        List<String> missingTerms = new ArrayList<>();
        Set<String> seenSuggested = new LinkedHashSet<>();
        if (suggestedTerms != null) {
            for (String rawTerm : suggestedTerms) {
                if (!hasText(rawTerm)) {
                    continue;
                }
                String trimmed = rawTerm.trim();
                String lower = trimmed.toLowerCase(Locale.ROOT);
                if (!seenSuggested.add(lower)) {
                    continue;
                }
                if (usedLower.contains(lower)) {
                    usedTerms.add(trimmed);
                } else {
                    missingTerms.add(trimmed);
                }
            }
        }
        return new MergedTerms(usedTerms, missingTerms);
    }

    private DictionaryPronunciations extractDictionaryPronunciations(Vocabulary vocab) {
        String usAudioUrl = null;
        String ukAudioUrl = null;
        String fallbackAudioUrl = null;
        String usIpa = null;
        String ukIpa = null;
        String fallbackIpa = null;

        if (vocab != null && vocab.getPronunciations() != null) {
            for (VocabularyPronunciation p : vocab.getPronunciations()) {
                String accent = normalizePronunciationAccent(p.getAccent());
                if ("US".equals(accent)) {
                    if (usIpa == null && hasText(p.getIpa())) {
                        usIpa = p.getIpa();
                    }
                    if (usAudioUrl == null && hasText(p.getAudioUrl())) {
                        usAudioUrl = p.getAudioUrl();
                    }
                } else if ("UK".equals(accent)) {
                    if (ukIpa == null && hasText(p.getIpa())) {
                        ukIpa = p.getIpa();
                    }
                    if (ukAudioUrl == null && hasText(p.getAudioUrl())) {
                        ukAudioUrl = p.getAudioUrl();
                    }
                } else {
                    if (fallbackIpa == null && hasText(p.getIpa())) {
                        fallbackIpa = p.getIpa();
                    }
                    if (fallbackAudioUrl == null && hasText(p.getAudioUrl())) {
                        fallbackAudioUrl = p.getAudioUrl();
                    }
                }
            }
        }

        return new DictionaryPronunciations(usAudioUrl, ukAudioUrl, fallbackAudioUrl, usIpa, ukIpa, fallbackIpa);
    }

    private String normalizePronunciationAccent(String accent) {
        if (!hasText(accent)) {
            return "OTHER";
        }
        return switch (accent.trim().toUpperCase(Locale.ROOT)) {
            case "US", "EN-US", "EN_US", "AMERICAN" -> "US";
            case "UK", "GB", "EN-GB", "EN_GB", "BRITISH" -> "UK";
            default -> "OTHER";
        };
    }

    private GrammarValidationResultDto deserializeGrammarCheckSafely(String json) {
        if (!hasText(json) || "{}".equals(json.trim())) {
            return null;
        }
        try {
            return objectMapper.readValue(json, GrammarValidationResultDto.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize persisted grammarCheckJson: {}", e.getMessage());
            return null;
        }
    }

    private record DraftReviewContext(
            String coreIdeaSummaryVn,
            DifficultyScale scaleType,
            String targetLevel,
            List<String> suggestedTerms) {
    }

    private record MergedTerms(List<String> usedTerms, List<String> missingTerms) {
    }

    private record DictionaryPronunciations(
            String usAudioUrl,
            String ukAudioUrl,
            String fallbackAudioUrl,
            String usIpa,
            String ukIpa,
            String fallbackIpa) {
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String serializeJsonSafely(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize Writing Assistant payload to JSON: {}", e.getMessage());
            return "{}";
        }
    }
}
