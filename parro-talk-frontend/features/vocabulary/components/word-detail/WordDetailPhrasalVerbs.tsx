"use client";

import React, { useState } from "react";
import { ChevronDown, ChevronUp, Link as LinkIcon, Volume2 } from "lucide-react";
import type { WordPhrasalVerbItem } from "./types";

interface Props {
  phrasalVerbs: WordPhrasalVerbItem[];
}

export function WordDetailPhrasalVerbs({ phrasalVerbs }: Props) {
  const [isExpanded, setIsExpanded] = useState(true);

  if (!phrasalVerbs || phrasalVerbs.length === 0) return null;

  return (
    <section
      id="phrasal-verbs"
      className="rounded-3xl border border-ink-200/80 bg-white p-6 sm:p-7 shadow-card transition-all space-y-4"
    >
      {/* Header */}
      <div
        onClick={() => setIsExpanded(!isExpanded)}
        className="flex items-center justify-between cursor-pointer select-none"
      >
        <div className="space-y-0.5">
          <div className="flex items-center gap-2">
            <h3 className="text-base sm:text-lg font-bold text-ink-900">
              Phrasal verbs
            </h3>
          </div>
          <p className="text-xs text-ink-400 font-medium">
            {phrasalVerbs.length} formal expression · explore a less common use
          </p>
        </div>

        <button
          type="button"
          className="text-ink-400 hover:text-ink-700 transition-colors p-1"
          aria-label="Toggle Phrasal verbs"
        >
          {isExpanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
        </button>
      </div>

      {/* Content */}
      {isExpanded && (
        <div className="space-y-5 pt-1 animate-fade-down">
          {phrasalVerbs.map((pv, idx) => (
            <div key={idx} className="space-y-3 pt-2 first:pt-0">
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-base font-bold text-brand-800">
                  {pv.phrasalVerb}
                </span>
                {pv.register && (
                  <span className="text-[11px] font-semibold text-ink-400 italic">
                    [{pv.register}]
                  </span>
                )}
              </div>

              <div className="space-y-1">
                <p className="text-xs sm:text-sm font-semibold text-ink-900 leading-relaxed font-sans">
                  {pv.definitionEn}
                </p>
                <p className="text-xs text-ocean-700 font-medium">
                  {pv.definitionVn}
                </p>
              </div>

              {/* Example context */}
              {pv.exampleEn && (
                <div className="rounded-2xl border border-brand-200/50 bg-[#f4f9f5] p-4 space-y-1">
                  <p className="text-xs sm:text-sm font-semibold text-ink-900 leading-relaxed">
                    &ldquo;{pv.exampleEn}&rdquo;
                  </p>
                  {pv.exampleVn && (
                    <p className="text-xs text-ink-500 font-medium">
                      {pv.exampleVn}
                    </p>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
