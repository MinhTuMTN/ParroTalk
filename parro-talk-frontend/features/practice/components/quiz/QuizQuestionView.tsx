"use client";

import { useState, type KeyboardEvent } from "react";
import {
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  Loader2,
  Sparkles,
  Volume2,
} from "lucide-react";
import type { LessonQuizAttemptResponse, LessonQuizQuestion } from "../../types/quiz";
import QuizFeedbackCard from "./QuizFeedbackCard";
import { QUIZ_MODE_LABELS } from "./quizUtils";

export interface QuizQuestionViewProps {
  attempt: LessonQuizAttemptResponse;
  currentIndex: number;
  onSelectIndex: (index: number) => void;
  onSubmitAnswer: (payload: {
    questionId: string;
    selectedIndex?: number | null;
    userAnswer?: string | null;
  }) => Promise<void> | void;
  onCompleteAttempt: () => Promise<void> | void;
  onPlayEvidence?: (startTime: number, endTime: number, text: string) => void;
  onPlayFullAudio?: () => void;
  onBackToSetup?: () => void;
  isGrading?: boolean;
  isCompleting?: boolean;
}

const OPTION_LETTERS = ["A", "B", "C", "D", "E", "F"];

function renderQuestionPrompt(questionText: string, isBlank: boolean) {
  if (!isBlank) {
    return <span>{questionText}</span>;
  }
  const parts = questionText.split(/(_{2,})/);
  if (parts.length === 1) {
    return <span>{questionText}</span>;
  }
  return (
    <>
      {parts.map((part, idx) =>
        /^_{2,}$/.test(part) ? (
          <span
            key={idx}
            className="inline-flex items-center justify-center mx-1 px-3 py-0.5 rounded-lg border-2 border-dashed border-emerald-400 dark:border-emerald-500 bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 font-black tracking-widest text-sm align-middle"
          >
            ___
          </span>
        ) : (
          <span key={idx}>{part}</span>
        ),
      )}
    </>
  );
}

function getQuestionDotClass(
  q: LessonQuizQuestion,
  idx: number,
  currentIndex: number,
): string {
  const isCurrent = idx === currentIndex;
  if (q.answered && q.answerResult) {
    if (q.answerResult.status === "CORRECT") {
      return isCurrent
        ? "bg-emerald-600 text-white border-emerald-600 ring-2 ring-emerald-300 dark:ring-emerald-700"
        : "bg-emerald-100 text-emerald-700 border-emerald-300 dark:bg-emerald-950/70 dark:text-emerald-300 dark:border-emerald-700";
    }
    if (q.answerResult.status === "PARTIAL") {
      return isCurrent
        ? "bg-amber-500 text-white border-amber-500 ring-2 ring-amber-300 dark:ring-amber-700"
        : "bg-amber-100 text-amber-800 border-amber-300 dark:bg-amber-950/70 dark:text-amber-300 dark:border-amber-700";
    }
    return isCurrent
      ? "bg-rose-500 text-white border-rose-500 ring-2 ring-rose-300 dark:ring-rose-700"
      : "bg-rose-100 text-rose-700 border-rose-300 dark:bg-rose-950/70 dark:text-rose-300 dark:border-rose-700";
  }

  if (isCurrent) {
    return "bg-emerald-500 text-white border-emerald-500 ring-2 ring-emerald-200 dark:ring-emerald-800";
  }

  return "bg-white dark:bg-slate-900 text-slate-600 dark:text-slate-300 border-slate-200 dark:border-slate-700 hover:border-emerald-300";
}

export default function QuizQuestionView({
  attempt,
  currentIndex,
  onSelectIndex,
  onSubmitAnswer,
  onCompleteAttempt,
  onPlayEvidence,
  onPlayFullAudio,
  onBackToSetup,
  isGrading = false,
  isCompleting = false,
}: QuizQuestionViewProps) {
  const questions = attempt.questions ?? [];
  const safeIndex = Math.max(0, Math.min(currentIndex, questions.length - 1));
  const question = questions[safeIndex];

  const [selectedOptionMap, setSelectedOptionMap] = useState<
    Record<string, number | null>
  >({});
  const [textAnswerMap, setTextAnswerMap] = useState<Record<string, string>>({});

  if (!question) {
    return (
      <div className="rounded-3xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-6 text-center space-y-3">
        <p className="text-sm font-semibold text-slate-600 dark:text-slate-300">
          Không tìm thấy câu hỏi nào trong lượt làm Quiz này.
        </p>
        {onBackToSetup && (
          <button
            type="button"
            onClick={onBackToSetup}
            className="inline-flex items-center gap-2 rounded-xl bg-emerald-500 text-white px-4 py-2 text-xs font-bold cursor-pointer"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Quay lại</span>
          </button>
        )}
      </div>
    );
  }

  const isAnswered = question.answered && question.answerResult !== null;
  const answerResult = question.answerResult;
  const isMcq = question.questionType === "MULTIPLE_CHOICE";
  const allAnswered =
    questions.length > 0 && questions.every((q) => q.answered);

  const draftSelectedIndex =
    selectedOptionMap[question.questionId] !== undefined
      ? selectedOptionMap[question.questionId]
      : (answerResult?.selectedIndex ?? null);
  const draftUserAnswer =
    textAnswerMap[question.questionId] !== undefined
      ? textAnswerMap[question.questionId]
      : (answerResult?.userAnswer ?? "");

  const canCheck = isMcq
    ? draftSelectedIndex !== null && draftSelectedIndex !== undefined
    : draftUserAnswer.trim().length > 0;

  const handleCheck = () => {
    if (isAnswered || isGrading || !canCheck) return;
    if (isMcq) {
      void onSubmitAnswer({
        questionId: question.questionId,
        selectedIndex: draftSelectedIndex,
        userAnswer: question.options?.[draftSelectedIndex ?? 0] ?? null,
      });
    } else {
      void onSubmitAnswer({
        questionId: question.questionId,
        userAnswer: draftUserAnswer.trim(),
      });
    }
  };

  const handleBlankKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      handleCheck();
    }
  };

  const handleNextOrComplete = () => {
    if (allAnswered) {
      void onCompleteAttempt();
      return;
    }
    if (safeIndex < questions.length - 1) {
      onSelectIndex(safeIndex + 1);
      return;
    }
    const nextUnanswered = questions.findIndex((q) => !q.answered);
    if (nextUnanswered !== -1) {
      onSelectIndex(nextUnanswered);
    }
  };

  return (
    <div className="space-y-5">
      {/* Top Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          {onBackToSetup && (
            <button
              type="button"
              onClick={onBackToSetup}
              className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800 px-3 py-1.5 text-xs font-bold text-slate-700 dark:text-slate-200 transition-all cursor-pointer"
            >
              <ArrowLeft className="h-3.5 w-3.5" aria-hidden="true" />
              <span>Chế độ</span>
            </button>
          )}
          <span className="inline-flex items-center rounded-full bg-emerald-50 dark:bg-emerald-950/60 border border-emerald-200 dark:border-emerald-800 px-3 py-1 text-xs font-extrabold text-emerald-700 dark:text-emerald-300">
            {QUIZ_MODE_LABELS[attempt.mode]?.title ?? attempt.mode}
          </span>
        </div>

        <div className="flex items-center gap-2">
          {onPlayFullAudio && (
            <button
              type="button"
              onClick={onPlayFullAudio}
              className="inline-flex items-center gap-1.5 rounded-xl border border-emerald-200 dark:border-emerald-800 bg-emerald-50 dark:bg-emerald-950/60 hover:bg-emerald-100 dark:hover:bg-emerald-900/60 px-3 py-1.5 text-xs font-bold text-emerald-700 dark:text-emerald-300 transition-all active:scale-95 cursor-pointer"
            >
              <Volume2 className="h-3.5 w-3.5" aria-hidden="true" />
              <span>Nghe lại toàn bài</span>
            </button>
          )}
        </div>
      </div>

      {/* Question Progress Dots */}
      <div className="rounded-2xl border border-slate-200/80 dark:border-slate-800 bg-white dark:bg-slate-900 p-3.5 sm:p-4 space-y-2.5 shadow-xs">
        <div className="flex items-center justify-between text-xs font-bold text-slate-500 dark:text-slate-400">
          <span>
            Câu {safeIndex + 1}/{questions.length}
          </span>
          <span>
            Đã trả lời {questions.filter((q) => q.answered).length}/
            {questions.length}
          </span>
        </div>
        <div className="flex flex-wrap gap-2">
          {questions.map((q, idx) => (
            <button
              key={q.questionId}
              type="button"
              onClick={() => onSelectIndex(idx)}
              aria-label={`Câu ${idx + 1}`}
              aria-current={idx === safeIndex ? "step" : undefined}
              className={`h-8 min-w-8 px-2 rounded-lg border text-xs font-black transition-all cursor-pointer ${getQuestionDotClass(
                q,
                idx,
                safeIndex,
              )}`}
            >
              {idx + 1}
            </button>
          ))}
        </div>
      </div>

      {/* Main Question Card */}
      <div className="rounded-3xl border border-slate-200/90 dark:border-slate-800 bg-white dark:bg-slate-900 p-5 sm:p-6 space-y-5 shadow-sm">
        <div className="flex items-center justify-between gap-2">
          <span className="inline-flex items-center rounded-full bg-slate-100 dark:bg-slate-800 px-3 py-1 text-[11px] font-black uppercase tracking-wider text-slate-600 dark:text-slate-300">
            {isMcq ? "Trắc nghiệm" : "Điền khuyết"}
          </span>
          <span className="text-xs font-bold text-slate-400 dark:text-slate-500">
            Câu #{question.displayOrder || safeIndex + 1}
          </span>
        </div>

        <h3 className="text-base sm:text-lg font-extrabold text-slate-900 dark:text-slate-100 leading-relaxed">
          {renderQuestionPrompt(question.questionText, !isMcq)}
        </h3>

        {isMcq ? (
          <div className="grid grid-cols-1 gap-2.5">
            {(question.options ?? []).map((option, idx) => {
              const isSelected = draftSelectedIndex === idx;
              const isCorrectOption =
                isAnswered && answerResult?.correctIndex === idx;
              const isWrongSelected =
                isAnswered &&
                answerResult?.selectedIndex === idx &&
                answerResult?.correctIndex !== idx;

              let optionClass =
                "border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-800 dark:text-slate-100 hover:border-emerald-400 hover:bg-emerald-50/40 dark:hover:bg-emerald-950/30";
              let badgeClass =
                "bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 border-slate-200 dark:border-slate-700";

              if (isAnswered) {
                if (isCorrectOption) {
                  optionClass =
                    "border-emerald-500 bg-emerald-50 dark:bg-emerald-950/60 text-emerald-900 dark:text-emerald-100 ring-1 ring-emerald-500/30";
                  badgeClass =
                    "bg-emerald-500 text-white border-emerald-500";
                } else if (isWrongSelected) {
                  optionClass =
                    "border-rose-500 bg-rose-50 dark:bg-rose-950/50 text-rose-900 dark:text-rose-100";
                  badgeClass = "bg-rose-500 text-white border-rose-500";
                } else {
                  optionClass =
                    "border-slate-200/70 dark:border-slate-800 bg-slate-50/60 dark:bg-slate-900/50 text-slate-500 dark:text-slate-400 opacity-75";
                }
              } else if (isSelected) {
                optionClass =
                  "border-emerald-500 bg-emerald-50/80 dark:bg-emerald-950/50 text-emerald-950 dark:text-emerald-100 ring-2 ring-emerald-500/20";
                badgeClass = "bg-emerald-500 text-white border-emerald-500";
              }

              return (
                <button
                  key={idx}
                  type="button"
                  disabled={isAnswered || isGrading}
                  onClick={() =>
                    setSelectedOptionMap((prev) => ({
                      ...prev,
                      [question.questionId]: idx,
                    }))
                  }
                  className={`w-full text-left rounded-2xl border p-3.5 sm:p-4 flex items-start gap-3 transition-all ${
                    isAnswered ? "cursor-default" : "cursor-pointer active:scale-[0.995]"
                  } ${optionClass}`}
                >
                  <span
                    className={`h-7 w-7 rounded-lg border flex items-center justify-center text-xs font-black shrink-0 ${badgeClass}`}
                  >
                    {OPTION_LETTERS[idx] ?? idx + 1}
                  </span>
                  <span className="text-sm sm:text-[15px] font-semibold leading-snug pt-0.5">
                    {option}
                  </span>
                </button>
              );
            })}
          </div>
        ) : (
          <div className="space-y-2.5">
            <label
              htmlFor={`quiz-blank-${question.questionId}`}
              className="block text-xs font-bold text-slate-500 dark:text-slate-400"
            >
              Nhập câu trả lời của bạn (có thể diễn đạt lại đúng ý):
            </label>
            <input
              id={`quiz-blank-${question.questionId}`}
              type="text"
              value={draftUserAnswer}
              disabled={isAnswered || isGrading}
              onChange={(e) =>
                setTextAnswerMap((prev) => ({
                  ...prev,
                  [question.questionId]: e.target.value,
                }))
              }
              onKeyDown={handleBlankKeyDown}
              placeholder="Gõ đáp án nghe được..."
              className="w-full rounded-2xl border border-slate-200 dark:border-slate-700 bg-slate-50/70 dark:bg-slate-800/70 px-4 py-3 text-sm sm:text-base font-semibold text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:border-emerald-500 focus:bg-white dark:focus:bg-slate-900 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 disabled:opacity-75"
            />
          </div>
        )}

        {/* AI Grading Loading Indicator */}
        {isGrading && !isMcq && (
          <div className="rounded-2xl border border-emerald-200 dark:border-emerald-800 bg-emerald-50/80 dark:bg-emerald-950/40 px-4 py-3 flex items-center gap-2.5 text-sm font-bold text-emerald-800 dark:text-emerald-200">
            <Loader2 className="h-4 w-4 animate-spin shrink-0" aria-hidden="true" />
            <span>AI đang chấm câu trả lời...</span>
          </div>
        )}

        {/* Action Button Row */}
        {!isAnswered ? (
          <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
            <button
              type="button"
              disabled={!canCheck || isGrading}
              onClick={handleCheck}
              className="inline-flex items-center justify-center gap-2 rounded-2xl bg-emerald-500 hover:bg-emerald-600 text-white px-6 py-3 text-sm font-black shadow-md shadow-emerald-500/15 dark:shadow-none transition-all active:scale-95 disabled:opacity-45 disabled:cursor-not-allowed cursor-pointer"
            >
              {isGrading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
                  <span>Đang chấm...</span>
                </>
              ) : (
                <>
                  <Sparkles className="h-4 w-4" aria-hidden="true" />
                  <span>{isMcq ? "Kiểm tra" : "Kiểm tra (AI chấm)"}</span>
                </>
              )}
            </button>

            <div className="flex items-center gap-2">
              {safeIndex > 0 && (
                <button
                  type="button"
                  onClick={() => onSelectIndex(safeIndex - 1)}
                  className="rounded-xl border border-slate-200 dark:border-slate-700 px-3.5 py-2 text-xs font-bold text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 cursor-pointer"
                >
                  Câu trước
                </button>
              )}
              {safeIndex < questions.length - 1 && (
                <button
                  type="button"
                  onClick={() => onSelectIndex(safeIndex + 1)}
                  className="rounded-xl border border-slate-200 dark:border-slate-700 px-3.5 py-2 text-xs font-bold text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 cursor-pointer"
                >
                  Bỏ qua tạm
                </button>
              )}
            </div>
          </div>
        ) : null}
      </div>

      {/* Feedback Card - ONLY rendered after question is answered */}
      {isAnswered && answerResult && (
        <div className="space-y-4">
          <QuizFeedbackCard
            answerResult={answerResult}
            onPlayEvidence={onPlayEvidence}
          />

          <div className="flex flex-wrap items-center justify-between gap-3">
            {safeIndex > 0 ? (
              <button
                type="button"
                onClick={() => onSelectIndex(safeIndex - 1)}
                className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 px-4 py-2.5 text-xs font-bold text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800 cursor-pointer"
              >
                <ArrowLeft className="h-3.5 w-3.5" aria-hidden="true" />
                <span>Câu trước</span>
              </button>
            ) : (
              <div />
            )}

            <button
              type="button"
              disabled={isCompleting}
              onClick={handleNextOrComplete}
              className="inline-flex items-center justify-center gap-2 rounded-2xl bg-emerald-500 hover:bg-emerald-600 text-white px-6 py-3 text-sm font-black shadow-md shadow-emerald-500/20 dark:shadow-none transition-all active:scale-95 disabled:opacity-50 cursor-pointer"
            >
              {isCompleting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
                  <span>Đang tổng kết...</span>
                </>
              ) : allAnswered ? (
                <>
                  <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
                  <span>Hoàn thành &amp; Xem kết quả</span>
                </>
              ) : (
                <>
                  <span>Câu tiếp theo</span>
                  <ArrowRight className="h-4 w-4" aria-hidden="true" />
                </>
              )}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
