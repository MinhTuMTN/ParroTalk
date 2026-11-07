"use client";

import React, { useState, useEffect, useCallback, useMemo } from "react";
import Link from "next/link";
import {
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  RotateCcw,
  Volume2,
  Shuffle,
  Sparkles,
} from "lucide-react";
import {
  LearnWordsCard,
  type LearnWordData,
  type RatingValue,
  type TranslationDirection,
} from "./LearnWordsCard";
import type { PracticeQuestionDto } from "../../types";
import {
  fetchLearnWords,
  recordPracticedWords,
  FALLBACK_LEARN_WORDS,
  type LearnWordItemDto,
} from "../../services/vocabularyPracticeService";

const INITIAL_SAMPLE_WORDS: LearnWordData[] = [
  {
    id: "word-compromise",
    word: "compromise",
    partOfSpeech: "noun / verb",
    cefrLevel: "B2",
    topic: "Business",
    phoneticUs: "/ˈkɑːmprəmaɪz/",
    phoneticUk: "/ˈkɒmprəmaɪz/",
    primaryEnglishDefinition:
      "An agreement in an argument, arrived at by people reducing their demands or changing their opinion in order to agree.",
    primaryVietnameseMeaning: "Sự thoả hiệp, sự nhượng bộ; dàn xếp đôi bên cùng có lợi",
    conversationExample: {
      english: "Both parties agreed to make a compromise to finalize the joint venture agreement.",
      vietnamese: "Cả hai bên đã đồng ý đưa ra sự thỏa hiệp để hoàn tất hợp đồng liên doanh.",
    },
    collocations: ["reach a compromise", "make a compromise", "compromise on price"],
    synonyms: ["agreement", "concession", "settlement"],
    antonyms: ["dispute", "intransigence"],
  },
  {
    id: "word-negotiate",
    word: "negotiate",
    partOfSpeech: "verb",
    cefrLevel: "B2",
    topic: "Business",
    phoneticUs: "/nəˈɡoʊ.ʃi.eɪt/",
    phoneticUk: "/nəˈɡəʊ.ʃi.eɪt/",
    primaryEnglishDefinition: "To have formal discussions with someone in order to reach an agreement.",
    primaryVietnameseMeaning: "Đàm phán, thương lượng các điều khoản hợp đồng",
    conversationExample: {
      english: "The government will negotiate a new contract with the international supplier.",
      vietnamese: "Chính phủ sẽ đàm phán một hợp đồng mới với nhà cung cấp quốc tế.",
    },
    collocations: ["negotiate a deal", "negotiate terms", "successfully negotiate"],
    synonyms: ["bargain", "confer", "discuss terms"],
    antonyms: [],
  },
  {
    id: "word-mitigate",
    word: "mitigate",
    partOfSpeech: "verb",
    cefrLevel: "C1",
    topic: "Environment",
    phoneticUs: "/ˈmɪt̬.ə.ɡeɪt/",
    phoneticUk: "/ˈmɪt.ɪ.ɡeɪt/",
    primaryEnglishDefinition: "To make something less severe, harmful, or painful.",
    primaryVietnameseMeaning: "Làm giảm nhẹ, làm dịu bớt mức độ nghiêm trọng hay tác hại",
    conversationExample: {
      english: "Governments must implement long-term policies to mitigate the adverse impact of climate change.",
      vietnamese: "Các chính phủ cần thực hiện các chính sách dài hạn để giảm thiểu tác động bất lợi của biến đổi khí hậu.",
    },
    collocations: ["mitigate risk", "mitigate the effects", "mitigate damages"],
    synonyms: ["alleviate", "lessen", "attenuate"],
    antonyms: ["exacerbate", "aggravate"],
  },
  {
    id: "word-destination",
    word: "destination",
    partOfSpeech: "noun",
    cefrLevel: "B1",
    topic: "Travel & Adventures",
    phoneticUs: "/ˌdes.təˈneɪ.ʃən/",
    phoneticUk: "/ˌdes.tɪˈneɪ.ʃən/",
    primaryEnglishDefinition: "The place where someone is going or where something is being sent or taken.",
    primaryVietnameseMeaning: "Điểm đến, nơi đích của chuyến hành trình",
    conversationExample: {
      english: "We arrived at our final vacation destination after a ten-hour overseas flight.",
      vietnamese: "Chúng tôi đã đến điểm đến nghỉ dưỡng cuối cùng sau chuyến bay mười tiếng.",
    },
    collocations: ["tourist destination", "final destination", "reach your destination"],
    synonyms: ["journey's end", "stop", "landing"],
    antonyms: ["departure point"],
  },
  {
    id: "word-itinerary",
    word: "itinerary",
    partOfSpeech: "noun",
    cefrLevel: "B1",
    topic: "Travel & Adventures",
    phoneticUs: "/aɪˈtɪn.ə.rer.i/",
    phoneticUk: "/aɪˈtɪn.ər.ər.i/",
    primaryEnglishDefinition: "A detailed plan or schedule of a journey, including the route and places to visit.",
    primaryVietnameseMeaning: "Lịch trình chi tiết của một chuyến đi hay hành trình",
    conversationExample: {
      english: "The tour company provided travelers with an organized daily itinerary.",
      vietnamese: "Công ty du lịch đã cung cấp cho du khách một lịch trình hàng ngày có tổ chức.",
    },
    collocations: ["planned itinerary", "travel itinerary", "flexible itinerary"],
    synonyms: ["route", "schedule", "journey plan"],
    antonyms: [],
  },
  {
    id: "word-deadline",
    word: "deadline",
    partOfSpeech: "noun",
    cefrLevel: "B1",
    topic: "Business",
    phoneticUs: "/ˈded.laɪn/",
    phoneticUk: "/ˈded.laɪn/",
    primaryEnglishDefinition: "A specific time or day by which something must be finished.",
    primaryVietnameseMeaning: "Hạn chót, thời hạn cuối cùng phải hoàn thành công việc",
    conversationExample: {
      english: "The engineering team worked overtime to meet the strict project deadline.",
      vietnamese: "Đội ngũ kỹ thuật đã làm thêm giờ để kịp hạn chót nghiêm ngặt của dự án.",
    },
    collocations: ["meet a deadline", "miss a deadline", "tight deadline"],
    synonyms: ["time limit", "due date", "cutoff"],
    antonyms: [],
  },
];

function mapDtoToLearnWord(dto: LearnWordItemDto): LearnWordData {
  return {
    id: dto.id,
    word: dto.word,
    partOfSpeech: dto.partOfSpeech || "noun",
    cefrLevel: dto.cefrLevel || "B2",
    topic: dto.topic || "Vocabulary",
    phonetic: dto.phonetic,
    phoneticUs: dto.phoneticUs || dto.phonetic || `/${dto.word}/`,
    phoneticUk: dto.phoneticUk || dto.phonetic || `/${dto.word}/`,
    audioUsUrl: dto.audioUrlUs || dto.audioUsUrl || dto.audioUrl,
    audioUkUrl: dto.audioUrlUk || dto.audioUkUrl || dto.audioUrl,
    primaryEnglishDefinition: dto.definitionEn || "",
    primaryVietnameseMeaning: dto.definitionVn || "",
    conversationExample: {
      english: dto.exampleEn || `We should study how ${dto.word} is used in professional contexts.`,
      vietnamese:
        dto.exampleVn || `Chúng ta nên học cách từ ${dto.word} được sử dụng trong ngữ cảnh chuyên nghiệp.`,
    },
    collocations: dto.collocations || [],
    synonyms: dto.synonyms || [],
    antonyms: dto.antonyms || [],
  };
}

function mapQuestionToLearnWord(q: PracticeQuestionDto): LearnWordData {
  return {
    id: q.id,
    word: q.correctAnswer || q.prompt,
    partOfSpeech: "word",
    cefrLevel: "B2",
    topic: "Practice",
    phonetic: q.wordHint || `/${q.correctAnswer}/`,
    phoneticUs: q.wordHint || `/${q.correctAnswer}/`,
    phoneticUk: q.wordHint || `/${q.correctAnswer}/`,
    audioUsUrl: q.audioUrl,
    audioUkUrl: q.audioUrl,
    primaryEnglishDefinition: q.definition || q.prompt,
    primaryVietnameseMeaning: q.explanation || q.definition || "Ý nghĩa từ vựng",
    conversationExample: {
      english: q.exampleSentence || q.fullSentence || `Learning ${q.correctAnswer} in daily conversations.`,
      vietnamese: q.explanation || "Học từ vựng trong hội thoại hàng ngày.",
    },
    collocations: q.clues || [],
    synonyms: q.synonymOrClue ? [q.synonymOrClue] : [],
  };
}

interface Props {
  topic?: string;
  questions?: PracticeQuestionDto[];
  onComplete?: () => void;
}

export function LearnWordsSession({ topic = "Business", questions, onComplete }: Props) {
  const [words, setWords] = useState<LearnWordData[]>(INITIAL_SAMPLE_WORDS);
  const [loading, setLoading] = useState(true);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);
  const [direction, setDirection] = useState<TranslationDirection>("EN_VI");
  const [ratings, setRatings] = useState<Record<string, RatingValue>>({});
  const [isFinished, setIsFinished] = useState(false);
  const [autoPlayAudio, setAutoPlayAudio] = useState(false);
  const [sessionRound, setSessionRound] = useState(0);
  const questionsRef = React.useRef(questions);
  questionsRef.current = questions;

  // Load words dynamically from API
  useEffect(() => {
    let isMounted = true;
    async function loadWords() {
      setLoading(true);
      try {
        const fetched = await fetchLearnWords(topic, 10);
        const propQuestions = questionsRef.current;
        if (isMounted) {
          if (fetched && fetched.length > 0) {
            setWords(fetched.map(mapDtoToLearnWord));
          } else if (propQuestions && propQuestions.length > 0) {
            setWords(propQuestions.map(mapQuestionToLearnWord));
          } else {
            setWords(FALLBACK_LEARN_WORDS.map(mapDtoToLearnWord));
          }
          setCurrentIndex(0);
          setIsFlipped(false);
        }
      } catch (err) {
        console.warn("Failed to load learn words, using fallback", err);
        if (isMounted) {
          setWords(FALLBACK_LEARN_WORDS.map(mapDtoToLearnWord));
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    loadWords();
    return () => {
      isMounted = false;
    };
  }, [topic, sessionRound]);

  // Reset flip state when moving cards or toggling direction
  useEffect(() => {
    setIsFlipped(false);
  }, [currentIndex, direction]);

  const currentWord = useMemo(() => {
    return words[currentIndex] || words[0] || INITIAL_SAMPLE_WORDS[0];
  }, [words, currentIndex]);

  const ratedCount = useMemo(() => {
    return Object.keys(ratings).length;
  }, [ratings]);

  // Auto-play audio when navigating to a new card
  useEffect(() => {
    if (autoPlayAudio && currentWord && !isFinished && !loading) {
      if (typeof window !== "undefined" && "speechSynthesis" in window) {
        window.speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(currentWord.word);
        utterance.lang = "en-US";
        window.speechSynthesis.speak(utterance);
      }
    }
  }, [currentIndex, autoPlayAudio, currentWord, isFinished, loading]);

  const handleRate = useCallback(
    (rating: RatingValue) => {
      if (!currentWord) return;
      setRatings((prev) => ({
        ...prev,
        [currentWord.id]: rating,
      }));
      void recordPracticedWords(topic, [
        {
          word: currentWord.word,
          rating,
          isCorrect: rating !== "again",
        },
      ]);
    },
    [currentWord, topic]
  );

  const finishSession = useCallback(() => {
    setIsFinished(true);

    void recordPracticedWords(
      topic,
      words.map((w) => ({
        word: w.word,
        rating: ratings[w.id] || "good",
        isCorrect: ratings[w.id] !== "again",
      }))
    );

    // Persist session progress to localStorage
    if (topic && typeof window !== "undefined") {
      try {
        const key = `parrotalk_mode_progress_${topic.toLowerCase().replace(/\s+/g, "_")}`;
        const stored = localStorage.getItem(key);
        const map = stored ? JSON.parse(stored) : {};

        map["flashcard"] = {
          completed: words.length,
          total: words.length,
          status: "completed",
        };
        map["learn-words"] = {
          completed: words.length,
          total: words.length,
          status: "completed",
        };

        localStorage.setItem(key, JSON.stringify(map));
      } catch (err) {
        console.error("Failed to persist session progress", err);
      }
    }

    if (onComplete) onComplete();
  }, [topic, words, ratings, onComplete]);

  const handleNext = useCallback(() => {
    if (currentWord) {
      void recordPracticedWords(topic, [
        {
          word: currentWord.word,
          rating: ratings[currentWord.id] || "good",
          isCorrect: ratings[currentWord.id] !== "again",
        },
      ]);
    }
    if (currentIndex < words.length - 1) {
      setCurrentIndex((prev) => prev + 1);
    } else {
      finishSession();
    }
  }, [currentIndex, words.length, currentWord, topic, ratings, finishSession]);

  const handlePrevious = useCallback(() => {
    if (currentIndex > 0) {
      setCurrentIndex((prev) => prev - 1);
    }
  }, [currentIndex]);

  const handleShuffle = useCallback(() => {
    setWords((prev) => {
      const copy = [...prev];
      for (let i = copy.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [copy[i], copy[j]] = [copy[j], copy[i]];
      }
      return copy;
    });
    setCurrentIndex(0);
    setIsFlipped(false);
  }, []);

  // Global Keyboard Navigation Listener
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Do not capture keys when user is typing in form controls
      const target = e.target as HTMLElement | null;
      if (
        target &&
        (target.tagName === "INPUT" ||
          target.tagName === "TEXTAREA" ||
          target.isContentEditable)
      ) {
        return;
      }

      // Space or Enter: Flip the card
      if (e.code === "Space" || e.code === "Enter") {
        e.preventDefault();
        setIsFlipped((prev) => !prev);
        return;
      }

      // ArrowRight: Next word or Finish
      if (e.code === "ArrowRight") {
        e.preventDefault();
        handleNext();
        return;
      }

      // ArrowLeft: Previous word
      if (e.code === "ArrowLeft") {
        e.preventDefault();
        handlePrevious();
        return;
      }

      // Keys 1-4: Select spaced repetition rating
      if (e.key === "1") {
        e.preventDefault();
        handleRate("again");
        return;
      }
      if (e.key === "2") {
        e.preventDefault();
        handleRate("hard");
        return;
      }
      if (e.key === "3") {
        e.preventDefault();
        handleRate("good");
        return;
      }
      if (e.key === "4") {
        e.preventDefault();
        handleRate("easy");
        return;
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [handleNext, handlePrevious, handleRate]);

  // Loading skeleton state
  if (loading) {
    return (
      <div className="max-w-3xl mx-auto py-12 px-4 space-y-6 animate-pulse">
        <div className="h-8 bg-[#e0e4dc] rounded-xl w-1/3" />
        <div className="h-4 bg-[#e0e4dc] rounded-lg w-1/2" />
        <div className="min-h-[460px] bg-white border border-[#e0e4dc] rounded-3xl p-10 flex flex-col justify-between shadow-xs">
          <div className="flex justify-between">
            <div className="h-6 bg-[#e0e4dc] rounded-full w-24" />
            <div className="h-6 bg-[#e0e4dc] rounded-full w-20" />
          </div>
          <div className="flex flex-col items-center gap-4 my-auto">
            <div className="h-10 bg-[#e0e4dc] rounded-xl w-64" />
            <div className="h-5 bg-[#e0e4dc] rounded-lg w-32" />
            <div className="h-10 bg-[#e0e4dc] rounded-xl w-48" />
          </div>
          <div className="h-6 bg-[#e0e4dc] rounded-lg w-full" />
        </div>
      </div>
    );
  }

  // Session Completed State
  if (isFinished) {
    return (
      <div className="max-w-2xl mx-auto py-12 px-4 animate-fade-up">
        <div className="rounded-3xl border border-[#005721]/20 bg-white p-8 sm:p-10 text-center shadow-[0_12px_40px_rgba(0,87,33,0.08)] space-y-6">
          <div className="w-16 h-16 rounded-full bg-[#005721]/10 border border-[#005721]/20 text-[#005721] flex items-center justify-center mx-auto shadow-sm">
            <CheckCircle2 className="w-8 h-8 text-[#005721]" />
          </div>

          <div className="space-y-2">
            <h2 className="text-3xl font-extrabold text-[#0F172A] tracking-tight">
              Session Completed!
            </h2>
            <p className="text-sm text-[#334155] max-w-md mx-auto leading-relaxed">
              You&apos;ve reviewed all {words.length} words in <strong className="text-[#005721]">{topic}</strong>.
              Consistent practice builds long-term fluency and confidence.
            </p>
          </div>

          {/* Ratings breakdown */}
          <div className="grid grid-cols-4 gap-3 max-w-sm mx-auto pt-2">
            {(["easy", "good", "hard", "again"] as RatingValue[]).map((r) => {
              const count = Object.values(ratings).filter((v) => v === r).length;
              const colorMap: Record<string, string> = {
                easy: "text-[#005721] bg-[#005721]/10 border-[#005721]/30",
                good: "text-[#257037] bg-[#257037]/10 border-[#257037]/30",
                hard: "text-[#8c4f00] bg-[#ffdcbf]/50 border-[#8c4f00]/30",
                again: "text-[#ba1a1a] bg-[#ffdad6]/50 border-[#ba1a1a]/30",
              };
              return (
                <div key={r} className={`p-3 rounded-2xl border ${colorMap[r]}`}>
                  <div className="text-xl font-extrabold">{count}</div>
                  <div className="text-[10px] uppercase font-bold tracking-wider mt-0.5">
                    {r}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Action buttons */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-4">
            <button
              type="button"
              aria-label="Learn next batch of words"
              onClick={() => {
                setCurrentIndex(0);
                setIsFlipped(false);
                setRatings({});
                setIsFinished(false);
                setSessionRound((r) => r + 1);
              }}
              className="min-h-[44px] inline-flex items-center gap-2 rounded-xl border border-[#E2E8F0] bg-white hover:bg-[#f8faf9] px-5 py-2.5 text-xs font-bold text-[#0F172A] transition-all shadow-xs w-full sm:w-auto justify-center"
            >
              <RotateCcw className="w-4 h-4 text-[#334155]" />
              <span>Learn Next Words</span>
            </button>

            <Link
              href={`/vocabulary/topics/${encodeURIComponent(topic)}`}
              className="min-h-[44px] inline-flex items-center gap-2 rounded-xl bg-[#005721] hover:bg-[#257037] px-6 py-2.5 text-xs font-bold text-white shadow-md hover:shadow-lg transition-all w-full sm:w-auto justify-center"
            >
              <span>Back to Topic Detail</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const progressPercent = Math.round(((currentIndex + 1) / words.length) * 100);

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* ── Top Header Ribbon & Direction Switcher ── */}
      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#005721]">
              FOCUSED PRACTICE SESSION
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[#0F172A]">
              Learn Words (Interactive Flashcards)
            </h1>
            <p className="text-xs sm:text-sm text-[#334155]">
              Topic: <strong className="text-[#005721]">{topic}</strong> · Master authentic vocabulary with 3D flashcards.
            </p>
          </div>

          {/* Bidirectional Translation Switcher (EN ↔ VI / VI ↔ EN) */}
          <div
            className="flex items-center bg-[#ebefe7] rounded-full p-1 border border-[#bfc9bc]/60 shadow-inner self-start sm:self-auto"
            role="group"
            aria-label="Translation direction toggle"
          >
            <button
              type="button"
              id="toggle-en-vi"
              aria-label="Study English to Vietnamese"
              aria-pressed={direction === "EN_VI"}
              onClick={() => setDirection("EN_VI")}
              className={`min-h-[36px] px-3.5 py-1 text-xs font-bold rounded-full transition-all duration-150 ${
                direction === "EN_VI"
                  ? "bg-[#005721] text-white shadow-xs font-bold"
                  : "text-[#334155] hover:text-[#0F172A]"
              }`}
            >
              EN → VI
            </button>
            <button
              type="button"
              id="toggle-vi-en"
              aria-label="Study Vietnamese to English (Reverse active recall)"
              aria-pressed={direction === "VI_EN"}
              onClick={() => setDirection("VI_EN")}
              className={`min-h-[36px] px-3.5 py-1 text-xs font-bold rounded-full transition-all duration-150 ${
                direction === "VI_EN"
                  ? "bg-[#005721] text-white shadow-xs font-bold"
                  : "text-[#334155] hover:text-[#0F172A]"
              }`}
            >
              VI → EN
            </button>
          </div>
        </div>

        {/* Top Utility Strip: Auto-play & Shuffle & Progress */}
        <div className="flex items-center justify-between gap-4 text-xs text-[#334155] pt-1">
          <div className="flex items-center gap-4">
            <label className="flex items-center gap-2 cursor-pointer group select-none">
              <input
                type="checkbox"
                checked={autoPlayAudio}
                onChange={(e) => setAutoPlayAudio(e.target.checked)}
                className="w-4 h-4 rounded border-[#bfc9bc] text-[#005721] focus:ring-[#005721] transition"
              />
              <span className="group-hover:text-[#0F172A] transition-colors flex items-center gap-1 font-medium">
                <Volume2 className="w-3.5 h-3.5 text-[#005721]" />
                Auto-play audio
              </span>
            </label>

            <button
              type="button"
              aria-label="Shuffle flashcards order"
              onClick={handleShuffle}
              className="inline-flex items-center gap-1 hover:text-[#005721] font-medium transition-colors"
            >
              <Shuffle className="w-3.5 h-3.5" />
              <span>Shuffle</span>
            </button>
          </div>

          <div className="flex items-center gap-2">
            <span className="font-semibold text-[#0F172A]">
              Word {currentIndex + 1} of {words.length}
            </span>
            <span className="text-[#64748B]">({ratedCount} rated)</span>
          </div>
        </div>

        {/* Progress bar */}
        <div className="space-y-1">
          <div className="w-full h-2 bg-[#ebefe7] rounded-full overflow-hidden">
            <div
              className="bg-[#60B515] h-full rounded-full transition-all duration-300"
              style={{ width: `${progressPercent}%` }}
              role="progressbar"
              aria-valuenow={progressPercent}
              aria-valuemin={0}
              aria-valuemax={100}
            />
          </div>
          <div className="flex gap-1.5 h-1 w-full pt-0.5">
            {words.map((w, idx) => (
              <div
                key={w.id || idx}
                className={`h-full flex-1 rounded-full transition-all duration-200 ${
                  idx === currentIndex
                    ? "bg-[#005721]"
                    : idx < currentIndex || ratings[w.id]
                    ? "bg-[#60B515]"
                    : "bg-[#e0e4dc]"
                }`}
              />
            ))}
          </div>
        </div>
      </div>

      {/* ── 3D Flashcard Component ── */}
      <LearnWordsCard
        key={`${currentWord.id}-${direction}`}
        wordData={currentWord}
        userRating={ratings[currentWord.id]}
        onRate={handleRate}
        direction={direction}
        isFlipped={isFlipped}
        onFlip={setIsFlipped}
        showRatingControls={true}
      />

      {/* ── Bottom Navigation Controls & Keyboard Helpers ── */}
      <div className="w-full flex flex-col sm:flex-row items-center justify-between gap-4 pt-2">
        {/* Previous Button */}
        <button
          type="button"
          aria-label="Go to previous flashcard"
          onClick={handlePrevious}
          disabled={currentIndex === 0}
          className="min-h-[44px] inline-flex items-center gap-1.5 rounded-xl border border-[#E2E8F0] bg-white hover:bg-[#f8faf9] disabled:opacity-40 disabled:cursor-not-allowed px-4 py-2.5 text-xs font-bold text-[#0F172A] transition-all shadow-xs order-2 sm:order-1"
        >
          <ArrowLeft className="w-4 h-4 text-[#334155]" />
          <span>Previous</span>
        </button>

        {/* Keyboard Helper Badges */}
        <div className="flex items-center gap-3 flex-wrap justify-center text-xs text-[#334155] order-1 sm:order-2">
          <span className="inline-flex items-center gap-1">
            <kbd className="px-1.5 py-0.5 bg-[#ebefe7] rounded text-[11px] font-mono text-[#0F172A] border border-[#bfc9bc]">
              Space
            </kbd>
            <span>Flip</span>
          </span>
          <span className="inline-flex items-center gap-1">
            <kbd className="px-1.5 py-0.5 bg-[#ebefe7] rounded text-[11px] font-mono text-[#0F172A] border border-[#bfc9bc]">
              ← / →
            </kbd>
            <span>Navigate</span>
          </span>
          <span className="hidden md:inline-flex items-center gap-1">
            <kbd className="px-1.5 py-0.5 bg-[#ebefe7] rounded text-[11px] font-mono text-[#0F172A] border border-[#bfc9bc]">
              1 - 4
            </kbd>
            <span>Rate</span>
          </span>
        </div>

        {/* Next / Finish Button */}
        <button
          type="button"
          aria-label={currentIndex === words.length - 1 ? "Finish practice session" : "Go to next flashcard"}
          onClick={handleNext}
          className="min-h-[44px] inline-flex items-center gap-1.5 rounded-xl bg-[#005721] hover:bg-[#257037] active:scale-[0.98] px-6 py-2.5 text-xs font-bold text-white shadow-sm hover:shadow-md transition-all order-3"
        >
          <span>{currentIndex === words.length - 1 ? "Finish Session" : "Next Word"}</span>
          <ArrowRight className="w-4 h-4" />
        </button>
      </div>

      {/* Spaced repetition footer badge */}
      <div className="flex items-center justify-center gap-1.5 pt-2 text-xs text-[#64748B]">
        <Sparkles className="w-3.5 h-3.5 text-[#60B515]" />
        <span>Spaced Repetition schedules review intervals automatically based on your rating.</span>
      </div>
    </div>
  );
}
