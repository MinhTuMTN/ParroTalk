import axiosInstance from "@/lib/axios";
import type { Lesson as AdminLesson, LessonStatus, Segment } from "@/features/lesson/types/lesson";

export interface Category {
  id: string;
  name: string;
}

export interface Lesson {
  id: string;
  status: "PENDING" | "PROCESSING" | "DONE" | "FAILED";
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
  difficulty?: "SHORT" | "MEDIUM" | "LONG";
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


type LessonVisibilityStatus = "published" | "hidden";

type BackendSegment = {
  id: string;
  text: string;
  start: number;
  end: number;
};

type BackendCategory = {
  id: string;
  name: string;
};

type BackendLesson = {
  id: string;
  title: string;
  fileUrl?: string | null;
  source?: string | null;
  duration?: number | null;
  visibilityStatus: LessonVisibilityStatus;
  categories?: BackendCategory[];
  segments?: BackendSegment[];
  createdAt: string;
};

type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type LessonListParams = {
  search?: string;
  status?: LessonStatus | "all";
  page?: number;
  limit?: number;
};

export type LessonListResult = {
  lessons: AdminLesson[];
  page: number;
  limit: number;
  totalItems: number;
  totalPages: number;
};

const toLessonStatus = (status: LessonVisibilityStatus): LessonStatus => status;

const toBackendStatus = (status: LessonStatus): LessonVisibilityStatus => status;

const mapSegment = (segment: BackendSegment): Segment => ({
  id: segment.id,
  text: segment.text,
  startTime: segment.start,
  endTime: segment.end,
});

const mapLesson = (lesson: BackendLesson): AdminLesson => ({
  id: lesson.id,
  title: lesson.title,
  source: lesson.source ?? lesson.fileUrl ?? "",
  duration: lesson.duration ?? 0,
  status: toLessonStatus(lesson.visibilityStatus),
  categories: lesson.categories ?? [],
  createdAt: lesson.createdAt,
  segments: (lesson.segments ?? []).map(mapSegment),
});

const mapSegmentsPayload = (segments: Segment[]) => ({
  segments: segments.map((segment, index) => ({
    id: segment.id.startsWith("seg-") ? null : segment.id,
    text: segment.text,
    startTime: segment.startTime,
    endTime: segment.endTime,
    order: index,
  })),
});


export const lessonService = {
  async getAllLessons(page = 0, size = 10, query?: string, categoryId?: string) {
    let url = `/lessons?page=${page}&size=${size}`;
    if (query) url += `&query=${encodeURIComponent(query)}`;
    if (categoryId) url += `&categoryId=${categoryId}`;
    const response = await axiosInstance.get<PageResponse<Lesson>>(url);

    return response.data;
  },

  async getLessonById(lessonId: string) {
    const response = await axiosInstance.get<Lesson>(`/lessons/${lessonId}`);
    return response.data;
  },

  async getCategories(query?: string, page = 0, size = 10) {
    let url = `/categories?page=${page}&size=${size}`;
    if (query) url += `&query=${encodeURIComponent(query)}`;
    const response = await axiosInstance.get<PageResponse<Category>>(url);
    return response.data;
  },

  async createCategory(name: string) {
    const response = await axiosInstance.post<Category>("/categories", { name });
    return response.data;
  },


  async getUserProfile() {
    const response = await axiosInstance.get<UserProfileResponse>("/users/profile");
    return response.data;
  },

  async getLessonProgress(lessonId: string) {
    const response = await axiosInstance.get<LessonProgressResponse>(`/lessons/${lessonId}/progress`);
    return response.data;
  },

  async submitAnswer(lessonId: string, segmentId: string | number, userAnswer: string) {
    const response = await axiosInstance.post<DraftSegmentResponse>(`/lessons/${lessonId}/answer`, {
      segmentId,
      userAnswer,
    });
    return response.data;
  },

  async incrementReplay(lessonId: string, segmentId: string | number) {
    await axiosInstance.post(`/lessons/${lessonId}/segments/${segmentId}/replay`);
  },

  async incrementHint(lessonId: string, segmentId: string | number) {
    await axiosInstance.post(`/lessons/${lessonId}/segments/${segmentId}/hint`);
  },

  async finalLessonSubmit(lessonId: string) {
    const response = await axiosInstance.post<SubmitLessonResponse>(`/lessons/${lessonId}/submit-lesson`);
    return response.data;
  },

  async resetProgress(lessonId: string) {
    await axiosInstance.get(`/lessons/${lessonId}/progress/reset`);
  },

  async uploadAudio(file: File, title: string, onProgress?: (progress: number) => void) {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("title", title);
    const response = await axiosInstance.post<{ lessonId: string; message: string }>("/audio/audio", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress(progress);
        }
      },
    });
    return response.data;
  },

  async processYoutube(url: string, title: string) {
    const response = await axiosInstance.post<{ lessonId: string; message: string }>("/audio/youtube", { url, title });
    return response.data;
  },

  async getLessons(params: LessonListParams = {}): Promise<LessonListResult> {
    const response = await axiosInstance.get<PageResponse<BackendLesson>>("/admin/lessons", {
      params: {
        search: params.search?.trim() || undefined,
        status: params.status && params.status !== "all" ? toBackendStatus(params.status) : undefined,
        page: params.page ?? 0,
        limit: params.limit ?? 10,
      },
    });

    return {
      lessons: response.data.content.map(mapLesson),
      page: response.data.page,
      limit: response.data.size,
      totalItems: response.data.totalElements,
      totalPages: response.data.totalPages,
    };
  },

  async getAdminLessonById(id: string): Promise<AdminLesson> {
    const response = await axiosInstance.get<BackendLesson>(`/admin/lessons/${id}`);
    return mapLesson(response.data);
  },

  async updateLesson(
    id: string,
    data: Pick<AdminLesson, "title" | "source" | "status"> & { categoryIds?: string[] },
  ): Promise<AdminLesson> {
    const response = await axiosInstance.put<BackendLesson>(`/admin/lessons/${id}`, {
      title: data.title,
      source: data.source,
      status: toBackendStatus(data.status),
      categoryIds: data.categoryIds,
    });

    return mapLesson(response.data);
  },

  async deleteLesson(id: string): Promise<void> {
    await axiosInstance.delete(`/admin/lessons/${id}`);
  },

  async updateLessonStatus(id: string, status: LessonStatus): Promise<AdminLesson> {
    const response = await axiosInstance.patch<BackendLesson>(`/admin/lessons/${id}/status`, {
      status: toBackendStatus(status),
    });

    return mapLesson(response.data);
  },

  async updateSegments(id: string, segments: Segment[], deletedSegmentIds: string[] = []): Promise<Segment[]> {
    const response = await axiosInstance.put<BackendLesson>(`/admin/lessons/${id}/segments`, {
      ...mapSegmentsPayload(segments),
      deletedSegmentIds,
    });

    return (response.data.segments ?? []).map(mapSegment);
  },

};

