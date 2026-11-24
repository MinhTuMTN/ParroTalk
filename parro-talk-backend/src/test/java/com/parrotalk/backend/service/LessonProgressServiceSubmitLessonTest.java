package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.util.ReflectionTestUtils;

import com.parrotalk.backend.constant.DictationMode;
import com.parrotalk.backend.dto.SubmitLessonResponse;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserLessonAnswerDetail;
import com.parrotalk.backend.entity.UserLessonDraftSegment;
import com.parrotalk.backend.entity.UserLessonDraftSegment.UserLessonDraftSegmentId;
import com.parrotalk.backend.entity.UserLessonHistory;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.mapper.DraftSegmentMapper;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;
import com.parrotalk.backend.repository.UserLessonAnswerDetailRepository;
import com.parrotalk.backend.repository.UserLessonDraftSegmentRepository;
import com.parrotalk.backend.repository.UserLessonHistoryRepository;
import com.parrotalk.backend.repository.UserLessonProgressRepository;
import com.parrotalk.backend.service.dictation.DictationAnswerValidator;
import com.parrotalk.backend.service.dictation.DictationBlankGenerator;
import com.parrotalk.backend.util.SegmentBlanksCodec;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class LessonProgressServiceSubmitLessonTest {

    @Mock
    private UserLessonProgressRepository progressRepository;

    @Mock
    private UserLessonDraftSegmentRepository draftSegmentRepository;

    @Mock
    private UserLessonHistoryRepository historyRepository;

    @Mock
    private UserLessonAnswerDetailRepository answerDetailRepository;

    @Mock
    private TranscriptionSegmentRepository segmentRepository;

    @Mock
    private LessonService lessonService;

    @Mock
    private UserLearningActivityService learningActivityService;

    @Mock
    private LibraryCacheService libraryCacheService;

    @Mock
    private DraftSegmentMapper draftSegmentMapper;

    @Mock
    private DictationAnswerValidator answerValidator;

    @Mock
    private DictationBlankGenerator blankGenerator;

    @Mock
    private SegmentBlanksCodec segmentBlanksCodec;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private LessonProgressService lessonProgressService;

    private UUID userId;
    private UUID lessonId;
    private UUID seg1Id;
    private UUID seg2Id;
    private User user;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(lessonProgressService, "entityManager", entityManager);
        userId = UUID.randomUUID();
        lessonId = UUID.randomUUID();
        seg1Id = UUID.randomUUID();
        seg2Id = UUID.randomUUID();
        user = User.builder().id(userId).build();
        lesson = Lesson.builder().id(lessonId).totalSegments(2).build();
    }

    @Test
    void submitLesson_noDrafts_throws400() {
        when(draftSegmentRepository.findByUserIdAndLessonIdAndMode(userId, lessonId, DictationMode.FULL))
                .thenReturn(List.of());

        ParroTalkException ex = assertThrows(
                ParroTalkException.class,
                () -> lessonProgressService.submitLesson(user, lessonId, DictationMode.FULL));

        assertEquals(400, ex.getHttpStatusCode().value());
        assertEquals("No draft progress found for this lesson", ex.getMessage());
        verifyNoSubmissionWrites();
    }

    @Test
    void submitLesson_lessonNotFound_throws404() {
        UserLessonDraftSegment draft = draft(seg1Id, DictationMode.FULL, true, 10, "Hello");
        when(draftSegmentRepository.findByUserIdAndLessonIdAndMode(userId, lessonId, DictationMode.FULL))
                .thenReturn(List.of(draft));
        when(segmentRepository.findIdsByLessonId(lessonId)).thenReturn(List.of());
        doThrow(new ParroTalkException("Lesson not found", HttpStatusCode.valueOf(404)))
                .when(lessonService).verifyExistsById(lessonId);

        ParroTalkException ex = assertThrows(
                ParroTalkException.class,
                () -> lessonProgressService.submitLesson(user, lessonId, DictationMode.FULL));

        assertEquals(404, ex.getHttpStatusCode().value());
        assertEquals("Lesson not found", ex.getMessage());
        verifyNoSubmissionWrites();
    }

    @Test
    void submitLesson_lessonHasNoSegments_throws400() {
        UserLessonDraftSegment draft = draft(seg1Id, DictationMode.FULL, true, 10, "Hello");
        when(draftSegmentRepository.findByUserIdAndLessonIdAndMode(userId, lessonId, DictationMode.FULL))
                .thenReturn(List.of(draft));
        when(segmentRepository.findIdsByLessonId(lessonId)).thenReturn(List.of());
        doNothing().when(lessonService).verifyExistsById(lessonId);

        ParroTalkException ex = assertThrows(
                ParroTalkException.class,
                () -> lessonProgressService.submitLesson(user, lessonId, DictationMode.FULL));

        assertEquals(400, ex.getHttpStatusCode().value());
        assertEquals("Lesson has no segments to submit", ex.getMessage());
        verifyNoSubmissionWrites();
    }

    @Test
    void submitLesson_incompleteSegments_throws400() {
        UserLessonDraftSegment draft1 = draft(seg1Id, DictationMode.MEDIUM, true, 10, "Hello");
        when(draftSegmentRepository.findByUserIdAndLessonIdAndMode(userId, lessonId, DictationMode.MEDIUM))
                .thenReturn(List.of(draft1));
        when(segmentRepository.findIdsByLessonId(lessonId))
                .thenReturn(List.of(seg1Id, seg2Id));

        ParroTalkException ex = assertThrows(
                ParroTalkException.class,
                () -> lessonProgressService.submitLesson(user, lessonId, DictationMode.MEDIUM));

        assertEquals(400, ex.getHttpStatusCode().value());
        assertEquals("All segments must be completed before submitting the lesson", ex.getMessage());
        verify(lessonService, never()).existsById(any());
        verifyNoSubmissionWrites();
    }

    @Test
    void submitLesson_incorrectSegmentDraft_throws400() {
        UserLessonDraftSegment draft1 = draft(seg1Id, DictationMode.FULL, true, 10, "Hello");
        UserLessonDraftSegment draft2 = draft(seg2Id, DictationMode.FULL, false, 0, "wrong");
        when(draftSegmentRepository.findByUserIdAndLessonIdAndMode(userId, lessonId, DictationMode.FULL))
                .thenReturn(List.of(draft1, draft2));
        when(segmentRepository.findIdsByLessonId(lessonId))
                .thenReturn(List.of(seg1Id, seg2Id));

        ParroTalkException ex = assertThrows(
                ParroTalkException.class,
                () -> lessonProgressService.submitLesson(user, lessonId, DictationMode.FULL));

        assertEquals(400, ex.getHttpStatusCode().value());
        assertEquals("All segments must be completed before submitting the lesson", ex.getMessage());
        verify(lessonService, never()).existsById(any());
        verifyNoSubmissionWrites();
    }

    @Test
    void submitLesson_allSegmentsCompletedCorrectly_recordsHistoryDeletesDraftsAndReturnsPassed() {
        UserLessonDraftSegment draft1 = draft(seg1Id, DictationMode.MEDIUM, true, 10, "Hello");
        UserLessonDraftSegment draft2 = draft(seg2Id, DictationMode.MEDIUM, true, 8, "World");
        TranscriptionSegment seg1 = segment(seg1Id);
        TranscriptionSegment seg2 = segment(seg2Id);

        when(draftSegmentRepository.findByUserIdAndLessonIdAndMode(userId, lessonId, DictationMode.MEDIUM))
                .thenReturn(List.of(draft1, draft2));
        when(segmentRepository.findIdsByLessonId(lessonId)).thenReturn(List.of(seg1Id, seg2Id));
        when(entityManager.getReference(User.class, userId)).thenReturn(user);
        when(entityManager.getReference(Lesson.class, lessonId)).thenReturn(lesson);
        when(historyRepository.save(any(UserLessonHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(entityManager.getReference(TranscriptionSegment.class, seg1Id)).thenReturn(seg1);
        when(entityManager.getReference(TranscriptionSegment.class, seg2Id)).thenReturn(seg2);

        SubmitLessonResponse response = lessonProgressService.submitLesson(user, lessonId, DictationMode.MEDIUM);

        assertEquals(9.0, response.getScore());
        assertTrue(response.isPassed());

        ArgumentCaptor<UserLessonHistory> historyCaptor = ArgumentCaptor.forClass(UserLessonHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertEquals(9, historyCaptor.getValue().getOverallScore());
        assertEquals(DictationMode.MEDIUM, historyCaptor.getValue().getDictationMode());
        verify(lessonService, never()).findById(any());
        verify(lessonService, never()).existsById(any());
        verify(segmentRepository, never()).findByLessonId(any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserLessonAnswerDetail>> detailsCaptor = ArgumentCaptor.forClass(List.class);
        verify(answerDetailRepository).saveAll(detailsCaptor.capture());
        assertEquals(2, detailsCaptor.getValue().size());

        verify(draftSegmentRepository).deleteByUserIdAndLessonIdAndMode(userId, lessonId, DictationMode.MEDIUM);
        verify(learningActivityService).recordCompletedLesson(userId, 9);
    }

    @Test
    void submitLesson_staleDraftForDeletedSegment_ignoredInScoreAndDetailsButAllDraftsDeleted() {
        UUID deletedSegId = UUID.randomUUID();
        UserLessonDraftSegment draft1 = draft(seg1Id, DictationMode.MEDIUM, true, 10, "Hello");
        UserLessonDraftSegment draft2 = draft(seg2Id, DictationMode.MEDIUM, true, 8, "World");
        UserLessonDraftSegment staleDraft = draft(deletedSegId, DictationMode.MEDIUM, true, 0, "Old deleted");
        TranscriptionSegment seg1 = segment(seg1Id);
        TranscriptionSegment seg2 = segment(seg2Id);

        when(draftSegmentRepository.findByUserIdAndLessonIdAndMode(userId, lessonId, DictationMode.MEDIUM))
                .thenReturn(List.of(draft1, draft2, staleDraft));
        when(segmentRepository.findIdsByLessonId(lessonId)).thenReturn(List.of(seg1Id, seg2Id));
        when(entityManager.getReference(User.class, userId)).thenReturn(user);
        when(entityManager.getReference(Lesson.class, lessonId)).thenReturn(lesson);
        when(historyRepository.save(any(UserLessonHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(entityManager.getReference(TranscriptionSegment.class, seg1Id)).thenReturn(seg1);
        when(entityManager.getReference(TranscriptionSegment.class, seg2Id)).thenReturn(seg2);

        SubmitLessonResponse response = lessonProgressService.submitLesson(user, lessonId, DictationMode.MEDIUM);

        assertEquals(9.0, response.getScore());
        assertTrue(response.isPassed());

        ArgumentCaptor<UserLessonHistory> historyCaptor = ArgumentCaptor.forClass(UserLessonHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertEquals(9, historyCaptor.getValue().getOverallScore());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserLessonAnswerDetail>> detailsCaptor = ArgumentCaptor.forClass(List.class);
        verify(answerDetailRepository).saveAll(detailsCaptor.capture());
        assertEquals(2, detailsCaptor.getValue().size());
        assertEquals(List.of(seg1Id, seg2Id), detailsCaptor.getValue().stream()
                .map(detail -> detail.getSegment().getId())
                .toList());

        verify(entityManager, never()).getReference(TranscriptionSegment.class, deletedSegId);
        verify(draftSegmentRepository).deleteByUserIdAndLessonIdAndMode(userId, lessonId, DictationMode.MEDIUM);
        verify(learningActivityService).recordCompletedLesson(userId, 9);
        verify(libraryCacheService).evictUserProcessingSections(userId);
    }

    @Test
    void resetProgress_deletesDraftsAndProgressAndEvictsUserLibraryCache() {
        lessonProgressService.resetProgress(user, lessonId, DictationMode.MEDIUM);

        verify(draftSegmentRepository).deleteByUserIdAndLessonIdAndMode(userId, lessonId, DictationMode.MEDIUM);
        verify(progressRepository).deleteById(
                new com.parrotalk.backend.entity.UserLessonProgress.UserLessonProgressId(
                        userId, lessonId, DictationMode.MEDIUM));
        verify(libraryCacheService).evictUserProcessingSections(userId);
    }

    private void verifyNoSubmissionWrites() {
        verify(historyRepository, never()).save(any());
        verify(answerDetailRepository, never()).saveAll(anyList());
        verify(draftSegmentRepository, never()).deleteByUserIdAndLessonIdAndMode(any(), any(), any());
        verify(learningActivityService, never()).recordCompletedLesson(any(), anyInt());
        verify(libraryCacheService, never()).evictUserProcessingSections(any());
    }

    private UserLessonDraftSegment draft(
            UUID segmentId,
            DictationMode mode,
            boolean correct,
            int score,
            String userAnswer) {
        return UserLessonDraftSegment.builder()
                .id(new UserLessonDraftSegmentId(userId, lessonId, segmentId, mode))
                .isCorrect(correct)
                .score(score)
                .userAnswer(userAnswer)
                .build();
    }

    private TranscriptionSegment segment(UUID id) {
        return TranscriptionSegment.builder()
                .id(id)
                .lesson(lesson)
                .text("Sample text")
                .build();
    }
}
