export type CmsItemStatus = 'ACTIVE' | 'INACTIVE';

export interface AdminCategoryDto {
    id: string;
    name: string;
    slug: string;
    description: string | null;
    icon: string | null;
    color: string | null;
    imageUrl: string | null;
    parentCategoryId: string | null;
    sortOrder: number;
    status: CmsItemStatus;
    createdAt: string;
    updatedAt: string;
    createdBy: string | null;
    updatedBy: string | null;
    lessonsCount: number;
    children?: AdminCategoryDto[];
}

export interface AdminCategoryCreateRequest {
    name: string;
    slug?: string;
    description?: string;
    icon?: string;
    color?: string;
    imageUrl?: string;
    parentCategoryId?: string;
    sortOrder: number;
    status: CmsItemStatus;
}

export interface AdminCategoryUpdateRequest extends AdminCategoryCreateRequest {
    slug: string;
}

export interface AdminTagDto {
    id: string;
    name: string;
    slug: string;
    color: string | null;
    description: string | null;
    status: CmsItemStatus;
    createdAt: string;
    updatedAt: string;
    createdBy: string | null;
    updatedBy: string | null;
    lessonsCount: number;
}

export interface AdminTagCreateRequest {
    name: string;
    slug?: string;
    color?: string;
    description?: string;
    status: CmsItemStatus;
}

export interface AdminTagUpdateRequest extends AdminTagCreateRequest {
    slug: string;
}
