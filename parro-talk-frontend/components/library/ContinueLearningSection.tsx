import React from "react";
import Link from "next/link";
import { PlayCircle, ChevronRight } from "lucide-react";
import type { ContinueLearningItem } from "./types";
import ContinueLearningCard from "./ContinueLearningCard";

export interface ContinueLearningSectionProps {
  items: ContinueLearningItem[];
  title?: string;
  seeAllHref?: string;
  onSeeAll?: () => void;
  onSelectLesson?: (item: ContinueLearningItem) => void;
  className?: string;
}

export default function ContinueLearningSection({
  items,
  title = "Tiếp tục học",
  seeAllHref,
  onSeeAll,
  onSelectLesson,
  className = "",
}: ContinueLearningSectionProps) {
  if (!items || items.length === 0) {
    return null;
  }

  return (
    <section className={`max-w-6xl mx-auto w-full px-4 md:px-8 space-y-4 ${className}`}>
      {/* Section Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-emerald-100 flex items-center justify-center text-emerald-700 shadow-2xs">
            <PlayCircle className="w-5 h-5" />
          </div>
          <h2 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
            {title}
          </h2>
          <span className="bg-emerald-100/80 text-emerald-800 font-bold text-xs px-2.5 py-0.5 rounded-full">
            {items.length} bài
          </span>
        </div>

        {/* See All Action */}
        {seeAllHref ? (
          <Link
            href={seeAllHref}
            className="text-xs sm:text-sm font-bold text-emerald-700 hover:text-emerald-800 flex items-center gap-1 hover:underline transition-colors"
          >
            <span>Xem tất cả</span>
            <ChevronRight className="w-4 h-4" />
          </Link>
        ) : onSeeAll ? (
          <button
            type="button"
            onClick={onSeeAll}
            className="text-xs sm:text-sm font-bold text-emerald-700 hover:text-emerald-800 flex items-center gap-1 hover:underline cursor-pointer transition-colors"
          >
            <span>Xem tất cả</span>
            <ChevronRight className="w-4 h-4" />
          </button>
        ) : null}
      </div>

      {/* Responsive Grid (4 columns) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
        {items.map((item) => (
          <ContinueLearningCard
            key={item.id}
            item={item}
            onSelect={onSelectLesson}
          />
        ))}
      </div>
    </section>
  );
}
