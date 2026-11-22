package com.parrotalk.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import com.parrotalk.backend.dto.quiz.LessonQuizOverviewResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@Slf4j
public class LessonQuizService {

    static final int REGENERATE_COOLDOWN_SECONDS = 60;
    private static final String COOLDOWN_KEY_PREFIX = "quiz:regen:cooldown:";
    private static final int HISTORY_LIMIT = 10;

    private final LessonRepository lessonRepository;
    private final TranscriptionSegmentRepository segmentRepository;
    private final LessonQuizSetRepository quizSetRepository;
    private final LessonQuizQuestionRepository quizQuestionRepository;
    private final UserLessonQuizAttemptRepository attemptRepository;
    private final UserLessonQuizAnswerRepository answerRepository;
    private final LessonQuizAiService quizAiService;
    private final UserLearningActivityService userLearningActivityService;
    private final ObjectMapper objectMapper;
    @Nullable
    private final StringRedisTemplate redisTemplate;
    @Nullable
    private final TransactionTemplate transactionTemplate;

    public LessonQuizService(
            LessonRepository lessonRepository,
            TranscriptionSegmentRepository segmentRepository,
            LessonQuizSetRepository quizSetRepository,
            LessonQuizQuestionRepository quizQuestionRepository,
            UserLessonQuizAttemptRepository attemptRepository,
            UserLessonQuizAnswerRepository answerRepository,
            LessonQuizAiService quizAiService,
            UserLearningActivityService userLearningActivityService,
            ObjectMapper objectMapper) {
        this(lessonRepository, segmentRepository, quizSetRepository, quizQuestionRepository,
                attemptRepository, answerRepository, quizAiService, userLearningActivityService,
                objectMapper, null, null);
    }

    @Autowired
    public LessonQuizService(
            LessonRepository lessonRepository,
            TranscriptionSegmentRepository segmentRepository,
            LessonQuizSetRepository quizSetRepository,
            LessonQuizQuestionRepository quizQuestionRepository,
            UserLessonQuizAttemptRepository attemptRepository,
            UserLessonQuizAnswerRepository answerRepository,
            LessonQuizAiService quizAiService,
            UserLearningActivityService userLearningActivityService,
            ObjectMapper objectMapper,
            @Autowired(required = false) @Nullable StringRedisTemplate redisTemplate,
            @Autowired(required = false) @Nullable TransactionTemplate transactionTemplate) {
        this.lessonRepository = lessonRepository;
        this.segmentRepository = segmentRepository;
        this.quizSetRepository = quizSetRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.quizAiService = quizAiService;
        this.userLearningActivityService = userLearningActivityService;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional(readOnly = true)
    public LessonQuizOverviewResponse getOverview(UUID lessonId, User user) {
        requireUser(user);
        LessonContext context = loadAccessibleReadyLesson(lessonId, user);

        int recommendedCount = computeRecommendedQuestionCount(context.segments());

        Optional<UserLessonQuizAttempt> activeAttemptOpt = attemptRepository
                .findFirstByUserIdAndLessonIdAndStatusOrderByStartedAtDesc(
                        user.getId(), lessonId, QuizAttemptStatus.IN_PROGRESS);

        List<QuizMode> cachedModes = quizSetRepository.findByLessonIdAndActiveTrue(lessonId).stream()
                .filter(set -> context.transcriptHash().equals(set.getTranscriptHash()))
                .map(LessonQuizSet::getMode)
                .distinct()
                .toList();

        List<UserLessonQuizAttempt> completedAttempts = attemptRepository
                .findByUserIdAndLessonIdAndStatusOrderByCompletedAtDesc(
                        user.getId(), lessonId, QuizAttemptStatus.COMPLETED, PageRequest.of(0, HISTORY_LIMIT));

        Map<QuizMode, Integer> cooldownByMode = resolveCooldownByMode(user.getId(), lessonId);

        return LessonQuizOverviewResponse.builder()
                .lessonId(lessonId)
                .recommendedQuestionCount(recommendedCount)
                .activeAttempt(activeAttemptOpt.map(this::toSummaryDto).orElse(null))
                .cachedModes(cachedModes)
                .regenerateCooldownSeconds(cooldownByMode)
                .history(completedAttempts.stream().map(this::toSummaryDto).toList())
                .build();
    }

    public LessonQuizAttemptResponse createOrResumeAttempt(
            UUID lessonId,
            CreateQuizAttemptRequest request,
            User user) {
        requireUser(user);
        QuizMode mode = (request != null && request.getMode() != null) ? request.getMode() : QuizMode.MIXED;
        boolean forceNew = request != null && request.isForceNew();

        LessonContext context = executeInTransaction(() -> loadAccessibleReadyLesson(lessonId, user));

        if (!forceNew) {
            Optional<LessonQuizAttemptResponse> existingInProgress = executeInTransaction(() -> attemptRepository
                    .findFirstByUserIdAndLessonIdAndStatusOrderByStartedAtDesc(
                            user.getId(), lessonId, QuizAttemptStatus.IN_PROGRESS)
                    .filter(attempt -> attempt.getMode() == mode)
                    .map(attempt -> buildAttemptResponse(attempt, getRemainingCooldownSeconds(user.getId(), lessonId, mode))));
            if (existingInProgress.isPresent()) {
                return existingInProgress.get();
            }
        }

        Optional<LessonQuizSet> cachedSetOpt = executeInTransaction(() -> quizSetRepository
                .findFirstByLessonIdAndModeAndActiveTrueOrderByGeneratedAtDesc(lessonId, mode)
                .filter(set -> context.transcriptHash().equals(set.getTranscriptHash())));

        LessonQuizSet quizSet;
        if (cachedSetOpt.isPresent()) {
            quizSet = cachedSetOpt.get();
        } else {
            try {
                quizSet = generateAndSaveNewQuizSet(context, mode, user, false);
            } catch (DataIntegrityViolationException ex) {
                log.warn("Concurrent quiz set creation detected for lessonId={}, mode={}; falling back to active set",
                        lessonId, mode, ex);
                quizSet = executeInTransaction(() -> quizSetRepository
                        .findFirstByLessonIdAndModeAndActiveTrueOrderByGeneratedAtDesc(lessonId, mode)
                        .orElseThrow(() -> ex));
            }
        }

        final UUID quizSetId = quizSet.getId();
        return executeInTransaction(() -> {
            Lesson lessonRef = lessonRepository.getReferenceById(lessonId);
            LessonQuizSet managedSet = quizSetRepository.findById(quizSetId)
                    .orElseThrow(() -> new ParroTalkException(ErrorCode.QUIZ_NOT_FOUND));
            List<LessonQuizQuestion> questions = quizQuestionRepository.findByQuizSetIdOrderByDisplayOrderAsc(quizSetId);

            UserLessonQuizAttempt attempt = UserLessonQuizAttempt.builder()
                    .user(user)
                    .lesson(lessonRef)
                    .quizSet(managedSet)
                    .mode(mode)
                    .status(QuizAttemptStatus.IN_PROGRESS)
                    .totalQuestions(questions.size())
                    .answeredCount(0)
                    .correctCount(0)
                    .partialCount(0)
                    .overallScore(0)
                    .startedAt(LocalDateTime.now())
                    .build();
            UserLessonQuizAttempt savedAttempt = attemptRepository.save(attempt);
            int cooldown = getRemainingCooldownSeconds(user.getId(), lessonId, mode);
            return buildAttemptResponse(savedAttempt, questions, List.of(), cooldown);
        });
    }

    public LessonQuizAttemptResponse regenerateQuiz(UUID lessonId, QuizMode mode, User user) {
        requireUser(user);
        QuizMode resolvedMode = mode != null ? mode : QuizMode.MIXED;

        LessonContext context = executeInTransaction(() -> loadAccessibleReadyLesson(lessonId, user));

        int cooldownRemaining = getRemainingCooldownSeconds(user.getId(), lessonId, resolvedMode);
        if (cooldownRemaining > 0) {
            throw new ParroTalkException(
                    "Vui lòng đợi " + cooldownRemaining + " giây trước khi tạo lại bộ câu hỏi.",
                    ErrorCode.QUIZ_GENERATION_COOLDOWN.name(),
                    ErrorCode.QUIZ_GENERATION_COOLDOWN.getHttpStatus());
        }

        LessonQuizSet newSet = generateAndSaveNewQuizSet(context, resolvedMode, user, true);

        final UUID quizSetId = newSet.getId();
        return executeInTransaction(() -> {
            Lesson lessonRef = lessonRepository.getReferenceById(lessonId);
            LessonQuizSet managedSet = quizSetRepository.findById(quizSetId)
                    .orElseThrow(() -> new ParroTalkException(ErrorCode.QUIZ_NOT_FOUND));
            List<LessonQuizQuestion> questions = quizQuestionRepository.findByQuizSetIdOrderByDisplayOrderAsc(quizSetId);

            UserLessonQuizAttempt attempt = UserLessonQuizAttempt.builder()
                    .user(user)
                    .lesson(lessonRef)
                    .quizSet(managedSet)
                    .mode(resolvedMode)
                    .status(QuizAttemptStatus.IN_PROGRESS)
                    .totalQuestions(questions.size())
                    .answeredCount(0)
                    .correctCount(0)
                    .partialCount(0)
                    .overallScore(0)
                    .startedAt(LocalDateTime.now())
                    .build();
            UserLessonQuizAttempt savedAttempt = attemptRepository.save(attempt);
            return buildAttemptResponse(savedAttempt, questions, List.of(), REGENERATE_COOLDOWN_SECONDS);
        });
    }

    @Transactional(readOnly = true)
    public LessonQuizAttemptResponse getAttempt(UUID lessonId, UUID attemptId, User user) {
        requireUser(user);
        loadAccessibleLesson(lessonId, user);
        UserLessonQuizAttempt attempt = attemptRepository.findByIdAndUserIdAndLessonId(attemptId, user.getId(), lessonId)
                .orElseThrow(() -> new ParroTalkException(ErrorCode.QUIZ_NOT_FOUND));
        int cooldown = getRemainingCooldownSeconds(user.getId(), lessonId, attempt.getMode());
        return buildAttemptResponse(attempt, cooldown);
    }

    public QuizAnswerResultDto submitAnswer(
            UUID lessonId,
            UUID attemptId,
            SubmitQuizAnswerRequest request,
            User user) {
        requireUser(user);
        if (request == null || request.getQuestionId() == null) {
            throw new ParroTalkException("questionId is required.", ErrorCode.VALIDATION_ERROR.name(), HttpStatus.BAD_REQUEST);
        }

        AnswerSnapshot snapshot = executeInTransaction(() -> {
            loadAccessibleLesson(lessonId, user);
            UserLessonQuizAttempt attempt = attemptRepository
                    .findByIdAndUserIdAndLessonId(attemptId, user.getId(), lessonId)
                    .orElseThrow(() -> new ParroTalkException(ErrorCode.QUIZ_NOT_FOUND));

            if (attempt.getStatus() == QuizAttemptStatus.COMPLETED) {
                throw new ParroTalkException(ErrorCode.QUIZ_ALREADY_COMPLETED);
            }

            LessonQuizQuestion question = quizQuestionRepository
                    .findByIdAndQuizSetId(request.getQuestionId(), attempt.getQuizSet().getId())
                    .orElseThrow(() -> new ParroTalkException(ErrorCode.QUIZ_NOT_FOUND));

            if (answerRepository.existsByAttemptIdAndQuestionId(attemptId, question.getId())) {
                throw new ParroTalkException(ErrorCode.QUIZ_QUESTION_ALREADY_ANSWERED);
            }

            UUID evidenceSegmentId = question.getEvidenceSegment() != null
                    ? question.getEvidenceSegment().getId()
                    : null;

            return new AnswerSnapshot(
                    question.getId(),
                    attempt.getQuizSet().getId(),
                    question.getQuestionType(),
                    question.getQuestionText(),
                    parseOptions(question.getOptionsJson()),
                    question.getCorrectIndex(),
                    question.getModelAnswer(),
                    question.getExplanationVn(),
                    evidenceSegmentId,
                    question.getEvidenceText(),
                    question.getEvidenceStartTime(),
                    question.getEvidenceEndTime());
        });

        QuizGradingStatus gradingStatus;
        int score;
        String feedback;
        String recordedUserAnswer;
        Short recordedSelectedIndex = null;

        if (snapshot.questionType() == QuizQuestionType.MULTIPLE_CHOICE) {
            Integer selected = request.getSelectedIndex();
            List<String> options = snapshot.options();
            if (selected == null || selected < 0 || selected > 3 || selected >= options.size()) {
                throw new ParroTalkException(
                        "selectedIndex must be between 0 and 3.",
                        ErrorCode.VALIDATION_ERROR.name(),
                        HttpStatus.BAD_REQUEST);
            }
            recordedSelectedIndex = selected.shortValue();
            recordedUserAnswer = options.get(selected);
            boolean correct = snapshot.correctIndex() != null
                    && selected.intValue() == snapshot.correctIndex().intValue();
            if (correct) {
                gradingStatus = QuizGradingStatus.CORRECT;
                score = 100;
                feedback = buildMcqFeedback(true, snapshot.explanationVn());
            } else {
                gradingStatus = QuizGradingStatus.INCORRECT;
                score = 0;
                feedback = buildMcqFeedback(false, snapshot.explanationVn());
            }
        } else {
            String userAnswer = request.getUserAnswer() == null ? "" : request.getUserAnswer().trim();
            recordedUserAnswer = userAnswer;
            LessonQuizAiService.GradingResult gradingResult = quizAiService.gradeFillInTheBlank(
                    snapshot.questionText(),
                    snapshot.modelAnswer(),
                    snapshot.explanationVn(),
                    snapshot.evidenceText(),
                    userAnswer);
            gradingStatus = gradingResult.getStatus();
            score = gradingResult.getScore();
            feedback = gradingResult.getFeedback();
        }

        final Short finalSelectedIndex = recordedSelectedIndex;
        final String finalUserAnswer = recordedUserAnswer;
        final QuizGradingStatus finalStatus = gradingStatus;
        final int finalScore = score;
        final String finalFeedback = feedback;

        return executeInTransaction(() -> {
            UserLessonQuizAttempt attempt = attemptRepository
                    .findByIdAndUserIdAndLessonId(attemptId, user.getId(), lessonId)
                    .orElseThrow(() -> new ParroTalkException(ErrorCode.QUIZ_NOT_FOUND));

            if (attempt.getStatus() == QuizAttemptStatus.COMPLETED) {
                throw new ParroTalkException(ErrorCode.QUIZ_ALREADY_COMPLETED);
            }

            LessonQuizQuestion managedQuestion = quizQuestionRepository
                    .findByIdAndQuizSetId(snapshot.questionId(), snapshot.quizSetId())
                    .orElseThrow(() -> new ParroTalkException(ErrorCode.QUIZ_NOT_FOUND));

            if (answerRepository.existsByAttemptIdAndQuestionId(attemptId, managedQuestion.getId())) {
                throw new ParroTalkException(ErrorCode.QUIZ_QUESTION_ALREADY_ANSWERED);
            }

            UserLessonQuizAnswer savedAnswer;
            try {
                savedAnswer = answerRepository.save(UserLessonQuizAnswer.builder()
                        .attempt(attempt)
                        .question(managedQuestion)
                        .userAnswer(finalUserAnswer)
                        .selectedIndex(finalSelectedIndex)
                        .gradingStatus(finalStatus)
                        .score(finalScore)
                        .feedbackVn(finalFeedback)
                        .answeredAt(LocalDateTime.now())
                        .build());
            } catch (DataIntegrityViolationException ex) {
                throw new ParroTalkException(ErrorCode.QUIZ_QUESTION_ALREADY_ANSWERED);
            }

            List<UserLessonQuizAnswer> allAnswers = answerRepository.findByAttemptId(attemptId);
            int answeredCount = allAnswers.size();
            int correctCount = (int) allAnswers.stream()
                    .filter(a -> a.getGradingStatus() == QuizGradingStatus.CORRECT)
                    .count();
            int partialCount = (int) allAnswers.stream()
                    .filter(a -> a.getGradingStatus() == QuizGradingStatus.PARTIAL)
                    .count();
            int sumScores = allAnswers.stream().mapToInt(UserLessonQuizAnswer::getScore).sum();
            int overallScore = answeredCount == 0 ? 0 : (int) Math.round((double) sumScores / answeredCount);

            attempt.setAnsweredCount(answeredCount);
            attempt.setCorrectCount(correctCount);
            attempt.setPartialCount(partialCount);
            attempt.setOverallScore(overallScore);
            attemptRepository.save(attempt);

            return toAnswerResultDto(managedQuestion, savedAnswer, snapshot.evidenceSegmentId());
        });
    }

    @Transactional
    public LessonQuizAttemptSummaryDto completeAttempt(UUID lessonId, UUID attemptId, User user) {
        requireUser(user);
        loadAccessibleLesson(lessonId, user);

        UserLessonQuizAttempt attempt = attemptRepository
                .findByIdAndUserIdAndLessonId(attemptId, user.getId(), lessonId)
                .orElseThrow(() -> new ParroTalkException(ErrorCode.QUIZ_NOT_FOUND));

        if (attempt.getStatus() == QuizAttemptStatus.COMPLETED) {
            return toSummaryDto(attempt);
        }

        List<UserLessonQuizAnswer> allAnswers = answerRepository.findByAttemptId(attemptId);
        int answeredCount = allAnswers.size();
        int totalQuestions = attempt.getTotalQuestions() != null ? attempt.getTotalQuestions() : 0;

        if (answeredCount < totalQuestions) {
            throw new ParroTalkException(ErrorCode.QUIZ_INCOMPLETE);
        }

        int correctCount = (int) allAnswers.stream()
                .filter(a -> a.getGradingStatus() == QuizGradingStatus.CORRECT)
                .count();
        int partialCount = (int) allAnswers.stream()
                .filter(a -> a.getGradingStatus() == QuizGradingStatus.PARTIAL)
                .count();
        int sumScores = allAnswers.stream().mapToInt(UserLessonQuizAnswer::getScore).sum();
        int overallScore = totalQuestions > 0
                ? (int) Math.round((double) sumScores / totalQuestions)
                : 0;

        attempt.setAnsweredCount(answeredCount);
        attempt.setCorrectCount(correctCount);
        attempt.setPartialCount(partialCount);
        attempt.setOverallScore(overallScore);
        attempt.setStatus(QuizAttemptStatus.COMPLETED);
        attempt.setCompletedAt(LocalDateTime.now());

        UserLessonQuizAttempt saved = attemptRepository.save(attempt);
        userLearningActivityService.recordStudyDay(user.getId());

        return toSummaryDto(saved);
    }

    int computeRecommendedQuestionCount(List<TranscriptionSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return 3;
        }
        int segmentCount = segments.size();
        int wordCount = 0;
        for (TranscriptionSegment segment : segments) {
            if (segment.getText() != null && !segment.getText().isBlank()) {
                wordCount += segment.getText().trim().split("\\s+").length;
            }
        }

        int segTier = tierForSegments(segmentCount);
        int wordTier = tierForWords(wordCount);
        int tier = Math.min(segTier, wordTier);
        return switch (tier) {
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 8;
            default -> 10;
        };
    }

    private int tierForSegments(int segmentCount) {
        if (segmentCount < 8) {
            return 1;
        }
        if (segmentCount <= 20) {
            return 2;
        }
        if (segmentCount <= 40) {
            return 3;
        }
        return 4;
    }

    private int tierForWords(int wordCount) {
        if (wordCount < 120) {
            return 1;
        }
        if (wordCount < 350) {
            return 2;
        }
        if (wordCount < 700) {
            return 3;
        }
        return 4;
    }

    String computeTranscriptHash(List<TranscriptionSegment> segments) {
        StringBuilder sb = new StringBuilder();
        for (TranscriptionSegment seg : segments) {
            sb.append(seg.getDisplayOrder())
                    .append('|')
                    .append(seg.getText() == null ? "" : seg.getText().trim())
                    .append('\n');
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private LessonQuizSet generateAndSaveNewQuizSet(
            LessonContext context,
            QuizMode mode,
            User user,
            boolean recordRegenerateCooldown) {
        int targetCount = computeRecommendedQuestionCount(context.segments());
        List<LessonQuizAiService.GeneratedQuestion> generatedQuestions = quizAiService.generateQuestions(
                context.lesson().getTitle(),
                context.segments(),
                mode,
                targetCount);

        LessonQuizSet savedSet = executeInTransaction(() -> {
            quizSetRepository.deactivateActiveSets(context.lesson().getId(), mode);
            quizSetRepository.flush();

            Lesson lessonRef = lessonRepository.getReferenceById(context.lesson().getId());
            LessonQuizSet quizSet = LessonQuizSet.builder()
                    .lesson(lessonRef)
                    .mode(mode)
                    .questionCount(generatedQuestions.size())
                    .transcriptHash(context.transcriptHash())
                    .active(true)
                    .generatedAt(LocalDateTime.now())
                    .createdByUserId(user.getId())
                    .build();

            List<LessonQuizQuestion> questionEntities = new ArrayList<>(generatedQuestions.size());
            for (LessonQuizAiService.GeneratedQuestion gq : generatedQuestions) {
                questionEntities.add(LessonQuizQuestion.builder()
                        .quizSet(quizSet)
                        .displayOrder(gq.getDisplayOrder())
                        .questionType(gq.getQuestionType())
                        .questionText(gq.getQuestionText())
                        .optionsJson(serializeOptions(gq.getOptions()))
                        .correctIndex(gq.getCorrectIndex())
                        .modelAnswer(gq.getModelAnswer())
                        .explanationVn(gq.getExplanationVn())
                        .evidenceSegment(gq.getEvidenceSegment())
                        .evidenceText(gq.getEvidenceText())
                        .evidenceStartTime(gq.getEvidenceStartTime())
                        .evidenceEndTime(gq.getEvidenceEndTime())
                        .build());
            }
            quizSet.setQuestions(questionEntities);
            return quizSetRepository.save(quizSet);
        });

        if (recordRegenerateCooldown) {
            setCooldown(user.getId(), context.lesson().getId(), mode);
        }
        return savedSet;
    }

    private LessonContext loadAccessibleReadyLesson(UUID lessonId, User user) {
        Lesson lesson = loadAccessibleLesson(lessonId, user);
        List<TranscriptionSegment> segments = segmentRepository.findByLessonIdOrderByDisplayOrderAsc(lessonId);
        if (lesson.getStatus() != LessonStatus.DONE || segments.isEmpty()) {
            throw new ParroTalkException(ErrorCode.QUIZ_TRANSCRIPT_NOT_READY);
        }
        return new LessonContext(lesson, segments, computeTranscriptHash(segments));
    }

    private Lesson loadAccessibleLesson(UUID lessonId, User user) {
        return lessonRepository.findByIdAndVisibilityStatus(lessonId, LessonVisibilityStatus.PUBLISHED)
                .or(() -> user != null ? lessonRepository.findByIdAndOwnerId(lessonId, user.getId()) : Optional.empty())
                .orElseThrow(() -> new ParroTalkException("Lesson not found", HttpStatus.NOT_FOUND));
    }

    private void requireUser(User user) {
        if (user == null || user.getId() == null) {
            throw new ParroTalkException("Authentication is required.", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
    }

    private Map<QuizMode, Integer> resolveCooldownByMode(UUID userId, UUID lessonId) {
        Map<QuizMode, Integer> map = new EnumMap<>(QuizMode.class);
        for (QuizMode mode : QuizMode.values()) {
            map.put(mode, Math.max(0, getRemainingCooldownSeconds(userId, lessonId, mode)));
        }
        return map;
    }

    int getRemainingCooldownSeconds(UUID userId, UUID lessonId, QuizMode mode) {
        if (redisTemplate != null) {
            try {
                Long expireSeconds = redisTemplate.getExpire(buildCooldownKey(userId, lessonId, mode));
                if (expireSeconds != null && expireSeconds > 0) {
                    return expireSeconds.intValue();
                }
                if (expireSeconds != null && expireSeconds == -2) {
                    // Key does not exist in Redis -> check DB fallback only if Redis is unavailable/mocked
                    return 0;
                }
            } catch (Exception ex) {
                log.debug("Redis cooldown lookup failed, falling back to DB check: {}", ex.getMessage());
            }
        }

        return quizSetRepository.findFirstByLessonIdAndModeOrderByGeneratedAtDesc(lessonId, mode)
                .filter(set -> userId.equals(set.getCreatedByUserId()))
                .map(set -> {
                    long elapsed = ChronoUnit.SECONDS.between(set.getGeneratedAt(), LocalDateTime.now());
                    if (elapsed >= 0 && elapsed < REGENERATE_COOLDOWN_SECONDS) {
                        return (int) (REGENERATE_COOLDOWN_SECONDS - elapsed);
                    }
                    return 0;
                })
                .orElse(0);
    }

    private void setCooldown(UUID userId, UUID lessonId, QuizMode mode) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    buildCooldownKey(userId, lessonId, mode),
                    "1",
                    Duration.ofSeconds(REGENERATE_COOLDOWN_SECONDS));
        } catch (Exception ex) {
            log.debug("Failed to write quiz cooldown to Redis: {}", ex.getMessage());
        }
    }

    private String buildCooldownKey(UUID userId, UUID lessonId, QuizMode mode) {
        return COOLDOWN_KEY_PREFIX + userId + ":" + lessonId + ":" + mode.name();
    }

    private LessonQuizAttemptResponse buildAttemptResponse(UserLessonQuizAttempt attempt, int cooldownSeconds) {
        List<LessonQuizQuestion> questions = quizQuestionRepository
                .findByQuizSetIdOrderByDisplayOrderAsc(attempt.getQuizSet().getId());
        List<UserLessonQuizAnswer> answers = answerRepository.findByAttemptId(attempt.getId());
        return buildAttemptResponse(attempt, questions, answers, cooldownSeconds);
    }

    private LessonQuizAttemptResponse buildAttemptResponse(
            UserLessonQuizAttempt attempt,
            List<LessonQuizQuestion> questions,
            List<UserLessonQuizAnswer> answers,
            int cooldownSeconds) {
        Map<UUID, UserLessonQuizAnswer> answerByQuestionId = new HashMap<>();
        for (UserLessonQuizAnswer answer : answers) {
            answerByQuestionId.put(answer.getQuestion().getId(), answer);
        }

        List<LessonQuizQuestionDto> questionDtos = questions.stream()
                .map(q -> {
                    UserLessonQuizAnswer answer = answerByQuestionId.get(q.getId());
                    boolean answered = answer != null;
                    return LessonQuizQuestionDto.builder()
                            .questionId(q.getId())
                            .displayOrder(q.getDisplayOrder())
                            .questionType(q.getQuestionType())
                            .questionText(q.getQuestionText())
                            .options(q.getQuestionType() == QuizQuestionType.MULTIPLE_CHOICE
                                    ? parseOptions(q.getOptionsJson())
                                    : Collections.emptyList())
                            .answered(answered)
                            .answerResult(answered ? toAnswerResultDto(q, answer) : null)
                            .build();
                })
                .toList();

        return LessonQuizAttemptResponse.builder()
                .attemptId(attempt.getId())
                .lessonId(attempt.getLesson().getId())
                .quizSetId(attempt.getQuizSet().getId())
                .mode(attempt.getMode())
                .status(attempt.getStatus())
                .totalQuestions(attempt.getTotalQuestions())
                .answeredCount(attempt.getAnsweredCount())
                .correctCount(attempt.getCorrectCount())
                .partialCount(attempt.getPartialCount())
                .overallScore(attempt.getOverallScore())
                .regenerateCooldownSeconds(cooldownSeconds)
                .startedAt(attempt.getStartedAt())
                .completedAt(attempt.getCompletedAt())
                .questions(questionDtos)
                .build();
    }

    private QuizAnswerResultDto toAnswerResultDto(LessonQuizQuestion question, UserLessonQuizAnswer answer) {
        UUID segmentId = question.getEvidenceSegment() != null ? question.getEvidenceSegment().getId() : null;
        return toAnswerResultDto(question, answer, segmentId);
    }

    private QuizAnswerResultDto toAnswerResultDto(
            LessonQuizQuestion question,
            UserLessonQuizAnswer answer,
            UUID evidenceSegmentId) {
        return QuizAnswerResultDto.builder()
                .questionId(question.getId())
                .userAnswer(answer.getUserAnswer())
                .selectedIndex(answer.getSelectedIndex() != null ? answer.getSelectedIndex().intValue() : null)
                .status(answer.getGradingStatus())
                .score(answer.getScore())
                .feedback(answer.getFeedbackVn())
                .modelAnswer(question.getModelAnswer())
                .correctIndex(question.getCorrectIndex() != null ? question.getCorrectIndex().intValue() : null)
                .explanationVn(question.getExplanationVn())
                .evidenceSegmentId(evidenceSegmentId)
                .evidenceText(question.getEvidenceText())
                .evidenceStartTime(question.getEvidenceStartTime())
                .evidenceEndTime(question.getEvidenceEndTime())
                .build();
    }

    private LessonQuizAttemptSummaryDto toSummaryDto(UserLessonQuizAttempt attempt) {
        return LessonQuizAttemptSummaryDto.builder()
                .attemptId(attempt.getId())
                .quizSetId(attempt.getQuizSet().getId())
                .mode(attempt.getMode())
                .status(attempt.getStatus())
                .totalQuestions(attempt.getTotalQuestions())
                .answeredCount(attempt.getAnsweredCount())
                .correctCount(attempt.getCorrectCount())
                .partialCount(attempt.getPartialCount())
                .overallScore(attempt.getOverallScore() != null ? attempt.getOverallScore() : 0)
                .startedAt(attempt.getStartedAt())
                .completedAt(attempt.getCompletedAt())
                .build();
    }

    private String serializeOptions(List<String> options) {
        if (options == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize quiz options", e);
        }
    }

    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize quiz options JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildMcqFeedback(boolean correct, String explanationVn) {
        String base = correct ? "Chính xác!" : "Chưa chính xác.";
        if (explanationVn == null || explanationVn.isBlank()) {
            return base;
        }
        return base + " " + explanationVn.trim();
    }

    private <T> T executeInTransaction(Supplier<T> supplier) {
        if (transactionTemplate != null) {
            return transactionTemplate.execute(status -> supplier.get());
        }
        return supplier.get();
    }

    private record LessonContext(Lesson lesson, List<TranscriptionSegment> segments, String transcriptHash) {}

    private record AnswerSnapshot(
            UUID questionId,
            UUID quizSetId,
            QuizQuestionType questionType,
            String questionText,
            List<String> options,
            Short correctIndex,
            String modelAnswer,
            String explanationVn,
            UUID evidenceSegmentId,
            String evidenceText,
            Double evidenceStartTime,
            Double evidenceEndTime) {}
}
