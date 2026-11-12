"use client";

import React from "react";
import {
  Sparkles,
  Lightbulb,
  BookOpenCheck,
  Wand2,
  RotateCcw,
} from "lucide-react";
import type { DifficultyScale, WritingMode } from "../types";

export const CEFR_LEVELS = [
  { value: "A1", label: "A1", desc: "Cơ bản" },
  { value: "A2", label: "A2", desc: "Sơ cấp" },
  { value: "B1", label: "B1", desc: "Trung cấp" },
  { value: "B2", label: "B2", desc: "Trung cao cấp" },
  { value: "C1", label: "C1", desc: "Cao cấp" },
  { value: "C2", label: "C2", desc: "Thành thạo" },
];

export const IELTS_LEVELS = [
  { value: "4.0", label: "4.0" },
  { value: "4.5", label: "4.5" },
  { value: "5.0", label: "5.0" },
  { value: "5.5", label: "5.5" },
  { value: "6.0", label: "6.0" },
  { value: "6.5", label: "6.5" },
  { value: "7.0", label: "7.0" },
  { value: "7.5", label: "7.5" },
  { value: "8.0", label: "8.0" },
];

export const SAMPLE_IDEAS = [
  {
    label: "An toàn giao thông",
    text: "Người say rượu không được lái xe vì nồng độ cồn trong máu làm giảm tầm nhìn, giảm sự tập trung.",
  },
  {
    label: "Môi trường & Khí hậu",
    text: "Chính phủ nên khuyến khích người dân sử dụng phương tiện công cộng để giảm lượng khí thải carbon ở các thành phố lớn.",
  },
  {
    label: "Công nghệ & Công việc",
    text: "Làm việc từ xa giúp nhân viên tiết kiệm thời gian đi lại nhưng đôi khi làm giảm sự gắn kết giữa các đồng nghiệp.",
  },
];

interface WritingPromptComposerProps {
  inputText: string;
  onInputTextChange: (value: string) => void;
  mode: WritingMode;
  onModeChange: (mode: WritingMode) => void;
  scaleType: DifficultyScale;
  onScaleTypeChange: (scale: DifficultyScale) => void;
  targetLevel: string;
  onTargetLevelChange: (level: string) => void;
  loading: boolean;
  onSubmit: () => void;
  onClear: () => void;
}

export function WritingPromptComposer({
  inputText,
  onInputTextChange,
  mode,
  onModeChange,
  scaleType,
  onScaleTypeChange,
  targetLevel,
  onTargetLevelChange,
  loading,
  onSubmit,
  onClear,
}: WritingPromptComposerProps) {
  const handleScaleSwitch = (newScale: DifficultyScale) => {
    onScaleTypeChange(newScale);
    if (newScale === "CEFR" && !CEFR_LEVELS.some((l) => l.value === targetLevel)) {
      onTargetLevelChange("B2");
    } else if (newScale === "IELTS" && !IELTS_LEVELS.some((l) => l.value === targetLevel)) {
      onTargetLevelChange("6.5");
    }
  };

  return (
    <section className="rounded-2xl border border-ink-200/80 bg-white p-4 sm:p-5 shadow-card space-y-3">
      {/* ── Top Row: Label + Inline Sample Chips + Character Count ── */}
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <label
            htmlFor="writing-idea-input"
            className="text-xs font-extrabold uppercase tracking-wider text-ink-600"
          >
            Ý tưởng cần diễn đạt
          </label>
          <span className="text-[11px] font-medium text-ink-400">
            ({inputText.length}/1000)
          </span>
        </div>

        <div className="flex flex-wrap items-center gap-1.5">
          <span className="text-[11px] font-semibold text-ink-400 inline-flex items-center gap-1">
            <Sparkles className="h-3 w-3 text-brand-600 shrink-0" aria-hidden="true" />
            Mẫu:
          </span>
          {SAMPLE_IDEAS.map((sample) => (
            <button
              key={sample.label}
              type="button"
              onClick={() => onInputTextChange(sample.text)}
              className="rounded-full border border-brand-200/80 bg-brand-50/60 px-2.5 py-0.5 text-[11px] font-semibold text-brand-700 hover:bg-brand-100/80 transition-colors cursor-pointer whitespace-nowrap"
            >
              {sample.label}
            </button>
          ))}
        </div>
      </div>

      {/* ── Main Idea Textarea ── */}
      <textarea
        id="writing-idea-input"
        rows={3}
        maxLength={1000}
        value={inputText}
        onChange={(e) => onInputTextChange(e.target.value)}
        placeholder="Nhập ý tưởng bằng tiếng Việt hoặc tiếng Anh (VD: Người say rượu không được lái xe vì nồng độ cồn trong máu làm giảm tầm nhìn...)"
        className="w-full rounded-xl border border-ink-200 bg-ink-50/40 px-3.5 py-3 text-sm sm:text-base text-ink-900 placeholder:text-ink-400 focus:border-brand-500 focus:bg-white focus:outline-none focus:ring-3 focus:ring-brand-500/15 transition-all resize-y"
      />

      {/* ── Compact Bottom Control Toolbar: Mode + Scale/Level + Actions ── */}
      <div className="flex flex-wrap items-center justify-between gap-2.5 pt-1.5 border-t border-ink-100">
        {/* Left: Compact Mode & Level Controls */}
        <div className="flex flex-wrap items-center gap-2">
          {/* 1. Compact Mode Segmented Control */}
          <div
            role="group"
            aria-label="Chế độ hỗ trợ"
            className="inline-flex items-center rounded-xl bg-ink-100/80 p-1 border border-ink-200/60"
          >
            <button
              type="button"
              aria-pressed={mode === "SUGGESTION"}
              onClick={() => onModeChange("SUGGESTION")}
              title="Suggestion Mode: Gợi ý nhanh từ vựng, collocations và cấu trúc câu để bạn tự đặt câu"
              className={`inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-xs font-bold transition-all cursor-pointer whitespace-nowrap ${
                mode === "SUGGESTION"
                  ? "bg-white text-brand-700 shadow-2xs ring-1 ring-brand-500/20"
                  : "text-ink-600 hover:text-ink-900"
              }`}
            >
              <Lightbulb className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
              <span>Gợi ý (Suggestion)</span>
            </button>

            <button
              type="button"
              aria-pressed={mode === "GUIDANCE"}
              onClick={() => onModeChange("GUIDANCE")}
              title="Guidance Mode: Giải thích chi tiết, hướng dẫn ghép câu và cung cấp câu mẫu hoàn chỉnh"
              className={`inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-xs font-bold transition-all cursor-pointer whitespace-nowrap ${
                mode === "GUIDANCE"
                  ? "bg-white text-brand-700 shadow-2xs ring-1 ring-brand-500/20"
                  : "text-ink-600 hover:text-ink-900"
              }`}
            >
              <BookOpenCheck className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
              <span>Hướng dẫn (Guidance)</span>
            </button>
          </div>

          {/* 2. Compact Scale & Target Level Toolbar */}
          <div
            role="group"
            aria-label="Trình độ mục tiêu"
            className="inline-flex flex-wrap items-center gap-1 rounded-xl bg-ink-100/80 p-1 border border-ink-200/60"
          >
            {/* Scale Toggle */}
            <button
              type="button"
              aria-pressed={scaleType === "CEFR"}
              onClick={() => handleScaleSwitch("CEFR")}
              className={`rounded-lg px-2 py-1 text-[11px] font-extrabold transition-all cursor-pointer whitespace-nowrap ${
                scaleType === "CEFR"
                  ? "bg-ink-900 text-white shadow-2xs"
                  : "text-ink-500 hover:text-ink-800"
              }`}
            >
              CEFR
            </button>
            <button
              type="button"
              aria-pressed={scaleType === "IELTS"}
              onClick={() => handleScaleSwitch("IELTS")}
              className={`rounded-lg px-2 py-1 text-[11px] font-extrabold transition-all cursor-pointer whitespace-nowrap ${
                scaleType === "IELTS"
                  ? "bg-ink-900 text-white shadow-2xs"
                  : "text-ink-500 hover:text-ink-800"
              }`}
            >
              IELTS
            </button>

            <span className="h-4 w-px bg-ink-200/90 mx-0.5" aria-hidden="true" />

            {/* Compact Level Pills */}
            {scaleType === "CEFR"
              ? CEFR_LEVELS.map((lvl) => {
                  const active = targetLevel === lvl.value;
                  return (
                    <button
                      key={lvl.value}
                      type="button"
                      aria-pressed={active}
                      onClick={() => onTargetLevelChange(lvl.value)}
                      title={`CEFR ${lvl.label} - ${lvl.desc}`}
                      className={`rounded-lg px-2 py-1 text-xs font-extrabold transition-all cursor-pointer whitespace-nowrap ${
                        active
                          ? "bg-brand-600 text-white shadow-2xs"
                          : "text-ink-600 hover:bg-white/70 hover:text-ink-900"
                      }`}
                    >
                      {lvl.label}
                    </button>
                  );
                })
              : IELTS_LEVELS.map((lvl) => {
                  const active = targetLevel === lvl.value;
                  return (
                    <button
                      key={lvl.value}
                      type="button"
                      aria-pressed={active}
                      onClick={() => onTargetLevelChange(lvl.value)}
                      title={`IELTS Band ${lvl.label}`}
                      className={`rounded-lg px-1.5 py-1 text-xs font-extrabold transition-all cursor-pointer whitespace-nowrap ${
                        active
                          ? "bg-brand-600 text-white shadow-2xs"
                          : "text-ink-600 hover:bg-white/70 hover:text-ink-900"
                      }`}
                    >
                      {lvl.label}
                    </button>
                  );
                })}
          </div>
        </div>

        {/* Right: Clear & Primary Submit Buttons */}
        <div className="flex items-center gap-2 ml-auto">
          {inputText.trim().length > 0 && (
            <button
              type="button"
              onClick={onClear}
              disabled={loading}
              title="Làm mới nội dung"
              className="inline-flex items-center justify-center gap-1.5 rounded-xl border border-ink-200 bg-white px-3 py-2 text-xs font-bold text-ink-600 hover:bg-ink-50 transition-colors cursor-pointer disabled:opacity-50 whitespace-nowrap"
            >
              <RotateCcw className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
              <span>Làm mới</span>
            </button>
          )}

          <button
            type="button"
            onClick={onSubmit}
            disabled={loading || inputText.trim().length < 3}
            aria-busy={loading}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-brand-600 px-4 py-2 text-xs sm:text-sm font-bold text-white shadow-brand hover:bg-brand-700 active:translate-y-0.5 transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap"
          >
            <Wand2 className={`h-4 w-4 shrink-0 ${loading ? "animate-spin" : ""}`} aria-hidden="true" />
            <span>
              {loading
                ? "Đang phân tích..."
                : mode === "SUGGESTION"
                ? "Phân tích & Gợi ý"
                : "Phân tích & Hướng dẫn"}
            </span>
          </button>
        </div>
      </div>
    </section>
  );
}
