import React, { useState, useEffect } from "react";
import Link from "next/link";
import { ArrowRight, Clock, Play } from "lucide-react";
import type { TopicLessonItem, TopicGradientTheme } from "./types";

export const DEFAULT_LESSON_THUMBNAIL =
  "https://res.cloudinary.com/dgrjyts1r/image/upload/v1778946072/default-audio-thumbnail.png";

export interface TopicLessonCardProps {
  item: TopicLessonItem;
  onSelect?: (item: TopicLessonItem) => void;
  className?: string;
}

export default function TopicLessonCard({
  item,
  onSelect,
  className = "",
}: TopicLessonCardProps) {
  const {
    id,
    title,
    topicBadge = "TOPIC",
    lessonNumber = "Lesson 01",
    durationMinutes = 6,
    thumbnailUrl,
    categoryName,
    source,
  } = item;

  const [imgSrc, setImgSrc] = useState<string>(() =>
    thumbnailUrl && thumbnailUrl.trim() ? thumbnailUrl : DEFAULT_LESSON_THUMBNAIL
  );

  useEffect(() => {
    setImgSrc(
      thumbnailUrl && thumbnailUrl.trim() ? thumbnailUrl : DEFAULT_LESSON_THUMBNAIL
    );
  }, [thumbnailUrl]);

  const isBbcStyle =
    (source || "").toUpperCase().includes("BBC") ||
    (topicBadge || "").toUpperCase().includes("6 MIN");

  const displayBadge = topicBadge || (isBbcStyle ? "6 MIN" : categoryName || "TOPIC");
  const formattedDuration = `${durationMinutes} min`;
  const subtext = `${lessonNumber} · ${formattedDuration}`;

  const cardContent = (
    <div
      className={`bg-white rounded-2xl overflow-hidden border border-slate-200/70 shadow-xs hover:shadow-lg hover:border-emerald-300 hover:-translate-y-1 transition-all duration-300 flex flex-col group cursor-pointer h-full ${className}`}
      onClick={() => onSelect?.(item)}
    >
      {/* Cover Header with Thumbnail */}
      <div className="h-36 sm:h-40 relative overflow-hidden flex flex-col justify-between p-4 bg-slate-900 shrink-0">
        <img
          src={imgSrc}
          alt={title}
          onError={() => {
            if (imgSrc !== DEFAULT_LESSON_THUMBNAIL) {
              setImgSrc(DEFAULT_LESSON_THUMBNAIL);
            }
          }}
          className="absolute inset-0 w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
        />

        {/* Gradient Scrim for Contrast & Legibility */}
        <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/35 to-black/30 pointer-events-none" />

        {/* Top Badges */}
        <div className="flex items-center gap-2 relative z-10">
          <span className="text-[10px] font-black uppercase tracking-wider px-2.5 py-0.5 rounded-md bg-amber-400 text-amber-950 shadow-xs">
            {displayBadge}
          </span>
        </div>

        {/* Center / Lower Title */}
        <div className="relative z-10 mt-auto">
          <h3 className="font-extrabold text-white text-sm sm:text-base leading-snug line-clamp-2 drop-shadow group-hover:translate-x-0.5 transition-transform">
            {title}
          </h3>
        </div>

        {/* Subtle hover play indicator */}
        <div className="absolute inset-0 bg-black/20 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center pointer-events-none z-20">
          <div className="w-10 h-10 rounded-full bg-white/95 text-slate-900 flex items-center justify-center shadow-lg transform scale-90 group-hover:scale-100 transition-transform">
            <Play className="w-4 h-4 fill-slate-900 ml-0.5" />
          </div>
        </div>
      </div>

      {/* Card Footer */}
      <div className="p-3.5 bg-white flex items-center justify-between border-t border-slate-100 mt-auto">
        <div className="text-xs text-slate-500 font-medium flex items-center gap-1.5">
          <Clock className="w-3.5 h-3.5 text-slate-400" />
          <span>{subtext}</span>
        </div>

        <div className="w-6 h-6 rounded-full bg-slate-50 group-hover:bg-emerald-600 text-slate-400 group-hover:text-white flex items-center justify-center transition-colors shadow-2xs">
          <ArrowRight className="w-3.5 h-3.5" />
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
