"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ArrowRight, FolderPlus, Plus, Sparkles, Users } from "lucide-react";
import type { VocabularyListSummaryResponse } from "../../types/customList";
import {
  createCustomVocabularyList,
  fetchCustomVocabularyLists,
} from "../../services/customVocabularyListService";
import { SectionHeader } from "../shared";
import { CreateCustomListModal } from "../custom-lists/CreateCustomListModal";

interface Props {
  className?: string;
}

export function CustomListsSection({ className = "" }: Props) {
  const [lists, setLists] = useState<VocabularyListSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);

  const loadData = async () => {
    try {
      const data = await fetchCustomVocabularyLists();
      // Combine my lists and shared with me, latest first
      const combined = [...data.myLists, ...data.sharedWithMe].sort(
        (a, b) =>
          new Date(b.updatedAt || 0).getTime() -
          new Date(a.updatedAt || 0).getTime()
      );
      setLists(combined);
    } catch {
      // Ignore when offline or unauthorized
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreate = async (payload: {
    title: string;
    description?: string;
    targetCefr?: string;
  }) => {
    await createCustomVocabularyList(payload);
    await loadData();
  };

  return (
    <section className={`animate-fade-up [animation-delay:200ms] ${className}`}>
      <SectionHeader
        title="Bộ từ vựng của tôi"
        right={
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setCreateOpen(true)}
              className="inline-flex items-center gap-1.5 rounded-xl bg-brand-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm transition hover:bg-brand-700"
            >
              <Plus className="h-3.5 w-3.5" /> Tạo danh sách
            </button>
            <Link
              href="/vocabulary/lists"
              className="group inline-flex items-center gap-1 font-semibold text-brand-600 hover:text-brand-700"
            >
              Xem tất cả
              <ArrowRight className="h-3.5 w-3.5 transition-transform group-hover:translate-x-0.5" />
            </Link>
          </div>
        }
      />

      <div className="mt-3">
        {loading ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <div
                key={i}
                className="h-36 animate-pulse rounded-2xl border border-ink-200/70 bg-white"
              />
            ))}
          </div>
        ) : lists.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-ink-200 bg-white p-8 text-center sm:p-10">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-50 text-brand-600">
              <FolderPlus className="h-6 w-6" />
            </div>
            <h3 className="mt-3 text-sm font-bold text-ink-900 sm:text-base">
              Chưa có bộ từ vựng cá nhân nào
            </h3>
            <p className="mt-1 max-w-md text-xs text-ink-500 sm:text-sm">
              Tạo danh sách từ riêng của bạn, tự động cào từ điển Oxford +
              Merriam-Webster và tạo bộ bài tập trắc nghiệm thông minh với AI.
            </p>
            <button
              type="button"
              onClick={() => setCreateOpen(true)}
              className="mt-4 inline-flex items-center gap-2 rounded-xl bg-brand-600 px-4 py-2 text-xs font-bold text-white shadow-sm transition hover:bg-brand-700"
            >
              <Plus className="h-4 w-4" /> Tạo bộ từ đầu tiên
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {lists.slice(0, 3).map((list) => {
              const isShared = !list.owner;
              return (
                <div
                  key={list.id}
                  className="group relative flex flex-col justify-between rounded-2xl border border-ink-200/70 bg-white p-5 shadow-card transition-all duration-200 hover:-translate-y-0.5 hover:border-brand-200 hover:shadow-card-hover"
                >
                  <div>
                    <div className="flex items-center justify-between gap-2">
                      <span
                        className={`inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-[10px] font-semibold ${
                          isShared
                            ? "bg-ocean-100 text-ocean-700"
                            : "bg-brand-100 text-brand-700"
                        }`}
                      >
                        {isShared ? (
                          <>
                            <Users className="h-3 w-3" /> Shared with me
                          </>
                        ) : (
                          "My list"
                        )}
                      </span>
                      {list.status === "ACTIVE" && (
                        <span className="inline-flex items-center gap-1 rounded-md bg-emerald-50 px-2 py-0.5 text-[10px] font-semibold text-emerald-700">
                          <Sparkles className="h-3 w-3" /> Ready
                        </span>
                      )}
                    </div>

                    <Link
                      href={`/vocabulary/lists/${list.id}`}
                      className="mt-3 block"
                    >
                      <h4 className="truncate text-base font-bold text-ink-900 group-hover:text-brand-600">
                        {list.title}
                      </h4>
                      <p className="mt-1 line-clamp-2 min-h-[32px] text-xs text-ink-500">
                        {list.description || "Chưa có mô tả cho bộ từ này."}
                      </p>
                    </Link>
                  </div>

                  <div className="mt-4 flex items-center justify-between border-t border-ink-100 pt-3 text-xs">
                    <span className="font-medium text-ink-500">
                      <b className="font-bold text-ink-900">
                        {list.wordCount}
                      </b>{" "}
                      từ vựng
                    </span>
                    <div className="flex items-center gap-2">
                      <Link
                        href={`/vocabulary/lists/${list.id}`}
                        className="rounded-lg px-2.5 py-1 font-semibold text-ink-600 hover:bg-ink-100"
                      >
                        Chi tiết
                      </Link>
                      {list.status === "ACTIVE" && (
                        <Link
                          href={`/vocabulary/lists/${list.id}/practice`}
                          className="rounded-lg bg-brand-50 px-2.5 py-1 font-semibold text-brand-700 transition hover:bg-brand-100"
                        >
                          Luyện tập
                        </Link>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <CreateCustomListModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onSubmit={handleCreate}
      />
    </section>
  );
}
