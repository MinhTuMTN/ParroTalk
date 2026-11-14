"use client";

import { getDictationMatching } from "@/lib/utils";
import { tokenizeText } from "@/lib/tokenizer";
import { CheckCircle2, RotateCcw } from "lucide-react";
import { useEffect, useRef } from "react";

interface Segment {
  start: number;
  end: number;
  text: string;
}

interface TranscriptListProps {
  segments: Segment[];
  activeIndex: number;
  completedIndices: Set<number>;
  skippedIndices?: Set<number>;
  inputs: Record<number, string>;
  onSelectSentence: (index: number) => void;
  onWordClick?: (word: string) => void;
}

export default function TranscriptList({
  segments,
  activeIndex,
  completedIndices,
  skippedIndices,
  inputs,
  onSelectSentence,
  onWordClick
}: TranscriptListProps) {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const activeElement = document.getElementById(`segment-${activeIndex}`);
    if (activeElement) {
      activeElement.scrollIntoView({
        behavior: "smooth",
        block: "center",
      });
    }
  }, [activeIndex]);

  const maskText = (text: string) => {
    return text.replace(/[a-zA-Z0-9]/g, "•");
  };

  const renderInputText = (input: string, truth: string) => {
    const matching = getDictationMatching(input || "", truth);
    return matching.map((match, idx) => (
      <span key={idx} className={match.isMatched ? "text-gray-800 dark:text-slate-100" : "text-gray-300 dark:text-slate-600"}>
        {match.displayString.replace(/\*/g, "•")}{" "}
      </span>
    ));
  };

  const renderCompletedText = (text: string) => {
    const tokens = tokenizeText(text);
    return tokens.map((token, i) => {
      if (token.isWord) {
        return (
          <span 
            key={i} 
            onClick={(e) => {
               e.stopPropagation();
               onWordClick?.(token.text);
            }}
            className="cursor-pointer hover:text-emerald-600 transition-colors border-b border-transparent hover:border-emerald-600 pb-0.5"
          >
            {token.text}
          </span>
        );
      }
      return <span key={i}>{token.text}</span>;
    });
  };

  const skippedIndicesList = segments
    .map((_, i) => i)
    .filter(i => !completedIndices.has(i) && (skippedIndices?.has(i) || i < activeIndex));
  const firstSkippedIndex = skippedIndicesList[0];
  const skippedCount = skippedIndicesList.length;

  return (
    <div ref={containerRef} className="flex-1 flex flex-col gap-4 w-full">
      <div className="flex items-center justify-between pb-2">
        <h2 className="text-lg font-black text-gray-800 dark:text-slate-100">Session Transcript</h2>
      </div>

      {skippedCount > 0 && firstSkippedIndex !== undefined && (
        <div className="bg-amber-50/90 dark:bg-amber-950/40 border border-amber-200/80 dark:border-amber-800/70 rounded-2xl p-3 flex items-center justify-between gap-3 text-xs shadow-xs">
          <div className="flex items-center gap-2 text-amber-900 dark:text-amber-200 font-semibold">
            <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse" />
            <span>{skippedCount} {skippedCount === 1 ? "sentence" : "sentences"} skipped</span>
          </div>
          <button
            type="button"
            onClick={() => onSelectSentence(firstSkippedIndex)}
            className="px-2.5 py-1 bg-white dark:bg-slate-800 hover:bg-amber-100 dark:hover:bg-amber-900/50 text-amber-800 dark:text-amber-200 font-bold rounded-lg border border-amber-200 dark:border-amber-800 shadow-xs transition-all text-[11px] flex items-center gap-1 cursor-pointer active:scale-95"
          >
            <RotateCcw size={12} />
            Review
          </button>
        </div>
      )}

      <div className="flex flex-col gap-3 pb-8">
        {segments.map((segment, index) => {
          const isCompleted = completedIndices.has(index);
          const isActive = index === activeIndex;
          const isSkipped = !isCompleted && (skippedIndices?.has(index) || index < activeIndex);
          const timeFormat = new Date(Math.round(segment.start) * 1000).toISOString().substring(14, 19);

          let containerStyle = "bg-transparent border-transparent hover:bg-gray-50/50 dark:hover:bg-slate-800/50 opacity-40 hover:opacity-100";
          if (isActive) {
            containerStyle = "bg-white dark:bg-slate-900 border-green-500 shadow-xl shadow-green-50 dark:shadow-none scale-[1.02] z-10";
          } else if (isCompleted) {
            containerStyle = "bg-white/80 dark:bg-slate-900/80 border-emerald-100 dark:border-emerald-900/60 opacity-90 hover:opacity-100";
          } else if (isSkipped) {
            containerStyle = "bg-amber-50/40 dark:bg-amber-950/30 border-amber-200/70 dark:border-amber-800/60 opacity-80 hover:opacity-100";
          }

          return (
            <div
              key={index}
              id={`segment-${index}`}
              onClick={() => onSelectSentence(index)}
              className={`p-4 rounded-2xl border transition-all relative duration-300 cursor-pointer ${containerStyle}`}
            >
              {isActive && (
                <div className="absolute left-[-2px] inset-y-6 w-1 bg-green-500 rounded-full" />
              )}

              <div className="flex flex-col gap-2">
                <div className="flex items-center gap-2">
                  {isActive && (
                    <span className="text-[9px] font-black text-green-500 uppercase tracking-widest mr-1">Active • </span>
                  )}
                  {isCompleted && (
                    <span className="text-[9px] font-black text-emerald-600 dark:text-emerald-300 uppercase tracking-widest bg-emerald-50 dark:bg-emerald-950/60 border border-emerald-200 dark:border-emerald-800 px-1.5 py-0.5 rounded-md flex items-center gap-1">
                      <CheckCircle2 size={10} /> Done
                    </span>
                  )}
                  {isSkipped && !isActive && (
                    <span className="text-[9px] font-black text-amber-700 dark:text-amber-300 uppercase tracking-widest bg-amber-100/70 dark:bg-amber-950/60 border border-amber-300 dark:border-amber-800 px-1.5 py-0.5 rounded-md flex items-center gap-1">
                      <RotateCcw size={10} /> Skipped
                    </span>
                  )}
                  <span className="text-[9px] font-black text-gray-400 dark:text-slate-500 uppercase tracking-widest">{timeFormat}</span>
                </div>
                <p className={`text-[15px] leading-relaxed transition-all ${isActive ? "font-bold text-gray-900 dark:text-slate-100" : isCompleted ? "font-medium text-gray-800 dark:text-slate-200" : isSkipped ? "font-medium text-amber-900 dark:text-amber-200" : "font-medium text-gray-500 dark:text-slate-400"}`}>
                  {isCompleted
                    ? renderCompletedText(segment.text)
                    : (isActive || inputs[index])
                      ? renderInputText(inputs[index] || "", segment.text)
                      : maskText(segment.text)}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
