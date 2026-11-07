"use client";

import React, { useState, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useVocabularyPractice } from "../../../../features/vocabulary/hooks";
import { MultipleChoiceQuiz } from "../../../../features/vocabulary/components/MultipleChoiceQuiz";
import { FlashcardDeck } from "../../../../features/vocabulary/components/FlashcardDeck";
import { ClozeTestPractice } from "../../../../features/vocabulary/components/ClozeTestPractice";
import Link from "next/link";
import { ArrowLeft, X } from "lucide-react";
import {
  LearnWordsSession,
  MultipleChoiceSession,
  TypeWordSession,
  ListenTypeSession,
  FillBlankSession,
} from "../../../../features/vocabulary/components/practice";

function PracticeTopNav({ topic, mode }: { topic?: string; mode?: string }) {
  const backHref = topic
    ? `/vocabulary/topics/${encodeURIComponent(topic)}`
    : "/vocabulary/topics";

  const modeTitleMap: Record<string, string> = {
    flashcard: "Learn Words",
    "learn-words": "Learn Words",
    "multiple-choice": "Multiple Choice",
    "type-word": "Type the Word",
    "fill-blank": "Fill in the Blank",
    cloze: "Fill in the Blank",
    "listen-type": "Listen & Type",
  };

  const modeTitle = (mode && modeTitleMap[mode]) || "Practice";

  return (
    <div className="max-w-2xl lg:max-w-3xl mx-auto mb-6 flex items-center justify-between gap-3 animate-fade-down">
      <Link
        href={backHref}
        className="group inline-flex items-center gap-2 rounded-xl border border-ink-200/90 bg-white hover:bg-brand-50 hover:border-brand-300 hover:text-brand-700 px-3.5 py-2 text-xs font-bold text-ink-700 shadow-2xs transition-all active:scale-[0.98]"
      >
        <ArrowLeft className="w-4 h-4 transition-transform group-hover:-translate-x-1 text-ink-500 group-hover:text-brand-600" />
        <span>Back to {topic || "Topic"}</span>
      </Link>

      <div className="flex items-center gap-2 text-xs font-medium text-ink-400">
        <Link href="/vocabulary/topics" className="hover:text-brand-700 transition-colors">
          Topics
        </Link>
        <span>/</span>
        {topic ? (
          <Link
            href={backHref}
            className="hover:text-brand-700 transition-colors truncate max-w-[120px] font-semibold text-ink-600"
          >
            {topic}
          </Link>
        ) : (
          <span>Vocabulary</span>
        )}
        <span>/</span>
        <span className="text-brand-700 font-bold">{modeTitle}</span>
      </div>
    </div>
  );
}

function PracticeContent() {
  const searchParams = useSearchParams();
  const topic = searchParams.get("topic") || undefined;
  const mode = searchParams.get("mode") || undefined;

  const isDedicatedSelfFetchingMode =
    mode === "flashcard" ||
    mode === "learn-words" ||
    mode === "multiple-choice" ||
    mode === "type-word" ||
    mode === "fill-blank" ||
    mode === "cloze";

  const { questions, loading } = useVocabularyPractice(topic, !isDedicatedSelfFetchingMode);
  const [idx, setIdx] = useState(0);

  // 1. Dedicated "Learn Words" (Flashcard) session
  if (mode === "flashcard" || mode === "learn-words") {
    return (
      <div className="min-h-full bg-ink-50 py-6 px-4 sm:px-6">
        <PracticeTopNav topic={topic} mode={mode} />
        <LearnWordsSession topic={topic || "Business"} />
      </div>
    );
  }

  // 2. Dedicated "Multiple Choice" session
  if (mode === "multiple-choice") {
    return (
      <div className="min-h-full bg-ink-50 py-6 px-4 sm:px-6">
        <PracticeTopNav topic={topic} mode={mode} />
        <MultipleChoiceSession topic={topic || "Business"} />
      </div>
    );
  }

  // 3. Dedicated "Type the Word" session (Word Recall from Definition)
  if (mode === "type-word") {
    return (
      <div className="min-h-full bg-ink-50 py-6 px-4 sm:px-6">
        <PracticeTopNav topic={topic} mode={mode} />
        <TypeWordSession topic={topic || "Business"} />
      </div>
    );
  }

  // 4. Dedicated "Fill in the Blank" session (Contextual Usage & Sentence Form)
  if (mode === "fill-blank" || mode === "cloze") {
    return (
      <div className="min-h-full bg-ink-50 py-6 px-4 sm:px-6">
        <PracticeTopNav topic={topic} mode={mode} />
        <FillBlankSession topic={topic || "Business"} />
      </div>
    );
  }

  // 5. Dedicated "Listen & Type" session (Audio & 3 Clues Progression)
  if (mode === "listen-type") {
    return (
      <div className="min-h-full bg-ink-50 py-6 px-4 sm:px-6">
        <PracticeTopNav topic={topic} mode={mode} />
        <ListenTypeSession
          topic={topic || "Business"}
          questions={questions}
          initialDifficulty="easy"
          modeKey={mode}
        />
      </div>
    );
  }

  // Filter questions for generic practice
  const filteredQuestions = React.useMemo(() => {
    if (!questions || questions.length === 0) return [];
    if (mode === "fill-blank" || mode === "cloze") {
      const fb = questions.filter((q) => q.questionType === "FILL_IN_BLANKS");
      return fb.length > 0 ? fb : questions;
    }
    return questions;
  }, [questions, mode]);

  // Persist mode progress when completing questions
  const handleNext = () => {
    if (topic && mode && filteredQuestions.length > 0) {
      try {
        const key = `parrotalk_mode_progress_${topic.toLowerCase().replace(/\s+/g, "_")}`;
        const stored = localStorage.getItem(key);
        const map = stored ? JSON.parse(stored) : {};
        const completed = Math.min(idx + 1, filteredQuestions.length);
        const status = completed >= filteredQuestions.length ? "completed" : "in-progress";

        map[mode] = {
          completed,
          total: filteredQuestions.length,
          status,
        };

        localStorage.setItem(key, JSON.stringify(map));
      } catch (err) {
        console.error("Failed to save progress in session", err);
      }
    }

    setIdx((i) => (i + 1) % filteredQuestions.length);
  };

  if (loading) {
    return (
      <div className="p-8 text-center text-gray-500 dark:text-gray-400">
        Loading practice questions...
      </div>
    );
  }

  if (!filteredQuestions || filteredQuestions.length === 0) {
    return (
      <div className="p-8 text-center text-gray-500 dark:text-gray-400">
        No practice questions available for this mode.
      </div>
    );
  }

  const q = filteredQuestions[idx] || filteredQuestions[0];

  return (
    <div className="p-8 max-w-4xl mx-auto min-h-screen flex flex-col justify-center">
      <PracticeTopNav topic={topic} mode={mode} />
      <div className="mb-8 text-center">
        <h1 className="text-3xl font-bold dark:text-white">Vocabulary Practice</h1>
        {topic && <p className="text-sm text-brand-600 font-semibold mt-1">Topic: {topic}</p>}
      </div>

      <div className="flex-1">
        {q.questionType === "MULTIPLE_CHOICE" && (
          <MultipleChoiceQuiz question={q} onAnswer={handleNext} />
        )}
        {q.questionType === "FLASHCARD" && (
          <FlashcardDeck card={q} onRate={handleNext} />
        )}
        {q.questionType === "FILL_IN_BLANKS" && (
          <ClozeTestPractice question={q} onCorrect={handleNext} />
        )}
      </div>

      <div className="mt-8 text-center text-gray-500 font-medium text-sm">
        Question {idx + 1} of {filteredQuestions.length}
      </div>
    </div>
  );
}

export default function PracticePage() {
  return (
    <Suspense fallback={<div className="p-8 text-center text-gray-500">Loading practice...</div>}>
      <PracticeContent />
    </Suspense>
  );
}