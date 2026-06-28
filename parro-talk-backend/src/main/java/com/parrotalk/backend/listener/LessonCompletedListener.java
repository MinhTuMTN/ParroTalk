package com.parrotalk.backend.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.parrotalk.backend.dto.LessonCompletedEvent;
import com.parrotalk.backend.service.LessonService;

import lombok.RequiredArgsConstructor;

/**
 * Listener for lesson completed events.
 * 
 * @author MinhTuMTN
 */
@Component
@RequiredArgsConstructor
public class LessonCompletedListener {

    /** Lesson service */
    private final LessonService lessonService;

    /**
     * Handle lesson completed event.
     *
     * @param event Lesson completed event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTranslationLessonAfterCompleted(LessonCompletedEvent event) {
        lessonService.requestAdminTranslationGeneration(event.lessonId());
    }
}
