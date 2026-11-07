import React from "react";
import Link from "next/link";
import { Play, ArrowRight } from "lucide-react";
import type { TopicWithProgress } from "../../lib/topicMeta";

interface Props {
  topic: TopicWithProgress;
  nextWord?: string;
}

export function TopicSidebar({ topic, nextWord = "Itinerary" }: Props) {
  const total = Number(topic.wordCount) || 40;
  const learned = Number(topic.learned ?? (topic as unknown as { learnedWordCount?: number }).learnedWordCount ?? 0) || 0;
  const progress = Number(topic.progress ?? 0) || 0;
  const mastered = Math.min(learned, Math.round(learned * 0.67));
  const learning = learned - mastered;
  const newCount = Math.max(0, total - learned);

  return (
    <aside className="space-y-5">
      {/* Card 1: Pick up where you left off */}
      <div className="rounded-3xl border border-ink-200/80 bg-white p-6 shadow-card space-y-4">
        {/* Play Icon Circle */}
        <div className="w-10 h-10 rounded-2xl bg-brand-50 border border-brand-100 flex items-center justify-center text-brand-600">
          <Play className="w-4.5 h-4.5 fill-brand-600" />
        </div>

        <div>
          <h3 className="text-base font-bold text-ink-900 leading-snug">
            Pick up where you left off
          </h3>
          <p className="text-xs text-ink-500 mt-1 leading-relaxed">
            You&apos;re {progress}% through this topic. Continue where you left off — a few words at a time.
          </p>
        </div>

        {/* NEXT UP block */}
        <div className="rounded-xl border border-ink-100 bg-ink-50/70 p-3.5 space-y-1">
          <div className="text-[10px] font-bold uppercase tracking-wider text-ink-400">
            NEXT UP
          </div>
          <div className="text-xs font-semibold text-ink-800">
            &ldquo;{nextWord}&rdquo; &amp; planning your trip
          </div>
        </div>

        {/* Button */}
        <Link
          href={`/vocabulary/practice?topic=${encodeURIComponent(topic.name)}`}
          className="group flex items-center justify-center gap-2 w-full rounded-xl border border-brand-200 bg-brand-50/60 hover:bg-brand-600 hover:text-white hover:border-brand-600 py-2.5 px-4 text-xs font-bold text-brand-700 transition-all shadow-sm"
        >
          <span>Resume learning</span>
          <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-1" />
        </Link>
      </div>

      {/* Card 2: Your words, at a glance */}
      <div className="rounded-3xl border border-ink-200/80 bg-white p-6 shadow-card space-y-4">
        <h3 className="text-sm font-bold text-ink-900">
          Your words, at a glance
        </h3>

        <div className="space-y-3 text-xs">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-ink-600">
              <span className="w-2 h-2 rounded-full bg-ink-300" />
              <span>New</span>
            </div>
            <span className="font-bold text-ink-900">{newCount}</span>
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-ink-600">
              <span className="w-2 h-2 rounded-full bg-coral-500" />
              <span>Learning</span>
            </div>
            <span className="font-bold text-ink-900">{learning}</span>
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-ink-600">
              <span className="w-2 h-2 rounded-full bg-brand-500" />
              <span>Mastered</span>
            </div>
            <span className="font-bold text-ink-900">{mastered}</span>
          </div>
        </div>

        <p className="text-[11px] text-ink-400 leading-relaxed border-t border-ink-100 pt-3">
          Learned words include the ones you&apos;re practicing and the ones you&apos;ve mastered.
        </p>
      </div>
    </aside>
  );
}
