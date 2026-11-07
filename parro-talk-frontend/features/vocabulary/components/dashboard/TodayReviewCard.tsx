import Link from "next/link";
import { ArrowRight, Clock, RotateCw } from "lucide-react";

interface Props {
  count: number;
  minutes: number;
}

export function TodayReviewCard({ count, minutes }: Props) {
  return (
    <section className="relative flex flex-col overflow-hidden rounded-3xl border border-ink-200/70 bg-white p-6 shadow-card animate-fade-up [animation-delay:120ms]">
      <div className="pointer-events-none absolute -right-10 -top-10 h-32 w-32 rounded-full bg-brand-50" />

      <div className="relative flex items-center justify-between">
        <h2 className="text-base font-bold text-ink-900">Today&apos;s Review</h2>
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-100 text-brand-600">
          <RotateCw className="h-4 w-4" />
        </div>
      </div>

      <div className="relative mt-5 flex items-baseline gap-2">
        <span className="bg-gradient-to-br from-ink-900 to-brand-700 bg-clip-text text-[44px] font-extrabold leading-none text-transparent">
          {count}
        </span>
        <span className="text-sm font-medium text-ink-600">words ready</span>
      </div>
      <p className="mt-3 max-w-[220px] text-sm text-ink-500">
        A quick refresh today helps your words stay with you.
      </p>
      <div className="mt-3 inline-flex w-fit items-center gap-1.5 rounded-full bg-ink-50 px-2.5 py-1 text-[11px] font-medium text-ink-500">
        <Clock className="h-3.5 w-3.5" />
        About {minutes} minutes
      </div>

      <div className="mt-auto pt-6">
        <Link
          href="/vocabulary/review"
          className="group flex items-center justify-center gap-2 rounded-xl border border-brand-200 bg-brand-50/50 py-2.5 text-sm font-semibold text-brand-700 transition-all hover:border-brand-600 hover:bg-brand-600 hover:text-white hover:shadow-brand"
        >
          Start review
          <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
        </Link>
      </div>
    </section>
  );
}
