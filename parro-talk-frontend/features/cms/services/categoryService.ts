import api from '@/lib/axios';
import { PageResponse, ApiResponse } from '@/types/api';
import { AdminCategoryDto, AdminCategoryCreateRequest, AdminCategoryUpdateRequest } from '../types';

export const categoryService = {
    async getCategories(page = 0, size = 10, search?: string): Promise<PageResponse<AdminCategoryDto>> {
        const params = new URLSearchParams({ page: page.toString(), size: size.toString() });
        if (search) params.append('search', search);
        
        const response = await api.get<ApiResponse<PageResponse<AdminCategoryDto>>>(`/admin/lesson-categories?${params.toString()}`);
        return response.data.result;
    },

    async getCategoryTree(): Promise<AdminCategoryDto[]> {
        const response = await api.get<ApiResponse<AdminCategoryDto[]>>('/admin/lesson-categories/tree');
        return response.data.result;
    },

    async getCategory(id: string): Promise<AdminCategoryDto> {
        const response = await api.get<ApiResponse<AdminCategoryDto>>(`/admin/lesson-categories/${id}`);
        return response.data.result;
    },

    async createCategory(data: AdminCategoryCreateRequest): Promise<AdminCategoryDto> {
        const response = await api.post<ApiResponse<AdminCategoryDto>>('/admin/lesson-categories', data);
        return response.data.result;
    },

    async updateCategory(id: string, data: AdminCategoryUpdateRequest): Promise<AdminCategoryDto> {
        const response = await api.put<ApiResponse<AdminCategoryDto>>(`/admin/lesson-categories/${id}`, data);
        return response.data.result;
    },

    async deleteCategory(id: string): Promise<void> {
        await api.delete(`/admin/lesson-categories/${id}`);
    }
};
