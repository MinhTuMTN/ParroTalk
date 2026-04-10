"use client";

import { useMemo } from "react";
import { getDictationMatching } from "@/lib/utils";

interface Segment {
  start: number;
  end: number;
  text: string;
}

interface TranscriptListProps {
  segments: Segment[];
  activeIndex: number;
  completedIndices: Set<number>;
  inputs: Record<number, string>;
  onSelectSentence: (index: number) => void;
}

export default function TranscriptList({ segments, activeIndex, completedIndices, inputs, onSelectSentence }: TranscriptListProps) {
  
  const maskText = (text: string) => {
    return text.replace(/[a-zA-Z0-9]/g, "•");
  };

  const renderInputText = (input: string, truth: string) => {
    const matching = getDictationMatching(input || "", truth);
    return matching.map((match, idx) => (
        <span key={idx} className={match.isMatched ? "text-gray-800" : "text-gray-300"}>
            {match.displayString.replace(/\*/g, "•")}{" "}
        </span>
    ));
  };

  return (
    <div className="flex-1 flex flex-col gap-4 w-full">
      <div className="flex items-center justify-between pb-2">
          <h2 className="text-lg font-black text-gray-800">Session Transcript</h2>
          <div className="flex bg-gray-50 p-1 rounded-xl gap-0.5">
                <button className="px-4 py-1.5 rounded-lg text-[9px] font-black bg-green-500 text-white shadow-sm shadow-green-100">Practice</button>
                <button className="px-4 py-1.5 rounded-lg text-[9px] font-black text-gray-400 hover:text-gray-600">Review</button>
          </div>
      </div>

      <div className="flex flex-col gap-3 pb-8">
        {segments.map((segment, index) => {
          const isCompleted = completedIndices.has(index);
          const isActive = index === activeIndex;
          const status = isActive ? "active" : isCompleted ? "completed" : "pending";
          const timeFormat = new Date(Math.round(segment.start) * 1000).toISOString().substring(14, 19);

          return (
            <div 
                key={index}
                onClick={() => onSelectSentence(index)}
                className={`
                    p-4 rounded-2xl border transition-all relative duration-500 cursor-pointer
                    ${status === "active" 
                        ? "bg-white border-green-500 shadow-xl shadow-green-50 scale-[1.02] z-10" 
                        : "bg-transparent border-transparent hover:bg-gray-50/50 opacity-40 hover:opacity-100"}
                `}
            >
                {status === "active" && (
                    <div className="absolute left-[-2px] inset-y-6 w-1 bg-green-500 rounded-full" />
                )}
                
                <div className="flex flex-col gap-2">
                <div className="flex items-center gap-2">
                    {status === "active" && (
                        <span className="text-[9px] font-black text-green-500 uppercase tracking-widest mr-1">Active • </span>
                    )}
                    <span className="text-[9px] font-black text-gray-400 uppercase tracking-widest">{timeFormat}</span>
                </div>
                <p className={`text-[15px] leading-relaxed transition-all ${status === "active" ? "font-bold text-gray-900" : "font-medium text-gray-500"}`}>
                    {status === "completed" 
                        ? segment.text 
                        : status === "active" 
                            ? renderInputText(inputs[index] || "", segment.text) 
                            : maskText(segment.text)}
                </p>
                </div>
            </div>
          )
        })}
      </div>
    </div>
  );
}
