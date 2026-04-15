"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import PracticeHeader from "@/components/practice/PracticeHeader";
import VideoPlayer from "@/components/practice/VideoPlayer";
import TranscriptList from "@/components/practice/TranscriptList";
import WordDictation from "@/components/practice/WordDictation";
import { Save, CheckCircle, ChevronLeft, ChevronRight } from "lucide-react";

import { lessonService, Sentence, SubmitLessonRequest, SegmentResultRequest } from "@/lib/services/lessonService";

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
      const payload = getPayload(isFinished);
      const result = await lessonService.submitLesson(jobId, payload);
      setLastSaved(new Date());
      if (isFinished) {
        // Redirect to result screen
        localStorage.removeItem(`parrotalk_progress_${jobId}`);
        router.push(`/result?lessonId=${jobId}&score=${result.score}&passed=${result.passed}`);
      }
    } catch (e) {
      console.error("Failed to save progress", e);
    }
  }, [jobId, router]);

  useEffect(() => {
    if (!jobId) return;

    const loadContent = async () => {
      try {
        const [jobData, resultData] = await Promise.all([
          lessonService.getLessonById(jobId),
          lessonService.getLessonResult(jobId),
        ]);

        setFileUrl(jobData.fileUrl || "");
        const sentences = resultData.sentences || [];
        setSegments(sentences);

        const storedStr = localStorage.getItem(`parrotalk_progress_${jobId}`);
        if (storedStr) {
          try {
            const data = JSON.parse(storedStr);
            if (data.completed) setCompletedIndices(new Set(data.completed));
            if (data.inputs) setInputs(data.inputs);
            if (data.stats) setSegmentStats(data.stats);
            if (typeof data.activeIndex === "number" && data.activeIndex < sentences.length) {
              setActiveIndex(data.activeIndex);
            }
          } catch (e) {
            console.error("Local storage parse err", e);
          }
        }
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
      saveProgressToDb(false);
    }, 60000);
    return () => clearInterval(interval);
  }, [saveProgressToDb]);

  const activeSegment = segments[activeIndex] || null;
  const activeSentence = activeSegment?.text || "";

  const handleInputChange = (val: string) => {
    setInputs(prev => {
      const newInputs = { ...prev, [activeIndex]: val };
      saveToStorage(activeIndex, completedIndices, newInputs, segmentStats);
      return newInputs;
    });
  };

  const incrementMetric = (metric: keyof SegmentStat) => {
    setSegmentStats(prev => {
      const current = prev[activeIndex] || { hintWords: 0, replayCount: 0, attempts: 1 };
      const nextStats = { ...prev, [activeIndex]: { ...current, [metric]: current[metric] + 1 } };
      saveToStorage(activeIndex, completedIndices, inputs, nextStats);
      return nextStats;
    });
  };

  const handleSentenceComplete = () => {
    const alreadyCompleted = completedIndices.has(activeIndex);
    let nextIndex = activeIndex;
    const newCompleted = new Set(completedIndices);
    newCompleted.add(activeIndex);
    setCompletedIndices(newCompleted);

    if (!alreadyCompleted && activeIndex < segments.length - 1) {
      nextIndex = activeIndex + 1;
      setActiveIndex(nextIndex);
    } else if (!alreadyCompleted && activeIndex === segments.length - 1) {
      // Lesson fully complete!
      setIsSubmitting(true);
      stateRef.current.completedIndices = newCompleted; // force latest state internally
      saveProgressToDb(true);
    }

    saveToStorage(nextIndex, newCompleted, inputs, segmentStats);
  };

  const handleSelectSentence = (index: number) => {
    setActiveIndex(index);
    saveToStorage(index, completedIndices, inputs, segmentStats);
  };

  const handleNext = () => {
    if (activeIndex < segments.length - 1) {
      handleSelectSentence(activeIndex + 1);
    }
  };

  const handlePrevious = () => {
    if (activeIndex > 0) {
      handleSelectSentence(activeIndex - 1);
    }
  };


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
    <div className="flex flex-col h-[100dvh] overflow-hidden overscroll-none">
      <PracticeHeader
        currentSentence={currentSentence}
        totalSentences={totalSentences}
        percent={percent}
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

        <div className="flex-1 p-4 sm:p-8 flex flex-col md:flex-row gap-6 lg:gap-8 items-start relative overflow-hidden min-h-0">

          {/* Left Column: Player and Controls */}
          <div className={`
            w-full md:w-[320px] lg:w-[360px] flex-shrink-0 transition-all duration-300
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
            flex-1 min-w-0 overflow-y-auto custom-scrollbar md:pr-4 pb-20 sm:pb-32 w-full
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


      {/* Bottom Area: Dictation Input */}
      {activeSegment && (
        <div className="shrink-0 bg-white shadow-[0_-4px_25px_-5px_rgba(0,0,0,0.05)] z-10 relative">
          <WordDictation
            sentence={activeSentence}
            fullInput={inputs[activeIndex] || ""}
            onInputChange={handleInputChange}
            onSentenceComplete={handleSentenceComplete}
            onHintUsed={() => incrementMetric('hintWords')}
          />
        </div>
      )}

      <style jsx global>{`
        .custom-scrollbar::-webkit-scrollbar { width: 6px; }
        .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background: #e5e7eb; border-radius: 10px; }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #d1d5db; }
      `}</style>
    </div>
  );
}
