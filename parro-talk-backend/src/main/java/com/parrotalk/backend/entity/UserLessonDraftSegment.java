package com.parrotalk.backend.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_lesson_draft_segment", indexes = {
        @Index(name = "idx_user_lesson_draft_user_lesson_segment", columnList = "user_id, lesson_id, segment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLessonDraftSegment extends BaseEntity {

    @EmbeddedId
    private UserLessonDraftSegmentId id;

    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;

    @Column(name = "score")
    private int score;

    @Column(name = "replay_count")
    private int replayCount;

    @Column(name = "hint_count")
    private int hintCount;

    @Column(name = "is_correct")
    private boolean isCorrect;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false),
            @JoinColumn(name = "lesson_id", referencedColumnName = "lesson_id", insertable = false, updatable = false)
    })
    private UserLessonProgress userLessonProgress;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class UserLessonDraftSegmentId implements Serializable {

        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "lesson_id")
        private UUID lessonId;

        @Column(name = "segment_id")
        private UUID segmentId;
    }

}
