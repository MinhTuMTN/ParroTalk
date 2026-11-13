"use client";

import React, { useEffect, useId, useMemo, useRef, useState } from "react";
import { AlertCircle, Image as ImageIcon, Sparkles, Wand2, X } from "lucide-react";
import type {
  AdminCategoryCreateRequest,
  AdminCategoryDto,
  CmsItemStatus,
} from "../../types";
import { extractCategoryErrorMessage } from "../../services/categoryService";
import {
  collectDescendantIds,
  flattenCategoryOptions,
  slugifyCategoryName,
} from "./categoryTreeUtils";
import CategoryIconPicker, {
  PRESET_CATEGORY_COLORS,
  renderCategoryIcon,
} from "./CategoryIconPicker";
import Switch from "@/components/ui/Switch";
import Badge from "@/components/ui/Badge";
import Button from "@/components/ui/Button";
import Input from "@/components/ui/Input";
import Label from "@/components/ui/Label";
import Textarea from "@/components/ui/Textarea";

interface CategoryEditorDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: AdminCategoryCreateRequest) => Promise<void>;
  category?: AdminCategoryDto | null;
  defaultParentId?: string;
  defaultSortOrder?: number;
  categories: AdminCategoryDto[];
}

const DEFAULT_COLOR = "#059669";

export default function CategoryEditorDrawer({
  isOpen,
  onClose,
  onSave,
  category,
  defaultParentId = "",
  defaultSortOrder = 0,
  categories,
}: CategoryEditorDrawerProps) {
  const titleId = useId();
  const dialogRef = useRef<HTMLDialogElement | null>(null);

  const [formData, setFormData] = useState<AdminCategoryCreateRequest>({
    name: "",
    nameVi: "",
    slug: "",
    description: "",
    icon: "Folder",
    color: DEFAULT_COLOR,
    imageUrl: "",
    parentCategoryId: "",
    sortOrder: 0,
    status: "ACTIVE",
  });

  const [slugTouched, setSlugTouched] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [imagePreviewError, setImagePreviewError] = useState(false);

  // Sync form state whenever drawer opens or target changes
  useEffect(() => {
    if (!isOpen) return;
    if (category) {
      setFormData({
        name: category.name,
        nameVi: category.nameVi ?? "",
        slug: category.slug,
        description: category.description ?? "",
        icon: category.icon ?? "Folder",
        color: category.color || DEFAULT_COLOR,
        imageUrl: category.imageUrl ?? "",
        parentCategoryId: category.parentCategoryId ?? "",
        sortOrder: category.sortOrder ?? 0,
        status: category.status,
      });
      setSlugTouched(true);
    } else {
      setFormData({
        name: "",
        nameVi: "",
        slug: "",
        description: "",
        icon: "Folder",
        color: DEFAULT_COLOR,
        imageUrl: "",
        parentCategoryId: defaultParentId,
        sortOrder: defaultSortOrder,
        status: "ACTIVE",
      });
      setSlugTouched(false);
    }
    setErrors({});
    setImagePreviewError(false);
  }, [isOpen, category, defaultParentId, defaultSortOrder]);

  // Manage native <dialog> showModal() / close() lifecycle
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    if (isOpen && !dialog.open) {
      dialog.showModal();
    } else if (!isOpen && dialog.open) {
      dialog.close();
    }
  }, [isOpen]);

  // Progressive fallback for browsers without native <dialog closedby="any">
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog || "closedBy" in HTMLDialogElement.prototype) return;

    const isOutside = (e: PointerEvent) => {
      const rect = dialog.getBoundingClientRect();
      return (
        e.clientX < rect.left ||
        e.clientX > rect.right ||
        e.clientY < rect.top ||
        e.clientY > rect.bottom
      );
    };

    let pressedOutside = false;
    const handlePointerDown = (e: PointerEvent) => {
      pressedOutside = isOutside(e);
    };
    const handlePointerUp = (e: PointerEvent) => {
      if (pressedOutside && isOutside(e) && dialog.open) {
        dialog.close();
      }
      pressedOutside = false;
    };

    dialog.addEventListener("pointerdown", handlePointerDown);
    dialog.addEventListener("pointerup", handlePointerUp);
    return () => {
      dialog.removeEventListener("pointerdown", handlePointerDown);
      dialog.removeEventListener("pointerup", handlePointerUp);
    };
  }, []);

  const excludedIds = useMemo(() => {
    if (!category) return undefined;
    return collectDescendantIds(categories, category.id);
  }, [categories, category]);

  const parentOptions = useMemo(
    () => flattenCategoryOptions(categories, excludedIds),
    [categories, excludedIds]
  );

  const generatedSlugPreview = useMemo(
    () => slugifyCategoryName(formData.name),
    [formData.name]
  );

  const effectiveSlug = formData.slug?.trim() || generatedSlugPreview || "category-slug";
  const effectiveColor =
    formData.color && /^#[0-9a-fA-F]{6}$/.test(formData.color)
      ? formData.color
      : DEFAULT_COLOR;

  const validate = (): boolean => {
    const nextErrors: Record<string, string> = {};

    if (!formData.name.trim()) {
      nextErrors.name = "Category name is required.";
    }

    if (formData.sortOrder < 0 || Number.isNaN(formData.sortOrder)) {
      nextErrors.sortOrder = "Sort order must be 0 or greater.";
    }

    if (formData.color && !/^#[0-9a-fA-F]{6}$/.test(formData.color.trim())) {
      nextErrors.color = "Color must be a valid 6-digit hex code (e.g. #059669).";
    }

    if (formData.imageUrl?.trim()) {
      const trimmedUrl = formData.imageUrl.trim();
      const validUrl =
        trimmedUrl.startsWith("/") ||
        trimmedUrl.startsWith("https://") ||
        trimmedUrl.startsWith("http://");
      if (!validUrl) {
        nextErrors.imageUrl = "Image URL must start with https://, http://, or /.";
      }
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleNameChange = (value: string) => {
    setFormData((prev) => ({
      ...prev,
      name: value,
      slug: !slugTouched && !category ? slugifyCategoryName(value) : prev.slug,
    }));
    if (errors.name) {
      setErrors((prev) => ({ ...prev, name: "" }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setLoading(true);
    setErrors((prev) => ({ ...prev, form: "" }));

    try {
      const payload: AdminCategoryCreateRequest = {
        name: formData.name.trim(),
        nameVi: formData.nameVi?.trim() || undefined,
        slug: formData.slug?.trim() || undefined,
        description: formData.description?.trim() || undefined,
        icon: formData.icon?.trim() || undefined,
        color: formData.color?.trim() || undefined,
        imageUrl: formData.imageUrl?.trim() || undefined,
        parentCategoryId: formData.parentCategoryId || undefined,
        sortOrder: Number(formData.sortOrder) || 0,
        status: formData.status,
      };

      await onSave(payload);
      onClose();
    } catch (err) {
      setErrors((prev) => ({
        ...prev,
        form: extractCategoryErrorMessage(err, "An error occurred while saving category."),
      }));
    } finally {
      setLoading(false);
    }
  };

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby={titleId}
      onClose={onClose}
      {...({ closedby: "any" } as React.DialogHTMLAttributes<HTMLDialogElement>)}
      className="fixed inset-y-0 right-0 left-auto m-0 h-dvh max-h-dvh w-full max-w-xl border-l border-slate-200 bg-white p-0 text-slate-900 shadow-2xl backdrop:bg-slate-950/45 backdrop:backdrop-blur-xs open:flex open:flex-col"
    >
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50/80 px-6 py-4">
        <div className="flex items-center gap-3 min-w-0">
          <div
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl border shadow-xs"
            style={{
              backgroundColor: `${effectiveColor}18`,
              borderColor: `${effectiveColor}40`,
              color: effectiveColor,
            }}
          >
            {renderCategoryIcon(formData.icon, "h-5 w-5")}
          </div>
          <div className="min-w-0">
            <h2 id={titleId} className="truncate text-lg font-bold text-slate-900">
              {category
                ? "Edit Lesson Category"
                : defaultParentId
                ? "Create Subcategory"
                : "Create Root Category"}
            </h2>
            <p className="text-xs text-slate-500">
              Configure bilingual labels, hierarchy, icon styling, and visibility.
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={() => dialogRef.current?.close()}
          aria-label="Close category editor"
          className="rounded-xl p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700 cursor-pointer outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
        >
          <X className="h-5 w-5" aria-hidden="true" />
        </button>
      </div>

      {/* Body */}
      <form onSubmit={handleSubmit} className="flex flex-1 flex-col overflow-hidden">
        <div className="flex-1 space-y-6 overflow-y-auto px-6 py-5">
          {/* Live Preview Chip */}
          <div className="rounded-2xl border border-slate-200/80 bg-gradient-to-br from-slate-50 to-emerald-50/40 p-4">
            <div className="mb-2 flex items-center justify-between">
              <span className="inline-flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                <Sparkles className="h-3.5 w-3.5 text-emerald-600" aria-hidden="true" />
                Live Preview
              </span>
              <Badge tone={formData.status === "ACTIVE" ? "success" : "neutral"}>
                {formData.status}
              </Badge>
            </div>
            <div className="flex items-center gap-3.5 rounded-xl border border-white/80 bg-white p-3 shadow-xs">
              <div
                className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl border"
                style={{
                  backgroundColor: `${effectiveColor}15`,
                  borderColor: `${effectiveColor}35`,
                  color: effectiveColor,
                }}
              >
                {renderCategoryIcon(formData.icon, "h-5 w-5")}
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="truncate text-sm font-bold text-slate-900">
                    {formData.name.trim() || "Category Name"}
                  </span>
                  <span className="rounded-md bg-slate-100 px-2 py-0.5 font-mono text-[11px] text-slate-600">
                    /{effectiveSlug}
                  </span>
                </div>
                <p className="truncate text-xs text-slate-500 mt-0.5">
                  {formData.nameVi?.trim() || "Tên tiếng Việt (tùy chọn)"}
                </p>
              </div>
            </div>
          </div>

          {errors.form ? (
            <div
              role="alert"
              className="flex items-start gap-2.5 rounded-2xl border border-rose-200 bg-rose-50 p-3.5 text-sm text-rose-700"
            >
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
              <span>{errors.form}</span>
            </div>
          ) : null}

          {/* Section: Basic Info */}
          <fieldset className="space-y-4">
            <legend className="text-xs font-bold uppercase tracking-wider text-slate-500">
              Basic Details
            </legend>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <Label htmlFor="cat-name" className="mb-1.5 block">
                  Name (English) <span className="text-rose-500">*</span>
                </Label>
                <Input
                  id="cat-name"
                  autoFocus
                  value={formData.name}
                  onChange={(e) => handleNameChange(e.target.value)}
                  placeholder="e.g. Travel & Transport"
                  aria-invalid={Boolean(errors.name)}
                />
                {errors.name ? (
                  <p className="mt-1 text-xs font-medium text-rose-600">{errors.name}</p>
                ) : null}
              </div>

              <div>
                <Label htmlFor="cat-name-vi">Vietnamese Name</Label>
                <Input
                  id="cat-name-vi"
                  value={formData.nameVi ?? ""}
                  onChange={(e) =>
                    setFormData((prev) => ({ ...prev, nameVi: e.target.value }))
                  }
                  placeholder="e.g. Du lịch & Di chuyển"
                />
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between">
                <Label htmlFor="cat-slug">URL Slug</Label>
                <button
                  type="button"
                  onClick={() => {
                    const generated = slugifyCategoryName(formData.name);
                    setSlugTouched(true);
                    setFormData((prev) => ({ ...prev, slug: generated }));
                  }}
                  className="mb-1 inline-flex items-center gap-1 text-xs font-semibold text-emerald-700 hover:text-emerald-800 cursor-pointer"
                >
                  <Wand2 className="h-3 w-3" aria-hidden="true" />
                  Auto-generate
                </button>
              </div>
              <Input
                id="cat-slug"
                value={formData.slug ?? ""}
                onChange={(e) => {
                  setSlugTouched(true);
                  setFormData((prev) => ({ ...prev, slug: e.target.value }));
                }}
                placeholder={generatedSlugPreview || "Auto-generated if left blank"}
                className="font-mono text-xs"
              />
              <p className="mt-1 text-xs text-slate-400">
                Leave empty to auto-generate cleanly (supports Vietnamese diacritics).
              </p>
            </div>

            <div>
              <Label htmlFor="cat-description">Description</Label>
              <Textarea
                id="cat-description"
                rows={3}
                value={formData.description ?? ""}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, description: e.target.value }))
                }
                placeholder="Short summary shown to learners or editors..."
              />
            </div>
          </fieldset>

          {/* Section: Hierarchy & Order */}
          <fieldset className="space-y-4 border-t border-slate-100 pt-5">
            <legend className="text-xs font-bold uppercase tracking-wider text-slate-500">
              Hierarchy & Ordering
            </legend>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div className="sm:col-span-2">
                <Label htmlFor="cat-parent">Parent Category</Label>
                <select
                  id="cat-parent"
                  value={formData.parentCategoryId ?? ""}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      parentCategoryId: e.target.value,
                    }))
                  }
                  className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-800 outline-none transition focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
                >
                  <option value="">None (Root Level)</option>
                  {parentOptions.map((opt) => (
                    <option key={opt.id} value={opt.id} disabled={opt.disabled}>
                      {"\u00A0\u00A0".repeat(opt.level * 2)}
                      {opt.level > 0 ? "└ " : ""}
                      {opt.name}
                      {opt.nameVi ? ` (${opt.nameVi})` : ""}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <Label htmlFor="cat-sort-order" className="mb-1.5 block">
                  Sort Order <span className="text-rose-500">*</span>
                </Label>
                <Input
                  id="cat-sort-order"
                  type="number"
                  min={0}
                  value={formData.sortOrder}
                  onChange={(e) => {
                    setFormData((prev) => ({
                      ...prev,
                      sortOrder: Number.parseInt(e.target.value, 10) || 0,
                    }));
                    if (errors.sortOrder) {
                      setErrors((prev) => ({ ...prev, sortOrder: "" }));
                    }
                  }}
                />
                {errors.sortOrder ? (
                  <p className="mt-1 text-xs font-medium text-rose-600">{errors.sortOrder}</p>
                ) : null}
              </div>
            </div>
          </fieldset>

          {/* Section: Appearance */}
          <fieldset className="space-y-4 border-t border-slate-100 pt-5">
            <legend className="text-xs font-bold uppercase tracking-wider text-slate-500">
              Visual Styling
            </legend>

            <div>
              <Label htmlFor="cat-color-hex">Accent Color</Label>
              <div className="flex flex-wrap items-center gap-2 mb-2.5">
                {PRESET_CATEGORY_COLORS.map((swatch) => {
                  const selected =
                    (formData.color ?? "").toLowerCase() === swatch.toLowerCase();
                  return (
                    <button
                      key={swatch}
                      type="button"
                      aria-label={`Select color ${swatch}`}
                      aria-pressed={selected}
                      onClick={() => {
                        setFormData((prev) => ({ ...prev, color: swatch }));
                        if (errors.color) setErrors((prev) => ({ ...prev, color: "" }));
                      }}
                      style={{ backgroundColor: swatch }}
                      className={`h-7 w-7 rounded-full border-2 transition cursor-pointer outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2 ${
                        selected
                          ? "border-slate-900 scale-110 shadow-sm"
                          : "border-white shadow-xs hover:scale-105"
                      }`}
                    />
                  );
                })}
              </div>
              <div className="flex items-center gap-2.5">
                <input
                  type="color"
                  aria-label="Custom color picker"
                  value={effectiveColor}
                  onChange={(e) => {
                    setFormData((prev) => ({ ...prev, color: e.target.value }));
                    if (errors.color) setErrors((prev) => ({ ...prev, color: "" }));
                  }}
                  className="h-10 w-12 cursor-pointer rounded-xl border border-slate-200 bg-white p-1"
                />
                <Input
                  id="cat-color-hex"
                  value={formData.color ?? ""}
                  onChange={(e) => {
                    setFormData((prev) => ({ ...prev, color: e.target.value }));
                    if (errors.color) setErrors((prev) => ({ ...prev, color: "" }));
                  }}
                  placeholder="#059669"
                  className="font-mono text-xs"
                />
              </div>
              {errors.color ? (
                <p className="mt-1 text-xs font-medium text-rose-600">{errors.color}</p>
              ) : null}
            </div>

            <div>
              <Label>Icon</Label>
              <CategoryIconPicker
                value={formData.icon ?? ""}
                accentColor={effectiveColor}
                onChange={(iconName) =>
                  setFormData((prev) => ({ ...prev, icon: iconName }))
                }
              />
            </div>

            <div>
              <Label htmlFor="cat-image-url">Cover Image URL</Label>
              <div className="flex items-start gap-3">
                <div className="flex-1">
                  <Input
                    id="cat-image-url"
                    value={formData.imageUrl ?? ""}
                    onChange={(e) => {
                      setImagePreviewError(false);
                      setFormData((prev) => ({ ...prev, imageUrl: e.target.value }));
                      if (errors.imageUrl) {
                        setErrors((prev) => ({ ...prev, imageUrl: "" }));
                      }
                    }}
                    placeholder="https://... or /images/..."
                  />
                  {errors.imageUrl ? (
                    <p className="mt-1 text-xs font-medium text-rose-600">
                      {errors.imageUrl}
                    </p>
                  ) : null}
                </div>
                {formData.imageUrl?.trim() && !imagePreviewError ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img
                    src={formData.imageUrl.trim()}
                    alt="Category cover preview"
                    onError={() => setImagePreviewError(true)}
                    className="h-10 w-14 shrink-0 rounded-xl border border-slate-200 object-cover bg-slate-50"
                  />
                ) : (
                  <div
                    aria-hidden="true"
                    className="flex h-10 w-14 shrink-0 items-center justify-center rounded-xl border border-dashed border-slate-200 bg-slate-50 text-slate-400"
                  >
                    <ImageIcon className="h-4 w-4" />
                  </div>
                )}
              </div>
            </div>
          </fieldset>

          {/* Section: Visibility */}
          <div className="flex items-center justify-between rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3.5">
            <div>
              <p className="text-sm font-semibold text-slate-900">Active Visibility</p>
              <p className="text-xs text-slate-500">
                Inactive categories are hidden from learners in the library.
              </p>
            </div>
            <Switch
              checked={formData.status === "ACTIVE"}
              onChange={(checked) =>
                setFormData((prev) => ({
                  ...prev,
                  status: (checked ? "ACTIVE" : "INACTIVE") as CmsItemStatus,
                }))
              }
            />
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 border-t border-slate-100 bg-slate-50/80 px-6 py-4">
          <Button
            variant="secondary"
            onClick={() => dialogRef.current?.close()}
            disabled={loading}
          >
            Cancel
          </Button>
          <Button type="submit" isLoading={loading}>
            {loading
              ? "Saving..."
              : category
              ? "Save Changes"
              : "Create Category"}
          </Button>
        </div>
      </form>
    </dialog>
  );
}
