"use client";

import type { DictationMode } from "@/features/lesson/services/lessonService";
import { Check, ChevronDown } from "lucide-react";
import { useEffect, useRef, useState } from "react";

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
}

export default function PracticeModeSelector({
  mode,
  onChange,
  disabled = false,
}: PracticeModeSelectorProps) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const open = isOpen && !disabled;

  const activeOption =
    PRACTICE_MODE_OPTIONS.find((opt) => opt.value === mode) ??
    PRACTICE_MODE_OPTIONS[3];

  useEffect(() => {
    if (!open) {
      return;
    }

    const handleOutsideClick = (event: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    const handleEscapeCapture = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        event.stopPropagation();
        event.stopImmediatePropagation();
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handleOutsideClick);
    window.addEventListener("keydown", handleEscapeCapture, true);

    return () => {
      document.removeEventListener("mousedown", handleOutsideClick);
      window.removeEventListener("keydown", handleEscapeCapture, true);
    };
  }, [open]);

  return (
    <div
      ref={containerRef}
      className="relative inline-block"
      data-testid="practice-mode-selector"
    >
      <button
        type="button"
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-label={`Dictation mode: ${activeOption.label} (${activeOption.badge})`}
        disabled={disabled}
        onClick={() => setIsOpen((prev) => !prev)}
        className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 min-h-[36px] [@media(pointer:coarse)]:min-h-[44px] text-xs font-bold transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed ${
          open
            ? "border-emerald-400 dark:border-emerald-500 bg-emerald-50/70 dark:bg-emerald-950/50 text-ink-900 dark:text-slate-100 shadow-xs"
            : "border-ink-200/80 dark:border-slate-700 bg-white dark:bg-slate-800/90 text-ink-800 dark:text-slate-100 hover:border-emerald-300 dark:hover:border-emerald-600 hover:bg-ink-50/70 dark:hover:bg-slate-800"
        }`}
      >
        <span
          className="w-2 h-2 rounded-full bg-emerald-500 shrink-0"
          aria-hidden="true"
        />
        <span className="sm:hidden truncate">{activeOption.shortLabel}</span>
        <span className="hidden sm:inline whitespace-nowrap">
          {activeOption.label}
        </span>
        <span
          className="text-ink-300 dark:text-slate-600 font-normal"
          aria-hidden="true"
        >
          ·
        </span>
        <span className="text-[11px] font-bold px-1.5 py-0.5 rounded-full bg-emerald-50 dark:bg-emerald-950/70 text-emerald-700 dark:text-emerald-300 whitespace-nowrap">
          {activeOption.badge}
        </span>
        <ChevronDown
          size={14}
          aria-hidden="true"
          className={`text-ink-400 dark:text-slate-400 shrink-0 transition-transform duration-200 ${
            open ? "rotate-180" : ""
          }`}
        />
      </button>

      {open && (
        <div className="absolute left-0 mt-2 w-72 max-w-[calc(100vw-1.5rem)] rounded-2xl bg-white dark:bg-slate-900 border border-ink-200/80 dark:border-slate-700 shadow-xl p-1.5 z-50">
          <div
            role="radiogroup"
            aria-label="Dictation difficulty mode"
            className="flex flex-col gap-1"
          >
            {PRACTICE_MODE_OPTIONS.map((option) => {
              const isActive = option.value === mode;
              return (
                <button
                  key={option.value}
                  type="button"
                  role="radio"
                  aria-checked={isActive}
                  onClick={() => {
                    if (option.value !== mode) {
                      onChange(option.value);
                    }
                    setIsOpen(false);
                  }}
                  className={`w-full flex items-start gap-2.5 rounded-xl px-2.5 py-2 min-h-[44px] text-left transition-colors cursor-pointer ${
                    isActive
                      ? "bg-emerald-50/90 dark:bg-emerald-950/50 text-emerald-900 dark:text-emerald-100 ring-1 ring-emerald-500/30 dark:ring-emerald-400/30"
                      : "text-ink-700 dark:text-slate-200 hover:bg-ink-100/70 dark:hover:bg-slate-800/80"
                  }`}
                >
                  <span className="w-4 h-4 mt-0.5 flex items-center justify-center shrink-0">
                    {isActive && (
                      <Check
                        size={14}
                        className="text-emerald-600 dark:text-emerald-400"
                        aria-hidden="true"
                      />
                    )}
                  </span>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-xs font-extrabold truncate">
                        {option.label}
                      </span>
                      <span
                        className={`text-[10px] font-bold px-1.5 py-0.5 rounded-full shrink-0 ${
                          isActive
                            ? "bg-emerald-100 dark:bg-emerald-900/70 text-emerald-700 dark:text-emerald-200"
                            : "bg-ink-100 dark:bg-slate-800 text-ink-600 dark:text-slate-300"
                        }`}
                      >
                        {option.badge}
                      </span>
                    </div>
                    <p className="text-[11px] text-ink-500 dark:text-slate-400 mt-0.5 leading-snug">
                      {option.description}
                    </p>
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
