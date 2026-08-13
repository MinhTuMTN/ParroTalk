import axiosInstance from '@/lib/axios';
import {
  PracticeQuestionDto,
  VocabularyDetailDto,
  VocabularyReportRequestDto,
  VocabularyTopicDto
} from '../types';

export const getTopics = async (): Promise<VocabularyTopicDto[]> => {
  const { data } = await axiosInstance.get<VocabularyTopicDto[]>('/vocabulary/topics');
  return data;
};

export const searchVocabulary = async (params: { keyword?: string; topic?: string; cefrLevel?: string; page?: number; size?: number }) => {
  const { data } = await axiosInstance.get('/vocabulary', { params });
  return data;
};

export const getVocabularyDetail = async (id: string): Promise<VocabularyDetailDto> => {
  const { data } = await axiosInstance.get<VocabularyDetailDto>(`/vocabulary/${id}`);
  return data;
};

export const reportVocabulary = async (id: string, payload: VocabularyReportRequestDto): Promise<void> => {
  await axiosInstance.post(`/vocabulary/${id}/report`, payload);
};

export const bookmarkVocabulary = async (id: string): Promise<void> => {
  await axiosInstance.post('/me/vocabulary', { vocabularyId: id });
};

export const getPracticeQuestions = async (params?: { topic?: string; cefrLevel?: string; count?: number }): Promise<PracticeQuestionDto[]> => {
  const { data } = await axiosInstance.get<PracticeQuestionDto[]>('/vocabulary/practice/questions', { params });
  return data;
};