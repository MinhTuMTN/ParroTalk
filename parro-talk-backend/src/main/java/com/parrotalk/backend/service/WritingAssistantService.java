package com.parrotalk.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.constant.DifficultyScale;
import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.constant.WritingItemType;
import com.parrotalk.backend.constant.WritingItemValidationStatus;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.writing.GrammarValidationResultDto;
import com.parrotalk.backend.dto.writing.WritingAnalyzeRequestDto;
import com.parrotalk.backend.dto.writing.WritingSessionResponseDto;
import com.parrotalk.backend.dto.writing.WritingSessionSummaryDto;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.entity.WritingGeneratedItem;
import com.parrotalk.backend.entity.WritingSession;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.VocabularyRepository;
import com.parrotalk.backend.repository.WritingSessionRepository;
import com.parrotalk.backend.util.WordNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
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
@RequiredArgsConstructor
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
                GrammarValidationResultDto draftVal = null;
                if (existing.getUserDraft() != null && !existing.getUserDraft().isBlank()) {
                    draftVal = grammarValidationService.validateUserDraft(
                            existing.getUserDraft(), extractSuggestedTerms(existing));
                }
                return mapSessionToDto(existing, draftVal);
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
                GrammarValidationResultDto validation = grammarValidationService.validateSentence(sent.getSentenceEn());
                items.add(WritingGeneratedItem.builder()
                        .session(session)
                        .itemType(WritingItemType.COMPLETE_SENTENCE)
                        .contentEn(sent.getSentenceEn().trim())
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
     * Retrieves a specific WritingSession owned by the user.
     */
    @Transactional(readOnly = true)
    public WritingSessionResponseDto getSessionDetail(UUID sessionId, User user) {
        WritingSession session = findOwnedSession(sessionId, user);
        GrammarValidationResultDto draftValidation = null;
        if (session.getUserDraft() != null && !session.getUserDraft().isBlank()) {
            List<String> suggestedTerms = extractSuggestedTerms(session);
            draftValidation = grammarValidationService.validateUserDraft(session.getUserDraft(), suggestedTerms);
        }
        return mapSessionToDto(session, draftValidation);
    }

    /**
     * Saves and validates the learner's drafted sentence for a session.
     */
    @Transactional
    public WritingSessionResponseDto updateUserDraft(UUID sessionId, String userDraft, User user) {
        WritingSession session = findOwnedSession(sessionId, user);
        String trimmedDraft = userDraft == null ? "" : userDraft.trim();
        session.setUserDraft(trimmedDraft);

        List<String> suggestedTerms = extractSuggestedTerms(session);
        GrammarValidationResultDto validation = grammarValidationService.validateUserDraft(trimmedDraft, suggestedTerms);
        session.setGrammarCheckJson(serializeJsonSafely(validation));

        WritingSession saved = sessionRepository.save(session);
        return mapSessionToDto(saved, validation);
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
                        || i.getItemType() == WritingItemType.COLLOCATION)
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

        return WritingGeneratedItem.builder()
                .session(session)
                .itemType(itemType)
                .contentEn(text)
                .normalizedText(normalized)
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
                    case COMPLETE_SENTENCE -> completeSentences.add(
                            WritingSessionResponseDto.WritingCompleteSentenceDto.builder()
                                    .id(item.getId())
                                    .tone(item.getPartOfSpeech())
                                    .sentenceEn(item.getContentEn())
                                    .translationVn(item.getMeaningVn())
                                    .grammarBreakdownVn(item.getExplanationVn())
                                    .whyItWorksVn(item.getUsageContextVn())
                                    .validationStatus(item.getValidationStatus())
                                    .grammarCheck(grammarValidationService.validateSentence(item.getContentEn()))
                                    .build()
                    );
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
        return WritingSessionResponseDto.WritingLexicalItemDto.builder()
                .id(item.getId())
                .itemType(item.getItemType())
                .text(item.getContentEn())
                .normalizedText(item.getNormalizedText())
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

    private String serializeJsonSafely(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize Writing Assistant payload to JSON: {}", e.getMessage());
            return "{}";
        }
    }
}
