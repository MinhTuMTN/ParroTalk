package com.parrotalk.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/**
 * Persisted per-user settings across general, audio/TTS, dictation, writing/practice, and goal groups.
 * Intentionally stores only {@code userId} without a JPA relation to {@link User} so Redis serialization of
 * {@link User} remains isolated.
 */
@Entity
@Table(name = "user_settings", indexes = {
        @Index(name = "idx_user_settings_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE user_settings SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class UserSettings extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "ui_language", nullable = false, length = 16)
    @Builder.Default
    private String uiLanguage = "vi";

    @Column(name = "translation_language", nullable = false, length = 16)
    @Builder.Default
    private String translationLanguage = "vi";

    @Column(name = "theme", nullable = false, length = 16)
    @Builder.Default
    private String theme = "light";

    @Column(name = "preferred_accent", nullable = false, length = 16)
    @Builder.Default
    private String preferredAccent = "US";

    @Column(name = "preferred_us_voice", nullable = false, length = 64)
    @Builder.Default
    private String preferredUsVoice = "en-US-JennyNeural";

    @Column(name = "preferred_uk_voice", nullable = false, length = 64)
    @Builder.Default
    private String preferredUkVoice = "en-GB-SoniaNeural";

    @Column(name = "playback_speed", nullable = false)
    @Builder.Default
    private Double playbackSpeed = 1.0;

    @Column(name = "auto_play_vocab_audio", nullable = false)
    @Builder.Default
    private Boolean autoPlayVocabAudio = Boolean.FALSE;

    @Column(name = "dictation_auto_advance", nullable = false)
    @Builder.Default
    private Boolean dictationAutoAdvance = Boolean.FALSE;

    @Column(name = "dictation_show_translation", nullable = false)
    @Builder.Default
    private Boolean dictationShowTranslation = Boolean.TRUE;

    @Column(name = "dictation_auto_replay", nullable = false)
    @Builder.Default
    private Boolean dictationAutoReplay = Boolean.FALSE;

    @Column(name = "dictation_replay_key", nullable = false, length = 32)
    @Builder.Default
    private String dictationReplayKey = "Escape";

    @Column(name = "default_writing_mode", nullable = false, length = 32)
    @Builder.Default
    private String defaultWritingMode = "SUGGESTION";

    @Column(name = "default_difficulty_scale", nullable = false, length = 16)
    @Builder.Default
    private String defaultDifficultyScale = "CEFR";

    @Column(name = "default_target_level", nullable = false, length = 16)
    @Builder.Default
    private String defaultTargetLevel = "B2";

    @Column(name = "vocab_session_size", nullable = false)
    @Builder.Default
    private Integer vocabSessionSize = 10;

    @Column(name = "default_flashcard_direction", nullable = false, length = 16)
    @Builder.Default
    private String defaultFlashcardDirection = "EN_VI";

    @Column(name = "daily_goal", nullable = false)
    @Builder.Default
    private Integer dailyGoal = 20;

    /**
     * Builds a new {@link UserSettings} entity populated with default values for the given user.
     */
    public static UserSettings createDefaults(UUID userId) {
        return UserSettings.builder()
                .userId(userId)
                .build();
    }
}
