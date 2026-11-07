"use client";

import React, { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import {
  ArrowRight,
  CheckCircle2,
  RotateCcw,
  Sprout,
  Flame,
  Award,
  Sparkles,
  Loader2,
} from "lucide-react";
import confetti from "canvas-confetti";
import { FillBlankCard, type FillBlankQuestionData } from "./FillBlankCard";
import type { PracticeQuestionDto } from "../../types";
import {
  fetchFillBlank,
  recordPracticedWords,
  type FillBlankItemDto,
} from "../../services/vocabularyPracticeService";

const FALLBACK_CONTEXT_QUESTIONS: FillBlankQuestionData[] = [
  {
    id: "fb-mitigate",
    word: "mitigate",
    baseWord: "mitigate",
    targetForm: "mitigate",
    acceptableVariants: ["mitigate", "mitigating"],
    topic: "Environment",
    partOfSpeech: "verb (base)",
    cefrLevel: "C1",
    definitionEn: "To make something less severe, harmful, or painful.",
    definitionVn: "Làm giảm nhẹ, làm dịu bớt mức độ nghiêm trọng hay tác hại",
    sentenceWithBlank:
      "Governments must implement proactive policies to ______ the adverse impacts of rising sea levels.",
    completedSentence:
      "Governments must implement proactive policies to mitigate the adverse impacts of rising sea levels.",
    sentenceVn:
      "Các chính phủ phải thực hiện các chính sách chủ động để giảm thiểu những tác động bất lợi của mực nước biển dâng cao.",
    grammarContextTip:
      "Follows infinitive marker 'to' -> requires base form verb (infinitive).",
    explanation:
      "'mitigate' is a formal transitive verb meaning to alleviate or reduce the severity of something.",
    source: "DATABASE",
  },
  {
    id: "fb-issues",
    word: "issue",
    baseWord: "issue",
    targetForm: "issues",
    acceptableVariants: ["issues", "issue"],
    topic: "Business",
    partOfSpeech: "noun (plural)",
    cefrLevel: "B1",
    definitionEn: "An important topic or problem for debate or discussion.",
    definitionVn: "Vấn đề, khía cạnh quan trọng cần thảo luận hoặc giải quyết",
    sentenceWithBlank: "The executive board addressed several financial ______ this quarter.",
    completedSentence:
      "The executive board addressed several financial issues this quarter.",
    sentenceVn:
      "Hội đồng quản trị đã giải quyết một số vấn đề tài chính trong quý này.",
    grammarContextTip:
      "Requires a plural countable noun (-s) after the quantifier 'several financial'.",
    explanation:
      "Here, 'issues' refers to complex problems or matters for debate. 'Several' requires plural count inflection.",
    source: "DATABASE",
  },
  {
    id: "fb-negotiate",
    word: "negotiate",
    baseWord: "negotiate",
    targetForm: "negotiate",
    acceptableVariants: ["negotiate", "negotiating"],
    topic: "Business",
    partOfSpeech: "verb (infinitive)",
    cefrLevel: "B2",
    definitionEn: "To have formal discussions with someone in order to reach an agreement.",
    definitionVn: "Đàm phán, thương lượng các điều khoản hợp đồng",
    sentenceWithBlank:
      "The procurement committee agreed to ______ better contract terms with the supplier.",
    completedSentence:
      "The procurement committee agreed to negotiate better contract terms with the supplier.",
    sentenceVn:
      "Ủy ban mua sắm đã đồng ý đàm phán các điều khoản hợp đồng tốt hơn với nhà cung cấp.",
    grammarContextTip:
      "Infinitive verb needed following the verb pattern 'agreed to + [base verb]'.",
    explanation:
      "Following 'agreed to', the bare verb 'negotiate' forms the infinitive 'to negotiate'.",
    source: "DATABASE",
  },
  {
    id: "fb-deadline",
    word: "deadline",
    baseWord: "deadline",
    targetForm: "deadline",
    acceptableVariants: ["deadline", "deadlines"],
    topic: "Business",
    partOfSpeech: "noun (singular)",
    cefrLevel: "B1",
    definitionEn: "A specific time or day by which something must be finished.",
    definitionVn: "Hạn chót, thời hạn cuối cùng phải hoàn thành công việc",
    sentenceWithBlank:
      "All project milestones must be submitted before Friday's strict ______.",
    completedSentence:
      "All project milestones must be submitted before Friday's strict deadline.",
    sentenceVn:
      "Tất cả các mốc của dự án phải được nộp trước hạn chót nghiêm ngặt vào thứ Sáu.",
    grammarContextTip:
      "Singular noun specifying a cutoff time following possessive modifier 'Friday's strict'.",
    explanation:
      "A 'deadline' is the final date or hour by which tasks must be completed.",
    source: "DATABASE",
  },
  {
    id: "fb-destinations",
    word: "destination",
    baseWord: "destination",
    targetForm: "destinations",
    acceptableVariants: ["destinations", "destination"],
    topic: "Travel & Adventures",
    partOfSpeech: "noun (plural)",
    cefrLevel: "B1",
    definitionEn: "The place where someone is going or where something is being sent or taken.",
    definitionVn: "Điểm đến, nơi đích của một chuyến hành trình",
    sentenceWithBlank:
      "Kyoto and Da Nang are among the most popular holiday ______ for international travelers.",
    completedSentence:
      "Kyoto and Da Nang are among the most popular holiday destinations for international travelers.",
    sentenceVn:
      "Kyoto và Đà Nẵng nằm trong số những điểm đến nghỉ dưỡng phổ biến nhất cho du khách quốc tế.",
    grammarContextTip:
      "Plural countable noun (-s) needed following the construction 'among the most popular holiday...'",
    explanation:
      "Multiple geographic locations ('Kyoto and Da Nang') necessitate the plural noun form 'destinations'.",
    source: "DATABASE",
  },
  {
    id: "fb-itinerary",
    word: "itinerary",
    baseWord: "itinerary",
    targetForm: "itinerary",
    acceptableVariants: ["itinerary"],
    topic: "Travel & Adventures",
    partOfSpeech: "noun",
    cefrLevel: "B1",
    definitionEn: "A detailed plan or schedule of a journey, including the route and places to visit.",
    definitionVn: "Lịch trình chi tiết của một chuyến đi hay hành trình",
    sentenceWithBlank:
      "Please review the detailed flight and hotel ______ prior to departure.",
    completedSentence:
      "Please review the detailed flight and hotel itinerary prior to departure.",
    sentenceVn:
      "Vui lòng xem lại hành trình chi tiết chuyến bay và khách sạn trước khi khởi hành.",
    grammarContextTip:
      "Singular noun referring to the comprehensive travel route and timetable.",
    explanation:
      "An 'itinerary' is a planned route or schedule of a journey.",
    source: "DATABASE",
  },
];

function mapDtoToQuestionData(dto: FillBlankItemDto, defaultTopic: string): FillBlankQuestionData {
  return {
    id: dto.id,
    word: dto.word,
    baseWord: dto.word,
    targetForm: dto.targetForm || dto.word,
    acceptableVariants: dto.acceptableVariants || [dto.targetForm, dto.word],
    topic: dto.topic || defaultTopic || "Vocabulary",
    partOfSpeech: dto.partOfSpeech || "context word",
    cefrLevel: dto.cefrLevel,
    definitionEn: dto.definitionEn,
    definitionVn: dto.definitionVn,
    sentenceWithBlank: dto.sentenceWithBlank,
    completedSentence: dto.completedSentence,
    sentenceVn: dto.sentenceVn,
    grammarContextTip: dto.grammarContextTip,
    explanation: dto.explanation,
    source: dto.source || "DATABASE",
    audioUrl: dto.audioUrl,
  };
}

function mapPracticeQuestion(
  q: PracticeQuestionDto,
  idx: number,
  topic: string
): FillBlankQuestionData {
  const target = q.correctAnswer || "word";
  return {
    id: q.id || `fb-mapped-${idx}`,
    word: target,
    baseWord: target,
    targetForm: target,
    acceptableVariants: [target.toLowerCase(), target.trim()],
    topic: topic || "Vocabulary",
    partOfSpeech: "target word",
    definitionEn: q.definition,
    definitionVn: q.explanation,
    sentenceWithBlank: (q.sentenceWithBlank || q.prompt || "").includes("___")
      ? (q.sentenceWithBlank || q.prompt)
      : `Complete the sentence: ______ (${q.wordHint || "context word"}).`,
    completedSentence: q.fullSentence || q.exampleSentence || q.explanation || `${q.prompt} (${target})`,
    sentenceVn: q.explanation,
    grammarContextTip:
      q.wordHint || "Determine the appropriate word form in this sentence structure.",
    explanation:
      q.explanation || `"${target}" appropriately completes the sentence context.`,
    source: "DATABASE",
    audioUrl: q.audioUrl,
  };
}

interface Props {
  topic?: string;
  questions?: PracticeQuestionDto[];
  onComplete?: () => void;
}

export function FillBlankSession({ topic = "Business", questions, onComplete }: Props) {
  const [sessionQuestions, setSessionQuestions] = useState<FillBlankQuestionData[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [checkedMap, setCheckedMap] = useState<
    Record<string, { isChecked: boolean; isCorrect: boolean; userAnswer: string }>
  >({});
  const [streak, setStreak] = useState(0);
  const [maxStreak, setMaxStreak] = useState(0);
  const [isCompleted, setIsCompleted] = useState(false);
  const [sessionRound, setSessionRound] = useState(0);
  const questionsRef = React.useRef(questions);
  questionsRef.current = questions;

  // Load questions dynamically from backend hybrid AI endpoint
  useEffect(() => {
    let isMounted = true;

    async function loadQuestions() {
      setLoading(true);
      try {
        const fetched = await fetchFillBlank(topic, 10);
        if (!isMounted) return;

        const propQuestions = questionsRef.current;
        if (fetched && fetched.length > 0) {
          setSessionQuestions(fetched.map((item) => mapDtoToQuestionData(item, topic)));
        } else if (sessionRound === 0 && propQuestions && propQuestions.length > 0) {
          const mapped = propQuestions
            .filter((q) => q.questionType === "FILL_IN_BLANKS" || q.correctAnswer)
            .map((q, i) => mapPracticeQuestion(q, i, topic));
          setSessionQuestions(mapped.length > 0 ? mapped : FALLBACK_CONTEXT_QUESTIONS);
        } else {
          const topicLower = topic.toLowerCase();
          const filtered = FALLBACK_CONTEXT_QUESTIONS.filter(
            (q) =>
              q.topic.toLowerCase().includes(topicLower) ||
              topicLower.includes(q.topic.toLowerCase())
          );
          setSessionQuestions(
            filtered.length > 0 ? filtered : FALLBACK_CONTEXT_QUESTIONS
          );
        }
      } catch (err) {
        console.warn("[FillBlankSession] Failed to fetch questions, using fallback:", err);
        if (isMounted) {
          setSessionQuestions(FALLBACK_CONTEXT_QUESTIONS);
        }
      } finally {
        if (isMounted) {
          setLoading(false);
          setCurrentIndex(0);
          setCheckedMap({});
          setStreak(0);
          setMaxStreak(0);
          setIsCompleted(false);
        }
      }
    }

    loadQuestions();

    return () => {
      isMounted = false;
    };
  }, [topic, sessionRound]);

  const total = sessionQuestions.length;
  const currentQ = sessionQuestions[currentIndex] || sessionQuestions[0];
  const currentState = currentQ ? checkedMap[currentQ.id] || null : null;
  const isChecked = currentState !== null && currentState.isChecked;

  // Answer Evaluation
  const handleCheckAnswer = useCallback(
    (answer: string) => {
      if (!currentQ) return;

      const cleanAnswer = answer.trim().toLowerCase().replace(/[.,!?;:]+$/, "");
      const target = currentQ.targetForm.trim().toLowerCase().replace(/[.,!?;:]+$/, "");
      const acceptable = (currentQ.acceptableVariants || [target]).map((v) =>
        v.trim().toLowerCase().replace(/[.,!?;:]+$/, "")
      );

      const isCorrect = cleanAnswer === target || acceptable.includes(cleanAnswer);

      if (isCorrect) {
        setStreak((prev) => {
          const next = prev + 1;
          setMaxStreak((m) => Math.max(m, next));
          return next;
        });
      } else {
        setStreak(0);
      }

      setCheckedMap((prev) => ({
        ...prev,
        [currentQ.id]: {
          isChecked: true,
          isCorrect,
          userAnswer: answer.trim(),
        },
      }));

      void recordPracticedWords(topic, [
        {
          word: currentQ.word || "",
          isCorrect,
        },
      ]);
    },
    [currentQ, topic]
  );

  // Advance to next question or complete session
  const handleNext = useCallback(() => {
    if (!isChecked) return;

    if (currentIndex + 1 < total) {
      setCurrentIndex((prev) => prev + 1);
    } else {
      setIsCompleted(true);

      void recordPracticedWords(
        topic,
        sessionQuestions.map((q) => ({
          word: q.word || "",
          isCorrect: checkedMap[q.id]?.isCorrect ?? true,
        }))
      );

      // Persist progress to localStorage
      if (typeof window !== "undefined" && topic) {
        try {
          const key = `parrotalk_mode_progress_${topic.toLowerCase().replace(/\s+/g, "_")}`;
          const stored = localStorage.getItem(key);
          const map = stored ? JSON.parse(stored) : {};

          const correctCount = Object.values(checkedMap).filter((s) => s.isCorrect).length;
          const accuracy = total > 0 ? Math.round((correctCount / total) * 100) : 100;

          const progressPayload = {
            completed: total,
            total,
            status: "completed",
            score: correctCount,
            accuracy,
            maxStreak,
            lastPracticed: new Date().toISOString(),
          };

          map["fill-blank"] = progressPayload;
          map["cloze"] = progressPayload; // Backward compatibility with topic detail badges

          localStorage.setItem(key, JSON.stringify(map));
        } catch (err) {
          console.error("Failed to save fill-blank progress to localStorage:", err);
        }
      }

      onComplete?.();
    }
  }, [currentIndex, isChecked, total, topic, sessionQuestions, checkedMap, maxStreak, onComplete]);

  // Restart session with a new set of unlearned words
  const handleRestart = useCallback(() => {
    setCurrentIndex(0);
    setCheckedMap({});
    setStreak(0);
    setMaxStreak(0);
    setIsCompleted(false);
    setSessionRound((r) => r + 1);
  }, []);

  // Trigger celebration confetti upon session completion
  useEffect(() => {
    if (isCompleted && total > 0) {
      const correctCount = Object.values(checkedMap).filter((s) => s.isCorrect).length;
      const accuracy = (correctCount / total) * 100;

      if (accuracy >= 50) {
        confetti({
          particleCount: 120,
          spread: 70,
          origin: { y: 0.6 },
          colors: ["#005721", "#257037", "#60B515", "#F3921F", "#ffffff"],
        });
      }
    }
  }, [isCompleted, checkedMap, total]);

  // Keyboard shortcut: Advance to next question on Enter when checked
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Enter" && isChecked && !isCompleted) {
        // Prevent interfering with active textarea/input if any
        const activeTag = document.activeElement?.tagName.toLowerCase();
        if (activeTag === "input") {
          // Input Enter is already captured by form submit
          return;
        }
        e.preventDefault();
        handleNext();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isChecked, isCompleted, handleNext]);

  // ── Loading Skeleton State ──
  if (loading) {
    return (
      <div className="max-w-2xl mx-auto space-y-6 animate-pulse">
        <div className="space-y-2">
          <div className="h-4 w-32 bg-emerald-100 rounded-md" />
          <div className="h-8 w-64 bg-ink-200 rounded-xl" />
          <div className="h-4 w-48 bg-ink-100 rounded-md" />
        </div>
        <div className="h-2 w-full bg-emerald-100 rounded-full" />
        <div className="rounded-3xl border border-ink-100 bg-white p-8 space-y-6 shadow-sm">
          <div className="flex items-center justify-between">
            <div className="h-4 w-40 bg-ink-100 rounded-md" />
            <div className="h-6 w-20 bg-emerald-50 rounded-lg" />
          </div>
          <div className="h-24 bg-emerald-50/50 rounded-2xl flex items-center justify-center">
            <div className="flex items-center gap-2 text-emerald-800 text-xs font-bold">
              <Loader2 className="w-4 h-4 animate-spin text-emerald-600" />
              <span>Generating contextual sentences with Hybrid AI...</span>
            </div>
          </div>
          <div className="h-12 bg-ink-100 rounded-2xl" />
        </div>
      </div>
    );
  }

  // ── Empty State ──
  if (total === 0) {
    return (
      <div className="max-w-xl mx-auto py-12 text-center space-y-4">
        <p className="text-sm text-ink-600 font-medium">
          No sentence practice questions available for {topic}.
        </p>
        <Link
          href={`/vocabulary/topics/${encodeURIComponent(topic)}`}
          className="inline-flex items-center justify-center gap-2 rounded-2xl bg-emerald-700 hover:bg-emerald-800 px-6 py-3 text-xs font-bold text-white shadow-sm transition-all min-h-[44px]"
        >
          <span>Back to {topic}</span>
        </Link>
      </div>
    );
  }

  // ── Session Completion Summary Screen ──
  if (isCompleted) {
    const correctCount = Object.values(checkedMap).filter((s) => s.isCorrect).length;
    const accuracy = Math.round((correctCount / total) * 100);

    return (
      <div className="max-w-xl mx-auto py-8 text-center space-y-6 animate-fade-up">
        {/* Celebration Trophy Icon */}
        <div className="w-20 h-20 rounded-3xl bg-emerald-50 border-2 border-emerald-300 text-emerald-700 mx-auto flex items-center justify-center shadow-sm">
          {accuracy >= 80 ? (
            <Award className="w-10 h-10 stroke-[2.2] text-emerald-700" />
          ) : (
            <CheckCircle2 className="w-10 h-10 stroke-[2.2] text-emerald-700" />
          )}
        </div>

        {/* Heading & Subtitle */}
        <div className="space-y-2">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl bg-emerald-100/80 text-emerald-900 text-xs font-bold">
            <Sparkles className="w-3.5 h-3.5 text-emerald-700" />
            <span>Session Complete</span>
          </div>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-ink-900 tracking-tight">
            Contextual Practice Completed!
          </h2>
          <p className="text-sm text-ink-600 max-w-md mx-auto">
            You practiced active recall in {total} authentic sentences in{" "}
            <span className="font-bold text-ink-900">{topic}</span>.
          </p>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-3 gap-3 pt-2">
          <div className="rounded-2xl bg-white border border-ink-200/90 p-4 text-center shadow-2xs">
            <span className="text-[11px] font-bold uppercase tracking-wider text-ink-500 block">
              Accuracy
            </span>
            <span
              className={`text-2xl sm:text-3xl font-black mt-1 block ${
                accuracy >= 80
                  ? "text-emerald-700"
                  : accuracy >= 60
                  ? "text-amber-600"
                  : "text-ink-800"
              }`}
            >
              {accuracy}%
            </span>
          </div>

          <div className="rounded-2xl bg-white border border-ink-200/90 p-4 text-center shadow-2xs">
            <span className="text-[11px] font-bold uppercase tracking-wider text-ink-500 block">
              Score
            </span>
            <span className="text-2xl sm:text-3xl font-black text-ink-900 mt-1 block">
              {correctCount} / {total}
            </span>
          </div>

          <div className="rounded-2xl bg-white border border-ink-200/90 p-4 text-center shadow-2xs">
            <span className="text-[11px] font-bold uppercase tracking-wider text-ink-500 block">
              Best Streak
            </span>
            <span className="text-2xl sm:text-3xl font-black text-amber-600 mt-1 flex items-center justify-center gap-1">
              <Flame className="w-6 h-6 fill-amber-500 text-amber-500" />
              <span>{maxStreak}</span>
            </span>
          </div>
        </div>

        {/* AI Hybrid Engine Note */}
        <div className="rounded-2xl border border-emerald-100 bg-emerald-50/60 p-3.5 text-xs text-emerald-900 font-medium">
          Sentences generated and curated via ParroTalk Hybrid AI for grammatical fidelity and active recall.
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-4">
          <button
            type="button"
            onClick={handleRestart}
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-2xl border border-ink-200 bg-white hover:bg-emerald-50 hover:border-emerald-300 px-6 py-3.5 text-xs font-bold text-ink-800 shadow-2xs transition-all min-h-[44px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
          >
            <RotateCcw className="w-4 h-4 text-emerald-700" />
            <span>Practice Again</span>
          </button>

          <Link
            href={`/vocabulary/topics/${encodeURIComponent(topic)}`}
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-2xl bg-emerald-700 hover:bg-emerald-800 px-7 py-3.5 text-xs font-bold text-white shadow-sm transition-all min-h-[44px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 active:scale-[0.98]"
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
          <div className="text-[11px] font-bold tracking-widest uppercase text-emerald-800">
            FOCUSED PRACTICE SESSION
          </div>
          <div className="flex items-center gap-2">
            {streak > 1 && (
              <span className="inline-flex items-center gap-1 text-xs font-extrabold text-amber-700 bg-amber-50 border border-amber-200 px-2.5 py-1 rounded-xl animate-bounce">
                <Flame className="w-3.5 h-3.5 fill-amber-500 text-amber-500" />
                <span>{streak} Streak</span>
              </span>
            )}
            <span className="text-xs font-semibold text-emerald-900 bg-emerald-50 border border-emerald-200 px-2.5 py-1 rounded-xl">
              Contextual Usage
            </span>
          </div>
        </div>

        <h1 className="text-3xl font-extrabold tracking-tight text-ink-900">
          Fill in the Blank
        </h1>
        <p className="text-xs text-ink-500">
          {topic} · Read the sentence and type the appropriate word or inflection.
        </p>
      </div>

      {/* ── Progress Indicators ── */}
      <div className="space-y-2 pt-1">
        <div className="flex items-center justify-between text-xs font-semibold">
          <span className="text-emerald-800 font-bold">
            Question {currentIndex + 1} / {total}
          </span>
          <span className="text-ink-500">
            {finishedCount} of {total} answered
          </span>
        </div>

        {/* Segmented Progress Bars */}
        <div
          className="grid gap-1.5"
          style={{ gridTemplateColumns: `repeat(${total}, minmax(0, 1fr))` }}
        >
          {sessionQuestions.map((q, i) => {
            const state = checkedMap[q.id];
            const isFinished = state !== undefined && state.isChecked;
            const isCurrent = i === currentIndex;

            let barColor = "bg-emerald-100";
            if (isFinished) {
              barColor = state.isCorrect ? "bg-emerald-600" : "bg-rose-400";
            } else if (isCurrent) {
              barColor = "bg-emerald-400";
            }

            return (
              <div
                key={q.id}
                className={`h-2 rounded-full transition-all duration-300 ${barColor}`}
                aria-label={`Question ${i + 1} ${
                  isFinished ? (state.isCorrect ? "Correct" : "Incorrect") : ""
                }`}
              />
            );
          })}
        </div>
      </div>

      {/* ── Main Question Card ── */}
      <FillBlankCard
        key={currentQ.id}
        questionData={currentQ}
        checkedState={currentState}
        onCheckAnswer={handleCheckAnswer}
        onNext={handleNext}
        isLastQuestion={currentIndex === total - 1}
      />

      {/* ── Bottom Action Row ── */}
      <div className="flex items-center justify-between gap-4 pt-2">
        <span className="text-xs text-ink-500 font-medium">
          {isChecked
            ? "Press Enter or click Continue to proceed."
            : "Type your answer and press Check Answer."}
        </span>

        <button
          type="button"
          onClick={handleNext}
          disabled={!isChecked}
          className="inline-flex items-center gap-2 rounded-2xl bg-emerald-700 hover:bg-emerald-800 disabled:bg-[#e4ebf0] disabled:text-ink-400 disabled:cursor-not-allowed px-6 py-3.5 text-xs font-bold text-white shadow-sm transition-all min-h-[44px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 active:scale-[0.98]"
        >
          <span>{currentIndex === total - 1 ? "Complete session" : "Next question"}</span>
          <ArrowRight className="w-4 h-4" />
        </button>
      </div>

      {/* ── Bottom Microcopy ── */}
      <div className="text-center space-y-1 pt-6 text-[11px] text-ink-500">
        <p>Grasping authentic syntactic patterns accelerates automatic language production.</p>
        <div className="inline-flex items-center gap-1.5 text-emerald-800 font-semibold">
          <Sprout className="w-3.5 h-3.5" />
          <span>One sentence at a time. Natural language intuition.</span>
        </div>
      </div>
    </div>
  );
}
