import axiosInstance, { API_URL, BACKEND_URL } from "@/lib/axios";
import { getStoredUserSettings } from "@/features/settings/hooks/useUserSettings";
import type {
  PageResponse,
  WritingAnalyzeRequest,
  WritingSessionResponse,
  WritingSessionSummary,
} from "../types";

export type WritingAccent = "US" | "UK";

function getPreferredVoiceForAccent(
  accent: WritingAccent,
  explicitVoice?: string
): string {
  if (explicitVoice?.trim()) {
    return explicitVoice.trim();
  }
  const settings = getStoredUserSettings();
  return accent === "UK" ? settings.preferredUkVoice : settings.preferredUsVoice;
}

function appendVoiceIfWritingTts(
  url: string,
  accent: WritingAccent,
  explicitVoice?: string
): string {
  if (!url) {
    return url;
  }
  const [pathWithoutQuery] = url.split(/[?#]/, 1);
  if (!/\/writing\/tts$/i.test(pathWithoutQuery)) {
    return url;
  }
  if (/[?&]voice=/.test(url)) {
    return url;
  }
  const selectedVoice = getPreferredVoiceForAccent(accent, explicitVoice);
  if (!selectedVoice) {
    return url;
  }
  const separator = url.includes("?") ? "&" : "?";
  return `${url}${separator}voice=${encodeURIComponent(selectedVoice)}`;
}

/**
 * Builds the Edge-TTS streaming endpoint URL for a Writing Assistant text clip.
 */
export function getWritingTtsUrl(
  text: string,
  accent: WritingAccent = "US",
  voice?: string
): string {
  const cleaned = text.trim().replace(/\s+/g, " ");
  if (!cleaned) {
    return "";
  }
  const baseApiUrl = API_URL.replace(/\/+$/, "");
  const baseUrl = `${baseApiUrl}/writing/tts?text=${encodeURIComponent(cleaned)}&accent=${accent}`;
  return appendVoiceIfWritingTts(baseUrl, accent, voice);
}

/**
 * Resolves a backend-supplied audio URL (absolute, backend-relative, or Edge-TTS fallback).
 */
export function resolveWritingAudioUrl(
  audioUrl?: string,
  fallbackText?: string,
  accent: WritingAccent = "US",
  voice?: string
): string {
  const trimmed = audioUrl?.trim() ?? "";
  if (/^https?:\/\//i.test(trimmed) || trimmed.startsWith("//")) {
    return appendVoiceIfWritingTts(trimmed, accent, voice);
  }
  if (trimmed.startsWith("/")) {
    const baseBackendUrl = BACKEND_URL.replace(/\/+$/, "");
    const baseApiUrl = API_URL.replace(/\/+$/, "");

    if (
      baseBackendUrl.startsWith("/") &&
      (trimmed === baseBackendUrl || trimmed.startsWith(`${baseBackendUrl}/`))
    ) {
      return appendVoiceIfWritingTts(trimmed, accent, voice);
    }

    if (
      baseBackendUrl.startsWith("/") &&
      baseApiUrl.startsWith("/") &&
      /^\/api(?=\/|$)/.test(trimmed)
    ) {
      const proxyPath = trimmed.replace(/^\/api(?=\/|$)/, baseApiUrl);
      return appendVoiceIfWritingTts(proxyPath, accent, voice);
    }

    return appendVoiceIfWritingTts(`${baseBackendUrl}${trimmed}`, accent, voice);
  }
  return getWritingTtsUrl(fallbackText || "", accent, voice);
}

export const writingService = {
  async analyzeIdea(payload: WritingAnalyzeRequest): Promise<WritingSessionResponse> {
    const response = await axiosInstance.post("/writing/analyze", payload);
    return response.data.result;
  },

  async getSessions(page = 0, size = 10): Promise<PageResponse<WritingSessionSummary>> {
    const response = await axiosInstance.get("/writing/sessions", {
      params: { page, size },
    });
    return response.data.result;
  },

  async getSessionDetail(sessionId: string): Promise<WritingSessionResponse> {
    const response = await axiosInstance.get(`/writing/sessions/${sessionId}`);
    return response.data.result;
  },

  async updateDraft(sessionId: string, userDraft: string): Promise<WritingSessionResponse> {
    const response = await axiosInstance.patch(`/writing/sessions/${sessionId}/draft`, {
      userDraft,
    });
    return response.data.result;
  },

  async deleteSession(sessionId: string): Promise<void> {
    await axiosInstance.delete(`/writing/sessions/${sessionId}`);
  },
};

