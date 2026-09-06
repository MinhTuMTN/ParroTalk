import React from "react";

export function HeroSkeleton() {
  return (
    <div className="w-full bg-[#F0FDF4] py-8 border-b border-emerald-100/60">
      <div className="max-w-6xl mx-auto w-full px-4 md:px-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
        <div className="space-y-3">
          <div className="h-9 w-40 bg-emerald-200/60 rounded-xl animate-pulse" />
          <div className="h-5 w-64 bg-emerald-100/80 rounded-lg animate-pulse" />
        </div>
        <div className="w-full md:w-[420px] space-y-3">
          <div className="h-12 w-full bg-white/90 rounded-full border border-emerald-200/50 shadow-xs animate-pulse" />
          <div className="flex gap-2 overflow-hidden">
            {[88, 104, 76, 120].map((width, idx) => (
              <div
                key={idx}
                className="h-7 bg-white/80 rounded-full animate-pulse shrink-0 border border-emerald-100"
                style={{ width: `${width}px` }}
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

export function StreakBannerSkeleton() {
  return (
    <div className="max-w-6xl mx-auto w-full px-4 md:px-8">
      <div className="bg-orange-50/80 border border-orange-200/60 rounded-2xl p-4 flex items-center justify-between shadow-xs">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-white shadow-xs animate-pulse" />
          <div className="space-y-2">
            <div className="h-4 w-28 bg-orange-200/80 rounded animate-pulse" />
            <div className="h-3 w-56 bg-orange-100 rounded animate-pulse" />
          </div>
        </div>
        <div className="h-4 w-20 bg-orange-200/60 rounded animate-pulse" />
      </div>
    </div>
  );
}

export function ContinueCardSkeleton() {
  return (
    <div className="bg-white rounded-2xl border border-gray-200/70 overflow-hidden shadow-xs flex flex-col">
      <div className="h-32 bg-emerald-100/50 animate-pulse relative flex items-center justify-center">
        <div className="w-12 h-12 rounded-full bg-white/80 shadow-xs" />
      </div>
      <div className="p-4 flex flex-col flex-1 gap-3">
        <div className="flex items-center gap-2">
          <div className="h-4 w-10 bg-gray-200 rounded animate-pulse" />
          <div className="h-3 w-14 bg-gray-200 rounded animate-pulse" />
        </div>
        <div className="space-y-1.5">
          <div className="h-4 w-full bg-gray-200 rounded animate-pulse" />
          <div className="h-4 w-3/4 bg-gray-200 rounded animate-pulse" />
        </div>
        <div className="mt-auto pt-2 space-y-1.5">
          <div className="flex justify-between">
            <div className="h-3 w-16 bg-gray-200 rounded animate-pulse" />
            <div className="h-3 w-8 bg-gray-200 rounded animate-pulse" />
          </div>
          <div className="w-full bg-gray-100 rounded-full h-1.5 overflow-hidden">
            <div className="h-full bg-emerald-200 rounded-full w-1/2 animate-pulse" />
          </div>
        </div>
      </div>
    </div>
  );
}

export function UploadedCardSkeleton() {
  return (
    <div className="bg-white rounded-2xl border border-gray-200/70 overflow-hidden shadow-xs flex flex-col">
      <div className="h-32 bg-emerald-50 relative flex items-center justify-center animate-pulse">
        <div className="absolute top-2 right-2 h-5 w-16 bg-white/90 rounded-full" />
        <div className="w-12 h-12 rounded-full bg-white shadow-xs" />
      </div>
      <div className="p-4 space-y-2">
        <div className="flex items-center gap-2">
          <div className="h-4 w-12 bg-emerald-100 rounded animate-pulse" />
          <div className="h-3 w-14 bg-gray-200 rounded animate-pulse" />
        </div>
        <div className="h-4 w-full bg-gray-200 rounded animate-pulse" />
        <div className="h-4 w-2/3 bg-gray-200 rounded animate-pulse" />
      </div>
    </div>
  );
}

export function ShelfCardSkeleton() {
  return (
    <div className="bg-white rounded-xl overflow-hidden border border-gray-200/70 shadow-xs flex flex-col">
      <div className="h-32 bg-gray-200/70 p-4 relative flex items-center justify-center animate-pulse">
        <div className="absolute top-2 left-2 h-4 w-10 bg-white/70 rounded" />
        <div className="absolute top-2 right-2 h-4 w-8 bg-white/50 rounded" />
        <div className="h-4 w-3/4 bg-white/60 rounded mt-4" />
      </div>
      <div className="p-3 bg-white flex items-center justify-between mt-auto">
        <div className="h-3 w-20 bg-gray-200 rounded animate-pulse" />
      </div>
    </div>
  );
}

export default function LibrarySkeleton() {
  return (
    <div className="w-full flex flex-col space-y-8 pb-20 bg-[#F8FAF9]">
      <HeroSkeleton />
      <StreakBannerSkeleton />

      {/* Continue Learning Shelf Skeleton */}
      <section className="max-w-6xl mx-auto w-full px-4 md:px-8 space-y-4">
        <div className="flex justify-between items-center">
          <div className="h-6 w-48 bg-gray-200 rounded-lg animate-pulse" />
          <div className="h-4 w-16 bg-gray-200 rounded animate-pulse" />
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <ContinueCardSkeleton key={i} />
          ))}
        </div>
      </section>

      {/* Uploaded Lessons Shelf Skeleton */}
      <section className="max-w-6xl mx-auto w-full px-4 md:px-8 space-y-4">
        <div className="flex justify-between items-center">
          <div className="h-6 w-52 bg-gray-200 rounded-lg animate-pulse" />
          <div className="h-4 w-16 bg-gray-200 rounded animate-pulse" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {[1, 2, 3].map((i) => (
            <UploadedCardSkeleton key={i} />
          ))}
        </div>
      </section>

      {/* Curated Shelves Skeletons */}
      {[1, 2].map((shelfIdx) => (
        <section key={shelfIdx} className="max-w-6xl mx-auto w-full px-4 md:px-8 space-y-4">
          <div className="flex justify-between items-center">
            <div className="h-6 w-44 bg-gray-200 rounded-lg animate-pulse" />
            <div className="h-4 w-16 bg-gray-200 rounded animate-pulse" />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {[1, 2, 3, 4].map((cardIdx) => (
              <ShelfCardSkeleton key={cardIdx} />
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}
