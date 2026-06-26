import axiosInstance from "@/lib/axios";
import type { UserStreakResponse } from "@/features/streak/types/streak";

export const streakService = {
  async getMyStreak(year?: number) {
    const response = await axiosInstance.get<UserStreakResponse>("/users/me/streak", {
      params: year ? { year } : undefined,
    });
    return response.data;
  },
};
