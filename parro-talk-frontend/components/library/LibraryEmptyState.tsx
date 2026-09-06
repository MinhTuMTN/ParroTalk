import React from "react";
import { SearchX, RotateCcw, Sparkles } from "lucide-react";

interface LibraryEmptyStateProps {
  title?: string;
  description?: string;
  searchQuery?: string;
  selectedCategory?: string;
  onResetFilters?: () => void;
}

export default function LibraryEmptyState({
  title = "Không tìm thấy bài học phù hợp",
  description = "Hãy thử thay đổi từ khóa tìm kiếm, chọn chủ đề khác hoặc xóa bộ lọc để khám phá thêm nhiều bài học thú vị.",
  searchQuery,
  selectedCategory,
  onResetFilters,
}: LibraryEmptyStateProps) {
  const hasActiveFilters = Boolean(searchQuery || selectedCategory);

  return (
    <div className="w-full max-w-4xl mx-auto my-8 px-4">
      <div className="bg-white rounded-3xl border border-gray-200/80 p-8 md:p-12 shadow-xs text-center flex flex-col items-center justify-center relative overflow-hidden">
        {/* Background ambient accents */}
        <div className="absolute top-0 right-0 -mt-8 -mr-8 w-40 h-40 bg-emerald-50 rounded-full blur-2xl pointer-events-none opacity-60" />
        <div className="absolute bottom-0 left-0 -mb-8 -ml-8 w-40 h-40 bg-amber-50 rounded-full blur-2xl pointer-events-none opacity-60" />

        {/* Center Icon badge */}
        <div className="relative mb-5">
          <div className="w-20 h-20 rounded-2xl bg-emerald-50 border border-emerald-100 flex items-center justify-center shadow-xs text-emerald-600">
            <SearchX className="w-10 h-10" />
          </div>
          <div className="absolute -bottom-1 -right-1 w-7 h-7 rounded-full bg-amber-100 border-2 border-white flex items-center justify-center text-amber-600">
            <Sparkles className="w-4 h-4" />
          </div>
        </div>

        {/* Content */}
        <h3 className="text-xl md:text-2xl font-bold text-gray-900 mb-2">
          {title}
        </h3>
        <p className="text-gray-500 text-sm md:text-base max-w-md mb-6 leading-relaxed">
          {description}
        </p>

        {/* Filter tags preview */}
        {hasActiveFilters && (
          <div className="flex flex-wrap items-center justify-center gap-2 mb-6">
            {searchQuery && (
              <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-gray-100 text-gray-700 text-xs font-medium rounded-full">
                <span>Từ khóa:</span>
                <span className="font-semibold text-gray-900">&ldquo;{searchQuery}&rdquo;</span>
              </span>
            )}
            {selectedCategory && (
              <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-50 text-emerald-700 text-xs font-medium rounded-full">
                <span>Bộ lọc đang chọn</span>
              </span>
            )}
          </div>
        )}

        {/* Reset Action */}
        {onResetFilters && (
          <button
            type="button"
            onClick={onResetFilters}
            className="btn-press inline-flex items-center gap-2 px-6 py-2.5 rounded-full bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 text-white text-sm font-semibold shadow-sm shadow-emerald-600/20 transition-colors cursor-pointer"
          >
            <RotateCcw className="w-4 h-4" />
            <span>Đặt lại bộ lọc</span>
          </button>
        )}
      </div>
    </div>
  );
}
