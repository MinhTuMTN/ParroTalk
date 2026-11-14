"use client";

import PracticeHeader from "@/features/practice/components/PracticeHeader";
import TranscriptList from "@/features/practice/components/TranscriptList";
import VocabularyDetailPopup from "@/components/vocabulary/VocabularyDetailPopup";
import TranslationReview from "@/features/practice/components/TranslationReview";
import VideoPlayer from "@/features/practice/components/VideoPlayer";
import WordDictation from "@/features/practice/components/WordDictation";
import ConfirmationModal from "@/components/ui/ConfirmationModal";
import FloatingToast from "@/components/ui/FloatingToast";
import { ChevronLeft, ChevronRight, Pause, Play, RotateCcw, SkipForward } from "lucide-react";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";

import { Sentence, lessonService } from "@/features/lesson/services/lessonService";
import { useUserSettings } from "@/features/settings/hooks/useUserSettings";
import { REPLAY_KEY_OPTIONS } from "@/features/settings/types";

interface SegmentStat {
  hintWords: number;
  replayCount: number;
  attempts: number;
}

export default function PracticePage() {
  const params = useParams();
  const router = useRouter();
  const lessonId = params?.lessonId as string;

  const [segments, setSegments] = useState<Sentence[]>([]);
  const [activeIndex, setActiveIndex] = useState(0);
  const [completedIndices, setCompletedIndices] = useState<Set<number>>(new Set());
  const [skippedIndices, setSkippedIndices] = useState<Set<number>>(new Set());
  const [isConfirmSubmitOpen, setIsConfirmSubmitOpen] = useState(false);
  const [selectedWord, setSelectedWord] = useState<string | null>(null);
  const [inputs, setInputs] = useState<Record<number, string>>({});
  const [segmentStats, setSegmentStats] = useState<Record<number, SegmentStat>>({});
  const [fileUrl, setFileUrl] = useState("");
  const [loading, setLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [toastError, setToastError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'practice' | 'transcript'>('practice');
  const { settings, updateSettings } = useUserSettings();
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
  const pendingSubmissionsRef = useRef<Map<string | number, Promise<void>>>(new Map());
  const failedSubmissionsRef = useRef<Map<string | number, string>>(new Map());

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
    skipped: Set<number> = new Set()
  ) => {
    if (!lessonId) return;
    const payload = {
      completed: Array.from(completed),
      skipped: Array.from(skipped),
      inputs: dict,
      stats: stats,
      activeIndex: idx,
    };
    localStorage.setItem(`parrotalk_progress_${lessonId}`, JSON.stringify(payload));
  }, [lessonId]);

  const saveProgressToDb = useCallback(async (isFinished: boolean = false) => {
    if (!lessonId) return;
    try {
      if (isFinished) {
        setIsSubmitting(true);
        setToastError(null);
        if (pendingSubmissionsRef.current.size > 0) {
          await Promise.allSettled(Array.from(pendingSubmissionsRef.current.values()));
        }

        // Retry any transiently failed background segment submissions before final grading
        if (failedSubmissionsRef.current.size > 0) {
          const retries = Array.from(failedSubmissionsRef.current.entries()).map(
            ([segId, ansText]) =>
              lessonService.submitAnswer(lessonId, segId, ansText, 0)
                .then(() => failedSubmissionsRef.current.delete(segId))
                .catch(err => console.warn(`Retry failed for segment ${segId}:`, err))
          );
          await Promise.allSettled(retries);
        }

        const result = await lessonService.finalLessonSubmit(lessonId);
        // Redirect to result screen
        localStorage.setItem('parrotalk_last_result', JSON.stringify({
          lessonId: lessonId,
          score: result.score,
          passed: result.passed
        }));
        localStorage.removeItem(`parrotalk_progress_${lessonId}`);
        router.push(`/result`);
      }
    } catch (e) {
      console.error("Failed to save progress", e);
      setToastError("Failed to submit lesson. Please check your network and try again.");
    } finally {
      setIsSubmitting(false);
    }
  }, [lessonId, router]);

  const consumeStudySeconds = useCallback(() => {
    const seconds = activeStudySecondsRef.current;
    activeStudySecondsRef.current = 0;
    return seconds;
  }, []);

  useEffect(() => {
    if (!lessonId) return;

    const loadContent = async () => {
      try {
        const [lessonData, progressData] = await Promise.all([
          lessonService.getLessonById(lessonId),
          lessonService.getLessonProgress(lessonId).catch(() => null),
        ]);

        setFileUrl(lessonData.fileUrl || "");
        setLessonTitle(lessonData.title || "Untitled Lesson");
        setLessonDuration(lessonData.duration || 0);
        const sentences = lessonData.segments || [];
        setSegments(sentences);

        // State initialization
        const initialCompleted = new Set<number>();
        const initialInputs: Record<number, string> = {};
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

        // Restore skipped from localStorage if present
        const initialSkipped = new Set<number>();
        try {
          const cached = localStorage.getItem(`parrotalk_progress_${lessonId}`);
          if (cached) {
            const parsed = JSON.parse(cached);
            if (Array.isArray(parsed.skipped)) {
              parsed.skipped.forEach((idx: number) => {
                if (!initialCompleted.has(idx)) {
                  initialSkipped.add(idx);
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
        setSegmentStats(initialStats);
        setActiveIndex(initialActiveIndex);

      } catch (err) {
        console.error("Failed fetching context", err);
      } finally {
        setLoading(false);
      }
    };

    loadContent();
  }, [lessonId]);

  const activeSegment = segments[activeIndex] || null;
  const activeSentence = activeSegment?.text || "";

  const handleInputChange = useCallback((val: string) => {
    setInputs(prev => {
      const newInputs = { ...prev, [activeIndex]: val };
      saveToStorage(activeIndex, completedIndices, newInputs, segmentStats, skippedIndices);
      return newInputs;
    });
  }, [activeIndex, completedIndices, segmentStats, skippedIndices, saveToStorage]);

  const incrementMetric = useCallback((metric: keyof SegmentStat) => {
    const isCompleted = completedIndices.has(activeIndex);

    if (activeSegment?.id && !isCompleted) {
      if (metric === 'replayCount') lessonService.incrementReplay(lessonId, activeSegment.id);
      if (metric === 'hintWords') lessonService.incrementHint(lessonId, activeSegment.id);
    }

    setSegmentStats(prev => {
      const current = prev[activeIndex] || { hintWords: 0, replayCount: 0, attempts: 1 };
      const nextStats = { ...prev, [activeIndex]: { ...current, [metric]: current[metric] + 1 } };
      saveToStorage(activeIndex, completedIndices, inputs, nextStats, skippedIndices);
      return nextStats;
    });
  }, [activeSegment?.id, activeIndex, completedIndices, inputs, lessonId, saveToStorage, skippedIndices]);

  const handleSentenceComplete = useCallback(() => {
    const alreadyCompleted = completedIndices.has(activeIndex);

    const newCompleted = new Set(completedIndices);
    newCompleted.add(activeIndex);
    setCompletedIndices(newCompleted);

    const newSkipped = new Set(skippedIndices);
    newSkipped.delete(activeIndex);
    setSkippedIndices(newSkipped);

    setAwaitingAdvance(true);
    saveToStorage(activeIndex, newCompleted, inputs, segmentStats, newSkipped);

    // Save to backend in background
    if (!alreadyCompleted && activeSegment?.id) {
      const segmentId = activeSegment.id;
      const userAnswer = inputs[activeIndex] || "";
      const studySeconds = consumeStudySeconds();

      const submitPromise = lessonService
        .submitAnswer(lessonId, segmentId, userAnswer, studySeconds)
        .then(() => {
          failedSubmissionsRef.current.delete(segmentId);
        })
        .catch((e) => {
          console.error(`Failed to submit background answer for segment ${segmentId}:`, e);
          failedSubmissionsRef.current.set(segmentId, userAnswer);
        })
        .finally(() => {
          pendingSubmissionsRef.current.delete(segmentId);
        });
      pendingSubmissionsRef.current.set(segmentId, submitPromise);
    }
  }, [activeIndex, activeSegment, completedIndices, consumeStudySeconds, inputs, lessonId, saveToStorage, segmentStats, skippedIndices]);

  const handleAdvanceAfterReview = useCallback(async () => {
    if (!awaitingAdvance) return;

    const isLastSegment = activeIndex >= segments.length - 1;
    setAwaitingAdvance(false);

    if (isLastSegment) {
      const uncompletedRemaining = segments.map((_, i) => i).filter(i => !completedIndices.has(i));
      if (uncompletedRemaining.length === 0) {
        await saveProgressToDb(true);
        return;
      }
      setIsConfirmSubmitOpen(true);
      return;
    }

    const nextIndex = activeIndex + 1;
    setActiveIndex(nextIndex);
    saveToStorage(nextIndex, completedIndices, inputs, segmentStats, skippedIndices);
  }, [activeIndex, awaitingAdvance, completedIndices, inputs, saveProgressToDb, saveToStorage, segmentStats, segments, skippedIndices]);

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

  const handleSelectSentence = useCallback((index: number) => {
    setAwaitingAdvance(false);
    setActiveIndex(index);
    saveToStorage(index, completedIndices, inputs, segmentStats, skippedIndices);
  }, [completedIndices, inputs, segmentStats, skippedIndices, saveToStorage]);

  const handleSkipSegment = useCallback(() => {
    if (awaitingAdvance) return;

    const nextSkipped = new Set(skippedIndices);
    if (!completedIndices.has(activeIndex)) {
      nextSkipped.add(activeIndex);
      setSkippedIndices(nextSkipped);
      saveToStorage(activeIndex, completedIndices, inputs, segmentStats, nextSkipped);
    }

    if (activeIndex < segments.length - 1) {
      handleSelectSentence(activeIndex + 1);
    } else {
      const firstUncompleted = segments.findIndex((_, idx) => !completedIndices.has(idx) && idx !== activeIndex);
      if (firstUncompleted !== -1) {
        handleSelectSentence(firstUncompleted);
      }
    }
  }, [activeIndex, awaitingAdvance, completedIndices, handleSelectSentence, inputs, saveToStorage, segmentStats, segments, skippedIndices]);

  const handleNext = useCallback(() => {
    if (awaitingAdvance) {
      void handleAdvanceAfterReview();
      return;
    }
    if (activeIndex < segments.length - 1) {
      handleSelectSentence(activeIndex + 1);
    }
  }, [activeIndex, awaitingAdvance, handleAdvanceAfterReview, handleSelectSentence, segments.length]);

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
  const hasCompletedLastSegment = totalSentences > 0 && completedIndices.has(totalSentences - 1);
  const isAllCompleted = completedIndices.size === totalSentences && totalSentences > 0;
  const canSubmit = isAllCompleted || (hasCompletedLastSegment && completedIndices.size > 0);

  const handleRequestSubmit = useCallback(() => {
    if (uncompletedCount > 0) {
      setIsConfirmSubmitOpen(true);
    } else {
      void saveProgressToDb(true);
    }
  }, [saveProgressToDb, uncompletedCount]);

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
        onFinish={handleRequestSubmit}
        isAllCompleted={isAllCompleted}
        canSubmit={canSubmit}
      />


      <div className="flex-1 overflow-hidden relative flex flex-col min-h-0">
        {/* Mobile Tabs */}
        <div className="md:hidden flex border-b border-gray-100 dark:border-slate-800 bg-white dark:bg-slate-900 shrink-0">
          <button
            onClick={() => setActiveTab('practice')}
            className={`flex-1 py-4 text-[10px] font-black uppercase tracking-widest transition-all ${activeTab === 'practice' ? 'text-green-500 border-b-2 border-green-500 bg-green-50/30 dark:bg-emerald-950/30' : 'text-gray-400 dark:text-slate-400'}`}
          >
            Practice
          </button>
          <button
            onClick={() => setActiveTab('transcript')}
            className={`flex-1 py-4 text-[10px] font-black uppercase tracking-widest transition-all ${activeTab === 'transcript' ? 'text-green-500 border-b-2 border-green-500 bg-green-50/30 dark:bg-emerald-950/30' : 'text-gray-400 dark:text-slate-400'}`}
          >
            Transcript
          </button>
        </div>

        <div className="flex-1 p-4 sm:p-8 flex flex-col md:flex-row gap-6 lg:gap-8 items-stretch relative overflow-y-auto md:overflow-hidden min-h-0">

          {/* Left Column: Player and Controls */}
          <div className={`
            w-full md:w-[320px] lg:w-[360px] flex-shrink-0 transition-all duration-300 md:sticky md:top-0 h-fit
            ${activeTab === 'practice' ? 'block' : 'hidden md:block'}
          `}>
            <VideoPlayer
              src={fileUrl}
              activeSegment={activeSegment}
              onReplay={() => incrementMetric('replayCount')}
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
            key={activeIndex}
            sentence={activeSentence}
            translatedText={activeSegment.translation?.translatedText}
            showTranslationByDefault={settings.dictationShowTranslation}
            onContinue={() => void handleAdvanceAfterReview()}
            onWordClick={setSelectedWord}
            isLastSegment={activeIndex >= segments.length - 1}
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
          <div className="relative bg-white dark:bg-slate-900 border-t border-gray-100 dark:border-slate-800 z-10 shrink-0 shadow-[0_-8px_30px_rgba(0,0,0,0.04)] py-3 px-4 sm:py-4">
            <div className="flex flex-col gap-3">
              {/* Practice Toolbar (Both Desktop and Mobile) */}
              <div className="flex items-center justify-between gap-2 w-full max-w-2xl mx-auto">
                <div className="flex items-center gap-1.5 sm:gap-2">
                  <button
                    type="button"
                    onClick={handlePrevious}
                    disabled={activeIndex === 0}
                    title="Previous sentence"
                    aria-label="Previous sentence"
                    className="p-2 sm:px-3 sm:py-2 bg-gray-50 dark:bg-slate-800 text-gray-600 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-700 rounded-xl font-black text-[10px] sm:text-xs uppercase tracking-wider border border-gray-200/80 dark:border-slate-700 flex items-center justify-center gap-1 disabled:opacity-30 active:scale-95 transition-all cursor-pointer"
                  >
                    <ChevronLeft size={16} />
                    <span className="hidden sm:inline">Prev</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setReplayTrigger(prev => prev + 1)}
                    title={`Replay audio (${
                      REPLAY_KEY_OPTIONS.find(
                        (k) => k.value === settings.dictationReplayKey
                      )?.shortLabel ?? "Esc"
                    })`}
                    aria-label="Replay audio"
                    className="p-2 sm:px-3 sm:py-2 bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 hover:bg-emerald-100 dark:hover:bg-emerald-900/60 rounded-xl font-black text-[10px] sm:text-xs uppercase tracking-wider border border-emerald-200/80 dark:border-emerald-800 flex items-center justify-center gap-1 active:scale-95 transition-all shadow-xs cursor-pointer"
                  >
                    <RotateCcw size={16} />
                    <span className="hidden sm:inline">Replay</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setIsPlaying(prev => !prev)}
                    title={isPlaying ? "Pause audio" : "Play audio"}
                    aria-label={isPlaying ? "Pause audio" : "Play audio"}
                    className="p-2 sm:px-3 sm:py-2 bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 hover:bg-emerald-100 dark:hover:bg-emerald-900/60 rounded-xl font-black text-[10px] sm:text-xs uppercase tracking-wider border border-emerald-200/80 dark:border-emerald-800 flex items-center justify-center gap-1 active:scale-95 transition-all shadow-xs cursor-pointer"
                  >
                    {isPlaying ? <Pause size={16} /> : <Play size={16} />}
                  </button>
                  <button
                    type="button"
                    onClick={toggleSpeed}
                    title="Change speed"
                    aria-label={`Playback speed: ${speed}x`}
                    className="p-2 sm:px-3 sm:py-2 bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 hover:bg-emerald-100 dark:hover:bg-emerald-900/60 rounded-xl font-black text-[10px] sm:text-xs uppercase tracking-wider border border-emerald-200/80 dark:border-emerald-800 flex items-center justify-center gap-1 active:scale-95 transition-all shadow-xs cursor-pointer"
                  >
                    {speed}x
                  </button>
                </div>

                <div className="flex items-center gap-1.5 sm:gap-2">
                  <button
                    type="button"
                    onClick={handleSkipSegment}
                    title="Skip this sentence and do it later"
                    aria-label="Skip sentence"
                    className="px-2.5 py-2 sm:px-3.5 sm:py-2 bg-amber-50 dark:bg-amber-950/50 text-amber-800 dark:text-amber-200 hover:bg-amber-100 dark:hover:bg-amber-900/50 rounded-xl font-black text-[10px] sm:text-xs uppercase tracking-wider border border-amber-200/90 dark:border-amber-800 flex items-center justify-center gap-1.5 active:scale-95 transition-all shadow-xs cursor-pointer"
                  >
                    <SkipForward size={15} />
                    <span>Skip</span>
                  </button>
                  <button
                    type="button"
                    onClick={handleNext}
                    disabled={activeIndex >= segments.length - 1}
                    title="Next sentence"
                    aria-label="Next sentence"
                    className="p-2 sm:px-3 sm:py-2 bg-gray-50 dark:bg-slate-800 text-gray-600 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-700 rounded-xl font-black text-[10px] sm:text-xs uppercase tracking-wider border border-gray-200/80 dark:border-slate-700 flex items-center justify-center gap-1 disabled:opacity-30 active:scale-95 transition-all cursor-pointer"
                  >
                    <span className="hidden sm:inline">Next</span>
                    <ChevronRight size={16} />
                  </button>
                </div>
              </div>

              <WordDictation
                sentence={activeSentence}
                fullInput={inputs[activeIndex] || ""}
                onInputChange={handleInputChange}
                onSentenceComplete={handleSentenceComplete}
                onHintUsed={() => incrementMetric('hintWords')}
                isCompleted={completedIndices.has(activeIndex)}
              />
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
      <ConfirmationModal
        isOpen={isConfirmSubmitOpen}
        onClose={() => {
          setIsConfirmSubmitOpen(false);
          const firstUncompleted = segments.findIndex((_, idx) => !completedIndices.has(idx));
          if (firstUncompleted !== -1) {
            handleSelectSentence(firstUncompleted);
          }
        }}
        onConfirm={() => {
          setIsConfirmSubmitOpen(false);
          void saveProgressToDb(true);
        }}
        title="Submit Lesson?"
        message={`You have completed the last sentence, but have ${uncompletedCount} skipped sentence(s). Would you like to submit your lesson now?`}
        confirmText="Submit Anyway"
        cancelText="Review Skipped"
      />
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
