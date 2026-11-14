"use client";

import React, { useCallback, useEffect, useRef } from "react";
import { Loader2, Volume2 } from "lucide-react";
import { useUserSettings } from "@/features/settings/hooks/useUserSettings";
import { preloadWritingAudio } from "../hooks/useWritingAudio";
import {
  resolveWritingAudioUrl,
  type WritingAccent,
} from "../services/writingService";

const ACCENT_CONFIG: ReadonlyArray<{
  accent: WritingAccent;
  label: string;
  titleVi: string;
  titleEn: string;
}> = [
  {
    accent: "US",
    label: "US",
    titleVi: "Phát âm tiếng Anh - Mỹ (US)",
    titleEn: "Pronounce in US English",
  },
  {
    accent: "UK",
    label: "UK",
    titleVi: "Phát âm tiếng Anh - Anh (UK)",
    titleEn: "Pronounce in UK English",
  },
];

/**
 * Normalizes an IPA phonetic string into `/.../` (or preserves `[...]`), returning null when empty.
 */
export function formatPhonetic(phonetic?: string): string | null {
  const trimmed = phonetic?.trim();
  if (!trimmed) return null;
  if (
    (trimmed.startsWith("/") && trimmed.endsWith("/") && trimmed.length > 2) ||
    (trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.length > 2)
  ) {
    return trimmed;
  }
  const inner = trimmed.replace(/^\/+|\/+$/g, "");
  return inner ? `/${inner}/` : null;
}

interface PhoneticBadgeProps {
  phonetic?: string;
  size?: "sm" | "md";
}

/**
 * Renders a consistent IPA pronunciation badge for lexical items.
 */
export function PhoneticBadge({ phonetic, size = "sm" }: PhoneticBadgeProps) {
  const formatted = formatPhonetic(phonetic);
  if (!formatted) return null;

  return (
    <span
      className={`inline-flex w-fit items-center rounded-md border border-brand-200/80 dark:border-emerald-800 bg-brand-50/70 dark:bg-emerald-950/60 font-mono font-medium tracking-wide text-brand-700 dark:text-emerald-300 ${
        size === "sm"
          ? "px-1.5 py-0.5 text-xs"
          : "px-2 py-0.5 text-xs sm:text-sm"
      }`}
    >
      {formatted}
    </span>
  );
}

interface AccentAudioButtonsProps {
  idPrefix: string;
  text: string;
  usAudioUrl?: string;
  ukAudioUrl?: string;
  playingAudioKey: string | null;
  loadingAudioKey?: string | null;
  onPlay: (
    key: string,
    text: string,
    accent: WritingAccent,
    audioUrl?: string
  ) => void;
  size?: "sm" | "md";
}

/**
 * Renders dual-accent [US] and [UK] pronunciation buttons backed by preloaded
 * Edge-TTS audio and Web Speech fallback.
 */
export function AccentAudioButtons({
  idPrefix,
  text,
  usAudioUrl,
  ukAudioUrl,
  playingAudioKey,
  loadingAudioKey = null,
  onPlay,
  size = "sm",
}: AccentAudioButtonsProps) {
  const { settings } = useUserSettings();
  const containerRef = useRef<HTMLDivElement | null>(null);
  const trimmedText = text.trim();
  const isEn = settings.uiLanguage === "en";

  const getVoiceForAccent = useCallback(
    (accent: WritingAccent) =>
      accent === "UK" ? settings.preferredUkVoice : settings.preferredUsVoice,
    [settings.preferredUkVoice, settings.preferredUsVoice]
  );

  const preloadAccent = useCallback(
    (accent: WritingAccent, audioUrl?: string) => {
      if (!trimmedText) return;
      const resolvedUrl = resolveWritingAudioUrl(
        audioUrl,
        trimmedText,
        accent,
        getVoiceForAccent(accent)
      );
      preloadWritingAudio(resolvedUrl);
    },
    [getVoiceForAccent, trimmedText]
  );

  // Light warm-up for visible short lexical/collocation cards using only the user's preferred accent.
  useEffect(() => {
    if (
      !trimmedText ||
      size !== "sm" ||
      trimmedText.split(/\s+/).length > 5 ||
      typeof IntersectionObserver === "undefined"
    ) {
      return;
    }

    const node = containerRef.current;
    if (!node) return;

    const preferredAccent: WritingAccent =
      settings.preferredAccent === "UK" ? "UK" : "US";
    const preferredUrl = preferredAccent === "UK" ? ukAudioUrl : usAudioUrl;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          preloadAccent(preferredAccent, preferredUrl);
          observer.disconnect();
        }
      },
      { threshold: 0.1 }
    );

    observer.observe(node);
    return () => observer.disconnect();
  }, [preloadAccent, settings.preferredAccent, size, trimmedText, ukAudioUrl, usAudioUrl]);

  if (!trimmedText) return null;

  const orderedAccents =
    settings.preferredAccent === "UK"
      ? [...ACCENT_CONFIG].reverse()
      : ACCENT_CONFIG;

  const sizeClasses =
    size === "sm"
      ? "rounded-lg px-2 py-0.5 text-[11px]"
      : "rounded-xl px-2.5 py-1 text-xs";
  const iconClasses = size === "sm" ? "h-3 w-3" : "h-3.5 w-3.5";

  return (
    <div ref={containerRef} className="inline-flex items-center gap-1.5 shrink-0">
      {orderedAccents.map(({ accent, label, titleVi, titleEn }) => {
        const key = `${idPrefix}-${accent.toLowerCase()}`;
        const isPlaying = playingAudioKey === key;
        const isLoading = loadingAudioKey === key;
        const audioUrl = accent === "US" ? usAudioUrl : ukAudioUrl;

        return (
          <button
            key={accent}
            type="button"
            onClick={() => onPlay(key, text, accent, audioUrl)}
            onMouseEnter={() => preloadAccent(accent, audioUrl)}
            onFocus={() => preloadAccent(accent, audioUrl)}
            onTouchStart={() => preloadAccent(accent, audioUrl)}
            aria-label={
              isEn
                ? `Pronounce ${accent} ${text}`
                : `Phát âm ${accent} ${text}`
            }
            aria-pressed={isPlaying}
            aria-busy={isLoading}
            title={isEn ? titleEn : titleVi}
            className={`inline-flex items-center gap-1 border font-bold transition-all cursor-pointer ${sizeClasses} ${
              isPlaying
                ? "bg-brand-100 dark:bg-emerald-900/70 border-brand-300 dark:border-emerald-500 text-brand-700 dark:text-emerald-200 ring-2 ring-brand-400 scale-105"
                : isLoading
                  ? "bg-brand-50 dark:bg-emerald-950/70 border-brand-300 dark:border-emerald-600 text-brand-700 dark:text-emerald-200"
                  : "bg-ink-50/80 dark:bg-slate-800 border-ink-200/80 dark:border-slate-700 text-ink-600 dark:text-slate-200 hover:bg-brand-50 dark:hover:bg-emerald-950/60 hover:border-brand-200 dark:hover:border-emerald-700 hover:text-brand-700 dark:hover:text-emerald-300"
            }`}
          >
            {isLoading ? (
              <Loader2
                className={`${iconClasses} animate-spin shrink-0`}
                aria-hidden="true"
              />
            ) : (
              <Volume2
                className={`${iconClasses} shrink-0 ${isPlaying ? "animate-pulse" : ""}`}
                aria-hidden="true"
              />
            )}
            <span>{label}</span>
          </button>
        );
      })}
    </div>
  );
}
