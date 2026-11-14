"use client";

import React, { useEffect, useId, useState } from "react";
import { X, Tag as TagIcon, Sparkles } from "lucide-react";
import type { AdminTagDto, AdminTagCreateRequest, AdminTagUpdateRequest, CmsItemStatus } from "../../types";
import Button from "@/components/ui/Button";
import Input from "@/components/ui/Input";
import Label from "@/components/ui/Label";
import Textarea from "@/components/ui/Textarea";
import Switch from "@/components/ui/Switch";
import Badge from "@/components/ui/Badge";

interface TagEditorModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: AdminTagCreateRequest | AdminTagUpdateRequest) => Promise<void>;
  tag?: AdminTagDto | null;
}

export const PRESET_TAG_COLORS = [
  { name: "Emerald", value: "#10b981" },
  { name: "Cyan", value: "#06b6d4" },
  { name: "Blue", value: "#3b82f6" },
  { name: "Indigo", value: "#6366f1" },
  { name: "Purple", value: "#8b5cf6" },
  { name: "Pink", value: "#ec4899" },
  { name: "Amber", value: "#f59e0b" },
  { name: "Rose", value: "#ef4444" },
  { name: "Teal", value: "#14b8a6" },
  { name: "Slate", value: "#64748b" },
];

function slugify(text: string): string {
  return text
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[đĐ]/g, "d")
    .toLowerCase()
    .trim()
    .replace(/[^\w\s-]/g, "")
    .replace(/[\s_-]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

export default function TagEditorModal({
  isOpen,
  onClose,
  onSave,
  tag,
}: TagEditorModalProps) {
  const titleId = useId();
  const [formData, setFormData] = useState<AdminTagCreateRequest>({
    name: "",
    slug: "",
    color: "#10b981",
    description: "",
    status: "ACTIVE",
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [isSlugTouched, setIsSlugTouched] = useState(false);

  useEffect(() => {
    if (!isOpen) return;

    if (tag) {
      setFormData({
        name: tag.name,
        slug: tag.slug,
        description: tag.description || "",
        color: tag.color || "#10b981",
        status: tag.status,
      });
      setIsSlugTouched(true);
    } else {
      setFormData({
        name: "",
        slug: "",
        description: "",
        color: "#10b981",
        status: "ACTIVE",
      });
      setIsSlugTouched(false);
    }
    setError("");
  }, [tag, isOpen]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && isOpen) {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleNameChange = (name: string) => {
    setFormData((prev) => ({
      ...prev,
      name,
      slug: !isSlugTouched ? slugify(name) : prev.slug,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      setError("Tag name is required.");
      return;
    }

    setError("");
    setLoading(true);
    try {
      const payload: AdminTagCreateRequest = {
        name: formData.name.trim(),
        slug: formData.slug?.trim() ? slugify(formData.slug) : slugify(formData.name),
        color: formData.color || "#10b981",
        description: formData.description?.trim() || undefined,
        status: formData.status,
      };
      await onSave(payload);
      onClose();
    } catch (err: unknown) {
      let message = "Failed to save tag.";
      if (err instanceof Error) {
        message = err.message;
      }
      if (err && typeof err === "object" && "response" in err) {
        const res = (err as { response?: { data?: { message?: string } } }).response;
        if (res?.data?.message) {
          message = res.data.message;
        }
      }
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm transition-opacity"
    >
      <div className="bg-white dark:bg-gray-900 rounded-2xl shadow-2xl w-full max-w-lg flex flex-col overflow-hidden border border-gray-200 dark:border-gray-800 animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-800 flex justify-between items-center bg-gray-50/70 dark:bg-gray-800/50">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
              <TagIcon className="h-5 w-5" />
            </div>
            <div>
              <h2 id={titleId} className="text-lg font-bold text-gray-900 dark:text-white">
                {tag ? "Edit Tag" : "Create New Tag"}
              </h2>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                {tag ? "Update tag attributes and appearance" : "Add a new classification tag for lessons"}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
            aria-label="Close dialog"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4 overflow-y-auto max-h-[calc(85vh-130px)]">
          {error && (
            <div className="bg-red-50 dark:bg-red-900/30 text-red-600 dark:text-red-400 p-3.5 rounded-xl border border-red-200 dark:border-red-800 text-sm">
              {error}
            </div>
          )}

          {/* Name */}
          <div className="space-y-1.5">
            <Label htmlFor="tag-name">Tag Name <span className="text-red-500">*</span></Label>
            <Input
              id="tag-name"
              type="text"
              required
              placeholder="e.g. Vocabulary, Grammar, Business..."
              value={formData.name}
              onChange={(e) => handleNameChange(e.target.value)}
              disabled={loading}
              autoFocus
            />
          </div>

          {/* Slug */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <Label htmlFor="tag-slug">URL Slug</Label>
              <button
                type="button"
                onClick={() => {
                  setFormData((prev) => ({ ...prev, slug: slugify(prev.name) }));
                  setIsSlugTouched(false);
                }}
                className="text-xs text-emerald-600 dark:text-emerald-400 hover:underline inline-flex items-center gap-1"
              >
                <Sparkles className="w-3 h-3" /> Auto-generate
              </button>
            </div>
            <Input
              id="tag-slug"
              type="text"
              placeholder="e.g. vocabulary"
              value={formData.slug}
              onChange={(e) => {
                setIsSlugTouched(true);
                setFormData((prev) => ({ ...prev, slug: e.target.value }));
              }}
              disabled={loading}
            />
          </div>

          {/* Color & Preview */}
          <div className="space-y-2">
            <Label>Accent Color & Preview</Label>
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-2 p-2 rounded-xl border border-gray-200 dark:border-gray-800 bg-gray-50 dark:bg-gray-800/40">
                <input
                  type="color"
                  value={formData.color || "#10b981"}
                  onChange={(e) => setFormData((prev) => ({ ...prev, color: e.target.value }))}
                  className="h-8 w-8 cursor-pointer rounded-lg border-0 bg-transparent p-0"
                  aria-label="Pick custom color"
                />
                <span className="font-mono text-xs font-medium text-gray-700 dark:text-gray-300">
                  {formData.color || "#10b981"}
                </span>
              </div>

              {/* Live Preview Badge */}
              <div className="flex-1 flex items-center justify-end">
                <span
                  className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold shadow-sm"
                  style={{
                    backgroundColor: `${formData.color || "#10b981"}18`,
                    color: formData.color || "#10b981",
                    border: `1px solid ${formData.color || "#10b981"}40`,
                  }}
                >
                  <span
                    className="w-2 h-2 rounded-full"
                    style={{ backgroundColor: formData.color || "#10b981" }}
                  />
                  {formData.name.trim() || "Preview Tag"}
                </span>
              </div>
            </div>

            {/* Presets */}
            <div className="flex flex-wrap gap-1.5 pt-1">
              {PRESET_TAG_COLORS.map((c) => (
                <button
                  key={c.value}
                  type="button"
                  onClick={() => setFormData((prev) => ({ ...prev, color: c.value }))}
                  className={`h-6 w-6 rounded-full transition-transform hover:scale-110 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2 dark:focus:ring-offset-gray-900 ${
                    formData.color?.toLowerCase() === c.value.toLowerCase()
                      ? "ring-2 ring-emerald-500 ring-offset-2 dark:ring-offset-gray-900 scale-110"
                      : ""
                  }`}
                  style={{ backgroundColor: c.value }}
                  title={c.name}
                  aria-label={`Select ${c.name} color`}
                />
              ))}
            </div>
          </div>

          {/* Description */}
          <div className="space-y-1.5">
            <Label htmlFor="tag-desc">Description (Optional)</Label>
            <Textarea
              id="tag-desc"
              rows={2}
              placeholder="Short note about when to use this tag..."
              value={formData.description || ""}
              onChange={(e) => setFormData((prev) => ({ ...prev, description: e.target.value }))}
              disabled={loading}
            />
          </div>

          {/* Status Switch */}
          <div className="flex items-center justify-between rounded-xl border border-gray-200 dark:border-gray-800 p-3 bg-gray-50/50 dark:bg-gray-800/30">
            <div>
              <div className="text-sm font-medium text-gray-900 dark:text-white">Active Status</div>
              <div className="text-xs text-gray-500 dark:text-gray-400">
                Inactive tags are hidden from public filters and lesson tagging options.
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Badge variant={formData.status === "ACTIVE" ? "success" : "default"}>
                {formData.status}
              </Badge>
              <Switch
                checked={formData.status === "ACTIVE"}
                onChange={(checked) =>
                  setFormData((prev) => ({
                    ...prev,
                    status: (checked ? "ACTIVE" : "INACTIVE") as CmsItemStatus,
                  }))
                }
                disabled={loading}
                aria-label="Toggle active status"
              />
            </div>
          </div>

          {/* Footer Actions */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-gray-200 dark:border-gray-800">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={loading}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              disabled={loading || !formData.name.trim()}
            >
              {loading ? "Saving..." : tag ? "Update Tag" : "Create Tag"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
