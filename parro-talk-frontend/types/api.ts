export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number?: number;
  page?: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  result: T;
}
