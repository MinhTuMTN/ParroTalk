import React from "react";
import Link from "next/link";
import { Flame, ChevronRight } from "lucide-react";
import type { StreakInfo } from "./types";

interface StreakBannerProps {
  streak?: StreakInfo | null;
  onViewProgress?: () => void;
  href?: string;
  className?: string;
}

export default function StreakBanner({
  streak,
  onViewProgress,
  href = "/profile",
  className = "",
}: StreakBannerProps) {
  const currentStreak = streak?.currentStreak ?? 0;
  const hasStudiedToday = streak?.hasStudiedToday ?? false;

  const title = currentStreak > 0
    ? `${currentStreak} Day Streak!`
    : "Bắt đầu chuỗi học tập!";

  const motivationalText = currentStreak > 0
    ? (hasStudiedToday
        ? "Tuyệt vời! Bạn đã duy trì chuỗi luyện tập hôm nay."
        : "You're on fire! Keep practicing to reach your goal.")
    : "Luyện tập ít nhất 1 bài học mỗi ngày để xây dựng thói quen đều đặn.";

  return (
    <div className={`max-w-6xl mx-auto w-full px-4 md:px-8 ${className}`}>
      <div className="bg-[#FFF4ED] hover:bg-[#FFEFE5] transition-colors rounded-2xl p-4 md:p-4.5 flex items-center justify-between border border-orange-200/70 shadow-xs">
        <div className="flex items-center gap-3.5 sm:gap-4">
          <div className="w-11 h-11 rounded-full bg-white flex items-center justify-center text-orange-500 shadow-xs border border-orange-100 shrink-0 relative">
            <Flame className="w-6 h-6 fill-orange-500 text-orange-500 animate-pulse" />
            <span className="absolute -top-0.5 -right-0.5 flex h-2.5 w-2.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-orange-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-orange-500"></span>
            </span>
          </div>
          <div className="min-w-0">
            <div className="font-extrabold text-orange-950 text-sm sm:text-base flex items-center gap-2">
              <span>{title}</span>
              {hasStudiedToday && (
                <span className="text-[10px] font-bold px-2 py-0.5 bg-emerald-100 text-emerald-800 rounded-full">
                  Đã học hôm nay
                </span>
              )}
            </div>
            <div className="text-xs sm:text-sm text-orange-900/80 line-clamp-1 mt-0.5">
              {motivationalText}
            </div>
          </div>
        </div>

        <div className="shrink-0 ml-3">
          {onViewProgress ? (
            <button
              type="button"
              onClick={onViewProgress}
              className="text-xs sm:text-sm font-bold text-orange-600 hover:text-orange-700 hover:underline flex items-center gap-1 cursor-pointer transition-colors"
            >
              <span>View Progress</span>
              <ChevronRight className="w-4 h-4" />
            </button>
          ) : (
            <Link
              href={href}
              className="text-xs sm:text-sm font-bold text-orange-600 hover:text-orange-700 hover:underline flex items-center gap-1 transition-colors"
            >
              <span>View Progress</span>
              <ChevronRight className="w-4 h-4" />
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}
