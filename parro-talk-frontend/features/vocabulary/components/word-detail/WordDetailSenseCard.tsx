"use client";

import React, { useState } from "react";
import { ChevronDown, ChevronUp, Volume2, Sparkles, BookOpen } from "lucide-react";
import type { WordSenseItem } from "./types";

interface Props {
  sense: WordSenseItem;
}

export function WordDetailSenseCard({ sense }: Props) {
  const [isOpen, setIsOpen] = useState(false);
  const [isPlayingExample, setIsPlayingExample] = useState(false);

  const playSentence = (text: string) => {
    if (typeof window === "undefined" || !("speechSynthesis" in window)) return;
    setIsPlayingExample(true);
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = "en-US";
    utterance.rate = 0.95;
    utterance.onend = () => setIsPlayingExample(false);
    utterance.onerror = () => setIsPlayingExample(false);
    window.speechSynthesis.speak(utterance);
  };

  const hasExtraData =
    Boolean(sense.collocations?.length) ||
    Boolean(sense.synonyms?.length) ||
    Boolean(sense.additionalExamples?.length);

  return (
    <article
      id={`sense-${sense.orderNumber}`}
      className="group rounded-3xl border border-ink-200/80 bg-white p-6 sm:p-7 shadow-card hover:border-brand-300 hover:shadow-card-hover transition-all space-y-4"
    >
      {/* ── Top Row: Number, Title, Grammar Tags & Topic ── */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          {/* Order number */}
          <span className="flex items-center justify-center w-8 h-8 rounded-xl bg-brand-50 border border-brand-200/70 text-xs font-black text-brand-700 shrink-0">
            {sense.orderNumber}
          </span>

          <div className="space-y-0.5">
            <h3 className="text-base sm:text-lg font-bold text-ink-900 leading-snug">
              {sense.title}
            </h3>
            <div className="flex items-center gap-2 text-xs text-ink-500 font-medium">
              <span>{sense.partOfSpeech}</span>
              {sense.countability && (
                <>
                  <span className="text-ink-300">•</span>
                  <span>{sense.countability}</span>
                </>
              )}
              {sense.cefrLevel && (
                <>
                  <span className="text-ink-300">•</span>
                  <span className="inline-flex items-center rounded-md bg-brand-50 border border-brand-200/80 px-1.5 py-0.2 text-[10px] font-bold text-brand-700">
                    {sense.cefrLevel}
                  </span>
                </>
              )}
            </div>
          </div>
        </div>

        {/* Topic Tag */}
        {sense.topicTag && (
          <span className="self-start sm:self-center inline-flex items-center rounded-lg border border-brand-200/70 bg-brand-50/60 px-2.5 py-1 text-xs font-semibold text-brand-700 shrink-0">
            {sense.topicTag}
          </span>
        )}
      </div>

      {/* ── Definitions & Register/Context ── */}
      <div className="space-y-1 pt-1">
        <p className="text-sm sm:text-base font-semibold text-ink-900 leading-relaxed font-sans">
          {sense.definitionEn}
        </p>

        <p className="text-xs sm:text-sm font-medium text-ocean-700">
          {sense.definitionVn}
        </p>

        {sense.usageContextNote && (
          <p className="text-[11px] text-ink-400 italic pt-0.5">
            {sense.usageContextNote}
          </p>
        )}
      </div>

      {/* ── In Context Sentence Box ── */}
      {sense.inContext && (
        <div className="rounded-2xl border border-brand-200/60 bg-[#f4f9f5] p-4 sm:p-5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-extrabold uppercase tracking-widest text-brand-700">
              IN CONTEXT
            </span>

            <button
              type="button"
              onClick={() => playSentence(sense.inContext.english)}
              className="inline-flex items-center gap-1 text-[11px] font-semibold text-brand-700 hover:text-brand-800 transition-colors"
              title="Listen to sentence"
            >
              <Volume2 className={`w-3.5 h-3.5 ${isPlayingExample ? "animate-pulse text-brand-600" : ""}`} />
              <span>Listen</span>
            </button>
          </div>

          <p className="text-xs sm:text-sm font-semibold text-ink-900 leading-relaxed">
            &ldquo;{sense.inContext.english}&rdquo;
          </p>

          <p className="text-xs text-ink-500 font-medium">
            {sense.inContext.vietnamese}
          </p>
        </div>
      )}

      {/* ── Collapsible Usage & Related Words ── */}
      {hasExtraData && (
        <div className="pt-1 border-t border-ink-100/70">
          <button
            type="button"
            onClick={() => setIsOpen(!isOpen)}
            className="group/btn inline-flex items-center gap-1.5 text-xs font-bold text-brand-700 hover:text-brand-800 transition-colors"
          >
            <span>Usage &amp; related words</span>
            {isOpen ? (
              <ChevronUp className="w-3.5 h-3.5 transition-transform" />
            ) : (
              <ChevronDown className="w-3.5 h-3.5 transition-transform group-hover/btn:translate-y-0.5" />
            )}
          </button>

          {isOpen && (
            <div className="mt-3 rounded-2xl bg-ink-50/60 border border-ink-100 p-4 space-y-3 animate-fade-down text-xs">
              {/* Collocations */}
              {sense.collocations && sense.collocations.length > 0 && (
                <div className="space-y-1">
                  <div className="font-bold text-ink-700 text-[11px] uppercase tracking-wide">
                    COMMON COLLOCATIONS
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {sense.collocations.map((c) => (
                      <span
                        key={c}
                        className="rounded-lg bg-white border border-ink-200/80 px-2 py-1 text-ink-700 font-medium text-xs shadow-2xs"
                      >
                        {c}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* Synonyms */}
              {sense.synonyms && sense.synonyms.length > 0 && (
                <div className="space-y-1">
                  <div className="font-bold text-ink-700 text-[11px] uppercase tracking-wide">
                    SYNONYMS
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {sense.synonyms.map((s) => (
                      <span
                        key={s}
                        className="rounded-lg bg-brand-50/70 border border-brand-200/80 px-2 py-1 text-brand-800 font-semibold text-xs shadow-2xs"
                      >
                        {s}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* Additional Examples */}
              {sense.additionalExamples && sense.additionalExamples.length > 0 && (
                <div className="space-y-1.5 pt-1">
                  <div className="font-bold text-ink-700 text-[11px] uppercase tracking-wide">
                    ADDITIONAL EXAMPLES
                  </div>
                  <div className="space-y-2">
                    {sense.additionalExamples.map((ex, i) => (
                      <div key={i} className="space-y-0.5 border-l-2 border-brand-300 pl-2.5">
                        <div className="font-semibold text-ink-900">{ex.english}</div>
                        <div className="text-ink-500 text-[11px]">{ex.vietnamese}</div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </article>
  );
}
