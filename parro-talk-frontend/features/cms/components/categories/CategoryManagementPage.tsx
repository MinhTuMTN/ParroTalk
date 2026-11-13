"use client";

import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDebounce } from "use-debounce";
import {
  ArrowDown,
  ArrowUp,
  BookOpen,
  ChevronDown,
  ChevronRight,
  ChevronsDownUp,
  ChevronsUpDown,
  Folder,
  FolderPlus,
  FolderTree,
  Pencil,
  Plus,
  RotateCcw,
  Search,
  Trash2,
  X,
} from "lucide-react";
import type {
  AdminCategoryCreateRequest,
  AdminCategoryDto,
  AdminCategoryUpdateRequest,
  CmsItemStatus,
} from "../../types";
import {
  categoryService,
  extractCategoryErrorMessage,
} from "../../services/categoryService";
import {
  collectExpandableIds,
  computeCategoryStats,
  filterCategoryTree,
} from "./categoryTreeUtils";
import { renderCategoryIcon } from "./CategoryIconPicker";
import CategoryEditorDrawer from "./CategoryEditorDrawer";
import Badge from "@/components/ui/Badge";
import Button from "@/components/ui/Button";
import ConfirmationModal from "@/components/ui/ConfirmationModal";
import FloatingToast from "@/components/ui/FloatingToast";
import Switch from "@/components/ui/Switch";

interface ToastState {
  message: string;
  variant: "success" | "error" | "info" | "warning";
}

function HighlightText({ text, query }: { text: string; query: string }) {
  const trimmed = query.trim();
  if (!trimmed) return <>{text}</>;

  const lowerText = text.toLowerCase();
  const lowerQuery = trimmed.toLowerCase();
  const idx = lowerText.indexOf(lowerQuery);
  if (idx === -1) return <>{text}</>;

  const before = text.slice(0, idx);
  const match = text.slice(idx, idx + trimmed.length);
  const after = text.slice(idx + trimmed.length);

  return (
    <>
      {before}
      <mark className="rounded bg-emerald-100 px-0.5 text-emerald-950">{match}</mark>
      {after}
    </>
  );
}

function updateNodeStatusInTree(
  nodes: AdminCategoryDto[],
  id: string,
  status: CmsItemStatus
): AdminCategoryDto[] {
  return nodes.map((node) => ({
    ...node,
    status: node.id === id ? status : node.status,
    children: node.children ? updateNodeStatusInTree(node.children, id, status) : [],
  }));
}

function swapSiblingsInTree(
  nodes: AdminCategoryDto[],
  parentId: string | null,
  fromIndex: number,
  toIndex: number
): AdminCategoryDto[] {
  if (parentId === null) {
    const next = [...nodes];
    const [moved] = next.splice(fromIndex, 1);
    next.splice(toIndex, 0, moved);
    return next.map((item, idx) => ({ ...item, sortOrder: idx }));
  }

  return nodes.map((node) => {
    if (node.id === parentId) {
      const siblings = [...(node.children ?? [])];
      const [moved] = siblings.splice(fromIndex, 1);
      siblings.splice(toIndex, 0, moved);
      return {
        ...node,
        children: siblings.map((item, idx) => ({ ...item, sortOrder: idx })),
      };
    }
    if (node.children && node.children.length > 0) {
      return {
        ...node,
        children: swapSiblingsInTree(node.children, parentId, fromIndex, toIndex),
      };
    }
    return node;
  });
}

export default function CategoryManagementPage() {
  const [categories, setCategories] = useState<AdminCategoryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [reordering, setReordering] = useState(false);

  // Filters & search
  const [searchInput, setSearchInput] = useState("");
  const [debouncedSearch] = useDebounce(searchInput, 250);
  const [statusFilter, setStatusFilter] = useState<"ALL" | CmsItemStatus>("ALL");

  // Tree expansion state
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());

  // Drawer state
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<AdminCategoryDto | null>(null);
  const [defaultParentId, setDefaultParentId] = useState<string>("");
  const [defaultSortOrder, setDefaultSortOrder] = useState<number>(0);

  // Delete confirmation modal
  const [deletingCategory, setDeletingCategory] = useState<AdminCategoryDto | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Toast & live region announcement
  const [toast, setToast] = useState<ToastState | null>(null);
  const [srStatus, setSrStatus] = useState("");

  const showToast = useCallback(
    (
      message: string,
      variant: "success" | "error" | "info" | "warning" = "success"
    ) => {
      setToast({ message, variant });
      setTimeout(() => {
        setToast((current) => (current?.message === message ? null : current));
      }, 4000);
    },
    []
  );

  const fetchTree = useCallback(
    async (expandAllOnFirstLoad = false) => {
      setLoading(true);
      try {
        const tree = await categoryService.getCategoryTree();
        setCategories(tree);
        if (expandAllOnFirstLoad) {
          setExpandedIds(collectExpandableIds(tree));
        }
      } catch (err) {
        console.error("Failed to load category tree", err);
        showToast(
          extractCategoryErrorMessage(err, "Failed to load lesson categories."),
          "error"
        );
      } finally {
        setLoading(false);
      }
    },
    [showToast]
  );

  useEffect(() => {
    void fetchTree(true);
  }, [fetchTree]);

  const stats = useMemo(() => computeCategoryStats(categories), [categories]);

  const { filteredTree, autoExpandedIds, matchingIds } = useMemo(
    () => filterCategoryTree(categories, debouncedSearch, statusFilter),
    [categories, debouncedSearch, statusFilter]
  );

  const isFiltered = debouncedSearch.trim().length > 0 || statusFilter !== "ALL";

  const effectiveExpandedIds = useMemo(() => {
    if (!isFiltered) return expandedIds;
    const merged = new Set(expandedIds);
    autoExpandedIds.forEach((id) => merged.add(id));
    return merged;
  }, [expandedIds, autoExpandedIds, isFiltered]);

  const toggleExpand = (id: string) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const handleExpandAll = () => {
    setExpandedIds(collectExpandableIds(categories));
  };

  const handleCollapseAll = () => {
    setExpandedIds(new Set());
  };

  const handleOpenCreateRoot = () => {
    setEditingCategory(null);
    setDefaultParentId("");
    setDefaultSortOrder(categories.length);
    setIsDrawerOpen(true);
  };

  const handleOpenCreateChild = (parent: AdminCategoryDto) => {
    setEditingCategory(null);
    setDefaultParentId(parent.id);
    setDefaultSortOrder(parent.children?.length ?? 0);
    setExpandedIds((prev) => new Set(prev).add(parent.id));
    setIsDrawerOpen(true);
  };

  const handleOpenEdit = (category: AdminCategoryDto) => {
    setEditingCategory(category);
    setDefaultParentId(category.parentCategoryId ?? "");
    setDefaultSortOrder(category.sortOrder ?? 0);
    setIsDrawerOpen(true);
  };

  const handleSaveCategory = async (data: AdminCategoryCreateRequest) => {
    if (editingCategory) {
      await categoryService.updateCategory(
        editingCategory.id,
        data as AdminCategoryUpdateRequest
      );
      showToast(`Updated "${data.name}".`, "success");
    } else {
      await categoryService.createCategory(data);
      showToast(`Created "${data.name}".`, "success");
    }
    await fetchTree(false);
  };

  const handleToggleStatus = async (
    category: AdminCategoryDto,
    nextChecked: boolean
  ) => {
    const nextStatus: CmsItemStatus = nextChecked ? "ACTIVE" : "INACTIVE";
    const previous = categories;

    setCategories((prev) => updateNodeStatusInTree(prev, category.id, nextStatus));

    try {
      await categoryService.updateCategoryStatus(category.id, nextStatus);
      showToast(
        `"${category.name}" is now ${nextStatus.toLowerCase()}.`,
        "success"
      );
    } catch (err) {
      setCategories(previous);
      showToast(
        extractCategoryErrorMessage(err, "Failed to update category status."),
        "error"
      );
    }
  };

  const handleReorder = async (
    siblings: AdminCategoryDto[],
    parentId: string | null,
    index: number,
    direction: "up" | "down"
  ) => {
    if (reordering || isFiltered) return;
    const targetIndex = direction === "up" ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= siblings.length) return;

    const previous = categories;
    const reorderedSiblings = [...siblings];
    const [moved] = reorderedSiblings.splice(index, 1);
    reorderedSiblings.splice(targetIndex, 0, moved);

    setCategories((prev) => swapSiblingsInTree(prev, parentId, index, targetIndex));
    setReordering(true);

    try {
      await categoryService.reorderCategories({
        parentCategoryId: parentId,
        orderedIds: reorderedSiblings.map((item) => item.id),
      });
      setSrStatus(
        `Moved "${moved.name}" to position ${targetIndex + 1} of ${reorderedSiblings.length}.`
      );
      showToast(`Updated order for "${moved.name}".`, "success");
    } catch (err) {
      setCategories(previous);
      showToast(
        extractCategoryErrorMessage(err, "Failed to reorder categories."),
        "error"
      );
    } finally {
      setReordering(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!deletingCategory) return;
    setIsDeleting(true);
    try {
      await categoryService.deleteCategory(deletingCategory.id);
      showToast(`Deleted "${deletingCategory.name}".`, "success");
      setDeletingCategory(null);
      await fetchTree(false);
    } catch (err) {
      showToast(
        extractCategoryErrorMessage(err, "Failed to delete category."),
        "error"
      );
      setDeletingCategory(null);
    } finally {
      setIsDeleting(false);
    }
  };

  const resetFilters = () => {
    setSearchInput("");
    setStatusFilter("ALL");
  };

  const renderTreeRows = (
    items: AdminCategoryDto[],
    parentId: string | null = null,
    level = 0
  ): React.ReactNode => {
    return items.map((item, index) => {
      const children = item.children ?? [];
      const hasChildren = children.length > 0;
      const isExpanded = effectiveExpandedIds.has(item.id);
      const isSearchHit = matchingIds.has(item.id);
      const accentColor =
        item.color && /^#[0-9a-fA-F]{6}$/.test(item.color) ? item.color : "#059669";
      const isFirst = index === 0;
      const isLast = index === items.length - 1;

      return (
        <React.Fragment key={item.id}>
          <tr
            className={`group border-b border-slate-100 transition-colors motion-reduce:transition-none hover:bg-slate-50/80 ${
              isSearchHit ? "bg-emerald-50/40" : ""
            }`}
          >
            {/* Category Name & Hierarchy */}
            <td className="py-3.5 pl-4 pr-3">
              <div
                className="flex items-center gap-2.5"
                style={{ paddingLeft: `${level * 1.5}rem` }}
              >
                {level > 0 ? (
                  <span
                    aria-hidden="true"
                    className="h-4 w-3 shrink-0 border-b-2 border-l-2 border-slate-200 rounded-bl-md -mt-2"
                  />
                ) : null}

                {hasChildren ? (
                  <button
                    type="button"
                    aria-expanded={isExpanded}
                    aria-label={`${isExpanded ? "Collapse" : "Expand"} ${item.name}`}
                    onClick={() => toggleExpand(item.id)}
                    className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg text-slate-500 transition hover:bg-slate-200/70 hover:text-slate-900 cursor-pointer outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
                  >
                    {isExpanded ? (
                      <ChevronDown className="h-4 w-4" aria-hidden="true" />
                    ) : (
                      <ChevronRight className="h-4 w-4" aria-hidden="true" />
                    )}
                  </button>
                ) : (
                  <span className="h-7 w-7 shrink-0" aria-hidden="true" />
                )}

                <div
                  className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border"
                  style={{
                    backgroundColor: `${accentColor}15`,
                    borderColor: `${accentColor}35`,
                    color: accentColor,
                  }}
                >
                  {renderCategoryIcon(item.icon, "h-4 w-4")}
                </div>

                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-semibold text-slate-900 group-hover:text-emerald-700 transition-colors">
                      <HighlightText text={item.name} query={debouncedSearch} />
                    </span>
                    {hasChildren ? (
                      <span className="inline-flex items-center rounded-md bg-slate-100 px-1.5 py-0.5 text-[11px] font-semibold text-slate-600">
                        {children.length} sub
                      </span>
                    ) : null}
                  </div>
                  {item.nameVi ? (
                    <p className="truncate text-xs text-slate-500">
                      <HighlightText text={item.nameVi} query={debouncedSearch} />
                    </p>
                  ) : null}
                  {item.description ? (
                    <p className="hidden sm:block max-w-md truncate text-xs text-slate-400">
                      {item.description}
                    </p>
                  ) : null}
                </div>
              </div>
            </td>

            {/* Slug */}
            <td className="hidden md:table-cell py-3.5 px-3">
              <span className="inline-block rounded-lg bg-slate-100 px-2.5 py-1 font-mono text-xs text-slate-600">
                <HighlightText text={item.slug} query={debouncedSearch} />
              </span>
            </td>

            {/* Lessons Count */}
            <td className="py-3.5 px-3 text-center">
              <span
                className={`inline-flex items-center gap-1 rounded-lg px-2.5 py-1 text-xs font-bold ${
                  (item.lessonsCount ?? 0) > 0
                    ? "bg-emerald-50 text-emerald-700 border border-emerald-200/70"
                    : "bg-slate-100 text-slate-500"
                }`}
              >
                <BookOpen className="h-3 w-3" aria-hidden="true" />
                {item.lessonsCount ?? 0}
              </span>
            </td>

            {/* Order */}
            <td className="hidden sm:table-cell py-3.5 px-3 text-center">
              <div className="inline-flex items-center gap-1">
                <button
                  type="button"
                  title={
                    isFiltered
                      ? "Clear search/filter to reorder"
                      : `Move ${item.name} up`
                  }
                  aria-label={`Move ${item.name} up`}
                  disabled={isFirst || isFiltered || reordering}
                  onClick={() => handleReorder(items, parentId, index, "up")}
                  className="rounded-lg p-1 text-slate-400 transition hover:bg-emerald-50 hover:text-emerald-700 disabled:cursor-not-allowed disabled:opacity-25 cursor-pointer outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
                >
                  <ArrowUp className="h-3.5 w-3.5" aria-hidden="true" />
                </button>
                <span className="inline-flex h-6 min-w-6 items-center justify-center rounded-md bg-slate-100 px-1.5 text-xs font-bold text-slate-700">
                  {item.sortOrder ?? index}
                </span>
                <button
                  type="button"
                  title={
                    isFiltered
                      ? "Clear search/filter to reorder"
                      : `Move ${item.name} down`
                  }
                  aria-label={`Move ${item.name} down`}
                  disabled={isLast || isFiltered || reordering}
                  onClick={() => handleReorder(items, parentId, index, "down")}
                  className="rounded-lg p-1 text-slate-400 transition hover:bg-emerald-50 hover:text-emerald-700 disabled:cursor-not-allowed disabled:opacity-25 cursor-pointer outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
                >
                  <ArrowDown className="h-3.5 w-3.5" aria-hidden="true" />
                </button>
              </div>
            </td>

            {/* Status Switch */}
            <td className="py-3.5 px-3 text-center">
              <div className="flex flex-col items-center gap-1">
                <Switch
                  checked={item.status === "ACTIVE"}
                  onChange={(next) => handleToggleStatus(item, next)}
                />
                <span className="sr-only">{item.status}</span>
              </div>
            </td>

            {/* Actions */}
            <td className="py-3.5 pl-3 pr-4 text-right">
              <div className="inline-flex items-center justify-end gap-1">
                <button
                  type="button"
                  title={`Add subcategory under ${item.name}`}
                  aria-label={`Add subcategory under ${item.name}`}
                  onClick={() => handleOpenCreateChild(item)}
                  className="rounded-xl p-2 text-slate-500 transition hover:bg-emerald-50 hover:text-emerald-700 cursor-pointer outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
                >
                  <FolderPlus className="h-4 w-4" aria-hidden="true" />
                </button>
                <button
                  type="button"
                  title={`Edit ${item.name}`}
                  aria-label={`Edit ${item.name}`}
                  onClick={() => handleOpenEdit(item)}
                  className="rounded-xl p-2 text-slate-500 transition hover:bg-indigo-50 hover:text-indigo-700 cursor-pointer outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
                >
                  <Pencil className="h-4 w-4" aria-hidden="true" />
                </button>
                <button
                  type="button"
                  title={`Delete ${item.name}`}
                  aria-label={`Delete ${item.name}`}
                  onClick={() => setDeletingCategory(item)}
                  className="rounded-xl p-2 text-slate-500 transition hover:bg-rose-50 hover:text-rose-700 cursor-pointer outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
                >
                  <Trash2 className="h-4 w-4" aria-hidden="true" />
                </button>
              </div>
            </td>
          </tr>

          {hasChildren && isExpanded
            ? renderTreeRows(children, item.id, level + 1)
            : null}
        </React.Fragment>
      );
    });
  };

  return (
    <div className="mx-auto max-w-7xl space-y-6 pb-12">
      {toast ? (
        <FloatingToast
          message={toast.message}
          variant={toast.variant}
          onClose={() => setToast(null)}
        />
      ) : null}

      <p role="status" className="sr-only">
        {srStatus}
      </p>

      {/* Header Banner */}
      <div className="flex flex-col justify-between gap-4 rounded-3xl border border-slate-100 bg-white p-6 shadow-sm sm:flex-row sm:items-center sm:p-8">
        <div className="flex items-start gap-4">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-tr from-emerald-600 to-teal-500 text-white shadow-lg shadow-emerald-500/20">
            <FolderTree className="h-6 w-6" aria-hidden="true" />
          </div>
          <div>
            <div className="flex flex-wrap items-center gap-3">
              <h1
                id="categories-table-heading"
                className="text-2xl font-black tracking-tight text-slate-900"
              >
                Lesson Categories
              </h1>
              <Badge tone="success">
                {stats.active} / {stats.total} Active
              </Badge>
            </div>
            <p className="mt-1 max-w-2xl text-sm text-slate-500">
              Organize hierarchical lesson topics, bilingual Vietnamese labels, visual icons, and display order across the library.
            </p>
          </div>
        </div>

        <Button
          onClick={handleOpenCreateRoot}
          leftIcon={<Plus className="h-4 w-4" aria-hidden="true" />}
          className="shrink-0"
        >
          Add Category
        </Button>
      </div>

      {/* Stats Strip */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <div className="flex flex-col justify-between rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
            Total Categories
          </span>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-2xl font-black text-slate-900">{stats.total}</span>
            <span className="rounded-md bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">
              {stats.rootCount} root
            </span>
          </div>
        </div>

        <div className="flex flex-col justify-between rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
            Active
          </span>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-2xl font-black text-emerald-700">{stats.active}</span>
            <span className="rounded-md bg-emerald-50 px-2 py-0.5 text-xs font-semibold text-emerald-700">
              Visible
            </span>
          </div>
        </div>

        <div className="flex flex-col justify-between rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
            Inactive
          </span>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-2xl font-black text-slate-700">{stats.inactive}</span>
            <span className="rounded-md bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">
              Hidden
            </span>
          </div>
        </div>

        <div className="flex flex-col justify-between rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
            Without Lessons
          </span>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-2xl font-black text-amber-700">
              {stats.withoutLessons}
            </span>
            <Folder className="h-4 w-4 text-amber-500" aria-hidden="true" />
          </div>
        </div>
      </div>

      {/* Toolbar */}
      <div className="flex flex-col gap-3 rounded-2xl border border-slate-100 bg-white p-4 shadow-sm lg:flex-row lg:items-center lg:justify-between">
        <div className="relative w-full lg:max-w-sm">
          <Search
            className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
            aria-hidden="true"
          />
          <input
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            aria-label="Search categories by English name, Vietnamese name, or slug"
            placeholder="Search name, tên tiếng Việt, or slug..."
            className="w-full rounded-xl border border-slate-200 bg-slate-50/60 py-2 pl-10 pr-9 text-sm text-slate-800 outline-none transition focus:border-emerald-500 focus:bg-white focus:ring-2 focus:ring-emerald-100"
          />
          {searchInput ? (
            <button
              type="button"
              onClick={() => setSearchInput("")}
              aria-label="Clear search"
              className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded-md p-1 text-slate-400 hover:text-slate-700 cursor-pointer"
            >
              <X className="h-3.5 w-3.5" aria-hidden="true" />
            </button>
          ) : null}
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3">
          {/* Status Filter Tabs */}
          <div
            role="group"
            aria-label="Filter by status"
            className="flex items-center gap-1 rounded-xl bg-slate-100 p-1"
          >
            {(
              [
                { key: "ALL", label: "All", count: stats.total },
                { key: "ACTIVE", label: "Active", count: stats.active },
                { key: "INACTIVE", label: "Inactive", count: stats.inactive },
              ] as const
            ).map((tab) => {
              const selected = statusFilter === tab.key;
              return (
                <button
                  key={tab.key}
                  type="button"
                  aria-pressed={selected}
                  onClick={() => setStatusFilter(tab.key)}
                  className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-bold transition cursor-pointer ${
                    selected
                      ? "bg-white text-slate-900 shadow-xs"
                      : "text-slate-600 hover:text-slate-900"
                  }`}
                >
                  <span>{tab.label}</span>
                  <span
                    className={`rounded-md px-1.5 py-0.5 text-[10px] ${
                      selected
                        ? "bg-emerald-100 text-emerald-800"
                        : "bg-slate-200/80 text-slate-600"
                    }`}
                  >
                    {tab.count}
                  </span>
                </button>
              );
            })}
          </div>

          {/* Expand / Collapse */}
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={handleExpandAll}
              className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-600 transition hover:bg-slate-50 hover:text-slate-900 cursor-pointer"
            >
              <ChevronsUpDown className="h-3.5 w-3.5" aria-hidden="true" />
              Expand all
            </button>
            <button
              type="button"
              onClick={handleCollapseAll}
              className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-600 transition hover:bg-slate-50 hover:text-slate-900 cursor-pointer"
            >
              <ChevronsDownUp className="h-3.5 w-3.5" aria-hidden="true" />
              Collapse all
            </button>
          </div>
        </div>
      </div>

      {/* Tree Table Card */}
      <div className="overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-sm">
        {loading ? (
          <div className="divide-y divide-slate-100 p-4" aria-busy="true" aria-label="Loading categories">
            {Array.from({ length: 6 }).map((_, idx) => (
              <div key={idx} className="flex items-center justify-between py-4 px-2 animate-pulse motion-reduce:animate-none">
                <div className="flex items-center gap-3">
                  <div className="h-9 w-9 rounded-xl bg-slate-200" />
                  <div className="space-y-2">
                    <div className="h-4 w-44 rounded bg-slate-200" />
                    <div className="h-3 w-28 rounded bg-slate-100" />
                  </div>
                </div>
                <div className="hidden md:block h-5 w-28 rounded-lg bg-slate-100" />
                <div className="h-6 w-12 rounded-lg bg-slate-100" />
                <div className="h-7 w-24 rounded-xl bg-slate-100" />
              </div>
            ))}
          </div>
        ) : filteredTree.length === 0 ? (
          <div className="mx-auto flex max-w-md flex-col items-center justify-center p-14 text-center">
            <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100 text-slate-400">
              <FolderTree className="h-7 w-7" aria-hidden="true" />
            </div>
            <h2 className="text-base font-bold text-slate-900">
              {isFiltered ? "No matching categories found" : "No lesson categories yet"}
            </h2>
            <p className="mt-1 mb-5 text-xs text-slate-500">
              {isFiltered
                ? "Try a different keyword or reset the status filter to view the full category tree."
                : "Create your first root category to start organizing lessons in the library."}
            </p>
            {isFiltered ? (
              <Button
                variant="secondary"
                size="sm"
                onClick={resetFilters}
                leftIcon={<RotateCcw className="h-3.5 w-3.5" aria-hidden="true" />}
              >
                Reset filters
              </Button>
            ) : (
              <Button
                size="sm"
                onClick={handleOpenCreateRoot}
                leftIcon={<Plus className="h-4 w-4" aria-hidden="true" />}
              >
                Create first category
              </Button>
            )}
          </div>
        ) : (
          <div
            tabIndex={0}
            role="region"
            aria-labelledby="categories-table-heading"
            className="overflow-x-auto outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
          >
            <table className="w-full border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50/80 text-xs font-bold uppercase tracking-wider text-slate-500">
                  <th scope="col" className="py-3.5 pl-4 pr-3">
                    Category
                  </th>
                  <th scope="col" className="hidden md:table-cell py-3.5 px-3">
                    Slug
                  </th>
                  <th scope="col" className="py-3.5 px-3 text-center">
                    Lessons
                  </th>
                  <th scope="col" className="hidden sm:table-cell py-3.5 px-3 text-center">
                    Order
                  </th>
                  <th scope="col" className="py-3.5 px-3 text-center">
                    Active
                  </th>
                  <th scope="col" className="py-3.5 pl-3 pr-4 text-right">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody>{renderTreeRows(filteredTree, null, 0)}</tbody>
            </table>
          </div>
        )}
      </div>

      {/* Editor Drawer */}
      <CategoryEditorDrawer
        isOpen={isDrawerOpen}
        onClose={() => {
          setIsDrawerOpen(false);
          setEditingCategory(null);
        }}
        onSave={handleSaveCategory}
        category={editingCategory}
        defaultParentId={defaultParentId}
        defaultSortOrder={defaultSortOrder}
        categories={categories}
      />

      {/* Delete Confirmation Modal */}
      <ConfirmationModal
        isOpen={Boolean(deletingCategory)}
        onClose={() => setDeletingCategory(null)}
        onConfirm={handleConfirmDelete}
        title="Delete Lesson Category?"
        message={
          deletingCategory
            ? deletingCategory.children && deletingCategory.children.length > 0
              ? `"${deletingCategory.name}" currently has ${deletingCategory.children.length} subcategories. You must move or delete its subcategories before deleting it.`
              : deletingCategory.lessonsCount > 0
              ? `"${deletingCategory.name}" is assigned to ${deletingCategory.lessonsCount} lessons and cannot be deleted until reassigned.`
              : `Are you sure you want to delete "${deletingCategory.name}"? This action can only be undone in the database.`
            : "Are you sure you want to delete this category?"
        }
        confirmText="Delete Category"
        isLoading={isDeleting}
      />
    </div>
  );
}
