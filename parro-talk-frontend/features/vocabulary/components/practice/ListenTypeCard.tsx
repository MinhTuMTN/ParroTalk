"use client";

import React, { useState } from "react";
import {
  Volume2,
  Briefcase,
  Plane,
  Check,
  X,
  Clock,
  Lightbulb,
  Headphones,
  Eye,
  EyeOff,
  ArrowRight,
} from "lucide-react";

export interface ListenTypeQuestionData {
  id: string;
  targetWord: string;
  topic: string;
  partOfSpeech?: string;
  definition: string;
  explanation?: string;
  sentenceWithBlank: string;
  fullSentence: string;
  easySentence: string;
  clues: [string, string, string]; // 3 clues describing the same word & sense
  audioUrl?: string;
}

export interface HardModeAttempt {
  userAnswer: string;
  isCorrect: boolean;
}

interface Props {
  key?: React.Key;
  difficulty: "easy" | "hard";
  questionData: ListenTypeQuestionData;
  // Easy mode state
  easyCheckedState?: { isChecked: boolean; isCorrect: boolean; userAnswer: string } | null;
  onEasyCheckAnswer?: (answer: string) => void;

  // Hard mode state
  hardAttempts: HardModeAttempt[];
  currentAttemptIndex: number;
  isHardAttemptLocked: boolean;
  isHardQuestionFinished: boolean;
  onHardCheckAttempt?: (answer: string) => void;
  onHardNextClue?: () => void;
}

export function ListenTypeCard({
  difficulty,
  questionData,
  easyCheckedState,
  onEasyCheckAnswer,
  hardAttempts,
  currentAttemptIndex,
  isHardAttemptLocked,
  isHardQuestionFinished,
  onHardCheckAttempt,
  onHardNextClue,
}: Props) {
  const [inputVal, setInputVal] = useState("");
  const [isPlayingAudio, setIsPlayingAudio] = useState(false);
  const [audioPlayed, setAudioPlayed] = useState(false);
  const [isListenOnly, setIsListenOnly] = useState(false);

  const [prevKey, setPrevKey] = useState({
    id: questionData.id,
    difficulty,
    attempt: currentAttemptIndex,
  });

  if (
    prevKey.id !== questionData.id ||
    prevKey.difficulty !== difficulty ||
    prevKey.attempt !== currentAttemptIndex
  ) {
    setPrevKey({
      id: questionData.id,
      difficulty,
      attempt: currentAttemptIndex,
    });
    setInputVal("");
    if (prevKey.id !== questionData.id || prevKey.difficulty !== difficulty) {
      setAudioPlayed(false);
      setIsListenOnly(false);
    }
  }

  // Audio speech synthesis helper
  const playAudio = (textToRead: string) => {
    if (typeof window === "undefined") return;
    setIsPlayingAudio(true);

    if (questionData.audioUrl && difficulty === "easy") {
      const audio = new Audio(questionData.audioUrl);
      audio.onended = () => {
        setIsPlayingAudio(false);
        setAudioPlayed(true);
      };
      audio.onerror = () => fallbackSpeech(textToRead);
      audio.play().catch(() => fallbackSpeech(textToRead));
      return;
    }

    fallbackSpeech(textToRead);
  };

  const fallbackSpeech = (text: string) => {
    if (typeof window !== "undefined" && "speechSynthesis" in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = "en-US";
      utterance.rate = 0.95;
      utterance.onend = () => {
        setIsPlayingAudio(false);
        setAudioPlayed(true);
      };
      utterance.onerror = () => {
        setIsPlayingAudio(false);
        setAudioPlayed(true);
      };
      window.speechSynthesis.speak(utterance);
    } else {
      setIsPlayingAudio(false);
      setAudioPlayed(true);
    }
  };

  const handlePlayEasyAudio = () => {
    // Speaks the target word followed by the full sentence
    const promptSpeech = `${questionData.targetWord}. ${questionData.easySentence || questionData.fullSentence}`;
    playAudio(promptSpeech);
  };

  const handlePlayClueAudio = () => {
    const clueText = questionData.clues[currentAttemptIndex] || questionData.definition;
    playAudio(clueText);
  };

  // Form submission handler
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = inputVal.trim();
    if (!trimmed) return;

    if (difficulty === "easy") {
      if (easyCheckedState?.isChecked) return;
      onEasyCheckAnswer?.(trimmed);
    } else {
      if (isHardAttemptLocked || isHardQuestionFinished) return;
      onHardCheckAttempt?.(trimmed);
    }
  };

  const topicIcon = questionData.topic?.toLowerCase().includes("travel") ? (
    <Plane className="w-3.5 h-3.5 text-brand-600" />
  ) : (
    <Briefcase className="w-3.5 h-3.5 text-brand-600" />
  );

  // Computed states
  const isEasyChecked = Boolean(easyCheckedState?.isChecked);
  const isEasyCorrect = Boolean(easyCheckedState?.isCorrect);

  const lastHardAttempt = hardAttempts[currentAttemptIndex];
  const isCurrentHardCorrect = Boolean(lastHardAttempt?.isCorrect);
  const hasFailedAllHardClues = currentAttemptIndex === 2 && isHardAttemptLocked && !isCurrentHardCorrect;

  const displayInputVal =
    difficulty === "easy"
      ? (isEasyChecked ? (easyCheckedState?.userAnswer ?? inputVal) : inputVal)
      : (isHardAttemptLocked && hardAttempts[currentAttemptIndex]
          ? hardAttempts[currentAttemptIndex].userAnswer
          : inputVal);

  return (
    <div className="rounded-3xl border border-ink-200/80 bg-white p-6 sm:p-9 shadow-card space-y-6 transition-all animate-fade-up">
      {/* ── Top row: Tagline & Category Badge ── */}
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-2 text-xs font-semibold text-ink-600">
          {difficulty === "easy" ? (
            <>
              <Headphones className="w-4 h-4 text-brand-600 shrink-0" />
              <span>Listen &amp; Type · Easy</span>
            </>
          ) : (
            <>
              <Lightbulb className="w-4 h-4 text-honey-600 shrink-0" />
              <span>Listen &amp; Type · Hard</span>
            </>
          )}
        </div>

        <div className="inline-flex items-center gap-1.5 rounded-xl border border-brand-200/60 bg-brand-50/70 px-3 py-1.5 text-xs font-semibold text-brand-700 shadow-sm shrink-0">
          {topicIcon}
          <span>{questionData.topic}</span>
        </div>
      </div>

      {/* ── Question Title ── */}
      <h2 className="text-2xl sm:text-[28px] font-extrabold tracking-tight text-ink-900 leading-snug">
        {difficulty === "easy" ? "Listen for the focus word" : "Which word fits this meaning?"}
      </h2>

      {/* ── Clue / Audio Container ── */}
      {difficulty === "easy" ? (
        /* Easy Mode Audio Box */
        <div className="space-y-2">
          <div className="rounded-2xl border border-brand-200/60 bg-[#eaf4ed] p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3.5">
              <div className="w-12 h-12 rounded-2xl bg-white border border-brand-200/70 flex items-center justify-center text-brand-600 shadow-2xs shrink-0">
                <div className="flex items-center gap-1">
                  <span className="w-0.5 h-3 bg-brand-500 rounded-full animate-pulse" />
                  <Headphones className="w-5 h-5" />
                  <span className="w-0.5 h-3 bg-brand-500 rounded-full animate-pulse" />
                </div>
              </div>
              <div className="space-y-0.5">
                <div className="text-[10px] font-extrabold uppercase tracking-widest text-brand-700">
                  LISTEN · RECALL · TYPE
                </div>
                <p className="text-xs sm:text-sm text-ink-700 leading-relaxed font-medium">
                  You&apos;ll hear the focus word, followed by a {questionData.topic.toLowerCase()} sentence. Type the word you hear.
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={handlePlayEasyAudio}
              className={`inline-flex items-center justify-center gap-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white px-4 py-3 text-xs font-bold shadow-sm transition-all shrink-0 ${
                isPlayingAudio ? "ring-4 ring-brand-200" : ""
              }`}
            >
              <Volume2 className={`w-4 h-4 ${isPlayingAudio ? "animate-pulse" : ""}`} />
              <span>{audioPlayed ? "Replay audio" : "Play audio"}</span>
            </button>
          </div>

          <div className="text-center text-[11px] text-ink-400">
            Device English voice · availability depends on your browser.
          </div>

          {audioPlayed && (
            <div className="text-xs font-medium text-brand-700 text-center animate-fade-up">
              Audio finished. Listen again whenever you need.
            </div>
          )}
        </div>
      ) : (
        /* Hard Mode Clue Box (With 3 attempts indicator & Listen Only option) */
        <div className="space-y-3">
          <div className="rounded-2xl border border-brand-200/60 bg-[#eaf4ed] p-5 space-y-4">
            {/* Clue Header row with Dash Segments */}
            <div className="flex items-center justify-between gap-3">
              <span className="text-[10px] font-extrabold uppercase tracking-widest text-brand-700">
                SAMPLE CLUE · ATTEMPT {currentAttemptIndex + 1} / 3
              </span>

              {/* 3 Dash Indicators */}
              <div className="flex items-center gap-1.5">
                {[0, 1, 2].map((idx) => {
                  const isFilled = idx <= currentAttemptIndex;
                  return (
                    <span
                      key={idx}
                      className={`h-1.5 w-6 rounded-full transition-all duration-300 ${
                        isFilled ? "bg-brand-600" : "bg-brand-200/80"
                      }`}
                    />
                  );
                })}
              </div>
            </div>

            {/* Clue Text or Listen-Only Mask */}
            <div className="min-h-[44px] flex items-center">
              {isListenOnly ? (
                <div className="flex items-center gap-2 text-sm italic text-brand-800 bg-white/70 border border-brand-200/80 rounded-xl px-3.5 py-2.5 w-full">
                  <Headphones className="w-4 h-4 text-brand-600 shrink-0" />
                  <span>Listen-only mode is active. Clue text is hidden. Use the audio button below to listen!</span>
                </div>
              ) : (
                <p className="text-sm sm:text-base font-semibold text-ink-900 leading-relaxed font-sans">
                  {questionData.clues[currentAttemptIndex] || questionData.definition}
                </p>
              )}
            </div>

            {/* Listen Action Buttons: Listen to clue & Toggle Listen-Only */}
            <div className="flex flex-wrap items-center justify-center gap-2.5 pt-1">
              <button
                type="button"
                onClick={handlePlayClueAudio}
                className={`inline-flex items-center gap-2 rounded-xl border border-brand-200 bg-white hover:bg-brand-50/80 text-brand-700 px-4 py-2 text-xs font-semibold shadow-2xs transition-all ${
                  isPlayingAudio ? "ring-2 ring-brand-300" : ""
                }`}
              >
                <Volume2 className={`w-3.5 h-3.5 ${isPlayingAudio ? "animate-pulse" : ""}`} />
                <span>Listen to clue</span>
              </button>

              {/* Listen-Only Hint Toggle: hides/reveals clue text without consuming attempt */}
              <button
                type="button"
                onClick={() => setIsListenOnly(!isListenOnly)}
                className="inline-flex items-center gap-1.5 rounded-xl border border-brand-200/80 bg-white/80 hover:bg-white text-ink-600 px-3 py-2 text-xs font-medium transition-all"
                title="Listen-only preference (does not consume an attempt)"
              >
                {isListenOnly ? (
                  <>
                    <Eye className="w-3.5 h-3.5 text-brand-600" />
                    <span>Show clue text</span>
                  </>
                ) : (
                  <>
                    <EyeOff className="w-3.5 h-3.5 text-ink-400" />
                    <span>Listen only (hide text)</span>
                  </>
                )}
              </button>
            </div>
          </div>

          <div className="text-center text-[11px] text-ink-400">
            Optional audio · Device English voice · availability depends on your browser.
          </div>
        </div>
      )}

      {/* ── Input Section ── */}
      <form onSubmit={handleSubmit} className="space-y-2 pt-1">
        <label htmlFor="listen-type-input" className="block text-xs font-bold text-ink-800">
          Your answer
        </label>

        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
          <div className="relative flex-1">
            <input
              id="listen-type-input"
              type="text"
              autoComplete="off"
              autoFocus
              disabled={
                difficulty === "easy"
                  ? isEasyChecked
                  : isHardAttemptLocked || isHardQuestionFinished
              }
              value={displayInputVal}
              onChange={(e) => setInputVal(e.target.value)}
              placeholder={difficulty === "easy" ? "Type the word you hear" : "What word is this?"}
              className={`w-full rounded-2xl border px-4 py-3 text-sm font-semibold tracking-wide outline-none transition-all ${
                difficulty === "easy"
                  ? isEasyChecked
                    ? isEasyCorrect
                      ? "border-emerald-500 bg-emerald-50/40 text-emerald-950 pr-10 ring-2 ring-emerald-200"
                      : "border-rose-400 bg-rose-50/40 text-rose-950 pr-10 ring-2 ring-rose-200"
                    : "border-ink-200/90 bg-white text-ink-900 focus:border-brand-500 focus:ring-4 focus:ring-brand-100"
                  : isHardAttemptLocked
                  ? isCurrentHardCorrect
                    ? "border-emerald-500 bg-emerald-50/40 text-emerald-950 pr-10 ring-2 ring-emerald-200"
                    : "border-rose-300 bg-rose-50/50 text-rose-950 pr-10 ring-2 ring-rose-100"
                  : "border-ink-200/90 bg-white text-ink-900 focus:border-brand-500 focus:ring-4 focus:ring-brand-100"
              }`}
            />

            {/* Input icon status */}
            {(isEasyChecked || (difficulty === "hard" && isHardAttemptLocked)) && (
              <div className="absolute right-3.5 top-1/2 -translate-y-1/2">
                {(difficulty === "easy" ? isEasyCorrect : isCurrentHardCorrect) ? (
                  <Check className="w-5 h-5 text-emerald-600 stroke-[2.5]" />
                ) : (
                  <X className="w-5 h-5 text-rose-500 stroke-[2.5]" />
                )}
              </div>
            )}
          </div>

          {/* Button: Check Answer or Answer Locked */}
          {difficulty === "easy" ? (
            <button
              type="submit"
              disabled={!inputVal.trim() || isEasyChecked}
              className={`inline-flex items-center justify-center gap-1.5 rounded-2xl px-6 py-3 text-xs font-bold transition-all shrink-0 ${
                isEasyChecked
                  ? "bg-[#d2e8db] text-brand-800 cursor-default"
                  : "bg-brand-600 hover:bg-brand-700 disabled:opacity-40 disabled:cursor-not-allowed text-white shadow-brand"
              }`}
            >
              <Check className="w-4 h-4 stroke-[2.5]" />
              <span>{isEasyChecked ? "Answer locked" : "Check Answer"}</span>
            </button>
          ) : (
            <button
              type="submit"
              disabled={!inputVal.trim() || isHardAttemptLocked || isHardQuestionFinished}
              className={`inline-flex items-center justify-center gap-1.5 rounded-2xl px-6 py-3 text-xs font-bold transition-all shrink-0 ${
                isHardAttemptLocked || isHardQuestionFinished
                  ? "bg-[#d2e8db] text-brand-800 cursor-default"
                  : "bg-brand-600 hover:bg-brand-700 disabled:opacity-40 disabled:cursor-not-allowed text-white shadow-brand"
              }`}
            >
              <Check className="w-4 h-4 stroke-[2.5]" />
              <span>{isHardAttemptLocked || isHardQuestionFinished ? "Answer locked" : "Check Answer"}</span>
            </button>
          )}
        </div>

        {/* Input helper text */}
        <p className="text-[11px] text-ink-400">
          {(difficulty === "easy" ? isEasyChecked : isHardAttemptLocked)
            ? "Your answer is locked for this attempt."
            : "Type one word. Press Enter or choose Check Answer."}
        </p>

        {/* ── Feedback States ── */}

        {/* 1. Easy Mode Feedback */}
        {difficulty === "easy" && isEasyChecked && (
          <div
            className={`p-4 rounded-2xl border text-xs font-medium space-y-1.5 mt-3 animate-fade-up ${
              isEasyCorrect
                ? "border-emerald-200 bg-emerald-50/80 text-emerald-900"
                : "border-rose-200 bg-rose-50/80 text-rose-900"
            }`}
          >
            <div className="font-bold text-sm flex items-center gap-1.5">
              {isEasyCorrect ? (
                <>
                  <Check className="w-4 h-4 text-emerald-600 stroke-[2.5]" />
                  <span>Correct. That&apos;s the word.</span>
                </>
              ) : (
                <>
                  <X className="w-4 h-4 text-rose-600 stroke-[2.5]" />
                  <span>
                    The word is &ldquo;
                    <span className="underline font-extrabold">{questionData.targetWord}</span>
                    &rdquo;.
                  </span>
                </>
              )}
            </div>

            <div className="text-ink-800 font-semibold pt-1">
              {questionData.targetWord}
            </div>
            <div className="text-ink-600">
              {questionData.definition}
              {questionData.partOfSpeech && ` Here, "${questionData.targetWord}" is a ${questionData.partOfSpeech}.`}
            </div>
            {questionData.fullSentence && (
              <div className="text-ink-600 italic">
                {questionData.fullSentence}
              </div>
            )}
          </div>
        )}

        {/* 2. Hard Mode - Incorrect Attempt (Clue 1 or Clue 2) -> "Try another clue" */}
        {difficulty === "hard" && isHardAttemptLocked && !isCurrentHardCorrect && currentAttemptIndex < 2 && (
          <div className="p-4 rounded-2xl border border-rose-200 bg-rose-50/60 text-xs font-medium space-y-2 mt-3 animate-fade-up">
            <div className="font-bold text-sm text-rose-700 flex items-center gap-1.5">
              <X className="w-4 h-4 text-rose-600 stroke-[2.5]" />
              <span>Not quite. Try another clue.</span>
            </div>

            <p className="text-ink-600 text-xs leading-relaxed">
              The next clue describes the same meaning. The answer stays hidden.
            </p>

            <div className="pt-1">
              <button
                type="button"
                onClick={onHardNextClue}
                className="inline-flex items-center gap-1.5 rounded-xl border border-ink-200 bg-white hover:bg-ink-50 px-4 py-2 text-xs font-bold text-ink-800 shadow-2xs transition-all active:scale-[0.98]"
              >
                <span>Next clue · {currentAttemptIndex + 2} / 3</span>
                <ArrowRight className="w-3.5 h-3.5 text-brand-600" />
              </button>
            </div>
          </div>
        )}

        {/* 3. Hard Mode - Correct Answer (Attempt 1, 2, or 3) */}
        {difficulty === "hard" && isHardAttemptLocked && isCurrentHardCorrect && (
          <div className="p-4 rounded-2xl border border-emerald-200 bg-emerald-50/80 text-xs font-medium space-y-1.5 mt-3 animate-fade-up">
            <div className="font-bold text-sm text-emerald-800 flex items-center gap-1.5">
              <Check className="w-4 h-4 text-emerald-600 stroke-[2.5]" />
              <span>Correct. That&apos;s the word.</span>
            </div>

            <div className="text-ink-900 font-bold text-sm pt-1">
              {questionData.targetWord}
            </div>

            <div className="text-ink-700">
              {questionData.definition}
              {questionData.partOfSpeech && ` Here, "${questionData.targetWord}" is a ${questionData.partOfSpeech}.`}
            </div>

            {questionData.fullSentence && (
              <div className="text-ink-600 italic">
                {questionData.fullSentence}
              </div>
            )}
          </div>
        )}

        {/* 4. Hard Mode - 3rd Attempt Failed -> Reveal Answer */}
        {difficulty === "hard" && hasFailedAllHardClues && (
          <div className="p-4 rounded-2xl border border-amber-200 bg-amber-50/80 text-xs font-medium space-y-1.5 mt-3 animate-fade-up">
            <div className="font-bold text-sm text-amber-900 flex items-center gap-1.5">
              <X className="w-4 h-4 text-amber-700 stroke-[2.5]" />
              <span>
                All 3 clues used. The target word was &ldquo;
                <span className="underline font-extrabold">{questionData.targetWord}</span>
                &rdquo;.
              </span>
            </div>

            <div className="text-ink-900 font-bold text-sm pt-1">
              {questionData.targetWord}
            </div>

            <div className="text-ink-700">
              {questionData.definition}
              {questionData.partOfSpeech && ` Here, "${questionData.targetWord}" is a ${questionData.partOfSpeech}.`}
            </div>

            {questionData.fullSentence && (
              <div className="text-ink-600 italic">
                {questionData.fullSentence}
              </div>
            )}
          </div>
        )}
      </form>

      {/* ── Footer Tip ── */}
      <div className="rounded-2xl border border-ink-100 bg-ink-50/60 p-3.5 flex items-center gap-2.5 text-xs text-ink-500">
        <Clock className="w-3.5 h-3.5 text-brand-600 shrink-0" />
        <span>
          {difficulty === "easy"
            ? "No rush. Replay as often as you need."
            : "Three clues, one meaning. Take your time."}
        </span>
      </div>
    </div>
  );
}
