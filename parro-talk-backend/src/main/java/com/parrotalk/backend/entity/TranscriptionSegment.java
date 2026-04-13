package com.parrotalk.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.parrotalk.backend.constant.Difficulty;

import java.util.UUID;

@Entity
@Table(name = "transcription_segments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE transcription_segments SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class TranscriptionSegment extends BaseEntity {

    /** Transcription Segment ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Lesson ID */
    @Column(nullable = false)
    private UUID lessonId;

    /** Transcription text */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    /** Start time in seconds */
    @Column(nullable = false)
    private double startTime;

    /** End time in seconds */
    @Column(nullable = false)
    private double endTime;

    /** Difificulty level */
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;
}
