"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  CopyPlus,
  Globe,
  Loader2,
  Play,
  Plus,
  Search,
  Settings2,
  Share2,
  Sparkles,
  Trash2,
  Volume2,
} from "lucide-react";
import type {
  VocabularyListDetailResponse,
  VocabularyListItemResponse,
} from "@/features/vocabulary/types/customList";
import {
  addWordToCustomList,
  cloneCustomVocabularyList,
  fetchCustomVocabularyListDetail,
  finalizeCustomListAndGenerateQuestions,
  generateMoreQuestionsForSense,
  removeWordFromCustomList,
  searchWordInDatabase,
  shareCustomVocabularyList,
  updateCustomListItemSelection,
} from "@/features/vocabulary/services/customVocabularyListService";
import { SenseSelectionDrawer } from "@/features/vocabulary/components/custom-lists/SenseSelectionDrawer";
import { ShareCustomListModal } from "@/features/vocabulary/components/custom-lists/ShareCustomListModal";
import { NotificationBellDropdown } from "@/features/vocabulary/components/custom-lists/NotificationBellDropdown";
import {
  formatDataSourceBadge,
  resolveIsOwner,
  resolveItemDisplayWord,
} from "@/features/vocabulary/components/custom-lists/customListUtils";

export default function CustomVocabularyListDetailPage() {
  const params = useParams<{ id: string }>();
  const searchParams = useSearchParams();
  const router = useRouter();
  const listId = params.id;
  const shareToken = searchParams.get("shareToken") || undefined;

  const [detail, setDetail] = useState<VocabularyListDetailResponse | null>(
    null
  );
  const isOwner = resolveIsOwner(detail);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState("");
  const [searching, setSearching] = useState(false);
  const [searchResults, setSearchResults] = useState<
    VocabularyListItemResponse[]
  >([]);
  const [activeItem, setActiveItem] =
    useState<VocabularyListItemResponse | null>(null);
  const [shareOpen, setShareOpen] = useState(false);
  const [finalizing, setFinalizing] = useState(false);
  const [cloning, setCloning] = useState(false);

  const loadDetail = useCallback(async () => {
    try {
      const data = await fetchCustomVocabularyListDetail(listId, shareToken);
      setDetail(data);
    } finally {
      setLoading(false);
    }
  }, [listId, shareToken]);

  useEffect(() => {
    loadDetail();
  }, [loadDetail]);

  const handleSearchDb = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!query.trim()) return;
    setSearching(true);
    try {
      const results = await searchWordInDatabase(query.trim());
      setSearchResults(results);
    } finally {
      setSearching(false);
    }
  };

  const handleAddFromDb = async (vocabId: string) => {
    await addWordToCustomList(listId, {
      vocabularyId: vocabId,
      crawlIfMissing: false,
    });
    setQuery("");
    setSearchResults([]);
    await loadDetail();
  };

  const handleRequestBackgroundCrawl = async () => {
    if (!query.trim()) return;
    await addWordToCustomList(listId, {
      rawWord: query.trim(),
      crawlIfMissing: true,
    });
    setQuery("");
    setSearchResults([]);
    await loadDetail();
  };

  const handleFinalize = async () => {
    setFinalizing(true);
    try {
      const updated = await finalizeCustomListAndGenerateQuestions(listId);
      setDetail(updated);
    } finally {
      setFinalizing(false);
    }
  };

  const handleClone = async () => {
    setCloning(true);
    try {
      const cloned = await cloneCustomVocabularyList(listId, shareToken);
      router.push(`/vocabulary/lists/${cloned.id}`);
    } finally {
      setCloning(false);
    }
  };

  const playAudio = (url?: string) => {
    if (!url) return;
    new Audio(url).play().catch(() => {});
  };

  if (loading || !detail) {
    return (
      <div className="mx-auto max-w-[1100px] p-8 text-center text-sm text-ink-500">
        Đang tải chi tiết danh sách từ vựng...
      </div>
    );
  }

  return (
    <div className="min-h-full bg-ink-50 text-ink-800">
      <div className="mx-auto max-w-[1100px] space-y-6 px-4 py-8 sm:px-8">
        {/* Header */}
        <div className="flex flex-wrap items-start justify-between gap-4 rounded-2xl border border-ink-200/80 bg-white p-6 shadow-card">
          <div>
            <Link
              href="/vocabulary/lists"
              className="inline-flex items-center gap-1 text-xs font-semibold text-ink-500 hover:text-brand-600"
            >
              <ArrowLeft className="h-3.5 w-3.5" /> Tất cả danh sách cá nhân
            </Link>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <h1 className="text-2xl font-extrabold text-ink-900">
                {detail.title}
              </h1>
              {detail.targetCefr && (
                <span className="rounded-full bg-brand-100 px-2.5 py-0.5 text-xs font-bold text-brand-700">
                  Mục tiêu {detail.targetCefr}
                </span>
              )}
              <span className="rounded-full bg-ink-100 px-2.5 py-0.5 text-xs font-semibold text-ink-700">
                {detail.status === "ACTIVE"
                  ? "Đã tạo câu hỏi AI"
                  : detail.status === "GENERATING_QUESTIONS"
                    ? "Đang tạo câu hỏi AI..."
                    : "Bản nháp"}
              </span>
            </div>
            {detail.description && (
              <p className="mt-1 text-xs text-ink-500">{detail.description}</p>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-2.5">
            <NotificationBellDropdown onRealtimeEvent={loadDetail} />

            {isOwner ? (
              <>
                <button
                  type="button"
                  onClick={() => setShareOpen(true)}
                  className="inline-flex items-center gap-1.5 rounded-xl border border-ink-200 bg-white px-3.5 py-2 text-xs font-semibold text-ink-700 hover:bg-ink-50"
                >
                  <Share2 className="h-4 w-4" /> Chia sẻ
                </button>
                <button
                  type="button"
                  disabled={finalizing || detail.items.length === 0}
                  onClick={handleFinalize}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-ocean-600 px-4 py-2 text-xs font-bold text-white hover:bg-ocean-700 disabled:opacity-50"
                >
                  <Sparkles className="h-4 w-4" />
                  {finalizing
                    ? "Đang gửi job AI..."
                    : "Chốt từ vựng & Tạo câu hỏi AI (Batch)"}
                </button>
              </>
            ) : (
              <button
                type="button"
                disabled={cloning}
                onClick={handleClone}
                className="inline-flex items-center gap-1.5 rounded-xl border border-brand-200 bg-brand-50 px-3.5 py-2 text-xs font-bold text-brand-700 hover:bg-brand-100"
              >
                <CopyPlus className="h-4 w-4" />
                {cloning
                  ? "Đang sao chép..."
                  : "Sao chép (Clone) về danh sách của tôi"}
              </button>
            )}

            <Link
              href={`/vocabulary/lists/${detail.id}/practice${
                shareToken ? `?shareToken=${shareToken}` : ""
              }`}
              className="inline-flex items-center gap-1.5 rounded-xl bg-brand-600 px-4 py-2 text-xs font-bold text-white hover:bg-brand-700"
            >
              <Play className="h-4 w-4" /> Luyện tập Multiple Choice
            </Link>
          </div>
        </div>

        {/* 2-Option Word Adder (Only for Owner) */}
        {isOwner && (
          <div className="rounded-2xl border border-ink-200/80 bg-white p-6 shadow-card">
            <h2 className="text-sm font-bold text-ink-900">
              Thêm từ vựng vào danh sách (2 nguồn: Kho dữ liệu sẵn có hoặc Cào
              tự động Oxford + Merriam-Webster)
            </h2>
            <form onSubmit={handleSearchDb} className="mt-3 flex gap-2">
              <div className="relative flex-1">
                <Search className="pointer-events-none absolute top-2.5 left-3.5 h-4 w-4 text-ink-400" />
                <input
                  type="text"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Nhập từ vựng tiếng Anh (VD: resilient, mitigate,ubiquitous)..."
                  className="w-full rounded-xl border border-ink-200 py-2 pr-4 pl-10 text-sm text-ink-900 focus:border-brand-500 focus:outline-hidden"
                />
              </div>
              <button
                type="submit"
                disabled={searching || !query.trim()}
                className="rounded-xl bg-ink-900 px-4 py-2 text-xs font-semibold text-white hover:bg-ink-800 disabled:opacity-50"
              >
                {searching ? "Đang tìm..." : "Option 1: Tìm trong Database"}
              </button>
              <button
                type="button"
                disabled={!query.trim()}
                onClick={handleRequestBackgroundCrawl}
                className="inline-flex items-center gap-1.5 rounded-xl border border-brand-300 bg-brand-50 px-4 py-2 text-xs font-bold text-brand-700 hover:bg-brand-100 disabled:opacity-50"
              >
                <Globe className="h-4 w-4" /> Option 2: Cào từ Oxford +
                Merriam-Webster
              </button>
            </form>

            {searchResults.length > 0 && (
              <div className="mt-4 space-y-2 rounded-xl border border-ink-100 bg-ink-50/60 p-3">
                <div className="text-xs font-semibold text-ink-600">
                  Kết quả trong Database ({searchResults.length}):
                </div>
                {searchResults.map((res) => (
                  <div
                    key={res.vocabularyId}
                    className="flex items-center justify-between rounded-xl bg-white p-3 shadow-2xs"
                  >
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-sm text-ink-900">
                          {res.word}
                        </span>
                        <span className="text-xs text-ink-500">
                          {res.phoneticUk || res.phoneticUs}
                        </span>
                        {res.dataSource && (
                          <span className="rounded-md bg-ink-100 px-2 py-0.5 text-[10px] font-medium text-ink-600">
                            {formatDataSourceBadge(res.dataSource)}
                          </span>
                        )}
                      </div>
                      <div className="mt-0.5 text-xs text-ink-500">
                        {res.availableSenses.length} nét nghĩa •{" "}
                        {res.availableIdioms.length} idioms •{" "}
                        {res.availablePhrasalVerbs.length} phrasal verbs
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() =>
                        res.vocabularyId && handleAddFromDb(res.vocabularyId)
                      }
                      className="inline-flex items-center gap-1 rounded-xl bg-brand-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-brand-700"
                    >
                      <Plus className="h-3.5 w-3.5" /> Thêm vào danh sách
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Word Items List */}
        <div className="space-y-3">
          <h2 className="text-base font-bold text-ink-900">
            Danh sách từ vựng ({detail.items.length})
          </h2>

          {detail.items.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-ink-300 bg-white p-8 text-center text-xs text-ink-500">
              Chưa có từ vựng nào trong danh sách này. Hãy tìm từ trong Database
              hoặc bấm &ldquo;Cào từ Oxford + Merriam-Webster&rdquo;.
            </div>
          ) : (
            detail.items.map((item) => {
              const selectedSenses = item.availableSenses.filter((s) =>
                item.selectedSenseIds.includes(s.id)
              );
              return (
                <div
                  key={item.id}
                  className="rounded-2xl border border-ink-200/80 bg-white p-5 shadow-card"
                >
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-lg font-extrabold text-ink-900">
                          {resolveItemDisplayWord(item)}
                        </span>
                        {item.phoneticUk && (
                          <button
                            type="button"
                            onClick={() => playAudio(item.audioUkUrl)}
                            className="inline-flex items-center gap-1 rounded-md bg-ink-50 px-2 py-0.5 text-xs text-ink-600 hover:text-brand-600"
                          >
                            <Volume2 className="h-3 w-3" /> {item.phoneticUk}
                          </button>
                        )}
                        {item.dataSource && (
                          <span className="rounded-full bg-ocean-50 px-2.5 py-0.5 text-[11px] font-semibold text-ocean-700">
                            {formatDataSourceBadge(item.dataSource)}
                          </span>
                        )}
                        {item.crawlStatus === "PENDING" ||
                        item.crawlStatus === "PROCESSING" ? (
                          <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2.5 py-0.5 text-xs font-semibold text-amber-700">
                            <Loader2 className="h-3 w-3 animate-spin" /> Đang
                            cào dữ liệu ngầm từ Oxford...
                          </span>
                        ) : item.crawlStatus === "NOT_FOUND" ||
                          item.crawlStatus === "FAILED" ? (
                          <span className="inline-flex items-center gap-1 rounded-full bg-red-50 px-2.5 py-0.5 text-xs font-semibold text-red-700">
                            <AlertCircle className="h-3 w-3" />{" "}
                            {item.crawlErrorMessage || "Không tìm thấy từ"}
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-0.5 text-[11px] font-semibold text-emerald-700">
                            <CheckCircle2 className="h-3 w-3" /> Đã chọn{" "}
                            {item.selectedSenseIds.length}/
                            {item.availableSenses.length} nghĩa
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      {item.availableSenses.length > 0 && (
                        <button
                          type="button"
                          onClick={() => setActiveItem(item)}
                          className="inline-flex items-center gap-1.5 rounded-xl border border-ink-200 px-3 py-1.5 text-xs font-semibold text-ink-700 hover:bg-ink-50"
                        >
                          <Settings2 className="h-3.5 w-3.5" /> Chọn nghĩa CEFR
                          / Idioms / Tạo thêm câu hỏi
                        </button>
                      )}
                      {isOwner && (
                        <button
                          type="button"
                          onClick={async () => {
                            await removeWordFromCustomList(listId, item.id);
                            await loadDetail();
                          }}
                          className="rounded-lg p-1.5 text-ink-400 hover:bg-red-50 hover:text-red-600"
                          title="Xoá từ khỏi danh sách"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      )}
                    </div>
                  </div>

                  {selectedSenses.length > 0 && (
                    <div className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
                      {selectedSenses.map((s) => (
                        <div
                          key={s.id}
                          className="rounded-xl border border-ink-100 bg-ink-50/50 p-3 text-xs"
                        >
                          <div className="flex items-center gap-1.5">
                            {s.cefrLevel && (
                              <span className="rounded bg-brand-100 px-1.5 py-0.5 text-[10px] font-bold text-brand-700">
                                {s.cefrLevel}
                              </span>
                            )}
                            <span className="font-semibold italic text-ink-600">
                              {s.partOfSpeech}
                            </span>
                            <span className="ml-auto text-[10px] text-ink-400">
                              {s.questionCount ?? 0} câu hỏi
                            </span>
                          </div>
                          <p className="mt-1 font-medium text-ink-900">
                            {s.definitionEn}
                          </p>
                          {s.definitionVn && (
                            <p className="mt-0.5 text-brand-700">
                              → {s.definitionVn}
                            </p>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>
      </div>

      <SenseSelectionDrawer
        item={activeItem}
        isOwner={isOwner}
        onClose={() => setActiveItem(null)}
        onSaveSelection={async (itemId, payload) => {
          await updateCustomListItemSelection(listId, itemId, payload);
          await loadDetail();
        }}
        onGenerateMoreForSense={async (senseId) => {
          await generateMoreQuestionsForSense(listId, senseId, 3);
          await loadDetail();
        }}
      />

      <ShareCustomListModal
        open={shareOpen}
        listId={detail.id}
        listTitle={detail.title}
        shareToken={detail.shareToken}
        shares={detail.shares}
        onClose={() => setShareOpen(false)}
        onShareSubmit={async (payload) => {
          const updated = await shareCustomVocabularyList(detail.id, payload);
          setDetail(updated);
        }}
      />
    </div>
  );
}
