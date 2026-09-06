import React, { useRef } from "react";
import Link from "next/link";
import {
  ChevronRight,
  ChevronLeft,
  Briefcase,
  Laptop,
  Heart,
  Headphones,
  Sparkles,
  BookOpen,
  type LucideIcon,
} from "lucide-react";
import type { TopicLessonItem, TopicGradientTheme } from "./types";
import TopicLessonCard from "./TopicLessonCard";

export interface CuratedShelfSectionProps {
  id?: string;
  title: string;
  subtitle?: string;
  iconName?: string;
  icon?: LucideIcon | React.ReactNode;
  gradientTheme?: TopicGradientTheme;
  accentColor?: string;
  items: TopicLessonItem[];
  seeAllHref?: string;
  onSeeAll?: () => void;
  onSelectLesson?: (item: TopicLessonItem) => void;
  className?: string;
}

const ICON_MAP: Record<string, LucideIcon> = {
  briefcase: Briefcase,
  work: Briefcase,
  laptop: Laptop,
  computer: Laptop,
  technology: Laptop,
  heart: Heart,
  favorite: Heart,
  health: Heart,
  lifestyle: Heart,
  headphones: Headphones,
  podcast: Headphones,
  "6-minute": Headphones,
  sparkles: Sparkles,
  book: BookOpen,
};

export default function CuratedShelfSection({
  title,
  subtitle,
  iconName,
  icon,
  gradientTheme = "emerald",
  items,
  seeAllHref,
  onSeeAll,
  onSelectLesson,
  className = "",
}: CuratedShelfSectionProps) {
  const scrollRef = useRef<HTMLDivElement>(null);

  if (!items || items.length === 0) {
    return null;
  }

  const renderIcon = () => {
    if (icon) {
      if (React.isValidElement(icon)) return icon;
      const IconComponent = icon as LucideIcon;
      return <IconComponent className="w-5 h-5" />;
    }

    if (iconName && ICON_MAP[iconName.toLowerCase()]) {
      const IconComponent = ICON_MAP[iconName.toLowerCase()];
      return <IconComponent className="w-5 h-5" />;
    }

    // Default icon fallback based on title keywords
    const lowerTitle = title.toLowerCase();
    if (lowerTitle.includes("work") || lowerTitle.includes("career") || lowerTitle.includes("công việc")) {
      return <Briefcase className="w-5 h-5" />;
    }
    if (lowerTitle.includes("tech") || lowerTitle.includes("công nghệ")) {
      return <Laptop className="w-5 h-5" />;
    }
    if (lowerTitle.includes("health") || lowerTitle.includes("lifestyle") || lowerTitle.includes("sức khỏe")) {
      return <Heart className="w-5 h-5" />;
    }
    if (lowerTitle.includes("6 min") || lowerTitle.includes("bbc") || lowerTitle.includes("podcast")) {
      return <Headphones className="w-5 h-5" />;
    }
    return <Sparkles className="w-5 h-5" />;
  };

  const getIconBackground = (theme?: TopicGradientTheme) => {
    switch (theme) {
      case "indigo":
      case "blue":
        return "bg-blue-100 text-blue-700";
      case "coral":
      case "amber":
        return "bg-orange-100 text-orange-700";
      case "purple":
        return "bg-purple-100 text-purple-700";
      case "emerald":
      default:
        return "bg-emerald-100 text-emerald-700";
    }
  };

  const scroll = (direction: "left" | "right") => {
    if (scrollRef.current) {
      const scrollAmount = 320;
      scrollRef.current.scrollBy({
        left: direction === "left" ? -scrollAmount : scrollAmount,
        behavior: "smooth",
      });
    }
  };

  return (
    <section className={`max-w-6xl mx-auto w-full px-4 md:px-8 space-y-4 ${className}`}>
      {/* Section Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-start sm:items-center gap-3">
          <div
            className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 shadow-2xs ${getIconBackground(
              gradientTheme
            )}`}
          >
            {renderIcon()}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
                {title}
              </h2>
              <span className="bg-slate-100 text-slate-700 font-bold text-xs px-2.5 py-0.5 rounded-full hidden sm:inline-block">
                {items.length} bài
              </span>
            </div>
            {subtitle && (
              <p className="text-xs sm:text-sm text-slate-500 mt-0.5 line-clamp-1">
                {subtitle}
              </p>
            )}
          </div>
        </div>

        {/* Right Actions: Carousel arrows + See All */}
        <div className="flex items-center gap-2 shrink-0">
          {/* Scroll navigation arrows for long shelves on desktop */}
          {items.length > 4 && (
            <div className="hidden lg:flex items-center gap-1 mr-2">
              <button
                type="button"
                onClick={() => scroll("left")}
                aria-label="Cuộn sang trái"
                className="w-8 h-8 rounded-full border border-slate-200 bg-white hover:bg-slate-50 text-slate-600 flex items-center justify-center shadow-2xs transition-colors cursor-pointer"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                type="button"
                onClick={() => scroll("right")}
                aria-label="Cuộn sang phải"
                className="w-8 h-8 rounded-full border border-slate-200 bg-white hover:bg-slate-50 text-slate-600 flex items-center justify-center shadow-2xs transition-colors cursor-pointer"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          )}

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
      </div>

      {/* Grid or Horizontal Scrollable Container */}
      <div
        ref={scrollRef}
        className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6"
      >
        {items.map((item) => (
          <TopicLessonCard
            key={item.id}
            item={{
              ...item,
              gradientTheme: item.gradientTheme || gradientTheme,
            }}
            onSelect={onSelectLesson}
          />
        ))}
      </div>
    </section>
  );
}
