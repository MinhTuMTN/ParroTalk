"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import Sidebar from "@/components/common/Sidebar";

import LessonListHeader from "./LessonListHeader";
import LessonTable from "@/features/lesson/components/LessonTable";
import Button from "@/components/ui/Button";
import ConfirmationModal from "@/components/ui/ConfirmationModal";
import { useLessons } from "@/features/lesson/hooks/useLessons";
import { LessonStatus, type LessonFilter } from "@/features/lesson/types/lesson";

const tabs: { value: LessonFilter; label: string }[] = [
  { value: "all", label: "All" },
  { value: LessonStatus.PUBLISHED, label: "Published" },
  { value: LessonStatus.HIDDEN, label: "Hidden" },
];

export default function LessonListPage() {
  const router = useRouter();
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const {
    filteredLessons,
    selectedIds,
    activeFilter,
    searchQuery,
    page,
    totalPages,
    totalItems,
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
  } = useLessons();

  const selectedCount = selectedIds.length;
  const lessonToDelete = useMemo(
    () => filteredLessons.find((lesson) => lesson.id === deleteId),
    [filteredLessons, deleteId],
  );

  return (
    <div className="min-h-screen bg-[#e9edf1]">
      <div className="flex max-w-[1800px]">
        <Sidebar
          collapsed={isSidebarCollapsed}
          onToggle={() => setIsSidebarCollapsed((v) => !v)}
        />
        <main className="w-full p-4 md:p-8">
          <div className="rounded-[28px] bg-[#f1f4f7] p-4 shadow-inner md:p-6">
            <LessonListHeader />

            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
              <div className="w-full md:max-w-md">
                <input
                  value={searchQuery}
                  onChange={(event) => setSearchQuery(event.target.value)}
                  placeholder="Search lessons..."
                  className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-700 shadow-sm outline-none transition focus:border-emerald-400 focus:ring-2 focus:ring-emerald-100"
                />
              </div>

              <div className="flex flex-wrap gap-2 rounded-2xl bg-white p-1 shadow-sm">
                {tabs.map((tab) => (
                  <button
                    key={tab.value}
                    onClick={() => setActiveFilter(tab.value)}
                    className={`rounded-xl px-5 py-2 text-sm font-semibold ${activeFilter === tab.value
                      ? "bg-emerald-500 text-white"
                      : "text-slate-600 hover:bg-slate-100"
                      }`}
                  >
                    {tab.label}
                  </button>
                ))}
              </div>

              <div className="flex items-center gap-2 text-sm">
                <Button variant="secondary" onClick={() => bulkAction("publish")}>
                  Bulk Publish
                </Button>
                <Button variant="secondary" onClick={() => bulkAction("hide")}>
                  Bulk Hide
                </Button>
                <Button variant="danger" onClick={() => bulkAction("delete")}>
                  Bulk Delete
                </Button>
              </div>
            </div>

            {selectedCount > 0 && (
              <p className="mb-4 text-sm text-slate-600">
                {selectedCount} lesson(s) selected.
              </p>
            )}

            {error && (
              <div className="mb-4 flex items-center justify-between rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
                <span>{error}</span>
                <button onClick={clearMessages} className="font-semibold">
                  Dismiss
                </button>
              </div>
            )}

            {success && (
              <div className="mb-4 flex items-center justify-between rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                <span>{success}</span>
                <button onClick={clearMessages} className="font-semibold">
                  Dismiss
                </button>
              </div>
            )}

            {loading ? (
              <div className="rounded-3xl bg-white p-10 text-center text-slate-500">
                Loading lessons...
              </div>
            ) : (
              <LessonTable
                lessons={filteredLessons}
                selectedIds={selectedIds}
                onSelectAll={selectAllVisible}
                onSelect={selectLesson}
                onToggleStatus={(lesson, next) =>
                  toggleStatus(lesson, next ? LessonStatus.PUBLISHED : LessonStatus.HIDDEN)
                }
                onEdit={(lesson) => router.push(`/admin/lessons/${lesson.id}`)}
                onDelete={(lesson) => setDeleteId(lesson.id)}
              />
            )}

            <div className="mt-4 flex items-center justify-between text-sm text-slate-600">
              <p>
                Page {Math.min(page + 1, Math.max(totalPages, 1))} / {Math.max(totalPages, 1)}
                {" • "}
                {totalItems} total lessons
              </p>
              <div className="flex items-center gap-2">
                <Button
                  variant="secondary"
                  onClick={goToPrevPage}
                  disabled={page <= 0 || loading || mutating}
                >
                  Previous
                </Button>
                <Button
                  variant="secondary"
                  onClick={goToNextPage}
                  disabled={page + 1 >= totalPages || loading || mutating}
                >
                  Next
                </Button>
              </div>
            </div>
          </div>
        </main>
      </div>

      <ConfirmationModal
        isOpen={Boolean(deleteId)}
        onClose={() => setDeleteId(null)}
        onConfirm={async () => {
          if (!deleteId) return;
          await removeLesson(deleteId);
          setDeleteId(null);
        }}
        title="Delete lesson?"
        message={
          lessonToDelete
            ? `This will permanently delete "${lessonToDelete.title}".`
            : "This action cannot be undone."
        }
        confirmText="Delete"
      />
    </div>
  );
}

