package com.parrotalk.backend.service;

import com.parrotalk.backend.dto.*;
import com.parrotalk.backend.entity.*;
import com.parrotalk.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonProgressService {

    private final UserLessonProgressRepository progressRepository;
    private final UserLessonDraftSegmentRepository draftSegmentRepository;
    private final UserLessonHistoryRepository historyRepository;
    private final UserLessonAnswerDetailRepository answerDetailRepository;
    private final TranscriptionSegmentRepository segmentRepository;
    private final ScoringService scoringService;
    private final LessonRepository lessonRepository;

    public LessonProgressResponse getProgress(User user, UUID lessonId) {

        UserLessonProgress.UserLessonProgressId key = new UserLessonProgress.UserLessonProgressId(user.getId(),
                lessonId);
        UserLessonProgress progress = progressRepository.findById(key)
                .orElse(UserLessonProgress.builder()
                        .id(key)
                        .lastProgress(0)
                        .currentSegmentId(null)
                        .build());

        List<UserLessonDraftSegment> drafts = draftSegmentRepository.findByUserIdAndLessonId(user.getId(),
                lessonId);

        return LessonProgressResponse.builder()
                .userId(user.getId())
                .lessonId(lessonId)
                .currentSegmentId(progress.getCurrentSegmentId())
                .lastProgress(progress.getLastProgress())
                .updatedAt(progress.getUpdatedAt())
                .draftSegments(drafts.stream().map(this::mapToDraftResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public DraftSegmentResponse submitAnswer(User user, UUID lessonId, AnswerRequest request) {
        TranscriptionSegment segment = segmentRepository.findById(request.getSegmentId())
                .orElseThrow(() -> new RuntimeException("Segment not found"));

        UserLessonDraftSegment draft = getOrCreateDraft(user.getId(), lessonId, request.getSegmentId());

        int score = scoringService.calculateScore(request.getUserAnswer(), segment.getText());

        draft.setUserAnswer(request.getUserAnswer());
        draft.setScore(score);

        draftSegmentRepository.save(draft);
        updateOverallProgress(user, lessonId, request.getSegmentId());

        return mapToDraftResponse(draft);
    }

    @Transactional
    public void incrementReplay(UUID userId, UUID lessonId, UUID segmentId) {
        UserLessonDraftSegment draft = getOrCreateDraft(userId, lessonId, segmentId);
        draft.setReplayCount(draft.getReplayCount() + 1);
        draftSegmentRepository.save(draft);
    }

    @Transactional
    public void incrementHint(UUID userId, UUID lessonId, UUID segmentId) {
        UserLessonDraftSegment draft = getOrCreateDraft(userId, lessonId, segmentId);
        draft.setHintCount(draft.getHintCount() + 1);
        draftSegmentRepository.save(draft);
    }

    @Transactional
    public SubmitLessonResponse submitLesson(User user, UUID lessonId) {
        List<UserLessonDraftSegment> drafts = draftSegmentRepository.findByUserIdAndLessonId(user.getId(),
                lessonId);
        if (drafts.isEmpty()) {
            throw new RuntimeException("No draft progress found for this lesson");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        int totalScore = drafts.stream().mapToInt(UserLessonDraftSegment::getScore).sum();
        int averageScore = totalScore / drafts.size();

        // 1. Create History
        UserLessonHistory history = UserLessonHistory.builder()
                .user(user)
                .lesson(lesson)
                .overallScore(averageScore)
                .totalTimeSpent(0) // Logic for time tracking can be added
                .build();
        history = historyRepository.save(history);

        // 2. Create Details
        for (UserLessonDraftSegment draft : drafts) {
            TranscriptionSegment segment = segmentRepository.findById(draft.getSegmentId())
                    .orElseThrow(() -> new RuntimeException("Segment not found"));

            UserLessonAnswerDetail detail = UserLessonAnswerDetail.builder()
                    .segment(segment)
                    .userAnswer(draft.getUserAnswer())
                    .score(draft.getScore())
                    .build();
            answerDetailRepository.save(detail);
        }

        // 3. Cleanup Drafts
        draftSegmentRepository.deleteByUserIdAndLessonId(user.getId(), lessonId);
        progressRepository.deleteById(new UserLessonProgress.UserLessonProgressId(user.getId(), lessonId));

        return SubmitLessonResponse.builder()
                .score((double) averageScore)
                .passed(averageScore >= 80)
                .build();
    }

    private UserLessonDraftSegment getOrCreateDraft(UUID userId, UUID lessonId, UUID segmentId) {
        return draftSegmentRepository
                .findById(new UserLessonDraftSegment.UserLessonDraftSegmentId(userId, lessonId,
                        segmentId))
                .orElse(UserLessonDraftSegment.builder()
                        .userId(userId)
                        .lessonId(lessonId)
                        .segmentId(segmentId)
                        .build());
    }

    private void updateOverallProgress(User user, UUID lessonId, UUID currentSegmentId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        UserLessonProgress progress = progressRepository
                .findById(new UserLessonProgress.UserLessonProgressId(user.getId(), lessonId))
                .orElse(UserLessonProgress.builder()
                        .user(user)
                        .lesson(lesson)
                        .build());

        progress.setCurrentSegmentId(currentSegmentId);
        progressRepository.save(progress);
    }

    private DraftSegmentResponse mapToDraftResponse(UserLessonDraftSegment draft) {
        return DraftSegmentResponse.builder()
                .segmentId(draft.getSegmentId())
                .userAnswer(draft.getUserAnswer())
                .score(draft.getScore())
                .replayCount(draft.getReplayCount())
                .hintCount(draft.getHintCount())
                .updatedAt(draft.getUpdatedAt())
                .build();
    }
}
