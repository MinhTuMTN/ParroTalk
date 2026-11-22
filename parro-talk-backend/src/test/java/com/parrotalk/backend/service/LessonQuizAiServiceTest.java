package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.QuizGradingStatus;
import com.parrotalk.backend.constant.QuizMode;
import com.parrotalk.backend.constant.QuizQuestionType;
import com.parrotalk.backend.entity.TranscriptionSegment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LessonQuizAiServiceTest {

    @Mock
    private AiCoreClient aiCoreClient;

    @InjectMocks
    private LessonQuizAiService quizAiService;

    private List<TranscriptionSegment> sampleSegments(int count) {
        List<TranscriptionSegment> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(TranscriptionSegment.builder()
                    .id(UUID.randomUUID())
                    .displayOrder(i)
                    .startTime(i * 4.0)
                    .endTime((i + 1) * 4.0)
                    .text("Segment number " + i + " explains an important concept about climate action.")
                    .build());
        }
        return list;
    }

    @Test
    @DisplayName("generateQuestions maps evidenceSegmentIndex (including out-of-bounds clamping) to real TranscriptionSegment")
    @SuppressWarnings("unchecked")
    void testGenerateQuestions_ClampsEvidenceIndexAndMapsSegment() {
        List<TranscriptionSegment> segments = sampleSegments(4);

        LessonQuizAiService.AiQuizGenerationPayload mockPayload = LessonQuizAiService.AiQuizGenerationPayload.builder()
                .questions(List.of(
                        LessonQuizAiService.AiQuizQuestionItem.builder()
                                .questionType("MULTIPLE_CHOICE")
                                .questionText("What is the main topic discussed?")
                                .options(List.of("Climate action", "Cooking", "Sports", "Music"))
                                .correctIndex(0)
                                .modelAnswer("Ignored answer")
                                .explanationVn("Đoạn đầu nói về hành động khí hậu.")
                                .evidenceSegmentIndex(-3) // clamps to 0
                                .build(),
                        LessonQuizAiService.AiQuizQuestionItem.builder()
                                .questionType("FILL_IN_THE_BLANK")
                                .questionText("The speaker highlights an important concept about ___.")
                                .options(List.of())
                                .modelAnswer("climate action")
                                .explanationVn("Đoạn cuối đề cập đến climate action.")
                                .evidenceSegmentIndex(99) // clamps to 3
                                .build(),
                        LessonQuizAiService.AiQuizQuestionItem.builder()
                                .questionType("MULTIPLE_CHOICE")
                                .questionText("Which segment gives a key idea?")
                                .options(List.of("A", "B", "C", "D"))
                                .correctIndex(2)
                                .explanationVn("Đáp án C.")
                                .evidenceSegmentIndex(2)
                                .build()
                ))
                .build();

        when(aiCoreClient.callAndValidate(anyList(), anyString(), anyString(),
                eq(LessonQuizAiService.AiQuizGenerationPayload.class), any()))
                .thenAnswer(inv -> {
                    Function<LessonQuizAiService.AiQuizGenerationPayload, LessonQuizAiService.AiQuizGenerationPayload> validator =
                            inv.getArgument(4);
                    return validator.apply(mockPayload);
                });

        List<LessonQuizAiService.GeneratedQuestion> result = quizAiService.generateQuestions(
                "Climate Talk", segments, QuizMode.MIXED, 3);

        assertEquals(3, result.size());

        LessonQuizAiService.GeneratedQuestion q1 = result.get(0);
        assertEquals(QuizQuestionType.MULTIPLE_CHOICE, q1.getQuestionType());
        assertEquals((short) 0, q1.getCorrectIndex());
        assertEquals("Climate action", q1.getModelAnswer()); // normalized to options[correctIndex]
        assertEquals(segments.get(0).getId(), q1.getEvidenceSegment().getId());
        assertEquals(segments.get(0).getStartTime(), q1.getEvidenceStartTime());

        LessonQuizAiService.GeneratedQuestion q2 = result.get(1);
        assertEquals(QuizQuestionType.FILL_IN_THE_BLANK, q2.getQuestionType());
        assertNull(q2.getCorrectIndex());
        assertEquals("climate action", q2.getModelAnswer());
        assertEquals(segments.get(3).getId(), q2.getEvidenceSegment().getId());
        assertEquals(segments.get(3).getEndTime(), q2.getEvidenceEndTime());
    }

    @Test
    @DisplayName("gradeFillInTheBlank uses normalized exact fast-path without calling AI")
    void testGradeFillInTheBlank_ExactFastPath() {
        LessonQuizAiService.GradingResult result = quizAiService.gradeFillInTheBlank(
                "The speaker talked about ___.",
                "Climate Change",
                "Đáp án là biến đổi khí hậu.",
                "We are facing climate change today.",
                "  climate   change! ");

        assertEquals(QuizGradingStatus.CORRECT, result.getStatus());
        assertEquals(100, result.getScore());
        assertTrue(result.getFeedback().contains("Chính xác"));
        verifyNoInteractions(aiCoreClient);
    }

    @Test
    @DisplayName("gradeFillInTheBlank returns INCORRECT with score 0 on blank answer without calling AI")
    void testGradeFillInTheBlank_BlankAnswer() {
        LessonQuizAiService.GradingResult result = quizAiService.gradeFillInTheBlank(
                "The speaker talked about ___.",
                "climate change",
                "Giải thích.",
                "Evidence text",
                "   ");

        assertEquals(QuizGradingStatus.INCORRECT, result.getStatus());
        assertEquals(0, result.getScore());
        verifyNoInteractions(aiCoreClient);
    }

    @Test
    @DisplayName("gradeFillInTheBlank calls AI semantic grading and aligns status with clamped score")
    @SuppressWarnings("unchecked")
    void testGradeFillInTheBlank_SemanticAiGrading() {
        LessonQuizAiService.AiGradePayload aiPayload = LessonQuizAiService.AiGradePayload.builder()
                .status("INCORRECT") // intentionally mismatched from AI to verify score-based status alignment
                .score(72)
                .feedback("Bạn đã nêu đúng ý chính nhưng diễn đạt hơi chung.")
                .build();

        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        when(aiCoreClient.callAndValidate(anyList(), anyString(), userPromptCaptor.capture(),
                eq(LessonQuizAiService.AiGradePayload.class), any()))
                .thenAnswer(inv -> {
                    Function<LessonQuizAiService.AiGradePayload, LessonQuizAiService.AiGradePayload> validator =
                            inv.getArgument(4);
                    return validator.apply(aiPayload);
                });

        LessonQuizAiService.GradingResult result = quizAiService.gradeFillInTheBlank(
                "Why did they leave early? Because of ___.",
                "heavy rain",
                "Do trời mưa to.",
                "We left early because of the heavy rain.",
                "bad weather");

        assertEquals(QuizGradingStatus.PARTIAL, result.getStatus());
        assertEquals(72, result.getScore());
        assertTrue(result.getFeedback().contains("đúng ý chính"));
        assertTrue(userPromptCaptor.getValue().contains("bad weather"));
        verify(aiCoreClient, times(1)).callAndValidate(anyList(), anyString(), anyString(),
                eq(LessonQuizAiService.AiGradePayload.class), any());
    }

    @Test
    @DisplayName("formatTranscriptForPrompt compacts transcripts exceeding 12000 chars while preserving head, middle, and tail")
    void testFormatTranscriptForPrompt_CompactsLongTranscript() {
        List<TranscriptionSegment> longSegments = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            longSegments.add(TranscriptionSegment.builder()
                    .id(UUID.randomUUID())
                    .displayOrder(i)
                    .startTime(i * 3.0)
                    .endTime((i + 1) * 3.0)
                    .text("This is a reasonably detailed transcript sentence number " + i
                            + " discussing renewable energy adoption and policy impacts across cities.")
                    .build());
        }

        String formatted = quizAiService.formatTranscriptForPrompt(longSegments, LessonQuizAiService.MAX_TRANSCRIPT_CHARS);
        assertTrue(formatted.length() <= LessonQuizAiService.MAX_TRANSCRIPT_CHARS);
        assertTrue(formatted.contains("[0]"));
        assertTrue(formatted.contains("[199]"));
        assertTrue(formatted.contains("..."));
    }
}
