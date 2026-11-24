"use client";

import PracticeHeader from "@/features/practice/components/PracticeHeader";
import PracticeModeSelector from "@/features/practice/components/PracticeModeSelector";
import TranscriptList from "@/features/practice/components/TranscriptList";
import VocabularyDetailPopup from "@/components/vocabulary/VocabularyDetailPopup";
import TranslationReview from "@/features/practice/components/TranslationReview";
import VideoPlayer from "@/features/practice/components/VideoPlayer";
import WordDictation from "@/features/practice/components/WordDictation";
import FillInBlanksDictation, {
  type FillInBlanksDraftState,
  type SegmentBlankCompletePayload,
} from "@/features/practice/components/FillInBlanksDictation";
import FloatingToast from "@/components/ui/FloatingToast";
import { ChevronLeft, ChevronRight, Pause, Play, RotateCcw } from "lucide-react";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";

import {
  type BlankAnswerItem,
  type DictationMode,
  type Lesson,
  Sentence,
  lessonService,
} from "@/features/lesson/services/lessonService";
import { useUserSettings } from "@/features/settings/hooks/useUserSettings";
import { REPLAY_KEY_OPTIONS } from "@/features/settings/types";

interface SegmentStat {
  hintWords: number;
  replayCount: number;
  attempts: number;
}

interface FailedSubmissionPayload {
  mode: DictationMode;
  segmentId: string | number;
  userAnswer: string;
  blankAnswers?: BlankAnswerItem[];
}

function getSegmentScopeKey(mode: DictationMode, segmentId: string | number): string {
  return `${mode}:${segmentId}`;
}

function getProgressStorageKey(lessonId: string, mode: DictationMode): string {
  return `parrotalk_progress_${lessonId}_${mode}`;
}

export default function PracticePage() {
  const params = useParams();
  const router = useRouter();
  const lessonId = params?.lessonId as string;

  const { settings, updateSettings } = useUserSettings();
  const [selectedModeOverride, setSelectedModeOverride] = useState<DictationMode | null>(null);
  const mode: DictationMode = selectedModeOverride ?? settings.defaultDictationMode ?? "FULL";

  const [segments, setSegments] = useState<Sentence[]>([]);
  const [activeIndex, setActiveIndex] = useState(0);
  const [completedIndices, setCompletedIndices] = useState<Set<number>>(new Set());
  const [skippedIndices, setSkippedIndices] = useState<Set<number>>(new Set());
  const [selectedWord, setSelectedWord] = useState<string | null>(null);
  const [inputs, setInputs] = useState<Record<number, string>>({});
  const [blankDrafts, setBlankDrafts] = useState<Record<number, FillInBlanksDraftState>>({});
  const [segmentStats, setSegmentStats] = useState<Record<number, SegmentStat>>({});
  const [fileUrl, setFileUrl] = useState("");
  const [loading, setLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [toastError, setToastError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'practice' | 'transcript'>('practice');
  const [awaitingAdvance, setAwaitingAdvance] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [replayTrigger, setReplayTrigger] = useState(0);
  const speed = settings.playbackSpeed || 1;
  const setSpeed = useCallback<React.Dispatch<React.SetStateAction<number>>>(
    (updater) => {
      const next =
        typeof updater === "function" ? updater(speed) : updater;
      void updateSettings({ playbackSpeed: next });
    },
    [speed, updateSettings]
  );
  const [lessonTitle, setLessonTitle] = useState("");
  const [lessonDuration, setLessonDuration] = useState(0);

  const toggleSpeed = useCallback(() => {
    setSpeed((prev) =>
      prev >= 1.5 ? 0.5 : Math.min(1.5, Math.round((prev + 0.25) * 100) / 100)
    );
  }, [setSpeed]);

  const activeStudySecondsRef = useRef(0);
  const lastFocusedInputRef = useRef<HTMLInputElement | HTMLTextAreaElement | null>(null);
  const lessonCacheRef = useRef<{ lessonId: string; data: Lesson } | null>(null);
  const prevLessonIdRef = useRef<string | null>(null);
  const pendingSubmissionsRef = useRef<Map<string, Promise<void>>>(new Map());
  const failedSubmissionsRef = useRef<Map<string, FailedSubmissionPayload>>(new Map());
  const pendingInteractionsRef = useRef<Map<string, Promise<unknown>>>(new Map());

  const preserveInputFocusOnPointerDown = useCallback(
    (e: React.PointerEvent<HTMLButtonElement>) => {
      const active = document.activeElement;
      if (
        (active instanceof HTMLInputElement || active instanceof HTMLTextAreaElement) &&
        !active.readOnly &&
        !active.disabled
      ) {
        lastFocusedInputRef.current = active;
      } else {
        lastFocusedInputRef.current = null;
      }
      e.preventDefault();
    },
    []
  );

  const runMediaActionPreservingFocus = useCallback((action: () => void) => {
    const target = lastFocusedInputRef.current;
    lastFocusedInputRef.current = null;
    action();
    if (
      target &&
      target.isConnected &&
      !target.readOnly &&
      !target.disabled &&
      document.activeElement !== target
    ) {
      target.focus({ preventScroll: true });
    }
  }, []);

  useEffect(() => {
    lastFocusedInputRef.current = null;
  }, [activeIndex, mode]);

  useEffect(() => {
    const interval = window.setInterval(() => {
      if (!document.hidden) {
        activeStudySecondsRef.current += 1;
      }
    }, 1000);

    return () => window.clearInterval(interval);
  }, []);

  const saveToStorage = useCallback((
    idx: number,
    completed: Set<number>,
    dict: Record<number, string>,
    stats: Record<number, SegmentStat>,
    skipped: Set<number> = new Set(),
    drafts: Record<number, FillInBlanksDraftState> = blankDrafts
  ) => {
    if (!lessonId) return;
    const payload = {
      completed: Array.from(completed),
      skipped: Array.from(skipped),
      inputs: dict,
      blankDrafts: drafts,
      stats: stats,
      activeIndex: idx,
    };
    localStorage.setItem(getProgressStorageKey(lessonId, mode), JSON.stringify(payload));
  }, [blankDrafts, lessonId, mode]);

  const saveProgressToDb = useCallback(async (isFinished: boolean = false) => {
    if (!lessonId) return;
    if (isFinished && (segments.length === 0 || completedIndices.size < segments.length)) {
      setToastError("Vui lòng hoàn thành tất cả các câu trước khi nộp bài.");
      return;
    }
    try {
      if (isFinished) {
        setIsSubmitting(true);
        setToastError(null);
        const modePrefix = `${mode}:`;
        const pendingForMode = Array.from(pendingSubmissionsRef.current.entries())
          .filter(([key]) => key.startsWith(modePrefix))
          .map(([, promise]) => promise);
        if (pendingForMode.length > 0) {
          await Promise.allSettled(pendingForMode);
        }

        // Retry any transiently failed background segment submissions for this mode before final grading
        const failedEntriesForMode = Array.from(failedSubmissionsRef.current.entries())
          .filter(([, payload]) => payload.mode === mode);
        if (failedEntriesForMode.length > 0) {
          const retries = failedEntriesForMode.map(([submissionKey, payload]) =>
            lessonService
              .submitAnswer(lessonId, payload.segmentId, payload.userAnswer, 0, {
                mode: payload.mode,
                ...(payload.blankAnswers
                  ? { blankAnswers: payload.blankAnswers }
                  : {}),
              })
              .then(() => {
                failedSubmissionsRef.current.delete(submissionKey);
              })
              .catch(err => console.warn(`Retry failed for segment ${payload.segmentId}:`, err))
          );
          await Promise.allSettled(retries);
        }

        const stillHasFailedForMode = Array.from(failedSubmissionsRef.current.values()).some(
          (payload) => payload.mode === mode
        );
        if (stillHasFailedForMode) {
          setToastError("Không thể đồng bộ một số câu đã hoàn thành. Vui lòng thử nộp bài lại.");
          return;
        }

        const result = await lessonService.finalLessonSubmit(lessonId, mode);
        // Redirect to result screen
        localStorage.setItem('parrotalk_last_result', JSON.stringify({
          lessonId: lessonId,
          score: result.score,
          passed: result.passed,
          mode,
        }));
        localStorage.removeItem(getProgressStorageKey(lessonId, mode));
        if (mode === "FULL") {
          localStorage.removeItem(`parrotalk_progress_${lessonId}`);
        }
        router.push(`/result`);
      }
    } catch (e) {
      console.error("Failed to save progress", e);
      setToastError("Failed to submit lesson. Please check your network and try again.");
    } finally {
      setIsSubmitting(false);
    }
  }, [completedIndices.size, lessonId, mode, router, segments.length]);

  const consumeStudySeconds = useCallback(() => {
    const seconds = activeStudySecondsRef.current;
    activeStudySecondsRef.current = 0;
    return seconds;
  }, []);

  useEffect(() => {
    if (!lessonId) return;
    let cancelled = false;

    const loadContent = async () => {
      setLoading(true);
      setAwaitingAdvance(false);
      if (prevLessonIdRef.current !== lessonId) {
        prevLessonIdRef.current = lessonId;
        pendingSubmissionsRef.current.clear();
        failedSubmissionsRef.current.clear();
        pendingInteractionsRef.current.clear();
      }

      try {
        const cachedLesson =
          lessonCacheRef.current?.lessonId === lessonId
            ? lessonCacheRef.current.data
            : null;
        const [lessonData, progressData] = cachedLesson
          ? [
              cachedLesson,
              await lessonService.getLessonProgress(lessonId, mode).catch(() => null),
            ]
          : await Promise.all([
              lessonService.getLessonById(lessonId),
              lessonService.getLessonProgress(lessonId, mode).catch(() => null),
            ]);
        if (cancelled) return;

        lessonCacheRef.current = { lessonId, data: lessonData };

        setFileUrl(lessonData.fileUrl || "");
        setLessonTitle(lessonData.title || "Untitled Lesson");
        setLessonDuration(lessonData.duration || 0);
        const sentences = lessonData.segments || [];
        setSegments(sentences);

        // State initialization
        const initialCompleted = new Set<number>();
        const initialInputs: Record<number, string> = {};
        const initialBlankDrafts: Record<number, FillInBlanksDraftState> = {};
        const initialStats: Record<number, SegmentStat> = {};
        let initialActiveIndex = 0;

        // 1. Load from Backend Progress (High Priority)
        if (progressData) {
          progressData.draftSegments?.forEach(draft => {
            const idx = sentences.findIndex(s => String(s.id) === String(draft.segmentId));
            if (idx !== -1) {
              initialInputs[idx] = draft.userAnswer;
              initialStats[idx] = {
                hintWords: draft.hintCount,
                replayCount: draft.replayCount,
                attempts: 1
              };
              if (draft.correct) {
                initialCompleted.add(idx);
              }
            }
          });

          if (progressData.currentSegmentId) {
            const activeIdx = sentences.findIndex(s => String(s.id) === String(progressData.currentSegmentId));
            if (activeIdx !== -1) initialActiveIndex = activeIdx;
          }
        }

        // Restore skipped and mode-scoped drafts from localStorage if present
        // For FULL mode, also fall back to legacy parrotalk_progress_${lessonId} key for backward compatibility
        const initialSkipped = new Set<number>();
        try {
          const modeScopedRaw = localStorage.getItem(getProgressStorageKey(lessonId, mode));
          const legacyRaw =
            !modeScopedRaw && mode === "FULL"
              ? localStorage.getItem(`parrotalk_progress_${lessonId}`)
              : null;
          const cached = modeScopedRaw ?? legacyRaw;
          if (cached) {
            const parsed = JSON.parse(cached);
            if (Array.isArray(parsed.skipped)) {
              parsed.skipped.forEach((idx: number) => {
                if (!initialCompleted.has(idx)) {
                  initialSkipped.add(idx);
                }
              });
            }
            if (parsed.inputs && typeof parsed.inputs === "object") {
              Object.entries(parsed.inputs as Record<string, string>).forEach(([k, v]) => {
                const idx = Number(k);
                if (!Number.isNaN(idx) && initialInputs[idx] === undefined && typeof v === "string") {
                  initialInputs[idx] = v;
                }
              });
            }
            if (parsed.blankDrafts && typeof parsed.blankDrafts === "object") {
              Object.entries(parsed.blankDrafts as Record<string, FillInBlanksDraftState>).forEach(([k, v]) => {
                const idx = Number(k);
                if (!Number.isNaN(idx) && v && typeof v === "object") {
                  initialBlankDrafts[idx] = v;
                }
              });
            }
          }
        } catch (e) {
          console.warn("Could not parse cached progress", e);
        }

        setCompletedIndices(initialCompleted);
        setSkippedIndices(initialSkipped);
        setInputs(initialInputs);
        setBlankDrafts(initialBlankDrafts);
        setSegmentStats(initialStats);
        setActiveIndex(initialActiveIndex);

      } catch (err) {
        console.error("Failed fetching context", err);
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    void loadContent();
    return () => {
      cancelled = true;
    };
  }, [lessonId, mode]);

  const activeSegment = segments[activeIndex] || null;
  const activeSentence = activeSegment?.text || "";

  const handleModeChange = useCallback((nextMode: DictationMode) => {
    setSelectedModeOverride(nextMode);
  }, []);

  const handleInputChange = useCallback((val: string) => {
    setInputs(prev => {
      const newInputs = { ...prev, [activeIndex]: val };
      saveToStorage(activeIndex, completedIndices, newInputs, segmentStats, skippedIndices, blankDrafts);
      return newInputs;
    });
  }, [activeIndex, blankDrafts, completedIndices, segmentStats, skippedIndices, saveToStorage]);

  const handleBlankDraftChange = useCallback((nextDraft: FillInBlanksDraftState) => {
    setBlankDrafts(prev => {
      const updated = { ...prev, [activeIndex]: nextDraft };
      saveToStorage(activeIndex, completedIndices, inputs, segmentStats, skippedIndices, updated);
      return updated;
    });
  }, [activeIndex, completedIndices, inputs, saveToStorage, segmentStats, skippedIndices]);

  const incrementMetric = useCallback((metric: keyof SegmentStat) => {
    const isCompleted = completedIndices.has(activeIndex);

    if (activeSegment?.id && !isCompleted) {
      const segmentId = activeSegment.id;
      const interactionMode = mode;
      const interactionKey = getSegmentScopeKey(interactionMode, segmentId);
      const prevInteraction =
        pendingInteractionsRef.current.get(interactionKey) ?? Promise.resolve();
      const nextInteraction = prevInteraction
        .catch(() => {})
        .then(() => {
          if (metric === 'replayCount') {
            return lessonService.incrementReplay(lessonId, segmentId, interactionMode);
          }
          if (metric === 'hintWords') {
            return lessonService.incrementHint(lessonId, segmentId, interactionMode);
          }
        })
        .catch((err) => {
          console.warn(`Failed to increment ${metric} for segment ${segmentId}:`, err);
        });
      pendingInteractionsRef.current.set(interactionKey, nextInteraction);
    }

    setSegmentStats(prev => {
      const current = prev[activeIndex] || { hintWords: 0, replayCount: 0, attempts: 1 };
      const nextStats = { ...prev, [activeIndex]: { ...current, [metric]: current[metric] + 1 } };
      saveToStorage(activeIndex, completedIndices, inputs, nextStats, skippedIndices, blankDrafts);
      return nextStats;
    });
  }, [activeSegment?.id, activeIndex, blankDrafts, completedIndices, inputs, lessonId, mode, saveToStorage, skippedIndices]);

  const handleVideoReplay = useCallback(() => {
    incrementMetric("replayCount");
  }, [incrementMetric]);

  const handleSentenceComplete = useCallback((payload?: SegmentBlankCompletePayload) => {
    const alreadyCompleted = completedIndices.has(activeIndex);

    const newCompleted = new Set(completedIndices);
    newCompleted.add(activeIndex);
    setCompletedIndices(newCompleted);

    const newSkipped = new Set(skippedIndices);
    newSkipped.delete(activeIndex);
    setSkippedIndices(newSkipped);

    const userAnswer = payload?.userAnswer ?? (inputs[activeIndex] || "");
    const nextInputs =
      payload?.userAnswer !== undefined
        ? { ...inputs, [activeIndex]: payload.userAnswer }
        : inputs;
    if (payload?.userAnswer !== undefined) {
      setInputs(nextInputs);
    }

    setAwaitingAdvance(true);
    saveToStorage(activeIndex, newCompleted, nextInputs, segmentStats, newSkipped, blankDrafts);

    // Save to backend in background (non-blocking)
    if (!alreadyCompleted && activeSegment?.id) {
      const segmentId = activeSegment.id;
      const submitMode = mode;
      const submissionKey = getSegmentScopeKey(submitMode, segmentId);
      const studySeconds = consumeStudySeconds();
      const blankAnswers = payload?.blankAnswers;

      let submitPromise: Promise<void> | null = null;
      submitPromise = (async () => {
        try {
          const pendingInteraction = pendingInteractionsRef.current.get(submissionKey);
          if (pendingInteraction) {
            await pendingInteraction;
          }
          await lessonService.submitAnswer(
            lessonId,
            segmentId,
            userAnswer,
            studySeconds,
            {
              mode: submitMode,
              ...(blankAnswers ? { blankAnswers } : {}),
            }
          );
          failedSubmissionsRef.current.delete(submissionKey);
        } catch (e) {
          console.error(`Failed to submit background answer for segment ${segmentId}:`, e);
          failedSubmissionsRef.current.set(submissionKey, {
            mode: submitMode,
            segmentId,
            userAnswer,
            ...(blankAnswers ? { blankAnswers } : {}),
          });
        } finally {
          if (pendingSubmissionsRef.current.get(submissionKey) === submitPromise) {
            pendingSubmissionsRef.current.delete(submissionKey);
          }
        }
      })();
      pendingSubmissionsRef.current.set(submissionKey, submitPromise);
    }
  }, [activeIndex, activeSegment, blankDrafts, completedIndices, consumeStudySeconds, inputs, lessonId, mode, saveToStorage, segmentStats, skippedIndices]);

  const handleAdvanceAfterReview = useCallback(async () => {
    if (!awaitingAdvance) return;

    setAwaitingAdvance(false);

    if (completedIndices.size === segments.length && segments.length > 0) {
      void saveProgressToDb(true);
      return;
    }

    if (activeIndex >= segments.length - 1) {
      const firstUncompleted = segments.findIndex((_, idx) => !completedIndices.has(idx));
      if (firstUncompleted !== -1) {
        setActiveIndex(firstUncompleted);
        saveToStorage(firstUncompleted, completedIndices, inputs, segmentStats, skippedIndices, blankDrafts);
      }
      return;
    }

    const nextIndex = activeIndex + 1;
    setActiveIndex(nextIndex);
    saveToStorage(nextIndex, completedIndices, inputs, segmentStats, skippedIndices, blankDrafts);
  }, [activeIndex, awaitingAdvance, blankDrafts, completedIndices, inputs, saveProgressToDb, saveToStorage, segmentStats, segments, skippedIndices]);

  useEffect(() => {
    if (!awaitingAdvance || !settings.dictationAutoAdvance) {
      return;
    }
    if (activeIndex >= segments.length - 1) {
      return;
    }
    const timer = setTimeout(() => {
      void handleAdvanceAfterReview();
    }, 350);
    return () => clearTimeout(timer);
  }, [
    awaitingAdvance,
    settings.dictationAutoAdvance,
    activeIndex,
    segments.length,
    handleAdvanceAfterReview,
  ]);

  const handleSelectSentence = useCallback((index: number, skippedOverride?: Set<number>) => {
    setAwaitingAdvance(false);
    setActiveIndex(index);
    saveToStorage(index, completedIndices, inputs, segmentStats, skippedOverride ?? skippedIndices, blankDrafts);
  }, [blankDrafts, completedIndices, inputs, segmentStats, skippedIndices, saveToStorage]);

  const handleNext = useCallback(() => {
    if (awaitingAdvance) {
      void handleAdvanceAfterReview();
      return;
    }

    let nextSkipped = skippedIndices;
    if (!completedIndices.has(activeIndex)) {
      nextSkipped = new Set(skippedIndices);
      nextSkipped.add(activeIndex);
      setSkippedIndices(nextSkipped);
      saveToStorage(activeIndex, completedIndices, inputs, segmentStats, nextSkipped, blankDrafts);
    }

    if (activeIndex < segments.length - 1) {
      handleSelectSentence(activeIndex + 1, nextSkipped);
    } else {
      const firstUncompleted = segments.findIndex((_, idx) => !completedIndices.has(idx) && idx !== activeIndex);
      if (firstUncompleted !== -1) {
        handleSelectSentence(firstUncompleted, nextSkipped);
      }
    }
  }, [activeIndex, awaitingAdvance, blankDrafts, completedIndices, handleAdvanceAfterReview, handleSelectSentence, inputs, saveToStorage, segmentStats, segments, skippedIndices]);

  const handlePrevious = useCallback(() => {
    if (activeIndex > 0) {
      handleSelectSentence(activeIndex - 1);
    }
  }, [activeIndex, handleSelectSentence]);

  useEffect(() => {
    if (!awaitingAdvance) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Enter") {
        event.preventDefault();
        void handleAdvanceAfterReview();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [awaitingAdvance, handleAdvanceAfterReview]);


  const totalSentences = segments.length;
  const currentSentence = activeIndex + 1;
  const percent = totalSentences > 0 ? Math.round((completedIndices.size / totalSentences) * 100) : 0;
  const uncompletedIndices = segments.map((_, i) => i).filter(i => !completedIndices.has(i));
  const uncompletedCount = uncompletedIndices.length;
  const isAllCompleted = completedIndices.size === totalSentences && totalSentences > 0;
  const replayKeyLabel =
    REPLAY_KEY_OPTIONS.find((k) => k.value === settings.dictationReplayKey)
      ?.shortLabel ?? "Esc";

  if (loading || isSubmitting) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center font-bold text-gray-500 gap-4 animate-pulse">
        <div className="w-12 h-12 border-4 border-gray-200 border-t-green-500 rounded-full animate-spin"></div>
        {isSubmitting ? 'Scoring learning results...' : 'Loading lesson context...'}
      </div>
    );
  }

  return (
    <div className="flex flex-col overflow-hidden h-[100dvh] overscroll-none">
      <PracticeHeader
        currentSentence={currentSentence}
        totalSentences={totalSentences}
        percent={percent}
        mode={mode}
        onModeChange={handleModeChange}
        modeDisabled={isSubmitting}
      />

      <div className="flex-1 overflow-hidden relative flex flex-col min-h-0">
        {/* Slim Unified Mobile Control Bar */}
        <div className="md:hidden flex items-center justify-between gap-2 px-3 py-2 border-b border-gray-100 dark:border-slate-800 bg-white dark:bg-slate-900 shrink-0">
          <PracticeModeSelector
            mode={mode}
            onChange={handleModeChange}
            disabled={isSubmitting}
          />
          <div
            role="tablist"
            aria-label="Practice view"
            className="inline-flex rounded-xl bg-ink-100/80 dark:bg-slate-800 p-0.5"
          >
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === 'practice'}
              onClick={() => setActiveTab('practice')}
              className={`px-3 py-1.5 min-h-[36px] [@media(pointer:coarse)]:min-h-[44px] rounded-lg text-[11px] font-extrabold transition-all cursor-pointer ${
                activeTab === 'practice'
                  ? 'bg-white dark:bg-slate-900 text-emerald-600 dark:text-emerald-300 shadow-xs'
                  : 'text-ink-500 dark:text-slate-400 hover:text-ink-800 dark:hover:text-slate-200'
              }`}
            >
              Practice
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === 'transcript'}
              onClick={() => setActiveTab('transcript')}
              className={`px-3 py-1.5 min-h-[36px] [@media(pointer:coarse)]:min-h-[44px] rounded-lg text-[11px] font-extrabold transition-all cursor-pointer ${
                activeTab === 'transcript'
                  ? 'bg-white dark:bg-slate-900 text-emerald-600 dark:text-emerald-300 shadow-xs'
                  : 'text-ink-500 dark:text-slate-400 hover:text-ink-800 dark:hover:text-slate-200'
              }`}
            >
              Transcript
            </button>
          </div>
        </div>

        {skippedIndices.size > 0 && (
          <div className="px-3 sm:px-6 lg:px-8 pt-2.5 shrink-0">
            <div className="bg-amber-50/90 dark:bg-amber-950/40 border border-amber-200/80 dark:border-amber-800/60 rounded-xl px-3 py-2 flex items-center justify-between gap-2">
              <div className="flex items-center gap-2 min-w-0">
                <span
                  className="w-2 h-2 rounded-full bg-amber-500 shrink-0"
                  aria-hidden="true"
                />
                <span className="text-xs font-semibold text-amber-900 dark:text-amber-200 truncate">
                  {skippedIndices.size}{" "}
                  {skippedIndices.size === 1 ? "sentence" : "sentences"} skipped in this session
                </span>
              </div>
              <button
                type="button"
                onClick={() => {
                  const firstSkipped = Math.min(...Array.from(skippedIndices));
                  handleSelectSentence(firstSkipped);
                  setActiveTab('practice');
                }}
                className="inline-flex items-center gap-1.5 px-2.5 py-1 min-h-[32px] [@media(pointer:coarse)]:min-h-[44px] rounded-lg bg-amber-100 hover:bg-amber-200/80 dark:bg-amber-900/60 dark:hover:bg-amber-900 text-amber-900 dark:text-amber-100 text-xs font-bold transition-colors shrink-0 cursor-pointer"
              >
                <RotateCcw size={13} aria-hidden="true" />
                <span>Review</span>
              </button>
            </div>
          </div>
        )}

        <div className="flex-1 p-3 sm:p-6 lg:p-8 flex flex-col md:flex-row gap-5 lg:gap-8 items-stretch relative overflow-y-auto md:overflow-hidden min-h-0">

          {/* Left Column: Player and Controls */}
          <div className={`
            w-full md:w-[320px] lg:w-[360px] flex-shrink-0 transition-all duration-300 md:sticky md:top-0 h-fit
            ${activeTab === 'practice' ? 'block' : 'hidden md:block'}
          `}>
            <VideoPlayer
              src={fileUrl}
              activeSegment={activeSegment}
              onReplay={handleVideoReplay}
              isPlaying={isPlaying}
              setIsPlaying={setIsPlaying}
              replayTrigger={replayTrigger}
              speed={speed}
              setSpeed={setSpeed}
              lessonTitle={lessonTitle}
              lessonDuration={lessonDuration}
              totalSegments={segments.length}
            />
          </div>

          {/* Right Column: Transcript */}
          <div className={`
            flex-1 min-w-0 h-full overflow-y-auto custom-scrollbar md:pr-4 pb-20 sm:pb-32 w-full
            ${activeTab === 'transcript' ? 'block' : 'hidden md:block'}
          `}>

            <TranscriptList
              segments={segments}
              activeIndex={activeIndex}
              completedIndices={completedIndices}
              skippedIndices={skippedIndices}
              inputs={inputs}
              mode={mode}
              onSelectSentence={(idx) => {
                handleSelectSentence(idx);
                if (activeTab === 'transcript') setActiveTab('practice');
              }}
              onWordClick={setSelectedWord}
            />
          </div>
        </div>
      </div>


      {activeSegment ? (
        awaitingAdvance ? (
          <TranslationReview
            key={`${mode}-${activeIndex}`}
            sentence={activeSentence}
            translatedText={activeSegment.translation?.translatedText}
            showTranslationByDefault={settings.dictationShowTranslation}
            onContinue={() => void handleAdvanceAfterReview()}
            onWordClick={setSelectedWord}
            isLastSegment={activeIndex >= segments.length - 1}
            isAllCompleted={isAllCompleted}
            uncompletedCount={uncompletedCount}
            onSubmitLesson={() => void saveProgressToDb(true)}
            onReviewSkipped={() => {
              setAwaitingAdvance(false);
              const firstUncompleted = segments.findIndex((_, idx) => !completedIndices.has(idx));
              if (firstUncompleted !== -1) {
                handleSelectSentence(firstUncompleted);
              }
            }}
          />
        ) : (
          <div className="relative bg-white dark:bg-slate-900 border-t border-gray-100 dark:border-slate-800 z-10 shrink-0 shadow-[0_-8px_30px_rgba(0,0,0,0.04)] py-2 px-3 sm:py-4 sm:px-4">
            <div className="flex flex-col gap-3">
              {/* Practice Toolbar (Both Desktop and Mobile) */}
              <div className="flex items-center justify-between gap-2 w-full max-w-2xl mx-auto">
                {/* Left: Prev + Replay */}
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onPointerDown={(e) => e.preventDefault()}
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={handlePrevious}
                    disabled={activeIndex === 0}
                    title="Previous sentence"
                    aria-label="Previous sentence"
                    className="px-2.5 py-1.5 sm:px-3 min-h-[36px] [@media(pointer:coarse)]:min-h-[44px] bg-gray-50 dark:bg-slate-800 text-gray-600 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-700 rounded-xl font-black text-[10px] sm:text-xs uppercase tracking-wider border border-gray-200/80 dark:border-slate-700 flex items-center justify-center gap-1 disabled:opacity-30 active:scale-95 transition-all cursor-pointer"
                  >
                    <ChevronLeft size={16} aria-hidden="true" />
                    <span className="hidden sm:inline">Prev</span>
                  </button>
                  <button
                    type="button"
                    onPointerDown={preserveInputFocusOnPointerDown}
                    onPointerCancel={() => {
                      lastFocusedInputRef.current = null;
                    }}
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() => runMediaActionPreservingFocus(() => setReplayTrigger(prev => prev + 1))}
                    title={`Replay audio (${replayKeyLabel})`}
                    aria-label="Replay audio"
                    className="px-2.5 py-1.5 sm:px-3 min-h-[36px] [@media(pointer:coarse)]:min-h-[44px] bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 hover:bg-emerald-100 dark:hover:bg-emerald-900/60 rounded-xl font-black text-[10px] sm:text-xs uppercase tracking-wider border border-emerald-200/80 dark:border-emerald-800 flex items-center justify-center gap-1 active:scale-95 transition-all shadow-xs cursor-pointer"
                  >
                    <RotateCcw size={16} aria-hidden="true" />
                    <span className="hidden sm:inline">Replay</span>
                  </button>
                </div>

                {/* Center: Play/Pause + Speed pill */}
                <div className="bg-emerald-50/80 dark:bg-emerald-950/50 border border-emerald-200/70 dark:border-emerald-800/60 rounded-full p-1 flex items-center gap-1">
                  <button
                    type="button"
                    onPointerDown={preserveInputFocusOnPointerDown}
                    onPointerCancel={() => {
                      lastFocusedInputRef.current = null;
                    }}
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() => runMediaActionPreservingFocus(() => setIsPlaying(prev => !prev))}
                    title={isPlaying ? "Pause audio" : "Play audio"}
                    aria-label={isPlaying ? "Pause audio" : "Play audio"}
                    className="bg-emerald-500 hover:bg-emerald-600 text-white rounded-full w-9 h-9 [@media(pointer:coarse)]:w-11 [@media(pointer:coarse)]:h-11 [@media(pointer:coarse)]:min-h-[44px] flex items-center justify-center active:scale-95 transition-all shadow-xs cursor-pointer"
                  >
                    {isPlaying ? <Pause size={16} aria-hidden="true" /> : <Play size={16} aria-hidden="true" />}
                  </button>
                  <button
                    type="button"
                    onPointerDown={preserveInputFocusOnPointerDown}
                    onPointerCancel={() => {
                      lastFocusedInputRef.current = null;
                    }}
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() => runMediaActionPreservingFocus(toggleSpeed)}
                    title="Change speed"
                    aria-label={`Playback speed: ${speed}x`}
                    className="px-2.5 py-1 min-h-[36px] [@media(pointer:coarse)]:min-h-[44px] text-emerald-700 dark:text-emerald-300 hover:bg-emerald-100/80 dark:hover:bg-emerald-900/60 rounded-full font-black text-xs tracking-wider flex items-center justify-center active:scale-95 transition-all cursor-pointer tabular-nums"
                  >
                    {speed}x
                  </button>
                </div>

                {/* Right: Mobile counter badge + Next */}
                <div className="flex items-center gap-2">
                  <span className="sm:hidden tabular-nums text-xs font-bold text-ink-500 dark:text-slate-400 px-2 py-1 rounded-full bg-ink-50 dark:bg-slate-800">
                    {currentSentence}/{totalSentences}
                  </span>
                  <button
                    type="button"
                    onPointerDown={(e) => e.preventDefault()}
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={handleNext}
                    disabled={activeIndex >= segments.length - 1 && !segments.some((_, idx) => !completedIndices.has(idx) && idx !== activeIndex)}
                    title="Next sentence"
                    aria-label="Next sentence"
                    className="px-2.5 py-1.5 sm:px-3 min-h-[36px] [@media(pointer:coarse)]:min-h-[44px] bg-gray-50 dark:bg-slate-800 text-gray-600 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-700 rounded-xl font-black text-[10px] sm:text-xs uppercase tracking-wider border border-gray-200/80 dark:border-slate-700 flex items-center justify-center gap-1 disabled:opacity-30 active:scale-95 transition-all cursor-pointer"
                  >
                    <span className="hidden sm:inline">Next</span>
                    <ChevronRight size={16} aria-hidden="true" />
                  </button>
                </div>
              </div>

              {mode === "FULL" ? (
                <WordDictation
                  sentence={activeSentence}
                  fullInput={inputs[activeIndex] || ""}
                  onInputChange={handleInputChange}
                  onSentenceComplete={() => handleSentenceComplete()}
                  onHintUsed={() => incrementMetric('hintWords')}
                  isCompleted={completedIndices.has(activeIndex)}
                />
              ) : (
                <FillInBlanksDictation
                  key={`${mode}-${activeSegment.id ?? activeIndex}`}
                  sentence={activeSentence}
                  mode={mode}
                  blanks={activeSegment.blanks?.[mode] ?? []}
                  suitableForBlanks={activeSegment.suitableForBlanks !== false}
                  isCompleted={completedIndices.has(activeIndex)}
                  draftState={blankDrafts[activeIndex]}
                  fallbackInput={inputs[activeIndex] || ""}
                  onFallbackInputChange={handleInputChange}
                  onDraftChange={handleBlankDraftChange}
                  onSegmentComplete={handleSentenceComplete}
                  onHintUsed={() => incrementMetric('hintWords')}
                />
              )}

              <div className="hidden md:flex items-center justify-center gap-4 text-[11px] text-ink-400 dark:text-slate-400 pt-1.5">
                <span className="inline-flex items-center gap-1.5">
                  <kbd className="px-1.5 py-0.5 rounded border border-ink-200 dark:border-slate-700 bg-ink-50 dark:bg-slate-800 font-mono text-[10px] font-bold text-ink-600 dark:text-slate-300">
                    {replayKeyLabel}
                  </kbd>
                  <span>Replay</span>
                </span>
                {mode !== "FULL" && (
                  <span className="inline-flex items-center gap-1.5">
                    <kbd className="px-1.5 py-0.5 rounded border border-ink-200 dark:border-slate-700 bg-ink-50 dark:bg-slate-800 font-mono text-[10px] font-bold text-ink-600 dark:text-slate-300">
                      Alt+H
                    </kbd>
                    <span>Hint</span>
                  </span>
                )}
                <span className="inline-flex items-center gap-1.5">
                  <kbd className="px-1.5 py-0.5 rounded border border-ink-200 dark:border-slate-700 bg-ink-50 dark:bg-slate-800 font-mono text-[10px] font-bold text-ink-600 dark:text-slate-300">
                    Enter
                  </kbd>
                  <span>{mode === "FULL" ? "Continue" : "Next / Check"}</span>
                </span>
              </div>
            </div>
          </div>
        )
      ) : null}
      {selectedWord && (
        <VocabularyDetailPopup 
          word={selectedWord} 
          onClose={() => setSelectedWord(null)} 
        />
      )}
      {toastError && (
        <FloatingToast
          message={toastError}
          variant="error"
          onClose={() => setToastError(null)}
        />
      )}
    </div>
  );
}
