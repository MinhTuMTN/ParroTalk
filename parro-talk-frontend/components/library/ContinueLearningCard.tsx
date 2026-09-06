import React from "react";
import Link from "next/link";
import { Play, Clock, Sparkles } from "lucide-react";
import type { ContinueLearningItem } from "./types";

export interface ContinueLearningCardProps {
  item: ContinueLearningItem;
  onSelect?: (item: ContinueLearningItem) => void;
  className?: string;
}

export default function ContinueLearningCard({
  item,
  onSelect,
  className = "",
}: ContinueLearningCardProps) {
  const {
    id,
    title,
    source,
    durationMinutes,
    progressPercent,
    thumbnailUrl,
  } = item;

  const clampedProgress = Math.min(100, Math.max(0, Math.round(progressPercent)));

  const getSourceBadgeStyle = (src: string) => {
    const normalized = (src || "").toUpperCase().trim();
    switch (normalized) {
      case "BBC":
        return "bg-red-600 text-white font-extrabold";
      case "6 MIN":
      case "6 MINUTE":
        return "bg-orange-100 text-orange-800 font-bold border border-orange-200/60";
      case "TED":
        return "bg-rose-600 text-white font-extrabold";
      case "VOA":
        return "bg-blue-600 text-white font-bold";
      default:
        return "bg-emerald-100 text-emerald-800 font-bold border border-emerald-200/60";
    }
  };

  const formattedDuration = durationMinutes > 0 ? `${durationMinutes} min` : "5 min";

  const cardContent = (
    <div
      className={`bg-white rounded-2xl border border-slate-200/70 overflow-hidden shadow-xs hover:shadow-md hover:border-emerald-300 transition-all duration-300 flex flex-col group cursor-pointer h-full ${className}`}
      onClick={() => onSelect?.(item)}
    >
      {/* Cover Header (h-32 relative) */}
      <div className="h-32 relative overflow-hidden bg-slate-900 flex items-center justify-center shrink-0">
        {thumbnailUrl ? (
          <img
            src={thumbnailUrl}
            alt={title}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
          />
        ) : (
          <div className="w-full h-full bg-gradient-to-br from-emerald-700 via-emerald-800 to-teal-900 p-4 flex flex-col justify-between relative">
            <div className="flex justify-between items-center text-white/40 text-[10px] font-black uppercase tracking-wider">
              <span>ParroTalk Practice</span>
              <Sparkles className="w-3.5 h-3.5" />
            </div>
            <div className="text-white/80 font-bold text-xs line-clamp-1">
              {source}
            </div>
          </div>
        )}

        {/* Dark subtle vignette on thumbnail */}
        {thumbnailUrl && (
          <div className="absolute inset-0 bg-black/20 group-hover:bg-black/10 transition-colors" />
        )}

        {/* Center Play Button Overlay */}
        <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
          <div className="w-11 h-11 sm:w-12 sm:h-12 rounded-full bg-white/95 text-emerald-700 flex items-center justify-center group-hover:scale-110 group-hover:bg-white shadow-md transition-all duration-300">
            <Play className="w-5 h-5 fill-emerald-700 ml-0.5" />
          </div>
        </div>
      </div>

      {/* Card Body */}
      <div className="p-4 flex flex-col flex-1 justify-between gap-3">
        <div>
          {/* Tag Row: Source Badge & Duration */}
          <div className="flex items-center justify-between gap-2 mb-2 text-xs">
            <span
              className={`text-[10px] px-2 py-0.5 rounded uppercase tracking-wide shrink-0 ${getSourceBadgeStyle(
                source
              )}`}
            >
              {source}
            </span>
            <span className="text-slate-500 font-medium flex items-center gap-1 text-xs shrink-0">
              <Clock className="w-3.5 h-3.5 text-slate-400" />
              {formattedDuration}
            </span>
          </div>

          {/* Lesson Title */}
          <h3 className="font-bold text-sm text-slate-900 leading-snug group-hover:text-emerald-700 transition-colors line-clamp-2">
            {title}
          </h3>
        </div>

        {/* Progress Tracker */}
        <div className="pt-1 mt-auto">
          <div className="flex items-center justify-between text-xs text-slate-500 mb-1.5">
            <span className="font-medium">Đã hoàn thành</span>
            <span className="font-bold text-emerald-700">{clampedProgress}%</span>
          </div>
          <div className="w-full bg-slate-100 rounded-full h-1.5 overflow-hidden">
            <div
              className="bg-emerald-600 h-full rounded-full transition-all duration-500"
              style={{ width: `${clampedProgress}%` }}
            />
          </div>
        </div>
      </div>
    </div>
  );

  if (onSelect) {
    return cardContent;
  }

  return (
    <Link prefetch={false} href={`/practice/${id}`} className="block h-full">
      {cardContent}
    </Link>
  );
}
