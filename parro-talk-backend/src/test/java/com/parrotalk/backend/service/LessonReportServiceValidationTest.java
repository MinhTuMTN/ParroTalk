package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.parrotalk.backend.constant.LessonReportReason;
import com.parrotalk.backend.constant.LessonReportType;
import com.parrotalk.backend.dto.report.CreateLessonReportRequest;
import com.parrotalk.backend.dto.report.LessonReportResponse;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.LessonReport;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.LessonReportRepository;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;

/**
 * Rules that Bean Validation cannot express and that guard the single-table
 * design of {@link LessonReport}.
 */
class LessonReportServiceValidationTest {

    private LessonReportRepository lessonReportRepository;
    private LessonRepository lessonRepository;
    private TranscriptionSegmentRepository transcriptionSegmentRepository;
    private LessonReportService lessonReportService;

    private Lesson lesson;
    private User reporter;

    @BeforeEach
    void setUp() {
        lessonReportRepository = mock(LessonReportRepository.class);
        lessonRepository = mock(LessonRepository.class);
        transcriptionSegmentRepository = mock(TranscriptionSegmentRepository.class);

        lessonReportService = new LessonReportService(
                lessonReportRepository,
                lessonRepository,
                transcriptionSegmentRepository,
                mock(ModerationService.class));

        lesson = new Lesson();
        lesson.setId(UUID.randomUUID());

        reporter = new User();
        reporter.setId(UUID.randomUUID());

        when(lessonRepository.existsById(lesson.getId())).thenReturn(true);
        when(lessonRepository.getReferenceById(lesson.getId())).thenReturn(lesson);
        when(lessonReportRepository.findActiveByReporterAndLessonAndReason(any(), any(), any(), anyList()))
                .thenReturn(List.of());
        when(lessonReportRepository.save(any(LessonReport.class)))
                .thenAnswer(invocation -> {
                    LessonReport saved = invocation.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    return saved;
                });
    }

    @Test
    void createRejectsReasonThatDoesNotBelongToReportType() {
        CreateLessonReportRequest request = new CreateLessonReportRequest(
                LessonReportType.FILE, LessonReportReason.TRANSLATION_GRAMMAR, null, null);

        ParroTalkException exception = assertThrows(ParroTalkException.class,
                () -> lessonReportService.create(reporter, lesson.getId(), request));

        assertEquals("REPORT_REASON_TYPE_MISMATCH", exception.getErrorCode());
    }

    @Test
    void createRejectsTranslationReportWithoutSegment() {
        CreateLessonReportRequest request = new CreateLessonReportRequest(
                LessonReportType.TRANSLATION, LessonReportReason.TRANSLATION_WRONG_MEANING, null, null);

        ParroTalkException exception = assertThrows(ParroTalkException.class,
                () -> lessonReportService.create(reporter, lesson.getId(), request));

        assertEquals("REPORT_SEGMENT_REQUIRED", exception.getErrorCode());
    }

    @Test
    void createRejectsFileReportCarryingSegment() {
        CreateLessonReportRequest request = new CreateLessonReportRequest(
                LessonReportType.FILE, LessonReportReason.AUDIO_NOISY, UUID.randomUUID(), null);

        ParroTalkException exception = assertThrows(ParroTalkException.class,
                () -> lessonReportService.create(reporter, lesson.getId(), request));

        assertEquals("REPORT_SEGMENT_NOT_ALLOWED", exception.getErrorCode());
    }

    @Test
    void createRejectsSegmentBelongingToAnotherLesson() {
        Lesson otherLesson = new Lesson();
        otherLesson.setId(UUID.randomUUID());

        TranscriptionSegment segment = new TranscriptionSegment();
        segment.setId(UUID.randomUUID());
        segment.setLesson(otherLesson);
        when(transcriptionSegmentRepository.findById(segment.getId())).thenReturn(Optional.of(segment));

        CreateLessonReportRequest request = new CreateLessonReportRequest(
                LessonReportType.TRANSLATION, LessonReportReason.TRANSLATION_WRONG_MEANING, segment.getId(), null);

        ParroTalkException exception = assertThrows(ParroTalkException.class,
                () -> lessonReportService.create(reporter, lesson.getId(), request));

        assertEquals("SEGMENT_NOT_IN_LESSON", exception.getErrorCode());
    }

    @Test
    void createAcceptsValidTranslationReport() {
        TranscriptionSegment segment = new TranscriptionSegment();
        segment.setId(UUID.randomUUID());
        segment.setLesson(lesson);
        when(transcriptionSegmentRepository.findById(segment.getId())).thenReturn(Optional.of(segment));

        CreateLessonReportRequest request = new CreateLessonReportRequest(
                LessonReportType.TRANSLATION, LessonReportReason.TRANSLATION_WRONG_MEANING, segment.getId(), "  ");

        LessonReportResponse response = lessonReportService.create(reporter, lesson.getId(), request);

        assertNotNull(response.id());
        assertEquals(segment.getId(), response.segmentId());
        assertNull(response.description(), "blank description should be normalized to null");
    }

    @Test
    void createReturnsExistingReportInsteadOfDuplicate() {
        LessonReport existing = LessonReport.builder()
                .id(UUID.randomUUID())
                .lesson(lesson)
                .reportType(LessonReportType.FILE)
                .reason(LessonReportReason.AUDIO_NOISY)
                .reporter(reporter)
                .build();
        when(lessonReportRepository.findActiveByReporterAndLessonAndReason(any(), any(), any(), anyList()))
                .thenReturn(List.of(existing));

        CreateLessonReportRequest request = new CreateLessonReportRequest(
                LessonReportType.FILE, LessonReportReason.AUDIO_NOISY, null, null);

        LessonReportResponse response = lessonReportService.create(reporter, lesson.getId(), request);

        assertEquals(existing.getId(), response.id());
    }

    @Test
    void everyReasonBelongsToExactlyOneReportType() {
        for (LessonReportReason reason : LessonReportReason.values()) {
            assertNotNull(reason.getType());
            assertEquals(
                    reason.getType() == LessonReportType.FILE,
                    reason.isValidFor(LessonReportType.FILE),
                    reason.name());
        }
    }
}
