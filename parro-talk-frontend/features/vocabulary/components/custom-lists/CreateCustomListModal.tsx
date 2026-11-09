"use client";

import { useState } from "react";
import { X } from "lucide-react";

export function CreateCustomListModal({
  open,
  onClose,
  onSubmit,
}: {
  open: boolean;
  onClose: () => void;
  onSubmit: (payload: {
    title: string;
    description?: string;
    targetCefr?: string;
  }) => Promise<void>;
}) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [targetCefr, setTargetCefr] = useState("B2");
  const [submitting, setSubmitting] = useState(false);

  if (!open) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    setSubmitting(true);
    try {
      await onSubmit({
        title: title.trim(),
        description: description.trim() || undefined,
        targetCefr,
      });
      setTitle("");
      setDescription("");
      onClose();
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink-900/50 p-4 backdrop-blur-xs">
      <div className="w-full max-w-md rounded-2xl border border-ink-200 bg-white p-6 shadow-xl">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-ink-900">
            Tạo danh sách từ vựng cá nhân
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1 text-ink-400 hover:bg-ink-100 hover:text-ink-700"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-ink-700">
              Tên danh sách *
            </label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="VD: IELTS Academic Environment & Policy"
              className="mt-1 w-full rounded-xl border border-ink-200 px-3.5 py-2 text-sm text-ink-900 focus:border-brand-500 focus:outline-hidden"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-ink-700">
              Mô tả ngắn (Tuỳ chọn)
            </label>
            <textarea
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Ghi chú mục tiêu ôn tập của danh sách này..."
              className="mt-1 w-full rounded-xl border border-ink-200 px-3.5 py-2 text-sm text-ink-900 focus:border-brand-500 focus:outline-hidden"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-ink-700">
              Mức CEFR mục tiêu (mặc định chọn các nghĩa từ A1 đến B2)
            </label>
            <select
              value={targetCefr}
              onChange={(e) => setTargetCefr(e.target.value)}
              className="mt-1 w-full rounded-xl border border-ink-200 px-3.5 py-2 text-sm text-ink-900 focus:border-brand-500 focus:outline-hidden"
            >
              <option value="A2">A2 (Sơ cấp)</option>
              <option value="B1">B1 (Trung cấp)</option>
              <option value="B2">B2 (Trung cao cấp - Khuyên dùng)</option>
              <option value="C1">C1 (Cao cấp)</option>
              <option value="C2">C2 (Thành thạo)</option>
            </select>
          </div>

          <div className="rounded-xl bg-ink-50 p-3 text-xs text-ink-600">
            Mặc định danh sách sẽ ở chế độ <strong>Riêng tư (Private)</strong>.
            Chỉ bạn và những người được bạn chia sẻ mới có quyền truy cập.
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-xl px-4 py-2 text-xs font-semibold text-ink-600 hover:bg-ink-100"
            >
              Huỷ
            </button>
            <button
              type="submit"
              disabled={submitting || !title.trim()}
              className="rounded-xl bg-brand-600 px-4 py-2 text-xs font-semibold text-white hover:bg-brand-700 disabled:opacity-50"
            >
              {submitting ? "Đang tạo..." : "Tạo danh sách"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
