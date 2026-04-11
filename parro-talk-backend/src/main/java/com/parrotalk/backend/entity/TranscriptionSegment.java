package com.parrotalk.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID lessonId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    private double startTime;
    private double endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SegmentType type;
}
