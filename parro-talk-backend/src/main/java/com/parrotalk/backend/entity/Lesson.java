package com.parrotalk.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String fileUrl;

    @Column(nullable = false, unique = true)
    private String fileHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LessonStatus status = LessonStatus.PENDING;

    @Builder.Default
    private int progress = 0;

    @Builder.Default
    private String currentStep = "Initializing...";

    // New Media fields
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MediaType mediaType = MediaType.AUDIO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SourceType sourceType = SourceType.CLOUDINARY;

    @Column(name = "youtube_url")
    private String youtubeUrl;

    @Column(name = "owner_id")
    private UUID ownerId;


}
