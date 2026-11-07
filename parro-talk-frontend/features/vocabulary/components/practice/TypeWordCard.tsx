"use client";

import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Volume2,
  Briefcase,
  Plane,
  Check,
  X,
  Clock,
  BookOpen,
  Lightbulb,
  RotateCcw,
} from "lucide-react";

export interface TypeWordQuestionData {
  id: string;
  targetWord: string;
  topic: string;
  partOfSpeech?: string;
  definition: string;
  definitionVn?: string;
  promptDefinition?: string;
  synonymOrClue?: string;
  charCount?: number;
  firstLetter?: string;
  exampleWithMask?: string;
  exampleSentence?: string;
  phonetic?: string;
  audioUrl?: string;
}

export interface TypeWordCheckedState {
  isChecked: boolean;
  isCorrect: boolean;
  userAnswer: string;
  hintsUsed?: number;
}

interface Props {
  key?: React.Key;
  questionData: TypeWordQuestionData;
  checkedState: TypeWordCheckedState | null;
  onCheckAnswer: (answer: string, hintsUsed: number) => void;
  onAdvance?: () => void;
  onRetry?: () => void;
}

export function normalizeWord(str: string): string {
  if (!str) return "";
  return str
    .toLowerCase()
    .trim()
    .replace(/\s+/g, " ")
    .replace(/[.,!?;:'"()]+$/g, "")
    .trim();
}

export function TypeWordCard({
  questionData,
  checkedState,
  onCheckAnswer,
  onAdvance,
  onRetry,
}: Props) {
  const [inputVal, setInputVal] = useState("");
  const [hintLevel, setHintLevel] = useState(0); // 0: None, 1: Length/slots, 2: First letter, 3: Context sentence
  const [isRetrying, setIsRetrying] = useState(false);
  const [playingAccent, setPlayingAccent] = useState<"US" | "UK" | null>(null);

  const inputRef = useRef<HTMLInputElement>(null);

  // Sync state when question changes during render (React 19 pattern)
  const [prevQuestionId, setPrevQuestionId] = useState(questionData.id);
  if (prevQuestionId !== questionData.id) {
    setPrevQuestionId(questionData.id);
    setInputVal("");
    setHintLevel(0);
    setIsRetrying(false);
  }

  const isChecked = Boolean(checkedState?.isChecked) && !isRetrying;
  const isCorrect = Boolean(checkedState?.isCorrect);

  // Persistent autofocus lifecycle: refocus input whenever question changes or retry
  useEffect(() => {
    const timer = setTimeout(() => {
      if (!isChecked) {
        inputRef.current?.focus();
      }
    }, 50);
    return () => clearTimeout(timer);
  }, [questionData.id, isChecked, isRetrying]);

  // Keyboard navigation: Enter to advance when checked, Tab to request hint
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (isChecked) {
        if (e.key === "Enter") {
          e.preventDefault();
          onAdvance?.();
        }
        return;
      }

      // If user presses H when not typing inside an input
      if (
        (e.key === "h" || e.key === "H") &&
        !(e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement)
      ) {
        e.preventDefault();
        setHintLevel((prev) => Math.min(prev + 1, 3));
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isChecked, onAdvance]);

  const fallbackSpeech = useCallback((text: string, accent: "US" | "UK") => {
    if (typeof window !== "undefined" && "speechSynthesis" in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = accent === "UK" ? "en-GB" : "en-US";
      utterance.rate = 0.9;
      utterance.onend = () => setPlayingAccent(null);
      utterance.onerror = () => setPlayingAccent(null);
      window.speechSynthesis.speak(utterance);
    } else {
      setPlayingAccent(null);
    }
  }, []);

  // Audio pronunciation helpers (US and UK)
  const playAudio = useCallback(
    (accent: "US" | "UK") => {
      if (typeof window === "undefined") return;
      setPlayingAccent(accent);

      if (accent === "US" && questionData.audioUrl) {
        const audio = new Audio(questionData.audioUrl);
        audio.onended = () => setPlayingAccent(null);
        audio.onerror = () => fallbackSpeech(questionData.targetWord, accent);
        audio.play().catch(() => fallbackSpeech(questionData.targetWord, accent));
        return;
      }

      fallbackSpeech(questionData.targetWord, accent);
    },
    [questionData.targetWord, questionData.audioUrl, fallbackSpeech]
  );

  const handleFormSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const normalized = normalizeWord(inputVal);
    if (!normalized || isChecked) return;

    setIsRetrying(false);
    onCheckAnswer(inputVal, hintLevel);
  };

  const handleRetryClick = () => {
    setIsRetrying(true);
    setInputVal("");
    onRetry?.();
    setTimeout(() => {
      inputRef.current?.focus();
    }, 50);
  };

  const handleAdvanceHint = () => {
    setHintLevel((prev) => Math.min(prev + 1, 3));
  };

  // Generate blank slots representation
  const target = questionData.targetWord;
  const charLength = questionData.charCount || target.length;

  const renderSlotBoxes = () => {
    const chars = target.split("");
    return (
      <div className="flex flex-wrap items-center gap-1.5 pt-1">
        {chars.map((ch, idx) => {
          if (ch === " " || ch === "-") {
            return (
              <span key={idx} className="w-4 text-center font-bold text-ink-400">
                {ch}
              </span>
            );
          }

          // In Hint 2+, reveal first character (or all leading characters before spaces)
          const isRevealed = hintLevel >= 2 && idx === 0;

          return (
            <span
              key={idx}
              className={`w-7 h-9 rounded-lg border flex items-center justify-center font-mono font-bold text-sm transition-all ${
                isRevealed
                  ? "border-amber-400 bg-amber-50 text-amber-900 shadow-2xs"
                  : "border-ink-200 bg-white text-ink-400"
              }`}
            >
              {isRevealed ? ch : "_"}
            </span>
          );
        })}
      </div>
    );
  };

  const topicIcon = questionData.topic?.toLowerCase().includes("travel") ? (
    <Plane className="w-3.5 h-3.5 text-amber-600" />
  ) : (
    <Briefcase className="w-3.5 h-3.5 text-amber-600" />
  );

  // Masked example sentence
  const maskedSentence =
    questionData.exampleWithMask ||
    (questionData.exampleSentence
      ? questionData.exampleSentence.replace(new RegExp(target, "gi"), "________")
      : `Complete the sentence with the word: ________`);

  return (
    <div className="rounded-3xl border border-ink-200/80 bg-white p-6 sm:p-9 shadow-card space-y-6 transition-all animate-fade-up">
      {/* ── Top row: Tag & Category Badge ── */}
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-2 text-xs font-semibold text-ink-500">
          <BookOpen className="w-3.5 h-3.5 text-amber-600 shrink-0" />
          <span className="text-ink-700 font-bold">Definition → Word Recall</span>
          {questionData.partOfSpeech && (
            <span className="rounded-md bg-amber-50 border border-amber-200/70 text-amber-800 px-2 py-0.5 text-[11px] font-bold">
              {questionData.partOfSpeech}
            </span>
          )}
        </div>

        <div className="inline-flex items-center gap-1.5 rounded-xl border border-amber-200/80 bg-amber-50/70 px-3 py-1.5 text-xs font-semibold text-amber-800 shadow-sm shrink-0">
          {topicIcon}
          <span>{questionData.topic}</span>
        </div>
      </div>

      {/* ── Question Title ── */}
      <div className="space-y-1">
        <h2 className="text-2xl sm:text-[28px] font-extrabold tracking-tight text-ink-900 leading-snug">
          What word fits this meaning?
        </h2>
        <p className="text-xs text-ink-400">
          Recall the exact word from its definition alone. Type and press Enter.
        </p>
      </div>

      {/* ── Definition / Meaning Clue Box (Hint 0) ── */}
      <div className="rounded-2xl border border-amber-200/80 bg-[#fffdf7] p-5 space-y-3 shadow-2xs">
        <div className="flex items-center justify-between gap-2">
          <div className="text-[10px] font-extrabold uppercase tracking-widest text-amber-700">
            DEFINITION &amp; MEANING
          </div>
          {questionData.phonetic && (
            <span className="font-mono text-xs text-amber-800 bg-amber-100/60 px-2 py-0.5 rounded">
              {questionData.phonetic}
            </span>
          )}
        </div>

        <p className="text-base sm:text-lg font-semibold text-ink-900 leading-relaxed font-sans">
          &ldquo;{questionData.definition}&rdquo;
        </p>

        {questionData.definitionVn && (
          <p className="text-xs sm:text-sm font-medium text-ink-600 italic border-t border-amber-100 pt-2">
            Nghĩa tiếng Việt: {questionData.definitionVn}
          </p>
        )}

        {/* ── Progressive Hint Disclosures (Hint 1, Hint 2, Hint 3) ── */}
        {hintLevel >= 1 && (
          <div
            role="region"
            aria-live="polite"
            className="rounded-xl border border-amber-200 bg-white/90 p-3.5 space-y-2 mt-3 animate-fade-in"
          >
            {/* Hint 1: Letter count and blank slots */}
            <div className="space-y-1">
              <div className="flex items-center justify-between text-xs font-bold text-amber-800">
                <span>
                  Letter Clue: {charLength} letters
                  {hintLevel >= 2 ? " (first letter revealed)" : ""}
                </span>
                <span className="text-[10px] text-ink-400 uppercase font-semibold">
                  Stage {hintLevel}/3
                </span>
              </div>
              {renderSlotBoxes()}
            </div>

            {/* Hint 3: Contextual sentence with mask */}
            {hintLevel >= 3 && (
              <div className="pt-2 border-t border-amber-100 space-y-1">
                <span className="text-[11px] font-bold text-amber-700 uppercase tracking-wider">
                  Context Sentence:
                </span>
                <p className="text-xs sm:text-sm text-ink-800 italic leading-relaxed">
                  &ldquo;{maskedSentence}&rdquo;
                </p>
              </div>
            )}
          </div>
        )}

        {/* Hint trigger bar */}
        <div className="flex items-center justify-between gap-2 pt-1">
          <button
            type="button"
            disabled={hintLevel >= 3 || isChecked}
            onClick={handleAdvanceHint}
            className={`inline-flex items-center gap-1.5 text-xs font-bold px-3 py-1.5 rounded-xl border transition-all ${
              hintLevel >= 3
                ? "border-ink-200 text-ink-400 bg-ink-50 cursor-default"
                : "border-amber-300 bg-amber-100/70 hover:bg-amber-200/80 text-amber-900 shadow-2xs"
            }`}
          >
            <Lightbulb className="w-3.5 h-3.5 text-amber-600" />
            <span>
              {hintLevel === 0
                ? "Need a hint? (Press Tab or H)"
                : hintLevel === 1
                ? "Reveal first letter (Stage 2/3)"
                : hintLevel === 2
                ? "Reveal context sentence (Stage 3/3)"
                : "All hints revealed"}
            </span>
          </button>

          <span className="text-[11px] text-ink-400">
            {hintLevel > 0 ? `${hintLevel}/3 hints used` : "No hints used"}
          </span>
        </div>
      </div>

      {/* ── Audio Pronunciation Buttons (Dual Accent: US and UK) ── */}
      <div className="flex flex-wrap items-center gap-2.5">
        <button
          type="button"
          onClick={() => playAudio("US")}
          className={`inline-flex items-center gap-1.5 rounded-xl border px-3 py-1.5 text-xs font-semibold transition-all ${
            playingAccent === "US"
              ? "border-amber-500 bg-amber-50 text-amber-800 ring-2 ring-amber-200"
              : "border-ink-200 bg-white text-ink-700 hover:border-amber-300 hover:bg-amber-50/50 shadow-2xs"
          }`}
        >
          <Volume2
            className={`w-3.5 h-3.5 ${playingAccent === "US" ? "animate-pulse text-amber-600" : ""}`}
          />
          <span>Listen US</span>
        </button>

        <button
          type="button"
          onClick={() => playAudio("UK")}
          className={`inline-flex items-center gap-1.5 rounded-xl border px-3 py-1.5 text-xs font-semibold transition-all ${
            playingAccent === "UK"
              ? "border-amber-500 bg-amber-50 text-amber-800 ring-2 ring-amber-200"
              : "border-ink-200 bg-white text-ink-700 hover:border-amber-300 hover:bg-amber-50/50 shadow-2xs"
          }`}
        >
          <Volume2
            className={`w-3.5 h-3.5 ${playingAccent === "UK" ? "animate-pulse text-amber-600" : ""}`}
          />
          <span>Listen UK</span>
        </button>

        <span className="text-[11px] text-ink-400 hidden sm:inline">
          Pronunciation guide available
        </span>
      </div>

      {/* ── Typing Input Section with Persistent Autofocus ── */}
      <form onSubmit={handleFormSubmit} className="space-y-2 pt-1">
        <label htmlFor="word-recall-input" className="block text-xs font-bold text-ink-800">
          Your answer
        </label>

        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
          <div className="relative flex-1">
            <input
              ref={inputRef}
              id="word-recall-input"
              type="text"
              autoComplete="off"
              disabled={isChecked}
              value={isChecked ? (checkedState?.userAnswer || inputVal) : inputVal}
              onChange={(e) => setInputVal(e.target.value)}
              onKeyDown={(e) => {
                // If user presses Tab while typing and not yet checked, provide progressive hint
                if (e.key === "Tab" && !isChecked && hintLevel < 3) {
                  e.preventDefault();
                  setHintLevel((prev) => Math.min(prev + 1, 3));
                }
              }}
              placeholder="Type the word you recall"
              className={`min-h-[48px] w-full rounded-2xl border px-4 py-3 text-sm font-semibold tracking-wide outline-none transition-all ${
                isChecked
                  ? isCorrect
                    ? "border-emerald-500 bg-emerald-50/40 text-emerald-950 pr-10 ring-2 ring-emerald-200"
                    : "border-rose-400 bg-rose-50/40 text-rose-950 pr-10 ring-2 ring-rose-200"
                  : "border-ink-200/90 bg-white text-ink-900 focus:border-amber-500 focus:ring-4 focus:ring-amber-100"
              }`}
            />

            {isChecked && (
              <div className="absolute right-3.5 top-1/2 -translate-y-1/2">
                {isCorrect ? (
                  <Check className="w-5 h-5 text-emerald-600 stroke-[2.5]" />
                ) : (
                  <X className="w-5 h-5 text-rose-500 stroke-[2.5]" />
                )}
              </div>
            )}
          </div>

          <button
            type="submit"
            disabled={!inputVal.trim() || isChecked}
            className={`min-h-[48px] inline-flex items-center justify-center gap-1.5 rounded-2xl px-6 py-3 text-xs font-bold transition-all shrink-0 ${
              isChecked
                ? "bg-[#e8ece9] text-ink-600 cursor-default"
                : "bg-amber-600 hover:bg-amber-700 disabled:opacity-40 disabled:cursor-not-allowed text-white shadow-sm"
            }`}
          >
            <Check className="w-4 h-4 stroke-[2.5]" />
            <span>{isChecked ? "Answer locked" : "Check Answer"}</span>
          </button>
        </div>

        <p className="text-[11px] text-ink-400">
          {isChecked
            ? "Your answer has been evaluated."
            : "Type the target word. Case-insensitive and trimmed. Press Enter to submit."}
        </p>

        {/* ── Feedback & Correction Card ── */}
        {isChecked && (
          <div
            role="region"
            aria-label="Result evaluation"
            className={`p-5 rounded-2xl border text-xs font-medium space-y-3 mt-3 animate-fade-up ${
              isCorrect
                ? "border-emerald-200 bg-emerald-50/80 text-emerald-900"
                : "border-rose-200 bg-rose-50/80 text-rose-900"
            }`}
          >
            <div className="font-bold text-sm flex items-center justify-between gap-2">
              <div className="flex items-center gap-1.5">
                {isCorrect ? (
                  <>
                    <Check className="w-4 h-4 text-emerald-600 stroke-[2.5]" />
                    <span className="text-emerald-950 font-extrabold">
                      Correct! Excellent recall.
                    </span>
                  </>
                ) : (
                  <>
                    <X className="w-4 h-4 text-rose-600 stroke-[2.5]" />
                    <span className="text-rose-950 font-extrabold">
                      Incorrect spelling or word
                    </span>
                  </>
                )}
              </div>

              {!isCorrect && (
                <button
                  type="button"
                  onClick={handleRetryClick}
                  className="inline-flex items-center gap-1 text-xs font-bold bg-white text-rose-800 border border-rose-300 hover:bg-rose-100/70 px-3 py-1 rounded-xl shadow-2xs transition-all"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  <span>Try again</span>
                </button>
              )}
            </div>

            {/* Spelling Reveal */}
            <div className="bg-white/90 border border-ink-100 rounded-xl p-3 space-y-1">
              <div className="flex items-center gap-2">
                <span className="text-ink-500 font-medium">Target Word:</span>
                <span className="text-base font-extrabold text-ink-900 underline decoration-amber-500">
                  {questionData.targetWord}
                </span>
                {questionData.partOfSpeech && (
                  <span className="text-ink-400 text-[11px]">({questionData.partOfSpeech})</span>
                )}
              </div>

              {!isCorrect && checkedState?.userAnswer && (
                <div className="flex items-center gap-2 text-rose-700">
                  <span className="text-ink-500">You typed:</span>
                  <span className="line-through font-mono font-semibold">
                    {checkedState.userAnswer}
                  </span>
                </div>
              )}
            </div>

            {questionData.exampleSentence && (
              <div className="text-ink-700 italic pt-1 border-t border-ink-100">
                &ldquo;{questionData.exampleSentence}&rdquo;
              </div>
            )}

            <div className="flex items-center justify-between pt-2 border-t border-ink-100 text-[11px] text-ink-500">
              <span>Press <kbd className="font-mono bg-white px-1.5 py-0.5 rounded border border-ink-200 font-bold">Enter ↵</kbd> or choose Next question</span>
            </div>
          </div>
        )}
      </form>

      {/* ── Footer Tip ── */}
      <div className="rounded-2xl border border-ink-100 bg-ink-50/60 p-3.5 flex items-center gap-2.5 text-xs text-ink-500">
        <Clock className="w-3.5 h-3.5 text-amber-600 shrink-0" />
        <span>Press Tab inside input to unlock letter and context hints.</span>
      </div>
    </div>
  );
}
