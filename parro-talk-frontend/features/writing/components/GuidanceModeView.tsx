"use client";

import React, { useState } from "react";
import {
  BookOpen,
  Check,
  CheckCircle2,
  Compass,
  Copy,
  Layers,
  Sparkles,
  Volume2,
} from "lucide-react";
import type { WritingLexicalItem, WritingSessionResponse } from "../types";

interface GuidanceModeViewProps {
  session: WritingSessionResponse;
  onOpenDictionary: (word: string) => void;
}

export function GuidanceModeView({
  session,
  onOpenDictionary,
}: GuidanceModeViewProps) {
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const speakText = (text: string) => {
    if (typeof window === "undefined" || !("speechSynthesis" in window)) return;
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = "en-US";
    utterance.rate = 0.95;
    window.speechSynthesis.speak(utterance);
  };

  const copySentence = async (id: string, text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedId(id);
      setTimeout(() => setCopiedId(null), 2000);
    } catch {
      // ignore clipboard errors
    }
  };

  const renderDetailedLexicalCard = (item: WritingLexicalItem) => (
    <article
      key={item.id || item.text}
      className="rounded-2xl border border-ink-200/80 bg-white p-5 shadow-2xs space-y-3"
    >
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2 flex-wrap">
          <h4 className="text-base sm:text-lg font-extrabold text-ink-900">
            {item.text}
          </h4>
          <button
            type="button"
            onClick={() => speakText(item.text)}
            className="rounded-lg p-1 text-ink-400 hover:bg-brand-50 hover:text-brand-700 transition-colors cursor-pointer"
            aria-label={`Nghe phát âm ${item.text}`}
          >
            <Volume2 className="h-4 w-4" aria-hidden="true" />
          </button>
        </div>

        <div className="flex items-center gap-1.5 shrink-0">
          {item.partOfSpeech && (
            <span className="rounded-md bg-ink-100 px-2 py-0.5 text-[10px] font-bold uppercase text-ink-600">
              {item.partOfSpeech}
            </span>
          )}
          {item.cefrLevel && (
            <span className="rounded-md bg-brand-50 border border-brand-200/80 px-1.5 py-0.5 text-[10px] font-bold text-brand-700">
              {item.cefrLevel}
            </span>
          )}
          {item.inDictionary && item.dictionaryWord && (
            <button
              type="button"
              onClick={() => onOpenDictionary(item.dictionaryWord!)}
              className="inline-flex items-center gap-1 rounded-lg bg-ocean-50 border border-ocean-100 px-2 py-0.5 text-[10px] font-bold text-ocean-600 hover:bg-ocean-100 transition-colors cursor-pointer"
            >
              <BookOpen className="h-3 w-3" aria-hidden="true" />
              <span>Từ điển</span>
            </button>
          )}
        </div>
      </div>

      {item.meaningVn && (
        <p className="text-sm font-bold text-brand-700">{item.meaningVn}</p>
      )}

      {item.explanationVn && (
        <p className="text-xs sm:text-sm text-ink-700 leading-relaxed">
          <span className="font-bold text-ink-900">Giải thích: </span>
          {item.explanationVn}
        </p>
      )}

      {item.usageContextVn && (
        <p className="text-xs text-ink-500 italic">
          Ngữ cảnh &amp; Cách dùng: {item.usageContextVn}
        </p>
      )}

      {item.exampleEn && (
        <div className="rounded-xl bg-ink-50/70 border border-ink-100 p-3 space-y-1">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-extrabold uppercase tracking-wider text-brand-700">
              Ví dụ minh họa
            </span>
            <button
              type="button"
              onClick={() => speakText(item.exampleEn!)}
              className="text-[11px] font-semibold text-brand-700 hover:text-brand-800 inline-flex items-center gap-1 cursor-pointer"
            >
              <Volume2 className="h-3 w-3" aria-hidden="true" />
              <span>Nghe</span>
            </button>
          </div>
          <p className="text-xs sm:text-sm font-semibold text-ink-900">
            &ldquo;{item.exampleEn}&rdquo;
          </p>
          {item.exampleVn && (
            <p className="text-xs text-ink-500">{item.exampleVn}</p>
          )}
        </div>
      )}
    </article>
  );

  return (
    <div className="space-y-7 animate-fade-up">
      {/* ── Complete English Reference Sentences (Hero Answer Section) ── */}
      <section className="rounded-3xl border-2 border-brand-300 bg-gradient-to-br from-brand-50/90 via-white to-brand-50/40 p-5 sm:p-7 shadow-card space-y-4">
        <div className="flex items-center justify-between flex-wrap gap-2">
          <div className="flex items-center gap-2.5">
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-600 text-white">
              <Sparkles className="h-5 w-5" aria-hidden="true" />
            </span>
            <div>
              <h3 className="text-base sm:text-lg font-extrabold text-ink-900">
                Câu tiếng Anh hoàn chỉnh &amp; Giải thích đáp án
              </h3>
              <p className="text-xs text-ink-500">
                Được điều chỉnh từ vựng và cấu trúc ngữ pháp theo đúng trình độ{" "}
                <span className="font-bold text-brand-700">
                  {session.scaleType} {session.targetLevel}
                </span>
              </p>
            </div>
          </div>
        </div>

        <div className="space-y-4">
          {session.completeSentences.map((sent, idx) => {
            const key = sent.id || String(idx);
            return (
              <div
                key={key}
                className="rounded-2xl border border-brand-200/90 bg-white p-5 shadow-2xs space-y-3"
              >
                <div className="flex items-center justify-between gap-2 flex-wrap">
                  <div className="flex items-center gap-2">
                    <span className="rounded-lg bg-brand-100 px-2.5 py-1 text-xs font-extrabold text-brand-800">
                      Phương án {idx + 1}: {sent.tone || "Natural"}
                    </span>
                    <span className="inline-flex items-center gap-1 rounded-md bg-emerald-50 border border-emerald-200 px-2 py-0.5 text-[10px] font-bold text-emerald-700">
                      <CheckCircle2 className="h-3 w-3" aria-hidden="true" />
                      {sent.validationStatus}
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => speakText(sent.sentenceEn)}
                      className="inline-flex items-center gap-1 rounded-xl border border-brand-200 bg-brand-50 px-3 py-1.5 text-xs font-bold text-brand-700 hover:bg-brand-100 transition-colors cursor-pointer"
                    >
                      <Volume2 className="h-3.5 w-3.5" aria-hidden="true" />
                      <span>Nghe câu</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => copySentence(key, sent.sentenceEn)}
                      className="inline-flex items-center gap-1 rounded-xl border border-ink-200 bg-white px-3 py-1.5 text-xs font-bold text-ink-700 hover:bg-ink-50 transition-colors cursor-pointer"
                    >
                      {copiedId === key ? (
                        <>
                          <Check className="h-3.5 w-3.5 text-brand-600" aria-hidden="true" />
                          <span>Đã chép</span>
                        </>
                      ) : (
                        <>
                          <Copy className="h-3.5 w-3.5" aria-hidden="true" />
                          <span>Sao chép</span>
                        </>
                      )}
                    </button>
                  </div>
                </div>

                <p className="text-base sm:text-lg font-extrabold text-ink-900 leading-relaxed">
                  &ldquo;{sent.sentenceEn}&rdquo;
                </p>

                {sent.translationVn && (
                  <p className="text-sm font-medium text-ocean-600">
                    Dịch nghĩa: {sent.translationVn}
                  </p>
                )}

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-2 border-t border-ink-100 text-xs">
                  {sent.grammarBreakdownVn && (
                    <div className="rounded-xl bg-ink-50/70 p-3 space-y-1">
                      <div className="font-extrabold uppercase tracking-wider text-ink-600 text-[10px]">
                        Phân tích cấu trúc ngữ pháp
                      </div>
                      <p className="text-ink-700 leading-relaxed">
                        {sent.grammarBreakdownVn}
                      </p>
                    </div>
                  )}

                  {sent.whyItWorksVn && (
                    <div className="rounded-xl bg-brand-50/60 p-3 space-y-1">
                      <div className="font-extrabold uppercase tracking-wider text-brand-700 text-[10px]">
                        Vì sao cách diễn đạt này tự nhiên?
                      </div>
                      <p className="text-ink-700 leading-relaxed">
                        {sent.whyItWorksVn}
                      </p>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* ── Step-by-Step Combination Guide ── */}
      {session.combinationGuideVn && (
        <section className="rounded-3xl border border-ocean-200/80 bg-ocean-50/50 p-5 sm:p-6 space-y-2.5">
          <div className="flex items-center gap-2 text-ocean-600 font-extrabold text-sm sm:text-base">
            <Compass className="h-5 w-5 shrink-0" aria-hidden="true" />
            <h3>Hướng dẫn kết hợp các thành phần câu</h3>
          </div>
          <p className="text-xs sm:text-sm text-ink-800 whitespace-pre-line leading-relaxed">
            {session.combinationGuideVn}
          </p>
        </section>
      )}

      {/* ── Detailed Vocabulary & Expressions ── */}
      <section className="space-y-3">
        <h3 className="text-base sm:text-lg font-extrabold text-ink-900">
          Giải thích chi tiết Từ vựng &amp; Expressions
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {session.vocabulary.map(renderDetailedLexicalCard)}
          {session.expressions.map(renderDetailedLexicalCard)}
        </div>
      </section>

      {/* ── Detailed Collocations & Sentence Patterns ── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {session.collocations.length > 0 && (
          <section className="space-y-3">
            <h3 className="text-base font-extrabold text-ink-900">
              Collocations (Cụm từ đi kèm tự nhiên)
            </h3>
            <div className="space-y-3">
              {session.collocations.map(renderDetailedLexicalCard)}
            </div>
          </section>
        )}

        {session.sentencePatterns.length > 0 && (
          <section className="space-y-3">
            <h3 className="text-base font-extrabold text-ink-900">
              Cấu trúc ngữ pháp (Sentence Patterns)
            </h3>
            <div className="space-y-3">
              {session.sentencePatterns.map((pat, idx) => (
                <div
                  key={pat.id || idx}
                  className="rounded-2xl border border-ink-200/80 bg-white p-5 shadow-2xs space-y-2"
                >
                  <div className="flex items-center gap-2">
                    <Layers className="h-4 w-4 text-brand-600 shrink-0" aria-hidden="true" />
                    <code className="text-xs sm:text-sm font-extrabold text-brand-800 bg-brand-50 px-2.5 py-1 rounded-lg border border-brand-200/70">
                      {pat.pattern}
                    </code>
                  </div>
                  {pat.meaningVn && (
                    <p className="text-sm font-bold text-ink-900">
                      {pat.meaningVn}
                    </p>
                  )}
                  {pat.explanationVn && (
                    <p className="text-xs sm:text-sm text-ink-600 leading-relaxed">
                      {pat.explanationVn}
                    </p>
                  )}
                  {pat.usageContextVn && (
                    <p className="text-xs text-ink-500 italic">
                      Lưu ý: {pat.usageContextVn}
                    </p>
                  )}
                </div>
              ))}
            </div>
          </section>
        )}
      </div>
    </div>
  );
}

