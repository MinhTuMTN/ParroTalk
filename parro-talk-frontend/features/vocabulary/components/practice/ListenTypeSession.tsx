"use client";

import React, { useState } from "react";
import Link from "next/link";
import { ArrowRight, CheckCircle2, RotateCcw, Sprout } from "lucide-react";
import {
  ListenTypeCard,
  type ListenTypeQuestionData,
  type HardModeAttempt,
} from "./ListenTypeCard";
import type { PracticeQuestionDto } from "../../types";

interface Props {
  topic?: string;
  questions?: PracticeQuestionDto[];
  initialDifficulty?: "easy" | "hard";
  modeKey?: string;
  onComplete?: () => void;
}

function buildClues(q: PracticeQuestionDto, topic: string): [string, string, string] {
  const word = q.correctAnswer || "word";
  const firstLetter = (q.firstLetter || word[0] || "").toUpperCase();
  const charCount = q.charCount ?? word.length;

  const fallbackClue1 =
    q.wordHint ||
    (q.definition ? `Definition: ${q.definition}` : `A key word in ${topic} related to "${word}".`);
  const fallbackClue2 =
    q.sentenceWithBlank ||
    (q.prompt && q.prompt.includes("___")
      ? `Context: ${q.prompt}`
      : `Think about the context of "${topic}".`);
  const fallbackClue3 = `This word begins with the letter "${firstLetter}" and has ${charCount} letters.`;

  if (q.clues && q.clues.length > 0) {
    const clue0 = q.clues[0]?.trim() || fallbackClue1;
    const clue1 = (q.clues.length > 1 && q.clues[1]?.trim()) || fallbackClue2;
    const clue2 = (q.clues.length > 2 && q.clues[2]?.trim()) || fallbackClue3;
    return [clue0, clue1, clue2];
  }

  return [fallbackClue1, fallbackClue2, fallbackClue3];
}

export function ListenTypeSession({
  topic = "Business",
  questions,
  initialDifficulty = "easy",
  modeKey = "listen-type",
  onComplete,
}: Props) {
  const [difficulty, setDifficulty] = useState<"easy" | "hard">(initialDifficulty);

  // Map question list from prop
  const sessionQuestions: ListenTypeQuestionData[] = React.useMemo(() => {
    if (!questions || questions.length === 0) return [];

    return questions
      .filter((q) => q.correctAnswer)
      .map((q, idx) => {
        const targetWord = q.correctAnswer;
        const definition =
          q.definition ||
          q.explanation ||
          `The focus vocabulary word "${targetWord}".`;
        const sentenceWithBlank =
          q.sentenceWithBlank ||
          (q.prompt && q.prompt.includes("___")
            ? q.prompt
            : `Fill in the blank with ${targetWord}: ______`);
        const fullSentence =
          q.fullSentence ||
          q.exampleSentence ||
          q.explanation ||
          `${q.prompt} (${targetWord})`;
        const easySentence =
          q.easySentence ||
          q.exampleSentence ||
          q.explanation ||
          `${q.prompt} (${targetWord})`;
        const explanation =
          q.explanation ||
          q.fullSentence ||
          q.exampleSentence ||
          `Sample context: ${q.prompt}`;

        return {
          id: q.id || `lt-${idx}`,
          targetWord,
          topic: topic || "Vocabulary",
          partOfSpeech: "noun",
          definition,
          explanation,
          sentenceWithBlank,
          fullSentence,
          easySentence,
          clues: buildClues(q, topic || "Vocabulary"),
          audioUrl: q.audioUrl,
        };
      });
  }, [topic, questions]);

  const [currentIndex, setCurrentIndex] = useState(0);

  // Easy mode state
  const [easyCheckedMap, setEasyCheckedMap] = useState<
    Record<string, { isChecked: boolean; isCorrect: boolean; userAnswer: string }>
  >({});

  // Hard mode state
  const [hardAttemptsMap, setHardAttemptsMap] = useState<
    Record<
      string,
      {
        attempts: HardModeAttempt[];
        currentAttemptIndex: number;
        isLocked: boolean;
        isFinished: boolean;
      }
    >
  >({});

  const [isCompletedSession, setIsCompletedSession] = useState(false);

  const total = sessionQuestions.length;

  // Switch difficulty: resets questions state as indicated in UI
  const handleSelectDifficulty = (newDiff: "easy" | "hard") => {
    if (newDiff === difficulty) return;
    setDifficulty(newDiff);
    setCurrentIndex(0);
    setEasyCheckedMap({});
    setHardAttemptsMap({});
    setIsCompletedSession(false);
  };

  const handleRestartSession = () => {
    setCurrentIndex(0);
    setEasyCheckedMap({});
    setHardAttemptsMap({});
    setIsCompletedSession(false);
  };

  if (total === 0) {
    return (
      <div className="max-w-xl mx-auto py-12 text-center space-y-4">
        <p className="text-sm text-ink-500">
          No practice questions available for {topic}.
        </p>
        <Link
          href={`/vocabulary/topics/${encodeURIComponent(topic)}`}
          className="inline-flex items-center justify-center gap-2 rounded-2xl bg-brand-600 hover:bg-brand-700 px-6 py-3 text-xs font-bold text-white shadow-brand transition-all"
        >
          <span>Back to {topic}</span>
        </Link>
      </div>
    );
  }

  const currentQ = sessionQuestions[currentIndex] || sessionQuestions[0];

  // Easy mode handlers
  const currentEasyState = easyCheckedMap[currentQ.id] || null;

  const handleEasyCheckAnswer = (answer: string) => {
    const isCorrect = answer.trim().toLowerCase() === currentQ.targetWord.trim().toLowerCase();
    setEasyCheckedMap((prev) => ({
      ...prev,
      [currentQ.id]: {
        isChecked: true,
        isCorrect,
        userAnswer: answer.trim(),
      },
    }));
  };

  // Hard mode handlers
  const currentHardData = hardAttemptsMap[currentQ.id] || {
    attempts: [],
    currentAttemptIndex: 0,
    isLocked: false,
    isFinished: false,
  };

  const handleHardCheckAttempt = (answer: string) => {
    const isCorrect = answer.trim().toLowerCase() === currentQ.targetWord.trim().toLowerCase();
    const newAttempt: HardModeAttempt = {
      userAnswer: answer.trim(),
      isCorrect,
    };
    const updatedAttempts = [...currentHardData.attempts, newAttempt];
    const isFinished = isCorrect || currentHardData.currentAttemptIndex >= 2;

    setHardAttemptsMap((prev) => ({
      ...prev,
      [currentQ.id]: {
        attempts: updatedAttempts,
        currentAttemptIndex: currentHardData.currentAttemptIndex,
        isLocked: true,
        isFinished,
      },
    }));
  };

  const handleHardNextClue = () => {
    if (currentHardData.currentAttemptIndex >= 2) return;
    setHardAttemptsMap((prev) => ({
      ...prev,
      [currentQ.id]: {
        ...prev[currentQ.id],
        currentAttemptIndex: prev[currentQ.id].currentAttemptIndex + 1,
        isLocked: false,
      },
    }));
  };

  // Can the user proceed to the Next Question?
  const canProceedNext =
    difficulty === "easy"
      ? Boolean(currentEasyState?.isChecked)
      : Boolean(currentHardData.isFinished);

  // Count how many questions are finished so far
  const finishedCount =
    difficulty === "easy"
      ? Object.keys(easyCheckedMap).length
      : Object.values(hardAttemptsMap).filter((d) => d.isFinished).length;

  // Advance question or complete
  const handleNextQuestion = () => {
    if (!canProceedNext) return;

    if (currentIndex + 1 < total) {
      setCurrentIndex((prev) => prev + 1);
    } else {
      setIsCompletedSession(true);
      // Persist progress to localStorage for both listen-type and fill-blank
      if (typeof window !== "undefined" && topic) {
        try {
          const key = `parrotalk_mode_progress_${topic.toLowerCase().replace(/\s+/g, "_")}`;
          const stored = localStorage.getItem(key);
          const map = stored ? JSON.parse(stored) : {};

          // Update the current mode and its partner mode
          map[modeKey] = {
            completed: total,
            total,
            status: "completed",
          };
          if (modeKey === "listen-type") {
            map["fill-blank"] = {
              completed: total,
              total,
              status: "completed",
            };
          }

          localStorage.setItem(key, JSON.stringify(map));
        } catch (err) {
          console.error("Failed to save progress", err);
        }
      }
      onComplete?.();
    }
  };

  // Completion summary screen
  if (isCompletedSession) {
    const correctCount =
      difficulty === "easy"
        ? Object.values(easyCheckedMap).filter((s) => s.isCorrect).length
        : Object.values(hardAttemptsMap).filter((h) => h.attempts.some((a) => a.isCorrect)).length;

    return (
      <div className="max-w-xl mx-auto py-8 text-center space-y-6 animate-fade-up">
        <div className="w-16 h-16 rounded-3xl bg-brand-50 border border-brand-200 text-brand-600 mx-auto flex items-center justify-center shadow-sm">
          <CheckCircle2 className="w-8 h-8 stroke-[2.5]" />
        </div>

        <div className="space-y-2">
          <h2 className="text-2xl sm:text-3xl font-extrabold text-ink-900 tracking-tight">
            Session Completed!
          </h2>
          <p className="text-sm text-ink-500 max-w-md mx-auto">
            You finished all {total} words in <span className="font-semibold text-brand-700 capitalize">{difficulty} mode</span>.
          </p>
        </div>

        {/* Score pill */}
        <div className="inline-flex items-center gap-3 rounded-2xl bg-brand-50 border border-brand-200/80 px-6 py-3.5 text-brand-800 shadow-sm">
          <span className="text-2xl font-black">{correctCount} / {total}</span>
          <span className="text-xs font-semibold text-brand-700">Correct answers</span>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-4">
          <button
            type="button"
            onClick={handleRestartSession}
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-2xl border border-ink-200 bg-white hover:bg-ink-50 px-5 py-3 text-xs font-bold text-ink-800 shadow-sm transition-all"
          >
            <RotateCcw className="w-4 h-4 text-brand-600" />
            <span>Practice Again</span>
          </button>

          <Link
            href={`/vocabulary/topics/${encodeURIComponent(topic)}`}
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-2xl bg-brand-600 hover:bg-brand-700 px-6 py-3 text-xs font-bold text-white shadow-brand transition-all"
          >
            <span>Back to {topic}</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* ── Top Header ── */}
      <div className="space-y-1">
        <div className="flex items-center justify-between">
          <div className="text-[11px] font-bold tracking-widest uppercase text-brand-700">
            YOUR FOCUSED LEARNING SESSION
          </div>
          <button
            type="button"
            className="rounded-xl border border-ink-200 bg-white px-3 py-1 text-[11px] font-semibold text-ink-600 shadow-2xs hover:bg-ink-50 transition-colors"
          >
            Practice session
          </button>
        </div>

        <h1 className="text-3xl font-extrabold tracking-tight text-ink-900">
          Listen &amp; Type
        </h1>
        <p className="text-xs text-ink-500">
          {topic} · Hear it. Recall it. Make it stick.
        </p>
      </div>

      {/* ── Difficulty Switcher ── */}
      <div className="space-y-1.5 pt-1">
        <div className="flex items-center justify-between gap-4">
          <div className="inline-flex items-center rounded-2xl bg-ink-100/90 p-1 border border-ink-200/60 shadow-2xs">
            <button
              type="button"
              onClick={() => handleSelectDifficulty("easy")}
              className={`rounded-xl px-5 py-1.5 text-xs font-bold transition-all ${
                difficulty === "easy"
                  ? "bg-white text-brand-800 shadow-sm"
                  : "text-ink-600 hover:text-ink-900"
              }`}
            >
              Easy
            </button>
            <button
              type="button"
              onClick={() => handleSelectDifficulty("hard")}
              className={`rounded-xl px-5 py-1.5 text-xs font-bold transition-all ${
                difficulty === "hard"
                  ? "bg-white text-brand-800 shadow-sm"
                  : "text-ink-600 hover:text-ink-900"
              }`}
            >
              Hard
            </button>
          </div>

          <div className="text-xs font-bold text-brand-700">
            {difficulty === "easy" ? "Listen → recall" : "Understand → infer"}
          </div>
        </div>

        <p className="text-[11px] text-ink-400">
          Changing difficulty starts a fresh session and clears these results.
        </p>
      </div>

      {/* ── Progress Indicators ── */}
      <div className="space-y-2 pt-1">
        <div className="flex items-center justify-between text-xs font-semibold">
          <span className="text-brand-800 font-bold">
            Question {currentIndex + 1} / {total}
          </span>
          <span className="text-ink-400">
            {finishedCount} of {total} finished
          </span>
        </div>

        {/* Segmented Progress Bars */}
        <div
          className="grid gap-2"
          style={{ gridTemplateColumns: `repeat(${total}, minmax(0, 1fr))` }}
        >
          {Array.from({ length: total }).map((_, i) => {
            const isFinished = i < currentIndex || (i === currentIndex && canProceedNext);
            const isCurrent = i === currentIndex;
            return (
              <div
                key={i}
                className={`h-1.5 rounded-full transition-all duration-300 ${
                  isFinished || isCurrent ? "bg-brand-600" : "bg-brand-100"
                }`}
              />
            );
          })}
        </div>
      </div>

      {/* ── Main Question Card ── */}
      <ListenTypeCard
        key={`${currentQ.id}-${difficulty}`}
        difficulty={difficulty}
        questionData={currentQ}
        easyCheckedState={currentEasyState}
        onEasyCheckAnswer={handleEasyCheckAnswer}
        hardAttempts={currentHardData.attempts}
        currentAttemptIndex={currentHardData.currentAttemptIndex}
        isHardAttemptLocked={currentHardData.isLocked}
        isHardQuestionFinished={currentHardData.isFinished}
        onHardCheckAttempt={handleHardCheckAttempt}
        onHardNextClue={handleHardNextClue}
      />

      {/* ── Bottom Action Row ── */}
      <div className="flex items-center justify-between gap-4 pt-2">
        <span className="text-xs text-ink-500 font-medium">
          {!canProceedNext
            ? difficulty === "easy"
              ? "Check your answer to continue."
              : "Answer correctly or finish all three clues to continue."
            : "Continue when you're ready."}
        </span>

        <button
          type="button"
          onClick={handleNextQuestion}
          disabled={!canProceedNext}
          className="inline-flex items-center gap-2 rounded-2xl bg-brand-600 hover:bg-brand-700 disabled:bg-[#d2e8db] disabled:text-brand-700/60 disabled:cursor-not-allowed px-6 py-3 text-xs font-bold text-white shadow-sm transition-all"
        >
          <span>Next question</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* ── Bottom Microcopy ── */}
      <div className="text-center space-y-1 pt-6 text-[11px] text-ink-400">
        <p>Listen carefully and type the word you hear.</p>
        <div className="inline-flex items-center gap-1.5 text-brand-700/80 font-medium">
          <Sprout className="w-3.5 h-3.5" />
          <span>One word at a time. Let it sink in.</span>
        </div>
      </div>
    </div>
  );
}
