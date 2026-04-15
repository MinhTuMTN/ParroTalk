"use client";

import { cleanWord, getDictationMatching } from "@/lib/utils";
import { Keyboard } from "lucide-react";
import { useMemo, useRef, useEffect } from "react";

// Use a simple basic cleaner for full string validation equivalence
const cleanStringForMatch = (str: string) => str.replace(/[^\w\s\']/g, "").trim().toLowerCase();

interface WordDictationProps {
  sentence: string;
  fullInput: string;
  onInputChange: (val: string) => void;
  onSentenceComplete: () => void;
  onHintUsed?: () => void;
}

export default function WordDictation({ sentence, fullInput, onInputChange, onSentenceComplete, onHintUsed }: WordDictationProps) {
  const targetWords = useMemo(() => sentence.split(" "), [sentence]);
  const inputRef = useRef<HTMLInputElement>(null);

  const { matchingResult, currentIdx, currentTypedPart, isAllMatched } = useMemo(() => {
    const userInput = fullInput.split(/\s+/);
    const result = getDictationMatching(fullInput, sentence);

    // Evaluate fully normalized sequence to capture the completed sequence securely
    const isMatched = cleanStringForMatch(sentence) === cleanStringForMatch(fullInput);

    const lastPart = userInput[userInput.length - 1] || "";
    const isCurrentlyTyping = fullInput.length > 0 && !fullInput.endsWith(" ");

    return {
      matchingResult: result,
      currentIdx: userInput.length - 1,
      currentTypedPart: isCurrentlyTyping ? lastPart : "",
      isAllMatched: isMatched
    };
  }, [fullInput, sentence]);

  useEffect(() => {
    if (isAllMatched) {
      // Minor timeout rendering UI green glow feedback prior to flushing transition
      const t = setTimeout(() => {
        onSentenceComplete();
      }, 150);
      return () => clearTimeout(t);
    }
  }, [isAllMatched, onSentenceComplete]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onInputChange(e.target.value);
  };

  const requestHint = () => {
    if (isAllMatched || currentIdx >= targetWords.length) return;
    const targetWord = targetWords[currentIdx];
    if (targetWord) {
      if (onHintUsed) onHintUsed();
      // Auto-complete the current word for them
      const words = fullInput.split(/\s+/).filter(w => w.length > 0);
      words[currentIdx] = targetWord;
      const newValue = words.join(" ") + " ";
      onInputChange(newValue);

      // Keep focus on input
      requestAnimationFrame(() => {
        if (inputRef.current) inputRef.current.focus();
      });
    }
  };

  return (
    <div className="w-full bg-white border-t border-gray-100 p-4 sm:p-5 pb-6 sm:pb-8 flex flex-col items-center gap-4 [padding-bottom:max(1.5rem,env(safe-area-inset-bottom))]">
      {/* Visual Word Progress */}
      <div className="flex flex-wrap justify-center gap-x-2 gap-y-3 max-w-4xl">
        {targetWords.map((word, idx) => {
          const match = matchingResult[idx];
          const isMatchedLocal = match?.isMatched || false;

          const isActive = idx == currentIdx;
          const isAfterActive = idx > currentIdx;

          let className = "text-gray-500 bg-gray-50/50 border border-gray-200 rounded-xl px-2 py-0.5 text-xs sm:text-sm inline-flex items-center justify-center min-h-[28px]";

          if (!isAfterActive && fullInput.length > 0) {
            className =
              isMatchedLocal
                ? "bg-green-100 text-green-600 shadow-sm border border-green-200 rounded-xl px-2 py-0.5 text-xs sm:text-sm inline-flex items-center justify-center min-h-[28px]"
                : "bg-red-200 text-red-500 scale-105 shadow-xl shadow-red-100 rounded-xl px-2 py-0.5 text-xs sm:text-sm inline-flex items-center justify-center min-h-[28px]";
          }

          // Complete sentence passed override style map 
          if (isAllMatched) {
            className = "bg-green-100 text-green-600 shadow-sm border border-green-200 rounded-xl px-2 py-0.5 text-xs sm:text-sm scale-105 transition-all duration-300 inline-flex items-center justify-center min-h-[28px]";
          }

          return (
            <div key={idx} className="relative group">
              <div className={className}>
                {match ? match.displayString : cleanWord(word).replace(/[a-zA-Z0-9]/g, "•")}
              </div>

              <div className="absolute top-10 left-1/2 -translate-x-1/2 opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap z-20 pointer-events-none bg-gray-800 text-white px-2 py-1 rounded text-[8px] font-black uppercase tracking-widest">
                {word}
              </div>
            </div>
          );
        })}
      </div>

      {/* Actual Input Field */}
      <div className="w-full max-w-2xl relative group mb-1">
        <input
          ref={inputRef}
          type="text"
          value={fullInput}
          onChange={handleInputChange}
          placeholder="Type what you hear..."
          className={`
            w-full bg-gray-50 border-2 rounded-2xl 
            py-2 px-3 sm:py-3 sm:px-4 lg:py-3 lg:px-4
            text-base lg:text-lg
            font-bold transition-all outline-none
            
            ${currentIdx !== -1 &&
              currentTypedPart.length > 0 &&
              targetWords[currentIdx] &&
              !cleanWord(targetWords[currentIdx]).startsWith(cleanWord(currentTypedPart))
              ? "border-red-400 focus:bg-white focus:ring-4 focus:ring-red-50 text-red-600 shadow-lg shadow-red-100/50"
              : "border-gray-100 focus:border-green-400 focus:bg-white focus:ring-4 focus:ring-green-50/50 text-gray-800"
            }
            
            ${isAllMatched ? "border-green-400 bg-green-50 shadow-lg shadow-green-100" : ""}
          `}
          autoFocus
        />
      </div>
    </div>

  );
}
