package com.parrotalk.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.constant.LessonStatus;
import com.parrotalk.backend.constant.LessonVisibilityStatus;
import com.parrotalk.backend.constant.QuizAttemptStatus;
import com.parrotalk.backend.constant.QuizGradingStatus;
import com.parrotalk.backend.constant.QuizMode;
import com.parrotalk.backend.constant.QuizQuestionType;
import com.parrotalk.backend.dto.quiz.CreateQuizAttemptRequest;
import com.parrotalk.backend.dto.quiz.LessonQuizAttemptResponse;
import com.parrotalk.backend.dto.quiz.LessonQuizAttemptSummaryDto;
import com.parrotalk.backend.dto.quiz.LessonQuizQuestionDto;
import com.parrotalk.backend.dto.quiz.QuizAnswerResultDto;
import com.parrotalk.backend.dto.quiz.SubmitQuizAnswerRequest;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.LessonQuizQuestion;
import com.parrotalk.backend.entity.LessonQuizSet;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserLessonQuizAnswer;
import com.parrotalk.backend.entity.UserLessonQuizAttempt;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.LessonQuizQuestionRepository;
import com.parrotalk.backend.repository.LessonQuizSetRepository;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;
import com.parrotalk.backend.repository.UserLessonQuizAnswerRepository;
import com.parrotalk.backend.repository.UserLessonQuizAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LessonQuizServiceTest {

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private TranscriptionSegmentRepository segmentRepository;
    @Mock
    private LessonQuizSetRepository quizSetRepository;
    @Mock
    private LessonQuizQuestionRepository quizQuestionRepository;
    @Mock
    private UserLessonQuizAttemptRepository attemptRepository;
    @Mock
    private UserLessonQuizAnswerRepository answerRepository;
    @Mock
    private LessonQuizAiService quizAiService;
    @Mock
    private UserLearningActivityService userLearningActivityService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private LessonQuizService quizService;

    private User testUser;
    private Lesson readyLesson;
    private List<TranscriptionSegment> segments;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        quizService = new LessonQuizService(
                lessonRepository,
                segmentRepository,
                quizSetRepository,
                quizQuestionRepository,
                attemptRepository,
                answerRepository,
                quizAiService,
                userLearningActivityService,
                objectMapper,
                redisTemplate,
                null);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("learner@parrotalk.fun")
                .fullName("Test Learner")
                .build();

        readyLesson = Lesson.builder()
                .id(UUID.randomUUID())
                .title("Urban Farming")
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .fileUrl("https://cdn.parrotalk.fun/audio.mp3")
                .build();

        segments = buildSegments(10, 20); // 10 segments, 200 words -> tier 2 -> 5 questions
    }

    private List<TranscriptionSegment> buildSegments(int count, int wordsPerSegment) {
        List<TranscriptionSegment> list = new ArrayList<>();
        String words = ("word ".repeat(wordsPerSegment)).trim();
        for (int i = 0; i < count; i++) {
            list.add(TranscriptionSegment.builder()
                    .id(UUID.randomUUID())
                    .lesson(readyLesson)
                    .displayOrder(i)
                    .startTime(i * 5.0)
                    .endTime((i + 1) * 5.0)
                    .text(words)
                    .build());
        }
        return list;
    }

    @Test
    @DisplayName("computeRecommendedQuestionCount returns adaptive counts across all tiers")
    void testComputeRecommendedQuestionCount() {
        assertEquals(3, quizService.computeRecommendedQuestionCount(buildSegments(5, 30)));   // < 8 segments
        assertEquals(3, quizService.computeRecommendedQuestionCount(buildSegments(10, 8)));   // < 120 words
        assertEquals(5, quizService.computeRecommendedQuestionCount(buildSegments(10, 20)));  // 10 segs, 200 words
        assertEquals(8, quizService.computeRecommendedQuestionCount(buildSegments(25, 20)));  // 25 segs, 500 words
        assertEquals(10, quizService.computeRecommendedQuestionCount(buildSegments(45, 20))); // > 40 segs, 900 words
    }

    @Test
    @DisplayName("createOrResumeAttempt hits cache when active set matches transcriptHash and prevents answer leak")
    void testCreateOrResumeAttempt_CacheHitAndNoAnswerLeak() {
        String transcriptHash = quizService.computeTranscriptHash(segments);
        UUID quizSetId = UUID.randomUUID();
        LessonQuizSet cachedSet = LessonQuizSet.builder()
                .id(quizSetId)
                .lesson(readyLesson)
                .mode(QuizMode.MULTIPLE_CHOICE)
                .questionCount(3)
                .transcriptHash(transcriptHash)
                .active(true)
                .generatedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        LessonQuizQuestion q1 = LessonQuizQuestion.builder()
                .id(UUID.randomUUID())
                .quizSet(cachedSet)
                .displayOrder(1)
                .questionType(QuizQuestionType.MULTIPLE_CHOICE)
                .questionText("What is discussed?")
                .optionsJson("[\"Opt A\",\"Opt B\",\"Opt C\",\"Opt D\"]")
                .correctIndex((short) 1)
                .modelAnswer("Opt B")
                .explanationVn("Giải thích bí mật")
                .evidenceSegment(segments.get(0))
                .evidenceText("Secret evidence text")
                .evidenceStartTime(0.0)
                .evidenceEndTime(5.0)
                .build();

        when(lessonRepository.findByIdAndVisibilityStatus(readyLesson.getId(), LessonVisibilityStatus.PUBLISHED))
                .thenReturn(Optional.of(readyLesson));
        when(segmentRepository.findByLessonIdOrderByDisplayOrderAsc(readyLesson.getId()))
                .thenReturn(segments);
        when(attemptRepository.findFirstByUserIdAndLessonIdAndStatusOrderByStartedAtDesc(
                testUser.getId(), readyLesson.getId(), QuizAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(quizSetRepository.findFirstByLessonIdAndModeAndActiveTrueOrderByGeneratedAtDesc(
                readyLesson.getId(), QuizMode.MULTIPLE_CHOICE))
                .thenReturn(Optional.of(cachedSet));
        when(lessonRepository.getReferenceById(readyLesson.getId())).thenReturn(readyLesson);
        when(quizSetRepository.findById(quizSetId)).thenReturn(Optional.of(cachedSet));
        when(quizQuestionRepository.findByQuizSetIdOrderByDisplayOrderAsc(quizSetId))
                .thenReturn(List.of(q1));
        when(attemptRepository.save(any(UserLessonQuizAttempt.class))).thenAnswer(inv -> {
            UserLessonQuizAttempt a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(redisTemplate.getExpire(anyString())).thenReturn(-2L);

        LessonQuizAttemptResponse response = quizService.createOrResumeAttempt(
                readyLesson.getId(),
                CreateQuizAttemptRequest.builder().mode(QuizMode.MULTIPLE_CHOICE).build(),
                testUser);

        assertEquals(0, response.getOverallScore());

        assertNotNull(response.getAttemptId());
        assertEquals(quizSetId, response.getQuizSetId());
        assertEquals(1, response.getQuestions().size());

        LessonQuizQuestionDto questionDto = response.getQuestions().get(0);
        assertFalse(questionDto.isAnswered());
        assertNull(questionDto.getAnswerResult());
        assertEquals(List.of("Opt A", "Opt B", "Opt C", "Opt D"), questionDto.getOptions());

        verifyNoInteractions(quizAiService);
    }

    @Test
    @DisplayName("createOrResumeAttempt generates new quiz set on cache miss or hash mismatch")
    void testCreateOrResumeAttempt_CacheMissGeneratesNewSet() {
        UUID newSetId = UUID.randomUUID();
        when(lessonRepository.findByIdAndVisibilityStatus(readyLesson.getId(), LessonVisibilityStatus.PUBLISHED))
                .thenReturn(Optional.of(readyLesson));
        when(segmentRepository.findByLessonIdOrderByDisplayOrderAsc(readyLesson.getId()))
                .thenReturn(segments);
        when(attemptRepository.findFirstByUserIdAndLessonIdAndStatusOrderByStartedAtDesc(
                testUser.getId(), readyLesson.getId(), QuizAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(quizSetRepository.findFirstByLessonIdAndModeAndActiveTrueOrderByGeneratedAtDesc(
                readyLesson.getId(), QuizMode.MIXED))
                .thenReturn(Optional.of(LessonQuizSet.builder()
                        .id(UUID.randomUUID())
                        .transcriptHash("stale-hash")
                        .active(true)
                        .build()));

        LessonQuizAiService.GeneratedQuestion genQ = LessonQuizAiService.GeneratedQuestion.builder()
                .displayOrder(1)
                .questionType(QuizQuestionType.FILL_IN_THE_BLANK)
                .questionText("Urban farming helps reduce ___.")
                .modelAnswer("food miles")
                .explanationVn("Giúp giảm quãng đường vận chuyển thực phẩm.")
                .evidenceSegment(segments.get(0))
                .evidenceText(segments.get(0).getText())
                .evidenceStartTime(0.0)
                .evidenceEndTime(5.0)
                .build();

        when(quizAiService.generateQuestions(eq("Urban Farming"), eq(segments), eq(QuizMode.MIXED), eq(5)))
                .thenReturn(List.of(genQ));

        when(lessonRepository.getReferenceById(readyLesson.getId())).thenReturn(readyLesson);
        when(quizSetRepository.save(any(LessonQuizSet.class))).thenAnswer(inv -> {
            LessonQuizSet set = inv.getArgument(0);
            set.setId(newSetId);
            return set;
        });
        when(quizSetRepository.findById(newSetId)).thenAnswer(inv -> Optional.of(LessonQuizSet.builder()
                .id(newSetId)
                .lesson(readyLesson)
                .mode(QuizMode.MIXED)
                .questionCount(1)
                .build()));
        when(quizQuestionRepository.findByQuizSetIdOrderByDisplayOrderAsc(newSetId))
                .thenReturn(List.of(LessonQuizQuestion.builder()
                        .id(UUID.randomUUID())
                        .displayOrder(1)
                        .questionType(QuizQuestionType.FILL_IN_THE_BLANK)
                        .questionText("Urban farming helps reduce ___.")
                        .modelAnswer("food miles")
                        .build()));
        when(attemptRepository.save(any(UserLessonQuizAttempt.class))).thenAnswer(inv -> {
            UserLessonQuizAttempt a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(redisTemplate.getExpire(anyString())).thenReturn(-2L);

        LessonQuizAttemptResponse response = quizService.createOrResumeAttempt(
                readyLesson.getId(),
                CreateQuizAttemptRequest.builder().mode(QuizMode.MIXED).build(),
                testUser);

        assertEquals(newSetId, response.getQuizSetId());
        assertEquals(0, response.getOverallScore());
        verify(quizSetRepository, times(1)).deactivateActiveSets(readyLesson.getId(), QuizMode.MIXED);
        verify(quizSetRepository, times(1)).flush();
        verify(quizAiService, times(1)).generateQuestions(eq("Urban Farming"), eq(segments), eq(QuizMode.MIXED), eq(5));
    }

    @Test
    @DisplayName("regenerateQuiz enforces 60-second cooldown and throws QUIZ_GENERATION_COOLDOWN")
    void testRegenerateQuiz_CooldownEnforced() {
        when(lessonRepository.findByIdAndVisibilityStatus(readyLesson.getId(), LessonVisibilityStatus.PUBLISHED))
                .thenReturn(Optional.of(readyLesson));
        when(segmentRepository.findByLessonIdOrderByDisplayOrderAsc(readyLesson.getId()))
                .thenReturn(segments);
        when(redisTemplate.getExpire(anyString())).thenReturn(42L);

        ParroTalkException ex = assertThrows(ParroTalkException.class,
                () -> quizService.regenerateQuiz(readyLesson.getId(), QuizMode.MIXED, testUser));

        assertEquals(ErrorCode.QUIZ_GENERATION_COOLDOWN.name(), ex.getErrorCode());
        assertEquals(429, ex.getHttpStatusCode().value());
        verifyNoInteractions(quizAiService);
    }

    @Test
    @DisplayName("regenerateQuiz generates new set and records Redis cooldown when cooldown expired")
    void testRegenerateQuiz_Success() {
        UUID newSetId = UUID.randomUUID();
        when(lessonRepository.findByIdAndVisibilityStatus(readyLesson.getId(), LessonVisibilityStatus.PUBLISHED))
                .thenReturn(Optional.of(readyLesson));
        when(segmentRepository.findByLessonIdOrderByDisplayOrderAsc(readyLesson.getId()))
                .thenReturn(segments);
        when(redisTemplate.getExpire(anyString())).thenReturn(-2L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(quizAiService.generateQuestions(eq("Urban Farming"), eq(segments), eq(QuizMode.MULTIPLE_CHOICE), eq(5)))
                .thenReturn(List.of(LessonQuizAiService.GeneratedQuestion.builder()
                        .displayOrder(1)
                        .questionType(QuizQuestionType.MULTIPLE_CHOICE)
                        .questionText("Q1?")
                        .options(List.of("A", "B", "C", "D"))
                        .correctIndex((short) 0)
                        .modelAnswer("A")
                        .explanationVn("Vì A.")
                        .evidenceSegment(segments.get(0))
                        .evidenceText(segments.get(0).getText())
                        .evidenceStartTime(0.0)
                        .evidenceEndTime(5.0)
                        .build()));

        when(lessonRepository.getReferenceById(readyLesson.getId())).thenReturn(readyLesson);
        when(quizSetRepository.save(any(LessonQuizSet.class))).thenAnswer(inv -> {
            LessonQuizSet set = inv.getArgument(0);
            set.setId(newSetId);
            return set;
        });
        when(quizSetRepository.findById(newSetId)).thenReturn(Optional.of(LessonQuizSet.builder()
                .id(newSetId)
                .lesson(readyLesson)
                .mode(QuizMode.MULTIPLE_CHOICE)
                .build()));
        when(quizQuestionRepository.findByQuizSetIdOrderByDisplayOrderAsc(newSetId))
                .thenReturn(List.of(LessonQuizQuestion.builder()
                        .id(UUID.randomUUID())
                        .displayOrder(1)
                        .questionType(QuizQuestionType.MULTIPLE_CHOICE)
                        .questionText("Q1?")
                        .optionsJson("[\"A\",\"B\",\"C\",\"D\"]")
                        .correctIndex((short) 0)
                        .modelAnswer("A")
                        .build()));
        when(attemptRepository.save(any(UserLessonQuizAttempt.class))).thenAnswer(inv -> {
            UserLessonQuizAttempt a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        LessonQuizAttemptResponse response = quizService.regenerateQuiz(readyLesson.getId(), QuizMode.MULTIPLE_CHOICE, testUser);

        assertEquals(60, response.getRegenerateCooldownSeconds());
        assertEquals(0, response.getOverallScore());
        verify(quizSetRepository, times(1)).flush();
        verify(valueOperations, times(1)).set(contains(testUser.getId().toString()), eq("1"), any());
    }

    @Test
    @DisplayName("submitAnswer grades MULTIPLE_CHOICE instantaneously and reveals answerResult")
    void testSubmitAnswer_McqInstantGrading() {
        UUID attemptId = UUID.randomUUID();
        UUID quizSetId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        LessonQuizSet quizSet = LessonQuizSet.builder().id(quizSetId).lesson(readyLesson).build();
        LessonQuizQuestion question = LessonQuizQuestion.builder()
                .id(questionId)
                .quizSet(quizSet)
                .questionType(QuizQuestionType.MULTIPLE_CHOICE)
                .questionText("What is the goal?")
                .optionsJson("[\"Save energy\",\"Cut costs\",\"Grow food\",\"None\"]")
                .correctIndex((short) 2)
                .modelAnswer("Grow food")
                .explanationVn("Bài nghe nói về trồng thực phẩm.")
                .evidenceSegment(segments.get(1))
                .evidenceText(segments.get(1).getText())
                .evidenceStartTime(5.0)
                .evidenceEndTime(10.0)
                .build();

        UserLessonQuizAttempt attempt = UserLessonQuizAttempt.builder()
                .id(attemptId)
                .user(testUser)
                .lesson(readyLesson)
                .quizSet(quizSet)
                .mode(QuizMode.MULTIPLE_CHOICE)
                .status(QuizAttemptStatus.IN_PROGRESS)
                .totalQuestions(1)
                .build();

        when(lessonRepository.findByIdAndVisibilityStatus(readyLesson.getId(), LessonVisibilityStatus.PUBLISHED))
                .thenReturn(Optional.of(readyLesson));
        when(attemptRepository.findByIdAndUserIdAndLessonId(attemptId, testUser.getId(), readyLesson.getId()))
                .thenReturn(Optional.of(attempt));
        when(quizQuestionRepository.findByIdAndQuizSetId(questionId, quizSetId))
                .thenReturn(Optional.of(question));
        when(answerRepository.existsByAttemptIdAndQuestionId(attemptId, questionId))
                .thenReturn(false);
        when(answerRepository.save(any(UserLessonQuizAnswer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(answerRepository.findByAttemptId(attemptId)).thenReturn(List.of(UserLessonQuizAnswer.builder()
                .attempt(attempt)
                .question(question)
                .userAnswer("Grow food")
                .selectedIndex((short) 2)
                .gradingStatus(QuizGradingStatus.CORRECT)
                .score(100)
                .build()));

        QuizAnswerResultDto result = quizService.submitAnswer(
                readyLesson.getId(),
                attemptId,
                SubmitQuizAnswerRequest.builder()
                        .questionId(questionId)
                        .selectedIndex(2)
                        .build(),
                testUser);

        assertEquals(QuizGradingStatus.CORRECT, result.getStatus());
        assertEquals(100, result.getScore());
        assertEquals("Grow food", result.getModelAnswer());
        assertEquals(2, result.getCorrectIndex());
        assertEquals(segments.get(1).getId(), result.getEvidenceSegmentId());
        assertEquals(1, attempt.getAnsweredCount());
        assertEquals(1, attempt.getCorrectCount());
        assertEquals(100, attempt.getOverallScore());
        verifyNoInteractions(quizAiService);
    }

    @Test
    @DisplayName("submitAnswer delegates FILL_IN_THE_BLANK to LessonQuizAiService")
    void testSubmitAnswer_FibAiGrading() {
        UUID attemptId = UUID.randomUUID();
        UUID quizSetId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        LessonQuizSet quizSet = LessonQuizSet.builder().id(quizSetId).lesson(readyLesson).build();
        LessonQuizQuestion question = LessonQuizQuestion.builder()
                .id(questionId)
                .quizSet(quizSet)
                .questionType(QuizQuestionType.FILL_IN_THE_BLANK)
                .questionText("Urban farms save ___.")
                .modelAnswer("water")
                .explanationVn("Tiết kiệm nước.")
                .evidenceSegment(segments.get(0))
                .evidenceText("They save water.")
                .evidenceStartTime(0.0)
                .evidenceEndTime(5.0)
                .build();

        UserLessonQuizAttempt attempt = UserLessonQuizAttempt.builder()
                .id(attemptId)
                .user(testUser)
                .lesson(readyLesson)
                .quizSet(quizSet)
                .mode(QuizMode.FILL_IN_THE_BLANK)
                .status(QuizAttemptStatus.IN_PROGRESS)
                .totalQuestions(1)
                .build();

        when(lessonRepository.findByIdAndVisibilityStatus(readyLesson.getId(), LessonVisibilityStatus.PUBLISHED))
                .thenReturn(Optional.of(readyLesson));
        when(attemptRepository.findByIdAndUserIdAndLessonId(attemptId, testUser.getId(), readyLesson.getId()))
                .thenReturn(Optional.of(attempt));
        when(quizQuestionRepository.findByIdAndQuizSetId(questionId, quizSetId))
                .thenReturn(Optional.of(question));
        when(answerRepository.existsByAttemptIdAndQuestionId(attemptId, questionId))
                .thenReturn(false);
        when(quizAiService.gradeFillInTheBlank(
                question.getQuestionText(),
                "water",
                "Tiết kiệm nước.",
                "They save water.",
                "fresh water"))
                .thenReturn(LessonQuizAiService.GradingResult.builder()
                        .status(QuizGradingStatus.CORRECT)
                        .score(92)
                        .feedback("Chính xác, bạn hiểu đúng ý.")
                        .build());
        when(answerRepository.save(any(UserLessonQuizAnswer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(answerRepository.findByAttemptId(attemptId)).thenReturn(List.of(UserLessonQuizAnswer.builder()
                .attempt(attempt)
                .question(question)
                .userAnswer("fresh water")
                .gradingStatus(QuizGradingStatus.CORRECT)
                .score(92)
                .build()));

        QuizAnswerResultDto result = quizService.submitAnswer(
                readyLesson.getId(),
                attemptId,
                SubmitQuizAnswerRequest.builder()
                        .questionId(questionId)
                        .userAnswer("fresh water")
                        .build(),
                testUser);

        assertEquals(QuizGradingStatus.CORRECT, result.getStatus());
        assertEquals(92, result.getScore());
        assertEquals("Chính xác, bạn hiểu đúng ý.", result.getFeedback());
        assertEquals("water", result.getModelAnswer());
    }

    @Test
    @DisplayName("completeAttempt validates completeness, computes final score, and calls recordStudyDay")
    void testCompleteAttempt_SuccessAndIdempotent() {
        UUID attemptId = UUID.randomUUID();
        LessonQuizSet quizSet = LessonQuizSet.builder().id(UUID.randomUUID()).lesson(readyLesson).build();
        UserLessonQuizAttempt attempt = UserLessonQuizAttempt.builder()
                .id(attemptId)
                .user(testUser)
                .lesson(readyLesson)
                .quizSet(quizSet)
                .mode(QuizMode.MIXED)
                .status(QuizAttemptStatus.IN_PROGRESS)
                .totalQuestions(2)
                .answeredCount(2)
                .startedAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(lessonRepository.findByIdAndVisibilityStatus(readyLesson.getId(), LessonVisibilityStatus.PUBLISHED))
                .thenReturn(Optional.of(readyLesson));
        when(attemptRepository.findByIdAndUserIdAndLessonId(attemptId, testUser.getId(), readyLesson.getId()))
                .thenReturn(Optional.of(attempt));
        when(answerRepository.findByAttemptId(attemptId)).thenReturn(List.of(
                UserLessonQuizAnswer.builder().gradingStatus(QuizGradingStatus.CORRECT).score(100).build(),
                UserLessonQuizAnswer.builder().gradingStatus(QuizGradingStatus.PARTIAL).score(65).build()));
        when(attemptRepository.save(any(UserLessonQuizAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        LessonQuizAttemptSummaryDto summary = quizService.completeAttempt(readyLesson.getId(), attemptId, testUser);

        assertEquals(QuizAttemptStatus.COMPLETED, summary.getStatus());
        assertEquals(83, summary.getOverallScore()); // round((100+65)/2) = 83
        assertEquals(1, summary.getCorrectCount());
        assertEquals(1, summary.getPartialCount());
        assertNotNull(summary.getCompletedAt());
        verify(userLearningActivityService, times(1)).recordStudyDay(testUser.getId());

        // Idempotent second call
        LessonQuizAttemptSummaryDto secondCall = quizService.completeAttempt(readyLesson.getId(), attemptId, testUser);
        assertEquals(QuizAttemptStatus.COMPLETED, secondCall.getStatus());
        verify(userLearningActivityService, times(1)).recordStudyDay(testUser.getId());
    }

    @Test
    @DisplayName("completeAttempt throws QUIZ_INCOMPLETE when not all questions answered")
    void testCompleteAttempt_IncompleteThrows() {
        UUID attemptId = UUID.randomUUID();
        LessonQuizSet quizSet = LessonQuizSet.builder().id(UUID.randomUUID()).lesson(readyLesson).build();
        UserLessonQuizAttempt attempt = UserLessonQuizAttempt.builder()
                .id(attemptId)
                .user(testUser)
                .lesson(readyLesson)
                .quizSet(quizSet)
                .mode(QuizMode.MIXED)
                .status(QuizAttemptStatus.IN_PROGRESS)
                .totalQuestions(3)
                .answeredCount(1)
                .build();

        when(lessonRepository.findByIdAndVisibilityStatus(readyLesson.getId(), LessonVisibilityStatus.PUBLISHED))
                .thenReturn(Optional.of(readyLesson));
        when(attemptRepository.findByIdAndUserIdAndLessonId(attemptId, testUser.getId(), readyLesson.getId()))
                .thenReturn(Optional.of(attempt));
        when(answerRepository.findByAttemptId(attemptId)).thenReturn(List.of(
                UserLessonQuizAnswer.builder().gradingStatus(QuizGradingStatus.CORRECT).score(100).build()));

        ParroTalkException ex = assertThrows(ParroTalkException.class,
                () -> quizService.completeAttempt(readyLesson.getId(), attemptId, testUser));

        assertEquals(ErrorCode.QUIZ_INCOMPLETE.name(), ex.getErrorCode());
        verifyNoInteractions(userLearningActivityService);
    }

    @Test
    @DisplayName("getOverview returns regenerateCooldownSeconds as Map with all 3 QuizMode keys")
    void testGetOverview_ReturnsCooldownMapForAllModes() {
        when(lessonRepository.findByIdAndVisibilityStatus(readyLesson.getId(), LessonVisibilityStatus.PUBLISHED))
                .thenReturn(Optional.of(readyLesson));
        when(segmentRepository.findByLessonIdOrderByDisplayOrderAsc(readyLesson.getId()))
                .thenReturn(segments);
        when(attemptRepository.findFirstByUserIdAndLessonIdAndStatusOrderByStartedAtDesc(
                testUser.getId(), readyLesson.getId(), QuizAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(quizSetRepository.findByLessonIdAndActiveTrue(readyLesson.getId()))
                .thenReturn(List.of());
        when(attemptRepository.findByUserIdAndLessonIdAndStatusOrderByCompletedAtDesc(
                eq(testUser.getId()), eq(readyLesson.getId()), eq(QuizAttemptStatus.COMPLETED), any()))
                .thenReturn(List.of());
        when(redisTemplate.getExpire(contains("MULTIPLE_CHOICE"))).thenReturn(25L);
        when(redisTemplate.getExpire(contains("FILL_IN_THE_BLANK"))).thenReturn(-2L);
        when(redisTemplate.getExpire(contains("MIXED"))).thenReturn(10L);

        var overview = quizService.getOverview(readyLesson.getId(), testUser);

        assertNotNull(overview.getRegenerateCooldownSeconds());
        assertEquals(3, overview.getRegenerateCooldownSeconds().size());
        assertEquals(25, overview.getRegenerateCooldownSeconds().get(QuizMode.MULTIPLE_CHOICE));
        assertEquals(0, overview.getRegenerateCooldownSeconds().get(QuizMode.FILL_IN_THE_BLANK));
        assertEquals(10, overview.getRegenerateCooldownSeconds().get(QuizMode.MIXED));
    }

    @Test
    @DisplayName("createOrResumeAttempt falls back to active set on concurrent DataIntegrityViolationException")
    void testCreateOrResumeAttempt_ConcurrentGenerationFallback() {
        UUID winnerSetId = UUID.randomUUID();
        LessonQuizSet winnerSet = LessonQuizSet.builder()
                .id(winnerSetId)
                .lesson(readyLesson)
                .mode(QuizMode.MIXED)
                .questionCount(1)
                .active(true)
                .build();

        when(lessonRepository.findByIdAndVisibilityStatus(readyLesson.getId(), LessonVisibilityStatus.PUBLISHED))
                .thenReturn(Optional.of(readyLesson));
        when(segmentRepository.findByLessonIdOrderByDisplayOrderAsc(readyLesson.getId()))
                .thenReturn(segments);
        when(attemptRepository.findFirstByUserIdAndLessonIdAndStatusOrderByStartedAtDesc(
                testUser.getId(), readyLesson.getId(), QuizAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(quizSetRepository.findFirstByLessonIdAndModeAndActiveTrueOrderByGeneratedAtDesc(
                readyLesson.getId(), QuizMode.MIXED))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winnerSet));
        when(quizAiService.generateQuestions(eq("Urban Farming"), eq(segments), eq(QuizMode.MIXED), eq(5)))
                .thenReturn(List.of(LessonQuizAiService.GeneratedQuestion.builder()
                        .displayOrder(1)
                        .questionType(QuizQuestionType.MULTIPLE_CHOICE)
                        .questionText("Q?")
                        .options(List.of("A", "B", "C", "D"))
                        .correctIndex((short) 0)
                        .modelAnswer("A")
                        .evidenceSegment(segments.get(0))
                        .build()));
        when(lessonRepository.getReferenceById(readyLesson.getId())).thenReturn(readyLesson);
        when(quizSetRepository.save(any(LessonQuizSet.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("uq_lesson_quiz_sets_active"));
        when(quizSetRepository.findById(winnerSetId)).thenReturn(Optional.of(winnerSet));
        when(quizQuestionRepository.findByQuizSetIdOrderByDisplayOrderAsc(winnerSetId))
                .thenReturn(List.of(LessonQuizQuestion.builder()
                        .id(UUID.randomUUID())
                        .displayOrder(1)
                        .questionType(QuizQuestionType.MULTIPLE_CHOICE)
                        .questionText("Q?")
                        .optionsJson("[\"A\",\"B\",\"C\",\"D\"]")
                        .correctIndex((short) 0)
                        .modelAnswer("A")
                        .build()));
        when(attemptRepository.save(any(UserLessonQuizAttempt.class))).thenAnswer(inv -> {
            UserLessonQuizAttempt a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(redisTemplate.getExpire(anyString())).thenReturn(-2L);

        LessonQuizAttemptResponse response = quizService.createOrResumeAttempt(
                readyLesson.getId(),
                CreateQuizAttemptRequest.builder().mode(QuizMode.MIXED).build(),
                testUser);

        assertEquals(winnerSetId, response.getQuizSetId());
        assertEquals(0, response.getOverallScore());
    }

    @Test
    @DisplayName("submitAnswer throws QUIZ_QUESTION_ALREADY_ANSWERED on concurrent DataIntegrityViolationException")
    void testSubmitAnswer_DoubleSubmitRaceThrowsAlreadyAnswered() {
        UUID attemptId = UUID.randomUUID();
        UUID quizSetId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        LessonQuizSet quizSet = LessonQuizSet.builder().id(quizSetId).lesson(readyLesson).build();
        LessonQuizQuestion question = LessonQuizQuestion.builder()
                .id(questionId)
                .quizSet(quizSet)
                .questionType(QuizQuestionType.MULTIPLE_CHOICE)
                .questionText("Q?")
                .optionsJson("[\"A\",\"B\",\"C\",\"D\"]")
                .correctIndex((short) 0)
                .modelAnswer("A")
                .evidenceSegment(segments.get(0))
                .build();

        UserLessonQuizAttempt attempt = UserLessonQuizAttempt.builder()
                .id(attemptId)
                .user(testUser)
                .lesson(readyLesson)
                .quizSet(quizSet)
                .mode(QuizMode.MULTIPLE_CHOICE)
                .status(QuizAttemptStatus.IN_PROGRESS)
                .totalQuestions(1)
                .build();

        when(lessonRepository.findByIdAndVisibilityStatus(readyLesson.getId(), LessonVisibilityStatus.PUBLISHED))
                .thenReturn(Optional.of(readyLesson));
        when(attemptRepository.findByIdAndUserIdAndLessonId(attemptId, testUser.getId(), readyLesson.getId()))
                .thenReturn(Optional.of(attempt));
        when(quizQuestionRepository.findByIdAndQuizSetId(questionId, quizSetId))
                .thenReturn(Optional.of(question));
        when(answerRepository.existsByAttemptIdAndQuestionId(attemptId, questionId))
                .thenReturn(false);
        when(answerRepository.save(any(UserLessonQuizAnswer.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("uq_quiz_answer_attempt_question"));

        ParroTalkException ex = assertThrows(ParroTalkException.class,
                () -> quizService.submitAnswer(
                        readyLesson.getId(),
                        attemptId,
                        SubmitQuizAnswerRequest.builder().questionId(questionId).selectedIndex(0).build(),
                        testUser));

        assertEquals(ErrorCode.QUIZ_QUESTION_ALREADY_ANSWERED.name(), ex.getErrorCode());
    }
}
