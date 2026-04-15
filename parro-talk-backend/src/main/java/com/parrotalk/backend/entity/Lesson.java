package com.parrotalk.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.parrotalk.backend.constant.LessonStatus;
import com.parrotalk.backend.constant.MediaType;
import com.parrotalk.backend.constant.SourceType;

import java.util.UUID;

/**
 * Lesson entity.
 * 
 * @author MinhTuMTN
 */
@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE lessons SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Lesson extends BaseEntity {

    /** Lesson ID */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Audio/Video file URL */
    @Column(nullable = false)
    private String fileUrl;

    /** Audio/Video file hash */
    @Column(nullable = true, unique = true)
    private String fileHash;

    /** Lesson status */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LessonStatus status = LessonStatus.PENDING;

    /** Transcription progress */
    @Builder.Default
    private int progress = 0;

    /** Current step */
    @Builder.Default
    private String currentStep = "Initializing...";

    /** Media type */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MediaType mediaType = MediaType.AUDIO;

    /** Source type */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SourceType sourceType = SourceType.CLOUDINARY;

    /** Owner ID */
    @Column(name = "owner_id")
    private UUID ownerId;

    /** Display title */
    @Column
    private String title;

    /** Description */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Lesson duration */
    @Column
    private Integer duration;
}