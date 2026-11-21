import { Activity, ArrowDownToLine, ArrowUpFromLine, Sigma } from "lucide-react";
import type { ReactNode } from "react";
import type { AiUsageSummary } from "../types/aiUsage";
import { Skeleton } from "./AiUsageStates";
import Num from "./Num";

function Card({
  icon,
  label,
  value,
  hint,
  loading,
}: {
  icon: ReactNode;
  label: string;
  value: ReactNode;
  hint?: ReactNode;
  loading: boolean;
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
      <div className="flex items-center gap-2 text-sm font-semibold text-slate-600">
        <span aria-hidden="true" className="text-emerald-600">
          {icon}
        </span>
        {label}
      </div>
      {loading ? (
        <>
          <Skeleton className="mt-3 h-8 w-24" />
          <Skeleton className="mt-2 h-4 w-32" />
        </>
      ) : (
        <>
          <p className="mt-2 text-3xl font-bold tabular-nums text-slate-900">
            {value}
          </p>
          {hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
        </>
      )}
    </div>
  );
}

export default function AiUsageKpiCards({
  summary,
  loading,
}: {
  summary: AiUsageSummary | null;
  loading: boolean;
}) {
  const s = summary;
  const pct = (n: number) =>
    s && s.totalTokens > 0 ? `${((n / s.totalTokens) * 100).toFixed(1)}% tổng token` : "—";
  return (
    <section
      aria-label="Chỉ số tổng quan"
      className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"
    >
      <Card
        loading={loading}
        icon={<Sigma className="h-4 w-4" />}
        label="Tổng token"
        value={<Num value={s?.totalTokens} />}
      />
      <Card
        loading={loading}
        icon={<ArrowDownToLine className="h-4 w-4" />}
        label="Input token"
        value={<Num value={s?.inputTokens} />}
        hint={pct(s?.inputTokens ?? 0)}
      />
      <Card
        loading={loading}
        icon={<ArrowUpFromLine className="h-4 w-4" />}
        label="Output token"
        value={<Num value={s?.outputTokens} />}
        hint={pct(s?.outputTokens ?? 0)}
      />
      <Card
        loading={loading}
        icon={<Activity className="h-4 w-4" />}
        label="Lượt gọi AI"
        value={<Num value={s?.totalRequests} />}
        hint={
          <>
            <Num value={s?.activeUsers} /> người dùng hoạt động · TB{" "}
            <Num value={Math.round(s?.avgTokensPerRequest ?? 0)} /> token/lượt
          </>
        }
      />
    </section>
  );
}
