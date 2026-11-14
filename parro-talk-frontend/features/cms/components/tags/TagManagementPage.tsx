"use client";

import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDebounce } from "use-debounce";
import {
  Tag as TagIcon,
  Search,
  Plus,
  RotateCcw,
  Pencil,
  Trash2,
  CheckCircle2,
  XCircle,
  Hash,
  BookOpen,
  Filter,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import type {
  AdminTagDto,
  AdminTagCreateRequest,
  AdminTagUpdateRequest,
  CmsItemStatus,
} from "../../types";
import { tagService } from "../../services/tagService";
import TagEditorModal from "./TagEditorModal";
import Badge from "@/components/ui/Badge";
import Button from "@/components/ui/Button";
import ConfirmationModal from "@/components/ui/ConfirmationModal";
import FloatingToast from "@/components/ui/FloatingToast";
import Switch from "@/components/ui/Switch";

interface ToastState {
  message: string;
  variant: "success" | "error" | "info" | "warning";
}

export default function TagManagementPage() {
  const [tags, setTags] = useState<AdminTagDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const pageSize = 10;
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [searchInput, setSearchInput] = useState("");
  const [debouncedSearch] = useDebounce(searchInput, 300);
  const [statusFilter, setStatusFilter] = useState<"ALL" | CmsItemStatus>("ALL");

  // Modal & selection state
  const [isEditorOpen, setIsEditorOpen] = useState(false);
  const [editingTag, setEditingTag] = useState<AdminTagDto | null>(null);
  const [tagToDelete, setTagToDelete] = useState<AdminTagDto | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Toast
  const [toast, setToast] = useState<ToastState | null>(null);

  const fetchTags = useCallback(async () => {
    setLoading(true);
    try {
      const statusParam = statusFilter === "ALL" ? undefined : statusFilter;
      const response = await tagService.getTags(page, pageSize, debouncedSearch, statusParam);
      setTags(response.content || []);
      setTotalPages(response.totalPages || 0);
      setTotalElements(response.totalElements || 0);
    } catch (err: unknown) {
      console.error("Failed to load tags", err);
      setToast({
        message: "Failed to load tags. Please try again.",
        variant: "error",
      });
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, debouncedSearch, statusFilter]);

  useEffect(() => {
    fetchTags();
  }, [fetchTags]);

  // Reset to page 0 when search or filter changes
  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, statusFilter]);

  // Compute KPI stats from current tags or totals
  const stats = useMemo(() => {
    const active = tags.filter((t) => t.status === "ACTIVE").length;
    const inactive = tags.filter((t) => t.status === "INACTIVE").length;
    const inUse = tags.filter((t) => t.lessonsCount > 0).length;
    return {
      total: totalElements,
      active,
      inactive,
      inUse,
    };
  }, [tags, totalElements]);

  const handleSaveTag = async (data: AdminTagCreateRequest | AdminTagUpdateRequest) => {
    try {
      if (editingTag) {
        await tagService.updateTag(editingTag.id, data as AdminTagUpdateRequest);
        setToast({
          message: `Tag "${data.name}" updated successfully.`,
          variant: "success",
        });
      } else {
        await tagService.createTag(data as AdminTagCreateRequest);
        setToast({
          message: `Tag "${data.name}" created successfully.`,
          variant: "success",
        });
      }
      setIsEditorOpen(false);
      setEditingTag(null);
      fetchTags();
    } catch (err: unknown) {
      let msg = "Failed to save tag.";
      if (err instanceof Error) msg = err.message;
      setToast({ message: msg, variant: "error" });
      throw err;
    }
  };

  const handleToggleStatus = async (tag: AdminTagDto) => {
    const nextStatus: CmsItemStatus = tag.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    try {
      await tagService.updateTag(tag.id, {
        name: tag.name,
        slug: tag.slug,
        color: tag.color || undefined,
        description: tag.description || undefined,
        status: nextStatus,
      });
      setTags((prev) =>
        prev.map((t) => (t.id === tag.id ? { ...t, status: nextStatus } : t))
      );
      setToast({
        message: `Tag status changed to ${nextStatus}.`,
        variant: "info",
      });
    } catch (err: unknown) {
      let msg = "Failed to update tag status.";
      if (err instanceof Error) msg = err.message;
      setToast({ message: msg, variant: "error" });
    }
  };

  const confirmDelete = async () => {
    if (!tagToDelete) return;
    setIsDeleting(true);

    // If tag is in use, clicking action deactivates it instead of failing deletion
    if (tagToDelete.lessonsCount > 0) {
      try {
        await tagService.updateTag(tagToDelete.id, {
          name: tagToDelete.name,
          slug: tagToDelete.slug,
          color: tagToDelete.color || undefined,
          description: tagToDelete.description || undefined,
          status: "INACTIVE",
        });
        setTags((prev) =>
          prev.map((t) => (t.id === tagToDelete.id ? { ...t, status: "INACTIVE" } : t))
        );
        setToast({
          message: `Tag "${tagToDelete.name}" was deactivated because it is in use by ${tagToDelete.lessonsCount} lesson(s).`,
          variant: "info",
        });
        setTagToDelete(null);
      } catch (err: unknown) {
        let msg = "Failed to deactivate tag.";
        if (err instanceof Error) msg = err.message;
        setToast({ message: msg, variant: "error" });
      } finally {
        setIsDeleting(false);
      }
      return;
    }

    try {
      await tagService.deleteTag(tagToDelete.id);
      setToast({
        message: `Tag "${tagToDelete.name}" deleted successfully.`,
        variant: "success",
      });
      setTagToDelete(null);
      fetchTags();
    } catch (err: unknown) {
      let msg = "Failed to delete tag.";
      if (err instanceof Error) msg = err.message;
      if (err && typeof err === "object" && "response" in err) {
        const res = (err as { response?: { data?: { message?: string } } }).response;
        if (res?.data?.message) msg = res.data.message;
      }
      setToast({ message: msg, variant: "error" });
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Toast Feedback */}
      {toast && (
        <FloatingToast
          message={toast.message}
          variant={toast.variant}
          onClose={() => setToast(null)}
        />
      )}

      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
              <Hash className="w-3.5 h-3.5" /> CMS Taxonomy
            </span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-gray-900 dark:text-white mt-1">
            Lesson Tags
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
            Organize and classify lessons with custom color-coded tags and filters.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <Button
            variant="outline"
            onClick={() => fetchTags()}
            disabled={loading}
            className="flex items-center gap-1.5"
            aria-label="Refresh tags list"
          >
            <RotateCcw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </Button>

          <Button
            variant="primary"
            onClick={() => {
              setEditingTag(null);
              setIsEditorOpen(true);
            }}
            className="flex items-center gap-1.5 shadow-sm"
          >
            <Plus className="w-4 h-4" />
            Add New Tag
          </Button>
        </div>
      </div>

      {/* KPI Stats Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-4 rounded-xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-sm">
          <div className="flex items-center justify-between text-gray-500 dark:text-gray-400">
            <span className="text-xs font-medium uppercase tracking-wider">Total Tags</span>
            <TagIcon className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
          </div>
          <div className="mt-2 text-2xl font-bold text-gray-900 dark:text-white">
            {totalElements}
          </div>
          <div className="mt-1 text-xs text-gray-500 dark:text-gray-400">Across all taxonomies</div>
        </div>

        <div className="p-4 rounded-xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-sm">
          <div className="flex items-center justify-between text-gray-500 dark:text-gray-400">
            <span className="text-xs font-medium uppercase tracking-wider">Active Tags</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-500" />
          </div>
          <div className="mt-2 text-2xl font-bold text-emerald-600 dark:text-emerald-400">
            {stats.active}
          </div>
          <div className="mt-1 text-xs text-gray-500 dark:text-gray-400">Visible for lesson tagging</div>
        </div>

        <div className="p-4 rounded-xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-sm">
          <div className="flex items-center justify-between text-gray-500 dark:text-gray-400">
            <span className="text-xs font-medium uppercase tracking-wider">Inactive Tags</span>
            <XCircle className="w-4 h-4 text-gray-400" />
          </div>
          <div className="mt-2 text-2xl font-bold text-gray-700 dark:text-gray-300">
            {stats.inactive}
          </div>
          <div className="mt-1 text-xs text-gray-500 dark:text-gray-400">Hidden from selectors</div>
        </div>

        <div className="p-4 rounded-xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-sm">
          <div className="flex items-center justify-between text-gray-500 dark:text-gray-400">
            <span className="text-xs font-medium uppercase tracking-wider">Tags In Use</span>
            <BookOpen className="w-4 h-4 text-indigo-500" />
          </div>
          <div className="mt-2 text-2xl font-bold text-indigo-600 dark:text-indigo-400">
            {stats.inUse}
          </div>
          <div className="mt-1 text-xs text-gray-500 dark:text-gray-400">Assigned to ≥ 1 lesson</div>
        </div>
      </div>

      {/* Main Table Card */}
      <div className="rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-sm overflow-hidden">
        {/* Search & Filter Bar */}
        <div className="p-4 border-b border-gray-200 dark:border-gray-800 flex flex-col md:flex-row items-center justify-between gap-3 bg-gray-50/60 dark:bg-gray-800/40">
          {/* Search Box */}
          <div className="relative w-full md:w-80">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search tags by name or slug..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              className="w-full pl-9 pr-4 py-2 text-sm rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-950 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500"
            />
          </div>

          {/* Status Filters */}
          <div className="flex items-center gap-1.5 w-full md:w-auto overflow-x-auto pb-1 md:pb-0">
            <span className="text-xs font-medium text-gray-500 dark:text-gray-400 mr-1 inline-flex items-center gap-1">
              <Filter className="w-3.5 h-3.5" /> Status:
            </span>
            {(["ALL", "ACTIVE", "INACTIVE"] as const).map((st) => (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-colors ${
                  statusFilter === st
                    ? "bg-emerald-600 text-white shadow-sm"
                    : "bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-700"
                }`}
              >
                {st === "ALL" ? "All Tags" : st === "ACTIVE" ? "Active Only" : "Inactive"}
              </button>
            ))}
          </div>
        </div>

        {/* Tags Table */}
        <div className="overflow-x-auto" tabIndex={0} role="region" aria-label="Tags Table">
          <table className="w-full text-left text-sm text-gray-700 dark:text-gray-300">
            <caption className="sr-only">List of lesson classification tags</caption>
            <thead className="bg-gray-50 dark:bg-gray-800/70 text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400 border-b border-gray-200 dark:border-gray-800">
              <tr>
                <th scope="col" className="py-3.5 px-4">Tag</th>
                <th scope="col" className="py-3.5 px-4">Slug</th>
                <th scope="col" className="py-3.5 px-4">Description</th>
                <th scope="col" className="py-3.5 px-4 text-center">Status</th>
                <th scope="col" className="py-3.5 px-4 text-center">Lessons</th>
                <th scope="col" className="py-3.5 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
              {loading ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center">
                    <div className="flex flex-col items-center justify-center gap-3">
                      <div className="flex items-center gap-2">
                        <div className="w-3 h-3 rounded-full bg-emerald-500 animate-bounce" />
                        <div className="w-3 h-3 rounded-full bg-emerald-500 animate-bounce [animation-delay:-0.15s]" />
                        <div className="w-3 h-3 rounded-full bg-emerald-500 animate-bounce [animation-delay:-0.3s]" />
                      </div>
                      <span className="text-xs text-gray-500 dark:text-gray-400">Loading tags...</span>
                    </div>
                  </td>
                </tr>
              ) : tags.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center">
                    <div className="flex flex-col items-center justify-center gap-2 text-gray-500 dark:text-gray-400">
                      <TagIcon className="w-8 h-8 stroke-1 text-gray-400" />
                      <p className="font-medium text-gray-700 dark:text-gray-300">No tags found</p>
                      <p className="text-xs">
                        {debouncedSearch
                          ? `No tags matched "${debouncedSearch}". Try clearing your search.`
                          : "Get started by creating your first lesson tag."}
                      </p>
                    </div>
                  </td>
                </tr>
              ) : (
                tags.map((item) => (
                  <tr
                    key={item.id}
                    className="hover:bg-gray-50/80 dark:hover:bg-gray-800/50 transition-colors group"
                  >
                    {/* Tag badge & preview */}
                    <td className="py-3.5 px-4">
                      <div className="flex items-center gap-2.5">
                        <span
                          className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold shadow-xs"
                          style={{
                            backgroundColor: `${item.color || "#10b981"}18`,
                            color: item.color || "#10b981",
                            border: `1px solid ${item.color || "#10b981"}40`,
                          }}
                        >
                          <span
                            className="w-2 h-2 rounded-full"
                            style={{ backgroundColor: item.color || "#10b981" }}
                          />
                          {item.name}
                        </span>
                      </div>
                    </td>

                    {/* Slug */}
                    <td className="py-3.5 px-4">
                      <code className="text-xs font-mono text-gray-500 dark:text-gray-400 bg-gray-100 dark:bg-gray-800 px-2 py-0.5 rounded">
                        {item.slug}
                      </code>
                    </td>

                    {/* Description */}
                    <td className="py-3.5 px-4 max-w-xs truncate text-xs text-gray-500 dark:text-gray-400">
                      {item.description || <span className="italic text-gray-400">No description</span>}
                    </td>

                    {/* Status */}
                    <td className="py-3.5 px-4 text-center">
                      <div className="inline-flex items-center gap-2">
                        <Switch
                          checked={item.status === "ACTIVE"}
                          onChange={() => void handleToggleStatus(item)}
                          aria-label={`Toggle ${item.name} status`}
                        />
                        <Badge variant={item.status === "ACTIVE" ? "success" : "default"}>
                          {item.status}
                        </Badge>
                      </div>
                    </td>

                    {/* Lessons count */}
                    <td className="py-3.5 px-4 text-center">
                      <span className="inline-flex items-center gap-1 text-xs font-semibold text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-800 px-2.5 py-1 rounded-full">
                        <BookOpen className="w-3 h-3 text-emerald-500" />
                        {item.lessonsCount ?? 0}
                      </span>
                    </td>

                    {/* Actions */}
                    <td className="py-3.5 px-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => {
                            setEditingTag(item);
                            setIsEditorOpen(true);
                          }}
                          className="p-1.5 rounded-lg text-gray-500 hover:text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-950/40 transition-colors"
                          title="Edit Tag"
                          aria-label={`Edit ${item.name}`}
                        >
                          <Pencil className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => setTagToDelete(item)}
                          className="p-1.5 rounded-lg text-gray-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/40 transition-colors"
                          title={
                            item.lessonsCount > 0
                              ? `Tag in use by ${item.lessonsCount} lesson(s) - click to deactivate`
                              : "Delete Tag"
                          }
                          aria-label={`Delete ${item.name}`}
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        {totalPages > 1 && (
          <div className="p-4 border-t border-gray-200 dark:border-gray-800 flex flex-col sm:flex-row items-center justify-between gap-3 text-sm text-gray-500 dark:text-gray-400 bg-gray-50/50 dark:bg-gray-800/30">
            <div>
              Showing page <span className="font-medium text-gray-900 dark:text-white">{page + 1}</span> of{" "}
              <span className="font-medium text-gray-900 dark:text-white">{totalPages}</span> ({totalElements} total tags)
            </div>

            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0 || loading}
                className="flex items-center gap-1"
              >
                <ChevronLeft className="w-4 h-4" /> Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1 || loading}
                className="flex items-center gap-1"
              >
                Next <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Editor Modal */}
      <TagEditorModal
        isOpen={isEditorOpen}
        onClose={() => {
          setIsEditorOpen(false);
          setEditingTag(null);
        }}
        onSave={handleSaveTag}
        tag={editingTag}
      />

      {/* Delete / Deactivate Confirmation Modal */}
      <ConfirmationModal
        isOpen={!!tagToDelete}
        title={tagToDelete?.lessonsCount ? "Tag In Use" : "Delete Tag"}
        message={
          tagToDelete
            ? tagToDelete.lessonsCount > 0
              ? `Tag "${tagToDelete.name}" is currently assigned to ${tagToDelete.lessonsCount} lesson(s). To protect lesson taxonomy integrity, tags in use cannot be permanently deleted. Would you like to deactivate this tag instead to hide it from future selection?`
              : `Are you sure you want to permanently delete tag "${tagToDelete.name}"? This action cannot be undone.`
            : ""
        }
        confirmText={tagToDelete?.lessonsCount ? "Deactivate Tag" : "Delete Tag"}
        cancelText="Cancel"
        isLoading={isDeleting}
        onConfirm={confirmDelete}
        onClose={() => setTagToDelete(null)}
      />
    </div>
  );
}
