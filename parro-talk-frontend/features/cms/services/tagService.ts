import api from '@/lib/axios';
import { PageResponse, ApiResponse } from '@/types/api';
import { AdminTagDto, AdminTagCreateRequest, AdminTagUpdateRequest } from '../types';

export const tagService = {
    async getTags(page = 0, size = 10): Promise<PageResponse<AdminTagDto>> {
        const params = new URLSearchParams({ page: page.toString(), size: size.toString() });
        const response = await api.get<ApiResponse<PageResponse<AdminTagDto>>>(`/admin/lesson-tags?${params.toString()}`);
        return response.data.result;
    },

    async getTag(id: string): Promise<AdminTagDto> {
        const response = await api.get<ApiResponse<AdminTagDto>>(`/admin/lesson-tags/${id}`);
        return response.data.result;
    },

    async createTag(data: AdminTagCreateRequest): Promise<AdminTagDto> {
        const response = await api.post<ApiResponse<AdminTagDto>>('/admin/lesson-tags', data);
        return response.data.result;
    },

    async updateTag(id: string, data: AdminTagUpdateRequest): Promise<AdminTagDto> {
        const response = await api.put<ApiResponse<AdminTagDto>>(`/admin/lesson-tags/${id}`, data);
        return response.data.result;
    },

    async deleteTag(id: string): Promise<void> {
        await api.delete(`/admin/lesson-tags/${id}`);
    }
};
