"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  AlertCircle,
  BookOpen,
  ChevronLeft,
  ChevronRight,
  Eye,
  EyeOff,
  Folder,
  Headphones,
  Plus,
  RotateCcw,
  Search,
  Trash2,
  Video,
  X,
} from "lucide-react";

import LessonListHeader from "./LessonListHeader";
import LessonTable from "@/features/lesson/components/LessonTable";
import Button from "@/components/ui/Button";
import ConfirmationModal from "@/components/ui/ConfirmationModal";
import FloatingToast from "@/components/ui/FloatingToast";
import { useLessons } from "@/features/lesson/hooks/useLessons";
import { lessonService } from "@/features/lesson/services/lessonService";
import { LessonStatus, type LessonFilter } from "@/features/lesson/types/lesson";
import { categoryService } from "@/features/cms/services/categoryService";
import type { AdminCategoryDto } from "@/features/cms/types";

type CategoryOption = {
  id: string;
  label: string;
};

const statusTabs: { value: LessonFilter; label: string }[] = [
  { value: "all", label: "All" },
  { value: LessonStatus.PUBLISHED, label: "Published" },
  { value: LessonStatus.HIDDEN, label: "Hidden" },
];

function flattenCategoryOptions(
  nodes: AdminCategoryDto[],
  depth = 0,
): CategoryOption[] {
  const result: CategoryOption[] = [];
  for (const node of nodes) {
    const prefix = depth > 0 ? `${"— ".repeat(depth)}` : "";
    result.push({ id: node.id, label: `${prefix}${node.name}` });
    if (node.children && node.children.length > 0) {
      result.push(...flattenCategoryOptions(node.children, depth + 1));
    }
  }
  return result;
}

function getPaginationRange(currentPage: number, totalPages: number): (number | "ellipsis")[] {
  if (totalPages <= 5) {
    return Array.from({ length: totalPages }, (_, index) => index);
  }
  const pages: (number | "ellipsis")[] = [0];
  const start = Math.max(1, currentPage - 1);
  const end = Math.min(totalPages - 2, currentPage + 1);

  if (start > 1) pages.push("ellipsis");
  for (let p = start; p <= end; p++) pages.push(p);
  if (end < totalPages - 2) pages.push("ellipsis");

  pages.push(totalPages - 1);
  return pages;
}

export default function LessonListPage() {
  const router = useRouter();
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const [confirmBulkDelete, setConfirmBulkDelete] = useState(false);
  const [categoryOptions, setCategoryOptions] = useState<CategoryOption[]>([]);

  const {
    filteredLessons,
    selectedIds,
    activeFilter,
    categoryFilter,
    searchQuery,
    debouncedSearch,
    page,
    limit,
    totalPages,
    totalItems,
    loading,
    mutating,
    error,
    success,
    setActiveFilter,
    setCategoryFilter,
    setSearchQuery,
    setPage,
    refresh,
    toggleStatus,
    removeLesson,
    selectLesson,
    selectAllVisible,
    clearSelection,
    bulkAction,
    goToNextPage,
    goToPrevPage,
    clearMessages,
  } = useLessons();

  useEffect(() => {
    let mounted = true;

    const loadCategories = async () => {
      try {
        const tree = await categoryService.getCategoryTree();
        if (!mounted) return;
        if (tree && tree.length > 0) {
          setCategoryOptions(flattenCategoryOptions(tree));
          return;
        }
      } catch {
        // Fall back to flat categories endpoint if tree endpoint is unavailable
      }

      try {
        const fallback = await lessonService.getCategories("", 0, 100);
        if (!mounted) return;
        setCategoryOptions(
          (fallback.content ?? []).map((item) => ({
            id: item.id,
            label: item.name,
          })),
        );
      } catch {
        if (!mounted) return;
        setCategoryOptions([]);
      }
    };

    void loadCategories();
    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    if (!success) return;
    const timer = window.setTimeout(() => {
      clearMessages();
    }, 3500);
    return () => window.clearTimeout(timer);
  }, [success, clearMessages]);

  const selectedCount = selectedIds.length;
  const lessonToDelete = useMemo(
    () => filteredLessons.find((lesson) => lesson.id === deleteId),
    [filteredLessons, deleteId],
  );

  const pageStats = useMemo(() => {
    let published = 0;
    let hidden = 0;
    let video = 0;
    let audio = 0;

    for (const lesson of filteredLessons) {
      if (lesson.status === LessonStatus.PUBLISHED) {
        published += 1;
      } else {
        hidden += 1;
      }

      if (lesson.mediaType?.toUpperCase() === "VIDEO") {
        video += 1;
      } else {
        audio += 1;
      }
    }

    return { published, hidden, video, audio };
  }, [filteredLessons]);

  const hasActiveFilters =
    Boolean(debouncedSearch.trim()) ||
    activeFilter !== "all" ||
    categoryFilter !== "all";

  const resetFilters = () => {
    setSearchQuery("");
    setActiveFilter("all");
    setCategoryFilter("all");
  };

  const rangeStart = totalItems === 0 ? 0 : page * limit + 1;
  const rangeEnd = Math.min(totalItems, page * limit + filteredLessons.length);
  const paginationItems = getPaginationRange(page, Math.max(totalPages, 1));

  return (
    <div className="mx-auto max-w-7xl space-y-6 pb-12">
      {success ? (
        <FloatingToast
          message={success}
          variant="success"
          onClose={clearMessages}
        />
      ) : null}

      <p role="status" aria-live="polite" className="sr-only">
        {loading
          ? "Loading lessons"
          : `Showing ${filteredLessons.length} of ${totalItems} lessons`}
      </p>

      <LessonListHeader
        totalCount={totalItems}
        loading={loading}
        onRefresh={() => void refresh()}
      />

      {/* KPI Stats Strip */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <div className="flex flex-col justify-between rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
            Total Matches
          </span>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-2xl font-black tabular-nums text-slate-900">
              {totalItems}
            </span>
            <span className="rounded-md bg-slate-100 px-2 py-0.5 text-xs font-semibold tabular-nums text-slate-600">
              Page {Math.min(page + 1, Math.max(totalPages, 1))}/
              {Math.max(totalPages, 1)}
            </span>
          </div>
        </div>

        <div className="flex flex-col justify-between rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
            Published (Page)
          </span>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-2xl font-black tabular-nums text-emerald-700">
              {pageStats.published}
            </span>
            <span className="inline-flex items-center gap-1 rounded-md bg-emerald-50 px-2 py-0.5 text-xs font-semibold text-emerald-700">
              <Eye className="h-3.5 w-3.5" aria-hidden="true" />
              Live
            </span>
          </div>
        </div>

        <div className="flex flex-col justify-between rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
            Hidden (Page)
          </span>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-2xl font-black tabular-nums text-slate-700">
              {pageStats.hidden}
            </span>
            <span className="inline-flex items-center gap-1 rounded-md bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">
              <EyeOff className="h-3.5 w-3.5" aria-hidden="true" />
              Draft
            </span>
          </div>
        </div>

        <div className="flex flex-col justify-between rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
            Media Mix (Page)
          </span>
          <div className="mt-2 flex items-center justify-between gap-2">
            <div className="flex items-center gap-2 text-xs font-bold tabular-nums">
              <span className="inline-flex items-center gap-1 rounded-md bg-indigo-50 px-2 py-1 text-indigo-700">
                <Video className="h-3.5 w-3.5" aria-hidden="true" />
                {pageStats.video}
              </span>
              <span className="inline-flex items-center gap-1 rounded-md bg-teal-50 px-2 py-1 text-teal-700">
                <Headphones className="h-3.5 w-3.5" aria-hidden="true" />
                {pageStats.audio}
              </span>
            </div>
            <BookOpen className="h-4 w-4 text-slate-400" aria-hidden="true" />
          </div>
        </div>
      </div>

      {/* Filter & Search Toolbar */}
      <div className="flex flex-col gap-3 rounded-2xl border border-slate-100 bg-white p-4 shadow-sm lg:flex-row lg:items-center lg:justify-between">
        <div className="flex flex-1 flex-col gap-3 sm:flex-row sm:items-center">
          <div className="relative w-full sm:max-w-sm">
            <Search
              className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
              aria-hidden="true"
            />
            <input
              type="search"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              aria-label="Search lessons by title"
              placeholder="Search lessons by title..."
              className="w-full rounded-xl border border-slate-200 bg-slate-50/60 py-2 pl-10 pr-9 text-sm text-slate-800 outline-none transition focus:border-emerald-500 focus:bg-white focus:ring-2 focus:ring-emerald-100"
            />
            {searchQuery ? (
              <button
                type="button"
                onClick={() => setSearchQuery("")}
                aria-label="Clear search query"
                className="absolute right-2.5 top-1/2 -translate-y-1/2 cursor-pointer rounded-md p-1 text-slate-400 hover:text-slate-700"
              >
                <X className="h-3.5 w-3.5" aria-hidden="true" />
              </button>
            ) : null}
          </div>

          <div className="relative w-full sm:w-56">
            <label htmlFor="lesson-category-filter" className="sr-only">
              Filter by category
            </label>
            <Folder
              className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
              aria-hidden="true"
            />
            <select
              id="lesson-category-filter"
              value={categoryFilter}
              onChange={(event) => setCategoryFilter(event.target.value)}
              className="w-full cursor-pointer appearance-none rounded-xl border border-slate-200 bg-slate-50/60 py-2 pl-9 pr-8 text-sm font-medium text-slate-700 outline-none transition focus:border-emerald-500 focus:bg-white focus:ring-2 focus:ring-emerald-100"
            >
              <option value="all">All Categories</option>
              {categoryOptions.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Status Filter Pills */}
        <div
          role="group"
          aria-label="Filter lessons by visibility status"
          className="flex items-center gap-1 self-start rounded-xl bg-slate-100 p-1 lg:self-auto"
        >
          {statusTabs.map((tab) => {
            const isActive = activeFilter === tab.value;
            return (
              <button
                key={tab.value}
                type="button"
                aria-pressed={isActive}
                onClick={() => setActiveFilter(tab.value)}
                className={`cursor-pointer rounded-lg px-3.5 py-1.5 text-xs font-bold transition ${
                  isActive
                    ? "bg-white text-slate-900 shadow-xs"
                    : "text-slate-600 hover:text-slate-900"
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Contextual Bulk Selection Toolbar */}
      {selectedCount > 0 ? (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-emerald-200 bg-emerald-50/80 px-4 py-3 shadow-xs">
          <div className="flex items-center gap-3">
            <span className="inline-flex h-7 min-w-7 items-center justify-center rounded-lg bg-emerald-600 px-2 text-xs font-black tabular-nums text-white">
              {selectedCount}
            </span>
            <span className="text-sm font-semibold text-emerald-950">
              lesson{selectedCount === 1 ? "" : "s"} selected
            </span>
            <button
              type="button"
              onClick={clearSelection}
              disabled={mutating}
              className="cursor-pointer text-xs font-bold text-emerald-700 underline underline-offset-2 hover:text-emerald-900 disabled:opacity-50"
            >
              Clear selection
            </button>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              disabled={mutating}
              leftIcon={<Eye className="h-3.5 w-3.5" aria-hidden="true" />}
              onClick={() => void bulkAction("publish")}
            >
              Publish Selected
            </Button>
            <Button
              type="button"
              variant="secondary"
              size="sm"
              disabled={mutating}
              leftIcon={<EyeOff className="h-3.5 w-3.5" aria-hidden="true" />}
              onClick={() => void bulkAction("hide")}
            >
              Hide Selected
            </Button>
            <Button
              type="button"
              variant="danger"
              size="sm"
              disabled={mutating}
              leftIcon={<Trash2 className="h-3.5 w-3.5" aria-hidden="true" />}
              onClick={() => setConfirmBulkDelete(true)}
            >
              Delete Selected
            </Button>
          </div>
        </div>
      ) : null}

      {/* Error State Banner */}
      {error ? (
        <div
          role="alert"
          className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800"
        >
          <div className="flex items-center gap-2.5">
            <AlertCircle className="h-4 w-4 shrink-0 text-rose-600" aria-hidden="true" />
            <span className="font-medium">{error}</span>
          </div>
          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() => void refresh()}
              leftIcon={<RotateCcw className="h-3.5 w-3.5" aria-hidden="true" />}
            >
              Try Again
            </Button>
            <button
              type="button"
              onClick={clearMessages}
              className="cursor-pointer rounded-lg px-2 py-1 text-xs font-bold text-rose-700 hover:bg-rose-100"
            >
              Dismiss
            </button>
          </div>
        </div>
      ) : null}

      {/* Main Table Card */}
      <div className="overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-sm">
        {loading ? (
          <div className="divide-y divide-slate-100">
            <div className="bg-slate-50/80 px-6 py-3.5">
              <div className="h-3 w-48 animate-pulse rounded bg-slate-200" />
            </div>
            {Array.from({ length: 6 }).map((_, index) => (
              <div key={index} className="flex items-center gap-4 px-6 py-4">
                <div className="h-4 w-4 animate-pulse rounded bg-slate-200" />
                <div className="h-11 w-16 shrink-0 animate-pulse rounded-xl bg-slate-200" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 w-2/5 animate-pulse rounded bg-slate-200" />
                  <div className="h-3 w-1/4 animate-pulse rounded bg-slate-100" />
                </div>
                <div className="hidden h-5 w-24 animate-pulse rounded bg-slate-100 md:block" />
                <div className="hidden h-5 w-16 animate-pulse rounded bg-slate-100 sm:block" />
                <div className="h-6 w-20 animate-pulse rounded-full bg-slate-200" />
              </div>
            ))}
          </div>
        ) : filteredLessons.length === 0 ? (
          <div className="mx-auto flex max-w-md flex-col items-center justify-center p-14 text-center">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-slate-100 text-slate-400">
              <BookOpen className="h-8 w-8" aria-hidden="true" />
            </div>
            <h2 className="text-base font-bold text-slate-800">
              {hasActiveFilters
                ? "No lessons match your filters"
                : "No lessons yet"}
            </h2>
            <p className="mt-1 text-xs leading-relaxed text-slate-500">
              {hasActiveFilters
                ? "Try clearing your search query, category filter, or visibility filter to view more lessons."
                : "Upload your first audio file or import a YouTube video to build your dictation library."}
            </p>
            <div className="mt-6">
              {hasActiveFilters ? (
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={resetFilters}
                  leftIcon={<RotateCcw className="h-3.5 w-3.5" aria-hidden="true" />}
                >
                  Reset Filters
                </Button>
              ) : (
                <Link
                  href="/upload"
                  className="inline-flex items-center gap-2 rounded-2xl bg-emerald-600 px-5 py-2.5 text-sm font-semibold text-white shadow-md shadow-emerald-600/20 transition hover:bg-emerald-700"
                >
                  <Plus className="h-4 w-4" aria-hidden="true" />
                  <span>Create Lesson</span>
                </Link>
              )}
            </div>
          </div>
        ) : (
          <LessonTable
            lessons={filteredLessons}
            selectedIds={selectedIds}
            disabled={mutating}
            onSelectAll={selectAllVisible}
            onSelect={selectLesson}
            onToggleStatus={(lesson, next) =>
              void toggleStatus(
                lesson,
                next ? LessonStatus.PUBLISHED : LessonStatus.HIDDEN,
              )
            }
            onEdit={(lesson) => router.push(`/admin/lessons/${lesson.id}`)}
            onDelete={(lesson) => setDeleteId(lesson.id)}
          />
        )}

        {/* Modern Pagination Footer */}
        <div className="flex flex-col items-center justify-between gap-3 border-t border-slate-100 bg-slate-50/50 px-5 py-4 text-xs text-slate-600 sm:flex-row">
          <p className="font-medium tabular-nums">
            Showing <span className="font-bold text-slate-900">{rangeStart}</span>–
            <span className="font-bold text-slate-900">{rangeEnd}</span> of{" "}
            <span className="font-bold text-slate-900">{totalItems}</span> lessons
          </p>

          <nav aria-label="Lessons pagination" className="flex items-center gap-1.5">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={goToPrevPage}
              disabled={page <= 0 || loading || mutating}
              leftIcon={<ChevronLeft className="h-3.5 w-3.5" aria-hidden="true" />}
            >
              Previous
            </Button>

            <div className="hidden items-center gap-1 sm:flex">
              {paginationItems.map((item, idx) =>
                item === "ellipsis" ? (
                  <span key={`ellipsis-${idx}`} className="px-1.5 text-slate-400">
                    &hellip;
                  </span>
                ) : (
                  <button
                    key={item}
                    type="button"
                    aria-label={`Go to page ${item + 1}`}
                    aria-current={item === page ? "page" : undefined}
                    disabled={loading || mutating}
                    onClick={() => setPage(item)}
                    className={`min-h-[32px] min-w-[32px] cursor-pointer rounded-lg px-2 text-xs font-bold tabular-nums transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 ${
                      item === page
                        ? "bg-emerald-600 text-white shadow-xs"
                        : "text-slate-600 hover:bg-slate-200/70 hover:text-slate-900"
                    }`}
                  >
                    {item + 1}
                  </button>
                ),
              )}
            </div>

            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={goToNextPage}
              disabled={page + 1 >= totalPages || loading || mutating}
              rightIcon={<ChevronRight className="h-3.5 w-3.5" aria-hidden="true" />}
            >
              Next
            </Button>
          </nav>
        </div>
      </div>

      {/* Single Lesson Delete Confirmation */}
      <ConfirmationModal
        isOpen={Boolean(deleteId)}
        onClose={() => setDeleteId(null)}
        isLoading={mutating}
        onConfirm={async () => {
          if (!deleteId) return;
          await removeLesson(deleteId);
          setDeleteId(null);
        }}
        title="Delete lesson?"
        message={
          lessonToDelete
            ? `This will permanently delete "${lessonToDelete.title}" and all of its segments.`
            : "This action cannot be undone."
        }
        confirmText="Delete"
      />

      {/* Bulk Lesson Delete Confirmation */}
      <ConfirmationModal
        isOpen={confirmBulkDelete}
        onClose={() => setConfirmBulkDelete(false)}
        isLoading={mutating}
        onConfirm={async () => {
          await bulkAction("delete");
          setConfirmBulkDelete(false);
        }}
        title={`Delete ${selectedCount} lesson${selectedCount === 1 ? "" : "s"}?`}
        message={`This will permanently delete the ${selectedCount} selected lesson${selectedCount === 1 ? "" : "s"} and all associated transcript segments.`}
        confirmText="Delete All"
      />
    </div>
  );
}
