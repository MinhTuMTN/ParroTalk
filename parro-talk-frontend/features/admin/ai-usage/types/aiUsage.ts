export interface AiUsageFilters {
  from: string; // yyyy-MM-dd (inclusive, interpreted in tz)
  to: string; // yyyy-MM-dd (inclusive day)
  model?: string;
  provider?: string;
  feature?: string;
  userId?: string;
  system?: boolean; // sent as userId=system
  tz?: string;
}

export interface AiUsageSummary {
  from: string;
  to: string;
  totalRequests: number;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  activeUsers: number;
  avgTokensPerRequest: number;
}

export interface AiUsageDailyPoint {
  date: string;
  requests: number;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
}

export interface AiUsageByModel {
  provider: string;
  model: string;
  requests: number;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  sharePercent: number;
}

export interface AiUsageByFeature {
  feature: string;
  requests: number;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  sharePercent: number;
}

export interface AiUsageByUser {
  userId: string | null;
  email: string | null;
  displayName: string | null;
  deleted?: boolean;
  requests: number;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  lastUsedAt: string | null;
}

export interface PagedResult<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
