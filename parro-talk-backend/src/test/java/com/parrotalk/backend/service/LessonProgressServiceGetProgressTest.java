package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatusCode;

import com.parrotalk.backend.constant.DictationMode;
import com.parrotalk.backend.dto.DraftSegmentResponse;
import com.parrotalk.backend.dto.LessonProgressResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserLessonDraftSegment;
import com.parrotalk.backend.entity.UserLessonDraftSegment.UserLessonDraftSegmentId;
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
import com.parrotalk.backend.service.dictation.DictationBlankGenerator;
import com.parrotalk.backend.util.SegmentBlanksCodec;

@ExtendWith(MockitoExtension.class)
class LessonProgressServiceGetProgressTest {

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

    @Spy
    private DraftSegmentMapper draftSegmentMapper = Mappers.getMapper(DraftSegmentMapper.class);

    @Mock
    private DictationAnswerValidator answerValidator;

    @Mock
    private DictationBlankGenerator blankGenerator;

    @Mock
    private SegmentBlanksCodec segmentBlanksCodec;

    @InjectMocks
    private LessonProgressService lessonProgressService;

    private UUID userId;
    private UUID lessonId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        lessonId = UUID.randomUUID();
        user = User.builder().id(userId).build();
    }

    @Test
    void getProgress_existingProgressWithDrafts_returnsMappedResponseFromEntityGraph() {
        UUID currentSegmentId = UUID.randomUUID();
        UUID seg1Id = UUID.randomUUID();
        UUID seg2Id = UUID.randomUUID();
        LocalDateTime progressUpdatedAt = LocalDateTime.of(2026, 11, 23, 10, 0);
        LocalDateTime draft1UpdatedAt = LocalDateTime.of(2026, 11, 23, 9, 55);
        LocalDateTime draft2UpdatedAt = LocalDateTime.of(2026, 11, 23, 10, 0);

        UserLessonDraftSegment draft1 = UserLessonDraftSegment.builder()
                .id(new UserLessonDraftSegmentId(userId, lessonId, seg1Id, DictationMode.EASY))
                .userAnswer("hello world")
                .score(90)
                .replayCount(1)
                .hintCount(2)
                .isCorrect(true)
                .build();
        draft1.setUpdatedAt(draft1UpdatedAt);

        UserLessonDraftSegment draft2 = UserLessonDraftSegment.builder()
                .id(new UserLessonDraftSegmentId(userId, lessonId, seg2Id, DictationMode.EASY))
                .userAnswer("draft text")
                .score(0)
                .replayCount(3)
                .hintCount(0)
                .isCorrect(false)
                .build();
        draft2.setUpdatedAt(draft2UpdatedAt);

        UserLessonProgressId progressId = new UserLessonProgressId(userId, lessonId, DictationMode.EASY);
        UserLessonProgress progress = UserLessonProgress.builder()
                .id(progressId)
                .currentSegmentId(currentSegmentId)
                .lastProgress(0.5)
                .draftSegments(List.of(draft1, draft2))
                .build();
        progress.setUpdatedAt(progressUpdatedAt);

        when(progressRepository.findById(progressId)).thenReturn(Optional.of(progress));

        LessonProgressResponse response = lessonProgressService.getProgress(user, lessonId, DictationMode.EASY);

        assertEquals(userId, response.getUserId());
        assertEquals(lessonId, response.getLessonId());
        assertEquals(DictationMode.EASY, response.getMode());
        assertEquals(currentSegmentId, response.getCurrentSegmentId());
        assertEquals(0.5, response.getLastProgress());
        assertEquals(progressUpdatedAt, response.getUpdatedAt());
        assertNotNull(response.getDraftSegments());
        assertEquals(2, response.getDraftSegments().size());

        DraftSegmentResponse first = response.getDraftSegments().get(0);
        assertEquals(seg1Id, first.getSegmentId());
        assertEquals("hello world", first.getUserAnswer());
        assertEquals(90, first.getScore());
        assertEquals(1, first.getReplayCount());
        assertEquals(2, first.getHintCount());
        assertTrue(first.isCorrect());
        assertEquals(draft1UpdatedAt, first.getUpdatedAt());

        DraftSegmentResponse second = response.getDraftSegments().get(1);
        assertEquals(seg2Id, second.getSegmentId());
        assertEquals("draft text", second.getUserAnswer());
        assertEquals(0, second.getScore());
        assertEquals(3, second.getReplayCount());
        assertEquals(0, second.getHintCount());
        assertFalse(second.isCorrect());
        assertEquals(draft2UpdatedAt, second.getUpdatedAt());

        verify(progressRepository).findById(progressId);
        verify(lessonService, never()).verifyExistsById(any());
        verify(lessonService, never()).findById(any());
        verify(progressRepository, never()).save(any());
    }

    @Test
    void getProgress_existingProgressWithoutDrafts_returnsEmptyDraftSegments() {
        UUID currentSegmentId = UUID.randomUUID();
        LocalDateTime progressUpdatedAt = LocalDateTime.of(2026, 11, 23, 11, 0);

        UserLessonProgressId progressId = new UserLessonProgressId(userId, lessonId, DictationMode.FULL);
        UserLessonProgress progress = UserLessonProgress.builder()
                .id(progressId)
                .currentSegmentId(currentSegmentId)
                .lastProgress(0.25)
                .draftSegments(List.of())
                .build();
        progress.setUpdatedAt(progressUpdatedAt);

        when(progressRepository.findById(progressId)).thenReturn(Optional.of(progress));

        LessonProgressResponse response = lessonProgressService.getProgress(user, lessonId, DictationMode.FULL);

        assertEquals(userId, response.getUserId());
        assertEquals(lessonId, response.getLessonId());
        assertEquals(DictationMode.FULL, response.getMode());
        assertEquals(currentSegmentId, response.getCurrentSegmentId());
        assertEquals(0.25, response.getLastProgress());
        assertEquals(progressUpdatedAt, response.getUpdatedAt());
        assertNotNull(response.getDraftSegments());
        assertTrue(response.getDraftSegments().isEmpty());

        verify(progressRepository).findById(progressId);
        verify(lessonService, never()).verifyExistsById(any());
        verify(progressRepository, never()).save(any());
    }

    @Test
    void getProgress_missingProgressForExistingLesson_returnsEmptyProgressWithoutWriting() {
        UserLessonProgressId progressId = new UserLessonProgressId(userId, lessonId, DictationMode.MEDIUM);
        when(progressRepository.findById(progressId)).thenReturn(Optional.empty());
        doNothing().when(lessonService).verifyExistsById(lessonId);

        LessonProgressResponse response = lessonProgressService.getProgress(user, lessonId, DictationMode.MEDIUM);

        assertEquals(userId, response.getUserId());
        assertEquals(lessonId, response.getLessonId());
        assertEquals(DictationMode.MEDIUM, response.getMode());
        assertNull(response.getCurrentSegmentId());
        assertEquals(0.0, response.getLastProgress());
        assertNull(response.getUpdatedAt());
        assertNotNull(response.getDraftSegments());
        assertTrue(response.getDraftSegments().isEmpty());

        verify(lessonService).verifyExistsById(lessonId);
        verify(lessonService, never()).findById(any());
        verify(progressRepository, never()).save(any());
    }

    @Test
    void getProgress_missingProgressForNonexistentLesson_throws404() {
        UserLessonProgressId progressId = new UserLessonProgressId(userId, lessonId, DictationMode.FULL);
        when(progressRepository.findById(progressId)).thenReturn(Optional.empty());
        doThrow(new ParroTalkException("Lesson not found", HttpStatusCode.valueOf(404)))
                .when(lessonService).verifyExistsById(lessonId);

        ParroTalkException ex = assertThrows(
                ParroTalkException.class,
                () -> lessonProgressService.getProgress(user, lessonId));

        assertEquals(404, ex.getHttpStatusCode().value());
        assertEquals("Lesson not found", ex.getMessage());

        verify(lessonService).verifyExistsById(lessonId);
        verify(lessonService, never()).findById(any());
        verify(progressRepository, never()).save(any());
    }
}
