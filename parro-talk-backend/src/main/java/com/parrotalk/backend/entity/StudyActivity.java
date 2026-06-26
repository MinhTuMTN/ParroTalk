package com.parrotalk.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "study_activity", indexes = {
        @Index(name = "idx_study_activity_user_date", columnList = "user_id, activity_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_study_activity_user_date", columnNames = { "user_id", "activity_date" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyActivity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "study_seconds", nullable = false)
    @Builder.Default
    private Integer studySeconds = 0;

    @Column(name = "segments_completed", nullable = false)
    @Builder.Default
    private Integer segmentsCompleted = 0;

    @Column(name = "lessons_completed", nullable = false)
    @Builder.Default
    private Integer lessonsCompleted = 0;

    @Column(name = "first_activity_at", nullable = false)
    private LocalDateTime firstActivityAt;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;
}
