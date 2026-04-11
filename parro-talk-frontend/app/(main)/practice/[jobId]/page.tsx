"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import PracticeHeader from "@/components/practice/PracticeHeader";
import VideoPlayer from "@/components/practice/VideoPlayer";
import TranscriptList from "@/components/practice/TranscriptList";
import WordDictation from "@/components/practice/WordDictation";

import { lessonService, Sentence } from "@/lib/services/lessonService";

export default function PracticePage() {
  const params = useParams();
  const jobId = params?.jobId as string;

  const [segments, setSegments] = useState<Sentence[]>([]);
  const [activeIndex, setActiveIndex] = useState(0);
  const [completedIndices, setCompletedIndices] = useState<Set<number>>(new Set());
  const [inputs, setInputs] = useState<Record<number, string>>({});
  const [fileUrl, setFileUrl] = useState("");
  const [loading, setLoading] = useState(true);

  const saveToStorage = (idx: number, completed: Set<number>, dict: Record<number, string>) => {
    if (!jobId) return;
    const payload = {
      completed: Array.from(completed),
      inputs: dict,
      activeIndex: idx,
    };
    localStorage.setItem(`parrotalk_progress_${jobId}`, JSON.stringify(payload));
  };

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

  const activeSegment = segments[activeIndex] || null;
  const activeSentence = activeSegment?.text || "";

  const handleInputChange = (val: string) => {
    setInputs(prev => {
      const newInputs = { ...prev, [activeIndex]: val };
      saveToStorage(activeIndex, completedIndices, newInputs);
      return newInputs;
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
      console.log("Lesson completed!");
    }

    saveToStorage(nextIndex, newCompleted, inputs);
  };

  const handleSelectSentence = (index: number) => {
    setActiveIndex(index);
    saveToStorage(index, completedIndices, inputs);
  };

  if (loading) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center font-bold text-gray-500 gap-4 animate-pulse">
        <div className="w-12 h-12 border-4 border-gray-200 border-t-green-500 rounded-full animate-spin"></div>
        Loading lesson context...
      </div>
    );
  }

  return (
    <div className="flex flex-col h-screen overflow-hidden">
      <PracticeHeader />

      {/* Main Content Area */}
      <div className="flex-1 overflow-hidden">
        <div className="h-full p-8 flex flex-col lg:flex-row gap-8 items-start">

          {/* Left Column: Player and Controls - Fixed */}
          <div className="w-full lg:w-[360px] flex-shrink-0 lg:sticky lg:top-0">
            <VideoPlayer src={fileUrl} activeSegment={activeSegment} />
          </div>

          {/* Right Column: Transcript - Scrollable */}
          <div className="flex-1 min-w-0 h-full overflow-y-auto custom-scrollbar pr-4 pb-32 pt-2">
            <TranscriptList
              segments={segments}
              activeIndex={activeIndex}
              completedIndices={completedIndices}
              inputs={inputs}
              onSelectSentence={handleSelectSentence}
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
