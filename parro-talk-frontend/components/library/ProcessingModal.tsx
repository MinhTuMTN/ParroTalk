"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import {
  X,
  Hourglass,
  Loader2,
  CheckCircle2,
  AlertCircle,
  RotateCcw,
  Trash2,
  Play,
  Sparkles,
  Volume2,
} from "lucide-react";
import type { ProcessingJobItem } from "./types";

export interface ProcessingModalProps {
  isOpen: boolean;
  onClose: () => void;
  jobs?: ProcessingJobItem[];
  onRetry?: (jobId: string) => Promise<void> | void;
  onCancel?: (jobId: string) => Promise<void> | void;
  onClearCompleted?: () => void;
  onPractice?: (jobId: string) => void;
  className?: string;
}

export default function ProcessingModal({
  isOpen,
  onClose,
  jobs = [],
  onRetry,
  onCancel,
  onClearCompleted,
  onPractice,
  className = "",
}: ProcessingModalProps) {
  const [retryingIds, setRetryingIds] = useState<Set<string>>(new Set());
  const [cancellingIds, setCancellingIds] = useState<Set<string>>(new Set());

  // Close on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && isOpen) {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose]);

  // Prevent body scrolling when modal is open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
    return () => {
      document.body.style.overflow = "";
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const activeJobs = jobs.filter(
    (j) => j.status === "PENDING" || j.status === "PROCESSING"
  );
  const completedJobs = jobs.filter(
    (j) => j.status === "DONE" || j.status === "FAILED"
  );

  const handleRetry = async (jobId: string) => {
    if (!onRetry) return;
    setRetryingIds((prev) => new Set(prev).add(jobId));
    try {
      await onRetry(jobId);
    } catch (err) {
      console.error("Retry failed:", err);
    } finally {
      setRetryingIds((prev) => {
        const next = new Set(prev);
        next.delete(jobId);
        return next;
      });
    }
  };

  const handleCancel = async (jobId: string) => {
    if (!onCancel) return;
    setCancellingIds((prev) => new Set(prev).add(jobId));
    try {
      await onCancel(jobId);
    } catch (err) {
      console.error("Cancel failed:", err);
    } finally {
      setCancellingIds((prev) => {
        const next = new Set(prev);
        next.delete(jobId);
        return next;
      });
    }
  };

  const getStatusText = (job: ProcessingJobItem) => {
    switch (job.status) {
      case "PENDING":
        return "Đang chờ vào hàng đợi xử lý...";
      case "PROCESSING":
        return "Đang xử lý âm thanh & tạo transcript...";
      case "DONE":
        return "Hoàn thành! Sẵn sàng luyện tập.";
      case "FAILED":
        return job.errorMessage || "Lỗi xử lý âm thanh. Vui lòng thử lại.";
    }
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 sm:p-6 animate-in fade-in duration-200">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-slate-900/60 backdrop-blur-xs transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Modal Container */}
      <div
        className={`relative bg-white w-full max-w-xl rounded-3xl shadow-2xl border border-emerald-100/80 overflow-hidden flex flex-col max-h-[85vh] z-10 animate-in zoom-in-95 duration-200 ${className}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby="processing-modal-title"
      >
        {/* Modal Header */}
        <div className="px-6 py-5 border-b border-slate-100 bg-[#F0FDF4] flex items-center justify-between shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-white text-emerald-700 flex items-center justify-center shadow-xs border border-emerald-200/60">
              <Hourglass className="w-5 h-5 text-amber-500 animate-spin" style={{ animationDuration: "6s" }} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2
                  id="processing-modal-title"
                  className="text-lg font-bold text-slate-900"
                >
                  Tiến trình xử lý bài học
                </h2>
                {activeJobs.length > 0 && (
                  <span className="bg-amber-100 text-amber-900 text-xs font-bold px-2 py-0.5 rounded-full">
                    {activeJobs.length} đang chạy
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-600 mt-0.5">
                Hệ thống AI đang trích xuất giọng nói, từ vựng và bài tập
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng cửa sổ"
            className="w-8 h-8 rounded-full bg-white hover:bg-slate-100 text-slate-400 hover:text-slate-700 flex items-center justify-center transition-colors shadow-2xs border border-slate-200/60 cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Content / Job List */}
        <div className="p-6 overflow-y-auto space-y-4 flex-1">
          {jobs.length === 0 ? (
            <div className="text-center py-12 px-4 flex flex-col items-center justify-center">
              <div className="w-16 h-16 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center mb-3">
                <Sparkles className="w-8 h-8" />
              </div>
              <h3 className="text-base font-bold text-slate-800">
                Không có tác vụ nào đang xử lý
              </h3>
              <p className="text-xs text-slate-500 max-w-sm mt-1 leading-relaxed">
                Khi bạn tải lên file âm thanh hoặc video bài học mới, tiến trình nhận diện giọng nói và phân đoạn AI sẽ xuất hiện tại đây.
              </p>
            </div>
          ) : (
            jobs.map((job) => {
              const isRetrying = retryingIds.has(job.id);
              const isCancelling = cancellingIds.has(job.id);
              const clampedProgress = Math.min(
                100,
                Math.max(0, Math.round(job.progressPercent || 0))
              );

              return (
                <div
                  key={job.id}
                  className="bg-slate-50/70 hover:bg-slate-50 rounded-2xl p-4 border border-slate-200/70 transition-colors flex flex-col gap-3"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-start gap-3 min-w-0">
                      {/* Status Icon */}
                      <div className="mt-0.5 shrink-0">
                        {job.status === "DONE" && (
                          <div className="w-8 h-8 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center shadow-2xs">
                            <CheckCircle2 className="w-5 h-5" />
                          </div>
                        )}
                        {(job.status === "PROCESSING" || job.status === "PENDING") && (
                          <div className="w-8 h-8 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center shadow-2xs">
                            <Loader2 className="w-5 h-5 animate-spin" />
                          </div>
                        )}
                        {job.status === "FAILED" && (
                          <div className="w-8 h-8 rounded-xl bg-rose-100 text-rose-700 flex items-center justify-center shadow-2xs">
                            <AlertCircle className="w-5 h-5" />
                          </div>
                        )}
                      </div>

                      {/* Job Title & Subtext */}
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <Volume2 className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                          <h4 className="font-bold text-sm text-slate-900 truncate">
                            {job.title}
                          </h4>
                        </div>
                        <p
                          className={`text-xs mt-1 font-medium ${
                            job.status === "FAILED"
                              ? "text-rose-600"
                              : job.status === "DONE"
                              ? "text-emerald-700"
                              : "text-amber-800"
                          }`}
                        >
                          {getStatusText(job)}
                        </p>
                      </div>
                    </div>

                    {/* Percentage or Status Badge */}
                    <div className="shrink-0 text-right">
                      {job.status === "PROCESSING" && (
                        <span className="text-xs font-black text-amber-800 bg-amber-100 px-2 py-0.5 rounded-full">
                          {clampedProgress}%
                        </span>
                      )}
                      {job.status === "DONE" && (
                        <span className="text-xs font-black text-emerald-800 bg-emerald-100 px-2 py-0.5 rounded-full">
                          100%
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Progress Bar for Active Processing */}
                  {(job.status === "PROCESSING" || job.status === "PENDING") && (
                    <div className="w-full bg-amber-100/70 rounded-full h-2 overflow-hidden relative">
                      <div
                        className="bg-amber-500 h-full rounded-full transition-all duration-500 relative overflow-hidden"
                        style={{ width: `${clampedProgress || 20}%` }}
                      >
                        <div className="absolute inset-0 bg-white/30 animate-[pulse_1.5s_infinite]" />
                      </div>
                    </div>
                  )}

                  {/* Action Buttons */}
                  <div className="flex items-center justify-end gap-2 pt-1">
                    {job.status === "DONE" && (
                      <Link
                        href={`/practice/${job.id}`}
                        onClick={() => {
                          onPractice?.(job.id);
                          onClose();
                        }}
                        className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition-all shadow-2xs active:scale-95 cursor-pointer"
                      >
                        <Play className="w-3.5 h-3.5 fill-current" />
                        <span>Luyện tập ngay</span>
                      </Link>
                    )}

                    {job.status === "FAILED" && onRetry && (
                      <button
                        type="button"
                        onClick={() => handleRetry(job.id)}
                        disabled={isRetrying}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold transition-all shadow-2xs active:scale-95 disabled:opacity-50 cursor-pointer"
                      >
                        <RotateCcw
                          className={`w-3.5 h-3.5 ${
                            isRetrying ? "animate-spin" : ""
                          }`}
                        />
                        <span>{isRetrying ? "Đang thử lại..." : "Thử lại"}</span>
                      </button>
                    )}

                    {onCancel && (
                      <button
                        type="button"
                        onClick={() => handleCancel(job.id)}
                        disabled={isCancelling}
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-xl text-slate-500 hover:text-rose-600 hover:bg-rose-50 text-xs font-semibold transition-colors cursor-pointer disabled:opacity-50"
                        title={
                          job.status === "PROCESSING" || job.status === "PENDING"
                            ? "Hủy xử lý"
                            : "Xóa tác vụ"
                        }
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        <span>
                          {job.status === "PROCESSING" || job.status === "PENDING"
                            ? "Hủy"
                            : "Xóa"}
                        </span>
                      </button>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Modal Footer */}
        <div className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex items-center justify-between shrink-0">
          <div>
            {completedJobs.length > 0 && onClearCompleted && (
              <button
                type="button"
                onClick={onClearCompleted}
                className="text-xs font-bold text-slate-500 hover:text-slate-800 flex items-center gap-1.5 transition-colors cursor-pointer"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>Xóa các mục đã xong ({completedJobs.length})</span>
              </button>
            )}
          </div>

          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs shadow-xs transition-all active:scale-95 cursor-pointer"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
}
