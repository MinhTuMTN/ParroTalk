"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useDebounce } from "use-debounce";
import { lessonService } from "@/features/lesson/services/lessonService";
import {
  LessonStatus,
  type Lesson,
  type LessonFilter,
} from "@/features/lesson/types/lesson";

type BulkAction = "publish" | "hide" | "delete";

export function useLessons() {
  const [lessons, setLessons] = useState<Lesson[]>([]);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [activeFilter, setActiveFilter] = useState<LessonFilter>("all");
  const [categoryFilter, setCategoryFilter] = useState<string>("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch] = useDebounce(searchQuery, 300);
  const [page, setPage] = useState(0);
  const [limit] = useState(10);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [mutating, setMutating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const fetchLessons = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await lessonService.getLessons({
        search: debouncedSearch,
        status: activeFilter,
        categoryId: categoryFilter,
        page,
        limit,
      });
      setLessons(data.lessons);
      setTotalItems(data.totalItems);
      setTotalPages(data.totalPages);
      setSelectedIds((current) =>
        current.filter((id) => data.lessons.some((lesson) => lesson.id === id)),
      );
    } catch {
      setError("Failed to load lessons.");
    } finally {
      setLoading(false);
    }
  }, [activeFilter, categoryFilter, debouncedSearch, page, limit]);

  const prevFiltersRef = useRef({ debouncedSearch, activeFilter, categoryFilter });

  useEffect(() => {
    if (
      prevFiltersRef.current.debouncedSearch !== debouncedSearch ||
      prevFiltersRef.current.activeFilter !== activeFilter ||
      prevFiltersRef.current.categoryFilter !== categoryFilter
    ) {
      prevFiltersRef.current = { debouncedSearch, activeFilter, categoryFilter };
      if (page !== 0) {
        setPage(0);
        return;
      }
    }
    void fetchLessons();
  }, [debouncedSearch, activeFilter, categoryFilter, page, fetchLessons]);

  const filteredLessons = useMemo(() => lessons, [lessons]);

  const toggleStatus = async (lesson: Lesson, status: LessonStatus) => {
    const previous = lessons;
    setError(null);
    setSuccess(null);
    setLessons((current) =>
      current.map((item) => (item.id === lesson.id ? { ...item, status } : item)),
    );

    try {
      const updated = await lessonService.updateLessonStatus(lesson.id, status);
      setLessons((current) =>
        current.map((item) =>
          item.id === lesson.id
            ? { ...item, ...updated, status: updated.status }
            : item,
        ),
      );
      setSuccess(
        `Lesson "${lesson.title}" ${status === LessonStatus.PUBLISHED ? "published" : "hidden"}.`,
      );
    } catch {
      setLessons(previous);
      setError("Failed to update lesson status.");
    }
  };

  const removeLesson = async (id: string) => {
    setMutating(true);
    setError(null);
    setSuccess(null);
    try {
      await lessonService.deleteLesson(id);
      setSelectedIds((current) => current.filter((item) => item !== id));
      await fetchLessons();
      setSuccess("Lesson deleted.");
    } catch {
      setError("Failed to delete lesson.");
    } finally {
      setMutating(false);
    }
  };

  const selectLesson = (id: string, checked: boolean) => {
    setSelectedIds((current) =>
      checked
        ? Array.from(new Set([...current, id]))
        : current.filter((item) => item !== id),
    );
  };

  const selectAllVisible = (checked: boolean) => {
    if (!checked) {
      setSelectedIds((current) =>
        current.filter(
          (id) => !filteredLessons.some((lesson) => lesson.id === id),
        ),
      );
      return;
    }

    setSelectedIds((current) =>
      Array.from(
        new Set([...current, ...filteredLessons.map((lesson) => lesson.id)]),
      ),
    );
  };

  const clearSelection = () => {
    setSelectedIds([]);
  };

  const bulkAction = async (action: BulkAction) => {
    const selected = lessons.filter((lesson) => selectedIds.includes(lesson.id));
    if (selected.length === 0) return;
    setError(null);
    setSuccess(null);
    setMutating(true);

    if (action === "delete") {
      try {
        const results = await Promise.allSettled(
          selected.map((lesson) => lessonService.deleteLesson(lesson.id)),
        );
        const fulfilled = results.filter((r) => r.status === "fulfilled").length;
        const rejected = results.length - fulfilled;
        setSelectedIds([]);
        await fetchLessons();
        if (rejected === 0) {
          setSuccess(`${selected.length} lesson(s) deleted.`);
        } else {
          setError(`Deleted ${fulfilled} of ${selected.length} lessons. ${rejected} failed.`);
        }
      } catch {
        setError("Bulk delete failed.");
      } finally {
        setMutating(false);
      }
      return;
    }

    const status: LessonStatus =
      action === "publish" ? LessonStatus.PUBLISHED : LessonStatus.HIDDEN;
    const previous = lessons;
    setLessons((current) =>
      current.map((lesson) =>
        selectedIds.includes(lesson.id) ? { ...lesson, status } : lesson,
      ),
    );
    try {
      const results = await Promise.allSettled(
        selected.map((lesson) =>
          lessonService.updateLessonStatus(lesson.id, status),
        ),
      );
      const fulfilled = results.filter((r) => r.status === "fulfilled").length;
      const rejected = results.length - fulfilled;
      await fetchLessons();
      if (rejected === 0) {
        setSuccess(
          `${selected.length} lesson(s) ${action === "publish" ? "published" : "hidden"}.`,
        );
      } else {
        setLessons(previous);
        setError(`Updated ${fulfilled} of ${selected.length} lessons. ${rejected} failed.`);
      }
    } catch {
      setLessons(previous);
      setError("Bulk action failed.");
    } finally {
      setMutating(false);
    }
  };

  const goToNextPage = () => {
    setPage((current) => (current + 1 < totalPages ? current + 1 : current));
  };

  const goToPrevPage = () => {
    setPage((current) => (current > 0 ? current - 1 : current));
  };

  const clearMessages = () => {
    setError(null);
    setSuccess(null);
  };

  return {
    filteredLessons,
    selectedIds,
    activeFilter,
    categoryFilter,
    searchQuery,
    debouncedSearch,
    page,
    limit,
    totalItems,
    totalPages,
    loading,
    mutating,
    error,
    success,
    setActiveFilter,
    setCategoryFilter,
    setSearchQuery,
    setPage,
    refresh: fetchLessons,
    toggleStatus,
    removeLesson,
    selectLesson,
    selectAllVisible,
    clearSelection,
    bulkAction,
    goToNextPage,
    goToPrevPage,
    clearMessages,
  };
}
