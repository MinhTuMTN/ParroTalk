"use client";

import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { AiUsageDailyPoint } from "../types/aiUsage";
import { EmptyState, Skeleton } from "./AiUsageStates";
import {
  INPUT_COLOR,
  OUTPUT_COLOR,
  formatCompact,
  formatFull,
  formatShortDate,
} from "./format";

export default function AiUsageChart({
  points,
  loading,
}: {
  points: AiUsageDailyPoint[] | null;
  loading: boolean;
}) {
  const hasData = !!points && points.some((p) => p.totalTokens > 0);
  return (
    <section
      aria-labelledby="ai-usage-chart-title"
      className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs"
    >
      <h2 id="ai-usage-chart-title" className="text-lg font-bold text-slate-900">
        Token theo ngày
      </h2>
      <p className="text-sm text-slate-500">Input so với output mỗi ngày.</p>

      {loading ? (
        <Skeleton className="mt-4 h-72 w-full" />
      ) : !hasData ? (
        <EmptyState message="Không có dữ liệu sử dụng trong khoảng thời gian này." />
      ) : (
        <>
          <div
            className="mt-4 h-72 w-full"
            role="img"
            aria-label="Biểu đồ cột chồng input và output token theo ngày. Xem bảng dữ liệu bên dưới để biết chi tiết."
          >
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={points!} margin={{ top: 8, right: 8, left: 0 }}>
                <CartesianGrid vertical={false} stroke="#E5E7EB" />
                <XAxis
                  dataKey="date"
                  tickFormatter={formatShortDate}
                  tick={{ fontSize: 12, fill: "#4B5563" }}
                  tickLine={false}
                  minTickGap={12}
                />
                <YAxis
                  tickFormatter={formatCompact}
                  tick={{ fontSize: 12, fill: "#4B5563" }}
                  tickLine={false}
                  axisLine={false}
                  width={48}
                />
                <Tooltip
                  cursor={{ fill: "rgba(148,163,184,0.15)" }}
                  labelFormatter={(l) => formatShortDate(String(l))}
                  formatter={(v, name) => [formatFull(Number(v)), name]}
                />
                <Legend />
                <Bar
                  dataKey="inputTokens"
                  name="Input"
                  stackId="t"
                  fill={INPUT_COLOR}
                  isAnimationActive={false}
                />
                <Bar
                  dataKey="outputTokens"
                  name="Output"
                  stackId="t"
                  fill={OUTPUT_COLOR}
                  radius={[4, 4, 0, 0]}
                  isAnimationActive={false}
                />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <details className="mt-3 text-sm">
            <summary className="cursor-pointer rounded font-semibold text-emerald-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600">
              Xem dạng bảng
            </summary>
            <div className="mt-2 max-h-72 overflow-auto rounded-xl border border-slate-200">
              <table className="w-full text-left text-sm">
                <caption className="sr-only">
                  Token input và output theo ngày
                </caption>
                <thead className="sticky top-0 bg-slate-50 text-xs uppercase text-slate-600">
                  <tr>
                    <th scope="col" className="px-3 py-2">Ngày</th>
                    <th scope="col" className="px-3 py-2 text-right">Input</th>
                    <th scope="col" className="px-3 py-2 text-right">Output</th>
                    <th scope="col" className="px-3 py-2 text-right">Tổng</th>
                    <th scope="col" className="px-3 py-2 text-right">Lượt gọi</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 tabular-nums">
                  {points!.map((p) => (
                    <tr key={p.date}>
                      <th scope="row" className="px-3 py-2 font-medium">{p.date}</th>
                      <td className="px-3 py-2 text-right">{formatFull(p.inputTokens)}</td>
                      <td className="px-3 py-2 text-right">{formatFull(p.outputTokens)}</td>
                      <td className="px-3 py-2 text-right">{formatFull(p.totalTokens)}</td>
                      <td className="px-3 py-2 text-right">{formatFull(p.requests)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </details>
        </>
      )}
    </section>
  );
}
