"use client";

import React, { useState } from "react";
import {
  ArrowRight,
  BookOpen,
  BookOpenCheck,
  CheckCircle2,
  Eye,
  Layers,
  PenLine,
  PlusCircle,
  Sparkles,
} from "lucide-react";
import { useUserSettings } from "@/features/settings/hooks/useUserSettings";
import type {
  WritingLexicalItem,
  WritingSessionResponse,
} from "../types";
import { useWritingAudio } from "../hooks/useWritingAudio";
import { AccentAudioButtons, PhoneticBadge } from "./WritingAudioControls";
import { WritingAssessmentResult } from "./WritingAssessmentResult";

interface SuggestionModeViewProps {
  session: WritingSessionResponse;
  onOpenDictionary: (word: string) => void;
  onSaveDraft: (draft: string) => Promise<void>;
  savingDraft: boolean;
  onSwitchToGuidance: () => void;
}

export function SuggestionModeView({
  session,
  onOpenDictionary,
  onSaveDraft,
  savingDraft,
  onSwitchToGuidance,
}: SuggestionModeViewProps) {
  const { settings } = useUserSettings();
  const isEn = settings.uiLanguage === "en";
  const [draftText, setDraftText] = useState(session.userDraft ?? "");
  const [revealAnswers, setRevealAnswers] = useState(false);
  const [copiedCorrected, setCopiedCorrected] = useState(false);
  const { playingAudioKey, loadingAudioKey, playWritingAudio } =
    useWritingAudio();

  const handleCopyCorrected = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedCorrected(true);
      setTimeout(() => setCopiedCorrected(false), 2000);
    } catch {
      // Ignore clipboard permission errors
    }
  };

  const insertTokenIntoDraft = (token: string) => {
    setDraftText((prev) => {
      const trimmed = prev.trimEnd();
      if (!trimmed) return token;
      return `${trimmed} ${token}`;
    });
  };

  const renderLexicalCard = (item: WritingLexicalItem, idx: number) => {
    const cardKey = item.id || `${item.itemType}-${idx}-${item.text}`;

    return (
      <div
        key={cardKey}
        className="flex flex-col justify-between rounded-xl border border-ink-200/80 dark:border-slate-700 bg-white dark:bg-slate-900 p-3.5 shadow-2xs hover:border-brand-300 dark:hover:border-brand-500/60 transition-all space-y-2.5"
      >
        <div className="space-y-1">
          <div className="flex items-start justify-between gap-2">
            <div className="flex flex-col gap-1 min-w-0">
              <div className="flex items-center gap-1.5 flex-wrap">
                <span className="text-sm sm:text-base font-extrabold text-ink-900 dark:text-slate-100">
                  {item.text}
                </span>
                <AccentAudioButtons
                  idPrefix={`lex-${cardKey}`}
                  text={item.text}
                  usAudioUrl={item.audioUsUrl || item.audioUrl}
                  ukAudioUrl={item.audioUkUrl}
                  playingAudioKey={playingAudioKey}
                  loadingAudioKey={loadingAudioKey}
                  onPlay={playWritingAudio}
                  size="sm"
                />
              </div>

              {/* Phonetic / Phiên âm IPA */}
              <PhoneticBadge phonetic={item.phonetic} size="sm" />
            </div>

            <div className="flex items-center gap-1 shrink-0">
              {item.partOfSpeech && (
                <span className="rounded-md bg-ink-100 dark:bg-slate-800 px-1.5 py-0.5 text-[10px] font-bold uppercase text-ink-600 dark:text-slate-300">
                  {item.partOfSpeech}
                </span>
              )}
              {item.cefrLevel && (
                <span className="rounded-md bg-brand-50 dark:bg-emerald-950/50 border border-brand-200/80 dark:border-brand-500/40 px-1.5 py-0.5 text-[10px] font-bold text-brand-700 dark:text-brand-300">
                  {item.cefrLevel}
                </span>
              )}
            </div>
          </div>

        {item.meaningVn && (
          <p className="text-xs sm:text-sm font-semibold text-brand-700 dark:text-brand-300">
            {item.meaningVn}
          </p>
        )}

        {item.usageContextVn && (
          <p className="text-[11px] text-ink-500 dark:text-slate-400 leading-relaxed">
            {item.usageContextVn}
          </p>
        )}
      </div>

      <div className="flex items-center justify-between gap-2 pt-2 border-t border-ink-100 dark:border-slate-800">
        <button
          type="button"
          onClick={() => insertTokenIntoDraft(item.text)}
          className="inline-flex items-center gap-1 text-xs font-bold text-brand-700 dark:text-brand-300 hover:text-brand-800 dark:hover:text-brand-200 transition-colors cursor-pointer"
        >
          <PlusCircle className="h-3.5 w-3.5" aria-hidden="true" />
          <span>Chèn vào câu nháp</span>
        </button>

        {item.inDictionary && item.dictionaryWord && (
          <button
            type="button"
            onClick={() => onOpenDictionary(item.dictionaryWord!)}
            className="inline-flex items-center gap-1 rounded-lg bg-ocean-50 dark:bg-sky-950/50 border border-ocean-100 dark:border-sky-800/60 px-2 py-0.5 text-[11px] font-bold text-ocean-600 dark:text-sky-300 hover:bg-ocean-100 dark:hover:bg-sky-900/60 transition-colors cursor-pointer"
          >
            <BookOpen className="h-3 w-3" aria-hidden="true" />
            <span>Từ điển</span>
          </button>
        )}
      </div>
    </div>
  );
};

  return (
    <div className="space-y-5 animate-fade-up">
      {/* ── Core Idea Banner ── */}
      {session.coreIdeaSummaryVn && (
        <div className="rounded-2xl border border-brand-200/70 dark:border-brand-500/30 bg-brand-50/70 dark:bg-emerald-950/30 px-4 py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-2.5">
          <div className="space-y-0.5">
            <div className="flex items-center gap-1.5 text-[11px] font-extrabold uppercase tracking-wider text-brand-700 dark:text-brand-300">
              <Sparkles className="h-3.5 w-3.5" aria-hidden="true" />
              <span>Ý chính ({session.scaleType} {session.targetLevel})</span>
            </div>
            <p className="text-sm font-semibold text-ink-900 dark:text-slate-100">
              {session.coreIdeaSummaryVn}
            </p>
          </div>

          <button
            type="button"
            onClick={onSwitchToGuidance}
            className="self-start sm:self-center inline-flex items-center gap-1.5 rounded-xl border border-brand-300 dark:border-brand-500/50 bg-white dark:bg-slate-800 px-3 py-1.5 text-xs font-bold text-brand-700 dark:text-brand-300 hover:bg-brand-50 dark:hover:bg-slate-700 transition-colors shrink-0 cursor-pointer"
          >
            <span>Xem Hướng dẫn &amp; Câu mẫu</span>
            <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
          </button>
        </div>
      )}

      {/* ── 1. Vocabulary & Expressions ── */}
      <section className="space-y-2.5">
        <div className="flex items-center justify-between">
          <h3 className="text-sm sm:text-base font-extrabold text-ink-900 dark:text-slate-100 flex items-center gap-2">
            <span className="flex h-6 w-6 items-center justify-center rounded-lg bg-brand-100 dark:bg-emerald-950/70 text-xs font-black text-brand-700 dark:text-emerald-300">
              1
            </span>
            <span>Từ vựng &amp; Expressions gợi ý</span>
          </h3>
          <span className="text-[11px] text-ink-400 dark:text-slate-400 hidden sm:inline">
            Nhấn &ldquo;Chèn vào câu nháp&rdquo; để ghép câu nhanh
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {session.vocabulary.map((item, idx) => renderLexicalCard(item, idx))}
          {session.expressions.map((item, idx) => renderLexicalCard(item, idx))}
        </div>
      </section>

      {/* ── 2. Collocations ── */}
      {session.collocations.length > 0 && (
        <section className="space-y-2.5">
          <h3 className="text-sm sm:text-base font-extrabold text-ink-900 dark:text-slate-100 flex items-center gap-2">
            <span className="flex h-6 w-6 items-center justify-center rounded-lg bg-honey-100 dark:bg-amber-950/70 text-xs font-black text-honey-600 dark:text-amber-300">
              2
            </span>
            <span>Cụm từ cố định (Collocations)</span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
            {session.collocations.map((col, idx) => {
              const colKey = col.id || `col-${idx}-${col.text}`;
              return (
                <div
                  key={colKey}
                  className="flex flex-col justify-between gap-2.5 rounded-xl border border-honey-100 dark:border-amber-800/50 bg-honey-50/50 dark:bg-amber-950/25 p-3"
                >
                  <div className="space-y-1 min-w-0">
                    <div className="flex items-start justify-between gap-2 flex-wrap">
                      <span className="text-sm font-extrabold text-ink-900 dark:text-slate-100 leading-snug">
                        {col.text}
                      </span>
                      <AccentAudioButtons
                        idPrefix={`col-${colKey}`}
                        text={col.text}
                        usAudioUrl={col.audioUsUrl || col.audioUrl}
                        ukAudioUrl={col.audioUkUrl}
                        playingAudioKey={playingAudioKey}
                        loadingAudioKey={loadingAudioKey}
                        onPlay={playWritingAudio}
                        size="sm"
                      />
                    </div>
                    <PhoneticBadge phonetic={col.phonetic} size="sm" />
                    {col.meaningVn && (
                      <p className="text-xs font-semibold text-honey-600 dark:text-amber-300">
                        {col.meaningVn}
                      </p>
                    )}
                  </div>

                  <div className="flex items-center justify-between gap-2 pt-1.5 border-t border-honey-100/80 dark:border-amber-800/40">
                    <button
                      type="button"
                      onClick={() => insertTokenIntoDraft(col.text)}
                      className="inline-flex items-center gap-1 rounded-lg bg-white dark:bg-slate-800 border border-honey-200 dark:border-amber-700/60 px-2.5 py-1 text-xs font-bold text-ink-700 dark:text-slate-200 hover:border-brand-400 hover:text-brand-700 dark:hover:text-brand-300 transition-colors cursor-pointer"
                    >
                      <PlusCircle className="h-3 w-3" aria-hidden="true" />
                      <span>Chèn</span>
                    </button>

                    {col.inDictionary && col.dictionaryWord && (
                      <button
                        type="button"
                        onClick={() => onOpenDictionary(col.dictionaryWord!)}
                        className="inline-flex items-center gap-1 rounded-lg bg-ocean-50 dark:bg-sky-950/50 border border-ocean-100 dark:border-sky-800/60 px-2 py-0.5 text-[11px] font-bold text-ocean-600 dark:text-sky-300 hover:bg-ocean-100 dark:hover:bg-sky-900/60 transition-colors cursor-pointer"
                      >
                        <BookOpen className="h-3 w-3" aria-hidden="true" />
                        <span>Từ điển</span>
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* ── 3. Sentence Patterns ── */}
      {session.sentencePatterns.length > 0 && (
        <section className="space-y-2.5">
          <h3 className="text-sm sm:text-base font-extrabold text-ink-900 dark:text-slate-100 flex items-center gap-2">
            <span className="flex h-6 w-6 items-center justify-center rounded-lg bg-ocean-100 dark:bg-sky-950/70 text-xs font-black text-ocean-600 dark:text-sky-300">
              3
            </span>
            <span>Khung cấu trúc câu (Sentence Patterns)</span>
          </h3>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {session.sentencePatterns.map((pat, idx) => (
              <div
                key={pat.id || idx}
                className="rounded-xl border border-ocean-100 dark:border-sky-800/50 bg-ocean-50/40 dark:bg-sky-950/25 p-3.5 space-y-1.5"
              >
                <div className="flex items-center gap-2">
                  <Layers className="h-4 w-4 text-ocean-600 dark:text-sky-400 shrink-0" aria-hidden="true" />
                  <code className="text-xs font-extrabold text-ink-900 dark:text-slate-100 bg-white dark:bg-slate-800 px-2 py-0.5 rounded-md border border-ocean-100 dark:border-sky-800/60">
                    {pat.pattern}
                  </code>
                </div>
                {pat.meaningVn && (
                  <p className="text-xs font-semibold text-ocean-600 dark:text-sky-300">
                    Ý nghĩa: {pat.meaningVn}
                  </p>
                )}
                {pat.usageContextVn && (
                  <p className="text-[11px] text-ink-500 dark:text-slate-400">
                    {pat.usageContextVn}
                  </p>
                )}
              </div>
            ))}
          </div>
        </section>
      )}

      {/* ── 4. Interactive Learner Sentence Builder ── */}
      <section className="rounded-2xl border-2 border-brand-300 dark:border-brand-500/40 bg-white dark:bg-slate-900 p-4 sm:p-5 shadow-card space-y-3">
        <div className="flex items-center gap-2.5">
          <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-brand-600 text-white shrink-0">
            <PenLine className="h-4 w-4" aria-hidden="true" />
          </span>
          <div>
            <h3 className="text-sm sm:text-base font-extrabold text-ink-900 dark:text-slate-100">
              Tự xây dựng câu tiếng Anh của bạn
            </h3>
            <p className="text-xs text-ink-500 dark:text-slate-400">
              Kết hợp từ vựng, collocations và cấu trúc câu ở trên để tự viết câu của bạn.
            </p>
          </div>
        </div>

        <textarea
          rows={4}
          maxLength={4000}
          value={draftText}
          onChange={(e) => setDraftText(e.target.value)}
          placeholder={
            isEn
              ? "Write your own English sentence or paragraph here..."
              : "Nhập câu hoặc đoạn văn tiếng Anh bạn tự viết tại đây..."
          }
          aria-label={
            isEn
              ? "Write your own English sentence or paragraph"
              : "Nhập câu hoặc đoạn văn tiếng Anh bạn tự xây dựng"
          }
          className="w-full rounded-xl border border-ink-200 dark:border-slate-700 bg-ink-50/40 dark:bg-slate-800/80 px-3.5 py-2.5 text-sm sm:text-base font-medium text-ink-900 dark:text-slate-100 placeholder:text-ink-400 dark:placeholder:text-slate-500 focus:border-brand-500 focus:bg-white dark:focus:bg-slate-800 focus:outline-none focus:ring-3 focus:ring-brand-500/15 transition-all"
        />

        {draftText.trim().length > 0 && draftText.trim().length < 3 && (
          <p className="text-[11px] font-medium text-amber-700 dark:text-amber-300">
            {isEn
              ? "Please enter at least 3 characters before checking."
              : "Vui lòng nhập tối thiểu 3 ký tự để kiểm tra."}
          </p>
        )}

        {session.draftValidation && (
          <WritingAssessmentResult
            validation={session.draftValidation}
            userDraft={session.userDraft}
            isEn={isEn}
            copiedCorrected={copiedCorrected}
            onCopyCorrected={(text) => void handleCopyCorrected(text)}
            onApplyCorrected={(text) => setDraftText(text)}
            onInsertTerm={insertTokenIntoDraft}
          />
        )}

        <div className="flex flex-wrap items-center justify-between gap-2.5">
          {session.completeSentences.length > 0 ? (
            <button
              type="button"
              onClick={() => setRevealAnswers((prev) => !prev)}
              className="inline-flex items-center gap-1.5 rounded-xl border border-ink-200 dark:border-slate-700 bg-ink-50/70 dark:bg-slate-800 px-3.5 py-2 text-xs font-bold text-ink-700 dark:text-slate-200 hover:bg-ink-100 dark:hover:bg-slate-700 transition-colors cursor-pointer"
            >
              <Eye className="h-3.5 w-3.5 text-brand-600 dark:text-brand-400" aria-hidden="true" />
              <span>
                {revealAnswers
                  ? "Ẩn câu tham khảo hoàn chỉnh"
                  : "Mở khóa câu tham khảo hoàn chỉnh"}
              </span>
            </button>
          ) : (
            <button
              type="button"
              onClick={onSwitchToGuidance}
              className="inline-flex items-center gap-1.5 rounded-xl border border-brand-200 dark:border-brand-500/40 bg-brand-50/60 dark:bg-emerald-950/40 px-3.5 py-2 text-xs font-bold text-brand-700 dark:text-brand-300 hover:bg-brand-100/80 dark:hover:bg-emerald-950/60 transition-colors cursor-pointer"
            >
              <BookOpenCheck className="h-3.5 w-3.5 text-brand-600 dark:text-brand-400" aria-hidden="true" />
              <span>Xem giải thích &amp; câu hoàn chỉnh (Guidance Mode)</span>
            </button>
          )}

          <button
            type="button"
            disabled={savingDraft || draftText.trim().length < 3}
            onClick={() => onSaveDraft(draftText)}
            className="inline-flex items-center gap-1.5 rounded-xl bg-brand-600 px-4 py-2 text-xs sm:text-sm font-bold text-white hover:bg-brand-700 transition-colors cursor-pointer disabled:opacity-50"
          >
            <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
            <span>{savingDraft ? "Đang kiểm tra..." : "Lưu & Kiểm tra câu của tôi"}</span>
          </button>
        </div>

        {/* Optional Revealed Complete Sentences */}
        {revealAnswers && session.completeSentences.length > 0 && (
          <div className="mt-3 pt-3 border-t border-ink-100 dark:border-slate-800 space-y-2.5">
            <div className="text-xs font-extrabold uppercase tracking-wider text-brand-700 dark:text-brand-300">
              Đáp án tham khảo ({session.scaleType} {session.targetLevel})
            </div>
            {session.completeSentences.map((sent, idx) => {
              const sentKey = sent.id || `ref-${idx}`;
              return (
                <div
                  key={sentKey}
                  className="rounded-xl border border-brand-200/80 dark:border-brand-500/35 bg-brand-50/40 dark:bg-emerald-950/25 p-3.5 space-y-1.5"
                >
                  <div className="flex items-center justify-between gap-2 flex-wrap">
                    <span className="rounded-md bg-brand-100 dark:bg-emerald-950/70 px-2 py-0.5 text-[10px] font-bold text-brand-800 dark:text-emerald-200">
                      {sent.tone || "Reference"}
                    </span>
                    <AccentAudioButtons
                      idPrefix={`ref-sent-${sentKey}`}
                      text={sent.sentenceEn}
                      usAudioUrl={sent.audioUsUrl}
                      ukAudioUrl={sent.audioUkUrl}
                      playingAudioKey={playingAudioKey}
                      loadingAudioKey={loadingAudioKey}
                      onPlay={playWritingAudio}
                      size="sm"
                    />
                  </div>
                  <p className="text-sm sm:text-base font-extrabold text-ink-900 dark:text-slate-100">
                    &ldquo;{sent.sentenceEn}&rdquo;
                  </p>
                  {sent.translationVn && (
                    <p className="text-xs text-ink-600 dark:text-slate-300">{sent.translationVn}</p>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
