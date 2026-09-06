import React, { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Search, X, Sparkles } from "lucide-react";
import type { CategoryChipItem } from "./types";

export interface LibraryHeroProps {
  title?: string;
  subtitle?: string;
  searchValue?: string;
  onSearchChange?: (value: string) => void;
  onSearchSubmit?: (value: string) => void;
  onClearSearch?: () => void;
  placeholder?: string;
  categories?: (CategoryChipItem | { id: string; label?: string; name?: string; slug?: string })[];
  selectedCategory?: string;
  onSelectCategory?: (categoryId: string) => void;
  className?: string;
}

const DEFAULT_CATEGORIES: CategoryChipItem[] = [
  { id: "", label: "Tất cả" },
  { id: "work-career", label: "Work & Career" },
  { id: "technology", label: "Technology" },
  { id: "ted-talks", label: "TED Talks" },
  { id: "6-minute-english", label: "6 Minute English" },
  { id: "health-lifestyle", label: "Health & Lifestyle" },
];

export default function LibraryHero({
  title = "Library",
  subtitle = "Khám phá và luyện nghe tiếng Anh qua hàng trăm bài học thực tế",
  searchValue = "",
  onSearchChange,
  onSearchSubmit,
  onClearSearch,
  placeholder = "Tìm bài học, chủ đề, chương trình...",
  categories = DEFAULT_CATEGORIES,
  selectedCategory = "",
  onSelectCategory,
  className = "",
}: LibraryHeroProps) {
  const router = useRouter();
  const [internalSearch, setInternalSearch] = useState(searchValue);

  useEffect(() => {
    setInternalSearch(searchValue);
  }, [searchValue]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setInternalSearch(val);
    if (onSearchChange) {
      onSearchChange(val);
    }
  };

  const handleExecuteSearch = (query: string) => {
    const trimmed = query.trim();
    if (onSearchSubmit) {
      onSearchSubmit(trimmed);
    } else if (trimmed) {
      router.push(`/library/search?q=${encodeURIComponent(trimmed)}`);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      handleExecuteSearch(internalSearch);
    } else if (e.key === "Escape") {
      handleClear();
    }
  };

  const handleClear = () => {
    setInternalSearch("");
    if (onClearSearch) {
      onClearSearch();
    } else if (onSearchChange) {
      onSearchChange("");
    }
  };

  const handleCategoryClick = (id: string) => {
    if (onSelectCategory) {
      onSelectCategory(id);
    } else if (id) {
      router.push(`/library/categories/${id}`);
    } else {
      router.push("/library");
    }
  };

  return (
    <div
      className={`w-full bg-[#F0FDF4] py-8 sm:py-10 border-b border-emerald-100/80 ${className}`}
    >
      <div className="max-w-6xl mx-auto px-4 md:px-8 flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6">
        {/* Left Column: Title & Subtitle */}
        <div className="flex-1 min-w-0 max-w-xl">
          <div className="flex items-center gap-2 mb-1.5">
            <span className="inline-flex items-center gap-1 text-[11px] font-extrabold uppercase tracking-wider text-emerald-800 bg-emerald-100/90 px-2.5 py-0.5 rounded-full">
              <Sparkles className="w-3 h-3 text-emerald-600" />
              ParroTalk Content Hub
            </span>
          </div>
          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-black text-slate-900 tracking-tight leading-tight">
            {title}
          </h1>
          <p className="text-slate-600 text-sm sm:text-base mt-2 leading-relaxed">
            {subtitle}
          </p>
        </div>

        {/* Right Column: Pill Search Bar & Quick Category Filter Chips */}
        <div className="w-full lg:w-[460px] flex flex-col gap-3.5 shrink-0">
          {/* Pill Search Input */}
          <div className="relative w-full group">
            <button
              type="button"
              onClick={() => handleExecuteSearch(internalSearch)}
              aria-label="Thực hiện tìm kiếm"
              className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 group-focus-within:text-emerald-600 hover:text-emerald-600 p-1 rounded-full transition-colors cursor-pointer"
            >
              <Search className="w-5 h-5" />
            </button>
            <input
              type="text"
              value={internalSearch}
              onChange={handleInputChange}
              onKeyDown={handleKeyDown}
              placeholder={placeholder}
              aria-label="Tìm kiếm bài học"
              className="w-full bg-white text-slate-800 placeholder-slate-400 pl-12 pr-10 py-3 sm:py-3.5 rounded-full border border-emerald-200/70 shadow-xs focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 text-sm sm:text-base transition-all font-medium"
            />
            {internalSearch ? (
              <button
                type="button"
                onClick={handleClear}
                aria-label="Xóa tìm kiếm"
                className="absolute right-3.5 top-1/2 -translate-y-1/2 p-1 text-slate-400 hover:text-slate-700 rounded-full hover:bg-slate-100 transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            ) : null}
          </div>

          {/* Quick Category Chips */}
          {categories && categories.length > 0 && (
            <div className="flex items-center gap-2 overflow-x-auto pb-1 -mx-1 px-1 no-scrollbar scroll-smooth">
              {categories.map((cat) => {
                const categoryId = cat.id;
                const label =
                  "label" in cat && cat.label
                    ? cat.label
                    : "name" in cat && cat.name
                    ? cat.name
                    : cat.id;
                const isSelected = selectedCategory === categoryId;

                return (
                  <button
                    key={categoryId || "all"}
                    type="button"
                    onClick={() => handleCategoryClick(categoryId)}
                    className={`px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all duration-200 active:scale-95 cursor-pointer ${
                      isSelected
                        ? "bg-emerald-600 text-white shadow-xs border border-emerald-600"
                        : "bg-white text-slate-600 hover:text-slate-900 hover:bg-emerald-50/50 border border-slate-200/80 shadow-2xs"
                    }`}
                  >
                    {label}
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
