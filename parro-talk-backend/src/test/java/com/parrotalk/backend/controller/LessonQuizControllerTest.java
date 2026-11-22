package com.parrotalk.backend.controller;

import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.constant.QuizAttemptStatus;
import com.parrotalk.backend.constant.QuizGradingStatus;
import com.parrotalk.backend.constant.QuizMode;
import com.parrotalk.backend.constant.QuizQuestionType;
import com.parrotalk.backend.dto.quiz.CreateQuizAttemptRequest;
import com.parrotalk.backend.dto.quiz.LessonQuizAttemptResponse;
import com.parrotalk.backend.dto.quiz.LessonQuizAttemptSummaryDto;
import com.parrotalk.backend.dto.quiz.LessonQuizOverviewResponse;
import com.parrotalk.backend.dto.quiz.LessonQuizQuestionDto;
import com.parrotalk.backend.dto.quiz.QuizAnswerResultDto;
import com.parrotalk.backend.dto.quiz.SubmitQuizAnswerRequest;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.GlobalExceptionHandler;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.service.LessonQuizService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class LessonQuizControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LessonQuizService lessonQuizService;

    @InjectMocks
    private LessonQuizController controller;

    private User testUser;
    private UUID lessonId;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("learner@parrotalk.fun")
                .fullName("Test Learner")
                .build();
        lessonId = UUID.randomUUID();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                                && User.class.isAssignableFrom(parameter.getParameterType());
                    }

                    @Override
                    public Object resolveArgument(
                            MethodParameter parameter,
                            ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest,
                            WebDataBinderFactory binderFactory) {
                        return testUser;
                    }
                })
                .build();
    }

    @Test
    @DisplayName("GET /api/lessons/{lessonId}/quiz/overview returns 200 with overview JSON contract")
    void testGetOverview_Success() throws Exception {
        LessonQuizOverviewResponse overview = LessonQuizOverviewResponse.builder()
                .lessonId(lessonId)
                .recommendedQuestionCount(5)
                .cachedModes(List.of(QuizMode.MIXED))
                .regenerateCooldownSeconds(java.util.Map.of(
                        QuizMode.MULTIPLE_CHOICE, 0,
                        QuizMode.FILL_IN_THE_BLANK, 15,
                        QuizMode.MIXED, 0))
                .history(List.of())
                .build();

        when(lessonQuizService.getOverview(lessonId, testUser)).thenReturn(overview);

        mockMvc.perform(get("/api/lessons/" + lessonId + "/quiz/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.lessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$.result.recommendedQuestionCount").value(5))
                .andExpect(jsonPath("$.result.cachedModes[0]").value("MIXED"))
                .andExpect(jsonPath("$.result.regenerateCooldownSeconds.MULTIPLE_CHOICE").value(0))
                .andExpect(jsonPath("$.result.regenerateCooldownSeconds.FILL_IN_THE_BLANK").value(15))
                .andExpect(jsonPath("$.result.regenerateCooldownSeconds.MIXED").value(0))
                .andExpect(jsonPath("$.result.history", hasSize(0)));
    }

    @Test
    @DisplayName("POST /api/lessons/{lessonId}/quiz/attempts returns attempt with hidden answerResult for unanswered questions")
    void testCreateOrResumeAttempt_Success() throws Exception {
        UUID attemptId = UUID.randomUUID();
        UUID quizSetId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        LessonQuizAttemptResponse response = LessonQuizAttemptResponse.builder()
                .attemptId(attemptId)
                .lessonId(lessonId)
                .quizSetId(quizSetId)
                .mode(QuizMode.MIXED)
                .status(QuizAttemptStatus.IN_PROGRESS)
                .totalQuestions(1)
                .answeredCount(0)
                .correctCount(0)
                .partialCount(0)
                .regenerateCooldownSeconds(0)
                .startedAt(LocalDateTime.now())
                .questions(List.of(LessonQuizQuestionDto.builder()
                        .questionId(questionId)
                        .displayOrder(1)
                        .questionType(QuizQuestionType.MULTIPLE_CHOICE)
                        .questionText("What is the main topic?")
                        .options(List.of("A", "B", "C", "D"))
                        .answered(false)
                        .answerResult(null)
                        .build()))
                .build();

        when(lessonQuizService.createOrResumeAttempt(eq(lessonId), any(CreateQuizAttemptRequest.class), eq(testUser)))
                .thenReturn(response);

        mockMvc.perform(post("/api/lessons/" + lessonId + "/quiz/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"MIXED\",\"forceNew\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.attemptId").value(attemptId.toString()))
                .andExpect(jsonPath("$.result.questions[0].answered").value(false))
                .andExpect(jsonPath("$.result.questions[0].answerResult").value(nullValue()));
    }

    @Test
    @DisplayName("POST /api/lessons/{lessonId}/quiz/regenerate returns 429 when cooldown active")
    void testRegenerate_CooldownReturns429() throws Exception {
        when(lessonQuizService.regenerateQuiz(lessonId, QuizMode.MIXED, testUser))
                .thenThrow(new ParroTalkException(ErrorCode.QUIZ_GENERATION_COOLDOWN));

        mockMvc.perform(post("/api/lessons/" + lessonId + "/quiz/regenerate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"MIXED\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("QUIZ_GENERATION_COOLDOWN"));
    }

    @Test
    @DisplayName("POST /api/lessons/{lessonId}/quiz/attempts/{attemptId}/answer returns QuizAnswerResultDto")
    void testSubmitAnswer_Success() throws Exception {
        UUID attemptId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID segmentId = UUID.randomUUID();

        QuizAnswerResultDto resultDto = QuizAnswerResultDto.builder()
                .questionId(questionId)
                .userAnswer("A")
                .selectedIndex(0)
                .status(QuizGradingStatus.CORRECT)
                .score(100)
                .feedback("Chính xác!")
                .modelAnswer("A")
                .correctIndex(0)
                .explanationVn("Giải thích tiếng Việt.")
                .evidenceSegmentId(segmentId)
                .evidenceText("Evidence text.")
                .evidenceStartTime(0.0)
                .evidenceEndTime(4.2)
                .build();

        when(lessonQuizService.submitAnswer(eq(lessonId), eq(attemptId), any(SubmitQuizAnswerRequest.class), eq(testUser)))
                .thenReturn(resultDto);

        mockMvc.perform(post("/api/lessons/" + lessonId + "/quiz/attempts/" + attemptId + "/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":\"" + questionId + "\",\"selectedIndex\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.status").value("CORRECT"))
                .andExpect(jsonPath("$.result.score").value(100))
                .andExpect(jsonPath("$.result.evidenceSegmentId").value(segmentId.toString()));
    }

    @Test
    @DisplayName("POST /api/lessons/{lessonId}/quiz/attempts/{attemptId}/complete returns LessonQuizAttemptSummaryDto")
    void testCompleteAttempt_Success() throws Exception {
        UUID attemptId = UUID.randomUUID();
        UUID quizSetId = UUID.randomUUID();

        LessonQuizAttemptSummaryDto summary = LessonQuizAttemptSummaryDto.builder()
                .attemptId(attemptId)
                .quizSetId(quizSetId)
                .mode(QuizMode.MIXED)
                .status(QuizAttemptStatus.COMPLETED)
                .totalQuestions(5)
                .answeredCount(5)
                .correctCount(4)
                .partialCount(1)
                .overallScore(92)
                .startedAt(LocalDateTime.now().minusMinutes(3))
                .completedAt(LocalDateTime.now())
                .build();

        when(lessonQuizService.completeAttempt(lessonId, attemptId, testUser)).thenReturn(summary);

        mockMvc.perform(post("/api/lessons/" + lessonId + "/quiz/attempts/" + attemptId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.overallScore").value(92));
    }
}
