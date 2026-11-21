import api from "@/lib/axios";
import type { ApiResponse } from "@/types/api";
import type {
  AiUsageByFeature,
  AiUsageByModel,
  AiUsageByUser,
  AiUsageDailyPoint,
  AiUsageFilters,
  AiUsageSummary,
  PagedResult,
} from "../types/aiUsage";

const BASE = "/admin/ai-usage";

function toParams(f: AiUsageFilters, extra: Record<string, unknown> = {}) {
  const params: Record<string, unknown> = {
    from: f.from,
    to: f.to,
    tz: f.tz,
    model: f.model || undefined,
    provider: f.provider || undefined,
    feature: f.feature || undefined,
    ...extra,
  };
  if (f.system) params.userId = "system";
  else if (f.userId) params.userId = f.userId;
  return params;
}

async function get<T>(
  path: string,
  params: Record<string, unknown>,
  signal?: AbortSignal
): Promise<T> {
  const res = await api.get<ApiResponse<T>>(`${BASE}/${path}`, {
    params,
    signal,
  });
  return res.data.result;
}

export const aiUsageService = {
  summary: (f: AiUsageFilters, signal?: AbortSignal) =>
    get<AiUsageSummary>("summary", toParams(f), signal),
  timeseries: (f: AiUsageFilters, signal?: AbortSignal) =>
    get<AiUsageDailyPoint[]>("timeseries", toParams(f), signal),
  models: (f: AiUsageFilters, signal?: AbortSignal) =>
    get<AiUsageByModel[]>("models", toParams(f), signal),
  features: (f: AiUsageFilters, signal?: AbortSignal) =>
    get<AiUsageByFeature[]>("features", toParams(f), signal),
  users: (
    f: AiUsageFilters,
    page: number,
    size: number,
    search: string,
    signal?: AbortSignal
  ) =>
    get<PagedResult<AiUsageByUser>>(
      "users",
      toParams(f, { page, size, search: search || undefined }),
      signal
    ),
};
