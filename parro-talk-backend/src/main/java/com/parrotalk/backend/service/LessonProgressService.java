package com.parrotalk.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.constant.DictationMode;
import com.parrotalk.backend.dto.AnswerRequest;
import com.parrotalk.backend.dto.DraftSegmentResponse;
import com.parrotalk.backend.dto.LessonProgressResponse;
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
import com.parrotalk.backend.service.dictation.SegmentBlanksCodec;
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
     */
    @Transactional
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
    @Transactional
    public LessonProgressResponse getProgress(User user, UUID lessonId, DictationMode mode) {
        DictationMode resolvedMode = resolveMode(mode);
        UserLessonProgressId key = new UserLessonProgressId(user.getId(), lessonId, resolvedMode);
        UserLessonProgress progress = progressRepository.findById(key)
                .orElseGet(() -> this.createLessonProgress(user, lessonId, resolvedMode));

        return LessonProgressResponse.builder()
                .userId(user.getId())
                .lessonId(lessonId)
                .mode(resolvedMode)
                .currentSegmentId(progress.getCurrentSegmentId())
                .lastProgress(progress.getLastProgress())
                .updatedAt(progress.getUpdatedAt())
                .draftSegments(progress.getDraftSegments() == null
                        ? List.of()
                        : progress.getDraftSegments().stream()
                                .map(draftSegmentMapper::toDraftSegmentResponse)
                                .collect(Collectors.toList()))
                .build();
    }

    /**
     * Submit answer for a segment.
     *
     * @param user     User
     * @param lessonId Lesson id
     * @param request  Answer request
     * @return Draft segment response
     */
    @Transactional
    public DraftSegmentResponse submitAnswer(User user, UUID lessonId, AnswerRequest request) {
        DictationMode resolvedMode = resolveMode(request.getMode());

        TranscriptionSegment segment = segmentRepository.findById(request.getSegmentId())
                .orElseThrow(() -> new ParroTalkException("Segment not found", HttpStatusCode.valueOf(404)));

        if (segment.getLesson() == null || !lessonId.equals(segment.getLesson().getId())) {
            throw new ParroTalkException("Segment does not belong to lesson", HttpStatusCode.valueOf(400));
        }

        // Ensure parent progress row exists before creating/saving child draft due to 3-column FK
        ensureLessonProgressExists(user, lessonId, resolvedMode);
        UserLessonDraftSegment draft = getDraftSegment(user.getId(), lessonId, request.getSegmentId(), resolvedMode);

        if (draft.isCorrect()) {
            throw new ParroTalkException(
                    "Cannot submit answer for correct segment",
                    HttpStatusCode.valueOf(400));
        }

        boolean isCorrect;
        SegmentValidationResult blankValidation = null;
        String storedUserAnswer;

        if (resolvedMode == DictationMode.FULL) {
            isCorrect = ScoringUtil.checkAnswerIsCorrect(request.getUserAnswer(), segment.getText());
            storedUserAnswer = request.getUserAnswer();
        } else {
            List<BlankSlotDto> blanks = resolveBlanksForSegment(segment, resolvedMode);
            if (blanks.isEmpty()) {
                isCorrect = ScoringUtil.checkAnswerIsCorrect(request.getUserAnswer(), segment.getText());
                storedUserAnswer = request.getUserAnswer();
            } else {
                blankValidation = answerValidator.validateSegmentBlanks(
                        segment.getText(),
                        blanks,
                        request.getBlankAnswers());
                isCorrect = blankValidation.isAllCorrect();
                storedUserAnswer = formatBlankUserAnswer(request.getUserAnswer(), request.getBlankAnswers());
            }
        }

        draft.setCorrect(isCorrect);
        int score = isCorrect
                ? ScoringUtil.calculateScore(segment.getText(), draft.getHintCount(), draft.getReplayCount())
                : 0;
        draft.setScore(score);
        draft.setUserAnswer(storedUserAnswer);

        draftSegmentRepository.save(draft);

        learningActivityService.recordSegmentAttempt(
                user.getId(),
                isCorrect,
                request.getStudySecondsDelta() == null ? 0 : request.getStudySecondsDelta());

        updateOverallProgress(user, lessonId, request.getSegmentId(), resolvedMode);

        DraftSegmentResponse response = draftSegmentMapper.toDraftSegmentResponse(draft);
        if (blankValidation != null) {
            response.setBlankResults(blankValidation.getResults());
            response.setAccuracy(blankValidation.getAccuracy());
        }
        return response;
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
        DictationMode resolvedMode = resolveMode(mode);
        ensureLessonProgressExistsByIds(userId, lessonId, resolvedMode);
        UserLessonDraftSegment draft = getDraftSegment(userId, lessonId, segmentId, resolvedMode);
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
        DictationMode resolvedMode = resolveMode(mode);
        ensureLessonProgressExistsByIds(userId, lessonId, resolvedMode);
        UserLessonDraftSegment draft = getDraftSegment(userId, lessonId, segmentId, resolvedMode);
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
        DictationMode resolvedMode = resolveMode(mode);
        List<UserLessonDraftSegment> drafts = draftSegmentRepository.findByUserIdAndLessonIdAndMode(
                user.getId(),
                lessonId,
                resolvedMode);
        if (drafts.isEmpty()) {
            throw new ParroTalkException("No draft progress found for this lesson", HttpStatusCode.valueOf(400));
        }

        Lesson lesson = lessonService.findById(lessonId)
                .orElseThrow(() -> new ParroTalkException("Lesson not found", HttpStatusCode.valueOf(404)));
        Map<UUID, TranscriptionSegment> segmentMap = segmentRepository.findByLessonId(lessonId).stream()
                .collect(Collectors.toMap(TranscriptionSegment::getId, segment -> segment));

        int totalScore = drafts.stream().mapToInt(UserLessonDraftSegment::getScore).sum();
        int averageScore = drafts.isEmpty() ? 0 : totalScore / drafts.size();

        UserLessonHistory history = UserLessonHistory.builder()
                .user(user)
                .lesson(lesson)
                .dictationMode(resolvedMode)
                .overallScore(averageScore)
                .totalTimeSpent(0)
                .build();
        history = historyRepository.save(history);

        List<UserLessonAnswerDetail> details = new ArrayList<>();
        for (UserLessonDraftSegment draft : drafts) {
            UserLessonDraftSegmentId id = draft.getId();
            TranscriptionSegment segment = segmentMap.get(id.getSegmentId());
            log.info("Id: {}, found: {}, text: {}", id.getSegmentId(), segment != null,
                    segment == null ? "null" : segment.getId());

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

        draftSegmentRepository.deleteByUserIdAndLessonIdAndMode(user.getId(), lessonId, resolvedMode);
        learningActivityService.recordCompletedLesson(user.getId(), averageScore);
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
        DictationMode resolvedMode = resolveMode(mode);
        draftSegmentRepository.deleteByUserIdAndLessonIdAndMode(user.getId(), lessonId, resolvedMode);
        progressRepository.deleteById(new UserLessonProgressId(user.getId(), lessonId, resolvedMode));
    }

    private DictationMode resolveMode(DictationMode mode) {
        return mode != null ? mode : DictationMode.FULL;
    }

    private List<BlankSlotDto> resolveBlanksForSegment(TranscriptionSegment segment, DictationMode mode) {
        SegmentBlanksPayload payload = segmentBlanksCodec.deserialize(segment.getBlanksJson());
        if (payload == null) {
            payload = blankGenerator.generateForSegment(segment.getText());
            segment.setBlanksJson(segmentBlanksCodec.serialize(payload));
            segmentRepository.save(segment);
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

    private void updateOverallProgress(
            User user,
            UUID lessonId,
            UUID currentSegmentId,
            DictationMode mode) {
        Lesson lesson = lessonService.findById(lessonId)
                .orElseThrow(() -> new ParroTalkException("Lesson not found",
                        HttpStatusCode.valueOf(404)));

        UserLessonProgressId progressId = new UserLessonProgressId(user.getId(), lessonId, mode);
        UserLessonProgress progress = progressRepository
                .findById(progressId)
                .orElseGet(() -> UserLessonProgress.builder()
                        .id(progressId)
                        .user(user)
                        .lesson(lesson)
                        .draftSegments(new ArrayList<>())
                        .build());

        int completedCount = (int) draftSegmentRepository.countCorrectByUserIdAndLessonIdAndMode(
                user.getId(), lessonId, mode);

        progress.setCurrentSegmentId(currentSegmentId);
        progress.setTotalSegmentsCompleted(completedCount);

        Integer totalSegments = lesson.getTotalSegments();
        if (totalSegments == null || totalSegments <= 0) {
            totalSegments = segmentRepository.findByLessonId(lessonId).size();
        }
        double progressPercent = (totalSegments != null && totalSegments > 0)
                ? (double) completedCount / totalSegments
                : 0.0;
        progressPercent = Math.min(1.0, Math.floor(progressPercent * 100.0) / 100.0);
        progress.setLastProgress(progressPercent);

        progressRepository.save(progress);
    }

    private void ensureLessonProgressExists(User user, UUID lessonId, DictationMode mode) {
        UserLessonProgressId key = new UserLessonProgressId(user.getId(), lessonId, mode);
        if (!progressRepository.existsById(key)) {
            createLessonProgress(user, lessonId, mode);
        }
    }

    private void ensureLessonProgressExistsByIds(UUID userId, UUID lessonId, DictationMode mode) {
        UserLessonProgressId key = new UserLessonProgressId(userId, lessonId, mode);
        if (!progressRepository.existsById(key)) {
            Lesson lesson = lessonService.findById(lessonId).orElseThrow(
                    () -> new ParroTalkException("Lesson not found", HttpStatusCode.valueOf(404)));
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

    private UserLessonProgress createLessonProgress(User user, UUID lessonId, DictationMode mode) {
        Lesson lesson = lessonService.findById(lessonId).orElseThrow(
                () -> new ParroTalkException("Lesson not found", HttpStatusCode.valueOf(404)));

        UserLessonProgressId key = new UserLessonProgressId(user.getId(), lessonId, mode);
        UserLessonProgress progress = UserLessonProgress.builder()
                .id(key)
                .user(user)
                .lesson(lesson)
                .draftSegments(new ArrayList<>())
                .build();
        return progressRepository.save(progress);
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
