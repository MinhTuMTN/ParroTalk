"use client";

import {
  CheckCircle2,
  Clock,
  History,
  Layers,
  ListChecks,
  PenLine,
  Play,
  RefreshCw,
  Sparkles,
} from "lucide-react";
import type {
  LessonQuizAttemptSummary,
  LessonQuizOverviewResponse,
  QuizMode,
} from "../../types/quiz";
import {
  QUIZ_MODE_LABELS,
  formatCooldownDuration,
  formatQuizDate,
} from "./quizUtils";

export interface QuizSetupPanelProps {
  overview: LessonQuizOverviewResponse;
  selectedMode: QuizMode;
  onSelectMode: (mode: QuizMode) => void;
  onStartQuiz: (mode: QuizMode) => void;
  onResumeAttempt: (attempt: LessonQuizAttemptSummary) => void;
  onRegenerateQuiz: (mode: QuizMode) => void;
  onReviewHistory: (attempt: LessonQuizAttemptSummary) => void;
  isSubmitting?: boolean;
  isRegenerating?: boolean;
}

const MODE_ORDER: QuizMode[] = [
  "MULTIPLE_CHOICE",
  "FILL_IN_THE_BLANK",
  "MIXED",
];

function ModeIcon({ mode }: { mode: QuizMode }) {
  if (mode === "MULTIPLE_CHOICE") {
    return <ListChecks className="h-5 w-5" aria-hidden="true" />;
  }
  if (mode === "FILL_IN_THE_BLANK") {
    return <PenLine className="h-5 w-5" aria-hidden="true" />;
  }
  return <Layers className="h-5 w-5" aria-hidden="true" />;
}

export default function QuizSetupPanel({
  overview,
  selectedMode,
  onSelectMode,
  onStartQuiz,
  onResumeAttempt,
  onRegenerateQuiz,
  onReviewHistory,
  isSubmitting = false,
  isRegenerating = false,
}: QuizSetupPanelProps) {
  const activeAttempt = overview.activeAttempt;
  const cachedSet = new Set<QuizMode>(overview.cachedModes ?? []);
  const cooldownSeconds =
    overview.regenerateCooldownSeconds?.[selectedMode] ?? 0;
  const isCooldownActive = cooldownSeconds > 0;
  const isBusy = isSubmitting || isRegenerating;

  const completedHistory = (overview.history ?? []).filter(
    (item) => item.status === "COMPLETED",
  );

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="rounded-3xl border border-emerald-100 dark:border-emerald-900/60 bg-gradient-to-br from-emerald-50/90 via-white to-teal-50/50 dark:from-emerald-950/40 dark:via-slate-900 dark:to-slate-900 p-5 sm:p-6 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="space-y-1.5">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-100/90 dark:bg-emerald-950/80 border border-emerald-200 dark:border-emerald-800 px-3 py-1 text-[11px] font-extrabold text-emerald-700 dark:text-emerald-300">
              <Sparkles className="h-3.5 w-3.5" aria-hidden="true" />
              <span>Listening Comprehension Quiz</span>
            </span>
            <h2 className="text-xl sm:text-2xl font-black text-slate-900 dark:text-slate-100 tracking-tight">
              Kiểm tra hiểu bài nghe
            </h2>
            <p className="text-sm text-slate-600 dark:text-slate-300 max-w-xl">
              Trả lời câu hỏi dựa trên nội dung bài nghe trước khi xem trích đoạn
              dẫn chứng. Bộ câu hỏi đề xuất gồm{" "}
              <span className="font-bold text-emerald-700 dark:text-emerald-300">
                {overview.recommendedQuestionCount} câu
              </span>
              .
            </p>
          </div>
        </div>
      </div>

      {/* Resume Active Attempt Banner */}
      {activeAttempt && activeAttempt.status === "IN_PROGRESS" && (
        <div className="rounded-2xl border-2 border-amber-300/90 dark:border-amber-700/80 bg-amber-50/90 dark:bg-amber-950/35 p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-sm">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="inline-flex items-center gap-1 rounded-full bg-amber-200/80 dark:bg-amber-900/70 px-2.5 py-0.5 text-[11px] font-black uppercase tracking-wider text-amber-900 dark:text-amber-200">
                <Clock className="h-3 w-3" aria-hidden="true" />
                <span>Tiếp tục bài làm dở</span>
              </span>
              <span className="text-xs font-bold text-amber-900/80 dark:text-amber-200/80">
                {QUIZ_MODE_LABELS[activeAttempt.mode]?.title ?? activeAttempt.mode}
              </span>
            </div>
            <p className="text-sm font-semibold text-slate-800 dark:text-slate-100">
              Bạn đã hoàn thành{" "}
              <span className="font-black text-amber-800 dark:text-amber-300">
                {activeAttempt.answeredCount}/{activeAttempt.totalQuestions}
              </span>{" "}
              câu hỏi trong lượt làm này.
            </p>
          </div>

          <button
            type="button"
            disabled={isBusy}
            onClick={() => onResumeAttempt(activeAttempt)}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-amber-500 hover:bg-amber-600 text-white px-5 py-2.5 text-sm font-black shadow-sm transition-all active:scale-95 disabled:opacity-50 cursor-pointer shrink-0"
          >
            <Play className="h-4 w-4" fill="currentColor" aria-hidden="true" />
            <span>Tiếp tục</span>
          </button>
        </div>
      )}

      {/* Mode Selection Cards */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-xs font-black uppercase tracking-widest text-slate-500 dark:text-slate-400">
            Chọn chế độ làm Quiz
          </h3>
          <span className="text-xs font-bold text-slate-500 dark:text-slate-400">
            Dự kiến {overview.recommendedQuestionCount} câu
          </span>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-3.5">
          {MODE_ORDER.map((mode) => {
            const meta = QUIZ_MODE_LABELS[mode];
            const isSelected = selectedMode === mode;
            const isCached = cachedSet.has(mode);

            return (
              <button
                key={mode}
                type="button"
                onClick={() => onSelectMode(mode)}
                aria-pressed={isSelected}
                className={`text-left rounded-2xl border p-4 transition-all cursor-pointer flex flex-col justify-between gap-3 ${
                  isSelected
                    ? "border-emerald-500 bg-emerald-50/70 dark:bg-emerald-950/40 ring-2 ring-emerald-500/20 shadow-md"
                    : "border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 hover:border-emerald-300 dark:hover:border-emerald-700 shadow-xs"
                }`}
              >
                <div className="space-y-2.5">
                  <div className="flex items-start justify-between gap-2">
                    <div
                      className={`p-2.5 rounded-xl border ${
                        isSelected
                          ? "bg-emerald-500 text-white border-emerald-500"
                          : "bg-slate-50 dark:bg-slate-800 text-emerald-600 dark:text-emerald-400 border-slate-200/80 dark:border-slate-700"
                      }`}
                    >
                      <ModeIcon mode={mode} />
                    </div>
                    <div className="flex flex-wrap items-center justify-end gap-1.5">
                      {isCached && (
                        <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 dark:bg-emerald-950/80 border border-emerald-200 dark:border-emerald-800 px-2 py-0.5 text-[10px] font-extrabold text-emerald-700 dark:text-emerald-300">
                          <CheckCircle2 className="h-3 w-3" aria-hidden="true" />
                          <span>Đã sẵn sàng</span>
                        </span>
                      )}
                      <span className="inline-flex items-center rounded-full bg-slate-100 dark:bg-slate-800 px-2 py-0.5 text-[10px] font-bold text-slate-600 dark:text-slate-300">
                        {meta.badge}
                      </span>
                    </div>
                  </div>

                  <div>
                    <p className="text-base font-black text-slate-900 dark:text-slate-100">
                      {meta.title}
                    </p>
                    <p className="mt-1 text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
                      {meta.subtitle}
                    </p>
                  </div>
                </div>

                <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-[11px] font-bold text-slate-500 dark:text-slate-400">
                  <span>{overview.recommendedQuestionCount} câu hỏi</span>
                  {isSelected && (
                    <span className="text-emerald-600 dark:text-emerald-400 font-black">
                      Đang chọn
                    </span>
                  )}
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Primary Action Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
        <button
          type="button"
          disabled={isBusy}
          onClick={() => onStartQuiz(selectedMode)}
          className="flex-1 inline-flex items-center justify-center gap-2 rounded-2xl bg-emerald-500 hover:bg-emerald-600 text-white px-6 py-3.5 text-sm font-black shadow-lg shadow-emerald-500/20 dark:shadow-none transition-all active:scale-[0.99] disabled:opacity-50 cursor-pointer"
        >
          <Play className="h-4 w-4" fill="currentColor" aria-hidden="true" />
          <span>Bắt đầu làm Quiz</span>
        </button>

        <button
          type="button"
          disabled={isBusy || isCooldownActive}
          onClick={() => onRegenerateQuiz(selectedMode)}
          className="inline-flex items-center justify-center gap-2 rounded-2xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200 px-5 py-3.5 text-sm font-bold transition-all active:scale-[0.99] disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
        >
          <RefreshCw
            className={`h-4 w-4 ${isRegenerating ? "animate-spin" : ""}`}
            aria-hidden="true"
          />
          <span>
            {isCooldownActive
              ? `Tạo lại sau ${formatCooldownDuration(cooldownSeconds)}`
              : "Tạo bộ câu hỏi mới"}
          </span>
        </button>
      </div>

      {/* History Section */}
      <div className="rounded-3xl border border-slate-200/80 dark:border-slate-800 bg-white dark:bg-slate-900 p-5 space-y-4 shadow-xs">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-black text-slate-800 dark:text-slate-100 flex items-center gap-2">
            <History className="h-4 w-4 text-emerald-500" aria-hidden="true" />
            <span>Lịch sử làm Quiz</span>
          </h3>
          <span className="text-xs font-bold text-slate-400 dark:text-slate-500">
            {completedHistory.length} lượt hoàn thành
          </span>
        </div>

        {completedHistory.length === 0 ? (
          <p className="text-sm text-slate-500 dark:text-slate-400 py-2">
            Bạn chưa hoàn thành lượt Quiz nào cho bài nghe này.
          </p>
        ) : (
          <div className="divide-y divide-slate-100 dark:divide-slate-800">
            {completedHistory.map((item) => {
              const modeLabel =
                QUIZ_MODE_LABELS[item.mode]?.title ?? item.mode;
              const dateLabel = formatQuizDate(
                item.completedAt || item.startedAt,
              );
              return (
                <div
                  key={item.attemptId}
                  className="py-3 first:pt-0 last:pb-0 flex flex-wrap items-center justify-between gap-3"
                >
                  <div className="space-y-0.5">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-bold text-slate-800 dark:text-slate-100">
                        {modeLabel}
                      </span>
                      <span className="inline-flex items-center rounded-full bg-emerald-50 dark:bg-emerald-950/70 border border-emerald-200/80 dark:border-emerald-800 px-2 py-0.5 text-xs font-black text-emerald-700 dark:text-emerald-300">
                        {Math.round(item.overallScore)}/100
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      Đúng {item.correctCount}/{item.totalQuestions} câu
                      {item.partialCount > 0
                        ? ` (${item.partialCount} gần đúng)`
                        : ""}
                      {dateLabel ? ` • ${dateLabel}` : ""}
                    </p>
                  </div>

                  <button
                    type="button"
                    disabled={isBusy}
                    onClick={() => onReviewHistory(item)}
                    className="rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 hover:bg-emerald-50 dark:hover:bg-emerald-950/60 hover:border-emerald-300 dark:hover:border-emerald-700 text-slate-700 dark:text-slate-200 px-3.5 py-1.5 text-xs font-bold transition-all cursor-pointer disabled:opacity-50"
                  >
                    Xem lại
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
