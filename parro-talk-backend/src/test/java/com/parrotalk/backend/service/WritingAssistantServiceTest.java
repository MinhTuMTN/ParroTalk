package com.parrotalk.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.constant.DifficultyScale;
import com.parrotalk.backend.constant.WritingItemType;
import com.parrotalk.backend.constant.WritingItemValidationStatus;
import com.parrotalk.backend.constant.WritingMode;
import com.parrotalk.backend.dto.writing.WritingAnalyzeRequestDto;
import com.parrotalk.backend.dto.writing.WritingSessionResponseDto;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularyPronunciation;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.entity.WritingGeneratedItem;
import com.parrotalk.backend.entity.WritingSession;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.VocabularyRepository;
import com.parrotalk.backend.repository.WritingSessionRepository;
import com.parrotalk.backend.util.WordNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WritingAssistantServiceTest {

    @Mock
    private WritingAssistantAiService aiService;

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private WritingSessionRepository sessionRepository;

    private WordNormalizer wordNormalizer;
    private GrammarValidationService grammarValidationService;
    private ObjectMapper objectMapper;
    private WritingTtsService writingTtsService;
    private WritingAssistantService writingService;

    private User testUser;

    @BeforeEach
    void setUp() {
        wordNormalizer = new WordNormalizer();
        grammarValidationService = new GrammarValidationService();
        objectMapper = new ObjectMapper();
        writingTtsService = new WritingTtsService();
        writingService = new WritingAssistantService(
                aiService,
                vocabularyRepository,
                sessionRepository,
                wordNormalizer,
                grammarValidationService,
                objectMapper,
                null,
                writingTtsService
        );

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("learner@parrotalk.fun")
                .fullName("Nguyen Van A")
                .build();
    }

    @Test
    @DisplayName("analyzeAndSave links existing Vocabulary/Sense in batch and saves session without mutating Vocabulary DB")
    void testAnalyzeAndSave_LinksExistingVocabularyAndPersistsSession() {
        UUID vocabId = UUID.randomUUID();
        UUID senseId = UUID.randomUUID();
        VocabularySense existingSense = VocabularySense.builder()
                .id(senseId)
                .partOfSpeech("verb")
                .cefrLevel("C1")
                .definitionEn("to weaken or damage something")
                .definitionVn("làm suy giảm")
                .build();
        VocabularyPronunciation pronUs = VocabularyPronunciation.builder()
                .accent("US")
                .ipa("/ɪmˈpeər/")
                .audioUrl("https://audio.parrotalk.fun/us/impair.mp3")
                .build();
        Vocabulary existingVocab = Vocabulary.builder()
                .id(vocabId)
                .word("impair")
                .normalizedWord("impair")
                .senses(List.of(existingSense))
                .pronunciations(List.of(pronUs))
                .build();
        existingSense.setVocabulary(existingVocab);

        WritingAssistantAiService.AiWritingAnalysisPayload payload =
                WritingAssistantAiService.AiWritingAnalysisPayload.builder()
                        .detectedLanguage("VI")
                        .coreIdeaSummaryVn("Người say rượu không được lái xe.")
                        .combinationGuideVn("Kết hợp bằng because.")
                        .vocabulary(List.of(
                                WritingAssistantAiService.AiLexicalItem.builder()
                                        .word("Impair")
                                        .partOfSpeech("verb")
                                        .cefrLevel("C1")
                                        .meaningVn("làm suy giảm")
                                        .explanationVn("Dùng cho thị lực, sự tập trung")
                                        .usageContextVn("Formal")
                                        .exampleEn("Alcohol can impair your vision.")
                                        .exampleVn("Rượu có thể làm giảm tầm nhìn của bạn.")
                                        .build(),
                                WritingAssistantAiService.AiLexicalItem.builder()
                                        .word("intoxicated")
                                        .phonetic("/ɪnˈtɒk.sɪ.keɪ.tɪd/")
                                        .partOfSpeech("adjective")
                                        .cefrLevel("C1")
                                        .meaningVn("say xỉn")
                                        .explanationVn("Từ trang trọng thay cho drunk")
                                        .build()
                        ))
                        .expressions(List.of(
                                WritingAssistantAiService.AiLexicalItem.builder()
                                        .phrase("drive under the influence")
                                        .cefrLevel("B2")
                                        .meaningVn("lái xe khi có cồn")
                                        .build()
                        ))
                        .collocations(List.of(
                                WritingAssistantAiService.AiLexicalItem.builder()
                                        .phrase("blood alcohol concentration")
                                        .cefrLevel("B2")
                                        .meaningVn("nồng độ cồn trong máu")
                                        .build()
                        ))
                        .sentencePatterns(List.of(
                                WritingAssistantAiService.AiPatternItem.builder()
                                        .pattern("[S] + must not + [V] + because + [Clause].")
                                        .meaningVn("Không được làm gì vì...")
                                        .explanationVn("Cấu trúc nguyên nhân kết quả.")
                                        .build()
                        ))
                        .completeSentences(List.of(
                                WritingAssistantAiService.AiCompleteSentenceItem.builder()
                                        .tone("Natural")
                                        .sentenceEn("Intoxicated people must not drive because high blood alcohol concentration impairs their vision and focus.")
                                        .translationVn("Người say rượu không được lái xe vì nồng độ cồn cao trong máu làm giảm tầm nhìn và sự tập trung.")
                                        .grammarBreakdownVn("Mệnh đề chính + because + mệnh đề phụ.")
                                        .whyItWorksVn("Tự nhiên và chính xác.")
                                        .build()
                        ))
                        .build();

        when(aiService.analyzeIdea(anyString(), eq(WritingMode.SUGGESTION), eq(DifficultyScale.CEFR), eq("B2")))
                .thenReturn(payload);
        when(vocabularyRepository.findByNormalizedWordInWithSenses(anyCollection()))
                .thenReturn(List.of(existingVocab));
        when(sessionRepository.save(any(WritingSession.class)))
                .thenAnswer(inv -> {
                    WritingSession s = inv.getArgument(0);
                    s.setId(UUID.randomUUID());
                    return s;
                });

        WritingAnalyzeRequestDto request = WritingAnalyzeRequestDto.builder()
                .inputText("Người say rượu không được lái xe vì nồng độ cồn trong máu làm giảm tầm nhìn, giảm sự tập trung.")
                .mode(WritingMode.SUGGESTION)
                .scaleType(DifficultyScale.CEFR)
                .targetLevel("B2")
                .build();

        WritingSessionResponseDto response = writingService.analyzeAndSave(request, testUser);

        assertNotNull(response);
        assertEquals(2, response.getVocabulary().size());

        // "Impair" should be linked to existing Vocabulary and Sense
        WritingSessionResponseDto.WritingLexicalItemDto impairDto = response.getVocabulary().get(0);
        assertTrue(impairDto.isInDictionary());
        assertEquals(vocabId, impairDto.getLinkedVocabularyId());
        assertEquals(senseId, impairDto.getLinkedSenseId());
        assertEquals("impair", impairDto.getDictionaryWord());
        assertEquals(WritingItemValidationStatus.APPROVED, impairDto.getValidationStatus());
        assertEquals("/ɪmˈpeər/", impairDto.getPhonetic());
        assertEquals("https://audio.parrotalk.fun/us/impair.mp3", impairDto.getAudioUrl());
        assertEquals("https://audio.parrotalk.fun/us/impair.mp3", impairDto.getAudioUsUrl());
        assertEquals("/api/writing/tts?text=Impair&accent=UK", impairDto.getAudioUkUrl());
        assertEquals("/api/writing/tts?text=Alcohol+can+impair+your+vision.&accent=US", impairDto.getExampleAudioUsUrl());
        assertEquals("/api/writing/tts?text=Alcohol+can+impair+your+vision.&accent=UK", impairDto.getExampleAudioUkUrl());

        // "intoxicated" is not in dictionary -> inDictionary = false, never saved to VocabularyRepository, falls back to TTS
        WritingSessionResponseDto.WritingLexicalItemDto intoxicatedDto = response.getVocabulary().get(1);
        assertFalse(intoxicatedDto.isInDictionary());
        assertNull(intoxicatedDto.getLinkedVocabularyId());
        assertEquals("/ɪnˈtɒk.sɪ.keɪ.tɪd/", intoxicatedDto.getPhonetic());
        assertEquals("/api/writing/tts?text=intoxicated&accent=US", intoxicatedDto.getAudioUrl());
        assertEquals("/api/writing/tts?text=intoxicated&accent=US", intoxicatedDto.getAudioUsUrl());
        assertEquals("/api/writing/tts?text=intoxicated&accent=UK", intoxicatedDto.getAudioUkUrl());
        assertNull(intoxicatedDto.getExampleAudioUsUrl());
        assertNull(intoxicatedDto.getExampleAudioUkUrl());

        // Expressions, collocations, and complete sentences also receive TTS URLs
        assertEquals("/api/writing/tts?text=drive+under+the+influence&accent=US",
                response.getExpressions().get(0).getAudioUsUrl());
        assertEquals("/api/writing/tts?text=drive+under+the+influence&accent=UK",
                response.getExpressions().get(0).getAudioUkUrl());
        assertEquals("/api/writing/tts?text=blood+alcohol+concentration&accent=US",
                response.getCollocations().get(0).getAudioUsUrl());
        assertEquals("/api/writing/tts?text=blood+alcohol+concentration&accent=UK",
                response.getCollocations().get(0).getAudioUkUrl());

        assertEquals(1, response.getCompleteSentences().size());
        WritingSessionResponseDto.WritingCompleteSentenceDto completeSentenceDto = response.getCompleteSentences().get(0);
        assertEquals(
                "/api/writing/tts?text=Intoxicated+people+must+not+drive+because+high+blood+alcohol+concentration+impairs+their+vision+and+focus.&accent=US",
                completeSentenceDto.getAudioUsUrl());
        assertEquals(
                "/api/writing/tts?text=Intoxicated+people+must+not+drive+because+high+blood+alcohol+concentration+impairs+their+vision+and+focus.&accent=UK",
                completeSentenceDto.getAudioUkUrl());

        // Verify official VocabularyRepository is NEVER written to
        verify(vocabularyRepository, never()).save(any());
        verify(vocabularyRepository, never()).saveAll(any());

        ArgumentCaptor<WritingSession> sessionCaptor = ArgumentCaptor.forClass(WritingSession.class);
        verify(sessionRepository, times(1)).save(sessionCaptor.capture());
        WritingSession savedSession = sessionCaptor.getValue();
        assertEquals(6, savedSession.getGeneratedItems().size());
        assertTrue(savedSession.getGeneratedItems().stream()
                .anyMatch(i -> i.getItemType() == WritingItemType.COMPLETE_SENTENCE
                        && i.getValidationStatus() == WritingItemValidationStatus.APPROVED
                        && i.getAudioUrl() != null));
        assertTrue(savedSession.getGeneratedItems().stream()
                .anyMatch(i -> "intoxicated".equals(i.getContentEn())
                        && "/api/writing/tts?text=intoxicated&accent=US".equals(i.getAudioUrl())));
    }

    @Test
    @DisplayName("Invalid targetLevel for scaleType throws ParroTalkException 400")
    void testAnalyzeAndSave_InvalidDifficultyLevel_ThrowsBadRequest() {
        WritingAnalyzeRequestDto invalidCefr = WritingAnalyzeRequestDto.builder()
                .inputText("Người say rượu không được lái xe.")
                .mode(WritingMode.GUIDANCE)
                .scaleType(DifficultyScale.CEFR)
                .targetLevel("9.0") // invalid for CEFR
                .build();

        assertThrows(ParroTalkException.class, () -> writingService.analyzeAndSave(invalidCefr, testUser));

        WritingAnalyzeRequestDto invalidIelts = WritingAnalyzeRequestDto.builder()
                .inputText("Người say rượu không được lái xe.")
                .mode(WritingMode.GUIDANCE)
                .scaleType(DifficultyScale.IELTS)
                .targetLevel("B2") // invalid for IELTS
                .build();

        assertThrows(ParroTalkException.class, () -> writingService.analyzeAndSave(invalidIelts, testUser));
    }

    @Test
    @DisplayName("updateUserDraft validates learner draft against session vocabulary and updates session")
    void testUpdateUserDraft_ValidatesAndSavesDraft() {
        UUID sessionId = UUID.randomUUID();
        WritingSession session = WritingSession.builder()
                .id(sessionId)
                .user(testUser)
                .inputText("Người say rượu không được lái xe.")
                .inputLanguage("VI")
                .mode(WritingMode.SUGGESTION)
                .scaleType(DifficultyScale.IELTS)
                .targetLevel("6.5")
                .responseJson("{}")
                .build();

        when(sessionRepository.findByIdAndUserId(sessionId, testUser.getId()))
                .thenReturn(Optional.of(session));
        when(sessionRepository.save(any(WritingSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        WritingSessionResponseDto updated = writingService.updateUserDraft(
                sessionId,
                "Drunk drivers should not drive because alcohol impairs their vision.",
                testUser
        );

        assertNotNull(updated);
        assertEquals("Drunk drivers should not drive because alcohol impairs their vision.", updated.getUserDraft());
        assertNotNull(updated.getDraftValidation());
        assertTrue(updated.getDraftValidation().isValid());
        assertEquals(WritingItemValidationStatus.APPROVED, updated.getDraftValidation().getStatus());
    }

    @Test
    @DisplayName("analyzeAndSave returns cached session immediately without calling AI when identical input and level exist")
    void testAnalyzeAndSave_ServesFromCacheWhenIdenticalInputAndLevelExist() {
        UUID cachedId = UUID.randomUUID();
        WritingSession cachedSession = WritingSession.builder()
                .id(cachedId)
                .user(testUser)
                .inputText("Người say rượu không được lái xe.")
                .inputLanguage("VI")
                .mode(WritingMode.SUGGESTION)
                .scaleType(DifficultyScale.CEFR)
                .targetLevel("B2")
                .coreIdeaSummaryVn("Cấm lái xe khi say.")
                .responseJson("{}")
                .build();

        when(sessionRepository.findLatestSessionIdByInputAndLevel(
                eq(testUser.getId()),
                eq("Người say rượu không được lái xe."),
                eq(DifficultyScale.CEFR),
                eq("B2"),
                eq(WritingMode.SUGGESTION)
        )).thenReturn(Optional.of(cachedId));
        when(sessionRepository.findByIdAndUserId(cachedId, testUser.getId()))
                .thenReturn(Optional.of(cachedSession));

        WritingAnalyzeRequestDto request = WritingAnalyzeRequestDto.builder()
                .inputText("Người say rượu không được lái xe.")
                .mode(WritingMode.SUGGESTION)
                .scaleType(DifficultyScale.CEFR)
                .targetLevel("B2")
                .build();

        WritingSessionResponseDto response = writingService.analyzeAndSave(request, testUser);

        assertNotNull(response);
        assertEquals(cachedId, response.getId());
        assertEquals(WritingMode.SUGGESTION, response.getMode());
        verify(aiService, never()).analyzeIdea(anyString(), any(), any(), anyString());
    }

    @Test
    @DisplayName("getSessionDetail populates TTS fallback URLs for legacy items with null audioUrl")
    void testGetSessionDetail_LegacyNullAudio_PopulatesTtsFallbackUrls() {
        UUID sessionId = UUID.randomUUID();
        WritingGeneratedItem legacyWord = WritingGeneratedItem.builder()
                .id(UUID.randomUUID())
                .itemType(WritingItemType.VOCABULARY)
                .contentEn("mitigate")
                .exampleEn("Trees mitigate urban heat.")
                .audioUrl(null)
                .validationStatus(WritingItemValidationStatus.APPROVED)
                .build();
        WritingGeneratedItem legacySentence = WritingGeneratedItem.builder()
                .id(UUID.randomUUID())
                .itemType(WritingItemType.COMPLETE_SENTENCE)
                .contentEn("We should mitigate traffic congestion.")
                .partOfSpeech("Formal / IELTS Writing")
                .audioUrl(null)
                .validationStatus(WritingItemValidationStatus.APPROVED)
                .build();
        WritingSession session = WritingSession.builder()
                .id(sessionId)
                .user(testUser)
                .inputText("Giảm tắc đường.")
                .inputLanguage("VI")
                .mode(WritingMode.GUIDANCE)
                .scaleType(DifficultyScale.IELTS)
                .targetLevel("6.5")
                .generatedItems(List.of(legacyWord, legacySentence))
                .build();

        when(sessionRepository.findByIdAndUserId(sessionId, testUser.getId()))
                .thenReturn(Optional.of(session));

        WritingSessionResponseDto detail = writingService.getSessionDetail(sessionId, testUser);

        assertEquals(1, detail.getVocabulary().size());
        WritingSessionResponseDto.WritingLexicalItemDto wordDto = detail.getVocabulary().get(0);
        assertEquals("/api/writing/tts?text=mitigate&accent=US", wordDto.getAudioUsUrl());
        assertEquals("/api/writing/tts?text=mitigate&accent=UK", wordDto.getAudioUkUrl());
        assertEquals("/api/writing/tts?text=Trees+mitigate+urban+heat.&accent=US", wordDto.getExampleAudioUsUrl());
        assertEquals("/api/writing/tts?text=Trees+mitigate+urban+heat.&accent=UK", wordDto.getExampleAudioUkUrl());

        assertEquals(1, detail.getCompleteSentences().size());
        WritingSessionResponseDto.WritingCompleteSentenceDto sentDto = detail.getCompleteSentences().get(0);
        assertEquals("/api/writing/tts?text=We+should+mitigate+traffic+congestion.&accent=US", sentDto.getAudioUsUrl());
        assertEquals("/api/writing/tts?text=We+should+mitigate+traffic+congestion.&accent=UK", sentDto.getAudioUkUrl());
    }

    @Test
    @DisplayName("analyzeAndSave inspects UK pronunciation for fallback phonetic even when US entry has audio without IPA")
    void testAnalyzeAndSave_UsLacksIpaFallsBackToUkIpa() {
        VocabularyPronunciation pronUsNoIpa = VocabularyPronunciation.builder()
                .accent("US")
                .ipa(null)
                .audioUrl("https://audio.parrotalk.fun/us/resilient.mp3")
                .build();
        VocabularyPronunciation pronUkWithIpa = VocabularyPronunciation.builder()
                .accent("UK")
                .ipa("/rɪˈzɪl.i.ənt/")
                .audioUrl("https://audio.parrotalk.fun/uk/resilient.mp3")
                .build();
        Vocabulary vocab = Vocabulary.builder()
                .id(UUID.randomUUID())
                .word("resilient")
                .normalizedWord("resilient")
                .pronunciations(List.of(pronUsNoIpa, pronUkWithIpa))
                .build();

        WritingAssistantAiService.AiWritingAnalysisPayload payload =
                WritingAssistantAiService.AiWritingAnalysisPayload.builder()
                        .detectedLanguage("VI")
                        .coreIdeaSummaryVn("Kiên cường")
                        .vocabulary(List.of(
                                WritingAssistantAiService.AiLexicalItem.builder()
                                        .word("resilient")
                                        .phonetic(null)
                                        .partOfSpeech("adjective")
                                        .cefrLevel("C1")
                                        .meaningVn("kiên cường")
                                        .build()
                        ))
                        .build();

        when(aiService.analyzeIdea(anyString(), eq(WritingMode.SUGGESTION), eq(DifficultyScale.CEFR), eq("C1")))
                .thenReturn(payload);
        when(vocabularyRepository.findByNormalizedWordInWithSenses(anyCollection()))
                .thenReturn(List.of(vocab));
        when(sessionRepository.save(any(WritingSession.class)))
                .thenAnswer(inv -> {
                    WritingSession s = inv.getArgument(0);
                    s.setId(UUID.randomUUID());
                    return s;
                });

        WritingAnalyzeRequestDto request = WritingAnalyzeRequestDto.builder()
                .inputText("Kiên cường vượt qua khó khăn")
                .mode(WritingMode.SUGGESTION)
                .scaleType(DifficultyScale.CEFR)
                .targetLevel("C1")
                .build();

        WritingSessionResponseDto response = writingService.analyzeAndSave(request, testUser);

        assertEquals(1, response.getVocabulary().size());
        WritingSessionResponseDto.WritingLexicalItemDto dto = response.getVocabulary().get(0);
        assertEquals("/rɪˈzɪl.i.ənt/", dto.getPhonetic());
        assertEquals("https://audio.parrotalk.fun/us/resilient.mp3", dto.getAudioUrl());
        assertEquals("https://audio.parrotalk.fun/us/resilient.mp3", dto.getAudioUsUrl());
        assertEquals("https://audio.parrotalk.fun/uk/resilient.mp3", dto.getAudioUkUrl());
    }

    @Test
    @DisplayName("updateUserDraft integrates AI grammar review issues, offsets, score, correctedSentence, and used/missing terms")
    void testUpdateUserDraft_IntegratesAiReviewAndOffsets() {
        UUID sessionId = UUID.randomUUID();
        WritingGeneratedItem patternItem = WritingGeneratedItem.builder()
                .id(UUID.randomUUID())
                .itemType(WritingItemType.SENTENCE_PATTERN)
                .contentEn("[Subject] + go to school + [Time].")
                .build();
        WritingGeneratedItem vocabItem = WritingGeneratedItem.builder()
                .id(UUID.randomUUID())
                .itemType(WritingItemType.VOCABULARY)
                .contentEn("yesterday")
                .build();
        WritingGeneratedItem unusedCollocation = WritingGeneratedItem.builder()
                .id(UUID.randomUUID())
                .itemType(WritingItemType.COLLOCATION)
                .contentEn("public transport")
                .build();
        WritingSession session = WritingSession.builder()
                .id(sessionId)
                .user(testUser)
                .inputText("Cô ấy đã đi học hôm qua.")
                .inputLanguage("VI")
                .mode(WritingMode.SUGGESTION)
                .scaleType(DifficultyScale.CEFR)
                .targetLevel("A2")
                .coreIdeaSummaryVn("Đi học hôm qua")
                .generatedItems(List.of(vocabItem, unusedCollocation, patternItem))
                .build();

        when(sessionRepository.findByIdAndUserId(sessionId, testUser.getId()))
                .thenReturn(Optional.of(session));
        when(sessionRepository.save(any(WritingSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(aiService.reviewDraft(
                eq("She go to school yesterday."),
                eq("Đi học hôm qua"),
                eq(DifficultyScale.CEFR),
                eq("A2"),
                argThat(list -> list != null
                        && list.contains("yesterday")
                        && list.contains("public transport")
                        && list.contains("[Subject] + go to school + [Time]."))
        )).thenReturn(WritingAssistantAiService.AiDraftReviewPayload.builder()
                .correctedSentence("She went to school yesterday.")
                .score(68)
                .overallVn("Động từ 'go' cần chia quá khứ thành 'went'.")
                .issues(List.of(
                        WritingAssistantAiService.AiDraftIssue.builder()
                                .type("GRAMMAR")
                                .original("go")
                                .suggestion("went")
                                .explanationVn("Dùng quá khứ đơn 'went' với 'yesterday'.")
                                .build()
                ))
                .usedItems(List.of(
                        WritingAssistantAiService.AiUsedItemEvaluation.builder()
                                .text("[Subject] + go to school + [Time].")
                                .used(true)
                                .correctUsage(false)
                                .note("Cần chia 'go' -> 'went'.")
                                .build()
                ))
                .build());

        WritingSessionResponseDto updated = writingService.updateUserDraft(
                sessionId,
                "She go to school yesterday.",
                testUser
        );

        assertNotNull(updated.getDraftValidation());
        assertFalse(updated.getDraftValidation().isValid());
        assertEquals(WritingItemValidationStatus.NEEDS_REVIEW, updated.getDraftValidation().getStatus());
        assertEquals("She went to school yesterday.", updated.getDraftValidation().getCorrectedSentence());
        assertEquals(68, updated.getDraftValidation().getScore());
        assertTrue(updated.getDraftValidation().getUsedTerms().contains("yesterday"));
        assertTrue(updated.getDraftValidation().getUsedTerms().contains("[Subject] + go to school + [Time]."));
        assertTrue(updated.getDraftValidation().getMissingTerms().contains("public transport"));
        assertFalse(updated.getDraftValidation().getIssues().isEmpty());
        assertEquals(4, updated.getDraftValidation().getIssues().get(0).getOffset());
        assertEquals(2, updated.getDraftValidation().getIssues().get(0).getLength());
        assertEquals("GRAMMAR", updated.getDraftValidation().getIssues().get(0).getType());
    }

    @Test
    @DisplayName("updateUserDraft falls back cleanly to rule-only validation when AI throws")
    void testUpdateUserDraft_AiFailureFallsBackWithoutThrowing() {
        UUID sessionId = UUID.randomUUID();
        WritingSession session = WritingSession.builder()
                .id(sessionId)
                .user(testUser)
                .inputText("Lái xe an toàn")
                .inputLanguage("VI")
                .mode(WritingMode.SUGGESTION)
                .scaleType(DifficultyScale.CEFR)
                .targetLevel("B1")
                .build();

        when(sessionRepository.findByIdAndUserId(sessionId, testUser.getId()))
                .thenReturn(Optional.of(session));
        when(sessionRepository.save(any(WritingSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(aiService.reviewDraft(anyString(), any(), any(), any(), anyList()))
                .thenThrow(new RuntimeException("OpenAI timeout"));

        WritingSessionResponseDto updated = writingService.updateUserDraft(
                sessionId,
                "People should drive safely.",
                testUser
        );

        assertNotNull(updated.getDraftValidation());
        assertTrue(updated.getDraftValidation().isValid());
        assertEquals(WritingItemValidationStatus.APPROVED, updated.getDraftValidation().getStatus());
        assertTrue(updated.getDraftValidation().getSummaryVn().contains("AI review unavailable"));
    }

    @Test
    @DisplayName("getSessionDetail and cached analyzeAndSave deserialize persisted grammarCheckJson without calling AI")
    void testReadPaths_DeserializePersistedGrammarCheckJson() throws Exception {
        UUID sessionId = UUID.randomUUID();
        com.parrotalk.backend.dto.writing.GrammarValidationResultDto persistedValidation =
                com.parrotalk.backend.dto.writing.GrammarValidationResultDto.builder()
                        .valid(false)
                        .status(WritingItemValidationStatus.NEEDS_REVIEW)
                        .correctedSentence("She went to school yesterday.")
                        .score(72)
                        .usedTerms(List.of("yesterday"))
                        .missingTerms(List.of("school bus"))
                        .summaryVn("Persisted AI feedback.")
                        .build();
        WritingSession session = WritingSession.builder()
                .id(sessionId)
                .user(testUser)
                .inputText("Cô ấy đã đi học.")
                .inputLanguage("VI")
                .mode(WritingMode.SUGGESTION)
                .scaleType(DifficultyScale.CEFR)
                .targetLevel("A2")
                .userDraft("She go to school yesterday.")
                .grammarCheckJson(objectMapper.writeValueAsString(persistedValidation))
                .build();

        when(sessionRepository.findByIdAndUserId(sessionId, testUser.getId()))
                .thenReturn(Optional.of(session));
        when(sessionRepository.findLatestSessionIdByInputAndLevel(
                testUser.getId(), "Cô ấy đã đi học.", DifficultyScale.CEFR, "A2", WritingMode.SUGGESTION))
                .thenReturn(Optional.of(sessionId));

        WritingSessionResponseDto detail = writingService.getSessionDetail(sessionId, testUser);
        assertEquals(72, detail.getDraftValidation().getScore());
        assertEquals("She went to school yesterday.", detail.getDraftValidation().getCorrectedSentence());
        assertFalse(detail.getDraftValidation().isValid());

        WritingSessionResponseDto cached = writingService.analyzeAndSave(
                WritingAnalyzeRequestDto.builder()
                        .inputText("Cô ấy đã đi học.")
                        .mode(WritingMode.SUGGESTION)
                        .scaleType(DifficultyScale.CEFR)
                        .targetLevel("A2")
                        .build(),
                testUser
        );
        assertEquals(72, cached.getDraftValidation().getScore());
        verify(aiService, never()).reviewDraft(anyString(), any(), any(), any(), anyList());
    }

    @Test
    @DisplayName("analyzeAndSave normalizes GB/EN-US accents consistently and invokes TtsPrewarmService after new save")
    void testAnalyzeAndSave_NormalizesGbAccentAndPrewarmsSession() {
        TtsPrewarmService prewarmMock = mock(TtsPrewarmService.class);
        WritingAssistantService serviceWithPrewarm = new WritingAssistantService(
                aiService, vocabularyRepository, sessionRepository, wordNormalizer,
                grammarValidationService, objectMapper, null, writingTtsService, prewarmMock
        );

        Vocabulary vocab = Vocabulary.builder()
                .id(UUID.randomUUID())
                .word("schedule")
                .normalizedWord("schedule")
                .pronunciations(List.of(
                        VocabularyPronunciation.builder()
                                .accent("EN-US")
                                .ipa("/ˈskedʒ.uːl/")
                                .audioUrl("https://cdn.parrotalk.fun/us/schedule.mp3")
                                .build(),
                        VocabularyPronunciation.builder()
                                .accent("GB")
                                .ipa("/ˈʃedʒ.uːl/")
                                .audioUrl("https://cdn.parrotalk.fun/uk/schedule.mp3")
                                .build()
                ))
                .build();

        WritingAssistantAiService.AiWritingAnalysisPayload payload =
                WritingAssistantAiService.AiWritingAnalysisPayload.builder()
                        .detectedLanguage("VI")
                        .coreIdeaSummaryVn("Lịch trình")
                        .vocabulary(List.of(
                                WritingAssistantAiService.AiLexicalItem.builder()
                                        .word("schedule")
                                        .partOfSpeech("noun")
                                        .cefrLevel("B1")
                                        .meaningVn("lịch trình")
                                        .build()
                        ))
                        .build();

        when(aiService.analyzeIdea(anyString(), eq(WritingMode.SUGGESTION), eq(DifficultyScale.CEFR), eq("B1")))
                .thenReturn(payload);
        when(vocabularyRepository.findByNormalizedWordInWithSenses(anyCollection()))
                .thenReturn(List.of(vocab));
        when(sessionRepository.save(any(WritingSession.class)))
                .thenAnswer(inv -> {
                    WritingSession s = inv.getArgument(0);
                    s.setId(UUID.randomUUID());
                    return s;
                });

        WritingSessionResponseDto response = serviceWithPrewarm.analyzeAndSave(
                WritingAnalyzeRequestDto.builder()
                        .inputText("Lập lịch trình học tập")
                        .mode(WritingMode.SUGGESTION)
                        .scaleType(DifficultyScale.CEFR)
                        .targetLevel("B1")
                        .build(),
                testUser
        );

        assertEquals("https://cdn.parrotalk.fun/us/schedule.mp3", response.getVocabulary().get(0).getAudioUsUrl());
        assertEquals("https://cdn.parrotalk.fun/uk/schedule.mp3", response.getVocabulary().get(0).getAudioUkUrl());
        verify(prewarmMock, times(1)).prewarmSession(response);
    }

    @Test
    @DisplayName("updateUserDraft unlocated AI PUNCTUATION issue uses length=0 and does not suppress deterministic punctuation rules")
    void testUpdateUserDraft_UnlocatedAiPunctuationDoesNotSuppressRuleIssues() {
        UUID sessionId = UUID.randomUUID();
        String draft = "drunk drivers (cause harm";
        WritingSession session = WritingSession.builder()
                .id(sessionId)
                .user(testUser)
                .inputText("Lái xe khi say rượu gây hại.")
                .inputLanguage("VI")
                .mode(WritingMode.SUGGESTION)
                .scaleType(DifficultyScale.CEFR)
                .targetLevel("B1")
                .coreIdeaSummaryVn("Gây hại")
                .build();

        when(sessionRepository.findByIdAndUserId(sessionId, testUser.getId()))
                .thenReturn(Optional.of(session));
        when(sessionRepository.save(any(WritingSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(aiService.reviewDraft(eq(draft), anyString(), eq(DifficultyScale.CEFR), eq("B1"), anyList()))
                .thenReturn(WritingAssistantAiService.AiDraftReviewPayload.builder()
                        .correctedSentence("Drunk drivers cause harm.")
                        .score(70)
                        .overallVn("Cần chỉnh dấu câu.")
                        .issues(List.of(
                                WritingAssistantAiService.AiDraftIssue.builder()
                                        .type("PUNCTUATION")
                                        .original("nonexistent snippet")
                                        .suggestion("Drunk drivers cause harm.")
                                        .explanationVn("Thiếu dấu chấm cuối câu và viết hoa.")
                                        .build()
                        ))
                        .build());

        WritingSessionResponseDto updated = writingService.updateUserDraft(sessionId, draft, testUser);

        assertNotNull(updated.getDraftValidation());
        var issues = updated.getDraftValidation().getIssues();
        assertTrue(issues.stream().anyMatch(i -> "AI_PUNCTUATION".equals(i.getRuleId())
                && i.getOffset() == 0 && i.getLength() == 0));
        assertTrue(issues.stream().anyMatch(i -> "UPPERCASE_START".equals(i.getRuleId())),
                () -> "Missing UPPERCASE_START in " + issues);
        assertTrue(issues.stream().anyMatch(i -> "TERMINAL_PUNCTUATION".equals(i.getRuleId())),
                () -> "Missing TERMINAL_PUNCTUATION in " + issues);
        assertTrue(issues.stream().anyMatch(i -> "UNBALANCED_BRACKETS".equals(i.getRuleId())),
                () -> "Missing UNBALANCED_BRACKETS in " + issues);
    }
}

