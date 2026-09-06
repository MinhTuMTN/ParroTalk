"use client";

import { lessonService, type FeaturedLesson } from "@/features/lesson/services/lessonService";
import { Clock, Play } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";

const CARD_METAS = [
  {
    image: "/images/hero/student-study.png",
    alt: "Student studying illustration",
  },
  {
    image: "/images/hero/student-reading.png",
    alt: "Student reading illustration",
  },
];

const DEFAULT_FALLBACK_LESSONS: FeaturedLesson[] = [
  {
    id: "",
    title: "Luyện Nghe: TED Talks B2",
    categories: ["TED Talk", "Phản xạ nâng cao"],
    duration: 15,
  },
  {
    id: "",
    title: "Hội Thoại: Daily Conversations",
    categories: ["Giao tiếp đời sống", "A2"],
    duration: 10,
  },
];

export function FeaturedClassCards() {
  const [lessons, setLessons] = useState<FeaturedLesson[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    async function fetchFeaturedLessons() {
      try {
        const data = await lessonService.getFeaturedLessons();
        if (isMounted && Array.isArray(data) && data.length > 0) {
          setLessons(data);
        }
      } catch (err) {
        // Silently fallback to curated preview without breaking landing page
        console.info("Featured lessons fallback to curated preview", err);
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    fetchFeaturedLessons();

    return () => {
      isMounted = false;
    };
  }, []);

  // --- SKELETON LOADING STATE (Eliminates Cumulative Layout Shift) ---
  if (isLoading) {
    return (
      <div className="grid grid-cols-1 gap-5 md:grid-cols-2 text-left">
        {[1, 2].map((i) => (
          <div
            key={`featured-skeleton-${i}`}
            className="rounded-3xl border border-emerald-100/60 bg-white/70 p-6 shadow-xs animate-pulse min-h-[140px] flex items-center justify-between"
          >
            <div className="flex items-center gap-4">
              <div className="h-20 w-20 rounded-2xl bg-emerald-100/50 shrink-0" />
              <div className="space-y-2.5">
                <div className="h-4 w-28 rounded-full bg-emerald-100/60" />
                <div className="h-5 w-44 rounded-md bg-slate-200" />
                <div className="h-3.5 w-32 rounded-md bg-slate-100" />
              </div>
            </div>
            <div className="h-12 w-12 rounded-full bg-emerald-100 shrink-0" />
          </div>
        ))}
      </div>
    );
  }

  // --- DATA MAPPING WITH REAL BACKEND LESSONS ---
  const displayLessons = lessons.length > 0 ? lessons : DEFAULT_FALLBACK_LESSONS;

  return (
    <div className="grid grid-cols-1 gap-5 md:grid-cols-2 text-left">
      {displayLessons.map((lesson, index) => {
        const meta = CARD_METAS[index % CARD_METAS.length];
        const title = lesson.title || "Bài học luyện nghe";
        const category =
          lesson.categories && lesson.categories.length > 0
            ? lesson.categories.join(" • ")
            : "Luyện nghe • Phản xạ";
        const duration = lesson.duration ? `${lesson.duration} phút` : "10 phút";
        const href = lesson.id ? `/practice/${lesson.id}` : "/library";

        return (
          <div
            key={lesson.id || `featured-card-${index}`}
            className="group relative overflow-hidden rounded-3xl border border-emerald-100 bg-white p-6 text-slate-900 shadow-lg shadow-emerald-900/5 transition-all duration-300 hover:-translate-y-1 hover:border-emerald-300 hover:shadow-xl"
          >
            <div className="relative z-10 flex items-center justify-between gap-4 h-full">
              <div className="flex items-center gap-4 min-w-0">
                <div className="relative h-20 w-20 shrink-0 overflow-hidden rounded-2xl bg-emerald-50 p-1 border border-emerald-100">
                  <Image
                    src={meta.image}
                    alt={meta.alt}
                    width={80}
                    height={80}
                    className="h-full w-full object-contain drop-shadow-sm transition-transform duration-300 group-hover:scale-110"
                  />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="inline-flex items-center gap-1.5 rounded-full bg-amber-50 px-2.5 py-0.5 text-[11px] font-bold tracking-wide text-amber-800 border border-amber-200/80 mb-1.5">
                    <span className="h-1.5 w-1.5 rounded-full bg-amber-500" />
                    Mới cập nhật
                  </div>
                  <h3 className="text-lg font-extrabold tracking-tight text-slate-950 leading-snug line-clamp-1">
                    {title}
                  </h3>
                  <p className="text-xs font-semibold text-slate-500 mt-1 flex items-center gap-1.5 line-clamp-1">
                    <span>{category}</span>
                    <span>•</span>
                    <span className="inline-flex items-center gap-0.5">
                      <Clock size={11} /> {duration}
                    </span>
                  </p>
                </div>
              </div>

              <Link
                href={href}
                className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 shadow-xs transition-transform duration-300 group-hover:scale-110 group-hover:bg-emerald-600 group-hover:text-white active:scale-95"
                aria-label={`Luyện tập bài ${title}`}
              >
                <Play size={20} className="fill-current ml-0.5" />
              </Link>
            </div>
          </div>
        );
      })}
    </div>
  );
}
