import { Trophy } from "lucide-react";

type LongestStreakCardProps = {
  longestStreak: number;
  achievedAt: string | null;
};

const formatDate = (value: string | null) => {
  if (!value) {
    return "No streak yet";
  }

  return new Date(`${value}T00:00:00`).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
};

export default function LongestStreakCard({ longestStreak, achievedAt }: LongestStreakCardProps) {
  return (
    <section className="rounded-3xl border border-gray-100 bg-white p-6 shadow-sm">
      <div className="flex h-full flex-col justify-between">
        <div className="flex items-center gap-5">
          <div className="flex h-24 w-24 items-center justify-center rounded-full bg-amber-50 text-amber-500">
            <Trophy size={44} fill="currentColor" strokeWidth={1.5} />
          </div>
          <div>
            <p className="text-sm font-bold text-slate-900">Longest Streak</p>
            <div className="mt-3 flex flex-col">
              <strong className="text-5xl font-black leading-none text-green-600">{longestStreak}</strong>
              <span className="mt-2 text-lg font-bold text-slate-900">days</span>
            </div>
          </div>
        </div>
        <p className="mt-5 text-sm text-slate-500">
          {achievedAt ? `Achieved on ${formatDate(achievedAt)}` : "No streak yet"}
        </p>
      </div>
    </section>
  );
}
