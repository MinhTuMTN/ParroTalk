import React from "react";
import type { TopicWithProgress } from "../../lib/topicMeta";

interface Props {
  topic: TopicWithProgress;
}

export function TopicProgressCard({ topic }: Props) {
  const total = topic.wordCount || 40;
  const progress = topic.progress || 0;

  // Derive granular stats
  const learned = topic.learned;
  const mastered = Math.min(learned, Math.round(learned * 0.67));
  const currentlyLearning = learned - mastered;
  const remaining = Math.max(0, total - learned);

  const masteredPct = (mastered / total) * 100;
  const learningPct = (currentlyLearning / total) * 100;

  return (
    <section className="rounded-3xl border border-ink-200/80 bg-white p-6 sm:p-7 shadow-card space-y-6">
      {/* Top Header & Percentage */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div>
          <h2 className="text-lg font-bold text-ink-900 tracking-tight">Your topic progress</h2>
          <p className="text-xs text-ink-500 mt-0.5">
            {learned > 0
              ? `${learned} words are already a little more familiar.`
              : "Start learning to track your mastery here."}
          </p>
        </div>

        <div className="flex items-baseline gap-1 text-right">
          <span className="text-2xl sm:text-3xl font-extrabold text-brand-700 tracking-tight">
            {progress}%
          </span>
          <span className="text-xs font-semibold text-ink-400">complete</span>
        </div>
      </div>

      {/* Segmented Progress Bar */}
      <div className="h-2.5 w-full rounded-full bg-ink-100 overflow-hidden flex">
        {masteredPct > 0 && (
          <div
            className="h-full bg-brand-600 transition-all duration-700 ease-out"
            style={{ width: `${masteredPct}%` }}
            title={`Mastered: ${mastered} words`}
          />
        )}
        {learningPct > 0 && (
          <div
            className="h-full bg-brand-300 transition-all duration-700 ease-out"
            style={{ width: `${learningPct}%` }}
            title={`Currently learning: ${currentlyLearning} words`}
          />
        )}
      </div>

      {/* 4 Stats Grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 divide-y sm:divide-y-0 sm:divide-x divide-ink-100 pt-1">
        <div className="pt-2 sm:pt-0 sm:pr-4">
          <div className="flex items-baseline gap-1.5">
            <span className="text-2xl font-extrabold text-ink-900">{learned}</span>
            <span className="text-xs font-bold text-ink-700">Learned</span>
          </div>
          <div className="text-[11px] text-ink-400 mt-0.5">Learning + mastered</div>
        </div>

        <div className="pt-2 sm:pt-0 sm:px-4">
          <div className="flex items-baseline gap-1.5">
            <span className="text-2xl font-extrabold text-ink-900">{currentlyLearning}</span>
            <span className="text-xs font-bold text-ink-700">Currently learning</span>
          </div>
          <div className="text-[11px] text-ink-400 mt-0.5">Getting more familiar</div>
        </div>

        <div className="pt-2 sm:pt-0 sm:px-4">
          <div className="flex items-baseline gap-1.5">
            <span className="text-2xl font-extrabold text-brand-700">{mastered}</span>
            <span className="text-xs font-bold text-ink-700">Mastered</span>
          </div>
          <div className="text-[11px] text-ink-400 mt-0.5">Ready to use confidently</div>
        </div>

        <div className="pt-2 sm:pt-0 sm:pl-4">
          <div className="flex items-baseline gap-1.5">
            <span className="text-2xl font-extrabold text-ink-900">{remaining}</span>
            <span className="text-xs font-bold text-ink-700">Remaining</span>
          </div>
          <div className="text-[11px] text-ink-400 mt-0.5">New words to discover</div>
        </div>
      </div>
    </section>
  );
}
