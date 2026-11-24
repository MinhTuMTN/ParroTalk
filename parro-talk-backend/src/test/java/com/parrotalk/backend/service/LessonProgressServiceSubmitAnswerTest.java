package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.parrotalk.backend.constant.DictationMode;
import com.parrotalk.backend.dto.AnswerRequest;
import com.parrotalk.backend.dto.DraftSegmentResponse;
import com.parrotalk.backend.dto.SegmentSubmissionProjection;
import com.parrotalk.backend.dto.dictation.BlankAnswerItem;
import com.parrotalk.backend.dto.dictation.BlankResultItem;
import com.parrotalk.backend.dto.dictation.BlankSlotDto;
import com.parrotalk.backend.dto.dictation.SegmentBlanksPayload;
import com.parrotalk.backend.entity.Lesson;
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
import com.parrotalk.backend.service.dictation.DictationAnswerValidator.SegmentValidationResult;
import com.parrotalk.backend.service.dictation.DictationBlankGenerator;
import com.parrotalk.backend.util.SegmentBlanksCodec;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class LessonProgressServiceSubmitAnswerTest {

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
    private UUID segmentId;
    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(lessonProgressService, "entityManager", entityManager);
        userId = UUID.randomUUID();
        lessonId = UUID.randomUUID();
        segmentId = UUID.randomUUID();
        user = User.builder().id(userId).build();
    }

    @Test
    void submitAnswer_firstTimeFullMode_createsProgressAndDraftWithoutEntityGraphQueries() {
        SegmentSubmissionProjection target = projection(segmentId, lessonId, lessonId, "Hello world", null, 4);
        when(segmentRepository.findSubmissionTarget(segmentId, lessonId)).thenReturn(Optional.of(target));
        when(progressRepository.findProgressAndDraft(userId, lessonId, segmentId, DictationMode.FULL))
                .thenReturn(List.of());
        when(entityManager.getReference(User.class, userId)).thenReturn(user);
        when(entityManager.getReference(Lesson.class, lessonId)).thenReturn(Lesson.builder().id(lessonId).build());
        when(progressRepository.saveAndFlush(any(UserLessonProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(draftSegmentRepository.save(any(UserLessonDraftSegment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(draftSegmentMapper.toDraftSegmentResponse(any(UserLessonDraftSegment.class)))
                .thenAnswer(invocation -> {
                    UserLessonDraftSegment draft = invocation.getArgument(0);
                    return DraftSegmentResponse.builder()
                            .segmentId(draft.getId().getSegmentId())
                            .userAnswer(draft.getUserAnswer())
                            .score(draft.getScore())
                            .correct(draft.isCorrect())
                            .build();
                });

        AnswerRequest request = AnswerRequest.builder()
                .segmentId(segmentId)
                .userAnswer("Hello world")
                .studySecondsDelta(10)
                .build();

        DraftSegmentResponse response = lessonProgressService.submitAnswer(user, lessonId, request);

        assertTrue(response.isCorrect());
        assertEquals(10, response.getScore());
        assertEquals("Hello world", response.getUserAnswer());

        ArgumentCaptor<UserLessonProgress> progressCaptor = ArgumentCaptor.forClass(UserLessonProgress.class);
        InOrder inOrder = inOrder(progressRepository, draftSegmentRepository);
        inOrder.verify(progressRepository).saveAndFlush(progressCaptor.capture());
        inOrder.verify(draftSegmentRepository).save(any(UserLessonDraftSegment.class));

        UserLessonProgress savedProgress = progressCaptor.getValue();
        assertEquals(segmentId, savedProgress.getCurrentSegmentId());
        assertEquals(1, savedProgress.getTotalSegmentsCompleted());
        assertEquals(0.25, savedProgress.getLastProgress());

        verify(learningActivityService).recordSegmentAttempt(userId, true, 10);
        verify(libraryCacheService).evictUserProcessingSections(userId);
        verify(lessonService, never()).findById(any());
        verify(lessonService, never()).existsById(any());
        verify(progressRepository, never()).findById(any());
        verify(progressRepository, never()).existsById(any());
        verify(segmentRepository, never()).findById(any());
        verify(segmentRepository, never()).countByLessonId(any());
        verify(draftSegmentRepository, never()).countCorrectByUserIdAndLessonIdAndMode(any(), any(), any());
    }

    @Test
    void submitAnswer_wrongMediumAnswerWithExistingProgress_skipsCountCorrectQuery() {
        String blanksJson = "{\"suitable\":true}";
        SegmentSubmissionProjection target = projection(
                segmentId,
                lessonId,
                lessonId,
                "My colleague reviewed the report",
                blanksJson,
                2);
        UserLessonProgress existingProgress = UserLessonProgress.builder()
                .id(new UserLessonProgressId(userId, lessonId, DictationMode.MEDIUM))
                .totalSegmentsCompleted(1)
                .lastProgress(0.5)
                .build();

        when(segmentRepository.findSubmissionTarget(segmentId, lessonId)).thenReturn(Optional.of(target));
        when(progressRepository.findProgressAndDraft(userId, lessonId, segmentId, DictationMode.MEDIUM))
                .thenReturn(List.<Object[]>of(new Object[] { existingProgress, null }));

        BlankSlotDto slot = BlankSlotDto.builder().wordIndex(1).charLength(9).build();
        SegmentBlanksPayload payload = SegmentBlanksPayload.builder()
                .suitable(true)
                .levels(Map.of(DictationMode.MEDIUM, List.of(slot)))
                .build();
        when(segmentBlanksCodec.deserialize(blanksJson)).thenReturn(payload);
        when(answerValidator.validateSegmentBlanks(any(), any(), any()))
                .thenReturn(SegmentValidationResult.builder()
                        .allCorrect(false)
                        .correctCount(0)
                        .totalBlanks(1)
                        .accuracy(0.0)
                        .results(List.of(BlankResultItem.builder().wordIndex(1).correct(false).build()))
                        .build());
        when(draftSegmentRepository.save(any(UserLessonDraftSegment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(draftSegmentMapper.toDraftSegmentResponse(any(UserLessonDraftSegment.class)))
                .thenAnswer(invocation -> {
                    UserLessonDraftSegment draft = invocation.getArgument(0);
                    return DraftSegmentResponse.builder()
                            .segmentId(draft.getId().getSegmentId())
                            .userAnswer(draft.getUserAnswer())
                            .score(draft.getScore())
                            .correct(draft.isCorrect())
                            .build();
                });

        AnswerRequest request = AnswerRequest.builder()
                .segmentId(segmentId)
                .mode(DictationMode.MEDIUM)
                .blankAnswers(List.of(BlankAnswerItem.builder().wordIndex(1).answer("wrong").build()))
                .studySecondsDelta(15)
                .build();

        DraftSegmentResponse response = lessonProgressService.submitAnswer(user, lessonId, request);

        assertFalse(response.isCorrect());
        assertEquals(0, response.getScore());
        assertEquals(0.0, response.getAccuracy());
        assertEquals(1, existingProgress.getTotalSegmentsCompleted());
        assertEquals(0.5, existingProgress.getLastProgress());
        assertEquals(segmentId, existingProgress.getCurrentSegmentId());

        verify(progressRepository, never()).saveAndFlush(any());
        verify(draftSegmentRepository, never()).countCorrectByUserIdAndLessonIdAndMode(any(), any(), any());
        verify(learningActivityService).recordSegmentAttempt(userId, false, 15);
    }

    @Test
    void submitAnswer_nullLessonTotalSegments_fallsBackToCountByLessonId() {
        SegmentSubmissionProjection target = projection(segmentId, lessonId, lessonId, "Hello world", null, null);
        UserLessonProgress existingProgress = UserLessonProgress.builder()
                .id(new UserLessonProgressId(userId, lessonId, DictationMode.FULL))
                .totalSegmentsCompleted(0)
                .build();

        when(segmentRepository.findSubmissionTarget(segmentId, lessonId)).thenReturn(Optional.of(target));
        when(progressRepository.findProgressAndDraft(userId, lessonId, segmentId, DictationMode.FULL))
                .thenReturn(List.<Object[]>of(new Object[] { existingProgress, null }));
        when(draftSegmentRepository.save(any(UserLessonDraftSegment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(segmentRepository.countByLessonId(lessonId)).thenReturn(4L);
        when(draftSegmentMapper.toDraftSegmentResponse(any(UserLessonDraftSegment.class)))
                .thenReturn(DraftSegmentResponse.builder().segmentId(segmentId).correct(true).score(10).build());

        AnswerRequest request = AnswerRequest.builder()
                .segmentId(segmentId)
                .userAnswer("Hello world")
                .build();

        lessonProgressService.submitAnswer(user, lessonId, request);

        assertEquals(0.25, existingProgress.getLastProgress());
        verify(segmentRepository).countByLessonId(lessonId);
        verify(draftSegmentRepository, never()).countCorrectByUserIdAndLessonIdAndMode(any(), any(), any());
        verify(segmentRepository, never()).findByLessonId(any());
        verify(lessonService, never()).findById(any());
    }

    @Test
    void submitAnswer_existingProgressWithNullCompletedCount_fallsBackToCountQuery() {
        SegmentSubmissionProjection target = projection(segmentId, lessonId, lessonId, "Hello world", null, 4);
        UserLessonProgress existingProgress = UserLessonProgress.builder()
                .id(new UserLessonProgressId(userId, lessonId, DictationMode.FULL))
                .totalSegmentsCompleted(null)
                .build();

        when(segmentRepository.findSubmissionTarget(segmentId, lessonId)).thenReturn(Optional.of(target));
        when(progressRepository.findProgressAndDraft(userId, lessonId, segmentId, DictationMode.FULL))
                .thenReturn(List.<Object[]>of(new Object[] { existingProgress, null }));
        when(draftSegmentRepository.save(any(UserLessonDraftSegment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(draftSegmentRepository.countCorrectByUserIdAndLessonIdAndMode(userId, lessonId, DictationMode.FULL))
                .thenReturn(2L);
        when(draftSegmentMapper.toDraftSegmentResponse(any(UserLessonDraftSegment.class)))
                .thenReturn(DraftSegmentResponse.builder().segmentId(segmentId).correct(true).score(10).build());

        AnswerRequest request = AnswerRequest.builder()
                .segmentId(segmentId)
                .userAnswer("Hello world")
                .build();

        lessonProgressService.submitAnswer(user, lessonId, request);

        assertEquals(2, existingProgress.getTotalSegmentsCompleted());
        assertEquals(0.5, existingProgress.getLastProgress());
        verify(draftSegmentRepository).countCorrectByUserIdAndLessonIdAndMode(userId, lessonId, DictationMode.FULL);
    }

    @Test
    void submitAnswer_existingProgressIncorrectDraftBecomesCorrect_incrementsCompletedCountInMemory() {
        SegmentSubmissionProjection target = projection(segmentId, lessonId, lessonId, "Hello world", null, 4);
        UserLessonProgress existingProgress = UserLessonProgress.builder()
                .id(new UserLessonProgressId(userId, lessonId, DictationMode.FULL))
                .totalSegmentsCompleted(1)
                .lastProgress(0.25)
                .build();
        UserLessonDraftSegment existingDraft = UserLessonDraftSegment.builder()
                .id(new UserLessonDraftSegmentId(userId, lessonId, segmentId, DictationMode.FULL))
                .isCorrect(false)
                .build();

        when(segmentRepository.findSubmissionTarget(segmentId, lessonId)).thenReturn(Optional.of(target));
        when(progressRepository.findProgressAndDraft(userId, lessonId, segmentId, DictationMode.FULL))
                .thenReturn(List.<Object[]>of(new Object[] { existingProgress, existingDraft }));
        when(draftSegmentRepository.save(any(UserLessonDraftSegment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(draftSegmentMapper.toDraftSegmentResponse(any(UserLessonDraftSegment.class)))
                .thenReturn(DraftSegmentResponse.builder().segmentId(segmentId).correct(true).score(10).build());

        AnswerRequest request = AnswerRequest.builder()
                .segmentId(segmentId)
                .userAnswer("Hello world")
                .build();

        lessonProgressService.submitAnswer(user, lessonId, request);

        assertEquals(2, existingProgress.getTotalSegmentsCompleted());
        assertEquals(0.5, existingProgress.getLastProgress());
        verify(draftSegmentRepository, never()).countCorrectByUserIdAndLessonIdAndMode(any(), any(), any());
    }

    @Test
    void submitAnswer_missingOrInvalidLessonSegment_throws400() {
        when(segmentRepository.findSubmissionTarget(segmentId, lessonId)).thenReturn(Optional.empty());

        AnswerRequest request = AnswerRequest.builder()
                .segmentId(segmentId)
                .userAnswer("Hello world")
                .build();

        ParroTalkException ex = assertThrows(
                ParroTalkException.class,
                () -> lessonProgressService.submitAnswer(user, lessonId, request));

        assertEquals(400, ex.getHttpStatusCode().value());
        assertEquals("Segment does not exist or does not belong to lesson " + lessonId, ex.getMessage());
    }

    @Test
    void submitAnswer_alreadyCorrectDraft_throws400WithoutWrites() {
        SegmentSubmissionProjection target = projection(segmentId, lessonId, lessonId, "Hello world", null, 2);
        UserLessonProgress existingProgress = UserLessonProgress.builder()
                .id(new UserLessonProgressId(userId, lessonId, DictationMode.FULL))
                .build();
        UserLessonDraftSegment existingDraft = UserLessonDraftSegment.builder()
                .id(new UserLessonDraftSegmentId(userId, lessonId, segmentId, DictationMode.FULL))
                .isCorrect(true)
                .build();

        when(segmentRepository.findSubmissionTarget(segmentId, lessonId)).thenReturn(Optional.of(target));
        when(progressRepository.findProgressAndDraft(userId, lessonId, segmentId, DictationMode.FULL))
                .thenReturn(List.<Object[]>of(new Object[] { existingProgress, existingDraft }));

        AnswerRequest request = AnswerRequest.builder()
                .segmentId(segmentId)
                .userAnswer("Hello world")
                .build();

        ParroTalkException ex = assertThrows(
                ParroTalkException.class,
                () -> lessonProgressService.submitAnswer(user, lessonId, request));

        assertEquals(400, ex.getHttpStatusCode().value());
        assertEquals("Cannot submit answer for correct segment", ex.getMessage());

        verify(progressRepository, never()).saveAndFlush(any());
        verify(draftSegmentRepository, never()).save(any());
        verify(learningActivityService, never()).recordSegmentAttempt(any(), any(Boolean.class), any(Integer.class));
        verify(libraryCacheService, never()).evictUserProcessingSections(any());
    }

    private static SegmentSubmissionProjection projection(
            UUID id,
            UUID rawLessonId,
            UUID activeLessonId,
            String text,
            String blanksJson,
            Integer lessonTotalSegments) {
        return new SegmentSubmissionProjection() {
            @Override
            public UUID getId() {
                return id;
            }

            @Override
            public UUID getRawLessonId() {
                return rawLessonId;
            }

            @Override
            public UUID getActiveLessonId() {
                return activeLessonId;
            }

            @Override
            public String getText() {
                return text;
            }

            @Override
            public String getBlanksJson() {
                return blanksJson;
            }

            @Override
            public Integer getLessonTotalSegments() {
                return lessonTotalSegments;
            }
        };
    }
}
