import api from '@/lib/axios';
import { PageResponse } from '@/types/api';
import {
  LessonReportResponse,
  AppFeedbackResponse,
  ModerationUpdateRequest,
  ModerationEventResponse,
  LessonReportSearchParams,
  AppFeedbackSearchParams,
} from '../types/report';

export const reportService = {
  // Lesson Reports
  async getLessonReports(params: LessonReportSearchParams = {}): Promise<PageResponse<LessonReportResponse>> {
    const searchParams = new URLSearchParams();
    if (params.status) searchParams.append('status', params.status);
    if (params.type) searchParams.append('type', params.type);
    if (params.priority) searchParams.append('priority', params.priority);
    if (params.lessonId) searchParams.append('lessonId', params.lessonId);
    if (params.assigneeId) searchParams.append('assigneeId', params.assigneeId);
    searchParams.append('page', (params.page ?? 0).toString());
    searchParams.append('size', (params.size ?? 10).toString());

    const response = await api.get<PageResponse<LessonReportResponse>>(`/admin/lesson-reports?${searchParams.toString()}`);
    // Support both direct response and wrapped ApiResponse defensively
    const data = response.data as unknown as { result?: PageResponse<LessonReportResponse> } & PageResponse<LessonReportResponse>;
    return data.result || data;
  },

  async getLessonReportDetail(id: string): Promise<LessonReportResponse> {
    const response = await api.get<LessonReportResponse>(`/admin/lesson-reports/${id}`);
    const data = response.data as unknown as { result?: LessonReportResponse } & LessonReportResponse;
    return data.result || data;
  },

  async updateLessonReport(id: string, data: ModerationUpdateRequest): Promise<LessonReportResponse> {
    const response = await api.patch<LessonReportResponse>(`/admin/lesson-reports/${id}`, data);
    const resData = response.data as unknown as { result?: LessonReportResponse } & LessonReportResponse;
    return resData.result || resData;
  },

  async getLessonReportEvents(id: string): Promise<ModerationEventResponse[]> {
    const response = await api.get<ModerationEventResponse[]>(`/admin/lesson-reports/${id}/events`);
    const data = response.data as unknown as { result?: ModerationEventResponse[] } & ModerationEventResponse[];
    return data.result || data;
  },

  // App Feedback
  async getAppFeedbacks(params: AppFeedbackSearchParams = {}): Promise<PageResponse<AppFeedbackResponse>> {
    const searchParams = new URLSearchParams();
    if (params.status) searchParams.append('status', params.status);
    if (params.category) searchParams.append('category', params.category);
    if (params.priority) searchParams.append('priority', params.priority);
    if (params.assigneeId) searchParams.append('assigneeId', params.assigneeId);
    searchParams.append('page', (params.page ?? 0).toString());
    searchParams.append('size', (params.size ?? 10).toString());

    const response = await api.get<PageResponse<AppFeedbackResponse>>(`/admin/feedback?${searchParams.toString()}`);
    const data = response.data as unknown as { result?: PageResponse<AppFeedbackResponse> } & PageResponse<AppFeedbackResponse>;
    return data.result || data;
  },

  async getAppFeedbackDetail(id: string): Promise<AppFeedbackResponse> {
    const response = await api.get<AppFeedbackResponse>(`/admin/feedback/${id}`);
    const data = response.data as unknown as { result?: AppFeedbackResponse } & AppFeedbackResponse;
    return data.result || data;
  },

  async updateAppFeedback(id: string, data: ModerationUpdateRequest): Promise<AppFeedbackResponse> {
    const response = await api.patch<AppFeedbackResponse>(`/admin/feedback/${id}`, data);
    const resData = response.data as unknown as { result?: AppFeedbackResponse } & AppFeedbackResponse;
    return resData.result || resData;
  },

  async getAppFeedbackEvents(id: string): Promise<ModerationEventResponse[]> {
    const response = await api.get<ModerationEventResponse[]>(`/admin/feedback/${id}/events`);
    const data = response.data as unknown as { result?: ModerationEventResponse[] } & ModerationEventResponse[];
    return data.result || data;
  },
};
