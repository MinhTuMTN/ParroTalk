"use client";

import {
  CheckCircle2,
  Headphones,
  HelpCircle,
  Quote,
  Sparkles,
  XCircle,
} from "lucide-react";
import type { QuizAnswerResult } from "../../types/quiz";
import { formatQuizTimeRange, getGradingStatusMeta } from "./quizUtils";

export interface QuizFeedbackCardProps {
  answerResult: QuizAnswerResult;
  onPlayEvidence?: (startTime: number, endTime: number, text: string) => void;
}

export default function QuizFeedbackCard({
  answerResult,
  onPlayEvidence,
}: QuizFeedbackCardProps) {
  const statusMeta = getGradingStatusMeta(answerResult.status);
  const StatusIcon =
    answerResult.status === "CORRECT"
      ? CheckCircle2
      : answerResult.status === "PARTIAL"
        ? HelpCircle
        : XCircle;

  const hasEvidenceTiming =
    Number.isFinite(answerResult.evidenceStartTime) &&
    Number.isFinite(answerResult.evidenceEndTime) &&
    answerResult.evidenceEndTime >= answerResult.evidenceStartTime;

  const timeRangeLabel = formatQuizTimeRange(
    answerResult.evidenceStartTime,
    answerResult.evidenceEndTime,
  );

  return (
    <div
      className={`rounded-2xl border p-4 sm:p-5 space-y-4 transition-all ${statusMeta.cardClass}`}
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span
            className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-extrabold ${statusMeta.badgeClass}`}
          >
            <StatusIcon className="h-3.5 w-3.5" aria-hidden="true" />
            <span>{statusMeta.label}</span>
          </span>
          <span className="inline-flex items-center rounded-full bg-white/80 dark:bg-slate-900/80 border border-slate-200/80 dark:border-slate-700 px-2.5 py-1 text-xs font-black text-slate-700 dark:text-slate-200">
            {Math.round(answerResult.score)}/100 điểm
          </span>
        </div>
      </div>

      {answerResult.feedback && (
        <div className="space-y-1">
          <p className="text-[11px] font-black uppercase tracking-wider text-slate-500 dark:text-slate-400 flex items-center gap-1.5">
            <Sparkles className="h-3.5 w-3.5 text-emerald-500" aria-hidden="true" />
            <span>Nhận xét AI</span>
          </p>
          <p className="text-sm font-medium text-slate-800 dark:text-slate-100 leading-relaxed">
            {answerResult.feedback}
          </p>
        </div>
      )}

      {answerResult.modelAnswer && (
        <div className="rounded-xl bg-white/85 dark:bg-slate-900/75 border border-slate-200/80 dark:border-slate-800 px-3.5 py-2.5">
          <p className="text-[11px] font-black uppercase tracking-wider text-slate-400 dark:text-slate-500">
            Đáp án mẫu
          </p>
          <p className="mt-1 text-sm font-bold text-emerald-700 dark:text-emerald-300">
            {answerResult.modelAnswer}
          </p>
        </div>
      )}

      {answerResult.explanationVn && (
        <div className="space-y-1">
          <p className="text-[11px] font-black uppercase tracking-wider text-slate-500 dark:text-slate-400">
            Giải thích
          </p>
          <p className="text-sm text-slate-700 dark:text-slate-200 leading-relaxed">
            {answerResult.explanationVn}
          </p>
        </div>
      )}

      {(answerResult.evidenceText || hasEvidenceTiming) && (
        <div className="rounded-2xl border border-emerald-200/80 dark:border-emerald-800/70 bg-white dark:bg-slate-900 p-3.5 sm:p-4 space-y-2.5 shadow-xs">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-1.5 text-[11px] font-black uppercase tracking-wider text-emerald-700 dark:text-emerald-300">
              <Quote className="h-3.5 w-3.5" aria-hidden="true" />
              <span>Trích đoạn dẫn chứng từ bài nghe</span>
            </div>
            {onPlayEvidence && hasEvidenceTiming && (
              <button
                type="button"
                onClick={() =>
                  onPlayEvidence(
                    answerResult.evidenceStartTime,
                    answerResult.evidenceEndTime,
                    answerResult.evidenceText,
                  )
                }
                className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white px-3 py-1.5 text-xs font-bold shadow-xs transition-all active:scale-95 cursor-pointer"
              >
                <Headphones className="h-3.5 w-3.5" aria-hidden="true" />
                <span>Nghe đoạn dẫn chứng ({timeRangeLabel})</span>
              </button>
            )}
          </div>
          {answerResult.evidenceText && (
            <p className="text-sm italic text-slate-700 dark:text-slate-200 leading-relaxed bg-emerald-50/50 dark:bg-emerald-950/30 rounded-xl px-3 py-2 border border-emerald-100 dark:border-emerald-900/50">
              &ldquo;{answerResult.evidenceText}&rdquo;
            </p>
          )}
        </div>
      )}
    </div>
  );
}
