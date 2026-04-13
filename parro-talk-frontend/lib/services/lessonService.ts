import axiosInstance from "../axios";

export interface Category {
  id: string;
  name: string;
}

export interface Lesson {
  id: string;
  status: 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED';
  progress: number;
  currentStep: string;
  fileUrl: string;
  createdAt: string;
  mediaType?: string;
  title?: string;
  content?: string;
  duration?: number;
  categories?: Category[];
}

export interface Sentence {
  id?: number;
  start: number;
  end: number;
  text: string;
  difficulty?: 'SHORT' | 'MEDIUM' | 'LONG';
}

export interface LessonResult {
  sentences: Sentence[];
  words: any[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SegmentResultRequest {
  segmentId: number;
  hintWords: number;
  replayCount: number;
  attempts: number;
  userAnswer: string;
}

export interface SubmitLessonRequest {
  segmentResults: SegmentResultRequest[];
  isFinished: boolean;
}

export interface SubmitLessonResponse {
  score: number;
  passed: boolean;
}

export interface UserProfileResponse {
  id: string;
  fullName: string;
  email: string;
  totalLessonsCompleted: number;
  totalScore: number;
  avgScore: number;
  currentStreak: number;
  longestStreak: number;
  activeDays: string[];
}

export const lessonService = {
  getAllLessons: async (page = 0, size = 10, q?: string, categoryId?: string) => {
    let url = `/lessons?page=${page}&size=${size}`;
    if (q) url += `&q=${encodeURIComponent(q)}`;
    if (categoryId) url += `&categoryId=${categoryId}`;
    const response = await axiosInstance.get<PageResponse<Lesson>>(url);
    return response.data;
  },

  getLessonById: async (lessonId: string) => {
    const response = await axiosInstance.get<Lesson>(`/lessons/${lessonId}`);
    return response.data;
  },

  getLessonResult: async (lessonId: string) => {
    const response = await axiosInstance.get<LessonResult>(`/lessons/${lessonId}/result`);
    return response.data;
  },

  submitLesson: async (lessonId: string, request: SubmitLessonRequest) => {
    const response = await axiosInstance.post<SubmitLessonResponse>(`/lessons/${lessonId}/submit`, request);
    return response.data;
  },

  getCategories: async () => {
    const response = await axiosInstance.get<Category[]>("/categories");
    return response.data;
  },

  getUserProfile: async () => {
    const response = await axiosInstance.get<UserProfileResponse>("/users/profile");
    return response.data;
  }
};
