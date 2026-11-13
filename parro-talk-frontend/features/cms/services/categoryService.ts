import api from '@/lib/axios';
import { PageResponse, ApiResponse } from '@/types/api';
import {
    AdminCategoryDto,
    AdminCategoryCreateRequest,
    AdminCategoryUpdateRequest,
    AdminCategoryReorderRequest,
    CmsItemStatus,
} from '../types';

export function extractCategoryErrorMessage(
    err: unknown,
    fallback = 'Operation failed. Please try again.'
): string {
    if (err && typeof err === 'object' && 'response' in err) {
        const response = (err as { response?: { data?: { message?: string } } }).response;
        if (response?.data?.message) {
            return response.data.message;
        }
    }
    if (err instanceof Error && err.message) {
        return err.message;
    }
    return fallback;
}

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

    async updateCategoryStatus(id: string, status: CmsItemStatus): Promise<AdminCategoryDto> {
        const response = await api.patch<ApiResponse<AdminCategoryDto>>(`/admin/lesson-categories/${id}/status`, { status });
        return response.data.result;
    },

    async reorderCategories(data: AdminCategoryReorderRequest): Promise<void> {
        await api.put('/admin/lesson-categories/reorder', data);
    },

    async deleteCategory(id: string): Promise<void> {
        await api.delete(`/admin/lesson-categories/${id}`);
    }
};
