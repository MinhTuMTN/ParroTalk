"use client";

import { cleanWord, getDictationMatching } from "@/lib/utils";
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";

interface WordDictationProps {
  sentence: string;
  fullInput: string;
  onInputChange: (val: string) => void;
  onSentenceComplete: () => void;
  onHintUsed?: () => void;
  isCompleted?: boolean;
}

export default function WordDictation({ sentence, fullInput, onInputChange, onSentenceComplete, onHintUsed, isCompleted }: WordDictationProps) {
  const targetWords = useMemo(() => sentence.split(" "), [sentence]);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  // Auto-resize textarea height when input text changes
  useEffect(() => {
    const textarea = inputRef.current;
    if (textarea) {
      textarea.style.height = "auto";
      const style = window.getComputedStyle(textarea);
      const borderTop = parseFloat(style.borderTopWidth) || 0;
      const borderBottom = parseFloat(style.borderBottomWidth) || 0;
      textarea.style.height = `${textarea.scrollHeight + borderTop + borderBottom}px`;
    }
  }, [fullInput, sentence]);
  const [revealedWords, setRevealedWords] = useState<Set<number>>(new Set());
  const [prevSentence, setPrevSentence] = useState(sentence);

  if (sentence !== prevSentence) {
    setPrevSentence(sentence);
    setRevealedWords(new Set());
  }

  const { matchingResult, currentIdx, currentTypedPart, isAllMatched } = useMemo(() => {
    const userInput = fullInput.split(/\s+/);
    const result = getDictationMatching(fullInput, sentence);

    // Evaluate fully normalized sequence to capture the completed sequence securely
    const isMatched = cleanWord(sentence) === cleanWord(fullInput);

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
    if (isAllMatched && !isCompleted) {
      // Minor timeout rendering UI green glow feedback prior to flushing transition
      const t = setTimeout(() => {
        onSentenceComplete();
      }, 150);
      return () => clearTimeout(t);
    }
  }, [isAllMatched, isCompleted, onSentenceComplete]);

  const handleInputChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    if (isCompleted) return;
    // Replace newlines with spaces to maintain single-line text behavior in the data model
    const cleanValue = e.target.value.replace(/\r?\n|\r/g, " ");
    onInputChange(cleanValue);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    // Prevent inserting newlines when Enter is pressed
    if (e.key === "Enter") {
      e.preventDefault();
    }
  };

  const handleWordClick = useCallback((idx: number) => {
    if (isCompleted || isAllMatched) return;

    // If already matched or already revealed, do nothing
    const match = matchingResult[idx];
    if (match?.isMatched || revealedWords.has(idx)) return;

    // Reveal and charge a hint
    setRevealedWords(prev => {
      const next = new Set(prev);
      next.add(idx);
      return next;
    });

    if (onHintUsed) onHintUsed();
  }, [isCompleted, isAllMatched, matchingResult, revealedWords, onHintUsed]);

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
    <div className="w-full flex flex-col items-center gap-2 sm:gap-4 [padding-bottom:env(safe-area-inset-bottom)]">
      {/* Visual Word Progress */}
      < div className="flex flex-wrap justify-center gap-x-2 gap-y-3 max-w-4xl" >
        {
          targetWords.map((word, idx) => {
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

            const isRevealed = revealedWords.has(idx);

            return (
              <div key={idx} className="relative">
                <div
                  className={`${className} cursor-pointer hover:bg-gray-100 transition-colors pointer-events-auto`}
                  onClick={() => handleWordClick(idx)}
                >
                  {(isRevealed || isMatchedLocal) ? word : (match ? match.displayString : cleanWord(word).replace(/[a-zA-Z0-9]/g, "•"))}
                </div>
              </div>
            );
          })
        }
      </div >

      {/* Actual Input Field */}
      < div className="w-full max-w-2xl relative group" >
        <textarea
          ref={inputRef}
          value={fullInput}
          onChange={handleInputChange}
          onKeyDown={handleKeyDown}
          placeholder="Type what you hear..."
          rows={1}
          className={`
            w-full bg-gray-50 border-2 rounded-2xl 
            py-2 px-3 sm:py-3 sm:px-4 lg:py-3 lg:px-4
            text-base lg:text-lg leading-normal
            font-bold transition-all outline-none
            resize-none overflow-hidden
            
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
      </div >
    </div >

  );
}

