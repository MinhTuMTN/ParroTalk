package com.parrotalk.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE user_progress SET is_deleted = true WHERE user_id = ?")
@SQLRestriction("is_deleted = false")
public class UserProgress extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "total_lessons_completed", nullable = false)
    @Builder.Default
    private Integer totalLessonsCompleted = 0;

    @Column(name = "total_score", nullable = false)
    @Builder.Default
    private Double totalScore = 0.0;

    @Column(name = "avg_score", nullable = false)
    @Builder.Default
    private Double avgScore = 0.0;

    @Column(name = "last_activity_date")
    private LocalDateTime lastActivityDate;
}
