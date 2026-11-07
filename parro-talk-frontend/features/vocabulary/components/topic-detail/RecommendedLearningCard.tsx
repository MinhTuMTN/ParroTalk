import React from "react";
import Link from "next/link";
import { Sparkles, ArrowRight, BookOpen, Clock } from "lucide-react";
import type { TopicWithProgress } from "../../lib/topicMeta";

interface Props {
  topic: TopicWithProgress;
  nextWord?: string;
}

export function RecommendedLearningCard({ topic, nextWord = "Itinerary" }: Props) {
  const activeWords = Number(topic.learned ?? (topic as unknown as { learnedWordCount?: number }).learnedWordCount ?? 0) || 0;
  const isStarted = topic.status !== "not-started" && activeWords > 0;
  const topicName = encodeURIComponent(topic.name);

  // Dynamic context
  const remainingWords = Math.max(0, (Number(topic.wordCount) || 0) - activeWords);
  const wordsInCard = isStarted ? Math.min(24, Number(topic.wordCount) || 0) : Number(topic.wordCount) || 0;
  const estimatedTime = isStarted ? "~10 min" : "~15 min";

  const practiceUrl = `/vocabulary/practice?topic=${topicName}&mode=continue`;

  return (
    <section className="relative overflow-hidden rounded-3xl border border-brand-200/80 bg-gradient-to-br from-brand-50/90 via-[#f0f9f4] to-brand-50 p-6 sm:p-7 shadow-card animate-fade-up">
      {/* Background ambient decorative glow */}
      <div className="pointer-events-none absolute -right-12 -top-12 h-44 w-44 rounded-full bg-white/70 blur-2xl" />

      <div className="relative flex flex-col lg:flex-row lg:items-center justify-between gap-6">
        {/* Left side: Context & Recommendation */}
        <div className="flex-1 min-w-0 space-y-3">
          <div className="inline-flex items-center gap-1.5 rounded-full bg-white/80 border border-brand-200/60 px-2.5 py-1 text-xs font-semibold text-brand-700 shadow-sm backdrop-blur">
            <Sparkles className="w-3.5 h-3.5 text-brand-600" />
            <span>Recommended for you</span>
          </div>

          <div>
            <h2 className="text-xl sm:text-2xl font-extrabold text-ink-900 tracking-tight leading-snug">
              {isStarted ? "Continue Learning" : "Start Learning"}
            </h2>
            <p className="text-xs sm:text-sm text-ink-600 mt-1 max-w-xl leading-relaxed">
              {isStarted
                ? `You're making good progress. Revisit the ${Math.min(
                    8,
                    activeWords
                  )} words you're learning, then discover ${Math.min(
                    16,
                    remainingWords
                  )} new ones at your own pace.`
                : `Start learning the vocabulary in this topic. Build a solid foundation with practical everyday words.`}
            </p>
          </div>

          <div className="flex items-center gap-4 text-xs font-medium text-ink-600 pt-1">
            <span className="inline-flex items-center gap-1.5">
              <BookOpen className="w-3.5 h-3.5 text-brand-600" />
              <span>{wordsInCard} words</span>
            </span>
            <span className="text-ink-300">•</span>
            <span className="inline-flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5 text-brand-600" />
              <span>{estimatedTime}</span>
            </span>
          </div>
        </div>

        {/* Right side: Pick up with contextual action */}
        <div className="flex flex-col sm:flex-row lg:flex-col items-start sm:items-center lg:items-end justify-between lg:justify-center gap-3 shrink-0 pt-2 lg:pt-0 border-t lg:border-t-0 border-brand-100">
          <div className="text-left lg:text-right space-y-0.5">
            <div className="text-[10px] font-bold uppercase tracking-wider text-ink-400">
              PICK UP WITH
            </div>
            <div className="text-xs font-bold text-ink-800">
              &ldquo;{nextWord}&rdquo; &amp; planning your trip
            </div>
          </div>

          <div className="w-full sm:w-auto text-left sm:text-right space-y-1.5">
            <Link
              href={practiceUrl}
              className="group inline-flex items-center justify-center gap-2 w-full sm:w-auto rounded-xl bg-gradient-to-b from-brand-500 to-brand-600 hover:from-brand-600 hover:to-brand-700 active:scale-[0.98] px-6 py-2.5 text-sm font-bold text-white shadow-brand transition-all hover:shadow-card-hover"
            >
              <span>{isStarted ? "Continue Learning" : "Start Learning"}</span>
              <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
            </Link>
            <div className="text-[11px] text-ink-400 text-center lg:text-right">
              A little progress, one word at a time.
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
