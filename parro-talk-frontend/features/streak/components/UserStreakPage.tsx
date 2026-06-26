"use client";

import { useCallback, useEffect, useState } from "react";
import CurrentStreakCard from "@/features/streak/components/CurrentStreakCard";
import LongestStreakCard from "@/features/streak/components/LongestStreakCard";
import LearningCalendar from "@/features/streak/components/LearningCalendar";
import StatisticsGrid from "@/features/streak/components/StatisticsGrid";
import AchievementList from "@/features/streak/components/AchievementList";
import MotivationCard from "@/features/streak/components/MotivationCard";
import StreakHeader from "@/features/streak/components/StreakHeader";
import StreakSkeleton from "@/features/streak/components/StreakSkeleton";
import { streakService } from "@/features/streak/services/streakService";
import type { UserStreakResponse } from "@/features/streak/types/streak";

export default function UserStreakPage() {
  const [data, setData] = useState<UserStreakResponse | null>(null);
  const [selectedYear, setSelectedYear] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const loadStreak = useCallback(async (year?: number) => {
    setLoading(true);
    setError(false);
    try {
      const response = await streakService.getMyStreak(year);
      setData(response);
      setSelectedYear(response.calendarYear);
    } catch (loadError) {
      console.error("Failed to load streak", loadError);
      setError(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadStreak();
  }, [loadStreak]);

  const handleYearChange = (year: number) => {
    setSelectedYear(year);
    loadStreak(year);
  };

  return (
    <div className="min-h-screen bg-[#FCFDFC] ">
      <StreakHeader />

      <main className="mx-auto flex w-full max-w-7xl flex-col gap-5 px-4 py-6 md:px-8 md:py-8">
        {loading ? (
          <StreakSkeleton />
        ) : error || !data ? (
          <div className="rounded-3xl border border-gray-100 bg-white p-8 text-center shadow-sm">
            <h2 className="text-xl font-black text-slate-900">Could not load your streak</h2>
            <p className="mt-2 text-sm text-slate-500">Please try again in a moment.</p>
            <button
              type="button"
              onClick={() => loadStreak(selectedYear ?? undefined)}
              className="mt-5 rounded-2xl bg-green-600 px-5 py-2.5 text-sm font-bold text-white transition hover:bg-green-700"
            >
              Retry
            </button>
          </div>
        ) : (
          <>
            <div className="grid gap-5 xl:grid-cols-[1.15fr_0.82fr_1.25fr]">
              <CurrentStreakCard currentStreak={data.currentStreak} />
              <LongestStreakCard
                longestStreak={data.longestStreak}
                achievedAt={data.longestStreakAchievedAt}
              />
              <StatisticsGrid statistics={data.statistics} />
            </div>

            <div>
              <LearningCalendar
                days={data.calendar}
                selectedYear={selectedYear ?? data.calendarYear}
                availableYears={data.calendarYears}
                onYearChange={handleYearChange}
              />
            </div>

            <div className="grid gap-5 xl:grid-cols-[1.25fr_1fr]">
              <AchievementList achievements={data.achievements} />
              <MotivationCard
                motivation={data.motivation}
                hasStudiedToday={data.hasStudiedToday}
                totalStudyDays={data.statistics.totalStudyDays}
              />
            </div>
          </>
        )}
      </main>

    </div>
  );
}
