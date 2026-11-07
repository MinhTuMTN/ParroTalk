"use client";

import React, { useState, useEffect, useCallback, useMemo } from "react";
import Link from "next/link";
import {
  ArrowRight,
  CheckCircle2,
  RotateCcw,
  Sprout,
  Flame,
  Trophy,
  Loader2,
} from "lucide-react";
import {
  MultipleChoiceCard,
  type MultipleChoiceQuestionData,
  type McState,
} from "./MultipleChoiceCard";
import type { PracticeQuestionDto } from "../../types";
import {
  fetchMultipleChoice,
  recordPracticedWords,
  FALLBACK_MULTIPLE_CHOICE,
  type MultipleChoiceItemDto,
} from "../../services/vocabularyPracticeService";

interface Props {
  topic?: string;
  questions?: PracticeQuestionDto[];
  onComplete?: () => void;
}

function mapDtoToMcData(
  dto: MultipleChoiceItemDto,
  topic: string,
  index: number
): MultipleChoiceQuestionData {
  const keys: ("A" | "B" | "C" | "D")[] = ["A", "B", "C", "D"];
  return {
    id: dto.id || `mc-${index}`,
    word: dto.targetWord || "Vocabulary",
    topic: topic || "Business",
    question: dto.question || `What is the meaning of "${dto.targetWord}"?`,
    sentence: dto.sentence || `Understanding "${dto.targetWord}" in context.`,
    phonetic: dto.phonetic,
    partOfSpeech: dto.partOfSpeech,
    explanation: dto.explanation,
    audioUrl: dto.audioUrl,
    options: (dto.options || []).slice(0, 4).map((text, i) => ({
      key: keys[i] || "A",
      text,
      isCorrect:
        text.trim().toLowerCase() === dto.correctAnswer?.trim().toLowerCase() ||
        i === dto.correctIndex,
    })),
  };
}

function mapQuestionToMcData(
  q: PracticeQuestionDto,
  topic: string,
  index: number
): MultipleChoiceQuestionData {
  const keys: ("A" | "B" | "C" | "D")[] = ["A", "B", "C", "D"];
  return {
    id: q.id || `mc-${index}`,
    word: q.prompt || "Vocabulary",
    topic: topic || "Vocabulary",
    question: `What is the meaning of "${q.prompt}"?`,
    sentence: q.fullSentence || q.exampleSentence || `Understanding "${q.prompt}" in English conversation.`,
    phonetic: q.wordHint,
    explanation: q.explanation,
    audioUrl: q.audioUrl,
    options: (q.options || []).slice(0, 4).map((text, i) => ({
      key: keys[i] || "A",
      text,
      isCorrect: text.trim().toLowerCase() === q.correctAnswer?.trim().toLowerCase(),
    })),
  };
}

export function MultipleChoiceSession({
  topic = "Business",
  questions,
  onComplete,
}: Props) {
  const [mcQuestions, setMcQuestions] = useState<MultipleChoiceQuestionData[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [sessionState, setSessionState] = useState<McState>("UNANSWERED");
  const [answers, setAnswers] = useState<
    Record<string, { key: "A" | "B" | "C" | "D"; isCorrect: boolean }>
  >({});
  const [streak, setStreak] = useState(0);
  const [maxStreak, setMaxStreak] = useState(0);
  const [sessionRound, setSessionRound] = useState(0);
  const questionsRef = React.useRef(questions);
  questionsRef.current = questions;

  // Fetch questions from API or use prop / fallback
  useEffect(() => {
    let isMounted = true;
    async function loadData() {
      setLoading(true);
      try {
        const propQuestions = questionsRef.current;
        if (sessionRound === 0 && propQuestions && propQuestions.length > 0) {
          const filtered = propQuestions.filter(
            (q) => q.questionType === "MULTIPLE_CHOICE" && q.options && q.options.length >= 2
          );
          if (filtered.length > 0 && isMounted) {
            setMcQuestions(filtered.map((q, idx) => mapQuestionToMcData(q, topic, idx)));
            setLoading(false);
            return;
          }
        }

        const fetched = await fetchMultipleChoice(topic, 10);
        if (isMounted) {
          if (fetched && fetched.length > 0) {
            setMcQuestions(fetched.map((dto, idx) => mapDtoToMcData(dto, topic, idx)));
          } else {
            setMcQuestions(FALLBACK_MULTIPLE_CHOICE.map((dto, idx) => mapDtoToMcData(dto, topic, idx)));
          }
          setLoading(false);
        }
      } catch (err) {
        console.warn("[MultipleChoiceSession] Load error, using fallback:", err);
        if (isMounted) {
          setMcQuestions(FALLBACK_MULTIPLE_CHOICE.map((dto, idx) => mapDtoToMcData(dto, topic, idx)));
          setLoading(false);
        }
      }
    }

    loadData();
    return () => {
      isMounted = false;
    };
  }, [topic, sessionRound]);

  const total = mcQuestions.length;
  const currentQ = mcQuestions[currentIndex] || mcQuestions[0];
  const currentAnswer = currentQ ? answers[currentQ.id] : undefined;

  const persistCompletion = useCallback(
    (count: number) => {
      if (typeof window === "undefined" || !topic) return;
      try {
        const key = `parrotalk_mode_progress_${topic.toLowerCase().replace(/\s+/g, "_")}`;
        const stored = localStorage.getItem(key);
        const map = stored ? JSON.parse(stored) : {};

        map["multiple-choice"] = {
          completed: count,
          total: count,
          status: "completed",
        };

        localStorage.setItem(key, JSON.stringify(map));
      } catch (err) {
        console.error("Failed to save multiple-choice progress to localStorage", err);
      }
    },
    [topic]
  );

  const handleSelectOption = useCallback(
    (key: "A" | "B" | "C" | "D", isCorrect: boolean) => {
      if (!currentQ || sessionState !== "UNANSWERED") return;

      setAnswers((prev) => ({
        ...prev,
        [currentQ.id]: { key, isCorrect },
      }));

      void recordPracticedWords(topic, [
        {
          word: currentQ.word,
          isCorrect,
        },
      ]);

      setStreak((prev) => {
        const next = isCorrect ? prev + 1 : 0;
        setMaxStreak((currentMax) => Math.max(currentMax, next));
        return next;
      });

      setSessionState("ANSWERED");
    },
    [currentQ, sessionState, topic]
  );

  const handleAdvance = useCallback(() => {
    if (sessionState !== "ANSWERED") return;

    if (currentIndex < total - 1) {
      setSessionState("ADVANCING");
      setCurrentIndex((prev) => prev + 1);
      setSessionState("UNANSWERED");
    } else {
      setSessionState("COMPLETED");
      void recordPracticedWords(
        topic,
        mcQuestions.map((q) => ({
          word: q.word,
          isCorrect: answers[q.id]?.isCorrect ?? true,
        }))
      );
      persistCompletion(total);
      onComplete?.();
    }
  }, [sessionState, currentIndex, total, topic, mcQuestions, answers, persistCompletion, onComplete]);

  // Restart session with a fresh batch of unpracticed words
  const handleRestart = () => {
    setCurrentIndex(0);
    setAnswers({});
    setStreak(0);
    setSessionState("UNANSWERED");
    setSessionRound((r) => r + 1);
  };

  const correctCount = useMemo(
    () => Object.values(answers).filter((a) => a.isCorrect).length,
    [answers]
  );

  if (loading) {
    return (
      <div className="max-w-2xl mx-auto py-16 px-4 text-center space-y-4">
        <div className="w-12 h-12 rounded-2xl bg-brand-50 border border-brand-200 text-brand-600 flex items-center justify-center mx-auto shadow-2xs">
          <Loader2 className="w-6 h-6 animate-spin" />
        </div>
        <p className="text-sm font-semibold text-ink-700">Loading Multiple Choice questions...</p>
        <p className="text-xs text-ink-400">Preparing vocabulary questions for {topic}</p>
      </div>
    );
  }

  if (total === 0) {
    return (
      <div className="max-w-xl mx-auto py-12 text-center space-y-4">
        <p className="text-sm text-ink-500">No multiple choice questions available for {topic}.</p>
        <Link
          href={`/vocabulary/topics/${encodeURIComponent(topic)}`}
          className="inline-flex items-center justify-center gap-2 rounded-2xl bg-brand-600 hover:bg-brand-700 px-6 py-3 text-xs font-bold text-white shadow-brand transition-all"
        >
          <span>Back to {topic}</span>
        </Link>
      </div>
    );
  }

  // Completion summary screen
  if (sessionState === "COMPLETED") {
    const scorePct = total > 0 ? Math.round((correctCount / total) * 100) : 0;

    return (
      <div className="max-w-2xl mx-auto py-12 px-4 animate-fade-up">
        <div className="rounded-3xl border border-brand-200/80 bg-white p-8 text-center shadow-card space-y-6">
          <div className="w-16 h-16 rounded-full bg-brand-50 border border-brand-200 text-brand-600 flex items-center justify-center mx-auto shadow-sm">
            <CheckCircle2 className="w-8 h-8 text-brand-600" />
          </div>

          <div className="space-y-2">
            <h2 className="text-2xl sm:text-3xl font-extrabold text-ink-900 tracking-tight">
              Multiple Choice Completed!
            </h2>
            <p className="text-sm text-ink-500 max-w-md mx-auto">
              Great job practicing {topic} vocabulary. Consistent recall solidifies active word recognition.
            </p>
          </div>

          {/* Score badges */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 max-w-md mx-auto pt-2">
            <div className="rounded-2xl border border-brand-100 bg-brand-50/60 p-4 space-y-1">
              <div className="text-[10px] font-bold uppercase tracking-wider text-brand-700">
                SCORE
              </div>
              <div className="text-2xl font-extrabold text-brand-900">
                {correctCount} / {total}
              </div>
              <div className="text-[11px] font-semibold text-brand-700">{scorePct}% accuracy</div>
            </div>

            <div className="rounded-2xl border border-amber-100 bg-amber-50/60 p-4 space-y-1">
              <div className="text-[10px] font-bold uppercase tracking-wider text-amber-700">
                BEST STREAK
              </div>
              <div className="text-2xl font-extrabold text-amber-900 flex items-center justify-center gap-1">
                <Flame className="w-5 h-5 text-amber-500" />
                <span>{maxStreak}</span>
              </div>
              <div className="text-[11px] font-semibold text-amber-700">consecutive</div>
            </div>

            <div className="col-span-2 sm:col-span-1 rounded-2xl border border-emerald-100 bg-emerald-50/60 p-4 space-y-1">
              <div className="text-[10px] font-bold uppercase tracking-wider text-emerald-700">
                XP EARNED
              </div>
              <div className="text-2xl font-extrabold text-emerald-900 flex items-center justify-center gap-1">
                <Trophy className="w-5 h-5 text-emerald-600" />
                <span>+{correctCount * 15}</span>
              </div>
              <div className="text-[11px] font-semibold text-emerald-700">Vocabulary XP</div>
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-4">
            <button
              type="button"
              onClick={handleRestart}
              className="inline-flex items-center gap-2 rounded-xl border border-ink-200 bg-white hover:bg-ink-50 px-5 py-3 text-xs font-bold text-ink-700 transition-all shadow-sm w-full sm:w-auto justify-center"
            >
              <RotateCcw className="w-4 h-4 text-brand-600" />
              <span>Practice Again</span>
            </button>

            <Link
              href={`/vocabulary/topics/${encodeURIComponent(topic)}`}
              className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-b from-brand-500 to-brand-600 hover:from-brand-600 hover:to-brand-700 px-6 py-3 text-xs font-bold text-white shadow-brand transition-all w-full sm:w-auto justify-center"
            >
              <span>Back to Topic Detail</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const answeredCount = Object.keys(answers).length;

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* ── Top Header ── */}
      <div className="space-y-4">
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-1">
            <div className="text-[11px] font-bold uppercase tracking-[0.16em] text-brand-700">
              YOUR FOCUSED LEARNING SESSION
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-ink-900">
              Multiple Choice
            </h1>
            <p className="text-xs sm:text-sm text-ink-500">
              {topic} · Find the meaning that fits the context.
            </p>
          </div>

          <div className="flex items-center gap-2">
            {streak > 1 && (
              <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 border border-amber-200 px-3 py-1 text-xs font-bold text-amber-800 shadow-2xs animate-fade-in">
                <Flame className="w-3.5 h-3.5 text-amber-500 animate-pulse" />
                <span>{streak} Streak</span>
              </span>
            )}
            <span className="rounded-full bg-ink-100 border border-ink-200/80 px-3 py-1 text-[11px] font-semibold text-ink-600 shrink-0">
              {topic}
            </span>
          </div>
        </div>

        {/* Step indicator & Segmented Progress Bar */}
        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs font-semibold">
            <span className="text-brand-700 font-bold">
              Question {currentIndex + 1} of {total}
            </span>
            <span className="text-ink-400 font-medium">
              {answeredCount} of {total} answered · {correctCount} correct
            </span>
          </div>

          <div className="flex gap-1.5 h-1.5 w-full">
            {mcQuestions.map((q, i) => {
              const isCurr = i === currentIndex;
              const isPast = i < currentIndex;
              const isAnsweredQ = Boolean(answers[q.id]);

              return (
                <div
                  key={q.id || i}
                  className={`h-full flex-1 rounded-full transition-all duration-300 ${
                    isCurr
                      ? "bg-brand-600 ring-2 ring-brand-200"
                      : isPast || isAnsweredQ
                      ? "bg-brand-400"
                      : "bg-ink-200/80"
                  }`}
                />
              );
            })}
          </div>
        </div>
      </div>

      {/* ── Question Card ── */}
      {currentQ && (
        <MultipleChoiceCard
          key={currentQ.id}
          questionData={currentQ}
          state={sessionState}
          selectedOptionKey={currentAnswer ? currentAnswer.key : null}
          onSelectOption={handleSelectOption}
          onAdvance={handleAdvance}
        />
      )}

      {/* ── Bottom Controls ── */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-2">
        <span className="text-xs text-ink-500 text-center sm:text-left order-2 sm:order-1 font-medium">
          {sessionState === "ANSWERED"
            ? currentAnswer?.isCorrect
              ? "Well done! Press Enter or choose Next question."
              : "Review the explanation above, then press Enter to continue."
            : "Use keyboard 1–4 / A–D or click an option to choose."}
        </span>

        <button
          type="button"
          onClick={handleAdvance}
          disabled={sessionState !== "ANSWERED"}
          className="inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-b from-brand-500 to-brand-600 hover:from-brand-600 hover:to-brand-700 disabled:opacity-40 disabled:cursor-not-allowed px-6 py-2.5 text-xs font-bold text-white shadow-brand transition-all order-1 sm:order-2 w-full sm:w-auto justify-center"
        >
          <span>{currentIndex === total - 1 ? "Finish session" : "Next question"}</span>
          <ArrowRight className="w-4 h-4" />
        </button>
      </div>

      {/* Footer note */}
      <div className="flex items-center justify-center gap-1.5 pt-4 text-xs text-ink-400">
        <Sprout className="w-3.5 h-3.5 text-brand-600" />
        <span>One question at a time. Let it sink in.</span>
      </div>
    </div>
  );
}
