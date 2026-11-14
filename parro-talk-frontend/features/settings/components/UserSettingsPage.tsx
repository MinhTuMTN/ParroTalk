"use client";

import React, { useCallback, useEffect, useRef, useState } from "react";
import axios from "axios";
import {
  BookOpen,
  Check,
  Globe,
  Headphones,
  Keyboard,
  Menu,
  Monitor,
  Moon,
  RotateCcw,
  Settings as SettingsIcon,
  Shield,
  Square,
  Sun,
  User as UserIcon,
  Volume2,
} from "lucide-react";
import UserMenu from "@/components/common/UserMenu";
import Switch from "@/components/ui/Switch";
import { useAuth } from "@/features/auth/hooks/useAuth";
import {
  CEFR_LEVELS,
  IELTS_LEVELS,
} from "@/features/writing/components/WritingPromptComposer";
import {
  getWritingTtsUrl,
  WritingAccent,
} from "@/features/writing/services/writingService";
import { useUI } from "@/hooks/useUI";
import { playTTS, stopTTS } from "@/lib/tts";
import { useUserSettings } from "../hooks/useUserSettings";
import { settingsService } from "../services/settingsService";
import {
  DAILY_GOAL_OPTIONS,
  getSettingsCopy,
  PLAYBACK_SPEED_OPTIONS,
  REPLAY_KEY_OPTIONS,
  TRANSLATION_LANGUAGE_OPTIONS,
  UK_VOICE_OPTIONS,
  US_VOICE_OPTIONS,
  VOCAB_SESSION_SIZE_OPTIONS,
  VoiceOptionItem,
} from "../types";

type SettingsSectionTab =
  | "all"
  | "voice"
  | "dictation"
  | "learning"
  | "appearance"
  | "account";

const PREVIEW_SAMPLE_TEXT =
  "Hello! Welcome to ParroTalk. Let's practice English together.";

function extractApiErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const raw = error.response?.data as
      | string
      | {
          message?: string;
          error?: string;
          result?: Record<string, string> | string;
        }
      | undefined;

    if (typeof raw === "string" && raw.trim()) {
      return raw;
    }
    if (raw && typeof raw === "object") {
      if (typeof raw.message === "string" && raw.message.trim()) {
        return raw.message;
      }
      if (typeof raw.error === "string" && raw.error.trim()) {
        return raw.error;
      }
      if (raw.result && typeof raw.result === "object") {
        const firstFieldMsg = Object.values(raw.result).find(
          (val) => typeof val === "string" && val.trim().length > 0
        );
        if (firstFieldMsg) {
          return firstFieldMsg;
        }
      }
    }
  }
  return fallback;
}

interface SettingRowProps {
  title: string;
  description?: string;
  children: React.ReactNode;
}

function SettingRow({ title, description, children }: SettingRowProps) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 py-4 border-b border-slate-100 dark:border-slate-800 last:border-b-0">
      <div className="space-y-0.5 max-w-xl">
        <p className="text-sm font-bold text-slate-800 dark:text-slate-100">
          {title}
        </p>
        {description ? (
          <p className="text-xs text-slate-500 dark:text-slate-400">
            {description}
          </p>
        ) : null}
      </div>
      <div className="shrink-0">{children}</div>
    </div>
  );
}

interface VoiceSelectorGridProps<T extends string> {
  label: string;
  accent: WritingAccent;
  options: VoiceOptionItem<T>[];
  selectedValue: T;
  uiLanguage: "vi" | "en";
  previewingVoice: string | null;
  previewBtnLabel: string;
  stopPreviewBtnLabel: string;
  onSelect: (voice: T) => void;
  onPreview: (voice: T, accent: WritingAccent) => void;
}

function VoiceSelectorGrid<T extends string>({
  label,
  accent,
  options,
  selectedValue,
  uiLanguage,
  previewingVoice,
  previewBtnLabel,
  stopPreviewBtnLabel,
  onSelect,
  onPreview,
}: VoiceSelectorGridProps<T>) {
  return (
    <div className="py-4 border-b border-slate-100 dark:border-slate-800 last:border-b-0">
      <p className="text-sm font-bold text-slate-800 dark:text-slate-100 mb-3">
        {label}
      </p>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
        {options.map((item) => {
          const isSelected = selectedValue === item.value;
          const isPreviewing = previewingVoice === item.value;
          return (
            <div
              key={item.value}
              className={`rounded-2xl border p-3.5 transition-all flex flex-col justify-between gap-3 ${
                isSelected
                  ? "border-emerald-500 bg-emerald-50/70 dark:bg-emerald-950/30 shadow-xs"
                  : "border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 hover:border-emerald-300 dark:hover:border-emerald-700"
              }`}
            >
              <button
                type="button"
                onClick={() => onSelect(item.value)}
                className="text-left cursor-pointer space-y-1"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="text-sm font-bold text-slate-900 dark:text-slate-100">
                    {item.label.split(" ")[0]}
                  </span>
                  <span
                    className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                      isSelected
                        ? "bg-emerald-500 text-white"
                        : "bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300"
                    }`}
                  >
                    {item.gender[uiLanguage]}
                  </span>
                </div>
                <p className="text-[11px] font-mono text-slate-500 dark:text-slate-400 truncate">
                  {item.value}
                </p>
                <p className="text-xs text-slate-600 dark:text-slate-300">
                  {item.note[uiLanguage]}
                </p>
              </button>

              <button
                type="button"
                onClick={() => onPreview(item.value, accent)}
                className={`inline-flex items-center justify-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-bold transition-all cursor-pointer ${
                  isPreviewing
                    ? "bg-emerald-600 text-white"
                    : "bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:bg-emerald-100 hover:text-emerald-800 dark:hover:bg-emerald-900/40 dark:hover:text-emerald-300"
                }`}
              >
                {isPreviewing ? (
                  <>
                    <Square size={12} />
                    <span>{stopPreviewBtnLabel}</span>
                  </>
                ) : (
                  <>
                    <Volume2 size={13} />
                    <span>{previewBtnLabel}</span>
                  </>
                )}
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default function UserSettingsPage() {
  const { openMobileMenu } = useUI();
  const { user, updateUser } = useAuth();
  const { settings, isSaving, syncError, updateSettings, resetSettings } =
    useUserSettings();

  const copy = getSettingsCopy(settings.uiLanguage);

  const [activeTab, setActiveTab] = useState<SettingsSectionTab>("all");
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  // Voice preview playback state & ref
  const previewAudioRef = useRef<HTMLAudioElement | null>(null);
  const [previewingVoice, setPreviewingVoice] = useState<string | null>(null);

  // Profile form overrides (keeps defaults synced with user without effect setState)
  const [fullNameOverride, setFullNameOverride] = useState<string | null>(null);
  const [usernameOverride, setUsernameOverride] = useState<string | null>(null);
  const [avatarUrlOverride, setAvatarUrlOverride] = useState<string | null>(
    null
  );
  const [failedPreviewAvatar, setFailedPreviewAvatar] = useState<string | null>(
    null
  );
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileFeedback, setProfileFeedback] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);

  // Password form state
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [passwordFeedback, setPasswordFeedback] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);

  const fullNameValue = fullNameOverride ?? (user?.fullName || "");
  const usernameValue = usernameOverride ?? (user?.username || "");
  const avatarUrlValue = avatarUrlOverride ?? (user?.avatarUrl || "");
  const showAvatarPreview =
    Boolean(avatarUrlValue.trim()) && failedPreviewAvatar !== avatarUrlValue;

  const stopPreviewAudio = useCallback(() => {
    if (previewAudioRef.current) {
      previewAudioRef.current.pause();
      previewAudioRef.current.currentTime = 0;
      previewAudioRef.current = null;
    }
    stopTTS();
    setPreviewingVoice(null);
  }, []);

  const handleTabChange = useCallback(
    (nextTab: SettingsSectionTab) => {
      stopPreviewAudio();
      setActiveTab(nextTab);
    },
    [stopPreviewAudio]
  );

  useEffect(() => {
    return () => {
      if (previewAudioRef.current) {
        previewAudioRef.current.pause();
        previewAudioRef.current.currentTime = 0;
        previewAudioRef.current = null;
      }
      stopTTS();
    };
  }, []);

  const handleVoicePreview = useCallback(
    (voice: string, accent: WritingAccent) => {
      if (previewingVoice === voice) {
        stopPreviewAudio();
        return;
      }

      stopPreviewAudio();
      setPreviewingVoice(voice);

      const url = getWritingTtsUrl(PREVIEW_SAMPLE_TEXT, accent, voice);
      const fallbackLang = accent === "UK" ? "en-GB" : "en-US";
      const speed = settings.playbackSpeed || 1.0;

      if (!url) {
        playTTS(PREVIEW_SAMPLE_TEXT, {
          lang: fallbackLang,
          rate: speed,
          onEnd: () => setPreviewingVoice(null),
          onError: () => setPreviewingVoice(null),
        });
        return;
      }

      const audio = new Audio(url);
      audio.playbackRate = speed;
      previewAudioRef.current = audio;

      audio.onended = () => {
        if (previewAudioRef.current === audio) {
          previewAudioRef.current = null;
        }
        setPreviewingVoice(null);
      };

      audio.onerror = () => {
        if (previewAudioRef.current === audio) {
          previewAudioRef.current = null;
        }
        playTTS(PREVIEW_SAMPLE_TEXT, {
          lang: fallbackLang,
          rate: speed,
          onEnd: () => setPreviewingVoice(null),
          onError: () => setPreviewingVoice(null),
        });
      };

      audio.play().catch(() => {
        if (previewAudioRef.current === audio) {
          previewAudioRef.current = null;
        }
        playTTS(PREVIEW_SAMPLE_TEXT, {
          lang: fallbackLang,
          rate: speed,
          onEnd: () => setPreviewingVoice(null),
          onError: () => setPreviewingVoice(null),
        });
      });
    },
    [previewingVoice, settings.playbackSpeed, stopPreviewAudio]
  );

  const handleResetDefaults = async () => {
    stopPreviewAudio();
    await resetSettings();
    setStatusMessage(copy.resetDoneNotice);
    setTimeout(() => setStatusMessage(null), 2500);
  };

  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setProfileFeedback(null);

    const trimmedName = fullNameValue.trim();
    const trimmedUsername = usernameValue.trim();
    const trimmedAvatar = avatarUrlValue.trim();

    if (!trimmedName) {
      setProfileFeedback({
        type: "error",
        text: copy.account.fullNameRequired,
      });
      return;
    }

    setProfileSaving(true);
    try {
      const updated = await settingsService.updateMyProfile({
        fullName: trimmedName,
        username: trimmedUsername || undefined,
        avatarUrl: trimmedAvatar,
      });
      updateUser({
        fullName: updated?.fullName ?? trimmedName,
        username: updated?.username ?? (trimmedUsername || undefined),
        avatarUrl:
          (updated?.avatarUrl !== undefined
            ? updated.avatarUrl
            : trimmedAvatar) || null,
      });
      setFullNameOverride(null);
      setUsernameOverride(null);
      setAvatarUrlOverride(null);
      setProfileFeedback({
        type: "success",
        text: copy.account.profileUpdated,
      });
    } catch (error) {
      setProfileFeedback({
        type: "error",
        text: extractApiErrorMessage(error, copy.account.profileError),
      });
    } finally {
      setProfileSaving(false);
    }
  };

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordFeedback(null);

    const requiresCurrent = user?.hasPassword !== false;
    if (requiresCurrent && !currentPassword) {
      setPasswordFeedback({
        type: "error",
        text: copy.account.currentPasswordRequired,
      });
      return;
    }
    if (newPassword.length < 6) {
      setPasswordFeedback({
        type: "error",
        text: copy.account.passwordTooShort,
      });
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordFeedback({
        type: "error",
        text: copy.account.passwordMismatch,
      });
      return;
    }

    setPasswordSaving(true);
    try {
      await settingsService.changeMyPassword({
        currentPassword: requiresCurrent ? currentPassword : undefined,
        newPassword,
        confirmPassword,
      });
      updateUser({ hasPassword: true });
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setPasswordFeedback({
        type: "success",
        text: copy.account.passwordUpdated,
      });
    } catch (error) {
      setPasswordFeedback({
        type: "error",
        text: extractApiErrorMessage(error, copy.account.passwordError),
      });
    } finally {
      setPasswordSaving(false);
    }
  };

  const navItems: Array<{
    id: SettingsSectionTab;
    label: string;
    icon: React.ComponentType<{ size?: number; className?: string }>;
  }> = [
    {
      id: "all",
      label: settings.uiLanguage === "en" ? "All settings" : "Tất cả cài đặt",
      icon: SettingsIcon,
    },
    { id: "voice", label: copy.tabs.voice, icon: Headphones },
    { id: "dictation", label: copy.tabs.dictation, icon: Keyboard },
    { id: "learning", label: copy.tabs.learning, icon: BookOpen },
    { id: "appearance", label: copy.tabs.appearance, icon: Globe },
    { id: "account", label: copy.tabs.account, icon: Shield },
  ];

  const showSection = (id: Exclude<SettingsSectionTab, "all">) =>
    activeTab === "all" || activeTab === id;

  const targetLevelOptions =
    settings.defaultDifficultyScale === "IELTS" ? IELTS_LEVELS : CEFR_LEVELS;

  return (
    <div className="min-h-screen bg-[#F8FAF9] dark:bg-slate-950 text-slate-900 dark:text-slate-100 flex flex-col">
      {/* Top Sticky Header */}
      <header className="sticky top-0 z-40 border-b border-slate-200/80 dark:border-slate-800 bg-white/90 dark:bg-slate-900/90 backdrop-blur-md px-4 md:px-8 py-3 flex items-center justify-between gap-3">
        <div className="flex items-center gap-3 min-w-0">
          <button
            type="button"
            onClick={openMobileMenu}
            aria-label="Open navigation menu"
            className="lg:hidden p-2 hover:bg-emerald-50 dark:hover:bg-slate-800 rounded-xl text-slate-600 dark:text-slate-300 transition-colors cursor-pointer"
          >
            <Menu size={22} />
          </button>
          <div className="flex items-center gap-2.5 min-w-0">
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-emerald-100 dark:bg-emerald-900/50 text-emerald-700 dark:text-emerald-300">
              <SettingsIcon size={18} />
            </span>
            <div className="min-w-0">
              <h1 className="text-base sm:text-lg font-extrabold tracking-tight truncate">
                {copy.pageTitle}
              </h1>
              <p className="text-xs text-slate-500 dark:text-slate-400 hidden sm:block truncate">
                {copy.pageSubtitle}
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2 sm:gap-3 shrink-0">
          {isSaving ? (
            <span className="hidden md:inline-flex items-center rounded-full bg-amber-50 dark:bg-amber-950/50 border border-amber-200 dark:border-amber-800 px-3 py-1 text-xs font-semibold text-amber-700 dark:text-amber-300">
              {copy.saving}
            </span>
          ) : syncError ? (
            <span className="hidden md:inline-flex items-center rounded-full bg-amber-50 dark:bg-amber-950/50 border border-amber-200 dark:border-amber-800 px-3 py-1 text-xs font-semibold text-amber-700 dark:text-amber-300">
              {copy.saveErrorNotice}
            </span>
          ) : statusMessage ? (
            <span className="hidden md:inline-flex items-center gap-1 rounded-full bg-emerald-50 dark:bg-emerald-950/50 border border-emerald-200 dark:border-emerald-800 px-3 py-1 text-xs font-semibold text-emerald-700 dark:text-emerald-300">
              <Check size={12} />
              {statusMessage}
            </span>
          ) : null}

          <button
            type="button"
            onClick={() => void handleResetDefaults()}
            className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 px-3 py-2 text-xs font-bold text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <RotateCcw size={14} />
            <span className="hidden sm:inline">{copy.resetDefaults}</span>
          </button>

          <UserMenu />
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 sm:px-6">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          {/* Section Navigation */}
          <aside className="lg:col-span-3 lg:sticky lg:top-20">
            <nav
              aria-label="Settings sections"
              className="flex lg:flex-col gap-1.5 overflow-x-auto pb-2 lg:pb-0 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 p-2 shadow-2xs"
            >
              {navItems.map((item) => {
                const Icon = item.icon;
                const active = activeTab === item.id;
                return (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => handleTabChange(item.id)}
                    className={`flex items-center gap-2.5 whitespace-nowrap rounded-xl px-3.5 py-2.5 text-sm font-bold transition-all cursor-pointer ${
                      active
                        ? "bg-emerald-500 text-white shadow-sm"
                        : "text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800"
                    }`}
                  >
                    <Icon size={16} />
                    <span>{item.label}</span>
                  </button>
                );
              })}
            </nav>
          </aside>

          {/* Setting Groups */}
          <div className="lg:col-span-9 space-y-6">
            {/* Group 1: Voice & Audio */}
            {showSection("voice") && (
              <section className="rounded-3xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 p-5 sm:p-6 shadow-xs">
                <div className="flex items-start gap-3 pb-4 border-b border-slate-100 dark:border-slate-800">
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-emerald-100 dark:bg-emerald-900/40 text-emerald-700 dark:text-emerald-300">
                    <Headphones size={20} />
                  </span>
                  <div>
                    <h2 className="text-base font-extrabold">
                      {copy.voice.title}
                    </h2>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      {copy.voice.subtitle}
                    </p>
                  </div>
                </div>

                <SettingRow
                  title={copy.voice.preferredAccentLabel}
                  description={copy.voice.preferredAccentDesc}
                >
                  <div className="inline-flex rounded-xl bg-slate-100 dark:bg-slate-800 p-1">
                    {(["US", "UK"] as const).map((accent) => {
                      const active = settings.preferredAccent === accent;
                      return (
                        <button
                          key={accent}
                          type="button"
                          onClick={() =>
                            void updateSettings({ preferredAccent: accent })
                          }
                          className={`rounded-lg px-3.5 py-1.5 text-xs font-bold transition-all cursor-pointer ${
                            active
                              ? "bg-white dark:bg-slate-700 text-emerald-600 dark:text-emerald-300 shadow-2xs"
                              : "text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white"
                          }`}
                        >
                          {accent === "US"
                            ? copy.voice.usOption
                            : copy.voice.ukOption}
                        </button>
                      );
                    })}
                  </div>
                </SettingRow>

                <VoiceSelectorGrid
                  label={copy.voice.usVoiceLabel}
                  accent="US"
                  options={US_VOICE_OPTIONS}
                  selectedValue={settings.preferredUsVoice}
                  uiLanguage={settings.uiLanguage}
                  previewingVoice={previewingVoice}
                  previewBtnLabel={copy.voice.previewBtn}
                  stopPreviewBtnLabel={copy.voice.stopPreviewBtn}
                  onSelect={(voice) =>
                    void updateSettings({ preferredUsVoice: voice })
                  }
                  onPreview={handleVoicePreview}
                />

                <VoiceSelectorGrid
                  label={copy.voice.ukVoiceLabel}
                  accent="UK"
                  options={UK_VOICE_OPTIONS}
                  selectedValue={settings.preferredUkVoice}
                  uiLanguage={settings.uiLanguage}
                  previewingVoice={previewingVoice}
                  previewBtnLabel={copy.voice.previewBtn}
                  stopPreviewBtnLabel={copy.voice.stopPreviewBtn}
                  onSelect={(voice) =>
                    void updateSettings({ preferredUkVoice: voice })
                  }
                  onPreview={handleVoicePreview}
                />

                <SettingRow
                  title={copy.voice.playbackSpeedLabel}
                  description={copy.voice.playbackSpeedDesc}
                >
                  <div className="flex flex-wrap gap-1.5">
                    {PLAYBACK_SPEED_OPTIONS.map((rate) => {
                      const active = settings.playbackSpeed === rate;
                      return (
                        <button
                          key={rate}
                          type="button"
                          onClick={() =>
                            void updateSettings({ playbackSpeed: rate })
                          }
                          className={`rounded-xl px-3 py-1.5 text-xs font-bold border transition-all cursor-pointer ${
                            active
                              ? "border-emerald-500 bg-emerald-500 text-white"
                              : "border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:border-emerald-300"
                          }`}
                        >
                          {rate}x
                        </button>
                      );
                    })}
                  </div>
                </SettingRow>

                <SettingRow
                  title={copy.voice.autoPlayVocabLabel}
                  description={copy.voice.autoPlayVocabDesc}
                >
                  <Switch
                    checked={settings.autoPlayVocabAudio}
                    onChange={(checked) =>
                      void updateSettings({ autoPlayVocabAudio: checked })
                    }
                  />
                </SettingRow>
              </section>
            )}

            {/* Group 2: Dictation */}
            {showSection("dictation") && (
              <section className="rounded-3xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 p-5 sm:p-6 shadow-xs">
                <div className="flex items-start gap-3 pb-4 border-b border-slate-100 dark:border-slate-800">
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-sky-100 dark:bg-sky-900/40 text-sky-700 dark:text-sky-300">
                    <Keyboard size={20} />
                  </span>
                  <div>
                    <h2 className="text-base font-extrabold">
                      {copy.dictation.title}
                    </h2>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      {copy.dictation.subtitle}
                    </p>
                  </div>
                </div>

                <SettingRow
                  title={copy.dictation.autoAdvanceLabel}
                  description={copy.dictation.autoAdvanceDesc}
                >
                  <Switch
                    checked={settings.dictationAutoAdvance}
                    onChange={(checked) =>
                      void updateSettings({ dictationAutoAdvance: checked })
                    }
                  />
                </SettingRow>

                <SettingRow
                  title={copy.dictation.showTranslationLabel}
                  description={copy.dictation.showTranslationDesc}
                >
                  <Switch
                    checked={settings.dictationShowTranslation}
                    onChange={(checked) =>
                      void updateSettings({ dictationShowTranslation: checked })
                    }
                  />
                </SettingRow>

                <SettingRow
                  title={copy.dictation.autoReplayLabel}
                  description={copy.dictation.autoReplayDesc}
                >
                  <Switch
                    checked={settings.dictationAutoReplay}
                    onChange={(checked) =>
                      void updateSettings({ dictationAutoReplay: checked })
                    }
                  />
                </SettingRow>

                <SettingRow
                  title={copy.dictation.replayKeyLabel}
                  description={copy.dictation.replayKeyDesc}
                >
                  <div className="flex flex-wrap gap-1.5">
                    {REPLAY_KEY_OPTIONS.map((keyOpt) => {
                      const active =
                        settings.dictationReplayKey === keyOpt.value;
                      return (
                        <button
                          key={keyOpt.value}
                          type="button"
                          onClick={() =>
                            void updateSettings({
                              dictationReplayKey: keyOpt.value,
                            })
                          }
                          className={`rounded-xl px-3 py-1.5 text-xs font-bold border transition-all cursor-pointer ${
                            active
                              ? "border-emerald-500 bg-emerald-500 text-white"
                              : "border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:border-emerald-300"
                          }`}
                        >
                          {keyOpt.label}
                        </button>
                      );
                    })}
                  </div>
                </SettingRow>
              </section>
            )}

            {/* Group 3: Learning & AI Assistant */}
            {showSection("learning") && (
              <section className="rounded-3xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 p-5 sm:p-6 shadow-xs">
                <div className="flex items-start gap-3 pb-4 border-b border-slate-100 dark:border-slate-800">
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-violet-100 dark:bg-violet-900/40 text-violet-700 dark:text-violet-300">
                    <BookOpen size={20} />
                  </span>
                  <div>
                    <h2 className="text-base font-extrabold">
                      {copy.learning.title}
                    </h2>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      {copy.learning.subtitle}
                    </p>
                  </div>
                </div>

                <SettingRow title={copy.learning.defaultWritingModeLabel}>
                  <div className="inline-flex rounded-xl bg-slate-100 dark:bg-slate-800 p-1">
                    {(["SUGGESTION", "GUIDANCE"] as const).map((mode) => {
                      const active = settings.defaultWritingMode === mode;
                      return (
                        <button
                          key={mode}
                          type="button"
                          onClick={() =>
                            void updateSettings({ defaultWritingMode: mode })
                          }
                          className={`rounded-lg px-3.5 py-1.5 text-xs font-bold transition-all cursor-pointer ${
                            active
                              ? "bg-white dark:bg-slate-700 text-emerald-600 dark:text-emerald-300 shadow-2xs"
                              : "text-slate-600 dark:text-slate-300"
                          }`}
                        >
                          {mode === "SUGGESTION"
                            ? copy.learning.suggestionMode
                            : copy.learning.guidanceMode}
                        </button>
                      );
                    })}
                  </div>
                </SettingRow>

                <SettingRow title={copy.learning.defaultScaleLabel}>
                  <div className="inline-flex rounded-xl bg-slate-100 dark:bg-slate-800 p-1">
                    {(["CEFR", "IELTS"] as const).map((scale) => {
                      const active = settings.defaultDifficultyScale === scale;
                      return (
                        <button
                          key={scale}
                          type="button"
                          onClick={() =>
                            void updateSettings({
                              defaultDifficultyScale: scale,
                              defaultTargetLevel:
                                scale === "IELTS" ? "6.5" : "B2",
                            })
                          }
                          className={`rounded-lg px-3.5 py-1.5 text-xs font-bold transition-all cursor-pointer ${
                            active
                              ? "bg-white dark:bg-slate-700 text-emerald-600 dark:text-emerald-300 shadow-2xs"
                              : "text-slate-600 dark:text-slate-300"
                          }`}
                        >
                          {scale}
                        </button>
                      );
                    })}
                  </div>
                </SettingRow>

                <SettingRow title={copy.learning.defaultTargetLevelLabel}>
                  <div className="flex flex-wrap gap-1.5">
                    {targetLevelOptions.map((lvl) => {
                      const active = settings.defaultTargetLevel === lvl.value;
                      return (
                        <button
                          key={lvl.value}
                          type="button"
                          onClick={() =>
                            void updateSettings({
                              defaultTargetLevel: lvl.value,
                            })
                          }
                          className={`rounded-xl px-3 py-1.5 text-xs font-bold border transition-all cursor-pointer ${
                            active
                              ? "border-emerald-500 bg-emerald-500 text-white"
                              : "border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:border-emerald-300"
                          }`}
                        >
                          {lvl.label}
                        </button>
                      );
                    })}
                  </div>
                </SettingRow>

                <SettingRow title={copy.learning.vocabSessionSizeLabel}>
                  <div className="flex flex-wrap gap-1.5">
                    {VOCAB_SESSION_SIZE_OPTIONS.map((size) => {
                      const active = settings.vocabSessionSize === size;
                      return (
                        <button
                          key={size}
                          type="button"
                          onClick={() =>
                            void updateSettings({ vocabSessionSize: size })
                          }
                          className={`rounded-xl px-3 py-1.5 text-xs font-bold border transition-all cursor-pointer ${
                            active
                              ? "border-emerald-500 bg-emerald-500 text-white"
                              : "border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:border-emerald-300"
                          }`}
                        >
                          {size} {copy.learning.wordsUnit}
                        </button>
                      );
                    })}
                  </div>
                </SettingRow>

                <SettingRow title={copy.learning.flashcardDirectionLabel}>
                  <div className="inline-flex rounded-xl bg-slate-100 dark:bg-slate-800 p-1">
                    {(["EN_VI", "VI_EN"] as const).map((dir) => {
                      const active =
                        settings.defaultFlashcardDirection === dir;
                      return (
                        <button
                          key={dir}
                          type="button"
                          onClick={() =>
                            void updateSettings({
                              defaultFlashcardDirection: dir,
                            })
                          }
                          className={`rounded-lg px-3.5 py-1.5 text-xs font-bold transition-all cursor-pointer ${
                            active
                              ? "bg-white dark:bg-slate-700 text-emerald-600 dark:text-emerald-300 shadow-2xs"
                              : "text-slate-600 dark:text-slate-300"
                          }`}
                        >
                          {dir === "EN_VI"
                            ? copy.learning.enViLabel
                            : copy.learning.viEnLabel}
                        </button>
                      );
                    })}
                  </div>
                </SettingRow>

                <SettingRow title={copy.learning.dailyGoalLabel}>
                  <div className="flex flex-wrap gap-1.5">
                    {DAILY_GOAL_OPTIONS.map((minutes) => {
                      const active = settings.dailyGoal === minutes;
                      return (
                        <button
                          key={minutes}
                          type="button"
                          onClick={() =>
                            void updateSettings({ dailyGoal: minutes })
                          }
                          className={`rounded-xl px-3 py-1.5 text-xs font-bold border transition-all cursor-pointer ${
                            active
                              ? "border-emerald-500 bg-emerald-500 text-white"
                              : "border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:border-emerald-300"
                          }`}
                        >
                          {minutes} {copy.learning.minutesUnit}
                        </button>
                      );
                    })}
                  </div>
                </SettingRow>
              </section>
            )}

            {/* Group 4: Language & Appearance */}
            {showSection("appearance") && (
              <section className="rounded-3xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 p-5 sm:p-6 shadow-xs">
                <div className="flex items-start gap-3 pb-4 border-b border-slate-100 dark:border-slate-800">
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-amber-100 dark:bg-amber-900/40 text-amber-700 dark:text-amber-300">
                    <Globe size={20} />
                  </span>
                  <div>
                    <h2 className="text-base font-extrabold">
                      {copy.appearance.title}
                    </h2>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      {copy.appearance.subtitle}
                    </p>
                  </div>
                </div>

                <SettingRow
                  title={copy.appearance.uiLanguageLabel}
                  description={copy.appearance.uiLanguageDesc}
                >
                  <div className="inline-flex rounded-xl bg-slate-100 dark:bg-slate-800 p-1">
                    {(
                      [
                        { value: "vi", label: "Tiếng Việt" },
                        { value: "en", label: "English" },
                      ] as const
                    ).map((langOpt) => {
                      const active = settings.uiLanguage === langOpt.value;
                      return (
                        <button
                          key={langOpt.value}
                          type="button"
                          onClick={() =>
                            void updateSettings({ uiLanguage: langOpt.value })
                          }
                          className={`rounded-lg px-3.5 py-1.5 text-xs font-bold transition-all cursor-pointer ${
                            active
                              ? "bg-white dark:bg-slate-700 text-emerald-600 dark:text-emerald-300 shadow-2xs"
                              : "text-slate-600 dark:text-slate-300"
                          }`}
                        >
                          {langOpt.label}
                        </button>
                      );
                    })}
                  </div>
                </SettingRow>

                <SettingRow
                  title={copy.appearance.translationLanguageLabel}
                  description={copy.appearance.translationLanguageDesc}
                >
                  <select
                    aria-label={copy.appearance.translationLanguageLabel}
                    value={settings.translationLanguage}
                    onChange={(e) =>
                      void updateSettings({
                        translationLanguage: e.target
                          .value as typeof settings.translationLanguage,
                      })
                    }
                    className="rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3.5 py-2 text-xs font-bold text-slate-800 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  >
                    {TRANSLATION_LANGUAGE_OPTIONS.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label[settings.uiLanguage]}
                      </option>
                    ))}
                  </select>
                </SettingRow>

                <SettingRow title={copy.appearance.themeLabel}>
                  <div className="grid grid-cols-3 gap-2">
                    {(
                      [
                        {
                          value: "light",
                          label: copy.appearance.themeLight,
                          icon: Sun,
                        },
                        {
                          value: "dark",
                          label: copy.appearance.themeDark,
                          icon: Moon,
                        },
                        {
                          value: "system",
                          label: copy.appearance.themeSystem,
                          icon: Monitor,
                        },
                      ] as const
                    ).map((themeOpt) => {
                      const Icon = themeOpt.icon;
                      const active = settings.theme === themeOpt.value;
                      return (
                        <button
                          key={themeOpt.value}
                          type="button"
                          onClick={() =>
                            void updateSettings({ theme: themeOpt.value })
                          }
                          className={`inline-flex items-center justify-center gap-1.5 rounded-xl border px-3 py-2 text-xs font-bold transition-all cursor-pointer ${
                            active
                              ? "border-emerald-500 bg-emerald-500 text-white"
                              : "border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:border-emerald-300"
                          }`}
                        >
                          <Icon size={14} />
                          <span>{themeOpt.label}</span>
                        </button>
                      );
                    })}
                  </div>
                </SettingRow>
              </section>
            )}

            {/* Group 5: Profile & Security */}
            {showSection("account") && (
              <section className="rounded-3xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 p-5 sm:p-6 shadow-xs space-y-6">
                <div className="flex items-start gap-3 pb-4 border-b border-slate-100 dark:border-slate-800">
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-rose-100 dark:bg-rose-900/40 text-rose-700 dark:text-rose-300">
                    <Shield size={20} />
                  </span>
                  <div>
                    <h2 className="text-base font-extrabold">
                      {copy.account.title}
                    </h2>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      {copy.account.subtitle}
                    </p>
                  </div>
                </div>

                {/* Profile Form */}
                <form onSubmit={handleProfileSubmit} className="space-y-4">
                  <h3 className="text-sm font-extrabold flex items-center gap-2 text-slate-800 dark:text-slate-100">
                    <UserIcon size={16} className="text-emerald-600" />
                    <span>{copy.account.profileSection}</span>
                  </h3>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-bold text-slate-600 dark:text-slate-300 mb-1.5">
                        {copy.account.emailLabel}
                      </label>
                      <input
                        type="email"
                        value={user?.email || ""}
                        disabled
                        className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-100 dark:bg-slate-800/60 px-3.5 py-2.5 text-sm text-slate-500 dark:text-slate-400 cursor-not-allowed"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-600 dark:text-slate-300 mb-1.5">
                        {copy.account.fullNameLabel}
                      </label>
                      <input
                        type="text"
                        value={fullNameValue}
                        onChange={(e) => setFullNameOverride(e.target.value)}
                        className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3.5 py-2.5 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-600 dark:text-slate-300 mb-1.5">
                        {copy.account.usernameLabel}
                      </label>
                      <input
                        type="text"
                        value={usernameValue}
                        onChange={(e) => setUsernameOverride(e.target.value)}
                        className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3.5 py-2.5 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-600 dark:text-slate-300 mb-1.5">
                        {copy.account.avatarUrlLabel}
                      </label>
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 shrink-0 rounded-full bg-emerald-100 dark:bg-emerald-900/50 text-emerald-700 dark:text-emerald-300 flex items-center justify-center font-bold overflow-hidden border border-slate-200 dark:border-slate-700">
                          {showAvatarPreview ? (
                            // eslint-disable-next-line @next/next/no-img-element
                            <img
                              src={avatarUrlValue.trim()}
                              alt={fullNameValue || "Avatar preview"}
                              className="w-full h-full object-cover"
                              onError={() =>
                                setFailedPreviewAvatar(avatarUrlValue)
                              }
                            />
                          ) : (
                            fullNameValue.charAt(0).toUpperCase() || "U"
                          )}
                        </div>
                        <input
                          type="url"
                          value={avatarUrlValue}
                          onChange={(e) => setAvatarUrlOverride(e.target.value)}
                          placeholder="https://..."
                          className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3.5 py-2.5 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                        />
                      </div>
                    </div>
                  </div>

                  {profileFeedback && (
                    <p
                      className={`text-xs font-semibold rounded-xl px-3.5 py-2.5 ${
                        profileFeedback.type === "success"
                          ? "bg-emerald-50 dark:bg-emerald-950/50 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800"
                          : "bg-rose-50 dark:bg-rose-950/50 text-rose-700 dark:text-rose-300 border border-rose-200 dark:border-rose-800"
                      }`}
                    >
                      {profileFeedback.text}
                    </p>
                  )}

                  <div className="flex justify-end">
                    <button
                      type="submit"
                      disabled={profileSaving}
                      className="rounded-xl bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 px-5 py-2.5 text-xs font-extrabold text-white transition-colors cursor-pointer"
                    >
                      {profileSaving
                        ? copy.account.savingProfileBtn
                        : copy.account.saveProfileBtn}
                    </button>
                  </div>
                </form>

                <hr className="border-slate-100 dark:border-slate-800" />

                {/* Password Form */}
                <form onSubmit={handlePasswordSubmit} className="space-y-4">
                  <h3 className="text-sm font-extrabold flex items-center gap-2 text-slate-800 dark:text-slate-100">
                    <Shield size={16} className="text-emerald-600" />
                    <span>
                      {user?.hasPassword === false
                        ? copy.account.createPasswordSection
                        : copy.account.securitySection}
                    </span>
                  </h3>

                  {user?.hasPassword === false && (
                    <p className="text-xs rounded-xl bg-sky-50 dark:bg-sky-950/40 text-sky-700 dark:text-sky-300 border border-sky-200 dark:border-sky-800 px-3.5 py-2.5">
                      {copy.account.oauthNoPasswordHint}
                    </p>
                  )}

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    {user?.hasPassword !== false && (
                      <div>
                        <label className="block text-xs font-bold text-slate-600 dark:text-slate-300 mb-1.5">
                          {copy.account.currentPasswordLabel}
                        </label>
                        <input
                          type="password"
                          value={currentPassword}
                          onChange={(e) => setCurrentPassword(e.target.value)}
                          autoComplete="current-password"
                          className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3.5 py-2.5 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                        />
                      </div>
                    )}

                    <div>
                      <label className="block text-xs font-bold text-slate-600 dark:text-slate-300 mb-1.5">
                        {copy.account.newPasswordLabel}
                      </label>
                      <input
                        type="password"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        autoComplete="new-password"
                        className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3.5 py-2.5 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-600 dark:text-slate-300 mb-1.5">
                        {copy.account.confirmPasswordLabel}
                      </label>
                      <input
                        type="password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        autoComplete="new-password"
                        className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3.5 py-2.5 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                      />
                    </div>
                  </div>

                  {passwordFeedback && (
                    <p
                      className={`text-xs font-semibold rounded-xl px-3.5 py-2.5 ${
                        passwordFeedback.type === "success"
                          ? "bg-emerald-50 dark:bg-emerald-950/50 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800"
                          : "bg-rose-50 dark:bg-rose-950/50 text-rose-700 dark:text-rose-300 border border-rose-200 dark:border-rose-800"
                      }`}
                    >
                      {passwordFeedback.text}
                    </p>
                  )}

                  <div className="flex justify-end">
                    <button
                      type="submit"
                      disabled={passwordSaving}
                      className="rounded-xl bg-slate-900 dark:bg-emerald-600 hover:bg-slate-800 dark:hover:bg-emerald-700 disabled:opacity-50 px-5 py-2.5 text-xs font-extrabold text-white transition-colors cursor-pointer"
                    >
                      {passwordSaving
                        ? copy.account.changingPasswordBtn
                        : user?.hasPassword === false
                          ? copy.account.createPasswordBtn
                          : copy.account.changePasswordBtn}
                    </button>
                  </div>
                </form>
              </section>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}
