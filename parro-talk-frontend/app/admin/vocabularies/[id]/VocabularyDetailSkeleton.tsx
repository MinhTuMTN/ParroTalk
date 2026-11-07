"use client";

import React from "react";

export default function VocabularyDetailSkeleton() {
  return (
    <div className="p-6 max-w-6xl mx-auto space-y-6 pb-24 relative animate-pulse">
      {/* Top back bar skeleton */}
      <div className="flex items-center gap-4 mb-2">
        <div className="w-10 h-10 rounded-xl bg-slate-200/80" />
        <div className="h-4 w-36 rounded-md bg-slate-200/80" />
      </div>

      {/* Header card skeleton */}
      <div className="bg-slate-50 rounded-[2.5rem] p-8 border border-slate-100 shadow-sm">
        <div className="flex flex-col lg:flex-row justify-between items-start gap-8">
          <div className="space-y-5 flex-1">
            {/* Word & meaning */}
            <div className="flex flex-wrap items-end gap-4">
              <div className="h-12 w-64 bg-slate-200 rounded-2xl" />
              <div className="h-8 w-44 bg-orange-100/80 rounded-xl" />
              <div className="h-6 w-28 bg-orange-100/60 rounded-md mb-1" />
            </div>

            {/* Pronunciations badges */}
            <div className="flex flex-wrap items-center gap-4 pt-1">
              <div className="h-10 w-44 bg-slate-200/80 rounded-xl" />
              <div className="h-10 w-44 bg-slate-200/80 rounded-xl" />
            </div>

            {/* CEFR Tiers */}
            <div className="flex items-center gap-3 pt-2">
              <div className="h-5 w-24 bg-slate-200/60 rounded-md" />
              <div className="h-7 w-24 bg-slate-200 rounded-lg" />
              <div className="h-7 w-20 bg-slate-200 rounded-lg" />
              <div className="h-7 w-20 bg-slate-200 rounded-lg" />
            </div>
          </div>

          {/* Completeness Card */}
          <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 w-full lg:w-80 shrink-0 space-y-4">
            <div className="flex justify-between items-center">
              <div className="h-4 w-36 bg-slate-200 rounded-md" />
              <div className="h-4 w-10 bg-emerald-100 rounded-md" />
            </div>
            <div className="h-2.5 bg-slate-100 rounded-full overflow-hidden">
              <div className="h-full bg-slate-200 rounded-full w-3/4" />
            </div>
            <div className="grid grid-cols-3 gap-2 pt-2">
              <div className="h-16 bg-slate-50 rounded-2xl border border-slate-100" />
              <div className="h-16 bg-slate-50 rounded-2xl border border-slate-100" />
              <div className="h-16 bg-emerald-50/60 rounded-2xl border border-emerald-100/50" />
            </div>
          </div>
        </div>
      </div>

      {/* Senses section skeleton */}
      <div className="mt-8 space-y-6">
        <div className="flex justify-between items-center px-2">
          <div className="h-7 w-32 bg-slate-200 rounded-xl" />
          <div className="h-9 w-36 bg-slate-200 rounded-xl" />
        </div>

        {/* 2 mock sense cards */}
        {[1, 2].map((i) => (
          <div
            key={i}
            className="bg-white rounded-[2rem] p-6 shadow-sm border border-slate-100 space-y-5"
          >
            <div className="flex justify-between items-center">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-lg bg-indigo-100/80" />
                <div className="w-20 h-7 rounded-lg bg-orange-100/80" />
                <div className="w-32 h-4 rounded bg-slate-200/70" />
              </div>
              <div className="w-16 h-8 rounded-xl bg-slate-100" />
            </div>

            <div className="space-y-3">
              <div className="h-14 bg-indigo-50/50 rounded-2xl border border-indigo-50/60" />
              <div className="h-14 bg-rose-50/30 rounded-2xl border border-rose-50/50" />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div className="h-28 bg-slate-50 rounded-2xl border border-slate-100" />
              <div className="h-28 bg-slate-50/60 rounded-2xl border border-slate-100" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
