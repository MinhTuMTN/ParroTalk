"use client";

import React, { useState, useEffect, useCallback, useMemo } from "react";
import Link from "next/link";
import {
  ArrowRight,
  CheckCircle2,
  RotateCcw,
  Sprout,
  Flame,
  Loader2,
  Lightbulb,
} from "lucide-react";
import {
  TypeWordCard,
  normalizeWord,
  type TypeWordQuestionData,
  type TypeWordCheckedState,
} from "./TypeWordCard";
import type { PracticeQuestionDto } from "../../types";
import {
  fetchTypeWord,
  recordPracticedWords,
  FALLBACK_TYPE_WORD,
  type TypeWordItemDto,
} from "../../services/vocabularyPracticeService";

interface Props {
  topic?: string;
  questions?: PracticeQuestionDto[];
  onComplete?: () => void;
}

function mapDtoToTypeWord(
  dto: TypeWordItemDto,
  topic: string,
  index: number
): TypeWordQuestionData {
  return {
    id: dto.id || `tw-${index}`,
    targetWord: dto.targetWord,
    topic: topic || "Business",
    partOfSpeech: dto.partOfSpeech || "word",
    definition: dto.definitionEn || dto.promptDefinition || `Define "${dto.targetWord}"`,
    definitionVn: dto.definitionVn,
    promptDefinition: dto.promptDefinition,
    synonymOrClue: dto.firstLetter ? `Starts with "${dto.firstLetter}"` : undefined,
    charCount: dto.characterCount || dto.targetWord.length,
    firstLetter: dto.firstLetter || dto.targetWord[0] || "",
    exampleWithMask: dto.exampleWithMask,
    exampleSentence: dto.exampleSentence,
    phonetic: dto.phonetic,
    audioUrl: dto.audioUrl,
  };
}

function mapQuestionToTypeWord(
  q: PracticeQuestionDto,
  topic: string,
  index: number
): TypeWordQuestionData {
  const word = q.correctAnswer || q.prompt;
  return {
    id: q.id || `tw-${index}`,
    targetWord: word,
    topic: topic || "Vocabulary",
    partOfSpeech: "word",
    definition: q.definition || q.explanation || `The vocabulary word meaning "${word}".`,
    definitionVn: q.explanation,
    promptDefinition: q.prompt,
    synonymOrClue: q.wordHint || q.synonymOrClue,
    charCount: q.charCount || word.length,
    firstLetter: q.firstLetter || word[0] || "",
    exampleWithMask: q.sentenceWithBlank,
    exampleSentence: q.exampleSentence || q.fullSentence,
    phonetic: q.wordHint,
    audioUrl: q.audioUrl,
  };
}

export function TypeWordSession({ topic = "Business", questions, onComplete }: Props) {
  const [typeQuestions, setTypeQuestions] = useState<TypeWordQuestionData[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [checkedMap, setCheckedMap] = useState<Record<string, TypeWordCheckedState>>({});
  const [isCompleted, setIsCompleted] = useState(false);
  const [streak, setStreak] = useState(0);
  const [maxStreak, setMaxStreak] = useState(0);
  const [sessionRound, setSessionRound] = useState(0);
  const questionsRef = React.useRef(questions);
  questionsRef.current = questions;

  // Asynchronously load questions from API or props / fallback
  useEffect(() => {
    let isMounted = true;
    async function loadData() {
      setLoading(true);
      try {
        const propQuestions = questionsRef.current;
        if (sessionRound === 0 && propQuestions && propQuestions.length > 0) {
          const filtered = propQuestions.filter((q) => q.correctAnswer);
          if (filtered.length > 0 && isMounted) {
            setTypeQuestions(filtered.map((q, idx) => mapQuestionToTypeWord(q, topic, idx)));
            setLoading(false);
            return;
          }
        }

        const fetched = await fetchTypeWord(topic, 10);
        if (isMounted) {
          if (fetched && fetched.length > 0) {
            setTypeQuestions(fetched.map((dto, idx) => mapDtoToTypeWord(dto, topic, idx)));
          } else {
            setTypeQuestions(FALLBACK_TYPE_WORD.map((dto, idx) => mapDtoToTypeWord(dto, topic, idx)));
          }
          setLoading(false);
        }
      } catch (err) {
        console.warn("[TypeWordSession] Load error, using fallback:", err);
        if (isMounted) {
          setTypeQuestions(FALLBACK_TYPE_WORD.map((dto, idx) => mapDtoToTypeWord(dto, topic, idx)));
          setLoading(false);
        }
      }
    }

    loadData();
    return () => {
      isMounted = false;
    };
  }, [topic, sessionRound]);

  const total = typeQuestions.length;
  const currentQ = typeQuestions[currentIndex] || typeQuestions[0];
  const currentState = currentQ ? checkedMap[currentQ.id] || null : null;
  const isChecked = Boolean(currentState?.isChecked);

  const persistCompletion = useCallback(
    (count: number) => {
      if (typeof window === "undefined" || !topic) return;
      try {
        const key = `parrotalk_mode_progress_${topic.toLowerCase().replace(/\s+/g, "_")}`;
        const stored = localStorage.getItem(key);
        const map = stored ? JSON.parse(stored) : {};

        map["type-word"] = {
          completed: count,
          total: count,
          status: "completed",
        };

        localStorage.setItem(key, JSON.stringify(map));
      } catch (err) {
        console.error("Failed to save type-word progress to localStorage", err);
      }
    },
    [topic]
  );

  const handleCheckAnswer = useCallback(
    (userAnswer: string, hintsUsed: number) => {
      if (!currentQ) return;

      const isCorrect = normalizeWord(userAnswer) === normalizeWord(currentQ.targetWord);

      setCheckedMap((prev) => ({
        ...prev,
        [currentQ.id]: {
          isChecked: true,
          isCorrect,
          userAnswer: userAnswer.trim(),
          hintsUsed,
        },
      }));

      void recordPracticedWords(topic, [
        {
          word: currentQ.targetWord,
          isCorrect,
        },
      ]);

      setStreak((prev) => {
        const next = isCorrect ? prev + 1 : 0;
        setMaxStreak((currentMax) => Math.max(currentMax, next));
        return next;
      });
    },
    [currentQ, topic]
  );

  const handleRetryCurrent = useCallback(() => {
    if (!currentQ) return;
    setCheckedMap((prev) => {
      const updated = { ...prev };
      delete updated[currentQ.id];
      return updated;
    });
  }, [currentQ]);

  const handleNext = useCallback(() => {
    if (!isChecked) return;

    if (currentIndex + 1 < total) {
      setCurrentIndex((prev) => prev + 1);
    } else {
      setIsCompleted(true);
      void recordPracticedWords(
        topic,
        typeQuestions.map((q) => ({
          word: q.targetWord,
          isCorrect: checkedMap[q.id]?.isCorrect ?? true,
        }))
      );
      persistCompletion(total);
      onComplete?.();
    }
  }, [isChecked, currentIndex, total, topic, typeQuestions, checkedMap, persistCompletion, onComplete]);

  const handleRestart = () => {
    setCurrentIndex(0);
    setCheckedMap({});
    setStreak(0);
    setIsCompleted(false);
    setSessionRound((r) => r + 1);
  };

  const correctCount = useMemo(
    () => Object.values(checkedMap).filter((s) => s.isCorrect).length,
    [checkedMap]
  );

  const totalHintsUsed = useMemo(
    () => Object.values(checkedMap).reduce((acc, s) => acc + (s.hintsUsed || 0), 0),
    [checkedMap]
  );

  if (loading) {
    return (
      <div className="max-w-2xl mx-auto py-16 px-4 text-center space-y-4">
        <div className="w-12 h-12 rounded-2xl bg-amber-50 border border-amber-200 text-amber-600 flex items-center justify-center mx-auto shadow-2xs">
          <Loader2 className="w-6 h-6 animate-spin" />
        </div>
        <p className="text-sm font-semibold text-ink-700">Loading Type the Word recall practice...</p>
        <p className="text-xs text-ink-400">Preparing vocabulary definitions for {topic}</p>
      </div>
    );
  }

  if (total === 0) {
    return (
      <div className="max-w-xl mx-auto py-12 text-center space-y-4">
        <p className="text-sm text-ink-500">No recall questions available for {topic}.</p>
        <Link
          href={`/vocabulary/topics/${encodeURIComponent(topic)}`}
          className="inline-flex items-center justify-center gap-2 rounded-2xl bg-amber-600 hover:bg-amber-700 px-6 py-3 text-xs font-bold text-white shadow-sm transition-all"
        >
          <span>Back to {topic}</span>
        </Link>
      </div>
    );
  }

  if (isCompleted) {
    const scorePct = total > 0 ? Math.round((correctCount / total) * 100) : 0;

    return (
      <div className="max-w-2xl mx-auto py-12 px-4 animate-fade-up">
        <div className="rounded-3xl border border-amber-200/80 bg-white p-8 text-center shadow-card space-y-6">
          <div className="w-16 h-16 rounded-full bg-amber-50 border border-amber-200 text-amber-600 mx-auto flex items-center justify-center shadow-sm">
            <CheckCircle2 className="w-8 h-8 stroke-[2.5]" />
          </div>

          <div className="space-y-2">
            <h2 className="text-2xl sm:text-3xl font-extrabold text-ink-900 tracking-tight">
              Word Recall Completed!
            </h2>
            <p className="text-sm text-ink-500 max-w-md mx-auto">
              You practiced active recall for all {total} words in {topic}. Retrieving words directly from definitions establishes strong neural pathways.
            </p>
          </div>

          {/* Stats summary */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 max-w-md mx-auto pt-2">
            <div className="rounded-2xl border border-amber-100 bg-amber-50/60 p-4 space-y-1">
              <div className="text-[10px] font-bold uppercase tracking-wider text-amber-700">
                RECALLED
              </div>
              <div className="text-2xl font-extrabold text-amber-900">
                {correctCount} / {total}
              </div>
              <div className="text-[11px] font-semibold text-amber-700">{scorePct}% accuracy</div>
            </div>

            <div className="rounded-2xl border border-orange-100 bg-orange-50/60 p-4 space-y-1">
              <div className="text-[10px] font-bold uppercase tracking-wider text-orange-700">
                MAX STREAK
              </div>
              <div className="text-2xl font-extrabold text-orange-900 flex items-center justify-center gap-1">
                <Flame className="w-5 h-5 text-orange-500" />
                <span>{maxStreak}</span>
              </div>
              <div className="text-[11px] font-semibold text-orange-700">consecutive</div>
            </div>

            <div className="col-span-2 sm:col-span-1 rounded-2xl border border-emerald-100 bg-emerald-50/60 p-4 space-y-1">
              <div className="text-[10px] font-bold uppercase tracking-wider text-emerald-700">
                HINTS USED
              </div>
              <div className="text-2xl font-extrabold text-emerald-900 flex items-center justify-center gap-1">
                <Lightbulb className="w-5 h-5 text-emerald-600" />
                <span>{totalHintsUsed}</span>
              </div>
              <div className="text-[11px] font-semibold text-emerald-700">across session</div>
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-4">
            <button
              type="button"
              onClick={handleRestart}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-2xl border border-ink-200 bg-white hover:bg-ink-50 px-5 py-3 text-xs font-bold text-ink-800 shadow-sm transition-all"
            >
              <RotateCcw className="w-4 h-4 text-amber-600" />
              <span>Practice Again</span>
            </button>

            <Link
              href={`/vocabulary/topics/${encodeURIComponent(topic)}`}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-2xl bg-amber-600 hover:bg-amber-700 px-6 py-3 text-xs font-bold text-white shadow-sm transition-all"
            >
              <span>Back to {topic}</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const finishedCount = Object.keys(checkedMap).length;

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* ── Top Header ── */}
      <div className="space-y-4">
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-1">
            <div className="text-[11px] font-bold tracking-widest uppercase text-amber-700">
              YOUR FOCUSED LEARNING SESSION
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-ink-900">
              Type the Word
            </h1>
            <p className="text-xs sm:text-sm text-ink-500">
              {topic} · Read the definition, remember the word, and type it.
            </p>
          </div>

          <div className="flex items-center gap-2">
            {streak > 1 && (
              <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 border border-amber-200 px-3 py-1 text-xs font-bold text-amber-800 shadow-2xs animate-fade-in">
                <Flame className="w-3.5 h-3.5 text-amber-500 animate-pulse" />
                <span>{streak} Streak</span>
              </span>
            )}
            <span className="text-xs font-semibold text-amber-800 bg-amber-50 border border-amber-200 px-2.5 py-1 rounded-xl">
              Word Recall
            </span>
          </div>
        </div>

        {/* ── Progress Indicators ── */}
        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs font-semibold">
            <span className="text-amber-800 font-bold">
              Question {currentIndex + 1} of {total}
            </span>
            <span className="text-ink-400 font-medium">
              {finishedCount} of {total} evaluated · {correctCount} correct
            </span>
          </div>

          <div className="flex gap-1.5 h-1.5 w-full">
            {typeQuestions.map((q, i) => {
              const isCurr = i === currentIndex;
              const isPast = i < currentIndex;
              const isAns = Boolean(checkedMap[q.id]);

              return (
                <div
                  key={q.id || i}
                  className={`h-full flex-1 rounded-full transition-all duration-300 ${
                    isCurr
                      ? "bg-amber-600 ring-2 ring-amber-200"
                      : isPast || isAns
                      ? "bg-amber-400"
                      : "bg-ink-200/80"
                  }`}
                />
              );
            })}
          </div>
        </div>
      </div>

      {/* ── Main Question Card ── */}
      {currentQ && (
        <TypeWordCard
          key={currentQ.id}
          questionData={currentQ}
          checkedState={currentState}
          onCheckAnswer={handleCheckAnswer}
          onAdvance={handleNext}
          onRetry={handleRetryCurrent}
        />
      )}

      {/* ── Bottom Action Row ── */}
      <div className="flex items-center justify-between gap-4 pt-2">
        <span className="text-xs text-ink-500 font-medium">
          {isChecked
            ? currentState?.isCorrect
              ? "Great recall! Press Enter or choose Next question."
              : "Review the spelling above, retry, or proceed."
            : "Type your answer and press Enter to check."}
        </span>

        <button
          type="button"
          onClick={handleNext}
          disabled={!isChecked}
          className="inline-flex items-center gap-2 rounded-2xl bg-amber-600 hover:bg-amber-700 disabled:bg-[#f0ede6] disabled:text-ink-400 disabled:cursor-not-allowed px-6 py-3 text-xs font-bold text-white shadow-sm transition-all"
        >
          <span>{currentIndex === total - 1 ? "Finish session" : "Next question"}</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* ── Bottom Microcopy ── */}
      <div className="text-center space-y-1 pt-6 text-[11px] text-ink-400">
        <p>Active vocabulary retrieval helps transfer words into long-term memory.</p>
        <div className="inline-flex items-center gap-1.5 text-amber-700 font-medium">
          <Sprout className="w-3.5 h-3.5" />
          <span>One word at a time. Let it sink in.</span>
        </div>
      </div>
    </div>
  );
}
