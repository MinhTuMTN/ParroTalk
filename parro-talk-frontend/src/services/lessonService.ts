import axiosInstance from "@/lib/axios";
import type { Lesson, LessonStatus, Segment } from "@/src/types/lesson";

type LessonVisibilityStatus = "published" | "hidden";

type BackendSegment = {
  id: string;
  text: string;
  start: number;
  end: number;
};

type BackendLesson = {
  id: string;
  title: string;
  fileUrl?: string | null;
  source?: string | null;
  duration?: number | null;
  visibilityStatus: LessonVisibilityStatus;
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
  lessons: Lesson[];
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

const mapLesson = (lesson: BackendLesson): Lesson => ({
  id: lesson.id,
  title: lesson.title,
  source: lesson.source ?? lesson.fileUrl ?? "",
  duration: lesson.duration ?? 0,
  status: toLessonStatus(lesson.visibilityStatus),
  createdAt: lesson.createdAt,
  segments: (lesson.segments ?? []).map(mapSegment),
});

const mapSegmentsPayload = (segments: Segment[]) => ({
  segments: segments.map((segment, index) => ({
    id: segment.id.startsWith("seg-") ? undefined : segment.id,
    text: segment.text,
    start: segment.startTime,
    end: segment.endTime,
    order: index,
  })),
});

export const lessonService = {
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

  async getLessonById(id: string): Promise<Lesson> {
    const response = await axiosInstance.get<BackendLesson>(`/admin/lessons/${id}`);
    return mapLesson(response.data);
  },

  async updateLesson(
    id: string,
    data: Pick<Lesson, "title" | "source" | "status">,
  ): Promise<Lesson> {
    const response = await axiosInstance.put<BackendLesson>(`/admin/lessons/${id}`, {
      title: data.title,
      source: data.source,
      status: toBackendStatus(data.status),
    });

    return mapLesson(response.data);
  },

  async deleteLesson(id: string): Promise<void> {
    await axiosInstance.delete(`/admin/lessons/${id}`);
  },

  async updateLessonStatus(id: string, status: LessonStatus): Promise<Lesson> {
    const response = await axiosInstance.patch<BackendLesson>(`/admin/lessons/${id}/status`, {
      status: toBackendStatus(status),
    });

    return mapLesson(response.data);
  },

  async updateSegments(id: string, segments: Segment[]): Promise<Segment[]> {
    const response = await axiosInstance.put<BackendLesson>(
      `/admin/lessons/${id}/segments`,
      mapSegmentsPayload(segments),
    );

    return (response.data.segments ?? []).map(mapSegment);
  },
};
