"use client";

import React, { useState } from "react";
import {
  BookOpen,
  BookOpenCheck,
  CheckCircle2,
  Eye,
  Layers,
  PenLine,
  PlusCircle,
  Sparkles,
  Volume2,
  AlertTriangle,
  ArrowRight,
} from "lucide-react";
import type { WritingLexicalItem, WritingSessionResponse } from "../types";

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
  const [draftText, setDraftText] = useState(session.userDraft ?? "");
  const [revealAnswers, setRevealAnswers] = useState(false);

  const speakText = (text: string) => {
    if (typeof window === "undefined" || !("speechSynthesis" in window)) return;
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = "en-US";
    utterance.rate = 0.95;
    window.speechSynthesis.speak(utterance);
  };

  const insertTokenIntoDraft = (token: string) => {
    setDraftText((prev) => {
      const trimmed = prev.trimEnd();
      if (!trimmed) return token;
      return `${trimmed} ${token}`;
    });
  };

  const renderLexicalCard = (item: WritingLexicalItem) => (
    <div
      key={item.id || item.text}
      className="flex flex-col justify-between rounded-xl border border-ink-200/80 bg-white p-3.5 shadow-2xs hover:border-brand-300 transition-all space-y-2.5"
    >
      <div className="space-y-1">
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="text-sm sm:text-base font-extrabold text-ink-900">
              {item.text}
            </span>
            <button
              type="button"
              onClick={() => speakText(item.text)}
              className="rounded-lg p-1 text-ink-400 hover:bg-brand-50 hover:text-brand-700 transition-colors cursor-pointer"
              aria-label={`Nghe phát âm ${item.text}`}
              title="Nghe phát âm"
            >
              <Volume2 className="h-3.5 w-3.5" aria-hidden="true" />
            </button>
          </div>

          <div className="flex items-center gap-1 shrink-0">
            {item.partOfSpeech && (
              <span className="rounded-md bg-ink-100 px-1.5 py-0.5 text-[10px] font-bold uppercase text-ink-600">
                {item.partOfSpeech}
              </span>
            )}
            {item.cefrLevel && (
              <span className="rounded-md bg-brand-50 border border-brand-200/80 px-1.5 py-0.5 text-[10px] font-bold text-brand-700">
                {item.cefrLevel}
              </span>
            )}
          </div>
        </div>

        {item.meaningVn && (
          <p className="text-xs sm:text-sm font-semibold text-brand-700">
            {item.meaningVn}
          </p>
        )}

        {item.usageContextVn && (
          <p className="text-[11px] text-ink-500 leading-relaxed">
            {item.usageContextVn}
          </p>
        )}
      </div>

      <div className="flex items-center justify-between gap-2 pt-2 border-t border-ink-100">
        <button
          type="button"
          onClick={() => insertTokenIntoDraft(item.text)}
          className="inline-flex items-center gap-1 text-xs font-bold text-brand-700 hover:text-brand-800 transition-colors cursor-pointer"
        >
          <PlusCircle className="h-3.5 w-3.5" aria-hidden="true" />
          <span>Chèn vào câu nháp</span>
        </button>

        {item.inDictionary && item.dictionaryWord && (
          <button
            type="button"
            onClick={() => onOpenDictionary(item.dictionaryWord!)}
            className="inline-flex items-center gap-1 rounded-lg bg-ocean-50 border border-ocean-100 px-2 py-0.5 text-[11px] font-bold text-ocean-600 hover:bg-ocean-100 transition-colors cursor-pointer"
          >
            <BookOpen className="h-3 w-3" aria-hidden="true" />
            <span>Từ điển</span>
          </button>
        )}
      </div>
    </div>
  );

  return (
    <div className="space-y-5 animate-fade-up">
      {/* ── Core Idea Banner ── */}
      {session.coreIdeaSummaryVn && (
        <div className="rounded-2xl border border-brand-200/70 bg-brand-50/70 px-4 py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-2.5">
          <div className="space-y-0.5">
            <div className="flex items-center gap-1.5 text-[11px] font-extrabold uppercase tracking-wider text-brand-700">
              <Sparkles className="h-3.5 w-3.5" aria-hidden="true" />
              <span>Ý chính ({session.scaleType} {session.targetLevel})</span>
            </div>
            <p className="text-sm font-semibold text-ink-900">
              {session.coreIdeaSummaryVn}
            </p>
          </div>

          <button
            type="button"
            onClick={onSwitchToGuidance}
            className="self-start sm:self-center inline-flex items-center gap-1.5 rounded-xl border border-brand-300 bg-white px-3 py-1.5 text-xs font-bold text-brand-700 hover:bg-brand-50 transition-colors shrink-0 cursor-pointer"
          >
            <span>Xem Hướng dẫn &amp; Câu mẫu</span>
            <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
          </button>
        </div>
      )}

      {/* ── 1. Vocabulary & Expressions ── */}
      <section className="space-y-2.5">
        <div className="flex items-center justify-between">
          <h3 className="text-sm sm:text-base font-extrabold text-ink-900 flex items-center gap-2">
            <span className="flex h-6 w-6 items-center justify-center rounded-lg bg-brand-100 text-xs font-black text-brand-700">
              1
            </span>
            <span>Từ vựng &amp; Expressions gợi ý</span>
          </h3>
          <span className="text-[11px] text-ink-400 hidden sm:inline">
            Nhấn &ldquo;Chèn vào câu nháp&rdquo; để ghép câu nhanh
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {session.vocabulary.map(renderLexicalCard)}
          {session.expressions.map(renderLexicalCard)}
        </div>
      </section>

      {/* ── 2. Collocations ── */}
      {session.collocations.length > 0 && (
        <section className="space-y-2.5">
          <h3 className="text-sm sm:text-base font-extrabold text-ink-900 flex items-center gap-2">
            <span className="flex h-6 w-6 items-center justify-center rounded-lg bg-honey-100 text-xs font-black text-honey-600">
              2
            </span>
            <span>Cụm từ cố định (Collocations)</span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
            {session.collocations.map((col) => (
              <div
                key={col.id || col.text}
                className="flex items-center justify-between gap-2.5 rounded-xl border border-honey-100 bg-honey-50/50 p-3"
              >
                <div className="space-y-0.5 min-w-0">
                  <div className="flex items-center gap-1.5">
                    <span className="text-sm font-extrabold text-ink-900 truncate">
                      {col.text}
                    </span>
                    <button
                      type="button"
                      onClick={() => speakText(col.text)}
                      className="text-ink-400 hover:text-brand-700 transition-colors cursor-pointer shrink-0"
                      aria-label={`Nghe ${col.text}`}
                    >
                      <Volume2 className="h-3.5 w-3.5" aria-hidden="true" />
                    </button>
                  </div>
                  {col.meaningVn && (
                    <p className="text-xs font-semibold text-honey-600">
                      {col.meaningVn}
                    </p>
                  )}
                </div>

                <button
                  type="button"
                  onClick={() => insertTokenIntoDraft(col.text)}
                  className="shrink-0 inline-flex items-center gap-1 rounded-lg bg-white border border-honey-200 px-2.5 py-1 text-xs font-bold text-ink-700 hover:border-brand-400 hover:text-brand-700 transition-colors cursor-pointer"
                >
                  <PlusCircle className="h-3 w-3" aria-hidden="true" />
                  <span>Chèn</span>
                </button>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* ── 3. Sentence Patterns ── */}
      {session.sentencePatterns.length > 0 && (
        <section className="space-y-2.5">
          <h3 className="text-sm sm:text-base font-extrabold text-ink-900 flex items-center gap-2">
            <span className="flex h-6 w-6 items-center justify-center rounded-lg bg-ocean-100 text-xs font-black text-ocean-600">
              3
            </span>
            <span>Khung cấu trúc câu (Sentence Patterns)</span>
          </h3>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {session.sentencePatterns.map((pat, idx) => (
              <div
                key={pat.id || idx}
                className="rounded-xl border border-ocean-100 bg-ocean-50/40 p-3.5 space-y-1.5"
              >
                <div className="flex items-center gap-2">
                  <Layers className="h-4 w-4 text-ocean-600 shrink-0" aria-hidden="true" />
                  <code className="text-xs font-extrabold text-ink-900 bg-white px-2 py-0.5 rounded-md border border-ocean-100">
                    {pat.pattern}
                  </code>
                </div>
                {pat.meaningVn && (
                  <p className="text-xs font-semibold text-ocean-600">
                    Ý nghĩa: {pat.meaningVn}
                  </p>
                )}
                {pat.usageContextVn && (
                  <p className="text-[11px] text-ink-500">
                    {pat.usageContextVn}
                  </p>
                )}
              </div>
            ))}
          </div>
        </section>
      )}

      {/* ── 4. Interactive Learner Sentence Builder ── */}
      <section className="rounded-2xl border-2 border-brand-300 bg-white p-4 sm:p-5 shadow-card space-y-3">
        <div className="flex items-center gap-2.5">
          <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-brand-600 text-white shrink-0">
            <PenLine className="h-4 w-4" aria-hidden="true" />
          </span>
          <div>
            <h3 className="text-sm sm:text-base font-extrabold text-ink-900">
              Tự xây dựng câu tiếng Anh của bạn
            </h3>
            <p className="text-xs text-ink-500">
              Kết hợp từ vựng, collocations và cấu trúc câu ở trên để tự viết câu của bạn.
            </p>
          </div>
        </div>

        <textarea
          rows={2}
          value={draftText}
          onChange={(e) => setDraftText(e.target.value)}
          placeholder="Nhập câu tiếng Anh bạn tự ghép tại đây..."
          aria-label="Nhập câu tiếng Anh bạn tự xây dựng"
          className="w-full rounded-xl border border-ink-200 bg-ink-50/40 px-3.5 py-2.5 text-sm sm:text-base font-medium text-ink-900 placeholder:text-ink-400 focus:border-brand-500 focus:bg-white focus:outline-none focus:ring-3 focus:ring-brand-500/15 transition-all"
        />

        {session.draftValidation && (
          <div
            className={`rounded-xl border p-3.5 space-y-1.5 ${
              session.draftValidation.valid
                ? "border-brand-200 bg-brand-50/70 text-brand-900"
                : "border-honey-200 bg-honey-50/70 text-ink-900"
            }`}
          >
            <div className="flex items-center gap-2 text-xs sm:text-sm font-bold">
              {session.draftValidation.valid ? (
                <CheckCircle2 className="h-4 w-4 text-brand-600 shrink-0" aria-hidden="true" />
              ) : (
                <AlertTriangle className="h-4 w-4 text-honey-600 shrink-0" aria-hidden="true" />
              )}
              <span>{session.draftValidation.summaryVn}</span>
            </div>

            {session.draftValidation.issues.length > 0 && (
              <ul className="space-y-1 pl-6 list-disc text-xs text-ink-700">
                {session.draftValidation.issues.map((issue, idx) => (
                  <li key={idx}>
                    <span className="font-semibold">{issue.messageVn}</span>
                    {issue.replacements?.length > 0 && (
                      <span className="text-brand-700 font-bold">
                        {" "}
                        (Gợi ý sửa: {issue.replacements.join(", ")})
                      </span>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        <div className="flex flex-wrap items-center justify-between gap-2.5">
          {session.completeSentences.length > 0 ? (
            <button
              type="button"
              onClick={() => setRevealAnswers((prev) => !prev)}
              className="inline-flex items-center gap-1.5 rounded-xl border border-ink-200 bg-ink-50/70 px-3.5 py-2 text-xs font-bold text-ink-700 hover:bg-ink-100 transition-colors cursor-pointer"
            >
              <Eye className="h-3.5 w-3.5 text-brand-600" aria-hidden="true" />
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
              className="inline-flex items-center gap-1.5 rounded-xl border border-brand-200 bg-brand-50/60 px-3.5 py-2 text-xs font-bold text-brand-700 hover:bg-brand-100/80 transition-colors cursor-pointer"
            >
              <BookOpenCheck className="h-3.5 w-3.5 text-brand-600" aria-hidden="true" />
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
          <div className="mt-3 pt-3 border-t border-ink-100 space-y-2.5">
            <div className="text-xs font-extrabold uppercase tracking-wider text-brand-700">
              Đáp án tham khảo ({session.scaleType} {session.targetLevel})
            </div>
            {session.completeSentences.map((sent, idx) => (
              <div
                key={sent.id || idx}
                className="rounded-xl border border-brand-200/80 bg-brand-50/40 p-3.5 space-y-1"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="rounded-md bg-brand-100 px-2 py-0.5 text-[10px] font-bold text-brand-800">
                    {sent.tone || "Reference"}
                  </span>
                  <button
                    type="button"
                    onClick={() => speakText(sent.sentenceEn)}
                    className="inline-flex items-center gap-1 text-xs font-bold text-brand-700 hover:text-brand-800 cursor-pointer"
                  >
                    <Volume2 className="h-3.5 w-3.5" aria-hidden="true" />
                    <span>Nghe câu</span>
                  </button>
                </div>
                <p className="text-sm sm:text-base font-extrabold text-ink-900">
                  &ldquo;{sent.sentenceEn}&rdquo;
                </p>
                {sent.translationVn && (
                  <p className="text-xs text-ink-600">{sent.translationVn}</p>
                )}
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
