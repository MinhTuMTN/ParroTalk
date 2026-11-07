"use client";

import React, { useState } from "react";
import Link from "next/link";
import { Check, Volume2, ChevronLeft, ChevronRight } from "lucide-react";
import type { VocabularySummaryDto } from "../../types";

export interface WordItemWithStatus extends VocabularySummaryDto {
  status?: "mastered" | "learning" | "new";
}

interface Props {
  words: WordItemWithStatus[];
  totalWords: number;
  loading?: boolean;
  page?: number;
  totalPages?: number;
  onPrevPage?: () => void;
  onNextPage?: () => void;
}

export function TopicWordList({
  words,
  totalWords,
  loading,
  page = 0,
  totalPages = 1,
  onPrevPage,
  onNextPage,
}: Props) {
  const [playingId, setPlayingId] = useState<string | null>(null);

  const displayWords = words;

  const handlePlayAudio = (e: React.MouseEvent, word: WordItemWithStatus) => {
    e.stopPropagation();
    e.preventDefault();

    setPlayingId(word.id);

    if (word.audioUsUrl || word.audioUkUrl) {
      const audio = new Audio(word.audioUsUrl || word.audioUkUrl);
      audio.onended = () => setPlayingId(null);
      audio.onerror = () => {
        fallbackSpeech(word.word);
      };
      audio.play().catch(() => fallbackSpeech(word.word));
    } else {
      fallbackSpeech(word.word);
    }
  };

  const fallbackSpeech = (text: string) => {
    if (typeof window !== "undefined" && "speechSynthesis" in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = "en-US";
      utterance.onend = () => setPlayingId(null);
      utterance.onerror = () => setPlayingId(null);
      window.speechSynthesis.speak(utterance);
    } else {
      setPlayingId(null);
    }
  };

  return (
    <section className="space-y-4">
      {/* Section Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-bold text-ink-900 tracking-tight">A peek at your words</h2>
          <p className="text-xs text-ink-500 mt-0.5">
            {displayWords.length} of {totalWords} words · A little taste of what&apos;s inside
          </p>
        </div>

        {(totalPages > 1 || totalWords > 6) && (
          <div className="flex items-center gap-1.5" title={`Page ${page + 1} of ${totalPages}`}>
            <button
              type="button"
              onClick={onPrevPage}
              disabled={page === 0 || loading}
              className="w-8 h-8 rounded-lg border border-ink-200 bg-white text-ink-600 flex items-center justify-center hover:bg-brand-50 hover:text-brand-700 hover:border-brand-300 disabled:opacity-30 disabled:pointer-events-none transition-all shadow-xs cursor-pointer"
              aria-label="Previous words"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={onNextPage}
              disabled={page >= totalPages - 1 || loading}
              className="w-8 h-8 rounded-lg border border-ink-200 bg-white text-ink-600 flex items-center justify-center hover:bg-brand-50 hover:text-brand-700 hover:border-brand-300 disabled:opacity-30 disabled:pointer-events-none transition-all shadow-xs cursor-pointer"
              aria-label="Next words"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>

      {/* Word Cards List */}
      <div className="rounded-2xl border border-ink-200/80 bg-white shadow-card divide-y divide-ink-100 overflow-hidden">
        {loading ? (
          Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="p-4 px-5 animate-pulse flex items-center justify-between">
              <div className="space-y-2">
                <div className="h-4 w-32 bg-ink-100 rounded" />
                <div className="h-3 w-48 bg-ink-50 rounded" />
              </div>
              <div className="flex items-center gap-3">
                <div className="h-6 w-16 bg-ink-100 rounded-md" />
                <div className="h-8 w-8 bg-ink-100 rounded-full" />
              </div>
            </div>
          ))
        ) : displayWords.length === 0 ? (
          <div className="p-8 text-center text-sm text-ink-400">
            No words found for this topic yet.
          </div>
        ) : (
          displayWords.map((item) => (
            <div
              key={item.id}
              className="group p-4 px-5 flex items-center justify-between gap-4 hover:bg-brand-50/40 transition-colors"
            >
              {/* Word & Meaning Info */}
              <Link
                href={`/vocabulary/${item.id}`}
                className="flex-1 min-w-0 space-y-1 block focus:outline-none"
              >
                <div className="flex items-baseline gap-2 flex-wrap">
                  <span className="font-bold text-base text-ink-900 group-hover:text-brand-700 transition-colors">
                    {item.word}
                  </span>
                  {item.phonetic && (
                    <span className="text-xs text-ink-400 font-mono">
                      {item.phonetic}
                    </span>
                  )}
                </div>

                <div className="text-xs flex items-center gap-1.5 flex-wrap">
                  {item.partOfSpeech && (
                    <span className="font-semibold text-ink-500 italic">
                      {item.partOfSpeech}
                    </span>
                  )}
                  <span className="text-ink-600 truncate">
                    {item.commonMeaningVi || "Ý nghĩa từ vựng"}
                  </span>
                </div>
              </Link>

              {/* Status Badge & Audio Action */}
              <div className="flex items-center gap-3 shrink-0">
                {item.status === "mastered" ? (
                  <span className="inline-flex items-center gap-1 rounded-md bg-brand-50 text-brand-700 ring-1 ring-inset ring-brand-200 px-2 py-0.5 text-[11px] font-semibold">
                    <Check className="w-3 h-3" strokeWidth={2.5} />
                    Mastered
                  </span>
                ) : item.status === "learning" ? (
                  <span className="inline-flex items-center gap-1 rounded-md bg-honey-50 text-honey-600 ring-1 ring-inset ring-honey-200 px-2 py-0.5 text-[11px] font-semibold">
                    Learning
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1 rounded-md bg-ink-100 text-ink-500 px-2 py-0.5 text-[11px] font-semibold">
                    New
                  </span>
                )}

                <button
                  type="button"
                  onClick={(e) => handlePlayAudio(e, item)}
                  aria-label={`Pronounce ${item.word}`}
                  className={`w-8 h-8 rounded-full border border-ink-200 flex items-center justify-center transition-all ${
                    playingId === item.id
                      ? "bg-brand-500 text-white border-brand-500 scale-105"
                      : "text-ink-400 hover:text-brand-700 hover:border-brand-300 hover:bg-brand-50"
                  }`}
                >
                  <Volume2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </section>
  );
}
