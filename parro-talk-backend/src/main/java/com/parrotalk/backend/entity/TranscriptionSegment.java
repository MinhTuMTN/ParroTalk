package com.parrotalk.backend.entity;

import java.util.UUID;

import org.hibernate.annotations.SQLRestriction;

import com.parrotalk.backend.constant.Difficulty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transcription Segment Entity
 * 
 * @author MinhTuMTN
 */
@Entity
@Table(name = "transcription_segments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("is_deleted = false")
public class TranscriptionSegment extends BaseEntity {

    /** Transcription Segment ID */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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
