import axiosInstance from "@/lib/axios";
import type {
  AdminResetPasswordResult,
  AdminUser,
  CreateAdminUserInput,
  UpdateAdminUserInput,
  UserListParams,
  UserListResult,
} from "@/features/users/types/user";

type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export const adminUserService = {
  async getUsers(params: UserListParams = {}): Promise<UserListResult> {
    const response = await axiosInstance.get<PageResponse<AdminUser>>("/admin/users", {
      params: {
        search: params.search?.trim() || undefined,
        role: params.role && params.role !== "all" ? params.role : undefined,
        status: params.status && params.status !== "all" ? params.status : undefined,
        page: params.page ?? 0,
        size: params.size ?? 10,
      },
    });

    return {
      users: response.data.content,
      page: response.data.page,
      size: response.data.size,
      totalItems: response.data.totalElements,
      totalPages: response.data.totalPages,
    };
  },

  async createUser(payload: CreateAdminUserInput): Promise<AdminUser> {
    const response = await axiosInstance.post<AdminUser>("/admin/users", payload);
    return response.data;
  },

  async getUser(id: string): Promise<AdminUser> {
    const response = await axiosInstance.get<AdminUser>(`/admin/users/${id}`);
    return response.data;
  },

  async updateUser(id: string, payload: UpdateAdminUserInput): Promise<AdminUser> {
    const response = await axiosInstance.put<AdminUser>(`/admin/users/${id}`, payload);
    return response.data;
  },

  async updateStatus(id: string, status: AdminUser["status"]): Promise<AdminUser> {
    const response = await axiosInstance.patch<AdminUser>(`/admin/users/${id}/status`, {
      status,
    });
    return response.data;
  },

  async resetPassword(id: string): Promise<AdminResetPasswordResult> {
    const response = await axiosInstance.post<AdminResetPasswordResult>(
      `/admin/users/${id}/reset-password`,
    );
    return response.data;
  },

  async deleteUser(id: string): Promise<void> {
    await axiosInstance.delete(`/admin/users/${id}`);
  },
};
