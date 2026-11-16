"use client";

import Link from "next/link";
import {
  BookOpen,
  CheckCircle2,
  Clock,
  Globe,
  Layers,
  Lock,
  Play,
  Share2,
  Sparkles,
} from "lucide-react";
import type { VocabularyListSummaryResponse } from "../../types/customList";
import { resolveIsOwner } from "./customListUtils";

export function CustomListCard({
  list,
  onShare,
}: {
  list: VocabularyListSummaryResponse;
  onShare?: (list: VocabularyListSummaryResponse) => void;
}) {
  const statusBadge = () => {
    switch (list.status) {
      case "ACTIVE":
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-0.5 text-[11px] font-semibold text-emerald-700 border border-emerald-200">
            <CheckCircle2 className="h-3 w-3" /> Sẵn sàng luyện tập
          </span>
        );
      case "GENERATING_QUESTIONS":
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2.5 py-0.5 text-[11px] font-semibold text-amber-700 border border-amber-200">
            <Sparkles className="h-3 w-3 animate-spin" /> AI đang tạo câu hỏi...
          </span>
        );
      case "CRAWLING":
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-sky-50 px-2.5 py-0.5 text-[11px] font-semibold text-sky-700 border border-sky-200">
            <Clock className="h-3 w-3 animate-spin" /> Đang thu thập từ vựng...
          </span>
        );
      case "READY_TO_FINALIZE":
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-indigo-50 px-2.5 py-0.5 text-[11px] font-semibold text-indigo-700 border border-indigo-200">
            <Clock className="h-3 w-3" /> Sẵn sàng chốt
          </span>
        );
      case "PARTIAL_ERROR":
      case "ERROR":
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-rose-50 px-2.5 py-0.5 text-[11px] font-semibold text-rose-700 border border-rose-200">
            <Clock className="h-3 w-3" /> Có lỗi khi xử lý
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-ink-100 px-2.5 py-0.5 text-[11px] font-semibold text-ink-600">
            <Clock className="h-3 w-3" /> Bản nháp (Draft)
          </span>
        );
    }
  };

  return (
    <div className="group flex flex-col justify-between rounded-2xl border border-ink-200/80 bg-white p-5 shadow-card transition-all hover:border-brand-300 hover:shadow-md">
      <div>
        <div className="flex items-start justify-between gap-2">
          <div className="flex flex-wrap items-center gap-1.5">
            {statusBadge()}
            {list.targetCefr && (
              <span className="rounded-full bg-brand-50 px-2 py-0.5 text-[11px] font-bold text-brand-700">
                CEFR {list.targetCefr}
              </span>
            )}
            <span className="inline-flex items-center gap-1 rounded-full bg-ink-50 px-2 py-0.5 text-[11px] text-ink-500">
              {list.visibility === "PRIVATE" ? (
                <>
                  <Lock className="h-3 w-3" /> Riêng tư
                </>
              ) : (
                <>
                  <Globe className="h-3 w-3" /> Có link chia sẻ
                </>
              )}
            </span>
          </div>
          {resolveIsOwner(list) && onShare && (
            <button
              type="button"
              onClick={() => onShare(list)}
              className="rounded-lg p-1.5 text-ink-400 hover:bg-ink-100 hover:text-ink-700"
              title="Chia sẻ danh sách"
            >
              <Share2 className="h-4 w-4" />
            </button>
          )}
        </div>

        <Link href={`/vocabulary/lists/${list.id}`} className="mt-3 block">
          <h3 className="text-base font-bold text-ink-900 group-hover:text-brand-600">
            {list.title}
          </h3>
          {list.description && (
            <p className="mt-1 line-clamp-2 text-xs text-ink-500">
              {list.description}
            </p>
          )}
        </Link>
      </div>

      <div className="mt-4 flex items-center justify-between border-t border-ink-100 pt-3 text-xs text-ink-500">
        <div className="flex items-center gap-3">
          <span className="inline-flex items-center gap-1 font-medium text-ink-700">
            <BookOpen className="h-3.5 w-3.5 text-brand-600" />
            {list.wordCount} từ
          </span>
          {list.selectedSenseCount != null ? (
            <span className="inline-flex items-center gap-1">
              <Layers className="h-3.5 w-3.5 text-ocean-600" />
              {list.selectedSenseCount} nét nghĩa
            </span>
          ) : list.targetCefr ? (
            <span className="inline-flex items-center gap-1 font-medium text-brand-700">
              <Layers className="h-3.5 w-3.5 text-brand-600" />
              Trình độ: {list.targetCefr}
            </span>
          ) : null}
        </div>

        <div className="flex items-center gap-2">
          <Link
            href={`/vocabulary/lists/${list.id}`}
            className="rounded-lg px-2.5 py-1 text-xs font-semibold text-ink-700 hover:bg-ink-100"
          >
            Chi tiết
          </Link>
          {list.status === "ACTIVE" && (
            <Link
              href={`/vocabulary/lists/${list.id}/practice`}
              className="inline-flex items-center gap-1 rounded-lg bg-brand-600 px-3 py-1 text-xs font-semibold text-white hover:bg-brand-700"
            >
              <Play className="h-3 w-3" /> Luyện tập
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}
