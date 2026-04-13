package com.parrotalk.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "user_lesson_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE user_lesson_results SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class UserLessonResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(nullable = false)
    private Double score;

    @Column(name = "hint_words", nullable = false)
    private Integer hintWords;

    @Column(name = "replay_count", nullable = false)
    private Integer replayCount;

    @Column(nullable = false)
    private Integer attempts;

    @Column(name = "is_passed", nullable = false)
    private Boolean isPassed;
}
