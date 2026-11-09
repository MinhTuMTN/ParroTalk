import axiosInstance from "@/lib/axios";
import type {
  CustomListMcqResponse,
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
    myLists: res.data?.result?.myLists ?? [],
    sharedWithMe: res.data?.result?.sharedWithMe ?? [],
  };
}

export async function createCustomVocabularyList(payload: {
  title: string;
  description?: string;
  targetCefr?: string;
}): Promise<VocabularyListDetailResponse> {
  const res = await axiosInstance.post<
    ApiResponse<VocabularyListDetailResponse>
  >("/vocabulary/lists", payload);
  return res.data.result;
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
  const raw = res.data.result;
  const isOwner = Boolean((raw as any).isOwner ?? raw.owner);
  const items = (raw.items ?? []).map((it) => ({
    ...it,
    word: it.word || it.normalizedWord || it.rawWord || it.rawInputWord || "",
    rawInputWord: it.rawInputWord || it.rawWord || it.normalizedWord || it.word || "",
    rawWord: it.rawWord || it.rawInputWord || it.word || "",
    normalizedWord: it.normalizedWord || it.word?.toLowerCase() || "",
  }));
  return {
    ...raw,
    isOwner,
    owner: isOwner,
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
  const res = await axiosInstance.post<ApiResponse<VocabularyListItemResponse>>(
    `/vocabulary/lists/${listId}/items`,
    payload
  );
  return res.data.result;
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
  return res.data.result;
}

export async function removeWordFromCustomList(
  listId: string,
  itemId: string
): Promise<void> {
  await axiosInstance.delete(`/vocabulary/lists/${listId}/items/${itemId}`);
}

export async function finalizeCustomListAndGenerateQuestions(
  listId: string
): Promise<VocabularyListDetailResponse> {
  const res = await axiosInstance.post<
    ApiResponse<VocabularyListDetailResponse>
  >(`/vocabulary/lists/${listId}/finalize`);
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
    enableShareLink?: boolean;
  }
): Promise<VocabularyListDetailResponse> {
  const res = await axiosInstance.post<
    ApiResponse<VocabularyListDetailResponse>
  >(`/vocabulary/lists/${listId}/share`, payload);
  return res.data.result;
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
  const [listRes, countRes] = await Promise.all([
    axiosInstance.get<ApiResponse<UserNotificationDto[]>>("/notifications"),
    axiosInstance.get<ApiResponse<number>>("/notifications/unread-count"),
  ]);
  return {
    notifications: listRes.data?.result ?? [],
    unreadCount: countRes.data?.result ?? 0,
  };
}

export async function markNotificationRead(id: string): Promise<void> {
  await axiosInstance.patch(`/notifications/${id}/read`);
}

export async function markAllNotificationsRead(): Promise<void> {
  await axiosInstance.patch("/notifications/read-all");
}
