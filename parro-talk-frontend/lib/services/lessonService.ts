import axiosInstance from "../axios";

export interface Lesson {
  id: string;
  status: 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED';
  progress: number;
  currentStep: string;
  fileUrl: string;
  createdAt: string;
  mediaType?: string;
}

export interface Sentence {
  start: number;
  end: number;
  text: string;
}

export interface LessonResult {
  sentences: Sentence[];
  words: any[];
}

export const lessonService = {
  getAllLessons: async () => {
    const response = await axiosInstance.get<Lesson[]>("/lessons");
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
};
