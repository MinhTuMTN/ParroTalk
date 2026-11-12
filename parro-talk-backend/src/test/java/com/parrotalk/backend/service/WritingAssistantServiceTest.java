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
import com.parrotalk.backend.entity.VocabularySense;
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
    private WritingAssistantService writingService;

    private User testUser;

    @BeforeEach
    void setUp() {
        wordNormalizer = new WordNormalizer();
        grammarValidationService = new GrammarValidationService();
        objectMapper = new ObjectMapper();
        writingService = new WritingAssistantService(
                aiService,
                vocabularyRepository,
                sessionRepository,
                wordNormalizer,
                grammarValidationService,
                objectMapper,
                null
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
        Vocabulary existingVocab = Vocabulary.builder()
                .id(vocabId)
                .word("impair")
                .normalizedWord("impair")
                .senses(List.of(existingSense))
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

        // "intoxicated" is not in dictionary -> inDictionary = false, never saved to VocabularyRepository
        WritingSessionResponseDto.WritingLexicalItemDto intoxicatedDto = response.getVocabulary().get(1);
        assertFalse(intoxicatedDto.isInDictionary());
        assertNull(intoxicatedDto.getLinkedVocabularyId());

        // Verify official VocabularyRepository is NEVER written to
        verify(vocabularyRepository, never()).save(any());
        verify(vocabularyRepository, never()).saveAll(any());

        ArgumentCaptor<WritingSession> sessionCaptor = ArgumentCaptor.forClass(WritingSession.class);
        verify(sessionRepository, times(1)).save(sessionCaptor.capture());
        WritingSession savedSession = sessionCaptor.getValue();
        assertEquals(6, savedSession.getGeneratedItems().size());
        assertTrue(savedSession.getGeneratedItems().stream()
                .anyMatch(i -> i.getItemType() == WritingItemType.COMPLETE_SENTENCE
                        && i.getValidationStatus() == WritingItemValidationStatus.APPROVED));
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
}

