package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.dto.UpdateUserSettingsRequest;
import com.parrotalk.backend.dto.UserSettingsDto;
import com.parrotalk.backend.entity.UserSettings;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.UserRepository;
import com.parrotalk.backend.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Service managing per-user settings across general, audio/TTS, dictation, writing/practice, and goal groups.
 */
@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private static final List<String> ALLOWED_UI_LANGUAGES = List.of("vi", "en");
    private static final List<String> ALLOWED_TRANSLATION_LANGUAGES = List.of("vi", "en", "ja", "ko", "zh");
    private static final List<String> ALLOWED_THEMES = List.of("light", "dark", "system");
    private static final List<String> ALLOWED_ACCENTS = List.of("US", "UK");
    private static final List<String> ALLOWED_REPLAY_KEYS = List.of("Escape", "Tab", "Control");
    private static final List<String> ALLOWED_WRITING_MODES = List.of("SUGGESTION", "GUIDANCE");
    private static final List<String> ALLOWED_DIFFICULTY_SCALES = List.of("CEFR", "IELTS");
    private static final List<String> ALLOWED_CEFR_LEVELS = List.of("A1", "A2", "B1", "B2", "C1", "C2");
    private static final List<String> ALLOWED_IELTS_LEVELS = List.of("4.0", "4.5", "5.0", "5.5", "6.0", "6.5", "7.0", "7.5", "8.0");
    private static final List<String> ALLOWED_FLASHCARD_DIRECTIONS = List.of("EN_VI", "VI_EN");

    private static final double MIN_PLAYBACK_SPEED = 0.5;
    private static final double MAX_PLAYBACK_SPEED = 1.5;
    private static final int MIN_VOCAB_SESSION_SIZE = 5;
    private static final int MAX_VOCAB_SESSION_SIZE = 50;
    private static final int MIN_DAILY_GOAL = 5;
    private static final int MAX_DAILY_GOAL = 100;

    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;

    /**
     * Returns persisted settings for the user or default settings if no row has been saved yet.
     */
    @Transactional(readOnly = true)
    public UserSettingsDto getSettings(UUID userId) {
        if (userId == null) {
            throw new ParroTalkException("Authentication is required.", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> UserSettings.createDefaults(userId));
        return toDto(settings);
    }

    /**
     * Upserts user settings with strict validation/normalization of allowlisted fields and numeric clamping.
     * Supports partial updates by leaving omitted (null) fields unchanged.
     */
    @Transactional
    public UserSettingsDto updateSettings(UUID userId, UpdateUserSettingsRequest request) {
        if (userId == null) {
            throw new ParroTalkException("Authentication is required.", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        if (request == null) {
            throw new ParroTalkException("Settings update request must not be null.", ErrorCode.VALIDATION_ERROR.name(), HttpStatus.BAD_REQUEST);
        }
        if (!userRepository.existsById(userId)) {
            throw new ParroTalkException("User not found.", "USER_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> UserSettings.createDefaults(userId));

        if (request.getUiLanguage() != null) {
            settings.setUiLanguage(normalizeAllowlistedValue(request.getUiLanguage(), ALLOWED_UI_LANGUAGES, "uiLanguage"));
        }
        if (request.getTranslationLanguage() != null) {
            settings.setTranslationLanguage(normalizeAllowlistedValue(
                    request.getTranslationLanguage(), ALLOWED_TRANSLATION_LANGUAGES, "translationLanguage"));
        }
        if (request.getTheme() != null) {
            settings.setTheme(normalizeAllowlistedValue(request.getTheme(), ALLOWED_THEMES, "theme"));
        }
        if (request.getPreferredAccent() != null) {
            settings.setPreferredAccent(normalizeAllowlistedValue(
                    request.getPreferredAccent(), ALLOWED_ACCENTS, "preferredAccent"));
        }
        if (request.getPreferredUsVoice() != null) {
            settings.setPreferredUsVoice(WritingTtsService.matchAllowedVoiceForAccent("US", request.getPreferredUsVoice())
                    .orElseThrow(() -> validationError(
                            "Invalid preferredUsVoice. Allowed values: " + String.join(", ", WritingTtsService.ALLOWED_US_VOICES))));
        }
        if (request.getPreferredUkVoice() != null) {
            settings.setPreferredUkVoice(WritingTtsService.matchAllowedVoiceForAccent("UK", request.getPreferredUkVoice())
                    .orElseThrow(() -> validationError(
                            "Invalid preferredUkVoice. Allowed values: " + String.join(", ", WritingTtsService.ALLOWED_UK_VOICES))));
        }
        if (request.getPlaybackSpeed() != null) {
            settings.setPlaybackSpeed(clampDouble(request.getPlaybackSpeed(), MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED));
        }
        if (request.getAutoPlayVocabAudio() != null) {
            settings.setAutoPlayVocabAudio(request.getAutoPlayVocabAudio());
        }
        if (request.getDictationAutoAdvance() != null) {
            settings.setDictationAutoAdvance(request.getDictationAutoAdvance());
        }
        if (request.getDictationShowTranslation() != null) {
            settings.setDictationShowTranslation(request.getDictationShowTranslation());
        }
        if (request.getDictationAutoReplay() != null) {
            settings.setDictationAutoReplay(request.getDictationAutoReplay());
        }
        if (request.getDictationReplayKey() != null) {
            settings.setDictationReplayKey(normalizeAllowlistedValue(
                    request.getDictationReplayKey(), ALLOWED_REPLAY_KEYS, "dictationReplayKey"));
        }
        if (request.getDefaultWritingMode() != null) {
            settings.setDefaultWritingMode(normalizeAllowlistedValue(
                    request.getDefaultWritingMode(), ALLOWED_WRITING_MODES, "defaultWritingMode"));
        }
        if (request.getDefaultDifficultyScale() != null) {
            settings.setDefaultDifficultyScale(normalizeAllowlistedValue(
                    request.getDefaultDifficultyScale(), ALLOWED_DIFFICULTY_SCALES, "defaultDifficultyScale"));
        }
        if (request.getDefaultTargetLevel() != null) {
            List<String> validLevels = "IELTS".equals(settings.getDefaultDifficultyScale())
                    ? ALLOWED_IELTS_LEVELS
                    : ALLOWED_CEFR_LEVELS;
            settings.setDefaultTargetLevel(normalizeAllowlistedValue(
                    request.getDefaultTargetLevel(), validLevels, "defaultTargetLevel"));
        } else if (request.getDefaultDifficultyScale() != null) {
            // Keep persisted scale and target level coherent when scale switches in a partial update
            List<String> validLevels = "IELTS".equals(settings.getDefaultDifficultyScale())
                    ? ALLOWED_IELTS_LEVELS
                    : ALLOWED_CEFR_LEVELS;
            if (!validLevels.contains(settings.getDefaultTargetLevel())) {
                settings.setDefaultTargetLevel("IELTS".equals(settings.getDefaultDifficultyScale()) ? "6.5" : "B2");
            }
        }
        if (request.getVocabSessionSize() != null) {
            settings.setVocabSessionSize(clampInt(request.getVocabSessionSize(), MIN_VOCAB_SESSION_SIZE, MAX_VOCAB_SESSION_SIZE));
        }
        if (request.getDefaultFlashcardDirection() != null) {
            settings.setDefaultFlashcardDirection(normalizeAllowlistedValue(
                    request.getDefaultFlashcardDirection(), ALLOWED_FLASHCARD_DIRECTIONS, "defaultFlashcardDirection"));
        }
        if (request.getDailyGoal() != null) {
            settings.setDailyGoal(clampInt(request.getDailyGoal(), MIN_DAILY_GOAL, MAX_DAILY_GOAL));
        }

        UserSettings saved = userSettingsRepository.save(settings);
        return toDto(saved);
    }

    private String normalizeAllowlistedValue(String rawValue, Collection<String> allowedCanonical, String fieldName) {
        if (rawValue == null || rawValue.isBlank()) {
            throw validationError("Invalid " + fieldName + ". Allowed values: " + String.join(", ", allowedCanonical));
        }
        String trimmed = rawValue.trim();
        for (String canonical : allowedCanonical) {
            if (canonical.equalsIgnoreCase(trimmed)) {
                return canonical;
            }
        }
        throw validationError("Invalid " + fieldName + ". Allowed values: " + String.join(", ", allowedCanonical));
    }

    private double clampDouble(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            throw validationError("playbackSpeed must be a finite number.");
        }
        return Math.max(min, Math.min(max, value));
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private ParroTalkException validationError(String message) {
        return new ParroTalkException(message, ErrorCode.VALIDATION_ERROR.name(), HttpStatus.BAD_REQUEST);
    }

    private UserSettingsDto toDto(UserSettings entity) {
        return UserSettingsDto.builder()
                .uiLanguage(entity.getUiLanguage())
                .translationLanguage(entity.getTranslationLanguage())
                .theme(entity.getTheme())
                .preferredAccent(entity.getPreferredAccent())
                .preferredUsVoice(entity.getPreferredUsVoice())
                .preferredUkVoice(entity.getPreferredUkVoice())
                .playbackSpeed(entity.getPlaybackSpeed())
                .autoPlayVocabAudio(entity.getAutoPlayVocabAudio())
                .dictationAutoAdvance(entity.getDictationAutoAdvance())
                .dictationShowTranslation(entity.getDictationShowTranslation())
                .dictationAutoReplay(entity.getDictationAutoReplay())
                .dictationReplayKey(entity.getDictationReplayKey())
                .defaultWritingMode(entity.getDefaultWritingMode())
                .defaultDifficultyScale(entity.getDefaultDifficultyScale())
                .defaultTargetLevel(entity.getDefaultTargetLevel())
                .vocabSessionSize(entity.getVocabSessionSize())
                .defaultFlashcardDirection(entity.getDefaultFlashcardDirection())
                .dailyGoal(entity.getDailyGoal())
                .build();
    }
}
