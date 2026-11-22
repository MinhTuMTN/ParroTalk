import axiosInstance from "@/lib/axios";
import type { ApiResponse } from "@/types/api";
import type {
  LessonQuizAttemptResponse,
  LessonQuizAttemptSummary,
  LessonQuizOverviewResponse,
  QuizAnswerResult,
  RegenerateQuizRequest,
  StartQuizAttemptRequest,
  SubmitQuizAnswerRequest,
} from "../types/quiz";

export const lessonQuizService = {
  async getOverview(lessonId: string): Promise<LessonQuizOverviewResponse> {
    const res = await axiosInstance.get<ApiResponse<LessonQuizOverviewResponse>>(
      `/lessons/${encodeURIComponent(lessonId)}/quiz/overview`,
    );
    return res.data.result;
  },

  async startAttempt(
    lessonId: string,
    payload: StartQuizAttemptRequest,
  ): Promise<LessonQuizAttemptResponse> {
    const res = await axiosInstance.post<ApiResponse<LessonQuizAttemptResponse>>(
      `/lessons/${encodeURIComponent(lessonId)}/quiz/attempts`,
      payload,
    );
    return res.data.result;
  },

  async regenerateQuiz(
    lessonId: string,
    payload: RegenerateQuizRequest,
  ): Promise<LessonQuizAttemptResponse> {
    const res = await axiosInstance.post<ApiResponse<LessonQuizAttemptResponse>>(
      `/lessons/${encodeURIComponent(lessonId)}/quiz/regenerate`,
      payload,
    );
    return res.data.result;
  },

  async getAttempt(
    lessonId: string,
    attemptId: string,
  ): Promise<LessonQuizAttemptResponse> {
    const res = await axiosInstance.get<ApiResponse<LessonQuizAttemptResponse>>(
      `/lessons/${encodeURIComponent(lessonId)}/quiz/attempts/${encodeURIComponent(attemptId)}`,
    );
    return res.data.result;
  },

  async submitAnswer(
    lessonId: string,
    attemptId: string,
    payload: SubmitQuizAnswerRequest,
  ): Promise<QuizAnswerResult> {
    const res = await axiosInstance.post<ApiResponse<QuizAnswerResult>>(
      `/lessons/${encodeURIComponent(lessonId)}/quiz/attempts/${encodeURIComponent(attemptId)}/answer`,
      payload,
    );
    return res.data.result;
  },

  async completeAttempt(
    lessonId: string,
    attemptId: string,
  ): Promise<LessonQuizAttemptSummary> {
    const res = await axiosInstance.post<ApiResponse<LessonQuizAttemptSummary>>(
      `/lessons/${encodeURIComponent(lessonId)}/quiz/attempts/${encodeURIComponent(attemptId)}/complete`,
    );
    return res.data.result;
  },
};
