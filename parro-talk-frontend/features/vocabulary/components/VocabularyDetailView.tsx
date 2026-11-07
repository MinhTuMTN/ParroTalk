"use client";

import React, { useMemo } from "react";
import Link from "next/link";
import { ArrowLeft, Sprout } from "lucide-react";
import type { VocabularyDetailDto } from "../types";
import {
  WordDetailHero,
  WordDetailSenseCard,
  WordDetailPhrasalVerbs,
  WordDetailIdioms,
  WordDetailSidebar,
  SAMPLE_ISSUE_DETAIL,
  transformDtoToRichDetail,
  type RichVocabularyDetailData,
} from "./word-detail";

interface Props {
  vocab?: VocabularyDetailDto;
  onSave?: () => void;
  onReport?: () => void;
}

export const VocabularyDetailView: React.FC<Props> = ({ vocab, onSave, onReport }) => {
  const detailData: RichVocabularyDetailData = useMemo(() => {
    if (vocab) {
      return transformDtoToRichDetail(vocab);
    }
    return SAMPLE_ISSUE_DETAIL;
  }, [vocab]);

  return (
    <div className="space-y-8 animate-fade-up">
      {/* ── Top Navigation: Back to Vocabulary ── */}
      <div>
        <Link
          href="/vocabulary"
          className="group inline-flex items-center gap-2 text-xs font-semibold text-ink-500 hover:text-brand-700 transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5 transition-transform group-hover:-translate-x-1" />
          <span>Back to Vocabulary</span>
        </Link>
      </div>

      {/* ── Hero Section ── */}
      <WordDetailHero
        data={detailData}
        isSaved={vocab?.isSaved}
        onSave={onSave}
      />

      {/* ── Main Two-Column Layout ── */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Left Column (Main content: senses, phrasal verbs, idioms) */}
        <div className="lg:col-span-8 space-y-6">
          {/* Section title */}
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-extrabold text-ink-900 tracking-tight">
              Meanings &amp; senses
            </h2>
            <span className="text-xs font-medium text-ink-400">
              {detailData.senses.length} meanings
            </span>
          </div>

          {/* Senses Cards */}
          <div className="space-y-4">
            {detailData.senses.map((sense) => (
              <WordDetailSenseCard key={sense.id} sense={sense} />
            ))}
          </div>

          {/* Phrasal verbs */}
          {detailData.phrasalVerbs && detailData.phrasalVerbs.length > 0 && (
            <WordDetailPhrasalVerbs phrasalVerbs={detailData.phrasalVerbs} />
          )}

          {/* Idioms & expressions */}
          {detailData.idioms && detailData.idioms.length > 0 && (
            <WordDetailIdioms word={detailData.word} idioms={detailData.idioms} />
          )}
        </div>

        {/* Right Column (Sticky Sidebar: In this word) */}
        <div className="lg:col-span-4">
          <WordDetailSidebar
            senses={detailData.senses}
            hasPhrasalVerbs={Boolean(detailData.phrasalVerbs?.length)}
            hasIdioms={Boolean(detailData.idioms?.length)}
          />
        </div>
      </div>

      {/* ── Bottom Microcopy ── */}
      <div className="pt-8 border-t border-ink-100 text-center space-y-2">
        <p className="text-[11px] text-ink-400 max-w-2xl mx-auto leading-relaxed">
          Curated sample entry; not imported production data. Core senses reuse the Learn Words sample. Finance and expressions are supplementary. CEFR labels are indicative for each sense, not a universal level for the word. Audio uses available device voices.
        </p>
        <div className="inline-flex items-center gap-1.5 text-xs text-brand-700/80 font-medium">
          <Sprout className="w-3.5 h-3.5" />
          <span>One word, a little more understood.</span>
        </div>
      </div>
    </div>
  );
};