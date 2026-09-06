"use client";

import React, { useState, useEffect, useCallback, useRef, useMemo, Suspense } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import {
  ChevronRight,
  Filter,
  ArrowUpDown,
  BookOpen,
  Sparkles,
  RotateCcw,
  AlertCircle,
  Menu,
  Flame,
  Search,
  Loader2,
  CheckCircle2,
  X,
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

const THEME_PALETTE: TopicGradientTheme[] = [
  "emerald",
  "indigo",
  "coral",
  "amber",
  "blue",
  "purple",
];

function getCategoryTheme(slugOrTitle?: string, index = 0): TopicGradientTheme {
  const lower = (slugOrTitle || "").toLowerCase();
  if (lower.includes("work") || lower.includes("career")) return "emerald";
  if (lower.includes("tech") || lower.includes("ai")) return "indigo";
  if (lower.includes("health") || lower.includes("life")) return "coral";
  if (lower.includes("daily")) return "amber";
  if (lower.includes("ted")) return "purple";
  if (lower.includes("6-min") || lower.includes("bbc")) return "emerald";
  return THEME_PALETTE[index % THEME_PALETTE.length];
}

function CategoriesContent() {
  const { isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const { openMobileMenu } = useUI();
  const router = useRouter();
  const searchParams = useSearchParams();

  // Filters from URL
  const currentCat = searchParams.get("cat") || "";
  const currentProgram = searchParams.get("program") || "";
  const currentSource = searchParams.get("source") || "";
  const currentSort = searchParams.get("sort") || "latest";

  const [selectedCategory, setSelectedCategory] = useState<string>(currentCat);
  const [program, setProgram] = useState<string>(currentProgram);
  const [source, setSource] = useState<string>(currentSource);
  const [sort, setSort] = useState<string>(currentSort);

  // Lazy loading pagination state
  const [page, setPage] = useState<number>(0);
  const [lessons, setLessons] = useState<LessonWithProgressDTO[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [totalPages, setTotalPages] = useState<number>(0);
  const [totalElements, setTotalElements] = useState<number>(0);

  // UI loading states
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isLoadingMore, setIsLoadingMore] = useState<boolean>(false);
  const [hasError, setHasError] = useState<boolean>(false);

  // Sentinel ref for infinite scroll
  const observerTargetRef = useRef<HTMLDivElement | null>(null);

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

  // Fetch all categories for filter chips
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

  // Sync state with URL params
  const updateUrlParams = useCallback(
    (newCat: string, newProgram: string, newSource: string, newSort: string) => {
      const p = new URLSearchParams();
      if (newCat) p.set("cat", newCat);
      if (newProgram) p.set("program", newProgram);
      if (newSource) p.set("source", newSource);
      if (newSort && newSort !== "latest") p.set("sort", newSort);

      const qs = p.toString();
      router.replace(`/library/categories${qs ? `?${qs}` : ""}`, { scroll: false });
    },
    [router]
  );

  // Fetch lessons with pagination
  const fetchLessons = useCallback(
    async (pageIndex: number, isInitial = false) => {
      if (isInitial) {
        setIsLoading(true);
      } else {
        setIsLoadingMore(true);
      }
      setHasError(false);

      try {
        let res;
        if (selectedCategory) {
          res = await lessonService.getCategoryLessons(selectedCategory, {
            page: pageIndex,
            size: 12,
            program: program || undefined,
            source: source || undefined,
            sort: sort || "latest",
          });
        } else {
          // If no specific category is selected, use search endpoint across all public lessons
          res = await lessonService.searchLibraryLessons({
            category: undefined,
            program: program || undefined,
            source: source || undefined,
            sort: sort || "latest",
            page: pageIndex,
            size: 12,
          });
        }

        const newItems = res.content || [];
        setTotalPages(res.totalPages || 0);
        setTotalElements(res.totalElements || 0);

        setLessons((prev) => {
          if (isInitial) {
            return newItems;
          }
          const seen = new Set(prev.map((item) => item.id));
          const filtered = newItems.filter((item) => !seen.has(item.id));
          return [...prev, ...filtered];
        });
      } catch (err) {
        console.error("Failed to load category lessons:", err);
        if (isInitial) {
          setHasError(true);
        }
      } finally {
        setIsLoading(false);
        setIsLoadingMore(false);
      }
    },
    [selectedCategory, program, source, sort]
  );

  // Trigger initial fetch or reset on filter changes
  useEffect(() => {
    if (isAuthenticated) {
      setPage(0);
      fetchLessons(0, true);
    }
  }, [fetchLessons, isAuthenticated, selectedCategory, program, source, sort]);

  // Load next page when page index increments > 0
  useEffect(() => {
    if (page > 0 && isAuthenticated) {
      fetchLessons(page, false);
    }
  }, [page, fetchLessons, isAuthenticated]);

  // Infinite Scroll Observer
  useEffect(() => {
    if (isLoading || isLoadingMore || page >= totalPages - 1) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          setPage((prev) => prev + 1);
        }
      },
      {
        threshold: 0.1,
        rootMargin: "300px",
      }
    );

    const currentTarget = observerTargetRef.current;
    if (currentTarget) {
      observer.observe(currentTarget);
    }

    return () => {
      if (currentTarget) {
        observer.unobserve(currentTarget);
      }
    };
  }, [isLoading, isLoadingMore, page, totalPages]);

  const handleCategorySelect = (catSlug: string) => {
    setSelectedCategory(catSlug);
    updateUrlParams(catSlug, program, source, sort);
  };

  const handleProgramChange = (newProgram: string) => {
    setProgram(newProgram);
    updateUrlParams(selectedCategory, newProgram, source, sort);
  };

  const handleSourceChange = (newSource: string) => {
    setSource(newSource);
    updateUrlParams(selectedCategory, program, newSource, sort);
  };

  const handleSortChange = (newSort: string) => {
    setSort(newSort);
    updateUrlParams(selectedCategory, program, source, newSort);
  };

  const handleResetFilters = () => {
    setSelectedCategory("");
    setProgram("");
    setSource("");
    setSort("latest");
    updateUrlParams("", "", "", "latest");
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

  const currentCategoryName = useMemo(() => {
    if (!selectedCategory) return "Tất cả chủ đề";
    const found = categories.find(
      (c) => c.id === selectedCategory || c.name.toLowerCase().replace(/\s+/g, "-") === selectedCategory.toLowerCase()
    );
    return found ? found.name : selectedCategory;
  }, [selectedCategory, categories]);

  const hasMore = page < totalPages - 1;

  return (
    <div className="bg-[#F8FAF9] flex flex-col flex-1 min-w-0 min-h-screen text-slate-900">
      {/* Sticky Header */}
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

          <nav className="hidden lg:flex items-center gap-8 text-sm font-bold">
            <Link
              href="/library"
              className="text-emerald-700 font-extrabold pb-1 border-b-2 border-emerald-600 transition-all"
            >
              Thư viện bài học
            </Link>
            <Link
              href="/library/search"
              className="text-slate-500 hover:text-slate-900 transition-colors flex items-center gap-1.5"
            >
              <Search className="w-4 h-4 text-slate-400" />
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

        <div className="flex items-center gap-3 sm:gap-4">
          <UserMenu />
        </div>
      </header>

      {/* Category Hero / Banner */}
      <div className="w-full bg-[#F0FDF4] py-8 sm:py-10 border-b border-emerald-100/80">
        <div className="max-w-6xl mx-auto px-4 md:px-8 space-y-4">
          <nav className="flex items-center gap-2 text-xs font-semibold text-slate-500">
            <Link href="/" className="hover:text-emerald-700 transition-colors">
              Trang chủ
            </Link>
            <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
            <Link href="/library" className="hover:text-emerald-700 transition-colors">
              Thư viện
            </Link>
            <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
            <span className="text-emerald-800 font-bold">{currentCategoryName}</span>
          </nav>

          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3.5">
              <div className="w-12 h-12 rounded-2xl bg-white shadow-xs border border-emerald-100 flex items-center justify-center shrink-0">
                <Sparkles className="w-6 h-6 text-emerald-700" />
              </div>
              <div>
                <h1 className="text-2xl sm:text-3xl lg:text-4xl font-black text-slate-900 tracking-tight">
                  Chủ đề bài học
                </h1>
                <p className="text-xs sm:text-sm text-slate-600 mt-1">
                  Khám phá toàn bộ bài học luyện nghe tiếng Anh phân loại theo các chủ đề chuyên sâu
                </p>
              </div>
            </div>

            <div className="self-start sm:self-auto">
              <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-emerald-100/90 text-emerald-800 text-xs font-extrabold shadow-2xs">
                <BookOpen className="w-3.5 h-3.5 text-emerald-600" />
                {totalElements} bài học
              </span>
            </div>
          </div>

          {/* Category Filter Chips */}
          <div className="flex items-center gap-2 overflow-x-auto pt-2 pb-1 no-scrollbar">
            <button
              type="button"
              onClick={() => handleCategorySelect("")}
              className={`px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all duration-200 active:scale-95 cursor-pointer ${
                !selectedCategory
                  ? "bg-emerald-600 text-white shadow-xs border border-emerald-600"
                  : "bg-white text-slate-600 hover:text-slate-900 hover:bg-emerald-50/50 border border-slate-200/80 shadow-2xs"
              }`}
            >
              Tất cả chủ đề
            </button>
            {categories.map((cat) => {
              const isSelected =
                selectedCategory === cat.id ||
                selectedCategory.toLowerCase() === cat.name.toLowerCase().replace(/\s+/g, "-");
              return (
                <button
                  key={cat.id}
                  type="button"
                  onClick={() => handleCategorySelect(cat.id)}
                  className={`px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all duration-200 active:scale-95 cursor-pointer ${
                    isSelected
                      ? "bg-emerald-600 text-white shadow-xs border border-emerald-600"
                      : "bg-white text-slate-600 hover:text-slate-900 hover:bg-emerald-50/50 border border-slate-200/80 shadow-2xs"
                  }`}
                >
                  {cat.name}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main className="flex-1 max-w-6xl mx-auto w-full px-4 md:px-8 py-6 sm:py-8 space-y-6 pb-24">
        {/* Filter Toolbar */}
        <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-2.5 sm:gap-3 flex-1 min-w-0">
            <div className="flex items-center gap-1 text-slate-400 text-xs font-bold shrink-0">
              <Filter className="w-3.5 h-3.5 text-emerald-600" />
              <span>Lọc:</span>
            </div>

            {/* Program Filter */}
            <select
              value={program}
              onChange={(e) => handleProgramChange(e.target.value)}
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
              onChange={(e) => handleSourceChange(e.target.value)}
              aria-label="Lọc theo nguồn"
              className="bg-slate-50 text-slate-700 text-xs sm:text-sm font-semibold px-3 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 cursor-pointer"
            >
              {SOURCE_OPTIONS.map((opt) => (
                <option key={opt.id} value={opt.id}>
                  {opt.label}
                </option>
              ))}
            </select>

            {/* Clear Filters */}
            {(selectedCategory || program || source || sort !== "latest") && (
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
              onChange={(e) => handleSortChange(e.target.value)}
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
              Không thể tải danh sách bài học
            </h3>
            <p className="text-sm text-slate-500 max-w-md">
              Đã xảy ra sự cố khi tải bài học. Vui lòng thử lại sau.
            </p>
            <button
              type="button"
              onClick={() => fetchLessons(0, true)}
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
                Chưa tìm thấy bài học nào
              </h3>
              <p className="text-sm text-slate-500 mt-1">
                Không có bài học nào khớp với bộ lọc hiện tại.
              </p>
            </div>
            {(selectedCategory || program || source) && (
              <button
                type="button"
                onClick={handleResetFilters}
                className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-bold transition-all active:scale-95 cursor-pointer shadow-xs"
              >
                <RotateCcw className="w-4 h-4" />
                <span>Xóa bộ lọc</span>
              </button>
            )}
          </div>
        ) : (
          <>
            {/* Grid of Lessons */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
              {lessons.map((lesson, idx) => {
                const durationMinutes = lesson.duration
                  ? Math.ceil(lesson.duration / 60)
                  : 6;
                const badge = currentCategoryName;
                const theme = getCategoryTheme(badge, idx);
                const topicItem: TopicLessonItem = {
                  id: lesson.id,
                  title: lesson.title || `Bài học ${idx + 1}`,
                  topicBadge: badge,
                  lessonNumber: `Lesson ${String(idx + 1).padStart(2, "0")}`,
                  durationMinutes,
                  gradientTheme: theme,
                  thumbnailUrl: lesson.thumbnailUrl || undefined,
                  categoryName: badge,
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

            {/* Infinite Scroll Sentinel and Status */}
            <div ref={observerTargetRef} className="w-full py-6 flex flex-col items-center justify-center">
              {isLoadingMore && (
                <div className="flex items-center gap-2 px-5 py-2.5 rounded-full bg-white border border-emerald-100 text-emerald-700 text-xs sm:text-sm font-bold shadow-xs animate-pulse">
                  <Loader2 className="w-4 h-4 animate-spin text-emerald-600" />
                  <span>Đang tải thêm bài học...</span>
                </div>
              )}

              {page >= totalPages - 1 && lessons.length > 0 && (
                <div className="py-6 text-center text-xs font-semibold text-slate-400 flex items-center justify-center gap-3">
                  <div className="h-px bg-slate-200 w-12 sm:w-20" />
                  <span className="flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
                    Đã hiển thị tất cả {totalElements} bài học
                  </span>
                  <div className="h-px bg-slate-200 w-12 sm:w-20" />
                </div>
              )}
            </div>
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

export default function CategoriesPage() {
  return (
    <Suspense fallback={<LibrarySkeleton />}>
      <CategoriesContent />
    </Suspense>
  );
}
