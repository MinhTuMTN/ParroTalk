"use client";

import React, { useState } from "react";
import { ChevronDown, ChevronUp, MessageSquare } from "lucide-react";
import type { WordIdiomItem } from "./types";

interface Props {
  word: string;
  idioms: WordIdiomItem[];
}

export function WordDetailIdioms({ word, idioms }: Props) {
  const [isExpanded, setIsExpanded] = useState(true);

  if (!idioms || idioms.length === 0) return null;

  return (
    <section
      id="idioms-expressions"
      className="rounded-3xl border border-ink-200/80 bg-white p-6 sm:p-7 shadow-card transition-all space-y-4"
    >
      {/* Header */}
      <div
        onClick={() => setIsExpanded(!isExpanded)}
        className="flex items-center justify-between cursor-pointer select-none"
      >
        <div className="space-y-0.5">
          <h3 className="text-base sm:text-lg font-bold text-ink-900">
            Idioms &amp; expressions
          </h3>
          <p className="text-xs text-ink-400 font-medium">
            {idioms.length} expressions · when &ldquo;{word}&rdquo; means a matter
          </p>
        </div>

        <button
          type="button"
          className="text-ink-400 hover:text-ink-700 transition-colors p-1"
          aria-label="Toggle Idioms & expressions"
        >
          {isExpanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
        </button>
      </div>

      {/* Content list */}
      {isExpanded && (
        <div className="space-y-6 pt-1 animate-fade-down divide-y divide-ink-100/70">
          {idioms.map((idm, idx) => (
            <div key={idx} className="space-y-3 pt-4 first:pt-0">
              {/* Idiom title */}
              <div>
                <div className="text-base font-bold text-brand-800">
                  {idm.idiom}
                </div>
                {idm.relatedSenseNote && (
                  <div className="text-[11px] text-ink-400 font-medium pt-0.5">
                    {idm.relatedSenseNote}
                  </div>
                )}
              </div>

              {/* Definitions */}
              <div className="space-y-1">
                <p className="text-xs sm:text-sm font-semibold text-ink-900 leading-relaxed font-sans">
                  {idm.definitionEn}
                </p>
                <p className="text-xs text-ocean-700 font-medium">
                  {idm.definitionVn}
                </p>
              </div>

              {/* In context */}
              {idm.exampleEn && (
                <div className="rounded-2xl border border-brand-200/50 bg-[#f4f9f5] p-4 space-y-1">
                  <p className="text-xs sm:text-sm font-semibold text-ink-900 leading-relaxed">
                    &ldquo;{idm.exampleEn}&rdquo;
                  </p>
                  {idm.exampleVn && (
                    <p className="text-xs text-ink-500 font-medium">
                      {idm.exampleVn}
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
