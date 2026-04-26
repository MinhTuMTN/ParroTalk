"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { lessonService } from "@/src/services/lessonService";
import type { Lesson, LessonFilter, LessonStatus } from "@/src/types/lesson";

type BulkAction = "publish" | "hide" | "delete";

export function useLessons() {
  const [lessons, setLessons] = useState<Lesson[]>([]);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [activeFilter, setActiveFilter] = useState<LessonFilter>("all");
  const [searchQuery, setSearchQuery] = useState("");
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
        search: searchQuery,
        status: activeFilter,
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
  }, [activeFilter, searchQuery, page, limit]);

  useEffect(() => {
    void fetchLessons();
  }, [fetchLessons]);

  useEffect(() => {
    setPage(0);
  }, [searchQuery, activeFilter]);

  const filteredLessons = useMemo(() => lessons, [lessons]);

  const toggleStatus = async (lesson: Lesson, status: LessonStatus) => {
    const previous = lessons;
    setError(null);
    setSuccess(null);
    setLessons((current) =>
      current.map((item) => (item.id === lesson.id ? { ...item, status } : item)),
    );

    try {
      await lessonService.updateLessonStatus(lesson.id, status);
      setSuccess("Lesson status updated.");
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
      checked ? Array.from(new Set([...current, id])) : current.filter((item) => item !== id),
    );
  };

  const selectAllVisible = (checked: boolean) => {
    if (!checked) {
      setSelectedIds((current) =>
        current.filter((id) => !filteredLessons.some((lesson) => lesson.id === id)),
      );
      return;
    }

    setSelectedIds((current) =>
      Array.from(new Set([...current, ...filteredLessons.map((lesson) => lesson.id)])),
    );
  };

  const bulkAction = async (action: BulkAction) => {
    const selected = lessons.filter((lesson) => selectedIds.includes(lesson.id));
    if (selected.length === 0) return;
    setError(null);
    setSuccess(null);

    if (action === "delete") {
      setMutating(true);
      try {
        await Promise.all(selected.map((lesson) => lessonService.deleteLesson(lesson.id)));
        setSelectedIds([]);
        await fetchLessons();
        setSuccess("Selected lessons deleted.");
      } catch {
        setError("Bulk delete failed.");
      } finally {
        setMutating(false);
      }
      return;
    }

    const status: LessonStatus = action === "publish" ? "published" : "hidden";
    const previous = lessons;
    setLessons((current) =>
      current.map((lesson) =>
        selectedIds.includes(lesson.id) ? { ...lesson, status } : lesson,
      ),
    );
    try {
      await Promise.all(
        selected.map((lesson) => lessonService.updateLessonStatus(lesson.id, status)),
      );
      setSuccess(`Selected lessons ${action === "publish" ? "published" : "hidden"}.`);
    } catch {
      setLessons(previous);
      setError("Bulk action failed.");
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
    searchQuery,
    page,
    limit,
    totalItems,
    totalPages,
    loading,
    mutating,
    error,
    success,
    setActiveFilter,
    setSearchQuery,
    toggleStatus,
    removeLesson,
    selectLesson,
    selectAllVisible,
    bulkAction,
    goToNextPage,
    goToPrevPage,
    clearMessages,
  };
}
