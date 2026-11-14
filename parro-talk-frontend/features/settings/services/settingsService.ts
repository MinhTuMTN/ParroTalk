import axios from "axios";
import api from "@/lib/axios";
import {
  ChangePasswordPayload,
  DEFAULT_USER_SETTINGS,
  DefaultDifficultyScale,
  DefaultFlashcardDirection,
  DefaultWritingMode,
  DictationReplayKey,
  PreferredAccent,
  ThemePreference,
  TranslationLanguage,
  UiLanguage,
  UkVoiceOption,
  UpdateUserProfilePayload,
  UsVoiceOption,
  UserProfileUpdateResult,
  UserSettings,
} from "../types";

const UI_LANGUAGES = new Set<UiLanguage>(["vi", "en"]);
const TRANSLATION_LANGUAGES = new Set<TranslationLanguage>([
  "vi",
  "en",
  "ja",
  "ko",
  "zh",
]);
const THEMES = new Set<ThemePreference>(["light", "dark", "system"]);
const ACCENTS = new Set<PreferredAccent>(["US", "UK"]);
const US_VOICES = new Set<UsVoiceOption>([
  "en-US-JennyNeural",
  "en-US-GuyNeural",
  "en-US-AriaNeural",
]);
const UK_VOICES = new Set<UkVoiceOption>([
  "en-GB-SoniaNeural",
  "en-GB-RyanNeural",
  "en-GB-LibbyNeural",
]);
const REPLAY_KEYS = new Set<DictationReplayKey>(["Escape", "Tab", "Control"]);
const WRITING_MODES = new Set<DefaultWritingMode>(["SUGGESTION", "GUIDANCE"]);
const DIFFICULTY_SCALES = new Set<DefaultDifficultyScale>(["CEFR", "IELTS"]);
const CEFR_TARGET_LEVELS = new Set(["A1", "A2", "B1", "B2", "C1", "C2"]);
const IELTS_TARGET_LEVELS = new Set([
  "4.0",
  "4.5",
  "5.0",
  "5.5",
  "6.0",
  "6.5",
  "7.0",
  "7.5",
  "8.0",
]);
const FLASHCARD_DIRECTIONS = new Set<DefaultFlashcardDirection>([
  "EN_VI",
  "VI_EN",
]);

function unwrapApiResponse<T>(body: unknown): T {
  if (body && typeof body === "object") {
    const record = body as Record<string, unknown>;
    if ("result" in record && record.result !== undefined) {
      return record.result as T;
    }
    if ("data" in record && record.data !== undefined) {
      return record.data as T;
    }
  }
  return body as T;
}

export function normalizeUserSettings(
  raw?: Partial<UserSettings> | null
): UserSettings {
  if (!raw || typeof raw !== "object") {
    return { ...DEFAULT_USER_SETTINGS };
  }

  const playbackSpeed =
    typeof raw.playbackSpeed === "number" && Number.isFinite(raw.playbackSpeed)
      ? Math.min(1.5, Math.max(0.5, Number(raw.playbackSpeed)))
      : DEFAULT_USER_SETTINGS.playbackSpeed;

  const vocabSessionSize =
    typeof raw.vocabSessionSize === "number" &&
    Number.isFinite(raw.vocabSessionSize)
      ? Math.min(50, Math.max(5, Math.round(raw.vocabSessionSize)))
      : DEFAULT_USER_SETTINGS.vocabSessionSize;

  const dailyGoal =
    typeof raw.dailyGoal === "number" && Number.isFinite(raw.dailyGoal)
      ? Math.min(100, Math.max(5, Math.round(raw.dailyGoal)))
      : DEFAULT_USER_SETTINGS.dailyGoal;

  const scale =
    raw.defaultDifficultyScale &&
    DIFFICULTY_SCALES.has(raw.defaultDifficultyScale)
      ? raw.defaultDifficultyScale
      : DEFAULT_USER_SETTINGS.defaultDifficultyScale;

  const rawTarget =
    typeof raw.defaultTargetLevel === "string"
      ? raw.defaultTargetLevel.trim()
      : "";
  const normalizedCefr = rawTarget.toUpperCase();
  const defaultTargetLevel =
    scale === "IELTS"
      ? IELTS_TARGET_LEVELS.has(rawTarget)
        ? rawTarget
        : "6.5"
      : CEFR_TARGET_LEVELS.has(normalizedCefr)
        ? normalizedCefr
        : "B2";

  return {
    uiLanguage:
      raw.uiLanguage && UI_LANGUAGES.has(raw.uiLanguage)
        ? raw.uiLanguage
        : DEFAULT_USER_SETTINGS.uiLanguage,
    translationLanguage:
      raw.translationLanguage &&
      TRANSLATION_LANGUAGES.has(raw.translationLanguage)
        ? raw.translationLanguage
        : DEFAULT_USER_SETTINGS.translationLanguage,
    theme:
      raw.theme && THEMES.has(raw.theme)
        ? raw.theme
        : DEFAULT_USER_SETTINGS.theme,
    preferredAccent:
      raw.preferredAccent && ACCENTS.has(raw.preferredAccent)
        ? raw.preferredAccent
        : DEFAULT_USER_SETTINGS.preferredAccent,
    preferredUsVoice:
      raw.preferredUsVoice && US_VOICES.has(raw.preferredUsVoice)
        ? raw.preferredUsVoice
        : DEFAULT_USER_SETTINGS.preferredUsVoice,
    preferredUkVoice:
      raw.preferredUkVoice && UK_VOICES.has(raw.preferredUkVoice)
        ? raw.preferredUkVoice
        : DEFAULT_USER_SETTINGS.preferredUkVoice,
    playbackSpeed,
    autoPlayVocabAudio:
      typeof raw.autoPlayVocabAudio === "boolean"
        ? raw.autoPlayVocabAudio
        : DEFAULT_USER_SETTINGS.autoPlayVocabAudio,
    dictationAutoAdvance:
      typeof raw.dictationAutoAdvance === "boolean"
        ? raw.dictationAutoAdvance
        : DEFAULT_USER_SETTINGS.dictationAutoAdvance,
    dictationShowTranslation:
      typeof raw.dictationShowTranslation === "boolean"
        ? raw.dictationShowTranslation
        : DEFAULT_USER_SETTINGS.dictationShowTranslation,
    dictationAutoReplay:
      typeof raw.dictationAutoReplay === "boolean"
        ? raw.dictationAutoReplay
        : DEFAULT_USER_SETTINGS.dictationAutoReplay,
    dictationReplayKey:
      raw.dictationReplayKey && REPLAY_KEYS.has(raw.dictationReplayKey)
        ? raw.dictationReplayKey
        : DEFAULT_USER_SETTINGS.dictationReplayKey,
    defaultWritingMode:
      raw.defaultWritingMode && WRITING_MODES.has(raw.defaultWritingMode)
        ? raw.defaultWritingMode
        : DEFAULT_USER_SETTINGS.defaultWritingMode,
    defaultDifficultyScale: scale,
    defaultTargetLevel,
    vocabSessionSize,
    defaultFlashcardDirection:
      raw.defaultFlashcardDirection &&
      FLASHCARD_DIRECTIONS.has(raw.defaultFlashcardDirection)
        ? raw.defaultFlashcardDirection
        : DEFAULT_USER_SETTINGS.defaultFlashcardDirection,
    dailyGoal,
  };
}

async function requestWithMethodFallback<T>(
  methods: Array<"put" | "patch" | "post">,
  url: string,
  payload: unknown
): Promise<T> {
  let lastError: unknown;
  for (const method of methods) {
    try {
      const response = await api.request({ method, url, data: payload });
      return unwrapApiResponse<T>(response.data);
    } catch (error) {
      lastError = error;
      if (axios.isAxiosError(error) && error.response?.status === 405) {
        continue;
      }
      throw error;
    }
  }
  throw lastError;
}

export const settingsService = {
  async getMySettings(): Promise<UserSettings> {
    const response = await api.get("/users/me/settings");
    const data = unwrapApiResponse<Partial<UserSettings>>(response.data);
    return normalizeUserSettings(data);
  },

  async updateMySettings(
    payload: Partial<UserSettings>
  ): Promise<UserSettings> {
    const data = await requestWithMethodFallback<Partial<UserSettings>>(
      ["put", "patch"],
      "/users/me/settings",
      payload
    );
    return normalizeUserSettings(data);
  },

  async updateMyProfile(
    payload: UpdateUserProfilePayload
  ): Promise<UserProfileUpdateResult> {
    return requestWithMethodFallback<UserProfileUpdateResult>(
      ["put", "patch"],
      "/users/me/profile",
      payload
    );
  },

  async changeMyPassword(payload: ChangePasswordPayload): Promise<void> {
    const body: Record<string, string> = {
      newPassword: payload.newPassword,
    };
    if (payload.currentPassword) {
      body.currentPassword = payload.currentPassword;
    }
    if (payload.confirmPassword) {
      body.confirmPassword = payload.confirmPassword;
    }
    await requestWithMethodFallback<unknown>(
      ["post", "put", "patch"],
      "/users/me/password",
      body
    );
  },
};
