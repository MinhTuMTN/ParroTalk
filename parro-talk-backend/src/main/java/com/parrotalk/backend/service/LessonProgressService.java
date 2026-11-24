package com.parrotalk.backend.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.parrotalk.backend.constant.DictationMode;
import com.parrotalk.backend.dto.AnswerRequest;
import com.parrotalk.backend.dto.DraftSegmentResponse;
import com.parrotalk.backend.dto.LessonProgressResponse;
import com.parrotalk.backend.dto.SegmentSubmissionProjection;
import com.parrotalk.backend.dto.SubmitLessonResponse;
import com.parrotalk.backend.dto.dictation.BlankAnswerItem;
import com.parrotalk.backend.dto.dictation.BlankSlotDto;
import com.parrotalk.backend.dto.dictation.SegmentBlanksPayload;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserLessonAnswerDetail;
import com.parrotalk.backend.entity.UserLessonDraftSegment;
import com.parrotalk.backend.entity.UserLessonDraftSegment.UserLessonDraftSegmentId;
import com.parrotalk.backend.entity.UserLessonHistory;
import com.parrotalk.backend.entity.UserLessonProgress;
import com.parrotalk.backend.entity.UserLessonProgress.UserLessonProgressId;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.mapper.DraftSegmentMapper;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;
import com.parrotalk.backend.repository.UserLessonAnswerDetailRepository;
import com.parrotalk.backend.repository.UserLessonDraftSegmentRepository;
import com.parrotalk.backend.repository.UserLessonHistoryRepository;
import com.parrotalk.backend.repository.UserLessonProgressRepository;
import com.parrotalk.backend.service.dictation.DictationAnswerValidator;
import com.parrotalk.backend.service.dictation.DictationAnswerValidator.SegmentValidationResult;
import com.parrotalk.backend.service.dictation.DictationBlankGenerator;
import com.parrotalk.backend.util.SegmentBlanksCodec;
import com.parrotalk.backend.util.ScoringUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonProgressService {

    /** User Lesson Progress Repository */
    private final UserLessonProgressRepository progressRepository;

    /** User Lesson Draft Segment Repository */
    private final UserLessonDraftSegmentRepository draftSegmentRepository;

    /** User Lesson History Repository */
    private final UserLessonHistoryRepository historyRepository;

    /** User Lesson Answer Detail Repository */
    private final UserLessonAnswerDetailRepository answerDetailRepository;

    /** Transcription Segment Repository */
    private final TranscriptionSegmentRepository segmentRepository;

    /** Lesson Service */
    private final LessonService lessonService;

    /** Shared learner activity service. */
    private final UserLearningActivityService learningActivityService;

    /** Library cache service. */
    private final LibraryCacheService libraryCacheService;

    /** Draft Segment Mapper */
    private final DraftSegmentMapper draftSegmentMapper;

    /** Fill-in-the-blank answer validator */
    private final DictationAnswerValidator answerValidator;

    /** Fill-in-the-blank generator for lazy backfill fallback */
    private final DictationBlankGenerator blankGenerator;

    /** JSON codec for segment blanks */
    private final SegmentBlanksCodec segmentBlanksCodec;

    /** Entity Manager */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Get lesson progress for a user in default FULL mode.
     *
     * @param user     User
     * @param lessonId Lesson id
     * @return Lesson progress response
     */
    @Transactional(readOnly = true)
    public LessonProgressResponse getProgress(User user, UUID lessonId) {
        return getProgress(user, lessonId, DictationMode.FULL);
    }

    /**
     * Get lesson progress for a user scoped by dictation mode.
     *
     * @param user     User
     * @param lessonId Lesson id
     * @param mode     Optional dictation mode (defaults to FULL)
     * @return Lesson progress response
     */
    @Transactional(readOnly = true)
    public LessonProgressResponse getProgress(User user, UUID lessonId, DictationMode mode) {
        UserLessonProgressId progressId = new UserLessonProgressId(user.getId(), lessonId, mode);
        UserLessonProgress progress = progressRepository.findById(progressId).orElse(null);

        if (progress == null) {
            lessonService.verifyExistsById(lessonId);
            return LessonProgressResponse.builder()
                    .userId(user.getId())
                    .lessonId(lessonId)
                    .mode(mode)
                    .lastProgress(0.0)
                    .draftSegments(List.of())
                    .build();
        }

        List<DraftSegmentResponse> draftSegments = progress.getDraftSegments() == null
                ? List.of()
                : progress.getDraftSegments().stream()
                        .map(draftSegmentMapper::toDraftSegmentResponse)
                        .collect(Collectors.toList());

        return LessonProgressResponse.builder()
                .userId(user.getId())
                .lessonId(lessonId)
                .mode(mode)
                .currentSegmentId(progress.getCurrentSegmentId())
                .lastProgress(progress.getLastProgress())
                .updatedAt(progress.getUpdatedAt())
                .draftSegments(draftSegments)
                .build();
    }

    /**
     * Submit an answer for a segment and update lesson progress.
     *
     * @param user     Authenticated user
     * @param lessonId Lesson id
     * @param request  Answer submission request
     * @return Draft segment response
     */
    @Transactional
    public DraftSegmentResponse submitAnswer(User user, UUID lessonId, AnswerRequest request) {
        DictationMode mode = request.getMode();

        // 1. Validate segment existence and lesson ownership in a single query
        SegmentSubmissionProjection target = segmentRepository.findSubmissionTarget(request.getSegmentId(), lessonId)
                .orElseThrow(() -> new ParroTalkException(
                        "Segment does not exist or does not belong to lesson " + lessonId,
                        HttpStatusCode.valueOf(400)));

        // 2. Load existing progress/draft or initialize new ones
        ProgressDraftContext context = loadOrInitializeProgressAndDraft(
                user.getId(),
                lessonId,
                request.getSegmentId(),
                mode);
        UserLessonProgress progress = context.progress();
        UserLessonDraftSegment draft = context.draft();

        if (draft.isCorrect()) {
            throw new ParroTalkException(
                    "Cannot submit answer for correct segment",
                    HttpStatusCode.valueOf(400));
        }

        // 3. Evaluate answer (FULL dictation vs blank-fill mode) and calculate score
        AnswerEvaluation evaluation = evaluateSubmission(request, target, mode);
        int score = evaluation.correct()
                ? ScoringUtil.calculateScore(target.getText(), draft.getHintCount(), draft.getReplayCount())
                : 0;

        draft.setCorrect(evaluation.correct());
        draft.setScore(score);
        draft.setUserAnswer(evaluation.storedUserAnswer());

        // 4. Persist parent progress (if new) before child draft, then record learning activity
        // Flush new parent progress before persisting child draft so hibernate.order_inserts never violates the 3-column FK
        if (context.isNewProgress()) {
            progressRepository.saveAndFlush(progress);
        }
        draftSegmentRepository.save(draft);

        learningActivityService.recordSegmentAttempt(
                user.getId(),
                evaluation.correct(),
                request.getStudySecondsDelta() == null ? 0 : request.getStudySecondsDelta());

        // 5. Recalculate lesson completion percentage and current segment pointer
        updateLessonProgressMetrics(
                progress,
                user.getId(),
                lessonId,
                request.getSegmentId(),
                mode,
                target,
                context.isNewProgress(),
                evaluation.correct());

        // 6. Build response and schedule cache eviction after commit
        DraftSegmentResponse response = draftSegmentMapper.toDraftSegmentResponse(draft);
        if (evaluation.blankValidation() != null) {
            response.setBlankResults(evaluation.blankValidation().getResults());
            response.setAccuracy(evaluation.blankValidation().getAccuracy());
        }
        evictUserLibraryCacheAfterCommit(user.getId());
        return response;
    }

    /**
     * Load existing progress and draft segment in a single LEFT JOIN query, or initialize new entities when absent.
     */
    private ProgressDraftContext loadOrInitializeProgressAndDraft(
            UUID userId,
            UUID lessonId,
            UUID segmentId,
            DictationMode mode) {
        List<Object[]> rows = progressRepository.findProgressAndDraft(userId, lessonId, segmentId, mode);
        if (rows.isEmpty()) {
            UserLessonProgress progress = UserLessonProgress.builder()
                    .id(new UserLessonProgressId(userId, lessonId, mode))
                    .user(entityManager.getReference(User.class, userId))
                    .lesson(entityManager.getReference(Lesson.class, lessonId))
                    .draftSegments(new ArrayList<>())
                    .build();
            UserLessonDraftSegment draft = UserLessonDraftSegment.builder()
                    .id(new UserLessonDraftSegmentId(userId, lessonId, segmentId, mode))
                    .build();
            return new ProgressDraftContext(progress, draft, true);
        }

        Object[] row = rows.get(0);
        UserLessonProgress progress = (UserLessonProgress) row[0];
        UserLessonDraftSegment draft = (UserLessonDraftSegment) row[1];
        if (draft == null) {
            draft = UserLessonDraftSegment.builder()
                    .id(new UserLessonDraftSegmentId(userId, lessonId, segmentId, mode))
                    .build();
        }
        return new ProgressDraftContext(progress, draft, false);
    }

    /**
     * Validate the submitted answer against FULL mode text or mode-specific blank slots.
     */
    private AnswerEvaluation evaluateSubmission(
            AnswerRequest request,
            SegmentSubmissionProjection target,
            DictationMode mode) {
        // FULL mode grades the entire sentence; blank-fill modes (EASY/MEDIUM/HARD) fall back to FULL when no blanks exist
        List<BlankSlotDto> blanks = (mode == DictationMode.FULL)
                ? List.of()
                : resolveBlanksForSegment(target, mode);
        if (blanks.isEmpty()) {
            boolean isCorrect = ScoringUtil.checkAnswerIsCorrect(request.getUserAnswer(), target.getText());
            return new AnswerEvaluation(isCorrect, request.getUserAnswer(), null);
        }

        // Blank-fill modes (EASY/MEDIUM/HARD): validate each submitted blank independently
        SegmentValidationResult blankValidation = answerValidator.validateSegmentBlanks(
                target.getText(),
                blanks,
                request.getBlankAnswers());
        String storedUserAnswer = formatBlankUserAnswer(request.getUserAnswer(), request.getBlankAnswers());
        return new AnswerEvaluation(blankValidation.isAllCorrect(), storedUserAnswer, blankValidation);
    }

    /**
     * Update completed segment count, completion ratio, and current segment pointer on UserLessonProgress.
     */
    private void updateLessonProgressMetrics(
            UserLessonProgress progress,
            UUID userId,
            UUID lessonId,
            UUID segmentId,
            DictationMode mode,
            SegmentSubmissionProjection target,
            boolean isNewProgress,
            boolean isCorrect) {
        Integer totalSegments = target.getLessonTotalSegments();
        if (totalSegments == null || totalSegments <= 0) {
            totalSegments = (int) segmentRepository.countByLessonId(lessonId);
        }

        int completedCount;
        if (isNewProgress) {
            completedCount = isCorrect ? 1 : 0;
        } else if (progress.getTotalSegmentsCompleted() != null) {
            // Draft was guaranteed not-correct before this submission, so only a correct answer increments the count
            completedCount = progress.getTotalSegmentsCompleted() + (isCorrect ? 1 : 0);
            if (totalSegments != null && totalSegments > 0) {
                completedCount = Math.max(0, Math.min(totalSegments, completedCount));
            } else {
                completedCount = Math.max(0, completedCount);
            }
        } else {
            completedCount = (int) draftSegmentRepository.countCorrectByUserIdAndLessonIdAndMode(
                    userId,
                    lessonId,
                    mode);
        }

        double progressPercent = (totalSegments != null && totalSegments > 0)
                ? (double) completedCount / totalSegments
                : 0.0;
        progressPercent = Math.min(1.0, Math.floor(progressPercent * 100.0) / 100.0);

        progress.setCurrentSegmentId(segmentId);
        progress.setTotalSegmentsCompleted(completedCount);
        progress.setLastProgress(progressPercent);
    }

    private record ProgressDraftContext(
            UserLessonProgress progress,
            UserLessonDraftSegment draft,
            boolean isNewProgress) {
    }

    private record AnswerEvaluation(
            boolean correct,
            String storedUserAnswer,
            SegmentValidationResult blankValidation) {
    }

    /**
     * Increment replay count in default FULL mode.
     */
    @Transactional
    public void incrementReplay(UUID userId, UUID lessonId, UUID segmentId) {
        incrementReplay(userId, lessonId, segmentId, DictationMode.FULL);
    }

    /**
     * Increment replay count scoped by dictation mode.
     */
    @Transactional
    public void incrementReplay(UUID userId, UUID lessonId, UUID segmentId, DictationMode mode) {
        ensureLessonProgressExistsByIds(userId, lessonId, mode);
        UserLessonDraftSegment draft = getDraftSegment(userId, lessonId, segmentId, mode);
        if (draft.isCorrect()) {
            throw new ParroTalkException(
                    "Cannot increment replay count for correct answer",
                    HttpStatusCode.valueOf(400));
        }

        draft.setReplayCount(draft.getReplayCount() + 1);
        draftSegmentRepository.save(draft);
    }

    /**
     * Increment hint count in default FULL mode.
     */
    @Transactional
    public void incrementHint(UUID userId, UUID lessonId, UUID segmentId) {
        incrementHint(userId, lessonId, segmentId, DictationMode.FULL);
    }

    /**
     * Increment hint count scoped by dictation mode.
     */
    @Transactional
    public void incrementHint(UUID userId, UUID lessonId, UUID segmentId, DictationMode mode) {
        ensureLessonProgressExistsByIds(userId, lessonId, mode);
        UserLessonDraftSegment draft = getDraftSegment(userId, lessonId, segmentId, mode);
        if (draft.isCorrect()) {
            throw new ParroTalkException(
                    "Cannot increment hint count for correct answer",
                    HttpStatusCode.valueOf(400));
        }

        draft.setHintCount(draft.getHintCount() + 1);
        draftSegmentRepository.save(draft);
    }

    /**
     * Submit lesson in default FULL mode.
     */
    @Transactional
    public SubmitLessonResponse submitLesson(User user, UUID lessonId) {
        return submitLesson(user, lessonId, DictationMode.FULL);
    }

    /**
     * Submit lesson scoped by dictation mode.
     */
    @Transactional
    public SubmitLessonResponse submitLesson(User user, UUID lessonId, DictationMode mode) {
        List<UserLessonDraftSegment> drafts = draftSegmentRepository.findByUserIdAndLessonIdAndMode(
                user.getId(),
                lessonId,
                mode);
        if (drafts.isEmpty()) {
            throw new ParroTalkException("No draft progress found for this lesson", HttpStatusCode.valueOf(400));
        }

        Set<UUID> segmentIds = new HashSet<>(segmentRepository.findIdsByLessonId(lessonId));
        int totalSegments = segmentIds.size();
        if (totalSegments <= 0) {
            lessonService.verifyExistsById(lessonId);
            throw new ParroTalkException("Lesson has no segments to submit", HttpStatusCode.valueOf(400));
        }

        List<UserLessonDraftSegment> validDrafts = drafts.stream()
                .filter(draft -> draft.getId() != null
                        && segmentIds.contains(draft.getId().getSegmentId())
                        && draft.isCorrect())
                .toList();

        long completedCorrectCount = validDrafts.stream()
                .map(draft -> draft.getId().getSegmentId())
                .distinct()
                .count();
        if (completedCorrectCount < totalSegments) {
            throw new ParroTalkException(
                    "All segments must be completed before submitting the lesson",
                    HttpStatusCode.valueOf(400));
        }

        int totalScore = validDrafts.stream().mapToInt(UserLessonDraftSegment::getScore).sum();
        int averageScore = validDrafts.isEmpty() ? 0 : totalScore / validDrafts.size();

        User userRef = entityManager.getReference(User.class, user.getId());
        Lesson lessonRef = entityManager.getReference(Lesson.class, lessonId);
        UserLessonHistory history = UserLessonHistory.builder()
                .user(userRef)
                .lesson(lessonRef)
                .dictationMode(mode)
                .overallScore(averageScore)
                .totalTimeSpent(0)
                .build();
        history = historyRepository.save(history);

        List<UserLessonAnswerDetail> details = new ArrayList<>();
        for (UserLessonDraftSegment draft : validDrafts) {
            UserLessonDraftSegmentId id = draft.getId();
            log.debug("Submitting answer detail for segment id: {}", id.getSegmentId());

            TranscriptionSegment segmentRef = entityManager.getReference(TranscriptionSegment.class,
                    id.getSegmentId());

            UserLessonAnswerDetail detail = UserLessonAnswerDetail.builder()
                    .segment(segmentRef)
                    .userAnswer(draft.getUserAnswer())
                    .score(draft.getScore())
                    .history(history)
                    .build();
            details.add(detail);
        }
        answerDetailRepository.saveAll(details);

        draftSegmentRepository.deleteByUserIdAndLessonIdAndMode(user.getId(), lessonId, mode);
        learningActivityService.recordCompletedLesson(user.getId(), averageScore);
        evictUserLibraryCacheAfterCommit(user.getId());
        return SubmitLessonResponse.builder()
                .score((double) averageScore)
                .passed(true)
                .build();
    }

    @Transactional
    public void resetProgress(User user, UUID lessonId) {
        resetProgress(user, lessonId, DictationMode.FULL);
    }

    @Transactional
    public void resetProgress(User user, UUID lessonId, DictationMode mode) {
        draftSegmentRepository.deleteByUserIdAndLessonIdAndMode(user.getId(), lessonId, mode);
        progressRepository.deleteById(new UserLessonProgressId(user.getId(), lessonId, mode));
        evictUserLibraryCacheAfterCommit(user.getId());
    }

    private void evictUserLibraryCacheAfterCommit(UUID userId) {
        if (userId == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    libraryCacheService.evictUserProcessingSections(userId);
                }
            });
        } else {
            libraryCacheService.evictUserProcessingSections(userId);
        }
    }

    /**
     * Resolve blank slots for a segment in the requested dictation mode, falling back to lazy backfill for legacy segments.
     *
     * @param target Segment and lesson metadata projection
     * @param mode   Dictation mode
     * @return List of blank slots for the mode
     */
    private List<BlankSlotDto> resolveBlanksForSegment(SegmentSubmissionProjection target, DictationMode mode) {
        SegmentBlanksPayload payload = segmentBlanksCodec.deserialize(target.getBlanksJson());
        if (payload == null) {
            payload = blankGenerator.generateForSegment(target.getText());
            String serialized = segmentBlanksCodec.serialize(payload);
            segmentRepository.findById(target.getId()).ifPresent(segment -> {
                segment.setBlanksJson(serialized);
                segmentRepository.save(segment);
            });
        }
        if (payload.getLevels() == null) {
            return List.of();
        }
        return payload.getLevels().getOrDefault(mode, List.of());
    }

    private String formatBlankUserAnswer(String rawUserAnswer, List<BlankAnswerItem> blankAnswers) {
        if (rawUserAnswer != null && !rawUserAnswer.isBlank()) {
            return rawUserAnswer;
        }
        if (blankAnswers == null || blankAnswers.isEmpty()) {
            return "";
        }
        return blankAnswers.stream()
                .map(item -> item == null || item.getAnswer() == null ? "" : item.getAnswer().trim())
                .collect(Collectors.joining(" | "));
    }

    private UserLessonDraftSegment getDraftSegment(
            UUID userId,
            UUID lessonId,
            UUID segmentId,
            DictationMode mode) {
        return draftSegmentRepository
                .findExactByMode(userId, lessonId, segmentId, mode)
                .orElseGet(() -> this.createDraftSegment(userId, lessonId, segmentId, mode));
    }

    /**
     * Ensure a progress row exists for the given user, lesson, and dictation mode.
     *
     * @param userId   User id
     * @param lessonId Lesson id
     * @param mode     Dictation mode
     */
    private void ensureLessonProgressExistsByIds(UUID userId, UUID lessonId, DictationMode mode) {
        UserLessonProgressId key = new UserLessonProgressId(userId, lessonId, mode);
        if (!progressRepository.existsById(key)) {
            Lesson lesson = getLessonReference(lessonId);
            User userRef = entityManager.getReference(User.class, userId);
            UserLessonProgress progress = UserLessonProgress.builder()
                    .id(key)
                    .user(userRef)
                    .lesson(lesson)
                    .draftSegments(new ArrayList<>())
                    .build();
            progressRepository.save(progress);
        }
    }

    /**
     * Verify lesson existence and return a lazy JPA reference without loading associations.
     *
     * @param lessonId Lesson id
     * @return Lazy lesson entity reference
     */
    private Lesson getLessonReference(UUID lessonId) {
        lessonService.verifyExistsById(lessonId);
        return entityManager.getReference(Lesson.class, lessonId);
    }

    private UserLessonDraftSegment createDraftSegment(
            UUID userId,
            UUID lessonId,
            UUID segmentId,
            DictationMode mode) {
        UserLessonDraftSegmentId id = new UserLessonDraftSegmentId(userId, lessonId, segmentId, mode);
        UserLessonDraftSegment draftSegment = UserLessonDraftSegment.builder()
                .id(id)
                .build();
        return draftSegmentRepository.save(draftSegment);
    }
}
