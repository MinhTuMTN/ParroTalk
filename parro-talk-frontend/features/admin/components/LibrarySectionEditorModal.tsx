"use client";

import React, { useState, useEffect } from "react";
import { X, Layers, Folder, Tv, Clock, Loader2, UploadCloud, AlertCircle } from "lucide-react";
import type {
  AdminLibrarySection,
  AdminLibrarySectionRequest,
  SectionType,
} from "../services/librarySectionAdminService";
import Switch from "@/components/ui/Switch";

interface CategoryOption {
  id: string;
  name: string;
  slug?: string;
}

interface LibrarySectionEditorModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: AdminLibrarySectionRequest, id?: string) => Promise<void>;
  section?: AdminLibrarySection | null;
  categories: CategoryOption[];
  nextOrderIndex: number;
}

const SECTION_TYPES: {
  type: SectionType;
  label: string;
  description: string;
  icon: typeof Layers;
  badgeColor: string;
}[] = [
  {
    type: "CATEGORY",
    label: "Category Shelf",
    description: "Curated shelf showcasing lessons from a selected category.",
    icon: Folder,
    badgeColor: "bg-indigo-50 text-indigo-700 border-indigo-200",
  },
  {
    type: "PROGRAM",
    label: "Program Series",
    description: "Shelf filtering lessons by series/program name.",
    icon: Tv,
    badgeColor: "bg-emerald-50 text-emerald-700 border-emerald-200",
  },
  {
    type: "IN_PROGRESS",
    label: "Continue Learning",
    description: "Personal user shelf showing lessons with saved progress.",
    icon: Clock,
    badgeColor: "bg-amber-50 text-amber-700 border-amber-200",
  },
  {
    type: "USER_PROCESSING",
    label: "Processing Lessons",
    description: "Personal user shelf displaying audio undergoing transcription.",
    icon: Loader2,
    badgeColor: "bg-purple-50 text-purple-700 border-purple-200",
  },
  {
    type: "USER_UPLOADED",
    label: "Uploaded Lessons",
    description: "Personal user shelf of custom lessons uploaded by the user.",
    icon: UploadCloud,
    badgeColor: "bg-sky-50 text-sky-700 border-sky-200",
  },
];

const PROGRAM_SUGGESTIONS = [
  "BBC 6 Minute English",
  "TED-Ed",
  "Spotlight English",
  "VOA Learning English",
  "CNN 10",
  "Business English Pod",
];

export default function LibrarySectionEditorModal({
  isOpen,
  onClose,
  onSave,
  section,
  categories,
  nextOrderIndex,
}: LibrarySectionEditorModalProps) {
  const [formData, setFormData] = useState<AdminLibrarySectionRequest>({
    title: "",
    type: "CATEGORY",
    categoryId: null,
    program: null,
    orderIndex: 0,
    isActive: true,
    maxItems: 10,
  });

  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (section) {
      setFormData({
        title: section.title,
        type: section.type,
        categoryId: section.categoryId || null,
        program: section.program || null,
        orderIndex: section.orderIndex ?? 0,
        isActive: section.isActive ?? true,
        maxItems: section.maxItems ?? 10,
      });
    } else {
      setFormData({
        title: "",
        type: "CATEGORY",
        categoryId: categories.length > 0 ? categories[0].id : null,
        program: "",
        orderIndex: nextOrderIndex,
        isActive: true,
        maxItems: 10,
      });
    }
    setErrors({});
  }, [section, isOpen, categories, nextOrderIndex]);

  if (!isOpen) return null;

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.title.trim()) {
      newErrors.title = "Section title is required";
    }

    if (formData.type === "CATEGORY") {
      if (!formData.categoryId) {
        newErrors.categoryId = "Please select a Category";
      }
    } else if (formData.type === "PROGRAM") {
      if (!formData.program?.trim()) {
        newErrors.program = "Program name is required";
      }
    }

    if (!formData.maxItems || formData.maxItems < 1) {
      newErrors.maxItems = "Max items must be at least 1";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setLoading(true);
    try {
      const payload: AdminLibrarySectionRequest = {
        title: formData.title.trim(),
        type: formData.type,
        categoryId: formData.type === "CATEGORY" ? formData.categoryId : null,
        program: formData.type === "PROGRAM" ? formData.program?.trim() || null : null,
        orderIndex: formData.orderIndex,
        isActive: formData.isActive,
        maxItems: Number(formData.maxItems) || 10,
      };

      await onSave(payload, section?.id);
      onClose();
    } catch (err) {
      let message = "An error occurred while saving.";
      if (err instanceof Error) message = err.message;
      if (err && typeof err === "object" && "response" in err) {
        const response = (err as { response?: { data?: { message?: string } } }).response;
        if (response?.data?.message) {
          message = response.data.message;
        }
      }
      setErrors({ form: message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-slate-950/50 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />

      {/* Modal Dialog */}
      <div className="relative w-full max-w-2xl rounded-3xl bg-white shadow-2xl border border-slate-100 flex flex-col max-h-[90vh] overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="px-6 py-5 border-b border-slate-100 flex items-center justify-between bg-slate-50/70">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center border border-emerald-100/60 shadow-sm">
              <Layers className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-900">
                {section ? "Edit Library Section" : "Create New Library Section"}
              </h2>
              <p className="text-xs text-slate-500">
                Configure shelf title, target type, visibility, and item limits
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-xl transition"
            aria-label="Close dialog"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-6">
          {errors.form ? (
            <div className="flex items-center gap-2 p-3.5 rounded-2xl border border-rose-200 bg-rose-50 text-rose-700 text-sm">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{errors.form}</span>
            </div>
          ) : null}

          {/* Section Type Selector */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">
              Section Type <span className="text-rose-500">*</span>
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              {SECTION_TYPES.map((item) => {
                const isSelected = formData.type === item.type;
                const Icon = item.icon;
                return (
                  <button
                    key={item.type}
                    type="button"
                    onClick={() => {
                      setFormData({
                        ...formData,
                        type: item.type,
                        // Auto-fill default title if creating and currently default
                        title:
                          !section && (!formData.title || formData.title === "New Section")
                            ? item.label
                            : formData.title,
                      });
                      if (errors.categoryId || errors.program) {
                        setErrors({ ...errors, categoryId: "", program: "" });
                      }
                    }}
                    className={`flex items-start gap-3 p-3 text-left rounded-2xl border transition-all ${
                      isSelected
                        ? "border-emerald-500 bg-emerald-50/50 shadow-sm ring-2 ring-emerald-500/20"
                        : "border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/60"
                    }`}
                  >
                    <div
                      className={`p-2 rounded-xl border shrink-0 ${
                        isSelected
                          ? "bg-emerald-500 text-white border-emerald-500"
                          : "bg-slate-100 text-slate-600 border-slate-200"
                      }`}
                    >
                      <Icon className="w-4 h-4" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center justify-between gap-1">
                        <span className="text-sm font-bold text-slate-800">
                          {item.label}
                        </span>
                      </div>
                      <p className="text-xs text-slate-500 line-clamp-2 mt-0.5">
                        {item.description}
                      </p>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Title Field */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1.5">
              Section Title <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              value={formData.title}
              onChange={(e) => {
                setFormData({ ...formData, title: e.target.value });
                if (errors.title) setErrors({ ...errors, title: "" });
              }}
              placeholder="e.g. Technology Spotlight, BBC 6 Minute English"
              className={`w-full px-4 py-2.5 rounded-2xl border bg-white text-sm text-slate-800 shadow-sm outline-none transition ${
                errors.title
                  ? "border-rose-400 focus:ring-2 focus:ring-rose-200"
                  : "border-slate-200 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
              }`}
            />
            {errors.title ? (
              <p className="text-xs text-rose-500 mt-1 font-medium">{errors.title}</p>
            ) : (
              <p className="text-xs text-slate-400 mt-1">
                Display header shown to learners on the library shelf.
              </p>
            )}
          </div>

          {/* Category Dropdown (for CATEGORY type) */}
          {formData.type === "CATEGORY" ? (
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1.5">
                Target Category <span className="text-rose-500">*</span>
              </label>
              <select
                value={formData.categoryId || ""}
                onChange={(e) => {
                  const val = e.target.value || null;
                  const cat = categories.find((c) => c.id === val);
                  setFormData({
                    ...formData,
                    categoryId: val,
                    title:
                      !section && (!formData.title || formData.title === "Category Shelf") && cat
                        ? cat.name
                        : formData.title,
                  });
                  if (errors.categoryId) setErrors({ ...errors, categoryId: "" });
                }}
                className={`w-full px-4 py-2.5 rounded-2xl border bg-white text-sm text-slate-800 shadow-sm outline-none transition ${
                  errors.categoryId
                    ? "border-rose-400 focus:ring-2 focus:ring-rose-200"
                    : "border-slate-200 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
                }`}
              >
                <option value="">-- Select a Category --</option>
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name}
                  </option>
                ))}
              </select>
              {errors.categoryId ? (
                <p className="text-xs text-rose-500 mt-1 font-medium">
                  {errors.categoryId}
                </p>
              ) : (
                <p className="text-xs text-slate-400 mt-1">
                  Public lessons under this category will populate this section.
                </p>
              )}
            </div>
          ) : null}

          {/* Program Name Input (for PROGRAM type) */}
          {formData.type === "PROGRAM" ? (
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1.5">
                Program / Series Name <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                value={formData.program || ""}
                onChange={(e) => {
                  const val = e.target.value;
                  setFormData({
                    ...formData,
                    program: val,
                    title:
                      !section && (!formData.title || formData.title === "Program Series") && val
                        ? val
                        : formData.title,
                  });
                  if (errors.program) setErrors({ ...errors, program: "" });
                }}
                placeholder="e.g. BBC 6 Minute English, TED-Ed, Spotlight English"
                className={`w-full px-4 py-2.5 rounded-2xl border bg-white text-sm text-slate-800 shadow-sm outline-none transition ${
                  errors.program
                    ? "border-rose-400 focus:ring-2 focus:ring-rose-200"
                    : "border-slate-200 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
                }`}
              />
              {errors.program ? (
                <p className="text-xs text-rose-500 mt-1 font-medium">{errors.program}</p>
              ) : (
                <div className="mt-2 flex flex-wrap items-center gap-1.5">
                  <span className="text-xs text-slate-400">Suggestions:</span>
                  {PROGRAM_SUGGESTIONS.map((prog) => (
                    <button
                      key={prog}
                      type="button"
                      onClick={() => {
                        setFormData({
                          ...formData,
                          program: prog,
                          title: !formData.title || formData.title === "Program Series" ? prog : formData.title,
                        });
                        if (errors.program) setErrors({ ...errors, program: "" });
                      }}
                      className="px-2.5 py-1 text-xs font-medium rounded-lg bg-slate-100 hover:bg-emerald-50 hover:text-emerald-700 text-slate-600 transition"
                    >
                      {prog}
                    </button>
                  ))}
                </div>
              )}
            </div>
          ) : null}

          {/* Grid: Max Items, Order Index, Active Switch */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2 border-t border-slate-100">
            {/* Max Items */}
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1.5">
                Max Items Limit <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                min="1"
                max="50"
                value={formData.maxItems ?? 10}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    maxItems: parseInt(e.target.value, 10) || 1,
                  })
                }
                className="w-full px-4 py-2.5 rounded-2xl border border-slate-200 bg-white text-sm text-slate-800 shadow-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
              />
              {errors.maxItems ? (
                <p className="text-xs text-rose-500 mt-1 font-medium">{errors.maxItems}</p>
              ) : (
                <p className="text-xs text-slate-400 mt-1">Default 10 items</p>
              )}
            </div>

            {/* Order Index */}
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1.5">
                Display Order
              </label>
              <input
                type="number"
                min="0"
                value={formData.orderIndex ?? 0}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    orderIndex: parseInt(e.target.value, 10) || 0,
                  })
                }
                className="w-full px-4 py-2.5 rounded-2xl border border-slate-200 bg-white text-sm text-slate-800 shadow-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
              />
              <p className="text-xs text-slate-400 mt-1">Lower comes first</p>
            </div>

            {/* Active Switch */}
            <div className="flex flex-col justify-between">
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1.5">
                Visibility Status
              </label>
              <div className="flex items-center gap-3 py-1.5">
                <Switch
                  checked={formData.isActive ?? true}
                  onChange={(checked) => setFormData({ ...formData, isActive: checked })}
                />
                <span className="text-sm font-semibold text-slate-700">
                  {formData.isActive ? "Active (Visible)" : "Inactive (Hidden)"}
                </span>
              </div>
            </div>
          </div>

          {/* Footer Buttons */}
          <div className="pt-4 border-t border-slate-100 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-5 py-2.5 rounded-xl border border-slate-200 font-semibold text-sm text-slate-600 hover:bg-slate-50 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-6 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-sm shadow-lg shadow-emerald-600/20 transition active:scale-95 disabled:opacity-50 flex items-center gap-2"
            >
              {loading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Saving...</span>
                </>
              ) : section ? (
                "Update Section"
              ) : (
                "Create Section"
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
