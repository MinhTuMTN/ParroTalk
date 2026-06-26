import { Check } from "lucide-react";
import type { WeeklyProgressDay } from "@/features/streak/types/streak";

type WeeklyProgressCardProps = {
  weeklyCompletedDays: number;
  weeklyGoalDays: number;
  days: WeeklyProgressDay[];
};

export default function WeeklyProgressCard({
  weeklyCompletedDays,
  weeklyGoalDays,
  days,
}: WeeklyProgressCardProps) {
  const progressPercent = weeklyGoalDays === 0 ? 0 : Math.round((weeklyCompletedDays / weeklyGoalDays) * 100);
  const remainingDays = Math.max(weeklyGoalDays - weeklyCompletedDays, 0);

  return (
    <section className="rounded-3xl border border-gray-100 bg-white p-6 shadow-sm">
      <h2 className="text-sm font-bold text-slate-900">Weekly Progress</h2>

      <div className="mt-4 flex items-end gap-2">
        <strong className="text-4xl font-black leading-none text-slate-900">{weeklyCompletedDays}</strong>
        <span className="pb-1 text-xl font-bold text-green-600">/ {weeklyGoalDays}</span>
        <span className="pb-1 text-sm text-slate-600">days completed</span>
      </div>

      <div className="mt-4 h-2 rounded-full bg-gray-100">
        <div
          className="h-full rounded-full bg-green-500 transition-all"
          style={{ width: `${progressPercent}%` }}
        />
      </div>

      <div className="mt-6 grid grid-cols-7 gap-2">
        {days.map((day) => (
          <div key={day.date} className="flex flex-col items-center gap-2">
            <div
              className={`flex h-7 w-7 items-center justify-center rounded-full border text-xs font-bold ${
                day.studied
                  ? "border-green-500 bg-green-500 text-white"
                  : "border-gray-200 bg-white text-transparent"
              }`}
            >
              {day.studied ? <Check size={15} strokeWidth={3} /> : "•"}
            </div>
            <span className="text-[11px] font-medium text-slate-600">{day.day}</span>
          </div>
        ))}
      </div>

      <p className="mt-5 text-sm leading-6 text-slate-600">
        {remainingDays === 0
          ? "Weekly goal complete. Keep the rhythm going."
          : `Keep it up! Complete ${remainingDays} more ${remainingDays === 1 ? "day" : "days"} to reach your weekly goal.`}
      </p>
    </section>
  );
}
