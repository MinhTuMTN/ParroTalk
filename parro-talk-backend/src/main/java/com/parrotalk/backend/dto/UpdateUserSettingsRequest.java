package com.parrotalk.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserSettingsRequest {
    private String uiLanguage;
    private String translationLanguage;
    private String theme;
    private String preferredAccent;
    private String preferredUsVoice;
    private String preferredUkVoice;
    private Double playbackSpeed;
    private Boolean autoPlayVocabAudio;
    private Boolean dictationAutoAdvance;
    private Boolean dictationShowTranslation;
    private Boolean dictationAutoReplay;
    private String dictationReplayKey;
    private String defaultWritingMode;
    private String defaultDifficultyScale;
    private String defaultTargetLevel;
    private Integer vocabSessionSize;
    private String defaultFlashcardDirection;
    private Integer dailyGoal;
}
