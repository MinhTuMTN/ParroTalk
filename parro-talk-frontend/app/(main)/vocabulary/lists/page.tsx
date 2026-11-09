"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft, FolderPlus, Layers, Users } from "lucide-react";
import type { VocabularyListSummaryResponse } from "@/features/vocabulary/types/customList";
import {
  createCustomVocabularyList,
  fetchCustomVocabularyLists,
  shareCustomVocabularyList,
} from "@/features/vocabulary/services/customVocabularyListService";
import { CustomListCard } from "@/features/vocabulary/components/custom-lists/CustomListCard";
import { CreateCustomListModal } from "@/features/vocabulary/components/custom-lists/CreateCustomListModal";
import { ShareCustomListModal } from "@/features/vocabulary/components/custom-lists/ShareCustomListModal";
import { NotificationBellDropdown } from "@/features/vocabulary/components/custom-lists/NotificationBellDropdown";

export default function CustomVocabularyListsPage() {
  const [myLists, setMyLists] = useState<VocabularyListSummaryResponse[]>([]);
  const [sharedWithMe, setSharedWithMe] = useState<
    VocabularyListSummaryResponse[]
  >([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [sharingList, setSharingList] =
    useState<VocabularyListSummaryResponse | null>(null);

  const loadLists = useCallback(async () => {
    try {
      const data = await fetchCustomVocabularyLists();
      setMyLists(data.myLists);
      setSharedWithMe(data.sharedWithMe);
    } catch {
      // Handle offline/unauthenticated gracefully
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadLists();
  }, [loadLists]);

  const handleCreate = async (payload: {
    title: string;
    description?: string;
    targetCefr?: string;
  }) => {
    await createCustomVocabularyList(payload);
    await loadLists();
  };

  return (
    <div className="min-h-full bg-ink-50 text-ink-800">
      <div className="mx-auto max-w-[1100px] space-y-8 px-4 py-8 sm:px-8 sm:py-10">
        {/* Top bar */}
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <Link
              href="/vocabulary"
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-ink-500 hover:text-brand-600"
            >
              <ArrowLeft className="h-3.5 w-3.5" /> Quay lại Vocabulary
              Dashboard
            </Link>
            <h1 className="mt-2 text-2xl font-extrabold text-ink-900 sm:text-3xl">
              Danh sách từ vựng cá nhân
            </h1>
            <p className="mt-1 text-sm text-ink-500">
              Tự tạo bộ từ vựng riêng, tra cứu &amp; cào tự động từ Oxford +
              Merriam-Webster, chọn nét nghĩa CEFR và tạo bộ câu hỏi AI 8 dạng.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <NotificationBellDropdown onRealtimeEvent={loadLists} />
            <button
              type="button"
              onClick={() => setCreateOpen(true)}
              className="inline-flex items-center gap-2 rounded-xl bg-brand-600 px-4 py-2.5 text-xs font-bold text-white shadow-sm hover:bg-brand-700"
            >
              <FolderPlus className="h-4 w-4" /> Tạo danh sách mới
            </button>
          </div>
        </div>

        {/* My Lists */}
        <section className="space-y-4">
          <div className="flex items-center gap-2">
            <Layers className="h-4.5 w-4.5 text-brand-600" />
            <h2 className="text-base font-bold text-ink-900">
              Danh sách của tôi ({myLists.length})
            </h2>
          </div>

          {loading ? (
            <div className="rounded-2xl border border-ink-200/70 bg-white p-8 text-center text-sm text-ink-400">
              Đang tải danh sách từ vựng...
            </div>
          ) : myLists.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-ink-300 bg-white p-10 text-center">
              <p className="text-sm font-semibold text-ink-700">
                Bạn chưa có danh sách từ vựng cá nhân nào.
              </p>
              <p className="mt-1 text-xs text-ink-500">
                Hãy tạo một danh sách riêng, thêm từ từ kho có sẵn hoặc yêu cầu
                hệ thống cào từ Oxford &amp; Merriam-Webster.
              </p>
              <button
                type="button"
                onClick={() => setCreateOpen(true)}
                className="mt-4 inline-flex items-center gap-1.5 rounded-xl bg-brand-600 px-4 py-2 text-xs font-bold text-white hover:bg-brand-700"
              >
                <FolderPlus className="h-4 w-4" /> Tạo danh sách đầu tiên
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
              {myLists.map((list) => (
                <CustomListCard
                  key={list.id}
                  list={list}
                  onShare={(l) => setSharingList(l)}
                />
              ))}
            </div>
          )}
        </section>

        {/* Shared With Me */}
        <section className="space-y-4">
          <div className="flex items-center gap-2">
            <Users className="h-4.5 w-4.5 text-ocean-600" />
            <h2 className="text-base font-bold text-ink-900">
              Được chia sẻ với tôi ({sharedWithMe.length})
            </h2>
          </div>

          {sharedWithMe.length === 0 ? (
            <div className="rounded-2xl border border-ink-200/60 bg-white p-6 text-center text-xs text-ink-400">
              Chưa có danh sách nào được chia sẻ với bạn.
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
              {sharedWithMe.map((list) => (
                <CustomListCard key={list.id} list={list} />
              ))}
            </div>
          )}
        </section>
      </div>

      <CreateCustomListModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onSubmit={handleCreate}
      />

      {sharingList && (
        <ShareCustomListModal
          open={Boolean(sharingList)}
          listId={sharingList.id}
          listTitle={sharingList.title}
          shareToken={sharingList.shareToken}
          onClose={() => setSharingList(null)}
          onShareSubmit={async (payload) => {
            await shareCustomVocabularyList(sharingList.id, payload);
            await loadLists();
          }}
        />
      )}
    </div>
  );
}
