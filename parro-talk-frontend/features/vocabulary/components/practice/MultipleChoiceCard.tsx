"use client";

import React, { useState, useEffect } from "react";
import { Volume2, Briefcase, Plane, Check, X, Clock, HelpCircle } from "lucide-react";

export type McState = "UNANSWERED" | "ANSWERED" | "ADVANCING" | "COMPLETED";

export interface MultipleChoiceOption {
  key: "A" | "B" | "C" | "D";
  text: string;
  isCorrect: boolean;
}

export interface MultipleChoiceQuestionData {
  id: string;
  word: string;
  topic: string;
  question: string;
  sentence?: string;
  phonetic?: string;
  partOfSpeech?: string;
  explanation?: string;
  audioUrl?: string;
  options: MultipleChoiceOption[];
}

interface Props {
  questionData: MultipleChoiceQuestionData;
  state?: McState;
  selectedOptionKey: string | null;
  onSelectOption: (key: "A" | "B" | "C" | "D", isCorrect: boolean) => void;
  onAdvance?: () => void;
}

export function MultipleChoiceCard({
  questionData,
  selectedOptionKey,
  onSelectOption,
  onAdvance,
}: Props) {
  const [isPlaying, setIsPlaying] = useState(false);

  const isAnswered = selectedOptionKey !== null;
  const chosenOption = questionData.options.find((o) => o.key === selectedOptionKey);
  const isCorrectChoice = Boolean(chosenOption?.isCorrect);

  // Keyboard navigation: 1-4 or A-D to select, Enter to advance
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Ignore if typing inside input, textarea or select
      if (
        e.target instanceof HTMLInputElement ||
        e.target instanceof HTMLTextAreaElement ||
        e.target instanceof HTMLSelectElement
      ) {
        return;
      }

      if (isAnswered) {
        if (e.key === "Enter") {
          e.preventDefault();
          onAdvance?.();
        }
        return;
      }

      const keyUpper = e.key.toUpperCase();
      let targetIndex = -1;

      if (e.key === "1" || keyUpper === "A") targetIndex = 0;
      else if (e.key === "2" || keyUpper === "B") targetIndex = 1;
      else if (e.key === "3" || keyUpper === "C") targetIndex = 2;
      else if (e.key === "4" || keyUpper === "D") targetIndex = 3;

      if (targetIndex >= 0 && targetIndex < questionData.options.length) {
        e.preventDefault();
        const opt = questionData.options[targetIndex];
        onSelectOption(opt.key, opt.isCorrect);
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isAnswered, onAdvance, onSelectOption, questionData.options]);

  const playSentenceAudio = () => {
    const textToRead = questionData.sentence || questionData.word;
    setIsPlaying(true);

    if (questionData.audioUrl) {
      const audio = new Audio(questionData.audioUrl);
      audio.onended = () => setIsPlaying(false);
      audio.onerror = () => fallbackSpeech(textToRead);
      audio.play().catch(() => fallbackSpeech(textToRead));
    } else {
      fallbackSpeech(textToRead);
    }
  };

  const fallbackSpeech = (text: string) => {
    if (typeof window !== "undefined" && "speechSynthesis" in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = "en-US";
      utterance.rate = 0.95;
      utterance.onend = () => setIsPlaying(false);
      utterance.onerror = () => setIsPlaying(false);
      window.speechSynthesis.speak(utterance);
    } else {
      setIsPlaying(false);
    }
  };

  const topicIcon = questionData.topic?.toLowerCase().includes("travel") ? (
    <Plane className="w-3.5 h-3.5 text-brand-600" />
  ) : (
    <Briefcase className="w-3.5 h-3.5 text-brand-600" />
  );

  return (
    <div className="rounded-3xl border border-ink-200/80 bg-white p-6 sm:p-9 shadow-card space-y-6 transition-all animate-fade-up">
      {/* ── Top row: Tag & Category Badge ── */}
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-2 text-xs font-semibold text-ink-500">
          <HelpCircle className="w-3.5 h-3.5 text-brand-600 shrink-0" />
          <span className="text-ink-600 font-bold">Multiple Choice Quiz</span>
          {questionData.partOfSpeech && (
            <span className="rounded-md bg-ink-100 px-2 py-0.5 text-[11px] font-semibold text-ink-600">
              {questionData.partOfSpeech}
            </span>
          )}
          {questionData.phonetic && (
            <span className="hidden sm:inline font-mono text-[11px] text-ink-400">
              {questionData.phonetic}
            </span>
          )}
        </div>

        <div className="inline-flex items-center gap-1.5 rounded-xl border border-brand-200/60 bg-brand-50/70 px-3 py-1.5 text-xs font-semibold text-brand-700 shadow-sm shrink-0">
          {topicIcon}
          <span>{questionData.topic}</span>
        </div>
      </div>

      {/* ── Question Title ── */}
      <div className="space-y-1">
        <h2 className="text-2xl sm:text-[28px] font-extrabold tracking-tight text-ink-900 leading-snug">
          {questionData.question}
        </h2>
        <p className="text-xs text-ink-400">
          Select the correct definition using keys 1–4 or A–D.
        </p>
      </div>

      {/* ── Context Box (Sentence in conversation) ── */}
      {questionData.sentence && (
        <div className="rounded-2xl border border-brand-200/60 bg-brand-50/60 p-4 sm:p-5 space-y-1.5">
          <div className="text-[10px] font-bold uppercase tracking-wider text-brand-700">
            IN A CONVERSATION
          </div>
          <p className="text-sm font-semibold text-ink-900 leading-relaxed font-sans">
            &ldquo;{questionData.sentence}&rdquo;
          </p>
        </div>
      )}

      {/* ── Audio Pronunciation Button ── */}
      <div>
        <button
          type="button"
          onClick={playSentenceAudio}
          className={`inline-flex items-center gap-2 rounded-xl border px-3.5 py-2 text-xs font-semibold transition-all ${
            isPlaying
              ? "border-brand-500 bg-brand-50 text-brand-700 ring-2 ring-brand-200"
              : "border-brand-200/80 bg-white text-brand-700 hover:border-brand-300 hover:bg-brand-50/50 shadow-2xs"
          }`}
        >
          <Volume2 className={`w-3.5 h-3.5 ${isPlaying ? "animate-pulse" : ""}`} />
          <span>{questionData.sentence ? "Listen to sentence" : "Listen to word"}</span>
        </button>
      </div>

      {/* ── 4 Options (A, B, C, D) ── */}
      <div
        role="radiogroup"
        aria-label={`Options for: ${questionData.question}`}
        aria-required="true"
        className="space-y-3"
      >
        {questionData.options.map((opt, idx) => {
          const isSelected = selectedOptionKey === opt.key;
          let optionStyles =
            "border-ink-200/80 bg-white hover:border-brand-300 hover:bg-brand-50/30 text-ink-800";
          let badgeStyles = "border-ink-200 bg-ink-50 text-ink-600";

          if (isAnswered) {
            if (opt.isCorrect) {
              optionStyles =
                "border-emerald-500 bg-emerald-50/90 text-emerald-950 ring-2 ring-emerald-200";
              badgeStyles = "border-emerald-500 bg-emerald-600 text-white font-bold";
            } else if (isSelected && !opt.isCorrect) {
              optionStyles =
                "border-rose-400 bg-rose-50/90 text-rose-950 ring-2 ring-rose-200";
              badgeStyles = "border-rose-400 bg-rose-500 text-white font-bold";
            } else {
              optionStyles = "border-ink-200/50 bg-ink-50/30 text-ink-400 opacity-50";
              badgeStyles = "border-ink-200 bg-ink-100 text-ink-400";
            }
          }

          return (
            <button
              key={opt.key}
              type="button"
              role="radio"
              aria-checked={isSelected}
              aria-disabled={isAnswered}
              disabled={isAnswered}
              tabIndex={isAnswered ? (isSelected ? 0 : -1) : 0}
              onClick={() => onSelectOption(opt.key, opt.isCorrect)}
              className={`min-h-[52px] w-full text-left p-3.5 sm:p-4 rounded-2xl border transition-all flex items-center justify-between gap-4 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2 ${optionStyles} ${
                isAnswered ? "cursor-default" : "cursor-pointer active:scale-[0.99]"
              }`}
            >
              <div className="flex items-center gap-3.5 min-w-0">
                <span
                  className={`w-8 h-8 rounded-xl border flex items-center justify-center text-xs font-bold shrink-0 transition-colors shadow-2xs ${badgeStyles}`}
                >
                  {opt.key}
                </span>
                <span className="text-sm font-semibold leading-snug">{opt.text}</span>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                {!isAnswered && (
                  <span className="hidden sm:inline-block font-mono text-[10px] text-ink-400 bg-ink-50 border border-ink-200/60 rounded px-1.5 py-0.5">
                    {idx + 1}
                  </span>
                )}

                {isAnswered && (
                  <div>
                    {opt.isCorrect ? (
                      <span className="inline-flex items-center gap-1 text-emerald-700 text-xs font-bold bg-emerald-100/80 px-2 py-0.5 rounded-lg">
                        <Check className="w-3.5 h-3.5 stroke-[2.5]" />
                        <span>Correct</span>
                      </span>
                    ) : isSelected ? (
                      <span className="inline-flex items-center gap-1 text-rose-700 text-xs font-bold bg-rose-100/80 px-2 py-0.5 rounded-lg">
                        <X className="w-3.5 h-3.5 stroke-[2.5]" />
                        <span>Your choice</span>
                      </span>
                    ) : null}
                  </div>
                )}
              </div>
            </button>
          );
        })}
      </div>

      {/* ── Explanation Drawer (concise explanation upon answering) ── */}
      {isAnswered && (
        <div
          role="region"
          aria-label="Answer explanation"
          className={`rounded-2xl border p-4 sm:p-5 space-y-2 animate-fade-up ${
            isCorrectChoice
              ? "border-emerald-200 bg-emerald-50/70 text-emerald-950"
              : "border-rose-200 bg-rose-50/70 text-rose-950"
          }`}
        >
          <div className="flex items-center gap-2 font-bold text-sm">
            {isCorrectChoice ? (
              <>
                <Check className="w-4 h-4 text-emerald-600 stroke-[2.5]" />
                <span className="text-emerald-900 font-extrabold">Great job!</span>
              </>
            ) : (
              <>
                <X className="w-4 h-4 text-rose-600 stroke-[2.5]" />
                <span className="text-rose-900 font-extrabold">Review &amp; Learn</span>
              </>
            )}
          </div>

          <p className="text-xs sm:text-sm leading-relaxed text-ink-800 font-sans">
            {questionData.explanation ||
              `"${questionData.word}" means: ${
                questionData.options.find((o) => o.isCorrect)?.text || ""
              }.`}
          </p>

          <div className="flex items-center justify-between text-[11px] text-ink-500 pt-2 border-t border-ink-200/50">
            <span>
              Press <kbd className="font-mono bg-white px-1.5 py-0.5 rounded border border-ink-200 font-bold shadow-2xs">Enter ↵</kbd> or choose Next question below
            </span>
          </div>
        </div>
      )}

      {/* ── Footer Tip ── */}
      <div className="rounded-2xl border border-ink-100 bg-ink-50/60 p-3.5 flex items-center gap-2.5 text-xs text-ink-500">
        <Clock className="w-3.5 h-3.5 text-brand-600 shrink-0" />
        <span>Keyboard shortcuts active: press 1–4 or A–D to pick an option.</span>
      </div>
    </div>
  );
}
