"use client";

import { use, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Sidebar from "@/components/practice/Sidebar";
import PracticeHeader from "@/components/practice/PracticeHeader";
import VideoPlayer from "@/components/practice/VideoPlayer";
import TranscriptList from "@/components/practice/TranscriptList";
import WordDictation from "@/components/practice/WordDictation";

interface Segment {
  start: number;
  end: number;
  text: string;
}

export default function PracticePage() {
  const params = useParams();
  const jobId = params?.jobId as string;

  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  
  const [segments, setSegments] = useState<Segment[]>([]);
  const [activeIndex, setActiveIndex] = useState(0);
  const [completedIndices, setCompletedIndices] = useState<Set<number>>(new Set());
  const [inputs, setInputs] = useState<Record<number, string>>({});
  const [fileUrl, setFileUrl] = useState("");
  const [loading, setLoading] = useState(true);

  // Helper Configuration Payload caching
  const saveToStorage = (idx: number, completed: Set<number>, dict: Record<number, string>) => {
      if (!jobId) return;
      const payload = {
          completed: Array.from(completed),
          inputs: dict,
          activeIndex: idx
      };
      localStorage.setItem(`parrotalk_progress_${jobId}`, JSON.stringify(payload));
  };

  // Initial Fetches
  useEffect(() => {
    if (!jobId) return;
    
    // We fetch logic in parallel
    const fetchJob = fetch(`http://localhost:8080/api/jobs/${jobId}`).then(res => res.json());
    const fetchResult = fetch(`http://localhost:8080/api/jobs/${jobId}/result`).then(res => res.json());

    Promise.all([fetchJob, fetchResult])
        .then(([jobData, resultData]) => {
            setFileUrl(jobData.fileUrl || "");
            const sentences = resultData.sentences || [];
            setSegments(sentences);
            
            // Check LocalStorage for saved progress sequence
            const storedStr = localStorage.getItem(`parrotalk_progress_${jobId}`);
            if (storedStr) {
                try {
                    const data = JSON.parse(storedStr);
                    if (data.completed) setCompletedIndices(new Set(data.completed));
                    if (data.inputs) setInputs(data.inputs);
                    if (typeof data.activeIndex === 'number' && data.activeIndex < sentences.length) {
                        setActiveIndex(data.activeIndex);
                    }
                } catch (e) {
                    console.error("Local storage parse err", e);
                }
            }
            setLoading(false);
        }).catch(err => {
            console.error("Failed fetching context", err);
            setLoading(false);
        })
  }, [jobId]);

  const activeSegment = segments[activeIndex] || null;
  const activeSentence = activeSegment?.text || "";

  // Dictation logic progression
  const handleInputChange = (val: string) => {
     setInputs(prev => {
        const newInputs = { ...prev, [activeIndex]: val };
        saveToStorage(activeIndex, completedIndices, newInputs);
        return newInputs;
     });
  };

  const handleSentenceComplete = () => {
     let nextIndex = activeIndex;
     const newCompleted = new Set(completedIndices);
     newCompleted.add(activeIndex);
     setCompletedIndices(newCompleted);

     if (activeIndex < segments.length - 1) {
         nextIndex = activeIndex + 1;
         setActiveIndex(nextIndex);
     } else {
         console.log("Lesson completed!");
     }
     saveToStorage(nextIndex, newCompleted, inputs);
  };

  const handleSelectSentence = (index: number) => {
      setActiveIndex(index);
      saveToStorage(index, completedIndices, inputs);
  };

  if (loading) {
     return <div className="min-h-screen bg-[#FDFDFD] flex flex-col items-center justify-center font-bold text-gray-500 gap-4 animate-pulse"><div className="w-12 h-12 border-4 border-gray-200 border-t-green-500 rounded-full animate-spin"></div>Loading lesson context...</div>
  }

  return (
    <div className="flex min-h-screen bg-[#FDFDFD] selection:bg-green-100">
      <Sidebar isCollapsed={isSidebarCollapsed} onToggle={() => setIsSidebarCollapsed(!isSidebarCollapsed)} />
      
      <div className="flex-1 flex flex-col h-screen overflow-hidden">
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
      </div>

      <style jsx global>{`
        .custom-scrollbar::-webkit-scrollbar {
          width: 6px;
        }
        .custom-scrollbar::-webkit-scrollbar-track {
          background: transparent;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb {
          background: #e5e7eb;
          border-radius: 10px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover {
          background: #d1d5db;
        }
      `}</style>
    </div>
  );
}
