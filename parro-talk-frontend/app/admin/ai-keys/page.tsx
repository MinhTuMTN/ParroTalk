"use client";

import { useCallback, useEffect, useState } from "react";
import {
  Activity,
  AlertCircle,
  Ban,
  CheckCircle2,
  Clock,
  KeyRound,
  Play,
  Power,
  RefreshCw,
  RotateCcw,
} from "lucide-react";
import axiosInstance from "@/lib/axios";

interface KeyStatusItem {
  maskedApiKey: string;
  models: string[];
  status: "ACTIVE" | "RATE_LIMITED" | "DEAD" | "DISABLED" | string;
  disabled: boolean;
  currentKey: boolean;
  quarantinedUntil: string | null;
  quarantineRemainingSeconds: number | null;
  lastError: string | null;
  lastErrorAt: string | null;
  lastCheckedAt: string | null;
  lastLatencyMs: number | null;
}

interface ProviderStatusDto {
  providerName: string;
  defaultProvider: boolean;
  totalNodes: number;
  healthyNodes: number;
  keys: KeyStatusItem[];
}

interface ApiResponse<T> {
  code: number;
  message?: string;
  result: T;
}

const PROVIDER_TITLES: Record<string, string> = {
  "gemini-vocab": "Google AI Studio — Vocab & MCQ Batch",
  gemini: "Google AI Studio — Writing Assessment",
  groq: "Groq — Fallback Pool",
};

function formatDuration(seconds: number | null): string {
  if (seconds == null || seconds <= 0) return "";
  if (seconds < 60) return `${seconds}s`;
  const mins = Math.floor(seconds / 60);
  if (mins < 60) return `${mins}m ${seconds % 60}s`;
  const hrs = Math.floor(mins / 60);
  return `${hrs}h ${mins % 60}m`;
}

function formatTime(iso: string | null): string {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleTimeString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  } catch {
    return iso;
  }
}

export default function AdminAiKeysPage() {
  const [providers, setProviders] = useState<ProviderStatusDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [busyAction, setBusyAction] = useState<string | null>(null);

  const loadStatuses = useCallback(async () => {
    try {
      setError(null);
      const res = await axiosInstance.get<ApiResponse<ProviderStatusDto[]>>(
        "/admin/ai-keys"
      );
      setProviders(res.data?.result ?? []);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })
        ?.response?.data?.message;
      setError(msg || "Không thể tải danh sách AI API keys.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadStatuses();
  }, [loadStatuses]);

  const updateProviderInState = (updated: ProviderStatusDto) => {
    setProviders((prev) =>
      prev.map((p) =>
        p.providerName === updated.providerName ? updated : p
      )
    );
  };

  const handleTest = async (providerName: string, maskedApiKey?: string) => {
    const actionKey = `test:${providerName}:${maskedApiKey ?? "ALL"}`;
    setBusyAction(actionKey);
    setError(null);
    try {
      const res = await axiosInstance.post<ApiResponse<ProviderStatusDto>>(
        `/admin/ai-keys/${encodeURIComponent(providerName)}/test`,
        maskedApiKey ? { maskedApiKey } : {}
      );
      if (res.data?.result) {
        updateProviderInState(res.data.result);
      }
      setStatusMessage(
        maskedApiKey
          ? `Đã kiểm tra key ${maskedApiKey} (${providerName}).`
          : `Đã kiểm tra toàn bộ key của provider ${providerName}.`
      );
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })
        ?.response?.data?.message;
      setError(msg || "Kiểm tra API key thất bại.");
    } finally {
      setBusyAction(null);
    }
  };

  const handleReset = async (providerName: string, maskedApiKey?: string) => {
    const actionKey = `reset:${providerName}:${maskedApiKey ?? "ALL"}`;
    setBusyAction(actionKey);
    setError(null);
    try {
      const res = await axiosInstance.post<ApiResponse<ProviderStatusDto>>(
        `/admin/ai-keys/${encodeURIComponent(providerName)}/reset`,
        maskedApiKey ? { maskedApiKey } : {}
      );
      if (res.data?.result) {
        updateProviderInState(res.data.result);
      }
      setStatusMessage(
        maskedApiKey
          ? `Đã gỡ quarantine cho key ${maskedApiKey}.`
          : `Đã reset toàn bộ quarantine cho ${providerName}.`
      );
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })
        ?.response?.data?.message;
      setError(msg || "Không thể reset trạng thái key.");
    } finally {
      setBusyAction(null);
    }
  };

  const handleToggle = async (
    providerName: string,
    maskedApiKey: string,
    disabled: boolean
  ) => {
    const actionKey = `toggle:${providerName}:${maskedApiKey}`;
    setBusyAction(actionKey);
    setError(null);
    try {
      const res = await axiosInstance.post<ApiResponse<ProviderStatusDto>>(
        `/admin/ai-keys/${encodeURIComponent(providerName)}/toggle`,
        { maskedApiKey, disabled }
      );
      if (res.data?.result) {
        updateProviderInState(res.data.result);
      }
      setStatusMessage(
        disabled
          ? `Đã tắt tạm key ${maskedApiKey} trong RAM.`
          : `Đã bật lại key ${maskedApiKey}.`
      );
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })
        ?.response?.data?.message;
      setError(msg || "Không thể thay đổi trạng thái key.");
    } finally {
      setBusyAction(null);
    }
  };

  const renderStatusBadge = (item: KeyStatusItem) => {
    switch (item.status) {
      case "ACTIVE":
        return (
          <span className="inline-flex items-center gap-1 rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700">
            <CheckCircle2 aria-hidden="true" className="h-3.5 w-3.5" />
            ACTIVE
          </span>
        );
      case "RATE_LIMITED":
        return (
          <span className="inline-flex items-center gap-1 rounded-full border border-amber-200 bg-amber-50 px-2.5 py-0.5 text-xs font-semibold text-amber-800">
            <Clock aria-hidden="true" className="h-3.5 w-3.5" />
            RATE LIMITED{" "}
            {item.quarantineRemainingSeconds != null
              ? `(${formatDuration(item.quarantineRemainingSeconds)})`
              : ""}
          </span>
        );
      case "DEAD":
        return (
          <span className="inline-flex items-center gap-1 rounded-full border border-rose-200 bg-rose-50 px-2.5 py-0.5 text-xs font-semibold text-rose-700">
            <AlertCircle aria-hidden="true" className="h-3.5 w-3.5" />
            DEAD / RESTRICTED
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-slate-100 px-2.5 py-0.5 text-xs font-semibold text-slate-700">
            <Ban aria-hidden="true" className="h-3.5 w-3.5" />
            DISABLED
          </span>
        );
    }
  };

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="flex items-center gap-2.5 text-2xl font-bold text-slate-900">
            <KeyRound aria-hidden="true" className="h-6 w-6 text-emerald-600" />
            Quản lý &amp; Kiểm tra AI API Keys
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Đọc trực tiếp từ environment variables trong{" "}
            <code className="rounded bg-slate-100 px-1.5 py-0.5 text-xs">
              docker-compose
            </code>{" "}
            (<code>GEMINI_API_KEYS</code>, <code>GROQ_API_KEYS</code>). Thao tác
            Test / Reset / Disable áp dụng trên bộ nhớ runtime của backend.
          </p>
        </div>

        <button
          type="button"
          onClick={loadStatuses}
          disabled={loading}
          className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 shadow-2xs transition hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 disabled:opacity-50"
        >
          <RefreshCw
            aria-hidden="true"
            className={`h-4 w-4 ${loading ? "animate-spin" : ""}`}
          />
          Làm mới
        </button>
      </div>

      {error && (
        <div
          role="alert"
          className="flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-800"
        >
          <AlertCircle aria-hidden="true" className="h-4 w-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {statusMessage && (
        <div
          role="status"
          aria-live="polite"
          className="flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-800"
        >
          <Activity aria-hidden="true" className="h-4 w-4 shrink-0" />
          <span>{statusMessage}</span>
        </div>
      )}

      {loading ? (
        <div className="rounded-2xl border border-slate-200 bg-white p-8 text-center text-sm text-slate-500">
          Đang tải trạng thái các AI provider...
        </div>
      ) : providers.length === 0 ? (
        <div className="rounded-2xl border border-slate-200 bg-white p-8 text-center text-sm text-slate-500">
          Chưa có AI provider nào được cấu hình.
        </div>
      ) : (
        <div className="space-y-6">
          {providers.map((provider) => {
            const captionId = `ai-keys-caption-${provider.providerName}`;
            const testAllBusy =
              busyAction === `test:${provider.providerName}:ALL`;
            const resetAllBusy =
              busyAction === `reset:${provider.providerName}:ALL`;

            return (
              <section
                key={provider.providerName}
                className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xs"
              >
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 bg-slate-50/70 px-5 py-4">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="text-base font-bold text-slate-900">
                        {PROVIDER_TITLES[provider.providerName] ??
                          provider.providerName}
                      </h2>
                      <code className="rounded-md bg-slate-200/70 px-2 py-0.5 text-xs font-semibold text-slate-700">
                        {provider.providerName}
                      </code>
                      {provider.defaultProvider && (
                        <span className="rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-bold text-emerald-800">
                          Default
                        </span>
                      )}
                    </div>
                    <p className="mt-1 text-xs text-slate-600">
                      Nodes khỏe:{" "}
                      <strong className="text-slate-900">
                        {provider.healthyNodes} / {provider.totalNodes}
                      </strong>{" "}
                      • Số key:{" "}
                      <strong className="text-slate-900">
                        {provider.keys.length}
                      </strong>
                    </p>
                  </div>

                  <div className="flex flex-wrap items-center gap-2">
                    <button
                      type="button"
                      disabled={
                        busyAction !== null || provider.keys.length === 0
                      }
                      onClick={() => handleTest(provider.providerName)}
                      aria-label={`Test toàn bộ ${provider.providerName}`}
                      className="inline-flex min-h-9 items-center gap-1.5 rounded-xl bg-emerald-600 px-3.5 py-1.5 text-xs font-bold text-white transition hover:bg-emerald-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 disabled:opacity-50"
                    >
                      <Play aria-hidden="true" className="h-3.5 w-3.5" />
                      {testAllBusy ? "Đang test..." : "Test toàn bộ"}
                    </button>
                    <button
                      type="button"
                      disabled={
                        busyAction !== null || provider.keys.length === 0
                      }
                      onClick={() => handleReset(provider.providerName)}
                      aria-label={`Reset toàn bộ ${provider.providerName}`}
                      className="inline-flex min-h-9 items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 disabled:opacity-50"
                    >
                      <RotateCcw aria-hidden="true" className="h-3.5 w-3.5" />
                      {resetAllBusy ? "Đang reset..." : "Reset Quarantine"}
                    </button>
                  </div>
                </div>

                {provider.keys.length === 0 ? (
                  <div className="px-5 py-6 text-sm text-slate-500">
                    Chưa nạp API key nào trong environment variables cho
                    provider này.
                  </div>
                ) : (
                  <div
                    tabIndex={0}
                    role="region"
                    aria-labelledby={captionId}
                    className="overflow-x-auto focus-visible:outline-2 focus-visible:outline-emerald-600"
                  >
                    <table className="w-full border-collapse text-left text-sm">
                      <caption id={captionId} className="sr-only">
                        Danh sách API keys của {provider.providerName}
                      </caption>
                      <thead className="sticky top-0 z-10 border-b border-slate-200 bg-slate-50 text-xs font-semibold uppercase tracking-wide text-slate-600">
                        <tr>
                          <th scope="col" className="px-5 py-3">
                            API Key
                          </th>
                          <th scope="col" className="px-4 py-3">
                            Models (Thứ tự ưu tiên)
                          </th>
                          <th scope="col" className="px-4 py-3">
                            Trạng thái
                          </th>
                          <th scope="col" className="px-4 py-3">
                            Kiểm tra / Lỗi gần nhất
                          </th>
                          <th scope="col" className="px-5 py-3 text-right">
                            Thao tác
                          </th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {provider.keys.map((k) => {
                          const testBusy =
                            busyAction ===
                            `test:${provider.providerName}:${k.maskedApiKey}`;
                          const resetBusy =
                            busyAction ===
                            `reset:${provider.providerName}:${k.maskedApiKey}`;
                          const toggleBusy =
                            busyAction ===
                            `toggle:${provider.providerName}:${k.maskedApiKey}`;

                          return (
                            <tr
                              key={k.maskedApiKey}
                              className="hover:bg-slate-50/70"
                            >
                              <th
                                scope="row"
                                className="sticky left-0 bg-white px-5 py-3.5 font-mono text-xs font-bold whitespace-nowrap text-slate-900"
                              >
                                <div className="flex items-center gap-2">
                                  <span>{k.maskedApiKey}</span>
                                  {k.currentKey && (
                                    <span className="rounded bg-sky-100 px-1.5 py-0.5 font-sans text-[10px] font-bold text-sky-800">
                                      Sticky
                                    </span>
                                  )}
                                </div>
                              </th>
                              <td className="px-4 py-3.5">
                                <div className="flex flex-wrap gap-1.5">
                                  {k.models.map((m, idx) => (
                                    <span
                                      key={m}
                                      className="rounded-md bg-slate-100 px-2 py-0.5 font-mono text-[11px] text-slate-700"
                                    >
                                      {idx + 1}. {m}
                                    </span>
                                  ))}
                                </div>
                              </td>
                              <td className="px-4 py-3.5 whitespace-nowrap">
                                {renderStatusBadge(k)}
                              </td>
                              <td className="max-w-xs px-4 py-3.5 text-xs text-slate-600">
                                <div className="space-y-0.5">
                                  {k.lastCheckedAt && (
                                    <p className="text-slate-700">
                                      Test lúc{" "}
                                      <strong>
                                        {formatTime(k.lastCheckedAt)}
                                      </strong>
                                      {k.lastLatencyMs != null &&
                                        ` (${k.lastLatencyMs} ms)`}
                                    </p>
                                  )}
                                  {k.lastError ? (
                                    <p
                                      title={k.lastError}
                                      className="truncate font-medium text-rose-700"
                                    >
                                      {formatTime(k.lastErrorAt)}: {k.lastError}
                                    </p>
                                  ) : (
                                    !k.lastCheckedAt && (
                                      <span className="text-slate-400">
                                        Chưa có lỗi runtime
                                      </span>
                                    )
                                  )}
                                </div>
                              </td>
                              <td className="px-5 py-3.5 text-right whitespace-nowrap">
                                <div className="inline-flex items-center justify-end gap-1.5">
                                  <button
                                    type="button"
                                    disabled={busyAction !== null}
                                    aria-label={`Test ${k.maskedApiKey}`}
                                    onClick={() =>
                                      handleTest(
                                        provider.providerName,
                                        k.maskedApiKey
                                      )
                                    }
                                    className="inline-flex min-h-8 items-center gap-1 rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-800 transition hover:bg-emerald-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 disabled:opacity-50"
                                  >
                                    <Play
                                      aria-hidden="true"
                                      className="h-3 w-3"
                                    />
                                    {testBusy ? "..." : "Test"}
                                  </button>
                                  <button
                                    type="button"
                                    disabled={busyAction !== null}
                                    aria-label={`Reset ${k.maskedApiKey}`}
                                    onClick={() =>
                                      handleReset(
                                        provider.providerName,
                                        k.maskedApiKey
                                      )
                                    }
                                    className="inline-flex min-h-8 items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs font-semibold text-slate-700 transition hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 disabled:opacity-50"
                                  >
                                    <RotateCcw
                                      aria-hidden="true"
                                      className="h-3 w-3"
                                    />
                                    {resetBusy ? "..." : "Reset"}
                                  </button>
                                  <button
                                    type="button"
                                    disabled={busyAction !== null}
                                    aria-label={`${
                                      k.disabled ? "Bật lại" : "Tắt tạm"
                                    } ${k.maskedApiKey}`}
                                    onClick={() =>
                                      handleToggle(
                                        provider.providerName,
                                        k.maskedApiKey,
                                        !k.disabled
                                      )
                                    }
                                    className={`inline-flex min-h-8 items-center gap-1 rounded-lg border px-2.5 py-1 text-xs font-semibold transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 disabled:opacity-50 ${
                                      k.disabled
                                        ? "border-sky-200 bg-sky-50 text-sky-800 hover:bg-sky-100"
                                        : "border-rose-200 bg-rose-50 text-rose-800 hover:bg-rose-100"
                                    }`}
                                  >
                                    <Power
                                      aria-hidden="true"
                                      className="h-3 w-3"
                                    />
                                    {toggleBusy
                                      ? "..."
                                      : k.disabled
                                        ? "Bật lại"
                                        : "Tắt tạm"}
                                  </button>
                                </div>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>
            );
          })}
        </div>
      )}
    </div>
  );
}
