"use client";

import { useMemo } from "react";
import { useTopicsWithProgress, useVocabularyStats } from "@/features/vocabulary/hooks/useTopicsWithProgress";
import { useTopicFilters, type StatusFilter } from "@/features/vocabulary/hooks/useTopicFilters";
import {
  TopicsHeader,
  TopicSearchBar,
  CategoryFilter,
  ContinueLearningStrip,
  ExploreToolbar,
  ExploreTopicGrid,
} from "@/features/vocabulary/components/topics";
import { FooterNote, SectionHeader } from "@/features/vocabulary/components/shared";

export default function VocabularyTopicsPage() {
  const { topics, loading } = useTopicsWithProgress();
  const { userLevel } = useVocabularyStats();
  const f = useTopicFilters(topics, userLevel);

  const inProgress = useMemo(() => topics.filter((t) => t.status === "in-progress").slice(0, 2), [topics]);

  // Status counts respect the other active filters so the numbers stay meaningful.
  const counts = useMemo(() => {
    const base = topics.filter(
      (t) => (f.category === "All" || t.category === f.category) && (f.level === "all" || t.level === f.level),
    );
    return {
      all: base.length,
      "not-started": base.filter((t) => t.status === "not-started").length,
      "in-progress": base.filter((t) => t.status === "in-progress").length,
      completed: base.filter((t) => t.status === "completed").length,
    } satisfies Record<StatusFilter, number>;
  }, [topics, f.category, f.level]);

  return (
    <div className="relative min-h-full bg-ink-50 text-ink-800">
      <div className="pointer-events-none absolute inset-x-0 top-0 h-72 bg-gradient-to-b from-brand-50 to-transparent" />

      <div className="relative mx-auto max-w-[1100px] space-y-8 px-4 py-8 sm:px-8 sm:py-10">
        <div className="space-y-4">
          <TopicsHeader />
          <div className="space-y-3 animate-fade-up [animation-delay:60ms]">
            <TopicSearchBar value={f.query} onChange={f.setQuery} />
            <CategoryFilter value={f.category} onChange={f.setCategory} />
          </div>
        </div>

        <ContinueLearningStrip topics={inProgress} />

        <section className="space-y-4 animate-fade-up [animation-delay:180ms]">
          <SectionHeader title="Explore Topics" right="Your next conversation starts here" className="" />
          <ExploreToolbar
            status={f.status}
            onStatusChange={f.setStatus}
            counts={counts}
            level={f.level}
            onLevelChange={f.setLevel}
            sort={f.sort}
            onSortChange={f.setSort}
          />
          <div className="flex items-center justify-between text-[11px]">
            <span className="font-semibold text-brand-700">
              {loading ? "Loading topics…" : `${f.filtered.length} topic${f.filtered.length === 1 ? "" : "s"} to explore`}
            </span>
            {f.sort === "recommended" && (
              <span className="text-ink-400">Picked with your {userLevel} level in mind</span>
            )}
          </div>
          <ExploreTopicGrid topics={f.filtered} loading={loading} onReset={f.reset} />
        </section>

        <FooterNote />
      </div>
    </div>
  );
}
