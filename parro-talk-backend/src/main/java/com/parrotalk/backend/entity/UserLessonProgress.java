package com.parrotalk.backend.entity;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User Lesson Progress Entity.
 * Store recent user's progress in a lesson.
 * 
 * @author MinhTuMTN
 */
@Entity
@Table(name = "user_lesson_progress", indexes = {
        @Index(name = "idx_user_lesson_progress_user_lesson", columnList = "user_id, lesson_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLessonProgress extends BaseEntity {

    @EmbeddedId
    private UserLessonProgressId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("lessonId")
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Column(name = "current_segment_id")
    private UUID currentSegmentId;

    @Column(name = "last_progress")
    private double lastProgress;

    @OneToMany(mappedBy = "userLessonProgress")
    private List<UserLessonDraftSegment> draftSegments;

    @Column(name = "total_segments_completed")
    @Builder.Default
    private Integer totalSegmentsCompleted = 0;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class UserLessonProgressId implements Serializable {
        private UUID userId;
        private UUID lessonId;
    }
}
