package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.DifficultyScale;
import com.parrotalk.backend.constant.WritingMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WritingAssistantAiServiceTest {

    @Mock
    private AiCoreClient aiCoreClient;

    @InjectMocks
    private WritingAssistantAiService aiService;

    @Test
    @DisplayName("AI Suggestion Mode uses lightweight AiSuggestionPayload schema without completeSentences")
    @SuppressWarnings("unchecked")
    void testAnalyzeIdea_SuggestionMode_Success() {
        WritingAssistantAiService.AiSuggestionPayload mockSuggestion =
                WritingAssistantAiService.AiSuggestionPayload.builder()
                        .detectedLanguage("VI")
                        .coreIdeaSummaryVn("Người say rượu không được lái xe vì giảm tầm nhìn và sự tập trung.")
                        .vocabulary(List.of(
                                WritingAssistantAiService.AiSuggestionLexicalItem.builder()
                                        .word("impair")
                                        .partOfSpeech("verb")
                                        .cefrLevel("C1")
                                        .meaningVn("làm suy giảm")
                                        .usageContextVn("Trang trọng")
                                        .build()
                        ))
                        .expressions(List.of(
                                WritingAssistantAiService.AiSuggestionLexicalItem.builder()
                                        .phrase("drive under the influence")
                                        .cefrLevel("B2")
                                        .meaningVn("lái xe khi có cồn")
                                        .build()
                        ))
                        .collocations(List.of(
                                WritingAssistantAiService.AiSuggestionLexicalItem.builder()
                                        .phrase("blood alcohol concentration")
                                        .cefrLevel("B2")
                                        .meaningVn("nồng độ cồn trong máu")
                                        .build()
                        ))
                        .sentencePatterns(List.of(
                                WritingAssistantAiService.AiSuggestionPatternItem.builder()
                                        .pattern("[S] + must not + [V] + because + [S] + [V] + [O].")
                                        .meaningVn("Ai đó không được làm gì vì...")
                                        .build()
                        ))
                        .build();

        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        when(aiCoreClient.callAndValidate(anyString(), userPromptCaptor.capture(),
                eq(WritingAssistantAiService.AiSuggestionPayload.class), any()))
                .thenAnswer(inv -> {
                    Function<WritingAssistantAiService.AiSuggestionPayload, WritingAssistantAiService.AiWritingAnalysisPayload> validator =
                            inv.getArgument(3);
                    return validator.apply(mockSuggestion);
                });

        WritingAssistantAiService.AiWritingAnalysisPayload result = aiService.analyzeIdea(
                "Người say rượu không được lái xe vì nồng độ cồn trong máu làm giảm tầm nhìn, giảm sự tập trung.",
                WritingMode.SUGGESTION,
                DifficultyScale.IELTS,
                "6.5"
        );

        assertNotNull(result);
        assertEquals("VI", result.getDetectedLanguage());
        assertEquals(1, result.getVocabulary().size());
        assertEquals("impair", result.getVocabulary().get(0).getWord());
        assertTrue(result.getCompleteSentences().isEmpty());
        assertNull(result.getCombinationGuideVn());
        assertTrue(userPromptCaptor.getValue().contains("IELTS 6.5"));
        verify(aiCoreClient, times(1)).callAndValidate(anyString(), anyString(),
                eq(WritingAssistantAiService.AiSuggestionPayload.class), any());
    }

    @Test
    @DisplayName("AI Guidance Mode validates and requires completeSentences")
    @SuppressWarnings("unchecked")
    void testAnalyzeIdea_GuidanceMode_ValidatorRejectsIncompletePayload() {
        WritingAssistantAiService.AiWritingAnalysisPayload invalidPayload =
                WritingAssistantAiService.AiWritingAnalysisPayload.builder()
                        .vocabulary(List.of(
                                WritingAssistantAiService.AiLexicalItem.builder()
                                        .word("drive")
                                        .partOfSpeech("verb")
                                        .cefrLevel("A2")
                                        .meaningVn("lái xe")
                                        .build()
                        ))
                        .sentencePatterns(List.of(
                                WritingAssistantAiService.AiPatternItem.builder()
                                        .pattern("[S] + should not + [V].")
                                        .meaningVn("Không nên làm gì")
                                        .build()
                        ))
                        .completeSentences(List.of())
                        .build();

        when(aiCoreClient.callAndValidate(anyString(), anyString(),
                eq(WritingAssistantAiService.AiWritingAnalysisPayload.class), any()))
                .thenAnswer(inv -> {
                    Function<WritingAssistantAiService.AiWritingAnalysisPayload, WritingAssistantAiService.AiWritingAnalysisPayload> validator =
                            inv.getArgument(3);
                    return validator.apply(invalidPayload);
                });

        assertThrows(IllegalArgumentException.class, () -> aiService.analyzeIdea(
                "Test input idea",
                WritingMode.GUIDANCE,
                DifficultyScale.CEFR,
                "B1"
        ));
    }
}
