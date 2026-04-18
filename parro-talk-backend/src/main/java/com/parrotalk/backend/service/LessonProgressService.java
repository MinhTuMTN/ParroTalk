package com.parrotalk.backend.service;

import com.parrotalk.backend.dto.*;
import com.parrotalk.backend.entity.*;
import com.parrotalk.backend.entity.UserLessonDraftSegment.UserLessonDraftSegmentId;
import com.parrotalk.backend.entity.UserLessonProgress.UserLessonProgressId;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.mapper.DraftSegmentMapper;
import com.parrotalk.backend.repository.*;
import com.parrotalk.backend.util.ScoringUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    /** Draft Segment Mapper */
    private final DraftSegmentMapper draftSegmentMapper;

    /**
     * Get lesson progress for a user.
     * 
     * @param user     User
     * @param lessonId Lesson id
     * @return Lesson progress response
     */
    public LessonProgressResponse getProgress(User user, UUID lessonId) {

        UserLessonProgressId key = new UserLessonProgressId(user.getId(), lessonId);
        UserLessonProgress progress = progressRepository.findById(key)
                .orElseGet(() -> this.createLessonProgress(user, lessonId));

        return LessonProgressResponse.builder()
                .userId(user.getId())
                .lessonId(lessonId)
                .currentSegmentId(progress.getCurrentSegmentId())
                .lastProgress(progress.getLastProgress())
                .updatedAt(progress.getUpdatedAt())
                .draftSegments(progress.getDraftSegments().stream()
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
        // Get segment
        TranscriptionSegment segment = segmentRepository.findById(request.getSegmentId())
                .orElseThrow(() -> new RuntimeException("Segment not found"));

        // Get draft segment
        UserLessonDraftSegment draft = getDraftSegment(user.getId(), lessonId, request.getSegmentId());

        if (draft.isCorrect()) {
            throw new ParroTalkException(
                    "Cannot submit answer for correct segment",
                    HttpStatusCode.valueOf(400));
        }

        // Check answer is correct
        boolean isCorrect = ScoringUtil.checkAnswerIsCorrect(request.getUserAnswer(), segment.getText());
        draft.setCorrect(isCorrect);

        // Calculate score
        int score = ScoringUtil.calculateScore(segment.getText(), draft.getHintCount(), draft.getReplayCount());
        draft.setScore(score);

        // Set user answer
        draft.setUserAnswer(request.getUserAnswer());

        // Save draft segment
        draftSegmentRepository.save(draft);

        // Update overall progress
        updateOverallProgress(user, lessonId, request.getSegmentId());

        return draftSegmentMapper.toDraftSegmentResponse(draft);
    }

    /**
     * Increment replay count
     * 
     * @param userId    User id
     * @param lessonId  Lesson id
     * @param segmentId Segment id
     */
    @Transactional
    public void incrementReplay(UUID userId, UUID lessonId, UUID segmentId) {
        UserLessonDraftSegment draft = getDraftSegment(userId, lessonId, segmentId);
        if (draft.isCorrect()) {
            throw new ParroTalkException(
                    "Cannot increment replay count for correct answer",
                    HttpStatusCode.valueOf(400));
        }

        draft.setReplayCount(draft.getReplayCount() + 1);
        draftSegmentRepository.save(draft);
    }

    /**
     * Increment hint count
     * 
     * @param userId    User id
     * @param lessonId  Lesson id
     * @param segmentId Segment id
     */
    @Transactional
    public void incrementHint(UUID userId, UUID lessonId, UUID segmentId) {
        UserLessonDraftSegment draft = getDraftSegment(userId, lessonId, segmentId);
        if (draft.isCorrect()) {
            throw new ParroTalkException(
                    "Cannot increment hint count for correct answer",
                    HttpStatusCode.valueOf(400));
        }

        draft.setHintCount(draft.getHintCount() + 1);
        draftSegmentRepository.save(draft);
    }

    /**
     * Submit lesson
     * 
     * @param user     User
     * @param lessonId Lesson id
     * @return Submit lesson response
     */
    @Transactional
    public SubmitLessonResponse submitLesson(User user, UUID lessonId) {
        // Get all draft segments
        List<UserLessonDraftSegment> drafts = draftSegmentRepository.findByUserIdAndLessonId(user.getId(),
                lessonId);
        if (drafts.isEmpty()) {
            throw new RuntimeException("No draft progress found for this lesson");
        }

        Lesson lesson = lessonService.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        Map<UUID, TranscriptionSegment> segmentMap = segmentRepository.findByLessonId(lessonId).stream()
                .collect(Collectors.toMap(TranscriptionSegment::getId, segment -> segment));

        int totalScore = drafts.stream().mapToInt(UserLessonDraftSegment::getScore).sum();
        int averageScore = drafts.isEmpty() ? 0 : totalScore / drafts.size();

        // 1. Create History
        UserLessonHistory history = UserLessonHistory.builder()
                .user(user)
                .lesson(lesson)
                .overallScore(averageScore)
                .submittedAt(LocalDateTime.now())
                .totalTimeSpent(0) // Logic for time tracking can be added
                .build();
        history = historyRepository.save(history);

        // 2. Create Details
        List<UserLessonAnswerDetail> details = new ArrayList<>();
        for (UserLessonDraftSegment draft : drafts) {
            UserLessonDraftSegment.UserLessonDraftSegmentId id = draft.getId();
            TranscriptionSegment segment = segmentMap.get(id.getSegmentId());

            UserLessonAnswerDetail detail = UserLessonAnswerDetail.builder()
                    .segment(segment)
                    .userAnswer(draft.getUserAnswer())
                    .score(draft.getScore())
                    .history(history)
                    .build();
            details.add(detail);
        }
        answerDetailRepository.saveAll(details);

        // 3. Cleanup Drafts
        draftSegmentRepository.deleteByUserIdAndLessonId(user.getId(), lessonId);
        return SubmitLessonResponse.builder()
                .score((double) averageScore)
                .passed(true)
                .build();
    }

    @Transactional
    public void resetProgress(User user, UUID lessonId) {
        draftSegmentRepository.deleteByUserIdAndLessonId(user.getId(), lessonId);
        progressRepository.deleteById(new UserLessonProgress.UserLessonProgressId(user.getId(), lessonId));
    }

    /**
     * Get draft segment
     *
     * @param userId    User id
     * @param lessonId  Lesson id
     * @param segmentId Segment id
     * @return Draft segment
     */
    private UserLessonDraftSegment getDraftSegment(UUID userId, UUID lessonId, UUID segmentId) {
        return draftSegmentRepository
                .findExact(userId, lessonId, segmentId)
                .orElseGet(() -> this.createDraftSegment(userId, lessonId, segmentId));
    }

    /**
     * Update overall progress
     * 
     * @param user             User
     * @param lessonId         Lesson id
     * @param currentSegmentId Current segment id
     */
    private void updateOverallProgress(User user, UUID lessonId, UUID currentSegmentId) {
        // Get lesson
        Lesson lesson = lessonService.findById(lessonId)
                .orElseThrow(() -> new ParroTalkException("Lesson not found",
                        HttpStatusCode.valueOf(404)));

        // Get or create progress
        UserLessonProgress progress = progressRepository
                .findById(new UserLessonProgress.UserLessonProgressId(user.getId(), lessonId))
                .orElse(UserLessonProgress.builder()
                        .user(user)
                        .lesson(lesson)
                        .build());

        // Update progress
        progress.setCurrentSegmentId(currentSegmentId);
        progress.setTotalSegmentsCompleted(progress.getTotalSegmentsCompleted() + 1);

        double progressPercent = (double) progress.getTotalSegmentsCompleted() / lesson.getTotalSegments();
        progressPercent = Math.floor(progressPercent * 100) / 100;
        progress.setLastProgress(progressPercent);

        // Save progress
        progressRepository.save(progress);
    }

    /**
     * Create lesson progress
     * 
     * @param user     User
     * @param lessonId Lesson id
     * @return UserLessonProgress
     */
    private UserLessonProgress createLessonProgress(User user, UUID lessonId) {
        Lesson lesson = lessonService.findById(lessonId).orElseThrow(
                () -> new ParroTalkException("Lesson not found", HttpStatusCode.valueOf(404)));

        UserLessonProgressId key = new UserLessonProgressId(user.getId(), lessonId);
        UserLessonProgress progress = UserLessonProgress.builder()
                .id(key)
                .user(user)
                .lesson(lesson)
                .draftSegments(new ArrayList<>())
                .build();
        return progressRepository.save(progress);
    }

    /**
     * Create draft segment
     * 
     * @param userId    User id
     * @param lessonId  Lesson id
     * @param segmentId Segment id
     * @return UserLessonDraftSegment
     */
    private UserLessonDraftSegment createDraftSegment(UUID userId, UUID lessonId, UUID segmentId) {
        UserLessonDraftSegmentId id = new UserLessonDraftSegmentId(userId, lessonId, segmentId);
        UserLessonDraftSegment draftSegment = UserLessonDraftSegment.builder()
                .id(id)
                .build();
        return draftSegmentRepository.save(draftSegment);
    }
}
