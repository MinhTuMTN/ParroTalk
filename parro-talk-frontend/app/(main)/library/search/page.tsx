"use client";

import React, { useState, useEffect, useCallback, useMemo, Suspense } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useDebounce } from "use-debounce";
import {
  Search,
  X,
  Filter,
  ArrowUpDown,
  BookOpen,
  RotateCcw,
  AlertCircle,
  Menu,
  Flame,
  ChevronLeft,
  ChevronRight,
  Sparkles,
} from "lucide-react";

import { useAuth } from "@/features/auth/hooks/useAuth";
import { useUI } from "@/hooks/useUI";
import {
  lessonService,
  LessonWithProgressDTO,
  Category,
} from "@/features/lesson/services/lessonService";
import UserMenu from "@/components/common/UserMenu";
import ConfirmationModal from "@/components/ui/ConfirmationModal";
import {
  TopicLessonCard,
  LibrarySkeleton,
  type TopicLessonItem,
  type TopicGradientTheme,
} from "@/components/library";

const PROGRAM_OPTIONS = [
  { id: "", label: "Tất cả chương trình" },
  { id: "6 Minute English", label: "6 Minute English" },
  { id: "English at Work", label: "English at Work" },
  { id: "TED Talks", label: "TED Talks" },
  { id: "VOA Learning English", label: "VOA Learning English" },
];

const SOURCE_OPTIONS = [
  { id: "", label: "Tất cả nguồn" },
  { id: "BBC", label: "BBC Learning English" },
  { id: "TED", label: "TED" },
  { id: "VOA", label: "VOA" },
];

const SORT_OPTIONS = [
  { id: "latest", label: "Mới nhất" },
  { id: "popular", label: "Phổ biến nhất" },
  { id: "duration_asc", label: "Thời lượng ngắn nhất" },
  { id: "duration_desc", label: "Thời lượng dài nhất" },
];

const SUGGESTED_SEARCHES = [
  "6 Minute English",
  "Technology & AI",
  "Business & Work",
  "Daily Conversation",
  "Health & Lifestyle",
  "TED Talks",
];

const THEME_PALETTE: TopicGradientTheme[] = [
  "emerald",
  "indigo",
  "coral",
  "amber",
  "blue",
  "purple",
];

function getThemeForLesson(title?: string | null, index = 0): TopicGradientTheme {
  const lower = (title || "").toLowerCase();
  if (lower.includes("work") || lower.includes("career") || lower.includes("công việc")) return "emerald";
  if (lower.includes("tech") || lower.includes("ai") || lower.includes("công nghệ")) return "indigo";
  if (lower.includes("health") || lower.includes("life") || lower.includes("sức khỏe")) return "coral";
  if (lower.includes("daily") || lower.includes("hàng ngày")) return "amber";
  if (lower.includes("ted")) return "purple";
  if (lower.includes("6 min") || lower.includes("bbc")) return "emerald";
  return THEME_PALETTE[index % THEME_PALETTE.length];
}

function SearchPageContent() {
  const { isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const { openMobileMenu } = useUI();
  const router = useRouter();
  const searchParams = useSearchParams();

  // URL query params
  const initialQuery = searchParams.get("q") || "";
  const initialCategory = searchParams.get("category") || "";
  const initialProgram = searchParams.get("program") || "";
  const initialSource = searchParams.get("source") || "";
  const initialSort = searchParams.get("sort") || "latest";
  const initialPage = Number(searchParams.get("page")) || 0;

  // Local state
  const [queryInput, setQueryInput] = useState<string>(initialQuery);
  const [debouncedQuery] = useDebounce(queryInput, 400);

  const [category, setCategory] = useState<string>(initialCategory);
  const [program, setProgram] = useState<string>(initialProgram);
  const [source, setSource] = useState<string>(initialSource);
  const [sort, setSort] = useState<string>(initialSort);
  const [page, setPage] = useState<number>(initialPage);

  // Data states
  const [lessons, setLessons] = useState<LessonWithProgressDTO[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [totalPages, setTotalPages] = useState<number>(0);
  const [totalElements, setTotalElements] = useState<number>(0);

  // UI states
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [hasError, setHasError] = useState<boolean>(false);

  // Reset Progress confirmation modal state
  const [resetModal, setResetModal] = useState<{
    isOpen: boolean;
    lessonId: string;
    lessonTitle: string;
    isResetting: boolean;
  }>({
    isOpen: false,
    lessonId: "",
    lessonTitle: "",
    isResetting: false,
  });

  // Redirect if unauthenticated
  useEffect(() => {
    if (!isAuthLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isAuthenticated, isAuthLoading, router]);

  // Load categories for filter dropdown
  useEffect(() => {
    let isMounted = true;
    async function loadCategories() {
      try {
        const res = await lessonService.getCategories("", 0, 50);
        if (isMounted && res?.content) {
          setCategories(res.content);
        }
      } catch (err) {
        console.error("Failed to load categories:", err);
      }
    }
    if (isAuthenticated) {
      loadCategories();
    }
    return () => {
      isMounted = false;
    };
  }, [isAuthenticated]);

  // Sync state changes to URL query parameters
  useEffect(() => {
    const p = new URLSearchParams();
    if (debouncedQuery.trim()) p.set("q", debouncedQuery.trim());
    if (category) p.set("category", category);
    if (program) p.set("program", program);
    if (source) p.set("source", source);
    if (sort && sort !== "latest") p.set("sort", sort);
    if (page > 0) p.set("page", page.toString());

    const newQueryString = p.toString();
    const currentQueryString = searchParams.toString();

    if (newQueryString !== currentQueryString) {
      router.replace(
        `/library/search${newQueryString ? `?${newQueryString}` : ""}`,
        { scroll: false }
      );
    }
  }, [debouncedQuery, category, program, source, sort, page, router, searchParams]);

  // Reset page to 0 on filter change
  useEffect(() => {
    setPage(0);
  }, [debouncedQuery, category, program, source, sort]);

  // Fetch search results
  const fetchSearchResults = useCallback(async () => {
    setIsLoading(true);
    setHasError(false);
    try {
      const res = await lessonService.searchLibraryLessons({
        q: debouncedQuery.trim() || undefined,
        category: category || undefined,
        program: program || undefined,
        source: source || undefined,
        sort: sort || "latest",
        page,
        size: 12,
      });

      setLessons(res.content || []);
      setTotalPages(res.totalPages || 0);
      setTotalElements(res.totalElements || 0);
    } catch (err) {
      console.error("Failed to search lessons:", err);
      setHasError(true);
    } finally {
      setIsLoading(false);
    }
  }, [debouncedQuery, category, program, source, sort, page]);

  useEffect(() => {
    if (isAuthenticated) {
      fetchSearchResults();
    }
  }, [fetchSearchResults, isAuthenticated]);

  const handleResetFilters = () => {
    setQueryInput("");
    setCategory("");
    setProgram("");
    setSource("");
    setSort("latest");
    setPage(0);
  };

  const handleSelectLesson = (lessonId: string, progress: number, title?: string) => {
    if (progress === 100) {
      setResetModal({
        isOpen: true,
        lessonId,
        lessonTitle: title || "Bài học này",
        isResetting: false,
      });
    } else {
      router.push(`/practice/${lessonId}`);
    }
  };

  const handleConfirmResetLesson = async () => {
    if (!resetModal.lessonId) return;
    setResetModal((prev) => ({ ...prev, isResetting: true }));
    try {
      await lessonService.resetProgress(resetModal.lessonId);
    } catch (err) {
      console.error("Failed to reset lesson progress:", err);
    } finally {
      const id = resetModal.lessonId;
      setResetModal({
        isOpen: false,
        lessonId: "",
        lessonTitle: "",
        isResetting: false,
      });
      router.push(`/practice/${id}`);
    }
  };

  return (
    <div className="bg-[#F8FAF9] flex flex-col flex-1 min-w-0 min-h-screen text-slate-900">
      {/* Sticky Header Navigation */}
      <header className="px-4 md:px-8 py-3.5 sm:py-4 flex items-center justify-between border-b border-emerald-100/70 bg-white/90 backdrop-blur-md sticky top-0 z-40 shadow-2xs">
        <div className="flex items-center gap-3 sm:gap-8">
          <button
            type="button"
            onClick={openMobileMenu}
            aria-label="Mở menu điều hướng"
            className="lg:hidden p-2 hover:bg-emerald-50 rounded-xl text-slate-600 transition-all active:scale-95 cursor-pointer"
          >
            <Menu size={22} />
          </button>

          {/* Brand Logo */}
          <Link href="/library" className="flex items-center gap-2 group">
            <div className="relative w-28 h-8 flex items-center">
              <Image
                src="/logo_long.png"
                alt="ParroTalk"
                width={80}
                height={28}
                className="object-contain group-hover:scale-105 transition-transform"
                priority
              />
            </div>
          </Link>

          {/* Tab Navigation */}
          <nav className="hidden lg:flex items-center gap-8 text-sm font-bold">
            <Link
              href="/library"
              className="text-slate-500 hover:text-slate-900 transition-colors"
            >
              Thư viện bài học
            </Link>
            <Link
              href="/library/search"
              className="text-emerald-700 font-extrabold pb-1 border-b-2 border-emerald-600 transition-all flex items-center gap-1.5"
            >
              <Search className="w-4 h-4 text-emerald-600" />
              <span>Tìm kiếm</span>
            </Link>
            <Link
              href="/profile"
              className="text-slate-500 hover:text-slate-900 transition-colors flex items-center gap-1.5"
            >
              <Flame className="w-4 h-4 text-orange-500" />
              <span>Tiến độ học tập</span>
            </Link>
          </nav>
        </div>

        {/* Right Nav Actions */}
        <div className="flex items-center gap-3 sm:gap-4">
          <UserMenu />
        </div>
      </header>

      {/* Hero Search Section */}
      <div className="w-full bg-[#F0FDF4] py-8 sm:py-10 border-b border-emerald-100/80">
        <div className="max-w-4xl mx-auto px-4 md:px-8 space-y-4">
          <div className="text-center space-y-1.5">
            <h1 className="text-2xl sm:text-3xl lg:text-4xl font-black text-slate-900 tracking-tight">
              Tìm kiếm bài học
            </h1>
            <p className="text-xs sm:text-sm text-slate-600 max-w-lg mx-auto">
              Tra cứu nhanh bài học theo tên, chủ đề, hoặc chương trình luyện nghe yêu thích
            </p>
          </div>

          {/* Search Input Box */}
          <div className="relative max-w-2xl mx-auto group">
            <div className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 group-focus-within:text-emerald-600 transition-colors">
              <Search className="w-5 h-5" />
            </div>
            <input
              type="text"
              value={queryInput}
              onChange={(e) => setQueryInput(e.target.value)}
              placeholder="Nhập từ khóa tìm kiếm (ví dụ: BBC, Technology, Work, Daily life...)"
              aria-label="Tìm kiếm bài học"
              className="w-full bg-white text-slate-800 placeholder-slate-400 pl-12 pr-10 py-3.5 rounded-full border border-emerald-200/80 shadow-xs focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 text-sm sm:text-base font-medium transition-all"
            />
            {queryInput && (
              <button
                type="button"
                onClick={() => setQueryInput("")}
                aria-label="Xóa từ khóa"
                className="absolute right-4 top-1/2 -translate-y-1/2 p-1 text-slate-400 hover:text-slate-700 rounded-full hover:bg-slate-100 transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>

          {/* Quick Suggestion Tags */}
          <div className="flex flex-wrap items-center justify-center gap-1.5 pt-1 text-xs">
            <span className="text-slate-500 font-bold flex items-center gap-1 mr-1">
              <Sparkles className="w-3.5 h-3.5 text-emerald-600" />
              Gợi ý:
            </span>
            {SUGGESTED_SEARCHES.map((item) => (
              <button
                key={item}
                type="button"
                onClick={() => setQueryInput(item)}
                className="px-3 py-1 rounded-full bg-white text-slate-600 hover:text-emerald-800 hover:bg-emerald-100/70 border border-emerald-200/60 shadow-2xs transition-all cursor-pointer font-semibold"
              >
                {item}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Main Content Area */}
      <main className="flex-1 max-w-6xl mx-auto w-full px-4 md:px-8 py-6 sm:py-8 space-y-6 pb-24">
        {/* Filter and Sort Toolbar */}
        <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-2.5 sm:gap-3 flex-1 min-w-0">
            <div className="flex items-center gap-1 text-slate-400 text-xs font-bold shrink-0">
              <Filter className="w-3.5 h-3.5 text-emerald-600" />
              <span>Lọc:</span>
            </div>

            {/* Category Filter */}
            {categories.length > 0 && (
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                aria-label="Lọc theo thể loại"
                className="bg-slate-50 text-slate-700 text-xs sm:text-sm font-semibold px-3 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 cursor-pointer"
              >
                <option value="">Tất cả thể loại</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.name}>
                    {c.name}
                  </option>
                ))}
              </select>
            )}

            {/* Program Filter */}
            <select
              value={program}
              onChange={(e) => setProgram(e.target.value)}
              aria-label="Lọc theo chương trình"
              className="bg-slate-50 text-slate-700 text-xs sm:text-sm font-semibold px-3 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 cursor-pointer"
            >
              {PROGRAM_OPTIONS.map((opt) => (
                <option key={opt.id} value={opt.id}>
                  {opt.label}
                </option>
              ))}
            </select>

            {/* Source Filter */}
            <select
              value={source}
              onChange={(e) => setSource(e.target.value)}
              aria-label="Lọc theo nguồn"
              className="bg-slate-50 text-slate-700 text-xs sm:text-sm font-semibold px-3 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 cursor-pointer"
            >
              {SOURCE_OPTIONS.map((opt) => (
                <option key={opt.id} value={opt.id}>
                  {opt.label}
                </option>
              ))}
            </select>

            {/* Active Filters Clear */}
            {(debouncedQuery || category || program || source || sort !== "latest") && (
              <button
                type="button"
                onClick={handleResetFilters}
                className="text-xs font-bold text-emerald-700 hover:text-emerald-800 flex items-center gap-1 ml-auto sm:ml-0 cursor-pointer hover:underline"
              >
                <RotateCcw className="w-3 h-3" />
                <span>Đặt lại</span>
              </button>
            )}
          </div>

          {/* Sort Dropdown */}
          <div className="flex items-center gap-2 shrink-0">
            <div className="flex items-center gap-1 text-slate-400 text-xs font-bold">
              <ArrowUpDown className="w-3.5 h-3.5 text-emerald-600" />
              <span>Sắp xếp:</span>
            </div>
            <select
              value={sort}
              onChange={(e) => setSort(e.target.value)}
              aria-label="Sắp xếp danh sách bài học"
              className="bg-slate-50 text-slate-700 text-xs sm:text-sm font-semibold px-3 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 cursor-pointer"
            >
              {SORT_OPTIONS.map((opt) => (
                <option key={opt.id} value={opt.id}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Results Header */}
        <div className="flex items-center justify-between pb-2 border-b border-slate-200/80">
          <div>
            <h2 className="text-lg sm:text-xl font-bold text-slate-900">
              {debouncedQuery
                ? `Kết quả tìm kiếm cho "${debouncedQuery}"`
                : program
                ? `Chương trình: ${program}`
                : category
                ? `Thể loại: ${category}`
                : "Tất cả bài học"}
            </h2>
            <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
              Tìm thấy {totalElements} bài học phù hợp
            </p>
          </div>
        </div>

        {/* Content States */}
        {isLoading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6 animate-pulse">
            {Array.from({ length: 8 }).map((_, idx) => (
              <div
                key={idx}
                className="bg-white rounded-2xl border border-slate-200/80 overflow-hidden shadow-xs h-64 flex flex-col justify-between p-4"
              >
                <div className="h-36 bg-slate-200 rounded-xl" />
                <div className="space-y-2 mt-4">
                  <div className="h-4 bg-slate-200 rounded w-3/4" />
                  <div className="h-3 bg-slate-100 rounded w-1/2" />
                </div>
              </div>
            ))}
          </div>
        ) : hasError ? (
          <div className="bg-white rounded-3xl border border-rose-100 p-8 text-center flex flex-col items-center gap-3 shadow-xs">
            <div className="w-12 h-12 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center">
              <AlertCircle className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-slate-800">
              Không thể tải kết quả tìm kiếm
            </h3>
            <p className="text-sm text-slate-500 max-w-md">
              Đã xảy ra sự cố khi tra cứu bài học. Vui lòng thử lại.
            </p>
            <button
              type="button"
              onClick={fetchSearchResults}
              className="mt-2 inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-bold transition-all active:scale-95 cursor-pointer shadow-xs"
            >
              <RotateCcw className="w-4 h-4" />
              <span>Thử tải lại</span>
            </button>
          </div>
        ) : lessons.length === 0 ? (
          <div className="bg-white rounded-3xl border border-slate-200 p-12 text-center flex flex-col items-center justify-center gap-4 shadow-xs">
            <div className="w-14 h-14 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <BookOpen className="w-7 h-7" />
            </div>
            <div className="max-w-md">
              <h3 className="text-lg font-bold text-slate-800">
                Không tìm thấy bài học phù hợp
              </h3>
              <p className="text-sm text-slate-500 mt-1">
                Thử tìm kiếm với từ khóa khác hoặc xóa bớt bộ lọc đang chọn.
              </p>
            </div>
            <button
              type="button"
              onClick={handleResetFilters}
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-bold transition-all active:scale-95 cursor-pointer shadow-xs"
            >
              <RotateCcw className="w-4 h-4" />
              <span>Xóa bộ lọc tìm kiếm</span>
            </button>
          </div>
        ) : (
          <>
            {/* Grid of Lessons */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
              {lessons.map((lesson, idx) => {
                const durationMinutes = lesson.duration
                  ? Math.ceil(lesson.duration / 60)
                  : 6;
                const theme = getThemeForLesson(lesson.title, idx);
                const topicItem: TopicLessonItem = {
                  id: lesson.id,
                  title: lesson.title || `Bài học ${idx + 1}`,
                  topicBadge: program || category || "LESSON",
                  lessonNumber: `Lesson ${String(page * 12 + idx + 1).padStart(2, "0")}`,
                  durationMinutes,
                  gradientTheme: theme,
                  thumbnailUrl: lesson.thumbnailUrl || undefined,
                  categoryName: category || undefined,
                };

                return (
                  <TopicLessonCard
                    key={lesson.id}
                    item={topicItem}
                    onSelect={() =>
                      handleSelectLesson(lesson.id, lesson.progress, lesson.title)
                    }
                  />
                );
              })}
            </div>

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-3 sm:gap-4 py-8">
                <button
                  type="button"
                  onClick={() => {
                    const newPage = Math.max(0, page - 1);
                    setPage(newPage);
                    window.scrollTo({ top: 0, behavior: "smooth" });
                  }}
                  disabled={page === 0}
                  aria-label="Trang trước"
                  className="p-3 bg-white border border-slate-200 rounded-full text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors shadow-xs cursor-pointer"
                >
                  <ChevronLeft size={20} />
                </button>
                <span className="text-xs sm:text-sm font-bold text-slate-700">
                  Trang {page + 1} / {totalPages}
                </span>
                <button
                  type="button"
                  onClick={() => {
                    const newPage = Math.min(totalPages - 1, page + 1);
                    setPage(newPage);
                    window.scrollTo({ top: 0, behavior: "smooth" });
                  }}
                  disabled={page >= totalPages - 1}
                  aria-label="Trang sau"
                  className="p-3 bg-white border border-slate-200 rounded-full text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors shadow-xs cursor-pointer"
                >
                  <ChevronRight size={20} />
                </button>
              </div>
            )}
          </>
        )}
      </main>

      {/* Reset Confirmation Modal */}
      <ConfirmationModal
        isOpen={resetModal.isOpen}
        onClose={() =>
          setResetModal({
            isOpen: false,
            lessonId: "",
            lessonTitle: "",
            isResetting: false,
          })
        }
        onConfirm={handleConfirmResetLesson}
        isLoading={resetModal.isResetting}
        title="Luyện tập lại bài học này?"
        message={`Bạn đã hoàn thành 100% bài học "${resetModal.lessonTitle}". Bạn có muốn bắt đầu lại từ đầu không? Tiến trình ghi nhớ hiện tại sẽ được đặt lại.`}
        confirmText="Học lại từ đầu"
        cancelText="Đóng"
      />
    </div>
  );
}

export default function SearchPage() {
  return (
    <Suspense fallback={<LibrarySkeleton />}>
      <SearchPageContent />
    </Suspense>
  );
}
