import React, { useRef } from "react";
import { ChevronRight, ChevronLeft, PlayCircle, BookOpen, Clock, UploadCloud, Folder } from "lucide-react";
import Link from "next/link";
import { LibrarySection } from "@/components/library/types";
import LessonCard from "./LessonCard";

interface LessonSectionProps {
  section: LibrarySection;
}

const getSectionIcon = (type: string) => {
  switch (type) {
    case "IN_PROGRESS":
      return <PlayCircle className="w-5 h-5 text-green-500" />;
    case "USER_PROCESSING":
      return <Clock className="w-5 h-5 text-amber-500" />;
    case "USER_UPLOADED":
      return <UploadCloud className="w-5 h-5 text-blue-500" />;
    case "CATEGORY":
      return <Folder className="w-5 h-5 text-purple-500" />;
    case "PROGRAM":
      return <BookOpen className="w-5 h-5 text-rose-500" />;
    default:
      return <BookOpen className="w-5 h-5 text-gray-500" />;
  }
};

export default function LessonSection({ section }: LessonSectionProps) {
  const scrollRef = useRef<HTMLDivElement>(null);

  const scroll = (direction: "left" | "right") => {
    if (scrollRef.current) {
      const { scrollLeft, clientWidth } = scrollRef.current;
      const scrollAmount = clientWidth * 0.8;
      scrollRef.current.scrollTo({
        left: direction === "left" ? scrollLeft - scrollAmount : scrollLeft + scrollAmount,
        behavior: "smooth",
      });
    }
  };

  const showSeeAll = section.items.length >= 6; // Arbitrary logic based on maxItems

  return (
    <div className="flex flex-col gap-4 py-2">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          {getSectionIcon(section.type)}
          <h2 className="text-xl font-bold text-gray-900">{section.title}</h2>
        </div>
        {showSeeAll && (
          <Link
            href={"/library/section/"} // Just a placeholder, as the actual page isn't implemented
            className="text-sm font-semibold text-green-600 hover:text-green-700 flex items-center gap-1 transition-colors"
          >
            See All <ChevronRight className="w-4 h-4" />
          </Link>
        )}
      </div>

      <div className="relative group">
        <button
          onClick={() => scroll("left")}
          className="absolute left-0 top-1/2 -translate-y-1/2 -translate-x-4 z-10 p-2 bg-white rounded-full shadow-lg border border-gray-100 text-gray-600 hover:text-green-600 opacity-0 group-hover:opacity-100 transition-all hidden md:flex"
        >
          <ChevronLeft className="w-5 h-5" />
        </button>

        <div
          ref={scrollRef}
          className="flex gap-4 md:gap-6 overflow-x-auto scrollbar-hide pb-4 snap-x snap-mandatory"
          style={{ scrollbarWidth: "none", msOverflowStyle: "none" }}
        >
          {section.items.map((lesson) => (
            <div key={lesson.id} className="min-w-[280px] md:min-w-[320px] max-w-[280px] md:max-w-[320px] snap-start shrink-0">
              <LessonCard lesson={lesson} />
            </div>
          ))}
        </div>

        <button
          onClick={() => scroll("right")}
          className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-4 z-10 p-2 bg-white rounded-full shadow-lg border border-gray-100 text-gray-600 hover:text-green-600 opacity-0 group-hover:opacity-100 transition-all hidden md:flex"
        >
          <ChevronRight className="w-5 h-5" />
        </button>
      </div>
    </div>
  );
}
