"use client";

import { useState } from "react";
import { ArrowRight, CheckCircle2, Eye, RotateCcw } from "lucide-react";
import { tokenizeText } from "@/lib/tokenizer";

type TranslationReviewProps = {
  sentence: string;
  translatedText?: string | null;
  showTranslationByDefault?: boolean;
  onContinue: () => void;
  onWordClick?: (word: string) => void;
  isLastSegment?: boolean;
  uncompletedCount?: number;
  onSubmitLesson?: () => void;
  onReviewSkipped?: () => void;
};

export default function TranslationReview({
  sentence,
  translatedText,
  showTranslationByDefault = true,
  onContinue,
  onWordClick,
  isLastSegment = false,
  uncompletedCount = 0,
  onSubmitLesson,
  onReviewSkipped,
}: TranslationReviewProps) {
  const [revealedOverride, setRevealedOverride] = useState<boolean | null>(
    null
  );
  const showTranslation = revealedOverride ?? showTranslationByDefault;
  const renderSentence = (text: string) => {
    return tokenizeText(text).map((token, i) => {
      if (token.isWord) {
        return (
          <span 
            key={i} 
            onClick={() => onWordClick?.(token.text)}
            className="cursor-pointer hover:text-emerald-600 transition-colors border-b border-transparent hover:border-emerald-600 pb-0.5"
          >
            {token.text}
          </span>
        );
      }
      return <span key={i}>{token.text}</span>;
    });
  };

  return (
    <section className="w-full border-t border-gray-100 dark:border-slate-800 bg-white dark:bg-slate-900 px-4 py-4 shadow-[0_-8px_30px_rgba(0,0,0,0.04)] sm:px-6 sm:py-6">
      <div className="mx-auto flex max-w-4xl flex-col gap-4">
        <div>
          <p className="text-sm font-semibold text-gray-400 dark:text-slate-400">Completed sentence</p>
          <p className="mt-1 text-base font-bold text-gray-900 dark:text-slate-100 sm:text-lg">
            {renderSentence(sentence)}
          </p>
        </div>

        {showTranslation ? (
          <div className="rounded-md border border-emerald-100 dark:border-emerald-800/80 bg-emerald-50 dark:bg-emerald-950/50 px-4 py-3">
            <p className="text-sm font-semibold text-emerald-700 dark:text-emerald-300">Vietnamese translation</p>
            <p className="mt-1 text-base font-medium text-emerald-950 dark:text-emerald-100">
              {translatedText || "Translation is not available yet."}
            </p>
          </div>
        ) : (
          <div>
            <button
              type="button"
              onClick={() => setRevealedOverride(true)}
              className="inline-flex items-center gap-2 rounded-lg border border-emerald-200 dark:border-emerald-800 bg-emerald-50/70 dark:bg-emerald-950/60 px-3.5 py-2 text-xs font-bold text-emerald-700 dark:text-emerald-300 hover:bg-emerald-100 dark:hover:bg-emerald-900/60 transition-colors cursor-pointer"
            >
              <Eye size={14} />
              <span>Hiện bản dịch</span>
            </button>
          </div>
        )}

        {isLastSegment ? (
          uncompletedCount > 0 ? (
            <div className="flex flex-col sm:flex-row gap-3">
              <button
                type="button"
                onClick={onSubmitLesson || onContinue}
                className="flex-1 inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-emerald-500 px-5 py-2.5 font-bold text-white shadow-md hover:bg-emerald-600 active:scale-[0.98] transition-all cursor-pointer"
              >
                Submit Lesson
                <CheckCircle2 className="h-4 w-4" />
              </button>
              {onReviewSkipped && (
                <button
                  type="button"
                  onClick={onReviewSkipped}
                  className="flex-1 inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border-2 border-amber-300 bg-amber-50 px-5 py-2.5 font-bold text-amber-900 hover:bg-amber-100 active:scale-[0.98] transition-all cursor-pointer"
                >
                  <RotateCcw className="h-4 w-4" />
                  Review Skipped ({uncompletedCount})
                </button>
              )}
            </div>
          ) : (
            <button
              type="button"
              onClick={onSubmitLesson || onContinue}
              className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-emerald-500 px-5 py-2.5 font-bold text-white shadow-md hover:bg-emerald-600 active:scale-[0.98] transition-all cursor-pointer"
            >
              Complete & Submit Lesson
              <CheckCircle2 className="h-4 w-4" />
            </button>
          )
        ) : (
          <button
            type="button"
            onClick={onContinue}
            className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-emerald-500 px-4 py-2 font-semibold text-white transition hover:bg-emerald-600 active:scale-[0.98] cursor-pointer"
          >
            Continue
            <ArrowRight className="h-4 w-4" />
          </button>
        )}
      </div>
    </section>
  );
}
