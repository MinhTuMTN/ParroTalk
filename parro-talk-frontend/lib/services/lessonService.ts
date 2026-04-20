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
  segments?: Sentence[];
}

export interface DraftSegmentResponse {
  segmentId: string;
  userAnswer: string;
  correct: boolean;
  score: number;
  replayCount: number;
  hintCount: number;
  updatedAt: string;
}

export interface LessonProgressResponse {
  userId: string;
  lessonId: string;
  currentSegmentId: string;
  lastPositionSeconds: number;
  lastProgress: number;
  updatedAt: string;
  draftSegments: DraftSegmentResponse[];
}

export interface Sentence {
  id?: string | number;
  start: number;
  end: number;
  text: string;
  difficulty?: 'SHORT' | 'MEDIUM' | 'LONG';
}


export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SegmentResultRequest {
  segmentId: string | number;
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
  getAllLessons: async (page = 0, size = 10, query?: string, categoryId?: string) => {
    let url = `/lessons?page=${page}&size=${size}`;
    if (query) url += `&query=${encodeURIComponent(query)}`;
    if (categoryId) url += `&categoryId=${categoryId}`;
    const response = await axiosInstance.get<PageResponse<Lesson>>(url);
    return response.data;
  },

  getLessonById: async (lessonId: string) => {
    const response = await axiosInstance.get<Lesson>(`/lessons/${lessonId}`);
    return response.data;
  },

  getCategories: async () => {
    const response = await axiosInstance.get<Category[]>("/categories");
    return response.data;
  },

  getUserProfile: async () => {
    const response = await axiosInstance.get<UserProfileResponse>("/users/profile");
    return response.data;
  },

  getLessonProgress: async (lessonId: string) => {
    const response = await axiosInstance.get<LessonProgressResponse>(`/lessons/${lessonId}/progress`);
    return response.data;
  },

  submitAnswer: async (lessonId: string, segmentId: string | number, userAnswer: string) => {
    const response = await axiosInstance.post<DraftSegmentResponse>(`/lessons/${lessonId}/answer`, {
      segmentId,
      userAnswer,
    });
    return response.data;
  },

  incrementReplay: async (lessonId: string, segmentId: string | number) => {
    await axiosInstance.post(`/lessons/${lessonId}/segments/${segmentId}/replay`);
  },

  incrementHint: async (lessonId: string, segmentId: string | number) => {
    await axiosInstance.post(`/lessons/${lessonId}/segments/${segmentId}/hint`);
  },

  finalLessonSubmit: async (lessonId: string) => {
    const response = await axiosInstance.post<SubmitLessonResponse>(`/lessons/${lessonId}/submit-lesson`);
    return response.data;
  },

  resetProgress: async (lessonId: string) => {
    await axiosInstance.get(`/lessons/${lessonId}/progress/reset`);
  },

  uploadAudio: async (file: File, title: string, onProgress?: (progress: number) => void) => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("title", title);
    const response = await axiosInstance.post<{ lessonId: string; message: string }>(
      "/audio/upload",
      formData,
      {
        headers: {
          "Content-Type": "multipart/form-data",
        },
        onUploadProgress: (progressEvent) => {
          if (onProgress && progressEvent.total) {
            const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            onProgress(progress);
          }
        },
      }
    );
    return response.data;
  },

  processYoutube: async (url: string, title: string) => {
    const response = await axiosInstance.post<{ lessonId: string; message: string }>(
      "/audio/youtube",
      { url, title }
    );
    return response.data;
  }
};
