"use client";

import React, { useState, useEffect, useCallback, useRef, use } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import {
  ChevronRight,
  Filter,
  ArrowUpDown,
  BookOpen,
  Headphones,
  Laptop,
  Briefcase,
  Heart,
  Sparkles,
  RotateCcw,
  AlertCircle,
  Menu,
  Flame,
  Search,
  Loader2,
  CheckCircle2,
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

function getCategoryTheme(slug: string): TopicGradientTheme {
  const lower = slug.toLowerCase();
  if (lower.includes("work") || lower.includes("career")) return "emerald";
  if (lower.includes("tech") || lower.includes("ai")) return "indigo";
  if (lower.includes("health") || lower.includes("life")) return "coral";
  if (lower.includes("daily")) return "amber";
  if (lower.includes("ted")) return "purple";
  if (lower.includes("6-min") || lower.includes("bbc")) return "emerald";
  return "emerald";
}

function getCategoryIcon(slug: string) {
  const lower = slug.toLowerCase();
  if (lower.includes("work") || lower.includes("career")) {
    return <Briefcase className="w-6 h-6 text-emerald-700" />;
  }
  if (lower.includes("tech") || lower.includes("ai")) {
    return <Laptop className="w-6 h-6 text-indigo-700" />;
  }
  if (lower.includes("health") || lower.includes("life")) {
    return <Heart className="w-6 h-6 text-rose-700" />;
  }
  if (lower.includes("6-min") || lower.includes("bbc") || lower.includes("podcast")) {
    return <Headphones className="w-6 h-6 text-emerald-700" />;
  }
  return <Sparkles className="w-6 h-6 text-emerald-700" />;
}

function formatCategoryTitle(slug: string, categories: Category[]): string {
  const matched = categories.find(
    (c) => c.name.toLowerCase().replace(/\s+/g, "-") === slug.toLowerCase() || c.id === slug
  );
  if (matched) return matched.name;

  return slug
    .split("-")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

interface CategoryPageProps {
  params: Promise<{
    categorySlug: string;
  }>;
}

export default function CategoryLessonsPage({ params }: CategoryPageProps) {
  const resolvedParams = use(params);
  const categorySlug = resolvedParams.categorySlug;

  const { isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const { openMobileMenu } = useUI();
  const router = useRouter();
  const searchParams = useSearchParams();

  // Filters from URL
  const currentProgram = searchParams.get("program") || "";
  const currentSource = searchParams.get("source") || "";
  const currentSort = searchParams.get("sort") || "latest";

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

  // Sentinel ref for infinite scroll intersection observer
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

  // Fetch category list to display proper category name in header/breadcrumbs
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
    (newProgram: string, newSource: string, newSort: string) => {
      const p = new URLSearchParams();
      if (newProgram) p.set("program", newProgram);
      if (newSource) p.set("source", newSource);
      if (newSort && newSort !== "latest") p.set("sort", newSort);

      const qs = p.toString();
      router.replace(
        `/library/categories/${encodeURIComponent(categorySlug)}${qs ? `?${qs}` : ""}`,
        { scroll: false }
      );
    },
    [categorySlug, router]
  );

  // Fetch lessons for category with pagination
  const fetchLessons = useCallback(
    async (pageIndex: number, isInitial = false) => {
      if (isInitial) {
        setIsLoading(true);
      } else {
        setIsLoadingMore(true);
      }
      setHasError(false);

      try {
        const res = await lessonService.getCategoryLessons(categorySlug, {
          page: pageIndex,
          size: 12,
          program: program || undefined,
          source: source || undefined,
          sort: sort || "latest",
        });

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
    [categorySlug, program, source, sort]
  );

  // Initial load or filter change
  useEffect(() => {
    if (isAuthenticated && categorySlug) {
      setPage(0);
      fetchLessons(0, true);
    }
  }, [fetchLessons, isAuthenticated, categorySlug, program, source, sort]);

  // Load next page when page index increments > 0
  useEffect(() => {
    if (page > 0 && isAuthenticated && categorySlug) {
      fetchLessons(page, false);
    }
  }, [page, fetchLessons, isAuthenticated, categorySlug]);

  // Infinite Scroll Intersection Observer
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

  const handleProgramChange = (newProgram: string) => {
    setProgram(newProgram);
    updateUrlParams(newProgram, source, sort);
  };

  const handleSourceChange = (newSource: string) => {
    setSource(newSource);
    updateUrlParams(program, newSource, sort);
  };

  const handleSortChange = (newSort: string) => {
    setSort(newSort);
    updateUrlParams(program, source, newSort);
  };

  const handleResetFilters = () => {
    setProgram("");
    setSource("");
    setSort("latest");
    updateUrlParams("", "", "latest");
  };

  // Lesson select handler
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

  const categoryTitle = formatCategoryTitle(categorySlug, categories);
  const theme = getCategoryTheme(categorySlug);

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

        {/* Right Nav Actions */}
        <div className="flex items-center gap-3 sm:gap-4">
          <UserMenu />
        </div>
      </header>

      {/* Category Hero / Header Banner */}
      <div className="w-full bg-[#F0FDF4] py-8 sm:py-10 border-b border-emerald-100/80">
        <div className="max-w-6xl mx-auto px-4 md:px-8 space-y-4">
          {/* Breadcrumbs */}
          <nav className="flex items-center gap-2 text-xs font-semibold text-slate-500">
            <Link href="/" className="hover:text-emerald-700 transition-colors">
              Trang chủ
            </Link>
            <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
            <Link href="/library" className="hover:text-emerald-700 transition-colors">
              Thư viện
            </Link>
            <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
            <span className="text-emerald-800 font-bold">{categoryTitle}</span>
          </nav>

          {/* Title & Info */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3.5">
              <div className="w-12 h-12 rounded-2xl bg-white shadow-xs border border-emerald-100 flex items-center justify-center shrink-0">
                {getCategoryIcon(categorySlug)}
              </div>
              <div>
                <h1 className="text-2xl sm:text-3xl lg:text-4xl font-black text-slate-900 tracking-tight">
                  {categoryTitle}
                </h1>
                <p className="text-xs sm:text-sm text-slate-600 mt-1">
                  Khám phá danh sách các bài học luyện nghe chọn lọc thuộc chủ đề {categoryTitle}
                </p>
              </div>
            </div>

            {/* Total Count Badge */}
            <div className="self-start sm:self-auto">
              <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-emerald-100/90 text-emerald-800 text-xs font-extrabold shadow-2xs">
                <BookOpen className="w-3.5 h-3.5 text-emerald-600" />
                {totalElements} bài học
              </span>
            </div>
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

            {/* Active Filters Clear */}
            {(program || source || sort !== "latest") && (
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
              Đã xảy ra sự cố khi tải bài học thuộc chủ đề &quot;{categoryTitle}&quot;. Vui lòng thử lại.
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
                Không có bài học nào khớp với bộ lọc hiện tại trong chủ đề &quot;{categoryTitle}&quot;.
              </p>
            </div>
            {(program || source) && (
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
                const topicItem: TopicLessonItem = {
                  id: lesson.id,
                  title: lesson.title || `Bài học ${idx + 1}`,
                  topicBadge: categoryTitle,
                  lessonNumber: `Lesson ${String(idx + 1).padStart(2, "0")}`,
                  durationMinutes,
                  gradientTheme: theme,
                  thumbnailUrl: lesson.thumbnailUrl || undefined,
                  categoryName: categoryTitle,
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
