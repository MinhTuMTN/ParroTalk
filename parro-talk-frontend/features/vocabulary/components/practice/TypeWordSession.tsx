"use client";

import React, { useState } from "react";
import Link from "next/link";
import { ArrowRight, CheckCircle2, RotateCcw, Sprout } from "lucide-react";
import { TypeWordCard, type TypeWordQuestionData } from "./TypeWordCard";
import type { PracticeQuestionDto } from "../../types";

interface Props {
  topic?: string;
  questions?: PracticeQuestionDto[];
  onComplete?: () => void;
}

export function TypeWordSession({ topic = "Business", questions, onComplete }: Props) {
  const typeQuestions: TypeWordQuestionData[] = React.useMemo(() => {
    if (!questions || questions.length === 0) return [];

    return questions
      .filter((q) => q.correctAnswer)
      .map((q, idx) => ({
        id: q.id || `tw-${idx}`,
        targetWord: q.correctAnswer,
        topic: topic || "Vocabulary",
        partOfSpeech: "noun",
        definition:
          q.definition ||
          q.explanation ||
          `The key vocabulary word meaning "${q.correctAnswer}".`,
        synonymOrClue: q.synonymOrClue || q.wordHint || "vocabulary recall",
        charCount: q.charCount ?? q.correctAnswer.length,
        firstLetter: q.firstLetter || q.correctAnswer[0] || "",
        exampleSentence: q.exampleSentence || q.prompt || "",
        audioUrl: q.audioUrl,
      }));
  }, [topic, questions]);

  const [currentIndex, setCurrentIndex] = useState(0);
  const [checkedMap, setCheckedMap] = useState<
    Record<string, { isChecked: boolean; isCorrect: boolean; userAnswer: string }>
  >({});
  const [isCompleted, setIsCompleted] = useState(false);

  const total = typeQuestions.length;

  if (total === 0) {
    return (
      <div className="max-w-xl mx-auto py-12 text-center space-y-4">
        <p className="text-sm text-ink-500">
          No practice questions available for {topic}.
        </p>
        <Link
          href={`/vocabulary/topics/${encodeURIComponent(topic)}`}
          className="inline-flex items-center justify-center gap-2 rounded-2xl bg-honey-600 hover:bg-honey-700 px-6 py-3 text-xs font-bold text-white shadow-sm transition-all"
        >
          <span>Back to {topic}</span>
        </Link>
      </div>
    );
  }

  const currentQ = typeQuestions[currentIndex] || typeQuestions[0];
  const currentState = checkedMap[currentQ.id] || null;
  const isChecked = currentState !== null && currentState.isChecked;

  const handleCheckAnswer = (answer: string) => {
    const isCorrect = answer.trim().toLowerCase() === currentQ.targetWord.trim().toLowerCase();
    setCheckedMap((prev) => ({
      ...prev,
      [currentQ.id]: {
        isChecked: true,
        isCorrect,
        userAnswer: answer.trim(),
      },
    }));
  };

  const handleNext = () => {
    if (!isChecked) return;

    if (currentIndex + 1 < total) {
      setCurrentIndex((prev) => prev + 1);
    } else {
      setIsCompleted(true);
      // Persist progress to localStorage
      if (typeof window !== "undefined" && topic) {
        try {
          const key = `parrotalk_mode_progress_${topic.toLowerCase().replace(/\s+/g, "_")}`;
          const stored = localStorage.getItem(key);
          const map = stored ? JSON.parse(stored) : {};
          map["type-word"] = {
            completed: total,
            total,
            status: "completed",
          };
          localStorage.setItem(key, JSON.stringify(map));
        } catch (err) {
          console.error("Failed to save progress", err);
        }
      }
      onComplete?.();
    }
  };

  const handleRestart = () => {
    setCurrentIndex(0);
    setCheckedMap({});
    setIsCompleted(false);
  };

  if (isCompleted) {
    const correctCount = Object.values(checkedMap).filter((s) => s.isCorrect).length;

    return (
      <div className="max-w-xl mx-auto py-8 text-center space-y-6 animate-fade-up">
        <div className="w-16 h-16 rounded-3xl bg-honey-50 border border-honey-200 text-honey-600 mx-auto flex items-center justify-center shadow-sm">
          <CheckCircle2 className="w-8 h-8 stroke-[2.5]" />
        </div>

        <div className="space-y-2">
          <h2 className="text-2xl sm:text-3xl font-extrabold text-ink-900 tracking-tight">
            Word Recall Completed!
          </h2>
          <p className="text-sm text-ink-500 max-w-md mx-auto">
            You practiced active recall for all {total} words in {topic}.
          </p>
        </div>

        <div className="inline-flex items-center gap-3 rounded-2xl bg-honey-50/80 border border-honey-200/80 px-6 py-3.5 text-honey-900 shadow-sm">
          <span className="text-2xl font-black">{correctCount} / {total}</span>
          <span className="text-xs font-semibold text-honey-800">Words recalled correctly</span>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-4">
          <button
            type="button"
            onClick={handleRestart}
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-2xl border border-ink-200 bg-white hover:bg-ink-50 px-5 py-3 text-xs font-bold text-ink-800 shadow-sm transition-all"
          >
            <RotateCcw className="w-4 h-4 text-honey-600" />
            <span>Practice Again</span>
          </button>

          <Link
            href={`/vocabulary/topics/${encodeURIComponent(topic)}`}
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-2xl bg-honey-600 hover:bg-honey-700 px-6 py-3 text-xs font-bold text-white shadow-sm transition-all"
          >
            <span>Back to {topic}</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </div>
    );
  }

  const finishedCount = Object.keys(checkedMap).length;

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* ── Top Header ── */}
      <div className="space-y-1">
        <div className="flex items-center justify-between">
          <div className="text-[11px] font-bold tracking-widest uppercase text-honey-700">
            YOUR FOCUSED LEARNING SESSION
          </div>
          <span className="text-xs font-semibold text-honey-800 bg-honey-50 border border-honey-200 px-2.5 py-1 rounded-xl">
            Word Recall
          </span>
        </div>

        <h1 className="text-3xl font-extrabold tracking-tight text-ink-900">
          Type the Word
        </h1>
        <p className="text-xs text-ink-500">
          {topic} · Read the definition, remember the word, and type it.
        </p>
      </div>

      {/* ── Progress Indicators ── */}
      <div className="space-y-2 pt-1">
        <div className="flex items-center justify-between text-xs font-semibold">
          <span className="text-honey-800 font-bold">
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
            const isFinished = i < currentIndex || (i === currentIndex && isChecked);
            const isCurrent = i === currentIndex;
            return (
              <div
                key={i}
                className={`h-1.5 rounded-full transition-all duration-300 ${
                  isFinished || isCurrent ? "bg-honey-500" : "bg-honey-100"
                }`}
              />
            );
          })}
        </div>
      </div>

      {/* ── Main Question Card ── */}
      <TypeWordCard
        key={currentQ.id}
        questionData={currentQ}
        checkedState={currentState}
        onCheckAnswer={handleCheckAnswer}
      />

      {/* ── Bottom Action Row ── */}
      <div className="flex items-center justify-between gap-4 pt-2">
        <span className="text-xs text-ink-500 font-medium">
          {isChecked
            ? "Continue when you're ready."
            : "Check your answer to continue."}
        </span>

        <button
          type="button"
          onClick={handleNext}
          disabled={!isChecked}
          className="inline-flex items-center gap-2 rounded-2xl bg-honey-600 hover:bg-honey-700 disabled:bg-[#f0ede6] disabled:text-ink-400 disabled:cursor-not-allowed px-6 py-3 text-xs font-bold text-white shadow-sm transition-all"
        >
          <span>Next question</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* ── Bottom Microcopy ── */}
      <div className="text-center space-y-1 pt-6 text-[11px] text-ink-400">
        <p>Active vocabulary retrieval helps transfer words into long-term memory.</p>
        <div className="inline-flex items-center gap-1.5 text-honey-700/80 font-medium">
          <Sprout className="w-3.5 h-3.5" />
          <span>One word at a time. Let it sink in.</span>
        </div>
      </div>
    </div>
  );
}
