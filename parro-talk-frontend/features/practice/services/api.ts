import api from '@/lib/axios';
import {
  PracticeSession,
  AnswerSubmission,
  AnswerResult,
  PracticeResult,
  PracticeStatistics
} from '../types';

export const practiceApi = {
  startSession: async (): Promise<PracticeSession> => {
    const { data } = await api.post<PracticeSession>('/api/v1/practice/session');
    return data;
  },

  submitAnswer: async (payload: AnswerSubmission): Promise<AnswerResult> => {
    const { data } = await api.post<AnswerResult>('/api/v1/practice/answer', payload);
    return data;
  },

  getResult: async (sessionId: string): Promise<PracticeResult> => {
    const { data } = await api.get<PracticeResult>(`/api/v1/practice/result/${sessionId}`);
    return data;
  },

  getStatistics: async (): Promise<PracticeStatistics> => {
    const { data } = await api.get<PracticeStatistics>('/api/v1/practice/statistics');
    return data;
  }
};
