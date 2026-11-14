package com.parrotalk.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tts_audio_cache")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsAudioCache {

    @Id
    @Column(name = "cache_key", length = 100, nullable = false)
    private String cacheKey;

    @Column(name = "cloudinary_url", length = 500, nullable = false)
    private String cloudinaryUrl;

    @Column(name = "accent", length = 10, nullable = false)
    private String accent;

    @Column(name = "voice", length = 80, nullable = false)
    private String voice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
