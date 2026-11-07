"use client";

import React, { useState, useEffect, useRef } from "react";
import {
  Volume2,
  Briefcase,
  Plane,
  Check,
  X,
  Clock,
  Puzzle,
  Sparkles,
  Lightbulb,
  ChevronDown,
  ArrowRight,
  BookOpen,
} from "lucide-react";

export interface FillBlankQuestionData {
  id: string;
  word?: string;
  baseWord?: string;
  targetForm: string; // The exact form needed in context (e.g., "mitigate", "issues", "deadline")
  acceptableVariants?: string[];
  topic: string;
  partOfSpeech?: string;
  cefrLevel?: string;
  definitionEn?: string;
  definitionVn?: string;
  sentenceWithBlank: string; // "The company has several financial ______ this year."
  completedSentence: string; // "The company has several financial issues this year."
  sentenceVn?: string; // Vietnamese translation
  grammarContextTip?: string; // e.g. "Requires plural noun after quantifier 'several financial'"
  explanation?: string;
  source?: "DATABASE" | "AI_GENERATED" | "DICTIONARY_FALLBACK";
  audioUrl?: string;
}

interface Props {
  questionData: FillBlankQuestionData;
  checkedState: { isChecked: boolean; isCorrect: boolean; userAnswer: string } | null;
  onCheckAnswer: (answer: string) => void;
  onNext?: () => void;
  isLastQuestion?: boolean;
}

function escapeRegExp(str: string): string {
  return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function maskTargetWordInClue(text: string | undefined, word?: string, targetForm?: string): string {
  if (!text) return "";
  let masked = text;
  if (targetForm && targetForm.trim().length > 1) {
    const tfRegex = new RegExp(`\\b${escapeRegExp(targetForm.trim())}\\b`, "gi");
    masked = masked.replace(tfRegex, "___");
  }
  if (word && word.trim().length > 1) {
    const wRegex = new RegExp(`\\b${escapeRegExp(word.trim())}\\b`, "gi");
    masked = masked.replace(wRegex, "___");
  }
  return masked;
}

function sanitizeGrammarTipText(tip: string | undefined, word?: string, targetForm?: string): string {
  if (!tip) return "";
  let cleaned = tip;
  if (targetForm && targetForm.trim().length > 1) {
    const tfRegex = new RegExp(`['"“”‘’]?\\b${escapeRegExp(targetForm.trim())}\\b['"“”‘’]?`, "gi");
    cleaned = cleaned.replace(tfRegex, "");
  }
  if (word && word.trim().length > 1) {
    const wRegex = new RegExp(`['"“”‘’]?\\b${escapeRegExp(word.trim())}\\b['"“”‘’]?`, "gi");
    cleaned = cleaned.replace(wRegex, "");
  }
  return cleaned.replace(/\s{2,}/g, " ").replace(/\s+([.,;:])/g, "$1").trim();
}

export function FillBlankCard({
  questionData,
  checkedState,
  onCheckAnswer,
  onNext,
  isLastQuestion = false,
}: Props) {
  const [inputVal, setInputVal] = useState(checkedState?.userAnswer || "");
  const [isHintOpen, setIsHintOpen] = useState(false);
  const [isPlayingAudio, setIsPlayingAudio] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const isChecked = checkedState !== null && checkedState.isChecked;

  // Focus input on mount and keep focus ready
  useEffect(() => {
    if (!isChecked) {
      inputRef.current?.focus();
    }
  }, [isChecked]);

  // Focus input when user clicks on sentence blank token
  const handleBlankClick = () => {
    if (!isChecked) {
      inputRef.current?.focus();
    }
  };

  // Play audio of the sentence (with blank or completed)
  const playSentenceAudio = () => {
    if (typeof window === "undefined") return;

    if (questionData.audioUrl) {
      setIsPlayingAudio(true);
      const audio = new Audio(questionData.audioUrl);
      audio.onended = () => setIsPlayingAudio(false);
      audio.onerror = () => setIsPlayingAudio(false);
      audio.play().catch(() => setIsPlayingAudio(false));
      return;
    }

    if ("speechSynthesis" in window) {
      setIsPlayingAudio(true);
      window.speechSynthesis.cancel();
      const textToRead = isChecked
        ? questionData.completedSentence
        : questionData.sentenceWithBlank.replace(/_{2,}|\[blank\]/gi, "blank");
      const utterance = new SpeechSynthesisUtterance(textToRead);
      utterance.lang = "en-US";
      utterance.rate = 0.95;
      utterance.onend = () => setIsPlayingAudio(false);
      utterance.onerror = () => setIsPlayingAudio(false);
      window.speechSynthesis.speak(utterance);
    }
  };

  // Play pronunciation of the target word alone
  const playTargetWordAudio = () => {
    if (typeof window === "undefined" || !("speechSynthesis" in window)) return;
    window.speechSynthesis.cancel();
    const wordToRead = questionData.targetForm || questionData.word || "";
    const utterance = new SpeechSynthesisUtterance(wordToRead);
    utterance.lang = "en-US";
    utterance.rate = 0.9;
    window.speechSynthesis.speak(utterance);
  };

  const handleFormSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (isChecked) {
      // If already checked, Enter key triggers advance
      onNext?.();
      return;
    }

    const trimmed = inputVal.trim();
    if (!trimmed) return;
    onCheckAnswer(trimmed);
  };

  // Split sentence into segments around the blank token
  const blankRegex = /_{2,}|\[blank\]/i;
  let segments: string[] = [];
  if (blankRegex.test(questionData.sentenceWithBlank)) {
    segments = questionData.sentenceWithBlank.split(blankRegex);
  } else if (
    questionData.targetForm &&
    questionData.sentenceWithBlank
      .toLowerCase()
      .includes(questionData.targetForm.toLowerCase())
  ) {
    const regex = new RegExp(`\\b${escapeRegExp(questionData.targetForm)}\\b`, "i");
    segments = questionData.sentenceWithBlank.split(regex);
  } else {
    // Graceful fallback: append blank token near the end
    segments = [questionData.sentenceWithBlank.replace(/\.+$/, ""), "."];
  }

  const topicIcon = questionData.topic?.toLowerCase().includes("travel") ? (
    <Plane className="w-3.5 h-3.5 text-emerald-700" aria-hidden="true" />
  ) : (
    <Briefcase className="w-3.5 h-3.5 text-emerald-700" aria-hidden="true" />
  );

  return (
    <div className="rounded-3xl border border-ink-200/90 bg-white p-6 sm:p-9 shadow-card space-y-6 transition-all animate-fade-up">
      {/* ── Top row: Tag & Category Badge ── */}
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-2 text-xs font-semibold text-ink-600">
          <Puzzle className="w-4 h-4 text-emerald-700 shrink-0" aria-hidden="true" />
          <span className="text-emerald-800 font-bold">
            Sentence Context → Live Blank Recall
          </span>
        </div>

        <div className="inline-flex items-center gap-1.5 rounded-xl border border-emerald-200/90 bg-emerald-50/80 px-3 py-1.5 text-xs font-semibold text-emerald-900 shadow-2xs shrink-0">
          {topicIcon}
          <span>{questionData.topic}</span>
        </div>
      </div>

      {/* ── Question Title ── */}
      <div>
        <h2 className="text-2xl sm:text-[28px] font-extrabold tracking-tight text-ink-900 leading-snug">
          Which word completes the sentence?
        </h2>
        <p className="text-xs text-ink-500 mt-1">
          Observe the syntactic structure and type the correct word or inflection.
        </p>
      </div>

      {/* ── Authentic Sentence Context Box with Live-Mirroring Blank ── */}
      <div className="rounded-2xl border border-emerald-200/80 bg-[#f4f9f5] p-5 sm:p-6 space-y-4 transition-all">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="text-[10px] font-extrabold uppercase tracking-widest text-emerald-800">
            AUTHENTIC CONVERSATION CONTEXT
          </div>
          <div className="flex items-center gap-1.5">
            {questionData.partOfSpeech && (
              <span className="text-[11px] font-semibold text-emerald-800 bg-emerald-100/90 border border-emerald-200/80 px-2.5 py-0.5 rounded-md">
                {questionData.partOfSpeech}
              </span>
            )}
            {questionData.cefrLevel && (
              <span className="text-[11px] font-bold text-emerald-900 bg-white/90 border border-emerald-200 px-2 py-0.5 rounded-md shadow-2xs">
                {questionData.cefrLevel}
              </span>
            )}
          </div>
        </div>

        {/* Live-Mirroring Sentence Flow */}
        <p className="text-lg sm:text-xl font-medium text-ink-900 leading-relaxed font-sans">
          {segments.map((segment, idx, arr) => (
            <React.Fragment key={idx}>
              <span>{segment}</span>
              {idx < arr.length - 1 && (
                <span className="inline-block mx-1.5 align-baseline">
                  {!isChecked ? (
                    // Live mirror state while typing
                    inputVal.trim().length > 0 ? (
                      <button
                        type="button"
                        onClick={handleBlankClick}
                        className="inline-flex items-center justify-center min-w-[100px] min-h-[44px] px-3 py-1 rounded-xl border-2 border-emerald-500 bg-emerald-50 text-emerald-950 font-sans text-base sm:text-lg font-bold shadow-xs hover:bg-emerald-100 transition-all cursor-text align-middle focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
                        aria-label={`Blank token currently contains: ${inputVal}. Click to focus input.`}
                      >
                        <span>{inputVal}</span>
                        <span
                          className="w-1.5 h-4 ml-1 bg-emerald-600 rounded-full animate-pulse"
                          aria-hidden="true"
                        />
                      </button>
                    ) : (
                      // Unfilled prompt token
                      <button
                        type="button"
                        onClick={handleBlankClick}
                        className="inline-flex items-center justify-center min-w-[100px] min-h-[44px] px-3 py-1 rounded-xl border-2 border-dashed border-emerald-400 bg-emerald-50/70 text-emerald-800 font-mono text-sm font-bold hover:bg-emerald-100/80 hover:border-emerald-500 transition-all cursor-text align-middle focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 shadow-2xs"
                        aria-label="Sentence blank token: click to type your answer"
                      >
                        <span className="opacity-70 text-xs tracking-wider">type here</span>
                      </button>
                    )
                  ) : checkedState.isCorrect ? (
                    // Verified correct
                    <span className="inline-flex items-center gap-1.5 min-w-[90px] h-9 px-3 py-0.5 rounded-xl border-2 border-emerald-500 bg-emerald-100 text-emerald-950 font-sans text-base sm:text-lg font-bold shadow-xs align-middle">
                      <Check
                        className="w-4 h-4 text-emerald-700 stroke-[3]"
                        aria-hidden="true"
                      />
                      <span>{checkedState.userAnswer}</span>
                    </span>
                  ) : (
                    // Verified incorrect: show user input struck out followed by target form
                    <span className="inline-flex flex-wrap items-center gap-1.5 align-middle">
                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-xl border-2 border-rose-300 bg-rose-50 text-rose-800 line-through text-sm font-semibold">
                        <X
                          className="w-3.5 h-3.5 text-rose-500 stroke-[2.5]"
                          aria-hidden="true"
                        />
                        <span>{checkedState.userAnswer || "(blank)"}</span>
                      </span>
                      <span className="inline-flex items-center gap-1 px-3 py-0.5 rounded-xl border-2 border-emerald-500 bg-emerald-50 text-emerald-900 font-bold text-sm sm:text-base shadow-xs">
                        <Check
                          className="w-3.5 h-3.5 text-emerald-600 stroke-[3]"
                          aria-hidden="true"
                        />
                        <span>{questionData.targetForm}</span>
                      </span>
                    </span>
                  )}
                </span>
              )}
            </React.Fragment>
          ))}
        </p>

        {/* ── Target Word Meaning & Definition Clue (Always visible so learner knows which word to recall) ── */}
        {(questionData.definitionVn || questionData.definitionEn) && (
          <div className="rounded-xl border border-emerald-200/90 bg-white/95 p-3.5 sm:p-4 space-y-1.5 shadow-2xs">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <span className="text-[10px] font-extrabold uppercase tracking-wider text-emerald-800 flex items-center gap-1.5">
                <BookOpen className="w-3.5 h-3.5 text-emerald-600" aria-hidden="true" />
                <span>Target Word Meaning Clue · Gợi ý nghĩa từ cần điền</span>
              </span>
              {questionData.partOfSpeech && (
                <span className="text-[11px] font-bold text-emerald-700">
                  Từ loại: {questionData.partOfSpeech}
                </span>
              )}
            </div>

            {questionData.definitionVn && (
              <div className="flex items-start gap-2 pt-0.5">
                <span className="font-bold text-[10px] uppercase text-emerald-900 bg-emerald-100 border border-emerald-200 px-1.5 py-0.5 rounded shrink-0 mt-0.5">
                  VI
                </span>
                <p className="text-sm sm:text-base font-bold text-emerald-950 leading-snug">
                  {questionData.definitionVn}
                </p>
              </div>
            )}

            {questionData.definitionEn && (
              <div className="flex items-start gap-2 pt-0.5">
                <span className="font-bold text-[10px] uppercase text-ink-600 bg-ink-100 border border-ink-200 px-1.5 py-0.5 rounded shrink-0 mt-0.5">
                  EN
                </span>
                <p className="text-xs sm:text-sm font-medium text-ink-700 italic leading-relaxed">
                  &ldquo;
                  {maskTargetWordInClue(
                    questionData.definitionEn,
                    questionData.word || questionData.baseWord,
                    questionData.targetForm
                  )}
                  &rdquo;
                </p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* ── Expandable Grammar Hint Disclosure Accordion ── */}
      {questionData.grammarContextTip && (
        <div className="pt-0.5">
          <button
            type="button"
            id={`grammar-hint-toggle-${questionData.id}`}
            aria-expanded={isHintOpen}
            aria-controls={`grammar-hint-panel-${questionData.id}`}
            onClick={() => setIsHintOpen((prev) => !prev)}
            className="group inline-flex items-center gap-2 rounded-xl border border-emerald-200/90 bg-emerald-50/60 hover:bg-emerald-100/80 text-emerald-800 px-4 py-2.5 text-xs font-bold transition-all min-h-[44px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 shadow-2xs active:scale-[0.99]"
          >
            <Lightbulb
              className={`w-4 h-4 text-emerald-600 transition-transform ${
                isHintOpen ? "rotate-12 text-emerald-700" : ""
              }`}
              aria-hidden="true"
            />
            <span>{isHintOpen ? "Hide grammar hint" : "Need a grammar hint?"}</span>
            <ChevronDown
              className={`w-4 h-4 text-emerald-600 transition-transform duration-200 ${
                isHintOpen ? "rotate-180" : ""
              }`}
              aria-hidden="true"
            />
          </button>

          {isHintOpen && (
            <div
              id={`grammar-hint-panel-${questionData.id}`}
              role="region"
              aria-labelledby={`grammar-hint-toggle-${questionData.id}`}
              className="mt-2.5 rounded-2xl border border-emerald-200 bg-emerald-50/90 p-4 text-xs text-emerald-950 font-medium space-y-1.5 animate-fade-down shadow-2xs"
            >
              <div className="font-bold text-emerald-800 flex items-center gap-1.5 uppercase tracking-wider text-[11px]">
                <Sparkles className="w-3.5 h-3.5 text-emerald-600" aria-hidden="true" />
                <span>Grammar & Syntactic Context Clue</span>
              </div>
              <p className="text-emerald-950 leading-relaxed font-sans text-sm">
                {sanitizeGrammarTipText(
                  questionData.grammarContextTip,
                  questionData.word || questionData.baseWord,
                  questionData.targetForm
                )}
              </p>
            </div>
          )}
        </div>
      )}

      {/* ── Audio Button Row ── */}
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={playSentenceAudio}
          className={`inline-flex items-center gap-2 rounded-xl border px-4 py-2.5 text-xs font-bold transition-all min-h-[44px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 ${
            isPlayingAudio
              ? "border-emerald-500 bg-emerald-50 text-emerald-800 ring-2 ring-emerald-200"
              : "border-ink-200 bg-white text-ink-700 hover:border-emerald-300 hover:bg-emerald-50/50 shadow-2xs"
          }`}
          aria-label="Listen to sentence audio"
        >
          <Volume2
            className={`w-4 h-4 ${
              isPlayingAudio ? "animate-pulse text-emerald-600" : "text-emerald-700"
            }`}
            aria-hidden="true"
          />
          <span>Listen to sentence</span>
        </button>

        <span className="text-[11px] text-ink-500">
          The blank is spoken as &ldquo;blank&rdquo; prior to answering.
        </span>
      </div>

      {/* ── Typing Input Section ── */}
      <form onSubmit={handleFormSubmit} className="space-y-2 pt-1">
        <label
          htmlFor={`fill-blank-input-${questionData.id}`}
          className="block text-xs font-bold text-ink-800"
        >
          Your answer (word or form)
        </label>

        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
          <div className="relative flex-1">
            <input
              ref={inputRef}
              id={`fill-blank-input-${questionData.id}`}
              type="text"
              autoComplete="off"
              autoFocus
              disabled={isChecked}
              value={inputVal}
              onChange={(e) => setInputVal(e.target.value)}
              placeholder="Type the word that fits..."
              className={`w-full rounded-2xl border px-4 py-3 text-base sm:text-sm font-semibold tracking-wide outline-none min-h-[48px] transition-all ${
                isChecked
                  ? checkedState.isCorrect
                    ? "border-emerald-500 bg-emerald-50/50 text-emerald-950 pr-11 ring-2 ring-emerald-200"
                    : "border-rose-400 bg-rose-50/50 text-rose-950 pr-11 ring-2 ring-rose-200"
                  : "border-ink-200 bg-white text-ink-900 focus:border-emerald-600 focus:ring-4 focus:ring-emerald-100"
              }`}
            />

            {isChecked && (
              <div className="absolute right-3.5 top-1/2 -translate-y-1/2">
                {checkedState.isCorrect ? (
                  <Check
                    className="w-5 h-5 text-emerald-600 stroke-[2.5]"
                    aria-hidden="true"
                  />
                ) : (
                  <X
                    className="w-5 h-5 text-rose-500 stroke-[2.5]"
                    aria-hidden="true"
                  />
                )}
              </div>
            )}
          </div>

          <button
            type="submit"
            disabled={!inputVal.trim() && !isChecked}
            className={`inline-flex items-center justify-center gap-2 rounded-2xl px-6 py-3 text-xs font-bold transition-all shrink-0 min-h-[48px] min-w-[130px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 ${
              isChecked
                ? "bg-ink-100 text-ink-600 hover:bg-ink-200 cursor-pointer shadow-2xs"
                : "bg-emerald-700 hover:bg-emerald-800 disabled:opacity-40 disabled:cursor-not-allowed text-white shadow-sm active:scale-[0.98]"
            }`}
          >
            {isChecked ? (
              <>
                <span>Next Question</span>
                <ArrowRight className="w-4 h-4" aria-hidden="true" />
              </>
            ) : (
              <>
                <Check className="w-4 h-4 stroke-[2.5]" aria-hidden="true" />
                <span>Check Answer</span>
              </>
            )}
          </button>
        </div>

        <p className="text-[11px] text-ink-500">
          {isChecked
            ? "Press Enter or click Continue to proceed."
            : "Type the word in its correct form. Press Enter to check."}
        </p>

        {/* ── Rich Feedback Box (ARIA live polite region) ── */}
        {isChecked && (
          <div
            role="status"
            aria-live="polite"
            className={`p-5 rounded-2xl border text-xs font-medium space-y-3 mt-4 animate-fade-up shadow-2xs ${
              checkedState.isCorrect
                ? "border-emerald-300 bg-emerald-50/90 text-emerald-950"
                : "border-rose-300 bg-rose-50/90 text-rose-950"
            }`}
          >
            {/* Correct/Incorrect Header */}
            <div className="flex items-start justify-between gap-3">
              <div className="font-bold text-base flex items-center gap-2">
                {checkedState.isCorrect ? (
                  <>
                    <div className="w-7 h-7 rounded-xl bg-emerald-200 text-emerald-800 flex items-center justify-center shrink-0">
                      <Check className="w-4 h-4 stroke-[3]" aria-hidden="true" />
                    </div>
                    <span className="text-emerald-900">
                      Correct! Fits the sentence context.
                    </span>
                  </>
                ) : (
                  <>
                    <div className="w-7 h-7 rounded-xl bg-rose-200 text-rose-800 flex items-center justify-center shrink-0">
                      <X className="w-4 h-4 stroke-[3]" aria-hidden="true" />
                    </div>
                    <span className="text-rose-950">
                      The correct answer is &ldquo;
                      <span className="font-extrabold underline text-emerald-900">
                        {questionData.targetForm}
                      </span>
                      &rdquo;
                      {questionData.baseWord &&
                        questionData.baseWord !== questionData.targetForm && (
                          <span className="font-normal text-ink-600">
                            {" "}
                            (base form: {questionData.baseWord})
                          </span>
                        )}
                    </span>
                  </>
                )}
              </div>

              {questionData.source && (
                <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-md bg-white/90 border border-ink-200 text-ink-600 shrink-0">
                  {questionData.source === "AI_GENERATED"
                    ? "Hybrid AI"
                    : "Curated"}
                </span>
              )}
            </div>

            {/* Completed Sentence & Translation */}
            <div className="rounded-xl bg-white/90 border border-ink-200/80 p-3.5 space-y-2">
              <div className="text-[10px] font-bold uppercase tracking-wider text-ink-500">
                Completed Full Sentence
              </div>
              <p className="text-ink-900 font-semibold text-base leading-relaxed">
                &ldquo;{questionData.completedSentence}&rdquo;
              </p>

              {questionData.sentenceVn && (
                <div className="pt-2 border-t border-ink-100 flex items-start gap-2 text-ink-600 text-sm">
                  <span className="font-bold text-xs uppercase text-emerald-800 bg-emerald-100/90 px-1.5 py-0.5 rounded shrink-0">
                    VI
                  </span>
                  <span className="leading-relaxed">{questionData.sentenceVn}</span>
                </div>
              )}
            </div>

            {/* Rich Grammar / Lexical Explanation */}
            {questionData.explanation && (
              <div className="text-ink-800 leading-relaxed text-sm bg-white/70 rounded-xl p-3.5 border border-ink-100">
                <span className="font-bold text-ink-900 block mb-0.5">
                  Explanation & Usage:
                </span>
                <p>{questionData.explanation}</p>
              </div>
            )}

            {/* Audio Pronunciation & Action Buttons */}
            <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={playTargetWordAudio}
                  className="inline-flex items-center gap-1.5 rounded-xl border border-ink-200 bg-white hover:bg-emerald-50 hover:border-emerald-300 text-ink-800 px-3.5 py-2.5 text-xs font-bold transition-all min-h-[44px] min-w-[44px] shadow-2xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
                  aria-label={`Listen to pronunciation of target word ${questionData.targetForm}`}
                >
                  <Volume2 className="w-4 h-4 text-emerald-700" aria-hidden="true" />
                  <span>Pronounce word</span>
                </button>

                <button
                  type="button"
                  onClick={playSentenceAudio}
                  className="inline-flex items-center gap-1.5 rounded-xl border border-ink-200 bg-white hover:bg-emerald-50 hover:border-emerald-300 text-ink-800 px-3.5 py-2.5 text-xs font-bold transition-all min-h-[44px] min-w-[44px] shadow-2xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
                  aria-label="Listen to full sentence audio"
                >
                  <Volume2 className="w-4 h-4 text-emerald-700" aria-hidden="true" />
                  <span>Full sentence</span>
                </button>
              </div>

              {onNext && (
                <button
                  type="button"
                  onClick={onNext}
                  className="inline-flex items-center gap-2 rounded-xl bg-emerald-700 hover:bg-emerald-800 text-white px-5 py-2.5 text-xs font-bold shadow-sm transition-all min-h-[44px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 active:scale-[0.98]"
                >
                  <span>{isLastQuestion ? "Complete session" : "Continue"}</span>
                  <ArrowRight className="w-4 h-4" aria-hidden="true" />
                </button>
              )}
            </div>
          </div>
        )}
      </form>

      {/* ── Footer Guidance ── */}
      <div className="rounded-2xl border border-ink-100 bg-ink-50/70 p-3.5 flex items-center gap-2.5 text-xs text-ink-600">
        <Clock className="w-4 h-4 text-emerald-700 shrink-0" aria-hidden="true" />
        <span className="flex-1">
          Active sentence recall strengthens morphological awareness and grammatical precision.
        </span>
        <div className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-800 shrink-0">
          <BookOpen className="w-3.5 h-3.5" aria-hidden="true" />
          <span>CEFR Context</span>
        </div>
      </div>
    </div>
  );
}
