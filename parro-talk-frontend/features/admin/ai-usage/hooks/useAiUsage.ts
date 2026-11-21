"use client";

import { useEffect, useMemo, useState } from "react";
import { useDebounce } from "use-debounce";
import { aiUsageService } from "../services/aiUsageService";
import type {
  AiUsageByFeature,
  AiUsageByModel,
  AiUsageByUser,
  AiUsageDailyPoint,
  AiUsageFilters,
  AiUsageSummary,
  PagedResult,
} from "../types/aiUsage";

export const MAX_RANGE_DAYS = 30;
export const USERS_PAGE_SIZE = 10;
export const DEFAULT_TZ = "Asia/Ho_Chi_Minh";
export type Preset = 7 | 14 | 30 | "custom";

export function toIsoDate(d: Date): string {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: DEFAULT_TZ,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(d);
}

function rangeForPreset(days: number) {
  const now = new Date();
  const from = new Date(now.getTime() - (days - 1) * 86_400_000);
  return { from: toIsoDate(from), to: toIsoDate(now) };
}

export function daysBetween(from: string, to: string): number {
  const a = new Date(`${from}T00:00:00`).getTime();
  const b = new Date(`${to}T00:00:00`).getTime();
  return Math.round((b - a) / 86_400_000) + 1;
}

interface Async<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
  retry: () => void;
}

/** Fetch keyed by `key`; loading is derived so no setState runs synchronously in effects. */
function useAsync<T>(
  key: string,
  fetcher: (signal: AbortSignal) => Promise<T>
): Async<T> {
  const [nonce, setNonce] = useState(0);
  const fullKey = `${key}#${nonce}`;
  const [state, setState] = useState<{
    key: string;
    data: T | null;
    error: string | null;
  }>({ key: "", data: null, error: null });

  useEffect(() => {
    const ctrl = new AbortController();
    fetcher(ctrl.signal)
      .then((data) => setState({ key: fullKey, data, error: null }))
      .catch((err: unknown) => {
        if (ctrl.signal.aborted) return;
        const msg = (err as { response?: { data?: { message?: string } } })
          ?.response?.data?.message;
        setState({
          key: fullKey,
          data: null,
          error: msg || "Không thể tải dữ liệu AI usage.",
        });
      });
    return () => ctrl.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fullKey]);

  const settled = state.key === fullKey;
  return {
    data: settled ? state.data : null,
    loading: !settled,
    error: settled ? state.error : null,
    retry: () => setNonce((n) => n + 1),
  };
}

export function useAiUsage() {
  const [preset, setPreset] = useState<Preset>(7);
  const [range, setRange] = useState(() => rangeForPreset(7));
  const [model, setModel] = useState("");
  const [userId, setUserId] = useState("");
  const [userLabel, setUserLabel] = useState("");
  const [system, setSystem] = useState(false);
  const [userSearchInput, setUserSearchInput] = useState("");
  const [userSearch] = useDebounce(userSearchInput.trim(), 350);
  const [userPage, setUserPage] = useState(0);

  const rangeDays = daysBetween(range.from, range.to);
  const rangeError =
    rangeDays < 1
      ? "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc."
      : rangeDays > MAX_RANGE_DAYS
        ? `Khoảng thời gian tối đa ${MAX_RANGE_DAYS} ngày.`
        : null;

  const filters: AiUsageFilters = useMemo(
    () => ({
      from: range.from,
      to: range.to,
      model: model || undefined,
      userId: userId || undefined,
      system,
      tz: DEFAULT_TZ,
    }),
    [range, model, userId, system]
  );
  // For the model dropdown options we ignore the model filter itself.
  const optionFilters: AiUsageFilters = useMemo(
    () => ({ ...filters, model: undefined }),
    [filters]
  );

  const baseKey = JSON.stringify(filters);
  const skip = rangeError !== null;

  const guard = <T,>(fn: (s: AbortSignal) => Promise<T>) =>
    skip ? () => Promise.resolve(null as T) : fn;

  const summary = useAsync<AiUsageSummary | null>(
    `s${baseKey}${skip}`,
    guard((s) => aiUsageService.summary(filters, s))
  );
  const timeseries = useAsync<AiUsageDailyPoint[] | null>(
    `t${baseKey}${skip}`,
    guard((s) => aiUsageService.timeseries(filters, s))
  );
  const models = useAsync<AiUsageByModel[] | null>(
    `m${baseKey}${skip}`,
    guard((s) => aiUsageService.models(filters, s))
  );
  const modelOptions = useAsync<AiUsageByModel[] | null>(
    `o${JSON.stringify(optionFilters)}${skip}`,
    guard((s) => aiUsageService.models(optionFilters, s))
  );
  const features = useAsync<AiUsageByFeature[] | null>(
    `f${baseKey}${skip}`,
    guard((s) => aiUsageService.features(filters, s))
  );
  const users = useAsync<PagedResult<AiUsageByUser> | null>(
    `u${baseKey}|${userSearch}|${userPage}${skip}`,
    guard((s) =>
      aiUsageService.users(filters, userPage, USERS_PAGE_SIZE, userSearch, s)
    )
  );

  const selectPreset = (p: Preset) => {
    setPreset(p);
    setUserPage(0);
    if (p !== "custom") setRange(rangeForPreset(p));
  };
  const setCustomRange = (next: { from: string; to: string }) => {
    setPreset("custom");
    setRange(next);
    setUserPage(0);
  };
  const changeModel = (m: string) => {
    setModel(m);
    setUserPage(0);
  };
  const selectUser = (id: string, label: string) => {
    setSystem(false);
    setUserId(id);
    setUserLabel(label);
    setUserPage(0);
  };
  const selectSystem = (on: boolean) => {
    setSystem(on);
    setUserId("");
    setUserLabel("");
    setUserPage(0);
  };
  const clearUser = () => selectSystem(false);
  const changeUserSearch = (v: string) => {
    setUserSearchInput(v);
    setUserPage(0);
  };

  return {
    preset,
    range,
    rangeError,
    model,
    userId,
    userLabel,
    system,
    userSearchInput,
    userPage,
    setUserPage,
    selectPreset,
    setCustomRange,
    changeModel,
    selectUser,
    selectSystem,
    clearUser,
    changeUserSearch,
    summary,
    timeseries,
    models,
    modelOptions,
    features,
    users,
  };
}
