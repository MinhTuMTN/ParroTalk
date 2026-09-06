import React from "react";
import Link from "next/link";
import { Upload, ChevronRight, PlusCircle } from "lucide-react";
import type { UploadedLessonItem } from "./types";
import UploadedLessonCard from "./UploadedLessonCard";

export interface UploadedLessonsSectionProps {
  items: UploadedLessonItem[];
  title?: string;
  uploadHref?: string;
  onUploadClick?: () => void;
  onPractice?: (item: UploadedLessonItem) => void;
  onDelete?: (item: UploadedLessonItem) => Promise<void> | void;
  seeAllHref?: string;
  onSeeAll?: () => void;
  className?: string;
}

export default function UploadedLessonsSection({
  items,
  title = "Bài học của bạn",
  uploadHref = "/upload",
  onUploadClick,
  onPractice,
  onDelete,
  seeAllHref,
  onSeeAll,
  className = "",
}: UploadedLessonsSectionProps) {
  return (
    <section className={`max-w-6xl mx-auto w-full px-4 md:px-8 space-y-4 ${className}`}>
      {/* Section Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-emerald-100 flex items-center justify-center text-emerald-700 shadow-2xs">
            <Upload className="w-5 h-5" />
          </div>
          <h2 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
            {title}
          </h2>
          {items && items.length > 0 && (
            <span className="bg-emerald-100/80 text-emerald-800 font-bold text-xs px-2.5 py-0.5 rounded-full">
              {items.length} bài
            </span>
          )}
        </div>

        {/* Right Actions: Upload CTA + See All */}
        <div className="flex items-center gap-3">
          {onUploadClick ? (
            <button
              type="button"
              onClick={onUploadClick}
              className="bg-emerald-600 hover:bg-emerald-700 text-white font-bold px-4 py-2 rounded-full text-xs sm:text-sm flex items-center gap-1.5 shadow-xs hover:shadow transition-all active:scale-95 cursor-pointer"
            >
              <PlusCircle className="w-4 h-4" />
              <span>Tải lên bài mới</span>
            </button>
          ) : (
            <Link
              href={uploadHref}
              className="bg-emerald-600 hover:bg-emerald-700 text-white font-bold px-4 py-2 rounded-full text-xs sm:text-sm flex items-center gap-1.5 shadow-xs hover:shadow transition-all active:scale-95"
            >
              <PlusCircle className="w-4 h-4" />
              <span>Tải lên bài mới</span>
            </Link>
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

      {/* Content Grid or Empty Prompt */}
      {items && items.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5 sm:gap-6">
          {items.map((item) => (
            <UploadedLessonCard
              key={item.id}
              item={item}
              onPractice={onPractice}
              onDelete={onDelete}
            />
          ))}
        </div>
      ) : (
        <div className="bg-emerald-50/50 rounded-2xl border border-dashed border-emerald-200/80 p-8 text-center flex flex-col items-center justify-center gap-3">
          <div className="w-12 h-12 rounded-full bg-white shadow-xs text-emerald-600 flex items-center justify-center">
            <Upload className="w-6 h-6" />
          </div>
          <div className="max-w-md">
            <h3 className="font-bold text-base text-slate-800">
              Bạn chưa có bài học riêng nào
            </h3>
            <p className="text-xs sm:text-sm text-slate-500 mt-1">
              Tải lên file âm thanh hoặc video của riêng bạn để hệ thống tự động tạo bài luyện nghe và từ vựng thông minh.
            </p>
          </div>
          <Link
            href={uploadHref}
            className="mt-2 inline-flex items-center gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs sm:text-sm px-5 py-2.5 rounded-full shadow-xs transition-all active:scale-95"
          >
            <PlusCircle className="w-4 h-4" />
            <span>Tải lên ngay</span>
          </Link>
        </div>
      )}
    </section>
  );
}
