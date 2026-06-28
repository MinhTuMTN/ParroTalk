package com.parrotalk.backend.dto;

import java.util.UUID;

import com.parrotalk.backend.constant.TranslationLanguage;

/**
 * Translation message for RabbitMQ.
 *
 * @author MinhTuMTN
 * @param lessonId       Lesson ID
 * @param targetLanguage Target language
 */
public record LessonCompletedEvent(UUID lessonId, TranslationLanguage targetLanguage) {
}
