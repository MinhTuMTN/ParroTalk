"use client";

import { Search, UserX, X } from "lucide-react";
import { MAX_RANGE_DAYS, type Preset } from "../hooks/useAiUsage";
import type { AiUsageByModel } from "../types/aiUsage";

interface Props {
  preset: Preset;
  from: string;
  to: string;
  rangeError: string | null;
  onPreset: (p: Preset) => void;
  onRange: (r: { from: string; to: string }) => void;
  model: string;
  modelOptions: AiUsageByModel[];
  onModel: (m: string) => void;
  system: boolean;
  onSystem: (on: boolean) => void;
  userLabel: string;
  onClearUser: () => void;
  userSearch: string;
  onUserSearch: (v: string) => void;
}

const PRESETS: { value: Preset; label: string }[] = [
  { value: 7, label: "7 ngày" },
  { value: 14, label: "14 ngày" },
  { value: 30, label: "30 ngày" },
  { value: "custom", label: "Tùy chọn" },
];

const focus =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600";
const control = `min-h-10 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 shadow-2xs ${focus}`;

export default function AiUsageFiltersBar(p: Props) {
  return (
    <section
      aria-label="Bộ lọc AI usage"
      className="space-y-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs"
    >
      <div className="flex flex-wrap items-end gap-3">
        <div
          role="group"
          aria-label="Khoảng thời gian"
          className="inline-flex rounded-xl border border-slate-200 bg-slate-50 p-1"
        >
          {PRESETS.map((o) => (
            <button
              key={String(o.value)}
              type="button"
              aria-pressed={p.preset === o.value}
              onClick={() => p.onPreset(o.value)}
              className={`min-h-8 rounded-lg px-3 py-1 text-sm font-semibold transition ${focus} ${
                p.preset === o.value
                  ? "bg-white text-emerald-700 shadow-2xs"
                  : "text-slate-600 hover:text-slate-900"
              }`}
            >
              {o.label}
            </button>
          ))}
        </div>

        {p.preset === "custom" && (
          <div className="flex flex-wrap items-end gap-2">
            <label className="text-xs font-semibold text-slate-600">
              Từ ngày
              <input
                type="date"
                value={p.from}
                max={p.to}
                onChange={(e) => p.onRange({ from: e.target.value, to: p.to })}
                aria-invalid={!!p.rangeError}
                className={`${control} mt-1 block`}
              />
            </label>
            <label className="text-xs font-semibold text-slate-600">
              Đến ngày
              <input
                type="date"
                value={p.to}
                min={p.from}
                onChange={(e) =>
                  p.onRange({ from: p.from, to: e.target.value })
                }
                aria-invalid={!!p.rangeError}
                className={`${control} mt-1 block`}
              />
            </label>
          </div>
        )}

        <label className="text-xs font-semibold text-slate-600">
          Model
          <select
            value={p.model}
            onChange={(e) => p.onModel(e.target.value)}
            className={`${control} mt-1 block min-w-44`}
          >
            <option value="">Tất cả model</option>
            {p.model && !p.modelOptions.some((m) => m.model === p.model) && (
              <option value={p.model}>{p.model}</option>
            )}
            {p.modelOptions.map((m) => (
              <option key={`${m.provider}/${m.model}`} value={m.model}>
                {m.model} ({m.provider})
              </option>
            ))}
          </select>
        </label>

        <label className="min-w-52 flex-1 text-xs font-semibold text-slate-600">
          Tìm người dùng
          <span className="relative mt-1 block">
            <Search
              aria-hidden="true"
              className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
            />
            <input
              type="search"
              value={p.userSearch}
              onChange={(e) => p.onUserSearch(e.target.value)}
              placeholder="Email hoặc tên…"
              className={`${control} w-full pl-9`}
            />
          </span>
        </label>

        <button
          type="button"
          aria-pressed={p.system}
          onClick={() => p.onSystem(!p.system)}
          className={`inline-flex min-h-10 items-center gap-2 rounded-xl border px-3 py-2 text-sm font-semibold transition ${focus} ${
            p.system
              ? "border-emerald-300 bg-emerald-50 text-emerald-800"
              : "border-slate-200 bg-white text-slate-700 hover:bg-slate-50"
          }`}
        >
          <UserX aria-hidden="true" className="h-4 w-4" />
          System
        </button>
      </div>

      {p.rangeError ? (
        <p role="alert" className="text-sm font-medium text-rose-700">
          {p.rangeError}
        </p>
      ) : (
        <p className="text-xs text-slate-500">
          Tối đa {MAX_RANGE_DAYS} ngày. Chọn một người dùng ở bảng bên dưới để
          lọc toàn bộ trang.
        </p>
      )}

      {p.userLabel && (
        <div className="inline-flex items-center gap-2 rounded-full border border-emerald-200 bg-emerald-50 py-1 pl-3 pr-1 text-sm font-semibold text-emerald-800">
          Đang lọc: {p.userLabel}
          <button
            type="button"
            onClick={p.onClearUser}
            aria-label="Bỏ lọc người dùng"
            className={`inline-flex h-6 w-6 items-center justify-center rounded-full hover:bg-emerald-100 ${focus}`}
          >
            <X aria-hidden="true" className="h-3.5 w-3.5" />
          </button>
        </div>
      )}
    </section>
  );
}
