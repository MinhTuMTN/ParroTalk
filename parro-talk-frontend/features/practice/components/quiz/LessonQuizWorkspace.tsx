"use client";

import { useCallback, useEffect, useState } from "react";
import { AlertCircle, Loader2, Sparkles, X } from "lucide-react";
import { lessonQuizService } from "../../services/lessonQuizService";
import type {
  LessonQuizAttemptResponse,
  LessonQuizAttemptSummary,
  LessonQuizOverviewResponse,
  QuizMode,
} from "../../types/quiz";
import QuizQuestionView from "./QuizQuestionView";
import QuizSetupPanel from "./QuizSetupPanel";
import QuizSummaryCard from "./QuizSummaryCard";
import { extractQuizErrorMessage } from "./quizUtils";

export type QuizWorkspaceViewState = "setup" | "active" | "summary" | "review";

export interface LessonQuizWorkspaceProps {
  lessonId: string;
  hasSegments: boolean;
  onPlayEvidence: (startTime: number, endTime: number, text: string) => void;
  onPlayFullAudio: () => void;
  onBackToDictation?: () => void;
}

export default function LessonQuizWorkspace({
  lessonId,
  hasSegments,
  onPlayEvidence,
  onPlayFullAudio,
  onBackToDictation,
}: LessonQuizWorkspaceProps) {
  const [viewState, setViewState] = useState<QuizWorkspaceViewState>("setup");
  const [overview, setOverview] = useState<LessonQuizOverviewResponse | null>(
    null,
  );
  const [selectedMode, setSelectedMode] = useState<QuizMode>("MIXED");
  const [activeAttempt, setActiveAttempt] =
    useState<LessonQuizAttemptResponse | null>(null);
  const [currentIndex, setCurrentIndex] = useState(0);

  const [isLoadingOverview, setIsLoadingOverview] = useState(true);
  const [loadingMessage, setLoadingMessage] = useState<string | null>(null);
  const [isGrading, setIsGrading] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const loadOverview = useCallback(async () => {
    if (!lessonId || !hasSegments) {
      setIsLoadingOverview(false);
      return;
    }
    setIsLoadingOverview(true);
    setErrorMessage(null);
    try {
      const data = await lessonQuizService.getOverview(lessonId);
      setOverview(data);
      if (data.activeAttempt?.mode) {
        setSelectedMode(data.activeAttempt.mode);
      }
    } catch (err) {
      const parsed = extractQuizErrorMessage(err);
      setErrorMessage(parsed.message);
    } finally {
      setIsLoadingOverview(false);
    }
  }, [lessonId, hasSegments]);

  useEffect(() => {
    void loadOverview();
  }, [loadOverview]);

  const hasPositiveCooldown = Object.values(
    overview?.regenerateCooldownSeconds ?? {},
  ).some((sec) => sec > 0);

  // Countdown timer for regenerateCooldownSeconds in setup/summary
  useEffect(() => {
    if (!hasPositiveCooldown) return;

    const timer = window.setInterval(() => {
      setOverview((prev) => {
        if (!prev) return prev;
        const nextCooldowns: Record<QuizMode, number> = {
          MULTIPLE_CHOICE: Math.max(
            0,
            (prev.regenerateCooldownSeconds?.MULTIPLE_CHOICE ?? 0) - 1,
          ),
          FILL_IN_THE_BLANK: Math.max(
            0,
            (prev.regenerateCooldownSeconds?.FILL_IN_THE_BLANK ?? 0) - 1,
          ),
          MIXED: Math.max(0, (prev.regenerateCooldownSeconds?.MIXED ?? 0) - 1),
        };
        return { ...prev, regenerateCooldownSeconds: nextCooldowns };
      });
    }, 1000);

    return () => window.clearInterval(timer);
  }, [hasPositiveCooldown]);

  const openAttemptForDoing = (attempt: LessonQuizAttemptResponse) => {
    setActiveAttempt(attempt);
    const firstUnanswered = (attempt.questions ?? []).findIndex(
      (q) => !q.answered,
    );
    setCurrentIndex(firstUnanswered !== -1 ? firstUnanswered : 0);
    setViewState("active");
  };

  const handleStartQuiz = async (mode: QuizMode, forceNew = false) => {
    setErrorMessage(null);
    const isCached = overview?.cachedModes?.includes(mode);
    setLoadingMessage(
      isCached && !forceNew
        ? "Đang tải bộ câu hỏi..."
        : "AI đang phân tích transcript và tạo câu hỏi...",
    );
    try {
      const attempt = await lessonQuizService.startAttempt(lessonId, {
        mode,
        forceNew,
      });
      setSelectedMode(attempt.mode);
      openAttemptForDoing(attempt);
    } catch (err) {
      const parsed = extractQuizErrorMessage(err);
      setErrorMessage(parsed.message);
    } finally {
      setLoadingMessage(null);
    }
  };

  const handleResumeAttempt = async (summary: LessonQuizAttemptSummary) => {
    setErrorMessage(null);
    setLoadingMessage("Đang khôi phục bài làm dở...");
    try {
      const attempt = await lessonQuizService.getAttempt(
        lessonId,
        summary.attemptId,
      );
      setSelectedMode(attempt.mode);
      openAttemptForDoing(attempt);
    } catch (err) {
      const parsed = extractQuizErrorMessage(err);
      setErrorMessage(parsed.message);
    } finally {
      setLoadingMessage(null);
    }
  };

  const handleRegenerateQuiz = async (mode: QuizMode) => {
    setErrorMessage(null);
    setLoadingMessage("AI đang phân tích transcript và tạo câu hỏi...");
    try {
      const attempt = await lessonQuizService.regenerateQuiz(lessonId, {
        mode,
      });
      setSelectedMode(attempt.mode);
      setOverview((prev) => {
        if (!prev) return prev;
        const nextCached = Array.from(
          new Set([...(prev.cachedModes ?? []), attempt.mode]),
        );
        return {
          ...prev,
          cachedModes: nextCached,
          regenerateCooldownSeconds: {
            ...prev.regenerateCooldownSeconds,
            [attempt.mode]: attempt.regenerateCooldownSeconds ?? 0,
          },
        };
      });
      openAttemptForDoing(attempt);
    } catch (err) {
      const parsed = extractQuizErrorMessage(err);
      setErrorMessage(parsed.message);
      if (parsed.isRateLimited && parsed.retryAfterSeconds) {
        const retrySec = parsed.retryAfterSeconds;
        setOverview((prev) =>
          prev
            ? {
                ...prev,
                regenerateCooldownSeconds: {
                  ...prev.regenerateCooldownSeconds,
                  [mode]: retrySec,
                },
              }
            : prev,
        );
      } else if (parsed.isRateLimited) {
        void loadOverview();
      }
    } finally {
      setLoadingMessage(null);
    }
  };

  const handleReviewHistory = async (summary: LessonQuizAttemptSummary) => {
    setErrorMessage(null);
    setLoadingMessage("Đang tải chi tiết bài làm...");
    try {
      const attempt = await lessonQuizService.getAttempt(
        lessonId,
        summary.attemptId,
      );
      setActiveAttempt(attempt);
      setViewState("review");
    } catch (err) {
      const parsed = extractQuizErrorMessage(err);
      setErrorMessage(parsed.message);
    } finally {
      setLoadingMessage(null);
    }
  };

  const handleSubmitAnswer = async (payload: {
    questionId: string;
    selectedIndex?: number | null;
    userAnswer?: string | null;
  }) => {
    if (!activeAttempt) return;
    setErrorMessage(null);
    setIsGrading(true);
    try {
      const result = await lessonQuizService.submitAnswer(
        lessonId,
        activeAttempt.attemptId,
        payload,
      );

      setActiveAttempt((prev) => {
        if (!prev) return prev;
        const updatedQuestions = prev.questions.map((q) =>
          q.questionId === payload.questionId
            ? { ...q, answered: true, answerResult: result }
            : q,
        );
        const answeredCount = updatedQuestions.filter((q) => q.answered).length;
        const correctCount = updatedQuestions.filter(
          (q) => q.answerResult?.status === "CORRECT",
        ).length;
        const partialCount = updatedQuestions.filter(
          (q) => q.answerResult?.status === "PARTIAL",
        ).length;

        return {
          ...prev,
          answeredCount,
          correctCount,
          partialCount,
          questions: updatedQuestions,
        };
      });
    } catch (err) {
      const parsed = extractQuizErrorMessage(err);
      setErrorMessage(parsed.message);
    } finally {
      setIsGrading(false);
    }
  };

  const handleCompleteAttempt = async () => {
    if (!activeAttempt) return;
    setErrorMessage(null);
    setIsCompleting(true);
    try {
      const summary = await lessonQuizService.completeAttempt(
        lessonId,
        activeAttempt.attemptId,
      );
      setActiveAttempt((prev) =>
        prev
          ? {
              ...prev,
              status: summary.status,
              overallScore: summary.overallScore,
              correctCount: summary.correctCount,
              partialCount: summary.partialCount,
              answeredCount: summary.answeredCount,
              completedAt: summary.completedAt,
            }
          : prev,
      );
      setViewState("summary");
      void loadOverview();
    } catch (err) {
      const parsed = extractQuizErrorMessage(err);
      setErrorMessage(parsed.message);
    } finally {
      setIsCompleting(false);
    }
  };

  const handleBackToSetup = () => {
    setViewState("setup");
    void loadOverview();
  };

  if (!hasSegments) {
    return (
      <div className="rounded-3xl border border-amber-200 dark:border-amber-800 bg-amber-50/70 dark:bg-amber-950/30 p-6 sm:p-8 text-center space-y-3">
        <p className="text-base font-black text-amber-900 dark:text-amber-200">
          Bài nghe chưa có dữ liệu transcript
        </p>
        <p className="text-sm text-amber-800/90 dark:text-amber-200/80 max-w-md mx-auto">
          Tính năng Listening Comprehension Quiz cần transcript của bài học để AI
          tạo câu hỏi và dẫn chứng. Vui lòng thử lại sau khi bài học hoàn tất xử
          lý transcript.
        </p>
        {onBackToDictation && (
          <button
            type="button"
            onClick={onBackToDictation}
            className="inline-flex items-center justify-center rounded-xl bg-amber-500 hover:bg-amber-600 text-white px-4 py-2 text-xs font-black cursor-pointer"
          >
            Quay lại Dictation
          </button>
        )}
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col gap-4 w-full max-w-3xl mx-auto">
      {errorMessage && (
        <div
          role="alert"
          className="rounded-2xl border border-rose-200 dark:border-rose-800 bg-rose-50 dark:bg-rose-950/40 px-4 py-3 flex items-start justify-between gap-3 text-sm font-medium text-rose-700 dark:text-rose-200"
        >
          <div className="flex items-start gap-2.5">
            <AlertCircle className="h-4 w-4 mt-0.5 shrink-0" aria-hidden="true" />
            <span>{errorMessage}</span>
          </div>
          <button
            type="button"
            aria-label="Đóng thông báo lỗi"
            onClick={() => setErrorMessage(null)}
            className="p-0.5 rounded-md hover:bg-rose-100 dark:hover:bg-rose-900/50 cursor-pointer"
          >
            <X className="h-4 w-4" aria-hidden="true" />
          </button>
        </div>
      )}

      {loadingMessage ? (
        <div className="rounded-3xl border border-emerald-200/80 dark:border-emerald-900/60 bg-white dark:bg-slate-900 p-10 flex flex-col items-center justify-center text-center gap-4 shadow-sm">
          <div className="h-14 w-14 rounded-2xl bg-emerald-50 dark:bg-emerald-950/60 border border-emerald-200 dark:border-emerald-800 flex items-center justify-center text-emerald-600 dark:text-emerald-300">
            <Sparkles className="h-6 w-6 animate-pulse" aria-hidden="true" />
          </div>
          <div className="space-y-1">
            <p className="text-base font-black text-slate-900 dark:text-slate-100 flex items-center justify-center gap-2">
              <Loader2 className="h-4 w-4 animate-spin text-emerald-500" aria-hidden="true" />
              <span>{loadingMessage}</span>
            </p>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Quá trình này có thể mất vài giây nếu bộ câu hỏi đang được tạo mới.
            </p>
          </div>
        </div>
      ) : isLoadingOverview && !overview ? (
        <div className="rounded-3xl border border-slate-200/80 dark:border-slate-800 bg-white dark:bg-slate-900 p-10 flex flex-col items-center justify-center gap-3">
          <Loader2 className="h-8 w-8 animate-spin text-emerald-500" aria-hidden="true" />
          <p className="text-sm font-bold text-slate-500 dark:text-slate-400">
            Đang tải thông tin Quiz...
          </p>
        </div>
      ) : viewState === "setup" && overview ? (
        <QuizSetupPanel
          overview={overview}
          selectedMode={selectedMode}
          onSelectMode={setSelectedMode}
          onStartQuiz={(mode) => void handleStartQuiz(mode, false)}
          onResumeAttempt={(attempt) => void handleResumeAttempt(attempt)}
          onRegenerateQuiz={(mode) => void handleRegenerateQuiz(mode)}
          onReviewHistory={(attempt) => void handleReviewHistory(attempt)}
          isSubmitting={Boolean(loadingMessage)}
        />
      ) : viewState === "active" && activeAttempt ? (
        <QuizQuestionView
          key={activeAttempt.attemptId}
          attempt={activeAttempt}
          currentIndex={currentIndex}
          onSelectIndex={setCurrentIndex}
          onSubmitAnswer={handleSubmitAnswer}
          onCompleteAttempt={handleCompleteAttempt}
          onPlayEvidence={onPlayEvidence}
          onPlayFullAudio={onPlayFullAudio}
          onBackToSetup={handleBackToSetup}
          isGrading={isGrading}
          isCompleting={isCompleting}
        />
      ) : (viewState === "summary" || viewState === "review") && activeAttempt ? (
        <QuizSummaryCard
          attempt={activeAttempt}
          isReviewOnly={viewState === "review"}
          regenerateCooldownSeconds={
            overview?.regenerateCooldownSeconds?.[activeAttempt.mode] ?? 0
          }
          onRetrySameQuiz={() => void handleStartQuiz(activeAttempt.mode, true)}
          onRegenerateQuiz={() => void handleRegenerateQuiz(activeAttempt.mode)}
          onBackToSetup={handleBackToSetup}
          onBackToDictation={onBackToDictation}
          onPlayEvidence={onPlayEvidence}
          isBusy={Boolean(loadingMessage)}
        />
      ) : (
        <div className="rounded-3xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-6 text-center space-y-3">
          <p className="text-sm font-semibold text-slate-600 dark:text-slate-300">
            Không thể tải dữ liệu Quiz cho bài học này.
          </p>
          <button
            type="button"
            onClick={() => void loadOverview()}
            className="inline-flex items-center justify-center rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white px-4 py-2 text-xs font-black cursor-pointer"
          >
            Thử lại
          </button>
        </div>
      )}
    </div>
  );
}
