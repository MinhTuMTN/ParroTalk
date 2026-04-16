package com.parrotalk.backend.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
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
@IdClass(UserLessonDraftSegment.UserLessonDraftSegmentId.class)
public class UserLessonDraftSegment extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "lesson_id")
    private UUID lessonId;

    @Id
    @Column(name = "segment_id")
    private UUID segmentId;

    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;

    @Column(name = "score")
    private int score;

    @Column(name = "replay_count")
    private int replayCount;

    @Column(name = "hint_count")
    private int hintCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserLessonDraftSegmentId implements Serializable {
        private UUID userId;
        private UUID lessonId;
        private UUID segmentId;
    }
}
