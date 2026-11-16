import axiosInstance from "@/lib/axios";
import type {
  CustomListMcqResponse,
  FinalizeListResponse,
  ShareEntryDto,
  ShareListResponse,
  UserNotificationDto,
  VocabularyListDetailResponse,
  VocabularyListItemResponse,
  VocabularyListSummaryResponse,
} from "../types/customList";

interface ApiResponse<T> {
  code: number;
  message?: string;
  result: T;
}

function normalizeSummary(raw: VocabularyListSummaryResponse): VocabularyListSummaryResponse {
  const isOwner = Boolean((raw as any).isOwner ?? raw.owner);
  return {
    ...raw,
    isOwner,
    owner: isOwner,
  };
}

export async function fetchCustomVocabularyLists(): Promise<{
  myLists: VocabularyListSummaryResponse[];
  sharedWithMe: VocabularyListSummaryResponse[];
}> {
  const res = await axiosInstance.get<
    ApiResponse<{
      myLists: VocabularyListSummaryResponse[];
      sharedWithMe: VocabularyListSummaryResponse[];
    }>
  >("/vocabulary/lists");

  return {
    myLists: (res.data?.result?.myLists ?? []).map(normalizeSummary),
    sharedWithMe: (res.data?.result?.sharedWithMe ?? []).map(normalizeSummary),
  };
}

export async function createCustomVocabularyList(payload: {
  title: string;
  description?: string;
  targetCefr?: string;
}): Promise<VocabularyListSummaryResponse> {
  const res = await axiosInstance.post<
    ApiResponse<VocabularyListSummaryResponse>
  >("/vocabulary/lists", payload);
  return normalizeSummary(res.data.result);
}

export async function fetchCustomVocabularyListDetail(
  listId: string,
  shareToken?: string
): Promise<VocabularyListDetailResponse> {
  const res = await axiosInstance.get<
    ApiResponse<VocabularyListDetailResponse>
  >(`/vocabulary/lists/${listId}`, {
    params: shareToken ? { shareToken } : undefined,
  });
  const raw = res.data.result as any;
  const isOwner = Boolean(raw.isOwner ?? raw.owner);
  const items = (raw.items ?? []).map((it: VocabularyListItemResponse) => ({
    ...it,
    word: it.word || it.normalizedWord || it.rawWord || it.rawInputWord || "",
    rawInputWord: it.rawInputWord || it.rawWord || it.normalizedWord || it.word || "",
    rawWord: it.rawWord || it.rawInputWord || it.word || "",
    normalizedWord: it.normalizedWord || it.word?.toLowerCase() || "",
  }));
  const rawShares: any[] = raw.shares ?? raw.sharedUsers ?? [];
  const shares: ShareEntryDto[] = rawShares.map((s) => ({
    id: s.id ?? s.userId ?? s.email ?? "",
    userId: s.userId,
    sharedWithEmail: s.sharedWithEmail ?? s.email ?? s.username ?? "",
    sharedWithName: s.sharedWithName ?? s.fullName ?? s.username ?? "",
    permission: s.permission ?? s.role ?? "VIEWER",
    createdAt: s.createdAt,
  }));
  return {
    ...raw,
    isOwner,
    owner: isOwner,
    shares,
    items,
  };
}

export async function searchWordInDatabase(
  query: string
): Promise<VocabularyListItemResponse[]> {
  if (!query.trim()) return [];
  const res = await axiosInstance.get<
    ApiResponse<VocabularyListItemResponse[]>
  >("/vocabulary/lists/search-words", {
    params: { q: query.trim() },
  });
  const items = res.data?.result ?? [];
  return items.map((it) => ({
    ...it,
    vocabularyId: it.vocabularyId ?? it.id,
    word: it.word || it.normalizedWord || it.rawWord || it.rawInputWord || "",
    rawInputWord: it.rawInputWord || it.rawWord || it.normalizedWord || it.word || "",
    rawWord: it.rawWord || it.rawInputWord || it.word || "",
    normalizedWord: it.normalizedWord || it.word?.toLowerCase() || "",
  }));
}

export async function addWordToCustomList(
  listId: string,
  payload: {
    vocabularyId?: string;
    rawWord?: string;
    crawlIfMissing?: boolean;
    selectedSenseIds?: string[];
    selectedIdiomIds?: string[];
    selectedPhrasalVerbIds?: string[];
  }
): Promise<VocabularyListItemResponse> {
  const body = {
    words: payload.rawWord ? [payload.rawWord] : [],
    rawWord: payload.rawWord,
    vocabularyId: payload.vocabularyId,
    sourceOption: payload.crawlIfMissing ? "EXTERNAL_CRAWL" : "DB_EXISTING",
  };
  const res = await axiosInstance.post<
    ApiResponse<VocabularyListItemResponse | VocabularyListItemResponse[]>
  >(`/vocabulary/lists/${listId}/items`, body);
  const raw = res.data.result;
  const item = Array.isArray(raw) ? raw[0] : raw;
  return {
    ...item,
    word: item.word || item.normalizedWord || item.rawWord || item.rawInputWord || "",
    rawInputWord: item.rawInputWord || item.rawWord || item.normalizedWord || item.word || "",
    rawWord: item.rawWord || item.rawInputWord || item.word || "",
    normalizedWord: item.normalizedWord || item.word?.toLowerCase() || "",
  };
}

export async function updateCustomListItemSelection(
  listId: string,
  itemId: string,
  payload: {
    selectedSenseIds: string[];
    selectedIdiomIds: string[];
    selectedPhrasalVerbIds: string[];
  }
): Promise<VocabularyListItemResponse> {
  const res = await axiosInstance.patch<
    ApiResponse<VocabularyListItemResponse>
  >(`/vocabulary/lists/${listId}/items/${itemId}`, payload);
  const item = res.data.result;
  return {
    ...item,
    word: item.word || item.normalizedWord || item.rawWord || item.rawInputWord || "",
    rawInputWord: item.rawInputWord || item.rawWord || item.normalizedWord || item.word || "",
    rawWord: item.rawWord || item.rawInputWord || item.word || "",
    normalizedWord: item.normalizedWord || item.word?.toLowerCase() || "",
  };
}

export async function removeWordFromCustomList(
  listId: string,
  itemId: string
): Promise<void> {
  await axiosInstance.delete(`/vocabulary/lists/${listId}/items/${itemId}`);
}

export async function finalizeCustomListAndGenerateQuestions(
  listId: string
): Promise<FinalizeListResponse> {
  const res = await axiosInstance.post<ApiResponse<FinalizeListResponse>>(
    `/vocabulary/lists/${listId}/finalize`
  );
  return res.data.result;
}

export async function generateMoreQuestionsForSense(
  listId: string,
  senseId: string,
  count: number = 3
): Promise<number> {
  const res = await axiosInstance.post<ApiResponse<number>>(
    `/vocabulary/lists/${listId}/generate-more`,
    { senseId, count }
  );
  return res.data?.result ?? 0;
}

export async function fetchCustomListPracticeQuestions(
  listId: string,
  shareToken?: string
): Promise<CustomListMcqResponse[]> {
  const res = await axiosInstance.get<ApiResponse<CustomListMcqResponse[]>>(
    `/vocabulary/lists/${listId}/practice/multiple-choice`,
    {
      params: shareToken ? { shareToken } : undefined,
    }
  );
  return res.data?.result ?? [];
}

export async function shareCustomVocabularyList(
  listId: string,
  payload: {
    recipientEmailOrUsername?: string;
    emailOrUsername?: string;
    enableShareLink?: boolean;
  }
): Promise<ShareListResponse> {
  const body = {
    emailOrUsername: payload.emailOrUsername || payload.recipientEmailOrUsername,
    recipientEmailOrUsername: payload.recipientEmailOrUsername || payload.emailOrUsername,
    enableShareLink: payload.enableShareLink,
  };
  const res = await axiosInstance.post<ApiResponse<ShareListResponse>>(
    `/vocabulary/lists/${listId}/share`,
    body
  );
  return res.data.result;
}

export async function submitCustomListAnswer(
  listId: string,
  payload: {
    listItemId?: string;
    questionId?: string;
    isCorrect: boolean;
  }
): Promise<void> {
  await axiosInstance.post(`/vocabulary/lists/${listId}/practice/answer`, payload);
}

export async function cloneCustomVocabularyList(
  listId: string,
  shareToken?: string
): Promise<VocabularyListDetailResponse> {
  const res = await axiosInstance.post<
    ApiResponse<VocabularyListDetailResponse>
  >(`/vocabulary/lists/${listId}/clone`, null, {
    params: shareToken ? { shareToken } : undefined,
  });
  return res.data.result;
}

export async function fetchUserNotifications(): Promise<{
  notifications: UserNotificationDto[];
  unreadCount: number;
}> {
  try {
    const listRes = await axiosInstance.get<ApiResponse<any>>("/notifications");
    const result = listRes.data?.result;
    if (result && typeof result === "object" && !Array.isArray(result) && "items" in result) {
      return {
        notifications: result.items ?? [],
        unreadCount: typeof result.unreadCount === "number" ? result.unreadCount : 0,
      };
    }
    let unreadCount = 0;
    try {
      const countRes = await axiosInstance.get<ApiResponse<number>>("/notifications/unread-count");
      unreadCount = typeof countRes.data?.result === "number" ? countRes.data.result : 0;
    } catch {
      // ignore
    }
    return {
      notifications: Array.isArray(result) ? result : [],
      unreadCount,
    };
  } catch {
    return {
      notifications: [],
      unreadCount: 0,
    };
  }
}

export async function markNotificationRead(id: string): Promise<void> {
  await axiosInstance.post(`/notifications/${id}/read`);
}

export async function markAllNotificationsRead(): Promise<void> {
  await axiosInstance.post("/notifications/read-all");
}
