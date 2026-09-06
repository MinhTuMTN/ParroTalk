"use client";

import React, { useState, useEffect, useMemo, useCallback } from "react";
import {
  Layers,
  Plus,
  Search,
  ArrowUp,
  ArrowDown,
  Pencil,
  Trash2,
  Folder,
  Tv,
  Clock,
  Loader2,
  UploadCloud,
} from "lucide-react";
import {
  librarySectionAdminService,
  type AdminLibrarySection,
  type AdminLibrarySectionRequest,
  type SectionType,
} from "../services/librarySectionAdminService";
import { categoryService } from "@/features/cms/services/categoryService";
import type { AdminCategoryDto } from "@/features/cms/types";
import LibrarySectionEditorModal from "./LibrarySectionEditorModal";
import ConfirmationModal from "@/components/ui/ConfirmationModal";
import FloatingToast from "@/components/ui/FloatingToast";
import Switch from "@/components/ui/Switch";
import Badge from "@/components/ui/Badge";

interface ToastState {
  message: string;
  variant: "success" | "error" | "info" | "warning";
}

interface CategoryOption {
  id: string;
  name: string;
  slug?: string;
}

export default function LibrarySectionListPage() {
  const [sections, setSections] = useState<AdminLibrarySection[]>([]);
  const [categories, setCategories] = useState<CategoryOption[]>([]);
  const [loading, setLoading] = useState(true);
  const [reordering, setReordering] = useState(false);

  // Search & Filter
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedTypeFilter, setSelectedTypeFilter] = useState<string>("ALL");

  // Modals & Actions
  const [isEditorModalOpen, setIsEditorModalOpen] = useState(false);
  const [editingSection, setEditingSection] = useState<AdminLibrarySection | null>(null);
  const [deletingSection, setDeletingSection] = useState<AdminLibrarySection | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Toast
  const [toast, setToast] = useState<ToastState | null>(null);

  const showToast = useCallback(
    (message: string, variant: "success" | "error" | "info" | "warning" = "success") => {
      setToast({ message, variant });
      setTimeout(() => {
        setToast((current) => (current?.message === message ? null : current));
      }, 3500);
    },
    []
  );

  // Flatten category tree helper
  const flattenCategoryTree = (tree: AdminCategoryDto[]): CategoryOption[] => {
    const list: CategoryOption[] = [];
    const traverse = (items: AdminCategoryDto[]) => {
      for (const item of items) {
        list.push({ id: item.id, name: item.name, slug: item.slug });
        if (item.children && item.children.length > 0) {
          traverse(item.children);
        }
      }
    };
    traverse(tree);
    return list;
  };

  // Fetch sections from API
  const fetchSections = useCallback(async () => {
    setLoading(true);
    try {
      const data = await librarySectionAdminService.getLibrarySections();
      // Ensure sorted by orderIndex
      const sorted = [...(data || [])].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));
      setSections(sorted);
    } catch (err) {
      console.error("Failed to load library sections", err);
      showToast("Failed to load library sections. Please try again.", "error");
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  // Fetch categories
  const fetchCategories = useCallback(async () => {
    try {
      const tree = await categoryService.getCategoryTree();
      if (tree && tree.length > 0) {
        setCategories(flattenCategoryTree(tree));
      } else {
        const page = await categoryService.getCategories(0, 100);
        if (page && page.content) {
          setCategories(
            page.content.map((c) => ({ id: c.id, name: c.name, slug: c.slug }))
          );
        }
      }
    } catch (err) {
      console.warn("Failed to load categories for sections", err);
    }
  }, []);

  useEffect(() => {
    fetchSections();
    fetchCategories();
  }, [fetchSections, fetchCategories]);

  // Toggle active status
  const handleToggleActive = async (section: AdminLibrarySection, nextActive: boolean) => {
    // Optimistic update
    const previous = [...sections];
    setSections((prev) =>
      prev.map((s) => (s.id === section.id ? { ...s, isActive: nextActive } : s))
    );

    try {
      const req: AdminLibrarySectionRequest = {
        title: section.title,
        type: section.type,
        categoryId: section.categoryId || null,
        program: section.program || null,
        orderIndex: section.orderIndex,
        isActive: nextActive,
        maxItems: section.maxItems,
      };
      await librarySectionAdminService.updateLibrarySection(section.id, req);
      showToast(
        `Section "${section.title}" ${nextActive ? "activated" : "deactivated"}.`,
        "success"
      );
    } catch (err) {
      console.error("Failed to toggle section status", err);
      setSections(previous);
      showToast("Failed to update section status.", "error");
    }
  };

  // Move Up / Move Down Reordering
  const handleMove = async (index: number, direction: "up" | "down") => {
    if (reordering) return;
    const targetIndex = direction === "up" ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= sections.length) return;

    const newSections = [...sections];
    const temp = newSections[index];
    newSections[index] = newSections[targetIndex];
    newSections[targetIndex] = temp;

    // Recalculate order indices
    const updatedSections = newSections.map((item, idx) => ({
      ...item,
      orderIndex: idx,
    }));

    setSections(updatedSections);
    setReordering(true);

    try {
      const reorderItems = updatedSections.map((s) => ({
        id: s.id,
        orderIndex: s.orderIndex,
      }));
      await librarySectionAdminService.reorderLibrarySections(reorderItems);
      showToast("Section order updated successfully.", "success");
    } catch (err) {
      console.error("Failed to reorder sections", err);
      showToast("Failed to save reordered sections.", "error");
      await fetchSections();
    } finally {
      setReordering(false);
    }
  };

  // Save (Create or Update)
  const handleSaveSection = async (data: AdminLibrarySectionRequest, id?: string) => {
    if (id) {
      await librarySectionAdminService.updateLibrarySection(id, data);
      showToast("Section updated successfully!", "success");
    } else {
      await librarySectionAdminService.createLibrarySection(data);
      showToast("Section created successfully!", "success");
    }
    await fetchSections();
  };

  // Delete Section
  const handleConfirmDelete = async () => {
    if (!deletingSection) return;
    setIsDeleting(true);
    try {
      await librarySectionAdminService.deleteLibrarySection(deletingSection.id);
      showToast(`Section "${deletingSection.title}" deleted.`, "success");
      setDeletingSection(null);
      await fetchSections();
    } catch (err) {
      console.error("Failed to delete section", err);
      let msg = "Failed to delete section.";
      if (err instanceof Error) msg = err.message;
      showToast(msg, "error");
    } finally {
      setIsDeleting(false);
    }
  };

  // Filter & Search Logic
  const filteredSections = useMemo(() => {
    return sections.filter((section) => {
      // Type Filter
      if (selectedTypeFilter === "CATEGORY" && section.type !== "CATEGORY") return false;
      if (selectedTypeFilter === "PROGRAM" && section.type !== "PROGRAM") return false;
      if (
        selectedTypeFilter === "PERSONAL" &&
        section.type !== "IN_PROGRESS" &&
        section.type !== "USER_PROCESSING" &&
        section.type !== "USER_UPLOADED"
      ) {
        return false;
      }

      // Search Filter
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesTitle = section.title.toLowerCase().includes(q);
        const matchesCategory = section.categoryName?.toLowerCase().includes(q);
        const matchesProgram = section.program?.toLowerCase().includes(q);
        const matchesType = section.type.toLowerCase().includes(q);
        return matchesTitle || matchesCategory || matchesProgram || matchesType;
      }

      return true;
    });
  }, [sections, selectedTypeFilter, searchQuery]);

  // Statistics Summary
  const stats = useMemo(() => {
    const total = sections.length;
    const active = sections.filter((s) => s.isActive).length;
    const categoriesCount = sections.filter((s) => s.type === "CATEGORY").length;
    const programsCount = sections.filter((s) => s.type === "PROGRAM").length;
    const personalCount = sections.filter(
      (s) =>
        s.type === "IN_PROGRESS" ||
        s.type === "USER_PROCESSING" ||
        s.type === "USER_UPLOADED"
    ).length;
    return { total, active, categoriesCount, programsCount, personalCount };
  }, [sections]);

  // Section Type Badge Helper
  const renderTypeBadge = (type: SectionType) => {
    switch (type) {
      case "CATEGORY":
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
            <Folder className="w-3.5 h-3.5" />
            Category
          </span>
        );
      case "PROGRAM":
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <Tv className="w-3.5 h-3.5" />
            Program
          </span>
        );
      case "IN_PROGRESS":
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-amber-50 text-amber-700 border border-amber-200">
            <Clock className="w-3.5 h-3.5" />
            Continue Learning
          </span>
        );
      case "USER_PROCESSING":
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-purple-50 text-purple-700 border border-purple-200">
            <Loader2 className="w-3.5 h-3.5" />
            Processing
          </span>
        );
      case "USER_UPLOADED":
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-sky-50 text-sky-700 border border-sky-200">
            <UploadCloud className="w-3.5 h-3.5" />
            Uploaded
          </span>
        );
      default:
        return <Badge tone="neutral">{type}</Badge>;
    }
  };

  const isSearchingOrFiltered = searchQuery.trim().length > 0 || selectedTypeFilter !== "ALL";

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12">
      {/* Toast Feedback */}
      {toast ? (
        <FloatingToast
          message={toast.message}
          variant={toast.variant}
          onClose={() => setToast(null)}
        />
      ) : null}

      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-emerald-600 to-teal-500 text-white flex items-center justify-center shadow-lg shadow-emerald-500/20 shrink-0">
            <Layers className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-black text-slate-900 tracking-tight">
                Library Sections
              </h1>
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                {stats.active} / {stats.total} Active
              </span>
            </div>
            <p className="text-sm text-slate-500 mt-1 max-w-2xl">
              Configure public curated bookshelves, category spotlights, program series, and personal learning sections displayed on the ParroTalk Library page.
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={() => {
            setEditingSection(null);
            setIsEditorModalOpen(true);
          }}
          className="inline-flex items-center justify-center gap-2 px-5 py-3 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-sm shadow-lg shadow-emerald-600/20 transition-all active:scale-95 shrink-0"
        >
          <Plus className="w-4 h-4 stroke-[3]" />
          <span>Add Section</span>
        </button>
      </div>

      {/* Quick Metric Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex flex-col justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
            Total Sections
          </span>
          <div className="flex items-baseline justify-between mt-2">
            <span className="text-2xl font-black text-slate-800">{stats.total}</span>
            <span className="text-xs font-semibold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-md">
              {stats.active} enabled
            </span>
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex flex-col justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
            Category Shelves
          </span>
          <div className="flex items-baseline justify-between mt-2">
            <span className="text-2xl font-black text-indigo-700">{stats.categoriesCount}</span>
            <Folder className="w-4 h-4 text-indigo-400" />
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex flex-col justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
            Program Series
          </span>
          <div className="flex items-baseline justify-between mt-2">
            <span className="text-2xl font-black text-emerald-700">{stats.programsCount}</span>
            <Tv className="w-4 h-4 text-emerald-400" />
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex flex-col justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
            Personal Shelves
          </span>
          <div className="flex items-baseline justify-between mt-2">
            <span className="text-2xl font-black text-amber-700">{stats.personalCount}</span>
            <Clock className="w-4 h-4 text-amber-400" />
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-100 shadow-sm flex flex-col md:flex-row items-center justify-between gap-4">
        {/* Search */}
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by title, category, program..."
            className="w-full pl-10 pr-4 py-2 text-sm rounded-xl border border-slate-200 bg-slate-50/50 focus:bg-white focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none transition"
          />
        </div>

        {/* Filter Tabs */}
        <div className="flex items-center gap-1.5 bg-slate-100 p-1 rounded-xl w-full md:w-auto overflow-x-auto">
          {[
            { key: "ALL", label: "All Sections", count: stats.total },
            { key: "CATEGORY", label: "Categories", count: stats.categoriesCount },
            { key: "PROGRAM", label: "Programs", count: stats.programsCount },
            { key: "PERSONAL", label: "Personal Shelves", count: stats.personalCount },
          ].map((tab) => (
            <button
              key={tab.key}
              type="button"
              onClick={() => setSelectedTypeFilter(tab.key)}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition flex items-center gap-1.5 whitespace-nowrap ${
                selectedTypeFilter === tab.key
                  ? "bg-white text-slate-900 shadow-sm"
                  : "text-slate-500 hover:text-slate-800"
              }`}
            >
              <span>{tab.label}</span>
              <span
                className={`px-1.5 py-0.2 rounded-md text-[10px] ${
                  selectedTypeFilter === tab.key
                    ? "bg-emerald-100 text-emerald-800"
                    : "bg-slate-200/80 text-slate-600"
                }`}
              >
                {tab.count}
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* Main Table Card */}
      <div className="bg-white rounded-3xl border border-slate-100 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-16 flex flex-col items-center justify-center text-center">
            <Loader2 className="w-8 h-8 text-emerald-500 animate-spin mb-3" />
            <p className="text-sm font-semibold text-slate-600">Loading library sections...</p>
          </div>
        ) : filteredSections.length === 0 ? (
          <div className="p-16 flex flex-col items-center justify-center text-center max-w-md mx-auto">
            <div className="w-16 h-16 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center mb-4">
              <Layers className="w-8 h-8" />
            </div>
            <h3 className="text-base font-bold text-slate-800 mb-1">
              {searchQuery ? "No matching sections found" : "No library sections yet"}
            </h3>
            <p className="text-xs text-slate-500 mb-6">
              {searchQuery
                ? `No sections match your search "${searchQuery}". Try a different keyword or reset filters.`
                : "Get started by creating your first curated bookshelf or category spotlight section."}
            </p>
            {searchQuery ? (
              <button
                type="button"
                onClick={() => {
                  setSearchQuery("");
                  setSelectedTypeFilter("ALL");
                }}
                className="px-4 py-2 rounded-xl text-xs font-bold bg-slate-100 hover:bg-slate-200 text-slate-700 transition"
              >
                Clear Search Filters
              </button>
            ) : (
              <button
                type="button"
                onClick={() => {
                  setEditingSection(null);
                  setIsEditorModalOpen(true);
                }}
                className="px-5 py-2.5 rounded-xl text-sm font-bold bg-emerald-600 hover:bg-emerald-700 text-white shadow-md shadow-emerald-600/20 transition"
              >
                Create Section
              </button>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50/70 text-slate-500 text-xs font-bold uppercase tracking-wider">
                  <th className="py-4 px-4 text-center w-20">Order</th>
                  <th className="py-4 px-4">Section Info & Target</th>
                  <th className="py-4 px-4">Type</th>
                  <th className="py-4 px-4 text-center">Max Items</th>
                  <th className="py-4 px-4 text-center">Active</th>
                  <th className="py-4 px-4">Created Date</th>
                  <th className="py-4 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredSections.map((section) => {
                  // Find original index in full sections array for reordering
                  const originalIndex = sections.findIndex((s) => s.id === section.id);
                  const isFirst = originalIndex === 0;
                  const isLast = originalIndex === sections.length - 1;

                  return (
                    <tr
                      key={section.id}
                      className="hover:bg-slate-50/70 transition-colors group"
                    >
                      {/* Order Controls */}
                      <td className="py-4 px-4 text-center">
                        <div className="flex items-center justify-center gap-1">
                          <div className="flex flex-col gap-0.5">
                            <button
                              type="button"
                              title="Move section up"
                              disabled={isFirst || isSearchingOrFiltered || reordering}
                              onClick={() => handleMove(originalIndex, "up")}
                              className="p-1 rounded text-slate-400 hover:text-emerald-600 hover:bg-emerald-50 disabled:opacity-20 disabled:hover:bg-transparent disabled:hover:text-slate-400 transition"
                            >
                              <ArrowUp className="w-3.5 h-3.5" />
                            </button>
                            <button
                              type="button"
                              title="Move section down"
                              disabled={isLast || isSearchingOrFiltered || reordering}
                              onClick={() => handleMove(originalIndex, "down")}
                              className="p-1 rounded text-slate-400 hover:text-emerald-600 hover:bg-emerald-50 disabled:opacity-20 disabled:hover:bg-transparent disabled:hover:text-slate-400 transition"
                            >
                              <ArrowDown className="w-3.5 h-3.5" />
                            </button>
                          </div>
                          <span className="inline-flex items-center justify-center w-6 h-6 rounded-md bg-slate-100 text-slate-700 text-xs font-bold">
                            {section.orderIndex ?? originalIndex}
                          </span>
                        </div>
                      </td>

                      {/* Title & Target */}
                      <td className="py-4 px-4">
                        <div className="flex flex-col">
                          <span className="font-bold text-slate-900 group-hover:text-emerald-700 transition-colors text-sm">
                            {section.title}
                          </span>
                          <div className="flex items-center gap-2 mt-0.5 text-xs text-slate-500">
                            {section.type === "CATEGORY" ? (
                              <span className="flex items-center gap-1 text-indigo-600 font-medium">
                                <Folder className="w-3 h-3" />
                                Category: {section.categoryName || section.categorySlug || "Unassigned"}
                              </span>
                            ) : section.type === "PROGRAM" ? (
                              <span className="flex items-center gap-1 text-emerald-600 font-medium">
                                <Tv className="w-3 h-3" />
                                Program: {section.program || "Unassigned"}
                              </span>
                            ) : (
                              <span className="text-slate-400">
                                Dynamic Personal User Shelf
                              </span>
                            )}
                          </div>
                        </div>
                      </td>

                      {/* Type Badge */}
                      <td className="py-4 px-4">{renderTypeBadge(section.type)}</td>

                      {/* Max Items */}
                      <td className="py-4 px-4 text-center">
                        <span className="inline-block px-2.5 py-1 rounded-lg text-xs font-bold bg-slate-100 text-slate-700">
                          {section.maxItems ?? 10} items
                        </span>
                      </td>

                      {/* Active Switch */}
                      <td className="py-4 px-4 text-center">
                        <div className="flex justify-center">
                          <Switch
                            checked={Boolean(section.isActive)}
                            onChange={(next) => handleToggleActive(section, next)}
                          />
                        </div>
                      </td>

                      {/* Created Date */}
                      <td className="py-4 px-4 text-xs text-slate-500">
                        {section.createdAt
                          ? new Date(section.createdAt).toLocaleDateString(undefined, {
                              year: "numeric",
                              month: "short",
                              day: "numeric",
                            })
                          : "N/A"}
                      </td>

                      {/* Actions */}
                      <td className="py-4 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            type="button"
                            title="Edit section"
                            onClick={() => {
                              setEditingSection(section);
                              setIsEditorModalOpen(true);
                            }}
                            className="p-2 rounded-xl text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 transition"
                          >
                            <Pencil className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            title="Delete section"
                            onClick={() => setDeletingSection(section)}
                            className="p-2 rounded-xl text-slate-500 hover:text-rose-600 hover:bg-rose-50 transition"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Editor Modal */}
      <LibrarySectionEditorModal
        isOpen={isEditorModalOpen}
        onClose={() => {
          setIsEditorModalOpen(false);
          setEditingSection(null);
        }}
        onSave={handleSaveSection}
        section={editingSection}
        categories={categories}
        nextOrderIndex={sections.length}
      />

      {/* Delete Confirmation Modal */}
      <ConfirmationModal
        isOpen={Boolean(deletingSection)}
        onClose={() => setDeletingSection(null)}
        onConfirm={handleConfirmDelete}
        title="Delete Library Section?"
        message={
          deletingSection
            ? `Are you sure you want to permanently delete the section "${deletingSection.title}"? This will remove the shelf from the library layout.`
            : "Are you sure you want to delete this section?"
        }
        confirmText="Delete Section"
        isLoading={isDeleting}
      />
    </div>
  );
}
