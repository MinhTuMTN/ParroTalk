package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.dto.UpdateUserSettingsRequest;
import com.parrotalk.backend.dto.UserSettingsDto;
import com.parrotalk.backend.entity.UserSettings;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.UserRepository;
import com.parrotalk.backend.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private UserRepository userRepository;

    private UserSettingsService userSettingsService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userSettingsService = new UserSettingsService(userSettingsRepository, userRepository);
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("getSettings returns default settings when no row exists in DB")
    void getSettings_ReturnsDefaultsWhenMissing() {
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());

        UserSettingsDto dto = userSettingsService.getSettings(userId);

        assertEquals("vi", dto.getUiLanguage());
        assertEquals("vi", dto.getTranslationLanguage());
        assertEquals("light", dto.getTheme());
        assertEquals("US", dto.getPreferredAccent());
        assertEquals("en-US-JennyNeural", dto.getPreferredUsVoice());
        assertEquals("en-GB-SoniaNeural", dto.getPreferredUkVoice());
        assertEquals(1.0, dto.getPlaybackSpeed());
        assertFalse(dto.getAutoPlayVocabAudio());
        assertFalse(dto.getDictationAutoAdvance());
        assertTrue(dto.getDictationShowTranslation());
        assertFalse(dto.getDictationAutoReplay());
        assertEquals("Escape", dto.getDictationReplayKey());
        assertEquals("SUGGESTION", dto.getDefaultWritingMode());
        assertEquals("CEFR", dto.getDefaultDifficultyScale());
        assertEquals("B2", dto.getDefaultTargetLevel());
        assertEquals(10, dto.getVocabSessionSize());
        assertEquals("EN_VI", dto.getDefaultFlashcardDirection());
        assertEquals(20, dto.getDailyGoal());
        verify(userSettingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSettings upserts partial updates, normalizes casing, and preserves omitted fields")
    void updateSettings_PartialUpdateAndNormalization() {
        when(userRepository.existsById(userId)).thenReturn(true);
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserSettingsRequest request = UpdateUserSettingsRequest.builder()
                .uiLanguage(" EN ")
                .translationLanguage("JA")
                .theme("Dark")
                .preferredAccent("uk")
                .preferredUsVoice("en-us-ariaNeural")
                .preferredUkVoice("en-gb-ryanNeural")
                .dictationReplayKey("control")
                .defaultWritingMode("guidance")
                .defaultDifficultyScale("ielts")
                .defaultTargetLevel("6.5")
                .defaultFlashcardDirection("vi_en")
                .autoPlayVocabAudio(true)
                .build();

        UserSettingsDto result = userSettingsService.updateSettings(userId, request);

        assertEquals("en", result.getUiLanguage());
        assertEquals("ja", result.getTranslationLanguage());
        assertEquals("dark", result.getTheme());
        assertEquals("UK", result.getPreferredAccent());
        assertEquals("en-US-AriaNeural", result.getPreferredUsVoice());
        assertEquals("en-GB-RyanNeural", result.getPreferredUkVoice());
        assertEquals("Control", result.getDictationReplayKey());
        assertEquals("GUIDANCE", result.getDefaultWritingMode());
        assertEquals("IELTS", result.getDefaultDifficultyScale());
        assertEquals("6.5", result.getDefaultTargetLevel());
        assertEquals("VI_EN", result.getDefaultFlashcardDirection());
        assertTrue(result.getAutoPlayVocabAudio());
        // Omitted fields keep defaults
        assertEquals(1.0, result.getPlaybackSpeed());
        assertEquals(10, result.getVocabSessionSize());
        assertEquals(20, result.getDailyGoal());
        assertTrue(result.getDictationShowTranslation());
    }

    @Test
    @DisplayName("updateSettings clamps numeric fields to valid ranges")
    void updateSettings_ClampsNumericBounds() {
        when(userRepository.existsById(userId)).thenReturn(true);
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSettingsDto low = userSettingsService.updateSettings(userId, UpdateUserSettingsRequest.builder()
                .playbackSpeed(0.1)
                .vocabSessionSize(1)
                .dailyGoal(0)
                .build());
        assertEquals(0.5, low.getPlaybackSpeed());
        assertEquals(5, low.getVocabSessionSize());
        assertEquals(5, low.getDailyGoal());

        UserSettingsDto high = userSettingsService.updateSettings(userId, UpdateUserSettingsRequest.builder()
                .playbackSpeed(2.8)
                .vocabSessionSize(200)
                .dailyGoal(500)
                .build());
        assertEquals(1.5, high.getPlaybackSpeed());
        assertEquals(50, high.getVocabSessionSize());
        assertEquals(100, high.getDailyGoal());
    }

    @Test
    @DisplayName("updateSettings adjusts defaultTargetLevel coherently if scale switches without explicit target level")
    void updateSettings_ScaleSwitchWithoutLevelAutoAligns() {
        UserSettings existing = UserSettings.createDefaults(userId);
        when(userRepository.existsById(userId)).thenReturn(true);
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSettingsDto updatedToIelts = userSettingsService.updateSettings(userId,
                UpdateUserSettingsRequest.builder().defaultDifficultyScale("IELTS").build());
        assertEquals("IELTS", updatedToIelts.getDefaultDifficultyScale());
        assertEquals("6.5", updatedToIelts.getDefaultTargetLevel());

        UserSettingsDto updatedToCefr = userSettingsService.updateSettings(userId,
                UpdateUserSettingsRequest.builder().defaultDifficultyScale("CEFR").build());
        assertEquals("CEFR", updatedToCefr.getDefaultDifficultyScale());
        assertEquals("B2", updatedToCefr.getDefaultTargetLevel());
    }

    @Test
    @DisplayName("updateSettings rejects invalid allowlist values and mismatched scale/target level with 400 VALIDATION_ERROR")
    void updateSettings_RejectsInvalidValues() {
        when(userRepository.existsById(userId)).thenReturn(true);
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertValidationError(UpdateUserSettingsRequest.builder().preferredUsVoice("en-GB-RyanNeural").build());
        assertValidationError(UpdateUserSettingsRequest.builder().preferredUkVoice("en-US-JennyNeural").build());
        assertValidationError(UpdateUserSettingsRequest.builder().preferredAccent("AU").build());
        assertValidationError(UpdateUserSettingsRequest.builder().uiLanguage("fr").build());
        assertValidationError(UpdateUserSettingsRequest.builder().translationLanguage("es").build());
        assertValidationError(UpdateUserSettingsRequest.builder().theme("solarized").build());
        assertValidationError(UpdateUserSettingsRequest.builder().dictationReplayKey("Space").build());
        assertValidationError(UpdateUserSettingsRequest.builder().defaultWritingMode("FREE").build());
        assertValidationError(UpdateUserSettingsRequest.builder().defaultDifficultyScale("TOEFL").build());
        assertValidationError(UpdateUserSettingsRequest.builder().defaultTargetLevel("6.5").build()); // current scale is CEFR
        assertValidationError(UpdateUserSettingsRequest.builder().defaultDifficultyScale("IELTS").defaultTargetLevel("9.0").build());
        assertValidationError(UpdateUserSettingsRequest.builder().defaultFlashcardDirection("BOTH").build());
        assertValidationError(UpdateUserSettingsRequest.builder().playbackSpeed(Double.NaN).build());

        verify(userSettingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSettings throws 404 when user does not exist")
    void updateSettings_MissingUserThrows404() {
        when(userRepository.existsById(userId)).thenReturn(false);

        ParroTalkException ex = assertThrows(ParroTalkException.class,
                () -> userSettingsService.updateSettings(userId, UpdateUserSettingsRequest.builder().theme("dark").build()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatusCode());
    }

    private void assertValidationError(UpdateUserSettingsRequest req) {
        ParroTalkException ex = assertThrows(ParroTalkException.class,
                () -> userSettingsService.updateSettings(userId, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatusCode());
        assertEquals(ErrorCode.VALIDATION_ERROR.name(), ex.getErrorCode());
    }
}
