"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { useId, useRef, useState, type KeyboardEvent, type ReactNode } from "react";
import type {
  AiUsageByFeature,
  AiUsageByModel,
  AiUsageByUser,
  PagedResult,
} from "../types/aiUsage";
import { EmptyState, ErrorState, Skeleton } from "./AiUsageStates";
import { formatDateTime } from "./format";
import Num from "./Num";

interface AsyncSlice<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
  retry: () => void;
}

interface Props {
  models: AsyncSlice<AiUsageByModel[] | null>;
  features: AsyncSlice<AiUsageByFeature[] | null>;
  users: AsyncSlice<PagedResult<AiUsageByUser> | null>;
  page: number;
  onPage: (p: number) => void;
  onSelectUser: (id: string, label: string) => void;
  onSelectSystem: () => void;
  activeUserId: string;
  system: boolean;
}

type Tab = "models" | "users" | "features";
const TABS: { id: Tab; label: string }[] = [
  { id: "models", label: "Theo model" },
  { id: "users", label: "Theo người dùng" },
  { id: "features", label: "Theo tính năng" },
];

const focus =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600";
const th = "px-3 py-2 text-xs font-bold uppercase tracking-wide text-slate-600";
const td = "px-3 py-2.5";

function Share({ pct }: { pct: number }) {
  return (
    <div className="flex items-center justify-end gap-2">
      <div
        aria-hidden="true"
        className="hidden h-1.5 w-16 overflow-hidden rounded-full bg-slate-100 sm:block"
      >
        <div
          className="h-full rounded-full bg-emerald-600"
          style={{ width: `${Math.min(100, Math.max(0, pct))}%` }}
        />
      </div>
      <span className="w-12 text-right tabular-nums">{pct.toFixed(1)}%</span>
    </div>
  );
}

function Table({
  caption,
  head,
  children,
}: {
  caption: string;
  head: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[640px] text-left text-sm">
        <caption className="sr-only">{caption}</caption>
        <thead className="border-b border-slate-200 bg-slate-50">
          <tr>{head}</tr>
        </thead>
        <tbody className="divide-y divide-slate-100 tabular-nums text-slate-800">
          {children}
        </tbody>
      </table>
    </div>
  );
}

function TableSkeleton() {
  return (
    <div aria-busy="true" className="space-y-2 py-2">
      {Array.from({ length: 5 }).map((_, i) => (
        <Skeleton key={i} className="h-9 w-full" />
      ))}
    </div>
  );
}

function userLabel(u: AiUsageByUser) {
  return u.userId === null
    ? "System"
    : u.displayName || u.email || u.userId;
}

export default function AiUsageBreakdown(p: Props) {
  const [tab, setTab] = useState<Tab>("models");
  const baseId = useId();
  const tabRefs = useRef<Record<Tab, HTMLButtonElement | null>>({
    models: null,
    users: null,
    features: null,
  });

  const onKey = (e: KeyboardEvent, idx: number) => {
    let next = idx;
    if (e.key === "ArrowRight") next = (idx + 1) % TABS.length;
    else if (e.key === "ArrowLeft") next = (idx - 1 + TABS.length) % TABS.length;
    else if (e.key === "Home") next = 0;
    else if (e.key === "End") next = TABS.length - 1;
    else return;
    e.preventDefault();
    setTab(TABS[next].id);
    tabRefs.current[TABS[next].id]?.focus();
  };

  const numHead = (label: string) => (
    <th scope="col" className={`${th} text-right`}>
      {label}
    </th>
  );

  const body = (() => {
    if (tab === "models") {
      const s = p.models;
      if (s.loading) return <TableSkeleton />;
      if (s.error) return <ErrorState message={s.error} onRetry={s.retry} />;
      if (!s.data?.length) return <EmptyState message="Chưa có dữ liệu theo model." />;
      return (
        <Table
          caption="Sử dụng AI theo model"
          head={
            <>
              <th scope="col" className={th}>Model</th>
              <th scope="col" className={th}>Provider</th>
              {numHead("Lượt gọi")}
              {numHead("Input")}
              {numHead("Output")}
              {numHead("Tổng")}
              {numHead("Tỉ lệ")}
            </>
          }
        >
          {s.data.map((m) => (
            <tr key={`${m.provider}/${m.model}`}>
              <th scope="row" className={`${td} font-semibold`}>{m.model}</th>
              <td className={td}>{m.provider}</td>
              <td className={`${td} text-right`}><Num value={m.requests} /></td>
              <td className={`${td} text-right`}><Num value={m.inputTokens} /></td>
              <td className={`${td} text-right`}><Num value={m.outputTokens} /></td>
              <td className={`${td} text-right font-semibold`}><Num value={m.totalTokens} /></td>
              <td className={td}><Share pct={m.sharePercent} /></td>
            </tr>
          ))}
        </Table>
      );
    }
    if (tab === "features") {
      const s = p.features;
      if (s.loading) return <TableSkeleton />;
      if (s.error) return <ErrorState message={s.error} onRetry={s.retry} />;
      if (!s.data?.length) return <EmptyState message="Chưa có dữ liệu theo tính năng." />;
      return (
        <Table
          caption="Sử dụng AI theo tính năng"
          head={
            <>
              <th scope="col" className={th}>Tính năng</th>
              {numHead("Lượt gọi")}
              {numHead("Input")}
              {numHead("Output")}
              {numHead("Tổng")}
              {numHead("Tỉ lệ")}
            </>
          }
        >
          {s.data.map((f) => (
            <tr key={f.feature}>
              <th scope="row" className={`${td} font-mono text-xs font-semibold`}>{f.feature}</th>
              <td className={`${td} text-right`}><Num value={f.requests} /></td>
              <td className={`${td} text-right`}><Num value={f.inputTokens} /></td>
              <td className={`${td} text-right`}><Num value={f.outputTokens} /></td>
              <td className={`${td} text-right font-semibold`}><Num value={f.totalTokens} /></td>
              <td className={td}><Share pct={f.sharePercent} /></td>
            </tr>
          ))}
        </Table>
      );
    }
    const s = p.users;
    if (s.loading) return <TableSkeleton />;
    if (s.error) return <ErrorState message={s.error} onRetry={s.retry} />;
    const items = s.data?.items ?? [];
    if (!items.length) return <EmptyState message="Không tìm thấy người dùng nào." />;
    const totalPages = s.data?.totalPages ?? 1;
    return (
      <>
        <Table
          caption="Sử dụng AI theo người dùng. Chọn một hàng để lọc toàn bộ trang."
          head={
            <>
              <th scope="col" className={th}>Người dùng</th>
              {numHead("Lượt gọi")}
              {numHead("Input")}
              {numHead("Output")}
              {numHead("Tổng")}
              <th scope="col" className={th}>Lần cuối</th>
            </>
          }
        >
          {items.map((u) => {
            const isSystem = u.userId === null;
            const selected = isSystem ? p.system : p.activeUserId === u.userId;
            return (
              <tr
                key={u.userId ?? "system"}
                className={`cursor-pointer hover:bg-emerald-50/50 ${selected ? "bg-emerald-50" : ""}`}
                onClick={() =>
                  isSystem ? p.onSelectSystem() : p.onSelectUser(u.userId!, userLabel(u))
                }
              >
                <th scope="row" className={`${td} font-normal`}>
                  <button
                    type="button"
                    aria-pressed={selected}
                    aria-label={`Lọc theo ${userLabel(u)}`}
                    onClick={(e) => {
                      e.stopPropagation();
                      if (isSystem) p.onSelectSystem();
                      else p.onSelectUser(u.userId!, userLabel(u));
                    }}
                    className={`rounded text-left ${focus}`}
                  >
                    <span className="block font-semibold text-slate-900">
                      {userLabel(u)}
                      {u.deleted && (
                        <span className="ml-2 rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">
                          đã xóa
                        </span>
                      )}
                    </span>
                    {u.email && (
                      <span className="block text-xs text-slate-500">{u.email}</span>
                    )}
                  </button>
                </th>
                <td className={`${td} text-right`}><Num value={u.requests} /></td>
                <td className={`${td} text-right`}><Num value={u.inputTokens} /></td>
                <td className={`${td} text-right`}><Num value={u.outputTokens} /></td>
                <td className={`${td} text-right font-semibold`}><Num value={u.totalTokens} /></td>
                <td className={`${td} whitespace-nowrap text-xs text-slate-600`}>
                  {formatDateTime(u.lastUsedAt)}
                </td>
              </tr>
            );
          })}
        </Table>
        <nav
          aria-label="Phân trang người dùng"
          className="flex items-center justify-between gap-3 border-t border-slate-100 pt-3 text-sm text-slate-600"
        >
          <span aria-live="polite">
            Trang {p.page + 1}/{Math.max(totalPages, 1)} ·{" "}
            {s.data?.totalElements ?? 0} người dùng
          </span>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={p.page <= 0}
              onClick={() => p.onPage(p.page - 1)}
              aria-label="Trang trước"
              className={`inline-flex min-h-9 items-center rounded-lg border border-slate-200 bg-white px-2.5 font-semibold hover:bg-slate-50 disabled:opacity-50 ${focus}`}
            >
              <ChevronLeft aria-hidden="true" className="h-4 w-4" />
            </button>
            <button
              type="button"
              disabled={p.page + 1 >= totalPages}
              onClick={() => p.onPage(p.page + 1)}
              aria-label="Trang sau"
              className={`inline-flex min-h-9 items-center rounded-lg border border-slate-200 bg-white px-2.5 font-semibold hover:bg-slate-50 disabled:opacity-50 ${focus}`}
            >
              <ChevronRight aria-hidden="true" className="h-4 w-4" />
            </button>
          </div>
        </nav>
      </>
    );
  })();

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
      <div
        role="tablist"
        aria-label="Phân tích chi tiết"
        className="mb-4 flex gap-1 overflow-x-auto border-b border-slate-200"
      >
        {TABS.map((t, i) => (
          <button
            key={t.id}
            ref={(el) => {
              tabRefs.current[t.id] = el;
            }}
            id={`${baseId}-tab-${t.id}`}
            role="tab"
            type="button"
            aria-selected={tab === t.id}
            aria-controls={`${baseId}-panel`}
            tabIndex={tab === t.id ? 0 : -1}
            onClick={() => setTab(t.id)}
            onKeyDown={(e) => onKey(e, i)}
            className={`-mb-px whitespace-nowrap border-b-2 px-4 py-2 text-sm font-semibold ${focus} ${
              tab === t.id
                ? "border-emerald-600 text-emerald-700"
                : "border-transparent text-slate-600 hover:text-slate-900"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>
      <div
        role="tabpanel"
        id={`${baseId}-panel`}
        aria-labelledby={`${baseId}-tab-${tab}`}
        tabIndex={0}
        className={`rounded ${focus}`}
      >
        {body}
      </div>
    </section>
  );
}
