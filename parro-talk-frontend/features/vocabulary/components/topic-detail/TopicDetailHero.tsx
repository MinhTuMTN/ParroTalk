import React from "react";
import Link from "next/link";
import { ArrowLeft, BookOpen } from "lucide-react";
import { CEFR_LABELS, type TopicWithProgress } from "../../lib/topicMeta";
import { TopicDetailBadge } from "./TopicDetailBadge";

interface Props {
  topic: TopicWithProgress;
}

export function TopicDetailHero({ topic }: Props) {
  return (
    <div className="space-y-4">
      {/* Top back navigation */}
      <div>
        <Link
          href="/vocabulary/topics"
          className="group inline-flex items-center gap-2 text-xs font-semibold text-ink-500 hover:text-brand-700 transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5 transition-transform group-hover:-translate-x-1" />
          <span>Back to Vocabulary Topics</span>
        </Link>
      </div>

      {/* Hero card */}
      <section className="relative overflow-hidden rounded-3xl border border-brand-200/80 bg-gradient-to-br from-brand-50/90 via-brand-100/40 to-brand-50 p-6 sm:p-8 shadow-card">
        {/* Soft background ambient glow */}
        <div className="pointer-events-none absolute -right-16 -top-16 h-64 w-64 rounded-full bg-white/70 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-16 left-1/3 h-52 w-52 rounded-full bg-brand-200/40 blur-3xl" />

        <div className="relative flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="flex-1 min-w-0 space-y-4">
            {/* Eyebrow */}
            <div className="text-[11px] font-bold uppercase tracking-[0.16em] text-brand-700">
              {topic.category ? topic.category.toUpperCase() : "GENERAL"} · WORDS FOR YOUR WORLD
            </div>

            {/* Title */}
            <h1 className="text-2xl sm:text-[32px] font-extrabold tracking-tight text-ink-900 leading-tight">
              {topic.name}
            </h1>

            {/* Description */}
            <p className="text-sm text-ink-600 max-w-xl leading-relaxed">
              {topic.description ||
                "Feel at home, wherever you go. Find the words for planning a trip, getting around, and making new memories."}
            </p>

            {/* Badges row */}
            <div className="flex flex-wrap items-center gap-2.5 pt-1 text-xs">
              <span className="inline-flex items-center rounded-lg bg-white/80 border border-brand-200/60 px-2.5 py-1 font-semibold text-brand-700 shadow-sm backdrop-blur">
                {topic.level} · {CEFR_LABELS[topic.level]}
              </span>

              <span className="inline-flex items-center gap-1.5 rounded-lg bg-white/80 border border-brand-200/60 px-2.5 py-1 font-medium text-ink-600 shadow-sm backdrop-blur">
                <BookOpen className="w-3.5 h-3.5 text-brand-600" />
                {topic.wordCount} words
              </span>

              <span className="inline-flex items-center gap-1.5 rounded-lg bg-white/80 border border-brand-200/60 px-2.5 py-1 font-medium text-ink-700 shadow-sm backdrop-blur">
                <span
                  className={`w-2 h-2 rounded-full ${
                    topic.status === "completed"
                      ? "bg-brand-500"
                      : topic.status === "in-progress"
                      ? "bg-coral-500 animate-pulse"
                      : "bg-ink-300"
                  }`}
                />
                {topic.status === "completed"
                  ? "Completed"
                  : topic.status === "in-progress"
                  ? "In progress"
                  : "Not started"}
              </span>
            </div>
          </div>

          {/* Right illustration sticker */}
          <div className="hidden sm:flex shrink-0 items-center justify-center">
            <TopicDetailBadge category={topic.category} />
          </div>
        </div>
      </section>
    </div>
  );
}
