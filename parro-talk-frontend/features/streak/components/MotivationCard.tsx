import Link from "next/link";
import { AlertTriangle, Flag, Quote } from "lucide-react";
import type { Motivation } from "@/features/streak/types/streak";

type MotivationCardProps = {
  motivation: Motivation;
  hasStudiedToday: boolean;
  totalStudyDays: number;
};

export default function MotivationCard({
  motivation,
  hasStudiedToday,
  totalStudyDays,
}: MotivationCardProps) {
  return (
    <section className="rounded-3xl border border-gray-100 bg-white p-6 shadow-sm">
      <div className="flex h-full flex-col justify-between gap-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-base font-bold text-slate-900">{motivation.title}</h2>
            <div className="mt-5 flex gap-3">
              <Quote className="mt-1 shrink-0 text-green-600" size={22} />
              <div>
                <p className="max-w-sm text-lg leading-7 text-slate-800">{motivation.quote}</p>
                <p className="mt-3 text-sm text-slate-500">— {motivation.author}</p>
              </div>
            </div>
          </div>

          <div className="hidden text-green-500 sm:block">
            <Flag size={52} />
          </div>
        </div>

        {!hasStudiedToday ? (
          <div className="flex flex-col gap-4 rounded-2xl border border-green-100 bg-green-50/70 p-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="flex items-center gap-2 text-sm font-bold text-slate-900">
                <AlertTriangle size={16} className="text-orange-500" />
                {totalStudyDays === 0 ? "Start your first lesson" : "Don't break your streak! 🔥"}
              </p>
              <p className="mt-2 text-sm text-slate-600">{motivation.warningMessage}</p>
            </div>
            <Link
              href="/library"
              className="inline-flex h-11 shrink-0 items-center justify-center rounded-xl border border-green-500 bg-white px-5 text-sm font-bold text-green-700 transition hover:bg-green-600 hover:text-white"
            >
              {totalStudyDays === 0 ? "Start your first lesson" : "Start Now"}
            </Link>
          </div>
        ) : null}
      </div>
    </section>
  );
}
