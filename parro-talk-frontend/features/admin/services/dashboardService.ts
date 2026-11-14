import api from '@/lib/axios';
import { ApiResponse } from '@/types/api';

export interface RecentLessonActivity {
  id: string;
  title: string;
  status: string;
  createdAt: string;
  totalSegments: number;
}

export interface AdminDashboardStats {
  totalUsers: number;
  activeUsers: number;
  totalLessons: number;
  publishedLessons: number;
  hiddenLessons: number;
  totalCategories: number;
  totalTags: number;
  totalVocabularies: number;
  pendingLessonReports: number;
  pendingFeedbacks: number;
  recentLessons: RecentLessonActivity[];
}

export const dashboardService = {
  async getStats(): Promise<AdminDashboardStats> {
    const response = await api.get<ApiResponse<AdminDashboardStats>>('/admin/dashboard/stats');
    return response.data.result;
  },
};
