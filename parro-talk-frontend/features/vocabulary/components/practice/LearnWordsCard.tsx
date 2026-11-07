"use client";

import React, { useState } from "react";
import {
  Volume2,
  Bookmark,
  BookmarkCheck,
  RotateCw,
  Sparkles,
  Quote,
  Check,
  CheckCircle2,
  Copy,
} from "lucide-react";

export type RatingValue = "again" | "hard" | "good" | "easy";
export type TranslationDirection = "EN_VI" | "VI_EN";

export interface OtherMeaningSense {
  partOfSpeech: string;
  cefrLevel: string;
  englishDefinition: string;
  vietnameseMeaning: string;
  exampleSentence?: string;
  exampleTranslation?: string;
}

export interface LearnWordData {
  id: string;
  word: string;
  partOfSpeech?: string;
  cefrLevel?: string;
  topic?: string;
  phonetic?: string;
  phoneticUs?: string;
  phoneticUk?: string;
  audioUsUrl?: string;
  audioUkUrl?: string;
  primaryEnglishDefinition: string;
  primaryVietnameseMeaning: string;
  conversationExample?: {
    english: string;
    vietnamese: string;
  };
  collocations?: string[];
  otherMeanings?: OtherMeaningSense[];
  synonyms?: string[];
  antonyms?: string[];
  additionalExamples?: { english: string; vietnamese: string }[];
  phrasalVerbs?: string[];
  idioms?: { idiom: string; meaning: string }[];
  additionalCollocations?: string[];
}

interface Props {
  wordData: LearnWordData;
  userRating?: RatingValue;
  onRate: (rating: RatingValue) => void;
  direction?: TranslationDirection;
  isFlipped?: boolean;
  onFlip?: (flipped: boolean) => void;
  showRatingControls?: boolean;
}

export function LearnWordsCard({
  wordData,
  userRating,
  onRate,
  direction = "EN_VI",
  isFlipped: controlledFlipped,
  onFlip: setControlledFlipped,
  showRatingControls = true,
}: Props) {
  const [internalFlipped, setInternalFlipped] = useState(false);
  const [playingAccent, setPlayingAccent] = useState<"US" | "UK" | "SENTENCE" | null>(null);
  const [isBookmarked, setIsBookmarked] = useState(false);
  const [copiedPhonetic, setCopiedPhonetic] = useState(false);

  const isFlipped = controlledFlipped !== undefined ? controlledFlipped : internalFlipped;

  const handleToggleFlip = () => {
    if (setControlledFlipped) {
      setControlledFlipped(!isFlipped);
    } else {
      setInternalFlipped((prev) => !prev);
    }
  };

  const phonetic =
    wordData.phoneticUs ||
    wordData.phonetic ||
    wordData.phoneticUk ||
    `/${wordData.word}/`;

  const playAudio = (accent: "US" | "UK") => {
    setPlayingAccent(accent);
    const audioUrl = accent === "US" ? wordData.audioUsUrl : wordData.audioUkUrl;

    if (audioUrl) {
      const audio = new Audio(audioUrl);
      audio.onended = () => setPlayingAccent(null);
      audio.onerror = () => fallbackSpeech(wordData.word, accent);
      audio.play().catch(() => fallbackSpeech(wordData.word, accent));
    } else {
      fallbackSpeech(wordData.word, accent);
    }
  };

  const playSentenceAudio = () => {
    const text = wordData.conversationExample?.english;
    if (!text) return;

    setPlayingAccent("SENTENCE");
    if (typeof window !== "undefined" && "speechSynthesis" in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = "en-US";
      utterance.onend = () => setPlayingAccent(null);
      utterance.onerror = () => setPlayingAccent(null);
      window.speechSynthesis.speak(utterance);
    } else {
      setPlayingAccent(null);
    }
  };

  const fallbackSpeech = (text: string, accent: "US" | "UK") => {
    if (typeof window !== "undefined" && "speechSynthesis" in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = accent === "US" ? "en-US" : "en-GB";
      utterance.onend = () => setPlayingAccent(null);
      utterance.onerror = () => setPlayingAccent(null);
      window.speechSynthesis.speak(utterance);
    } else {
      setPlayingAccent(null);
    }
  };

  const copyPhoneticToClipboard = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (typeof navigator !== "undefined" && navigator.clipboard) {
      navigator.clipboard.writeText(phonetic).then(() => {
        setCopiedPhonetic(true);
        setTimeout(() => setCopiedPhonetic(false), 2000);
      });
    }
  };

  const renderHighlightedText = (sentence: string, target: string) => {
    if (!sentence || !target) return sentence;
    const regex = new RegExp(`(${target})`, "gi");
    const parts = sentence.split(regex);

    return parts.map((part, i) =>
      part.toLowerCase() === target.toLowerCase() ? (
        <strong
          key={i}
          className="text-[#005721] font-bold underline decoration-[#60B515] decoration-2 underline-offset-2"
        >
          {part}
        </strong>
      ) : (
        <React.Fragment key={i}>{part}</React.Fragment>
      )
    );
  };

  const collocations = wordData.collocations || wordData.additionalCollocations || [];
  const synonyms = wordData.synonyms || [];

  return (
    <div className="w-full flex flex-col items-center">
      {/* ── 3D Interactive Flashcard Container ── */}
      <div className="w-full max-w-2xl perspective-1200 select-none">
        <div
          role="button"
          tabIndex={0}
          aria-label={`Flashcard for ${wordData.word}. Press Space or Enter to flip card.`}
          aria-expanded={isFlipped}
          onClick={handleToggleFlip}
          onKeyDown={(e) => {
            if (e.key === "Enter" || e.key === " ") {
              e.preventDefault();
              handleToggleFlip();
            }
          }}
          className={`relative w-full min-h-[460px] md:min-h-[500px] transition-transform duration-500 transform-style-3d cursor-pointer outline-none focus-visible:ring-4 focus-visible:ring-[#60B515]/50 rounded-3xl ${
            isFlipped ? "rotate-y-180" : ""
          }`}
        >
          {/* ══════════════════════════════════════════════════════════════════════
              CARD FRONT FACE
             ══════════════════════════════════════════════════════════════════════ */}
          <div className="absolute inset-0 w-full h-full bg-white border-2 border-[#E2E8F0] rounded-3xl p-6 sm:p-10 flex flex-col justify-between shadow-[0_8px_30px_rgb(0,0,0,0.06)] hover:shadow-[0_12px_36px_rgb(0,0,0,0.09)] transition-shadow backface-hidden">
            {/* Front Header */}
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="px-3 py-1 rounded-full bg-[#f1f5ed] text-[#0F172A] text-xs font-semibold flex items-center gap-1.5 border border-[#e0e4dc]">
                  <span>💼</span> {wordData.topic || "Vocabulary Practice"}
                </span>
                <span className="px-3 py-1 rounded-full bg-[#005721]/10 text-[#005721] text-xs font-bold border border-[#005721]/20">
                  {wordData.cefrLevel || "B2"} CEFR
                </span>
                <span className="px-2.5 py-0.5 rounded-full bg-[#0F172A]/5 text-[#334155] text-xs font-medium">
                  {direction === "EN_VI" ? "EN → VI" : "VI → EN"}
                </span>
              </div>

              <button
                type="button"
                aria-label={isBookmarked ? "Remove bookmark" : "Bookmark this word"}
                onClick={(e) => {
                  e.stopPropagation();
                  setIsBookmarked(!isBookmarked);
                }}
                className="min-w-[44px] min-h-[44px] inline-flex items-center justify-center rounded-xl text-[#334155] hover:text-[#005721] hover:bg-[#f1f5ed] transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-[#005721]"
              >
                {isBookmarked ? (
                  <BookmarkCheck className="w-5 h-5 text-[#005721]" />
                ) : (
                  <Bookmark className="w-5 h-5 text-[#334155]" />
                )}
              </button>
            </div>

            {/* Front Center Content */}
            {direction === "EN_VI" ? (
              /* EN → VI Mode: English headword on front */
              <div className="my-auto text-center flex flex-col items-center justify-center py-6">
                <div className="inline-block px-3 py-0.5 rounded-full bg-[#f1f5ed] text-[#005721] text-xs font-bold uppercase tracking-wider mb-3">
                  {wordData.partOfSpeech || "word"}
                </div>

                <h1 className="text-4xl sm:text-[44px] font-extrabold text-[#0F172A] tracking-tight mb-2">
                  {wordData.word}
                </h1>

                <div className="flex items-center gap-2 text-[#334155] mb-6 text-base sm:text-lg">
                  <span className="font-mono tracking-wide">{phonetic}</span>
                  <button
                    type="button"
                    aria-label="Copy phonetic transcription"
                    onClick={copyPhoneticToClipboard}
                    className="min-w-[36px] min-h-[36px] inline-flex items-center justify-center rounded-lg hover:bg-[#f1f5ed] text-[#64748B] hover:text-[#005721] transition-colors"
                  >
                    {copiedPhonetic ? (
                      <Check className="w-4 h-4 text-[#60B515]" />
                    ) : (
                      <Copy className="w-4 h-4" />
                    )}
                  </button>
                </div>

                {/* Dual Audio Buttons with Animated Equalizer Bars */}
                <div
                  className="flex items-center gap-4 flex-wrap justify-center"
                  onClick={(e) => e.stopPropagation()}
                >
                  {/* US Audio */}
                  <button
                    type="button"
                    aria-label="Play US audio pronunciation"
                    onClick={() => playAudio("US")}
                    className={`min-h-[44px] min-w-[120px] px-4 py-2.5 rounded-xl border flex items-center gap-3 transition-all duration-200 shadow-xs active:scale-95 ${
                      playingAccent === "US"
                        ? "border-[#005721] bg-[#005721]/10 text-[#005721] ring-2 ring-[#60B515]/40"
                        : "border-[#E2E8F0] hover:border-[#005721] bg-white text-[#0F172A] hover:bg-[#f8faf9]"
                    }`}
                  >
                    <span className="font-bold text-xs bg-[#005721]/10 text-[#005721] px-1.5 py-0.5 rounded">
                      US
                    </span>
                    <Volume2 className="w-4 h-4 text-[#005721]" />
                    <div className="flex items-end gap-0.5 h-4 w-5">
                      <span
                        className={`w-1 bg-[#005721] rounded-full transition-all ${
                          playingAccent === "US" ? "wave-bar-1" : "h-1"
                        }`}
                      />
                      <span
                        className={`w-1 bg-[#005721] rounded-full transition-all ${
                          playingAccent === "US" ? "wave-bar-2" : "h-2"
                        }`}
                      />
                      <span
                        className={`w-1 bg-[#005721] rounded-full transition-all ${
                          playingAccent === "US" ? "wave-bar-3" : "h-3"
                        }`}
                      />
                      <span
                        className={`w-1 bg-[#005721] rounded-full transition-all ${
                          playingAccent === "US" ? "wave-bar-4" : "h-1"
                        }`}
                      />
                    </div>
                  </button>

                  {/* UK Audio */}
                  <button
                    type="button"
                    aria-label="Play UK audio pronunciation"
                    onClick={() => playAudio("UK")}
                    className={`min-h-[44px] min-w-[120px] px-4 py-2.5 rounded-xl border flex items-center gap-3 transition-all duration-200 shadow-xs active:scale-95 ${
                      playingAccent === "UK"
                        ? "border-[#8c4f00] bg-[#ffdcbf]/40 text-[#8c4f00] ring-2 ring-[#ff9c2b]/40"
                        : "border-[#E2E8F0] hover:border-[#8c4f00] bg-white text-[#0F172A] hover:bg-[#f8faf9]"
                    }`}
                  >
                    <span className="font-bold text-xs bg-[#ffdcbf] text-[#8c4f00] px-1.5 py-0.5 rounded">
                      UK
                    </span>
                    <Volume2 className="w-4 h-4 text-[#8c4f00]" />
                    <div className="flex items-end gap-0.5 h-4 w-5">
                      <span
                        className={`w-1 bg-[#8c4f00] rounded-full transition-all ${
                          playingAccent === "UK" ? "wave-bar-1" : "h-1.5"
                        }`}
                      />
                      <span
                        className={`w-1 bg-[#8c4f00] rounded-full transition-all ${
                          playingAccent === "UK" ? "wave-bar-2" : "h-3"
                        }`}
                      />
                      <span
                        className={`w-1 bg-[#8c4f00] rounded-full transition-all ${
                          playingAccent === "UK" ? "wave-bar-3" : "h-2"
                        }`}
                      />
                      <span
                        className={`w-1 bg-[#8c4f00] rounded-full transition-all ${
                          playingAccent === "UK" ? "wave-bar-4" : "h-1"
                        }`}
                      />
                    </div>
                  </button>
                </div>
              </div>
            ) : (
              /* VI → EN Mode: Vietnamese meaning prompts English recall */
              <div className="my-auto text-center flex flex-col items-center justify-center py-6">
                <div className="inline-block px-3 py-0.5 rounded-full bg-[#f1f5ed] text-[#005721] text-xs font-bold uppercase tracking-wider mb-3">
                  Gợi ý từ loại: {wordData.partOfSpeech || "từ vựng"}
                </div>

                <span className="text-xs uppercase font-semibold text-[#64748B] tracking-wider mb-2">
                  Ý nghĩa tiếng Việt cần nhớ
                </span>

                <h1 className="text-2xl sm:text-3xl font-extrabold text-[#005721] tracking-tight mb-4 max-w-lg leading-snug">
                  {wordData.primaryVietnameseMeaning}
                </h1>

                {wordData.primaryEnglishDefinition && (
                  <p className="text-sm text-[#334155] max-w-md italic mb-4">
                    Context: &ldquo;{wordData.primaryEnglishDefinition}&rdquo;
                  </p>
                )}

                <div className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-[#f7fbf2] border border-[#e0e4dc] text-xs font-semibold text-[#005721]">
                  <span>💡 Bạn có nhớ từ tiếng Anh tương ứng không?</span>
                </div>
              </div>
            )}

            {/* Front Card Bottom Prompt */}
            <div className="pt-4 border-t border-[#E2E8F0] flex items-center justify-between text-xs text-[#334155]">
              <span className="flex items-center gap-1.5">
                <Sparkles className="w-4 h-4 text-[#60B515]" />
                <span>Spaced Repetition Active</span>
              </span>
              <div className="flex items-center gap-1.5 font-semibold text-[#005721] group-hover:underline">
                <span>
                  {direction === "EN_VI"
                    ? "Click card or press [Space] to reveal Vietnamese meaning"
                    : "Click card or press [Space] to reveal English word & audio"}
                </span>
                <RotateCw className="w-3.5 h-3.5" />
              </div>
            </div>
          </div>

          {/* ══════════════════════════════════════════════════════════════════════
              CARD BACK FACE
             ══════════════════════════════════════════════════════════════════════ */}
          <div className="absolute inset-0 w-full h-full bg-white border-2 border-[#005721]/30 rounded-3xl p-6 sm:p-10 flex flex-col justify-between shadow-[0_12px_40px_rgba(0,87,33,0.08)] backface-hidden rotate-y-180">
            {/* Back Header */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="px-3 py-1 rounded-full bg-[#005721]/10 text-[#005721] text-xs font-bold flex items-center gap-1.5 border border-[#005721]/20">
                  <CheckCircle2 className="w-3.5 h-3.5 text-[#60B515]" />
                  {direction === "EN_VI" ? "Nghĩa tiếng Việt đã mở" : "Từ tiếng Anh mục tiêu"}
                </span>
                <span className="px-2.5 py-0.5 text-xs text-[#334155] bg-[#f1f5ed] rounded-full font-medium">
                  {wordData.partOfSpeech}
                </span>
              </div>
              <span className="text-xs font-bold text-[#005721] bg-[#005721]/10 px-2.5 py-0.5 rounded-full border border-[#005721]/20">
                Level {wordData.cefrLevel || "B2"}
              </span>
            </div>

            {/* Back Content */}
            <div className="my-auto py-3 space-y-4">
              {direction === "EN_VI" ? (
                /* EN → VI Mode: Vietnamese definition as primary on back */
                <div>
                  <span className="text-xs uppercase font-semibold text-[#64748B] tracking-wider block mb-1">
                    Định nghĩa chính
                  </span>
                  <h2 className="text-2xl sm:text-[28px] text-[#005721] font-bold leading-tight">
                    {wordData.primaryVietnameseMeaning}
                  </h2>
                  {wordData.primaryEnglishDefinition && (
                    <p className="text-xs sm:text-sm text-[#334155] mt-1.5 font-medium">
                      {wordData.primaryEnglishDefinition}
                    </p>
                  )}
                </div>
              ) : (
                /* VI → EN Mode: English target word & audio revealed on back */
                <div>
                  <span className="text-xs uppercase font-semibold text-[#64748B] tracking-wider block mb-1">
                    Target English Word
                  </span>
                  <div className="flex items-center gap-4 flex-wrap">
                    <h2 className="text-3xl sm:text-4xl text-[#005721] font-extrabold tracking-tight">
                      {wordData.word}
                    </h2>
                    <span className="text-base text-[#334155] font-mono">{phonetic}</span>
                    <button
                      type="button"
                      aria-label="Play US audio pronunciation"
                      onClick={(e) => {
                        e.stopPropagation();
                        playAudio("US");
                      }}
                      className="min-w-[44px] min-h-[44px] px-3 py-1.5 rounded-xl border border-[#005721]/40 bg-[#005721]/10 text-[#005721] text-xs font-bold inline-flex items-center gap-1.5 hover:bg-[#005721]/20 transition-colors"
                    >
                      <Volume2 className="w-4 h-4 text-[#005721]" />
                      <span>US Listen</span>
                    </button>
                  </div>
                  <p className="text-xs text-[#64748B] mt-1">
                    Nghĩa tiếng Việt: <strong className="text-[#0F172A]">{wordData.primaryVietnameseMeaning}</strong>
                  </p>
                </div>
              )}

              {/* Contextual Example Sentence */}
              {wordData.conversationExample && (
                <div className="bg-[#f7fbf2] rounded-2xl p-4 border border-[#e0e4dc] space-y-2">
                  <div className="flex items-center justify-between text-xs text-[#334155]">
                    <span className="font-bold flex items-center gap-1 text-[#005721]">
                      <Quote className="w-3.5 h-3.5 text-[#005721]" />
                      Ví dụ ngữ cảnh ({wordData.topic || "Giao tiếp thực tế"})
                    </span>
                    <button
                      type="button"
                      aria-label="Listen to example sentence"
                      onClick={(e) => {
                        e.stopPropagation();
                        playSentenceAudio();
                      }}
                      className="min-w-[36px] min-h-[36px] inline-flex items-center justify-center rounded-lg hover:bg-white text-[#005721] transition-colors"
                    >
                      <Volume2
                        className={`w-4 h-4 ${
                          playingAccent === "SENTENCE" ? "animate-pulse text-[#60B515]" : ""
                        }`}
                      />
                    </button>
                  </div>
                  <p className="text-sm sm:text-base text-[#0F172A] font-medium leading-relaxed">
                    &ldquo;{renderHighlightedText(wordData.conversationExample.english, wordData.word)}&rdquo;
                  </p>
                  {wordData.conversationExample.vietnamese && (
                    <p className="text-xs sm:text-sm text-[#334155] italic">
                      {wordData.conversationExample.vietnamese}
                    </p>
                  )}
                </div>
              )}

              {/* Collocations */}
              {collocations.length > 0 && (
                <div>
                  <span className="text-xs font-semibold text-[#64748B] block mb-2">
                    Cụm từ thông dụng (Collocations):
                  </span>
                  <div className="flex flex-wrap gap-2">
                    {collocations.map((item, idx) => (
                      <span
                        key={idx}
                        className="px-3 py-1 bg-[#f1f5ed] rounded-lg text-xs font-semibold text-[#0F172A] border border-[#e0e4dc]"
                      >
                        {item}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* Synonyms */}
              {synonyms.length > 0 && (
                <div className="flex items-center gap-2 flex-wrap text-xs">
                  <span className="font-semibold text-[#64748B]">Từ đồng nghĩa:</span>
                  {synonyms.map((s, idx) => (
                    <span
                      key={idx}
                      className="text-[#005721] font-semibold underline decoration-[#60B515] underline-offset-2"
                    >
                      {s}
                    </span>
                  ))}
                </div>
              )}
            </div>

            {/* Back Card Bottom Prompt */}
            <div className="pt-3 border-t border-[#E2E8F0] flex items-center justify-between text-xs text-[#334155]">
              <span className="text-[#005721] font-medium flex items-center gap-1">
                <Check className="w-3.5 h-3.5 text-[#60B515]" /> Đã mở khóa nội dung
              </span>
              <div className="flex items-center gap-1.5 font-semibold text-[#005721]">
                <span>[Space] or click to flip back</span>
                <RotateCw className="w-3.5 h-3.5" />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ── Spaced Repetition (SM-2) Feedback Controls ── */}
      {showRatingControls && (
        <div className="w-full max-w-2xl mt-6">
          <div className="bg-white border border-[#E2E8F0] rounded-2xl p-4 shadow-xs">
            <div className="flex items-center justify-between mb-3 px-1">
              <span className="text-xs font-bold text-[#0F172A] uppercase tracking-wider">
                Rate Your Recall
              </span>
              <span className="text-xs text-[#334155]">
                {isFlipped
                  ? "Select interval or press key [1 - 4]"
                  : "Flip card to reveal answer before rating"}
              </span>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {/* Again [1] */}
              <button
                type="button"
                aria-label="Rate recall as Again (< 1 min, shortcut key 1)"
                onClick={() => onRate("again")}
                className={`min-h-[64px] flex flex-col items-center justify-center p-3 rounded-xl border transition-all active:scale-95 ${
                  userRating === "again"
                    ? "border-[#ba1a1a] bg-[#ffdad6]/40 text-[#ba1a1a] ring-2 ring-[#ba1a1a]/40"
                    : "border-[#ba1a1a]/30 hover:border-[#ba1a1a] bg-white hover:bg-[#ffdad6]/20 text-[#ba1a1a]"
                }`}
              >
                <span className="text-xs font-bold uppercase tracking-wider">Again</span>
                <span className="text-xs text-[#334155] font-medium mt-0.5">&lt; 1 min</span>
                <span className="text-[10px] text-[#707a6e] mt-1 bg-[#f1f5ed] px-1.5 py-0.5 rounded font-mono">
                  [ 1 ]
                </span>
              </button>

              {/* Hard [2] */}
              <button
                type="button"
                aria-label="Rate recall as Hard (10 mins, shortcut key 2)"
                onClick={() => onRate("hard")}
                className={`min-h-[64px] flex flex-col items-center justify-center p-3 rounded-xl border transition-all active:scale-95 ${
                  userRating === "hard"
                    ? "border-[#8c4f00] bg-[#ffdcbf]/50 text-[#8c4f00] ring-2 ring-[#ff9c2b]/40"
                    : "border-[#8c4f00]/40 hover:border-[#8c4f00] bg-white hover:bg-[#ffdcbf]/20 text-[#8c4f00]"
                }`}
              >
                <span className="text-xs font-bold uppercase tracking-wider">Hard</span>
                <span className="text-xs text-[#334155] font-medium mt-0.5">10 mins</span>
                <span className="text-[10px] text-[#707a6e] mt-1 bg-[#f1f5ed] px-1.5 py-0.5 rounded font-mono">
                  [ 2 ]
                </span>
              </button>

              {/* Good [3] */}
              <button
                type="button"
                aria-label="Rate recall as Good (1 day, shortcut key 3)"
                onClick={() => onRate("good")}
                className={`min-h-[64px] flex flex-col items-center justify-center p-3 rounded-xl border transition-all active:scale-95 ${
                  userRating === "good"
                    ? "border-[#005721] bg-[#005721]/15 text-[#005721] ring-2 ring-[#60B515]/40"
                    : "border-[#005721]/40 hover:border-[#005721] bg-[#005721]/5 hover:bg-[#005721]/10 text-[#005721]"
                }`}
              >
                <span className="text-xs font-bold uppercase tracking-wider">Good</span>
                <span className="text-xs text-[#005721] font-medium mt-0.5">1 day</span>
                <span className="text-[10px] text-[#005721] mt-1 bg-[#005721]/10 px-1.5 py-0.5 rounded font-mono">
                  [ 3 ]
                </span>
              </button>

              {/* Easy [4] */}
              <button
                type="button"
                aria-label="Rate recall as Easy (4 days, shortcut key 4)"
                onClick={() => onRate("easy")}
                className={`min-h-[64px] flex flex-col items-center justify-center p-3 rounded-xl transition-all shadow-sm hover:shadow-md border-b-4 border-[#002108] active:translate-y-0.5 text-white ${
                  userRating === "easy"
                    ? "bg-[#257037] ring-2 ring-[#60B515]"
                    : "bg-[#005721] hover:bg-[#257037]"
                }`}
              >
                <span className="text-xs font-bold uppercase tracking-wider">Easy</span>
                <span className="text-xs text-white/90 font-medium mt-0.5">4 days</span>
                <span className="text-[10px] text-white/90 mt-1 bg-white/20 px-1.5 py-0.5 rounded font-mono">
                  [ 4 ]
                </span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
