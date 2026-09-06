import React, { useState } from "react";
import Link from "next/link";
import { Play, Clock, Upload, Loader2, AlertCircle, CheckCircle2, Trash2 } from "lucide-react";
import type { UploadedLessonItem } from "./types";
import ConfirmationModal from "@/components/ui/ConfirmationModal";

export interface UploadedLessonCardProps {
  item: UploadedLessonItem;
  onPractice?: (item: UploadedLessonItem) => void;
  onDelete?: (item: UploadedLessonItem) => Promise<void> | void;
  className?: string;
}

export default function UploadedLessonCard({
  item,
  onPractice,
  onDelete,
  className = "",
}: UploadedLessonCardProps) {
  const {
    id,
    title,
    status,
    durationMinutes,
    createdAt,
    thumbnailUrl,
    progress,
    errorMessage,
  } = item;

  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  const formattedDuration = durationMinutes > 0 ? `${durationMinutes} min` : "Mới tải lên";

  const formattedDate = (() => {
    if (!createdAt) return "";
    try {
      const d = new Date(createdAt);
      if (isNaN(d.getTime())) return createdAt;
      return d.toLocaleDateString("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      });
    } catch {
      return createdAt;
    }
  })();

  const isReady = status === "READY";
  const isProcessing = status === "PROCESSING";
  const isFailed = status === "FAILED";

  const handlePracticeClick = (e: React.MouseEvent) => {
    if (!isReady) {
      e.preventDefault();
      return;
    }
    if (onPractice) {
      e.preventDefault();
      onPractice(item);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!onDelete) {
      setIsDeleteModalOpen(false);
      return;
    }
    setIsDeleting(true);
    try {
      await onDelete(item);
    } catch (err) {
      console.error("Failed to delete lesson:", err);
    } finally {
      setIsDeleting(false);
      setIsDeleteModalOpen(false);
    }
  };

  return (
    <>
      <div
        className={`bg-white rounded-2xl border border-slate-200/70 overflow-hidden shadow-xs hover:shadow-md hover:border-emerald-300 transition-all duration-300 flex flex-col group h-full ${className}`}
      >
        {/* Cover Header */}
        <div className="h-32 relative overflow-hidden bg-gradient-to-br from-emerald-700 via-teal-700 to-emerald-900 flex items-center justify-center shrink-0">
          {thumbnailUrl ? (
            <img
              src={thumbnailUrl}
              alt={title}
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
            />
          ) : (
            <div className="w-full h-full flex flex-col items-center justify-center text-white/40 p-4">
              <Upload className="w-8 h-8 mb-1 text-white/50" />
              <span className="text-[10px] font-bold uppercase tracking-wider text-white/70">
                Bài học tải lên
              </span>
            </div>
          )}

          {/* Status Badge (Top Right) */}
          <div className="absolute top-2.5 right-2.5">
            {isReady && (
              <span className="inline-flex items-center gap-1 bg-white/95 text-emerald-700 font-bold text-[10px] px-2 py-0.5 rounded-full shadow-xs border border-emerald-200/60">
                <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                Sẵn sàng
              </span>
            )}
            {isProcessing && (
              <span className="inline-flex items-center gap-1 bg-amber-50 text-amber-800 font-bold text-[10px] px-2 py-0.5 rounded-full shadow-xs border border-amber-300/80">
                <Loader2 className="w-3 h-3 text-amber-600 animate-spin" />
                Đang xử lý
              </span>
            )}
            {isFailed && (
              <span className="inline-flex items-center gap-1 bg-rose-50 text-rose-700 font-bold text-[10px] px-2 py-0.5 rounded-full shadow-xs border border-rose-200">
                <AlertCircle className="w-3 h-3 text-rose-600" />
                Thất bại
              </span>
            )}
          </div>

          {/* Play Button Overlay (Only when Ready) */}
          {isReady && (
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
              <div className="w-11 h-11 rounded-full bg-white/95 text-emerald-700 flex items-center justify-center group-hover:scale-110 group-hover:bg-white shadow-md transition-all">
                <Play className="w-5 h-5 fill-emerald-700 ml-0.5" />
              </div>
            </div>
          )}
        </div>

        {/* Card Body */}
        <div className="p-4 flex flex-col flex-1 justify-between gap-3">
          <div>
            {/* Tag Row: Upload Pill + Duration + Date */}
            <div className="flex items-center justify-between text-xs mb-2">
              <span className="text-[10px] font-bold text-emerald-800 bg-emerald-100 px-2 py-0.5 rounded uppercase tracking-wider">
                Upload
              </span>
              <div className="flex items-center gap-2 text-slate-500 text-xs">
                <span className="flex items-center gap-1 font-medium">
                  <Clock className="w-3.5 h-3.5 text-slate-400" />
                  {formattedDuration}
                </span>
                {formattedDate && (
                  <span className="text-slate-400 hidden sm:inline">
                    · {formattedDate}
                  </span>
                )}
              </div>
            </div>

            {/* Lesson Title */}
            <h3 className="font-bold text-sm text-slate-900 leading-snug group-hover:text-emerald-700 transition-colors line-clamp-2">
              {title}
            </h3>

            {/* Processing details if active */}
            {isProcessing && (
              <div className="mt-2.5 space-y-1">
                <div className="flex justify-between text-[11px] text-amber-700 font-medium">
                  <span>Đang trích xuất audio & từ vựng...</span>
                  <span>{progress ?? 60}%</span>
                </div>
                <div className="w-full bg-amber-100 rounded-full h-1.5 overflow-hidden">
                  <div
                    className="bg-amber-500 h-full rounded-full transition-all duration-500 animate-pulse"
                    style={{ width: `${progress ?? 60}%` }}
                  />
                </div>
              </div>
            )}

            {isFailed && errorMessage && (
              <p className="mt-2 text-xs text-rose-600 line-clamp-1 font-medium">
                {errorMessage}
              </p>
            )}
          </div>

          {/* Action Buttons */}
          <div className="flex items-center gap-2 pt-2 border-t border-slate-100 mt-auto">
            {isReady ? (
              <Link
                href={`/practice/${id}`}
                prefetch={false}
                onClick={handlePracticeClick}
                className="flex-1 py-2 px-3 rounded-xl bg-slate-900 hover:bg-emerald-700 text-white font-bold text-xs text-center transition-colors shadow-2xs active:scale-95 flex items-center justify-center gap-1.5"
              >
                <Play className="w-3.5 h-3.5 fill-current" />
                <span>Luyện tập</span>
              </Link>
            ) : isProcessing ? (
              <button
                type="button"
                disabled
                className="flex-1 py-2 px-3 rounded-xl bg-slate-100 text-slate-400 font-bold text-xs text-center cursor-not-allowed flex items-center justify-center gap-1.5"
              >
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                <span>Đang xử lý</span>
              </button>
            ) : (
              <button
                type="button"
                disabled
                className="flex-1 py-2 px-3 rounded-xl bg-rose-50 text-rose-400 font-bold text-xs text-center cursor-not-allowed"
              >
                Lỗi tải lên
              </button>
            )}

            {onDelete && (
              <button
                type="button"
                onClick={() => setIsDeleteModalOpen(true)}
                aria-label="Xóa bài học"
                className="p-2 rounded-xl text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
      </div>

      {onDelete && (
        <ConfirmationModal
          isOpen={isDeleteModalOpen}
          onClose={() => setIsDeleteModalOpen(false)}
          onConfirm={handleDeleteConfirm}
          isLoading={isDeleting}
          title="Xóa bài học này?"
          message={`Bạn có chắc chắn muốn xóa bài học "${title}" không? Hành động này không thể hoàn tác.`}
          confirmText="Xóa bài học"
        />
      )}
    </>
  );
}
