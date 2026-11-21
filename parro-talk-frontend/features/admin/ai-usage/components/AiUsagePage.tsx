"use client";

import { BrainCircuit, RefreshCw } from "lucide-react";
import { useAiUsage } from "../hooks/useAiUsage";
import AiUsageBreakdown from "./AiUsageBreakdown";
import AiUsageChart from "./AiUsageChart";
import AiUsageFiltersBar from "./AiUsageFiltersBar";
import AiUsageKpiCards from "./AiUsageKpiCards";
import { ErrorState } from "./AiUsageStates";

export default function AiUsagePage() {
  const u = useAiUsage();
  const all = [u.summary, u.timeseries, u.models, u.features, u.users, u.modelOptions];
  const refreshing = all.some((s) => s.loading);
  const refresh = () => all.forEach((s) => s.retry());

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="flex items-center gap-2.5 text-2xl font-bold text-slate-900">
            <BrainCircuit aria-hidden="true" className="h-6 w-6 text-emerald-600" />
            AI Usage
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Theo dõi lượng token AI đã dùng theo ngày, model, người dùng và
            tính năng. Dữ liệu được giữ 30 ngày.
          </p>
        </div>
        <button
          type="button"
          onClick={refresh}
          disabled={refreshing}
          className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 shadow-2xs transition hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 disabled:opacity-50"
        >
          <RefreshCw
            aria-hidden="true"
            className={`h-4 w-4 ${refreshing ? "animate-spin" : ""}`}
          />
          Làm mới
        </button>
      </div>

      <AiUsageFiltersBar
        preset={u.preset}
        from={u.range.from}
        to={u.range.to}
        rangeError={u.rangeError}
        onPreset={u.selectPreset}
        onRange={u.setCustomRange}
        model={u.model}
        modelOptions={u.modelOptions.data ?? []}
        onModel={u.changeModel}
        system={u.system}
        onSystem={u.selectSystem}
        userLabel={u.system ? "System (không có người dùng)" : u.userLabel}
        onClearUser={u.clearUser}
        userSearch={u.userSearchInput}
        onUserSearch={u.changeUserSearch}
      />

      {u.summary.error && (
        <ErrorState message={u.summary.error} onRetry={u.summary.retry} />
      )}

      <AiUsageKpiCards summary={u.summary.data} loading={u.summary.loading} />

      {u.timeseries.error ? (
        <ErrorState message={u.timeseries.error} onRetry={u.timeseries.retry} />
      ) : (
        <AiUsageChart points={u.timeseries.data} loading={u.timeseries.loading} />
      )}

      <AiUsageBreakdown
        models={u.models}
        features={u.features}
        users={u.users}
        page={u.userPage}
        onPage={u.setUserPage}
        onSelectUser={u.selectUser}
        onSelectSystem={() => u.selectSystem(true)}
        activeUserId={u.userId}
        system={u.system}
      />
    </div>
  );
}
