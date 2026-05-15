"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import PracticeHeader from "@/features/practice/components/PracticeHeader";
import VideoPlayer from "@/features/practice/components/VideoPlayer";
import TranscriptList from "@/features/practice/components/TranscriptList";
import WordDictation from "@/features/practice/components/WordDictation";
import { Save, CheckCircle, ChevronLeft, ChevronRight } from "lucide-react";

import { lessonService, Sentence, SubmitLessonRequest, SegmentResultRequest } from "@/features/lesson/services/lessonService";

interface SegmentStat {
  hintWords: number;
  replayCount: number;
  attempts: number;
}

export default function PracticePage() {
  const params = useParams();
  const router = useRouter();
  const jobId = params?.jobId as string;

  const [segments, setSegments] = useState<Sentence[]>([]);
  const [activeIndex, setActiveIndex] = useState(0);
  const [completedIndices, setCompletedIndices] = useState<Set<number>>(new Set());
  const [inputs, setInputs] = useState<Record<number, string>>({});
  const [segmentStats, setSegmentStats] = useState<Record<number, SegmentStat>>({});
  const [fileUrl, setFileUrl] = useState("");
  const [loading, setLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [lastSaved, setLastSaved] = useState<Date | null>(null);
  const [activeTab, setActiveTab] = useState<'practice' | 'transcript'>('practice');


  // Use refs for latest state during intervals
  const stateRef = useRef({ segments, completedIndices, inputs, segmentStats, activeIndex });
  useEffect(() => {
    stateRef.current = { segments, completedIndices, inputs, segmentStats, activeIndex };
  }, [segments, completedIndices, inputs, segmentStats, activeIndex]);

  const saveToStorage = (idx: number, completed: Set<number>, dict: Record<number, string>, stats: Record<number, SegmentStat>) => {
    if (!jobId) return;
    const payload = {
      completed: Array.from(completed),
      inputs: dict,
      stats: stats,
      activeIndex: idx,
    };
    localStorage.setItem(`parrotalk_progress_${jobId}`, JSON.stringify(payload));
  };

  const getPayload = (isFinished: boolean): SubmitLessonRequest => {
    const { segments, inputs, segmentStats } = stateRef.current;
    const segmentResults: SegmentResultRequest[] = segments.map((seg, idx) => {
      const stat = segmentStats[idx] || { hintWords: 0, replayCount: 0, attempts: 1 };
      return {
        segmentId: seg.id || idx,
        hintWords: stat.hintWords,
        replayCount: stat.replayCount,
        attempts: stat.attempts,
        userAnswer: inputs[idx] || ""
      };
    });
    return { segmentResults, isFinished };
  };

  const saveProgressToDb = useCallback(async (isFinished: boolean = false) => {
    if (!jobId) return;
    try {
      if (isFinished) {
        setIsSubmitting(true);
        const result = await lessonService.finalLessonSubmit(jobId);
        setLastSaved(new Date());
        // Redirect to result screen
        localStorage.setItem('parrotalk_last_result', JSON.stringify({
          lessonId: jobId,
          score: result.score,
          passed: result.passed
        }));
        localStorage.removeItem(`parrotalk_progress_${jobId}`);
        router.push(`/result`);
      }
    } catch (e) {
      console.error("Failed to save progress", e);
    } finally {
      setIsSubmitting(false);
    }
  }, [jobId, router]);

  useEffect(() => {
    if (!jobId) return;

    const loadContent = async () => {
      try {
        const [jobData, progressData] = await Promise.all([
          lessonService.getLessonById(jobId),
          lessonService.getLessonProgress(jobId).catch(() => null),
        ]);

        console.log("Job data: ", jobData);
        console.log("Progress data: ", progressData);
        setFileUrl(jobData.fileUrl || "");
        const sentences = jobData.segments || [];
        setSegments(sentences);

        // State initialization
        let initialCompleted = new Set<number>();
        let initialInputs: Record<number, string> = {};
        let initialStats: Record<number, SegmentStat> = {};
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

        setCompletedIndices(initialCompleted);
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
  }, [jobId]);

  // Auto-save every 1 minute
  useEffect(() => {
    const interval = setInterval(() => {
      // Periodic full sync can still be useful
      // saveProgressToDb(false);
    }, 60000);
    return () => clearInterval(interval);
  }, [saveProgressToDb]);

  const activeSegment = segments[activeIndex] || null;
  const activeSentence = activeSegment?.text || "";

  const handleInputChange = useCallback((val: string) => {
    setInputs(prev => {
      const newInputs = { ...prev, [activeIndex]: val };
      saveToStorage(activeIndex, completedIndices, newInputs, segmentStats);
      return newInputs;
    });
  }, [activeIndex, completedIndices, segmentStats, saveToStorage]);

  const incrementMetric = useCallback((metric: keyof SegmentStat) => {
    const isCompleted = completedIndices.has(activeIndex);

    if (activeSegment?.id && !isCompleted) {
      if (metric === 'replayCount') lessonService.incrementReplay(jobId, activeSegment.id);
      if (metric === 'hintWords') lessonService.incrementHint(jobId, activeSegment.id);
    }

    setSegmentStats(prev => {
      const current = prev[activeIndex] || { hintWords: 0, replayCount: 0, attempts: 1 };
      const nextStats = { ...prev, [activeIndex]: { ...current, [metric]: current[metric] + 1 } };
      saveToStorage(activeIndex, completedIndices, inputs, nextStats);
      return nextStats;
    });
  }, [activeSegment?.id, activeIndex, completedIndices, inputs, jobId, saveToStorage]);

  const handleSentenceComplete = useCallback(async () => {
    console.log("handleSentenceComplete");
    const alreadyCompleted = completedIndices.has(activeIndex);
    const isFullCompletion = !alreadyCompleted && (completedIndices.size + 1 === segments.length);

    if (isFullCompletion) {
      setIsSubmitting(true);
    }

    const newCompleted = new Set(completedIndices);
    newCompleted.add(activeIndex);
    setCompletedIndices(newCompleted);

    let nextIndex = activeIndex;
    if (!alreadyCompleted && activeIndex < segments.length - 1) {
      nextIndex = activeIndex + 1;
      setActiveIndex(nextIndex);
    }


    // Save to backend immediately
    if (!alreadyCompleted && activeSegment?.id) {
      try {
        if (newCompleted.size === segments.length) {
          await lessonService.submitAnswer(jobId, activeSegment.id, inputs[activeIndex] || "");
        } else {
          lessonService.submitAnswer(jobId, activeSegment.id, inputs[activeIndex] || "");
        }
      } catch (e) {
        console.error("Failed to submit individual answer", e);
      }
    }

    saveToStorage(nextIndex, newCompleted, inputs, segmentStats);

    // Auto-submit if all sentences are completed
    if (newCompleted.size === segments.length && segments.length > 0) {
      await saveProgressToDb(true);
    }
  }, [activeIndex, activeSegment, completedIndices, inputs, jobId, segments.length, saveToStorage, saveProgressToDb]);

  const handleSelectSentence = useCallback((index: number) => {
    setActiveIndex(index);
    saveToStorage(index, completedIndices, inputs, segmentStats);
  }, [completedIndices, inputs, segmentStats, saveToStorage]);

  const handleNext = useCallback(() => {
    if (activeIndex < segments.length - 1) {
      handleSelectSentence(activeIndex + 1);
    }
  }, [activeIndex, handleSelectSentence, segments.length]);

  const handlePrevious = useCallback(() => {
    if (activeIndex > 0) {
      handleSelectSentence(activeIndex - 1);
    }
  }, [activeIndex, handleSelectSentence]);


  if (loading || isSubmitting) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center font-bold text-gray-500 gap-4 animate-pulse">
        <div className="w-12 h-12 border-4 border-gray-200 border-t-green-500 rounded-full animate-spin"></div>
        {isSubmitting ? 'Scoring learning results...' : 'Loading lesson context...'}
      </div>
    );
  }

  const totalSentences = segments.length;
  const currentSentence = activeIndex + 1;
  const percent = totalSentences > 0 ? Math.round((completedIndices.size / totalSentences) * 100) : 0;

  return (
    <div className="flex flex-col overflow-hidden h-[100dvh] overscroll-none">
      <PracticeHeader
        currentSentence={currentSentence}
        totalSentences={totalSentences}
        percent={percent}
        onFinish={() => saveProgressToDb(true)}
        isAllCompleted={completedIndices.size === totalSentences && totalSentences > 0}
      />


      <div className="flex-1 overflow-hidden relative flex flex-col min-h-0">
        {/* Mobile Tabs */}
        <div className="md:hidden flex border-b border-gray-100 bg-white shrink-0">
          <button
            onClick={() => setActiveTab('practice')}
            className={`flex-1 py-4 text-[10px] font-black uppercase tracking-widest transition-all ${activeTab === 'practice' ? 'text-green-500 border-b-2 border-green-500 bg-green-50/30' : 'text-gray-400'}`}
          >
            Practice
          </button>
          <button
            onClick={() => setActiveTab('transcript')}
            className={`flex-1 py-4 text-[10px] font-black uppercase tracking-widest transition-all ${activeTab === 'transcript' ? 'text-green-500 border-b-2 border-green-500 bg-green-50/30' : 'text-gray-400'}`}
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
              onSave={() => saveProgressToDb(false)}
              onNext={handleNext}
              onPrevious={handlePrevious}
              hasNext={activeIndex < segments.length - 1}
              hasPrevious={activeIndex > 0}
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
              inputs={inputs}
              onSelectSentence={(idx) => {
                handleSelectSentence(idx);
                if (activeTab === 'transcript') setActiveTab('practice');
              }}
            />
          </div>
        </div>
      </div>


      {activeSegment && (
        <div className="shrink-0 bg-white shadow-[0_-8px_30px_rgba(0,0,0,0.04)] z-10 relative">
          <WordDictation
            sentence={activeSentence}
            fullInput={inputs[activeIndex] || ""}
            onInputChange={handleInputChange}
            onSentenceComplete={handleSentenceComplete}
            onHintUsed={() => incrementMetric('hintWords')}
            isCompleted={completedIndices.has(activeIndex)}
          />
        </div>
      )}

    </div>
  );
}
