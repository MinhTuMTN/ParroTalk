import { Flame } from "lucide-react";

type CurrentStreakCardProps = {
  currentStreak: number;
};

export default function CurrentStreakCard({ currentStreak }: CurrentStreakCardProps) {
  return (
    <section className="rounded-3xl border border-gray-100 bg-white p-6 shadow-sm">
      <div className="flex h-full items-center gap-5">
        <div className="flex h-32 w-32 shrink-0 items-center justify-center rounded-full bg-green-50 text-orange-500">
          <Flame size={68} fill="currentColor" strokeWidth={1.5} />
        </div>

        <div className="min-w-0">
          <p className="text-sm font-bold text-slate-900">Current Streak</p>
          <div className="mt-3 flex items-end gap-2">
            <strong className="text-5xl font-black leading-none text-green-600">{currentStreak}</strong>
            <span className="pb-1 text-sm font-bold text-slate-900">days</span>
          </div>
          <span className="mt-4 inline-flex rounded-full border border-green-100 bg-green-50 px-3 py-1 text-xs font-bold text-green-700">
            You&apos;re on fire! 🔥
          </span>
          <p className="mt-4 max-w-[180px] text-sm leading-6 text-slate-500">Study every day to keep your streak alive.</p>
        </div>
      </div>
    </section>
  );
}
