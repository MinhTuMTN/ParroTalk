"use client";

import React, { useState } from "react";
import Link from "next/link";
import { Volume2, Bookmark, Check, ArrowRight, Sparkles } from "lucide-react";
import type { RichVocabularyDetailData } from "./types";

interface Props {
  data: RichVocabularyDetailData;
  isSaved?: boolean;
  onSave?: () => void;
}

export function WordDetailHero({ data, isSaved = false, onSave }: Props) {
  const [playingAccent, setPlayingAccent] = useState<"uk" | "us" | null>(null);
  const [savedLocal, setSavedLocal] = useState(isSaved);

  const playPronunciation = (accent: "uk" | "us") => {
    setPlayingAccent(accent);
    const audioData = data.pronunciations[accent];

    if (audioData?.audioUrl) {
      const audio = new Audio(audioData.audioUrl);
      audio.onended = () => setPlayingAccent(null);
      audio.onerror = () => fallbackSpeak(accent);
      audio.play().catch(() => fallbackSpeak(accent));
      return;
    }

    fallbackSpeak(accent);
  };

  const fallbackSpeak = (accent: "uk" | "us") => {
    if (typeof window !== "undefined" && "speechSynthesis" in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(data.word);
      utterance.lang = accent === "uk" ? "en-GB" : "en-US";
      utterance.rate = 0.9;
      utterance.onend = () => setPlayingAccent(null);
      utterance.onerror = () => setPlayingAccent(null);
      window.speechSynthesis.speak(utterance);
    } else {
      setPlayingAccent(null);
    }
  };

  const handleToggleSave = () => {
    setSavedLocal(!savedLocal);
    onSave?.();
  };

  // Primary topic for practice link
  const primaryTopic = data.topics[0] || "Business";

  return (
    <section className="relative overflow-hidden rounded-3xl border border-brand-200/80 bg-brand-50/70 p-6 sm:p-8 shadow-card animate-fade-up">
      {/* Background subtle mint glow */}
      <div className="pointer-events-none absolute -right-16 -top-16 h-56 w-56 rounded-full bg-white/60 blur-3xl" />
      <div className="pointer-events-none absolute -bottom-16 left-1/3 h-48 w-48 rounded-full bg-brand-200/40 blur-3xl" />

      <div className="relative flex flex-col md:flex-row md:items-start justify-between gap-6">
        {/* ── Left content ── */}
        <div className="flex-1 min-w-0 space-y-3">
          {/* Eyebrow */}
          <div className="text-[10px] font-extrabold uppercase tracking-widest text-brand-700">
            WORD REFERENCE · SAMPLE ENTRY
          </div>

          {/* Word title & New pill */}
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-4xl sm:text-5xl font-extrabold text-ink-900 tracking-tight">
              {data.word}
            </h1>
            {data.isNew && (
              <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 border border-emerald-200/80 px-2.5 py-0.5 text-[11px] font-bold text-emerald-800 shadow-2xs">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-600 animate-pulse" />
                <span>New</span>
              </span>
            )}
          </div>

          {/* Part of speech & CEFR range */}
          <p className="text-xs sm:text-sm font-medium text-ink-500">
            {data.partOfSpeechSummary}
          </p>

          {/* Vietnamese meaning summary */}
          <div className="text-sm sm:text-base font-bold text-ink-800 leading-snug">
            {data.vietnameseSummary}
          </div>

          {/* Audio pronunciations */}
          <div className="flex flex-wrap items-center gap-2.5 pt-1">
            {/* UK Audio */}
            <button
              type="button"
              onClick={() => playPronunciation("uk")}
              className={`inline-flex items-center gap-1.5 rounded-xl border border-brand-200 bg-white hover:bg-brand-50 px-3.5 py-1.5 text-xs font-semibold text-brand-800 shadow-2xs transition-all ${
                playingAccent === "uk" ? "ring-2 ring-brand-300 bg-brand-50" : ""
              }`}
            >
              <Volume2 className={`w-3.5 h-3.5 ${playingAccent === "uk" ? "animate-pulse text-brand-600" : "text-brand-700"}`} />
              <span>UK</span>
              <span className="font-mono text-ink-500 font-normal">{data.pronunciations.uk.ipa}</span>
            </button>

            {/* US Audio */}
            <button
              type="button"
              onClick={() => playPronunciation("us")}
              className={`inline-flex items-center gap-1.5 rounded-xl border border-brand-200 bg-white hover:bg-brand-50 px-3.5 py-1.5 text-xs font-semibold text-brand-800 shadow-2xs transition-all ${
                playingAccent === "us" ? "ring-2 ring-brand-300 bg-brand-50" : ""
              }`}
            >
              <Volume2 className={`w-3.5 h-3.5 ${playingAccent === "us" ? "animate-pulse text-brand-600" : "text-brand-700"}`} />
              <span>US</span>
              <span className="font-mono text-ink-500 font-normal">{data.pronunciations.us.ipa}</span>
            </button>
          </div>

          {/* Topic tags row */}
          <div className="flex flex-wrap items-center gap-2 pt-1.5">
            {data.topics.map((t) => (
              <span
                key={t}
                className="inline-flex items-center rounded-lg border border-ink-200/80 bg-white/80 px-2.5 py-1 text-[11px] font-semibold text-ink-600 shadow-2xs backdrop-blur"
              >
                {t}
              </span>
            ))}
          </div>
        </div>

        {/* ── Right Action Box ── */}
        <div className="flex flex-col items-center md:items-end justify-center shrink-0 space-y-2 pt-2 md:pt-0">
          <button
            type="button"
            onClick={handleToggleSave}
            className={`w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-xl px-5 py-2.5 text-xs font-bold transition-all shadow-sm active:scale-[0.98] ${
              savedLocal
                ? "bg-brand-100 text-brand-800 border border-brand-300"
                : "bg-brand-600 hover:bg-brand-700 text-white shadow-brand"
            }`}
          >
            {savedLocal ? (
              <>
                <Check className="w-4 h-4 stroke-[2.5]" />
                <span>Saved to My Words</span>
              </>
            ) : (
              <>
                <Bookmark className="w-4 h-4" />
                <span>Add to My Words</span>
              </>
            )}
          </button>

          <Link
            href={`/vocabulary/practice?topic=${encodeURIComponent(primaryTopic)}&mode=learn-words`}
            className="group inline-flex items-center gap-1.5 text-xs font-bold text-brand-700 hover:text-brand-800 transition-colors pt-1"
          >
            <span>Learn this word</span>
            <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-1" />
          </Link>

          <p className="text-[10px] text-ink-400 text-center md:text-right max-w-[210px] leading-tight pt-0.5">
            Session-only preview. Nothing is saved to an account. Learn opens the sample {primaryTopic} session.
          </p>
        </div>
      </div>
    </section>
  );
}
