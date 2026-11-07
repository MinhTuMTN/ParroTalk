"use client";

import { useAuth } from "@/features/auth/hooks/useAuth";
import { useTopicsWithProgress, useVocabularyStats } from "@/features/vocabulary/hooks/useTopicsWithProgress";
import {
  DashboardHeader,
  ContinueLearningHero,
  TodayReviewCard,
  ProgressOverview,
  MyTopicsList,
  RecommendedTopics,
  NextStepActions,
} from "@/features/vocabulary/components/dashboard";
import { FooterNote } from "@/features/vocabulary/components/shared";

export default function VocabularyDashboard() {
  const { user } = useAuth();
  const { topics, loading } = useTopicsWithProgress();
  const stats = useVocabularyStats();

  // Vietnamese names put the given name last.
  const firstName = user?.fullName?.trim().split(/\s+/).pop();

  const started = topics.filter((t) => t.status !== "not-started");
  const currentTopic = topics.find((t) => t.isCurrent) ?? started[0];
  const myTopics = started.slice(0, 3);
  const recommended = topics.filter((t) => t.status === "not-started").slice(0, 3);

  return (
    <div className="relative min-h-full bg-ink-50 text-ink-800">
      {/* soft page-level glow */}
      <div className="pointer-events-none absolute inset-x-0 top-0 h-72 bg-gradient-to-b from-brand-50 to-transparent" />

      <div className="relative mx-auto max-w-[1100px] space-y-8 px-4 py-8 sm:px-8 sm:py-10">
        <DashboardHeader name={firstName} streak={stats.streak} />

        <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
          <ContinueLearningHero topic={currentTopic} loading={loading} />
          <TodayReviewCard count={stats.reviewCount} minutes={stats.reviewMinutes} />
        </div>

        <ProgressOverview
          totalLearned={stats.totalLearned}
          learning={stats.learning}
          mastered={stats.mastered}
          weeklyGain={stats.weeklyGain}
          wordGoal={stats.wordGoal}
        />

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <MyTopicsList topics={myTopics} loading={loading} />
          <RecommendedTopics topics={recommended} loading={loading} />
        </div>

        <NextStepActions />
        <FooterNote />
      </div>
    </div>
  );
}
