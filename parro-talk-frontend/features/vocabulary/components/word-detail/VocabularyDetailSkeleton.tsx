import React from "react";

export function VocabularyDetailSkeleton() {
  return (
    <div className="space-y-8 animate-fade-in">
      {/* ── Top Navigation Skeleton ── */}
      <div>
        <div className="h-4 w-36 bg-ink-200/60 rounded-lg animate-pulse" />
      </div>

      {/* ── Hero Card Skeleton ── */}
      <section className="relative overflow-hidden rounded-3xl border border-brand-200/70 bg-brand-50/50 p-6 sm:p-8 shadow-card animate-pulse">
        <div className="flex flex-col md:flex-row md:items-start justify-between gap-6">
          {/* Left content skeleton */}
          <div className="flex-1 space-y-3.5">
            <div className="h-3 w-40 bg-brand-200/70 rounded-full" />

            <div className="flex items-center gap-3">
              <div className="h-11 w-44 bg-brand-200/80 rounded-2xl" />
              <div className="h-6 w-14 bg-brand-200/60 rounded-full" />
            </div>

            <div className="h-4 w-60 bg-brand-200/50 rounded-lg" />

            <div className="h-5 w-72 bg-brand-200/70 rounded-lg" />

            {/* Audio buttons skeleton */}
            <div className="flex items-center gap-2.5 pt-1">
              <div className="h-8 w-28 bg-white/90 rounded-xl border border-brand-200/50 shadow-2xs" />
              <div className="h-8 w-28 bg-white/90 rounded-xl border border-brand-200/50 shadow-2xs" />
            </div>

            {/* Topics row skeleton */}
            <div className="flex items-center gap-2 pt-1.5">
              {[80, 72, 96, 76].map((w, idx) => (
                <div
                  key={idx}
                  className="h-6 bg-white/80 rounded-lg border border-brand-200/40 shadow-2xs"
                  style={{ width: `${w}px` }}
                />
              ))}
            </div>
          </div>

          {/* Right action box skeleton */}
          <div className="flex flex-col items-center md:items-end justify-center shrink-0 space-y-2.5 pt-2 md:pt-0">
            <div className="h-10 w-36 bg-brand-300/80 rounded-xl shadow-sm" />
            <div className="h-4 w-28 bg-brand-200/60 rounded-lg" />
            <div className="h-7 w-48 bg-brand-100/60 rounded-lg" />
          </div>
        </div>
      </section>

      {/* ── Main Two-Column Layout Skeleton ── */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Left Column (~70%): Senses, Phrasal verbs, Idioms */}
        <div className="lg:col-span-8 space-y-6">
          {/* Section header skeleton */}
          <div className="flex items-center justify-between">
            <div className="h-6 w-44 bg-ink-200/80 rounded-lg animate-pulse" />
            <div className="h-4 w-20 bg-ink-200/50 rounded animate-pulse" />
          </div>

          {/* 3 Senses Card Skeletons */}
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="rounded-3xl border border-ink-200/70 bg-white p-6 sm:p-7 shadow-card space-y-4 animate-pulse"
            >
              {/* Top row */}
              <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-xl bg-brand-50 border border-brand-200/60 shrink-0" />
                  <div className="space-y-1.5">
                    <div
                      className="h-5 bg-ink-200/70 rounded-lg"
                      style={{ width: `${140 + i * 40}px` }}
                    />
                    <div className="h-3.5 w-32 bg-ink-100 rounded" />
                  </div>
                </div>
                <div className="h-6 w-20 bg-brand-50 rounded-lg border border-brand-200/60 shrink-0" />
              </div>

              {/* Definitions */}
              <div className="space-y-2 pt-1">
                <div className="h-4 w-full bg-ink-100/80 rounded" />
                <div className="h-3.5 w-2/3 bg-ocean-100/70 rounded" />
                <div className="h-3 w-48 bg-ink-100/50 rounded italic" />
              </div>

              {/* In Context box */}
              <div className="rounded-2xl border border-brand-100/70 bg-brand-50/40 p-4 sm:p-5 space-y-2">
                <div className="flex items-center justify-between">
                  <div className="h-3 w-20 bg-brand-200/70 rounded" />
                  <div className="h-3 w-12 bg-brand-200/50 rounded" />
                </div>
                <div className="h-4 w-full bg-ink-200/50 rounded" />
                <div className="h-3.5 w-3/4 bg-ink-100/70 rounded" />
              </div>

              {/* Collapsible toggle skeleton */}
              <div className="pt-1 border-t border-ink-100/60">
                <div className="h-4 w-36 bg-brand-100/80 rounded-lg" />
              </div>
            </div>
          ))}

          {/* Phrasal verbs card skeleton */}
          <div className="rounded-3xl border border-ink-200/70 bg-white p-6 sm:p-7 shadow-card space-y-3 animate-pulse">
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <div className="h-5 w-32 bg-ink-200/70 rounded-lg" />
                <div className="h-3 w-48 bg-ink-100 rounded" />
              </div>
              <div className="w-5 h-5 bg-ink-100 rounded-full" />
            </div>
          </div>

          {/* Idioms card skeleton */}
          <div className="rounded-3xl border border-ink-200/70 bg-white p-6 sm:p-7 shadow-card space-y-3 animate-pulse">
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <div className="h-5 w-40 bg-ink-200/70 rounded-lg" />
                <div className="h-3 w-52 bg-ink-100 rounded" />
              </div>
              <div className="w-5 h-5 bg-ink-100 rounded-full" />
            </div>
          </div>
        </div>

        {/* Right Column (~30%): Sticky Sidebar Skeleton */}
        <div className="lg:col-span-4">
          <aside className="rounded-3xl border border-ink-200/70 bg-white p-5 sm:p-6 shadow-card space-y-4 sticky top-6 animate-pulse">
            <div className="h-4 w-24 bg-ink-200/70 rounded-lg" />

            <div className="space-y-2">
              {[1, 2, 3, 4, 5, 6].map((idx) => (
                <div
                  key={idx}
                  className="h-8 w-full bg-ink-50/80 rounded-xl border border-ink-100/50"
                />
              ))}
            </div>

            {/* Advice callout box */}
            <div className="rounded-2xl border border-brand-200/50 bg-brand-50/40 p-4 space-y-2">
              <div className="h-4 w-36 bg-brand-200/70 rounded" />
              <div className="h-3 w-full bg-brand-100/80 rounded" />
              <div className="h-3 w-4/5 bg-brand-100/70 rounded" />
            </div>
          </aside>
        </div>
      </div>

      {/* ── Footer Skeleton ── */}
      <div className="pt-8 border-t border-ink-100/80 text-center space-y-2 animate-pulse">
        <div className="h-3 w-96 bg-ink-100/80 rounded mx-auto" />
        <div className="h-4 w-44 bg-brand-100/80 rounded-full mx-auto" />
      </div>
    </div>
  );
}
