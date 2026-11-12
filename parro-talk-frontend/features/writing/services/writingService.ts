import axiosInstance from "@/lib/axios";
import type {
  PageResponse,
  WritingAnalyzeRequest,
  WritingSessionResponse,
  WritingSessionSummary,
} from "../types";

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

