import api from "@/lib/axios";
import type { ApiResponse } from "@/types/api";

export type SectionType =
  | "CATEGORY"
  | "PROGRAM"
  | "IN_PROGRESS"
  | "USER_PROCESSING"
  | "USER_UPLOADED";

export interface AdminLibrarySection {
  id: string;
  title: string;
  type: SectionType;
  categoryId?: string | null;
  categoryName?: string | null;
  categorySlug?: string | null;
  program?: string | null;
  orderIndex: number;
  isActive: boolean;
  maxItems: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminLibrarySectionRequest {
  title: string;
  type: SectionType;
  categoryId?: string | null;
  program?: string | null;
  orderIndex?: number;
  isActive?: boolean;
  maxItems?: number;
}

export interface ReorderSectionItem {
  id: string;
  orderIndex: number;
}

export const librarySectionAdminService = {
  async getLibrarySections(): Promise<AdminLibrarySection[]> {
    const response = await api.get<ApiResponse<AdminLibrarySection[]>>(
      "/admin/library/sections"
    );
    return response.data.result;
  },

  async createLibrarySection(
    data: AdminLibrarySectionRequest
  ): Promise<AdminLibrarySection> {
    const response = await api.post<ApiResponse<AdminLibrarySection>>(
      "/admin/library/sections",
      data
    );
    return response.data.result;
  },

  async updateLibrarySection(
    id: string,
    data: AdminLibrarySectionRequest
  ): Promise<AdminLibrarySection> {
    const response = await api.put<ApiResponse<AdminLibrarySection>>(
      `/admin/library/sections/${id}`,
      data
    );
    return response.data.result;
  },

  async deleteLibrarySection(id: string): Promise<void> {
    await api.delete(`/admin/library/sections/${id}`);
  },

  async reorderLibrarySections(
    items: ReorderSectionItem[]
  ): Promise<void> {
    await api.put("/admin/library/sections/reorder", { items });
  },
};

export default librarySectionAdminService;
