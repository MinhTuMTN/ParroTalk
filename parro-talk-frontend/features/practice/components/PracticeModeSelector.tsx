"use client";

import type { DictationMode } from "@/features/lesson/services/lessonService";
import { Check, Sparkles } from "lucide-react";

export interface PracticeModeOption {
  value: DictationMode;
  label: string;
  shortLabel: string;
  description: string;
  badge: string;
}

export const PRACTICE_MODE_OPTIONS: PracticeModeOption[] = [
  {
    value: "EASY",
    label: "Easy",
    shortLabel: "Easy",
    description: "~15–20% key words",
    badge: "15–20%",
  },
  {
    value: "MEDIUM",
    label: "Medium",
    shortLabel: "Medium",
    description: "~30–40% balanced listening",
    badge: "30–40%",
  },
  {
    value: "HARD",
    label: "Hard",
    shortLabel: "Hard",
    description: "~50–60% connected speech & weak forms",
    badge: "50–60%",
  },
  {
    value: "FULL",
    label: "Full Dictation",
    shortLabel: "Full",
    description: "100% full sentence",
    badge: "100%",
  },
];

interface PracticeModeSelectorProps {
  mode: DictationMode;
  onChange: (mode: DictationMode) => void;
  disabled?: boolean;
  compact?: boolean;
}

export default function PracticeModeSelector({
  mode,
  onChange,
  disabled = false,
  compact = false,
}: PracticeModeSelectorProps) {
  const activeOption =
    PRACTICE_MODE_OPTIONS.find((opt) => opt.value === mode) ??
    PRACTICE_MODE_OPTIONS[3];

  return (
    <div
      className="w-full flex flex-col gap-2"
      data-testid="practice-mode-selector"
    >
      <div
        role="radiogroup"
        aria-label="Dictation difficulty mode"
        className={`grid grid-cols-2 sm:grid-cols-4 gap-1.5 sm:gap-2 ${
          compact ? "" : "max-w-3xl"
        }`}
      >
        {PRACTICE_MODE_OPTIONS.map((option) => {
          const isActive = option.value === mode;
          return (
            <button
              key={option.value}
              type="button"
              role="radio"
              aria-checked={isActive}
              disabled={disabled}
              onClick={() => {
                if (option.value !== mode) {
                  onChange(option.value);
                }
              }}
              className={`group relative flex flex-col items-start justify-between rounded-xl border px-3 py-2 text-left transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed min-h-[44px] ${
                isActive
                  ? "border-brand-500 bg-brand-50 dark:bg-brand-900/40 text-brand-800 dark:text-brand-100 shadow-xs ring-1 ring-brand-500/30"
                  : "border-ink-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-ink-700 dark:text-slate-200 hover:border-brand-300 dark:hover:border-brand-700 hover:bg-ink-50/70 dark:hover:bg-slate-800/70"
              }`}
            >
              <div className="w-full flex items-center justify-between gap-1.5">
                <span className="text-xs font-extrabold tracking-tight flex items-center gap-1">
                  {isActive && (
                    <Check
                      size={13}
                      className="text-brand-600 dark:text-brand-300 shrink-0"
                      aria-hidden="true"
                    />
                  )}
                  <span>{option.label}</span>
                </span>
                <span
                  className={`text-[10px] font-bold px-1.5 py-0.5 rounded-md shrink-0 ${
                    isActive
                      ? "bg-brand-500 text-white"
                      : "bg-ink-100 dark:bg-slate-800 text-ink-500 dark:text-slate-400"
                  }`}
                >
                  {option.badge}
                </span>
              </div>
              <span
                className={`mt-0.5 text-[11px] leading-snug line-clamp-2 ${
                  isActive
                    ? "text-brand-700 dark:text-brand-200 font-semibold"
                    : "text-ink-500 dark:text-slate-400"
                }`}
              >
                {option.description}
              </span>
            </button>
          );
        })}
      </div>

      <div className="flex items-center gap-1.5 text-[11px] text-ink-600 dark:text-slate-300 px-0.5">
        <Sparkles
          size={12}
          className="text-brand-500 dark:text-brand-400 shrink-0"
          aria-hidden="true"
        />
        <span>
          Active mode:{" "}
          <strong className="font-bold text-ink-900 dark:text-slate-100">
            {activeOption.label}
          </strong>{" "}
          — {activeOption.description}
        </span>
      </div>
    </div>
  );
}
