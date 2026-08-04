package com.parrotalk.backend.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Predefined reason a user can pick when reporting lesson data.
 *
 * <p>
 * Every reason declares the {@link LessonReportType} it belongs to, so the
 * reason/type consistency check stays a single equality comparison instead of
 * a growing switch statement. Adding a new reason only requires one line here.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum LessonReportReason {

    /** Audio is noisy or distorted. */
    AUDIO_NOISY(LessonReportType.FILE),

    /** Audio track is silent or partially missing. */
    AUDIO_MISSING(LessonReportType.FILE),

    /** A part of the media is cut off. */
    AUDIO_TRUNCATED(LessonReportType.FILE),

    /** Audio does not line up with the transcript timings. */
    AUDIO_OUT_OF_SYNC(LessonReportType.FILE),

    /** Media cannot be played at all. */
    FILE_NOT_PLAYABLE(LessonReportType.FILE),

    /** The attached media is not the expected one. */
    WRONG_FILE(LessonReportType.FILE),

    /** Any other file problem, described in free text. */
    FILE_OTHER(LessonReportType.FILE),

    /** Translation conveys the wrong meaning. */
    TRANSLATION_WRONG_MEANING(LessonReportType.TRANSLATION),

    /** Part of the source sentence is not translated. */
    TRANSLATION_MISSING_CONTENT(LessonReportType.TRANSLATION),

    /** Translation adds content that is not in the source. */
    TRANSLATION_EXTRA_CONTENT(LessonReportType.TRANSLATION),

    /** Translation is grammatically wrong. */
    TRANSLATION_GRAMMAR(LessonReportType.TRANSLATION),

    /** Proper nouns are mistranslated. */
    TRANSLATION_PROPER_NOUN(LessonReportType.TRANSLATION),

    /** Any other translation problem, described in free text. */
    TRANSLATION_OTHER(LessonReportType.TRANSLATION);

    /** Report type this reason is valid for. */
    private final LessonReportType type;

    /**
     * Check whether this reason may be used with the given report type.
     *
     * @param reportType Report type sent by the client
     * @return {@code true} when the reason belongs to the report type
     */
    public boolean isValidFor(LessonReportType reportType) {
        return this.type == reportType;
    }
}
