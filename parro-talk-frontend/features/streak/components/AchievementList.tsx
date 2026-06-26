"use client";

import { ChevronLeft, ChevronRight, Flame, Lock, MoonStar, SunMedium, Trophy } from "lucide-react";
import { useRef } from "react";
import type { Achievement } from "@/features/streak/types/streak";

type AchievementListProps = {
  achievements: Achievement[];
};

const iconMap = {
  STREAK_7_DAYS: Flame,
  STREAK_30_DAYS: Flame,
  STREAK_100_DAYS: Trophy,
  EARLY_BIRD: SunMedium,
  NIGHT_OWL: MoonStar,
  CONSISTENCY_MASTER: Lock,
} as const;

const formatDate = (value: string | null) => {
  if (!value) {
    return null;
  }

  return new Date(`${value}T00:00:00`).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
};

export default function AchievementList({ achievements }: AchievementListProps) {
  const listRef = useRef<HTMLDivElement>(null);

  const scrollByAmount = (direction: number) => {
    listRef.current?.scrollBy({ left: direction * 240, behavior: "smooth" });
  };

  return (
    <section className="rounded-3xl border border-gray-100 bg-white p-6 shadow-sm">
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-base font-bold text-slate-900">Achievements</h2>
        <div className="hidden items-center gap-2 md:flex">
          <button
            type="button"
            onClick={() => scrollByAmount(-1)}
            className="rounded-xl border border-gray-100 p-2 text-gray-400 transition hover:text-green-600"
            aria-label="Scroll achievements left"
          >
            <ChevronLeft size={18} />
          </button>
          <button
            type="button"
            onClick={() => scrollByAmount(1)}
            className="rounded-xl border border-gray-100 p-2 text-gray-400 transition hover:text-green-600"
            aria-label="Scroll achievements right"
          >
            <ChevronRight size={18} />
          </button>
        </div>
      </div>

      <div
        ref={listRef}
        className="mt-5 flex gap-4 overflow-x-auto pb-2 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      >
        {achievements.map((achievement) => {
          const Icon = iconMap[achievement.code as keyof typeof iconMap] ?? Lock;
          const achievedAt = formatDate(achievement.achievedAt);
          return (
            <div key={achievement.code} className="w-28 shrink-0 text-center">
              <div
                className={`mx-auto flex h-20 w-20 items-center justify-center ${
                  achievement.achieved
                    ? "bg-orange-50 text-orange-500"
                    : achievement.code === "EARLY_BIRD"
                      ? "bg-purple-50 text-purple-500"
                      : achievement.code === "NIGHT_OWL"
                        ? "bg-blue-50 text-blue-500"
                        : "bg-slate-50 text-slate-500"
                }`}
                style={{
                  clipPath: "polygon(25% 6%, 75% 6%, 100% 50%, 75% 94%, 25% 94%, 0% 50%)",
                }}
              >
                <Icon size={30} />
              </div>
              <p className="mt-3 min-h-10 text-sm font-bold leading-5 text-slate-900">{achievement.title}</p>
              <p className={`mt-1 text-xs font-medium ${achievement.achieved ? "text-green-600" : "text-slate-500"}`}>
                {achievement.achieved ? "Achieved" : "Not achieved"}
              </p>
              <p className="mt-2 min-h-4 text-xs text-slate-500">
                {achievedAt ??
                  (achievement.progress !== null && achievement.target !== null
                    ? `${achievement.progress} / ${achievement.target} days`
                    : "—")}
              </p>
              {!achievement.achieved && achievement.progress !== null && achievement.target !== null ? (
                <div className="mt-2 h-1 rounded-full bg-gray-100">
                  <div
                    className="h-full rounded-full bg-green-500"
                    style={{ width: `${Math.min((achievement.progress / achievement.target) * 100, 100)}%` }}
                  />
                </div>
              ) : null}
            </div>
          );
        })}
      </div>
    </section>
  );
}
