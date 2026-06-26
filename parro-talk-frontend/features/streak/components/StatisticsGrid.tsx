import { CalendarDays, Clock3, TrendingUp, BookOpen } from "lucide-react";
import type { StreakStatistics } from "@/features/streak/types/streak";

type StatisticsGridProps = {
  statistics: StreakStatistics;
};

const stats = [
  {
    key: "totalStudyDays",
    label: "Total Study Days",
    icon: CalendarDays,
    suffix: "days",
  },
  {
    key: "totalStudyMinutes",
    label: "Total Study Time",
    icon: Clock3,
    suffix: "min",
  },
  {
    key: "lessonsCompleted",
    label: "Lessons Completed",
    icon: BookOpen,
    suffix: "",
  },
  {
    key: "averageDailyStudyMinutes",
    label: "Avg. Daily Study Time",
    icon: TrendingUp,
    suffix: "min",
  },
] as const;

export default function StatisticsGrid({ statistics }: StatisticsGridProps) {
  return (
    <section className="rounded-3xl border border-gray-100 bg-white p-6 shadow-sm">
      <h2 className="text-base font-bold text-slate-900">Statistics</h2>

      <div className="mt-5 grid gap-3 md:grid-cols-2">
        {stats.map((stat) => {
          const Icon = stat.icon;
          const value = statistics[stat.key];
          return (
            <div key={stat.key} className="flex items-center gap-4 rounded-2xl border border-gray-100 p-4">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-green-50 text-green-600">
                <Icon size={22} />
              </div>
              <div className="min-w-0">
                <p className="text-sm text-slate-500">{stat.label}</p>
                <div className="mt-1 flex items-end gap-1.5">
                  <strong className="text-2xl font-black leading-none text-slate-900">{value}</strong>
                  {stat.suffix ? <span className="text-sm font-medium text-slate-600">{stat.suffix}</span> : null}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
