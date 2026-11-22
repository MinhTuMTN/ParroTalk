"use client";

import {
  ArrowLeft,
  Award,
  CheckCircle2,
  Flame,
  Headphones,
  HelpCircle,
  RefreshCw,
  RotateCcw,
  XCircle,
} from "lucide-react";
import type { LessonQuizAttemptResponse } from "../../types/quiz";
import QuizFeedbackCard from "./QuizFeedbackCard";
import {
  QUIZ_MODE_LABELS,
  formatCooldownDuration,
  formatQuizDate,
  formatQuizTimeRange,
  getGradingStatusMeta,
} from "./quizUtils";

export interface QuizSummaryCardProps {
  attempt: LessonQuizAttemptResponse;
  isReviewOnly?: boolean;
  regenerateCooldownSeconds?: number;
  onRetrySameQuiz?: () => void;
  onRegenerateQuiz?: () => void;
  onBackToSetup: () => void;
  onBackToDictation?: () => void;
  onPlayEvidence?: (startTime: number, endTime: number, text: string) => void;
  isBusy?: boolean;
}

export default function QuizSummaryCard({
  attempt,
  isReviewOnly = false,
  regenerateCooldownSeconds = 0,
  onRetrySameQuiz,
  onRegenerateQuiz,
  onBackToSetup,
  onBackToDictation,
  onPlayEvidence,
  isBusy = false,
}: QuizSummaryCardProps) {
  const questions = attempt.questions ?? [];
  const correctCount =
    attempt.correctCount ??
    questions.filter((q) => q.answerResult?.status === "CORRECT").length;
  const partialCount =
    attempt.partialCount ??
    questions.filter((q) => q.answerResult?.status === "PARTIAL").length;
  const incorrectCount = Math.max(
    0,
    questions.length - correctCount - partialCount,
  );
  const overallScore = Math.round(attempt.overallScore ?? 0);
  const modeLabel = QUIZ_MODE_LABELS[attempt.mode]?.title ?? attempt.mode;
  const completedDate = formatQuizDate(attempt.completedAt || attempt.startedAt);
  const isCooldown = regenerateCooldownSeconds > 0;

  return (
    <div className="space-y-6">
      {/* Score Overview Card */}
      <div className="rounded-3xl border border-emerald-200/80 dark:border-emerald-900/60 bg-gradient-to-br from-emerald-50/90 via-white to-teal-50/50 dark:from-emerald-950/40 dark:via-slate-900 dark:to-slate-900 p-6 shadow-sm space-y-5">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="space-y-1.5">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-100 dark:bg-emerald-950/80 border border-emerald-200 dark:border-emerald-800 px-3 py-1 text-[11px] font-black uppercase tracking-wider text-emerald-700 dark:text-emerald-300">
              <Award className="h-3.5 w-3.5" aria-hidden="true" />
              <span>
                {isReviewOnly ? "Xem lại kết quả Quiz" : "Hoàn thành Quiz"}
              </span>
            </span>
            <h2 className="text-2xl font-black text-slate-900 dark:text-slate-100">
              {modeLabel}
            </h2>
            {completedDate && (
              <p className="text-xs font-semibold text-slate-500 dark:text-slate-400">
                Hoàn thành lúc {completedDate}
              </p>
            )}
          </div>

          <div className="rounded-2xl bg-white dark:bg-slate-900 border border-emerald-200/80 dark:border-emerald-800 px-5 py-3 text-center shadow-xs">
            <p className="text-[10px] font-black uppercase tracking-widest text-slate-400 dark:text-slate-500">
              Điểm tổng
            </p>
            <p className="text-3xl font-black text-emerald-600 dark:text-emerald-400">
              {overallScore}
              <span className="text-base font-bold text-slate-400 dark:text-slate-500">
                /100
              </span>
            </p>
          </div>
        </div>

        {/* Breakdown Stats */}
        <div className="grid grid-cols-3 gap-3">
          <div className="rounded-2xl bg-white/90 dark:bg-slate-900/80 border border-emerald-100 dark:border-emerald-900/50 p-3 flex items-center gap-2.5">
            <CheckCircle2
              className="h-5 w-5 text-emerald-500 shrink-0"
              aria-hidden="true"
            />
            <div>
              <p className="text-lg font-black text-slate-900 dark:text-slate-100 leading-none">
                {correctCount}
              </p>
              <p className="text-[11px] font-bold text-slate-500 dark:text-slate-400 mt-1">
                Đúng
              </p>
            </div>
          </div>

          <div className="rounded-2xl bg-white/90 dark:bg-slate-900/80 border border-amber-100 dark:border-amber-900/50 p-3 flex items-center gap-2.5">
            <HelpCircle
              className="h-5 w-5 text-amber-500 shrink-0"
              aria-hidden="true"
            />
            <div>
              <p className="text-lg font-black text-slate-900 dark:text-slate-100 leading-none">
                {partialCount}
              </p>
              <p className="text-[11px] font-bold text-slate-500 dark:text-slate-400 mt-1">
                Gần đúng
              </p>
            </div>
          </div>

          <div className="rounded-2xl bg-white/90 dark:bg-slate-900/80 border border-rose-100 dark:border-rose-900/50 p-3 flex items-center gap-2.5">
            <XCircle
              className="h-5 w-5 text-rose-500 shrink-0"
              aria-hidden="true"
            />
            <div>
              <p className="text-lg font-black text-slate-900 dark:text-slate-100 leading-none">
                {incorrectCount}
              </p>
              <p className="text-[11px] font-bold text-slate-500 dark:text-slate-400 mt-1">
                Sai
              </p>
            </div>
          </div>
        </div>

        {/* Streak confirmation */}
        {!isReviewOnly && (
          <div className="rounded-2xl bg-orange-50/90 dark:bg-orange-950/35 border border-orange-200/80 dark:border-orange-800/70 px-4 py-2.5 flex items-center gap-2.5 text-xs font-bold text-orange-800 dark:text-orange-200">
            <Flame className="h-4 w-4 text-orange-500 shrink-0" aria-hidden="true" />
            <span>
              Kết quả bài Quiz đã được ghi nhận vào chuỗi ngày học (streak) hôm
              nay!
            </span>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex flex-wrap gap-2.5 pt-1">
          {onRetrySameQuiz && (
            <button
              type="button"
              disabled={isBusy}
              onClick={onRetrySameQuiz}
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white px-4 py-2.5 text-xs font-black shadow-sm transition-all active:scale-95 disabled:opacity-50 cursor-pointer"
            >
              <RotateCcw className="h-3.5 w-3.5" aria-hidden="true" />
              <span>Làm lại</span>
            </button>
          )}

          {onRegenerateQuiz && (
            <button
              type="button"
              disabled={isBusy || isCooldown}
              onClick={onRegenerateQuiz}
              className="inline-flex items-center justify-center gap-2 rounded-xl border border-emerald-200 dark:border-emerald-800 bg-emerald-50 dark:bg-emerald-950/60 hover:bg-emerald-100 dark:hover:bg-emerald-900/60 text-emerald-700 dark:text-emerald-300 px-4 py-2.5 text-xs font-black transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              <RefreshCw className="h-3.5 w-3.5" aria-hidden="true" />
              <span>
                {isCooldown
                  ? `Tạo mới sau ${formatCooldownDuration(regenerateCooldownSeconds)}`
                  : "Tạo bộ câu hỏi mới"}
              </span>
            </button>
          )}

          <button
            type="button"
            disabled={isBusy}
            onClick={onBackToSetup}
            className="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200 px-4 py-2.5 text-xs font-bold transition-all cursor-pointer"
          >
            <ArrowLeft className="h-3.5 w-3.5" aria-hidden="true" />
            <span>Quay về chọn chế độ</span>
          </button>

          {onBackToDictation && (
            <button
              type="button"
              onClick={onBackToDictation}
              className="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 px-4 py-2.5 text-xs font-bold transition-all cursor-pointer"
            >
              <span>Quay lại Dictation</span>
            </button>
          )}
        </div>
      </div>

      {/* Detailed Question Review List */}
      <div className="space-y-4">
        <h3 className="text-sm font-black uppercase tracking-widest text-slate-500 dark:text-slate-400">
          Chi tiết từng câu hỏi ({questions.length} câu)
        </h3>

        {questions.map((q, idx) => {
          const result = q.answerResult;
          const statusMeta = result
            ? getGradingStatusMeta(result.status)
            : null;
          const timeRange =
            result &&
            Number.isFinite(result.evidenceStartTime) &&
            Number.isFinite(result.evidenceEndTime)
              ? formatQuizTimeRange(
                  result.evidenceStartTime,
                  result.evidenceEndTime,
                )
              : null;

          return (
            <div
              key={q.questionId}
              className="rounded-3xl border border-slate-200/80 dark:border-slate-800 bg-white dark:bg-slate-900 p-5 space-y-4 shadow-xs"
            >
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div className="space-y-1 flex-1 min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-xs font-black text-slate-400 dark:text-slate-500">
                      Câu {idx + 1}
                    </span>
                    {statusMeta && result && (
                      <span
                        className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-[11px] font-extrabold ${statusMeta.badgeClass}`}
                      >
                        {statusMeta.label} • {Math.round(result.score)}/100
                      </span>
                    )}
                  </div>
                  <p className="text-sm sm:text-base font-bold text-slate-900 dark:text-slate-100 leading-relaxed">
                    {q.questionText}
                  </p>
                </div>

                {onPlayEvidence && result && timeRange && (
                  <button
                    type="button"
                    onClick={() =>
                      onPlayEvidence(
                        result.evidenceStartTime,
                        result.evidenceEndTime,
                        result.evidenceText,
                      )
                    }
                    className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/60 border border-emerald-200 dark:border-emerald-800 hover:bg-emerald-100 dark:hover:bg-emerald-900/60 text-emerald-700 dark:text-emerald-300 px-3 py-1.5 text-xs font-bold transition-all cursor-pointer shrink-0"
                  >
                    <Headphones className="h-3.5 w-3.5" aria-hidden="true" />
                    <span>Nghe ({timeRange})</span>
                  </button>
                )}
              </div>

              {result?.userAnswer && (
                <div className="text-xs text-slate-600 dark:text-slate-300">
                  <span className="font-bold">Câu trả lời của bạn: </span>
                  <span className="font-semibold text-slate-900 dark:text-slate-100">
                    {result.userAnswer}
                  </span>
                </div>
              )}

              {result && (
                <QuizFeedbackCard
                  answerResult={result}
                  onPlayEvidence={onPlayEvidence}
                />
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
