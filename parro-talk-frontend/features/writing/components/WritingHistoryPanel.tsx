"use client";

import React from "react";
import { Clock, Trash2, CheckCircle2, FileText } from "lucide-react";
import type { WritingSessionSummary } from "../types";

interface WritingHistoryPanelProps {
  sessions: WritingSessionSummary[];
  activeSessionId?: string;
  loading: boolean;
  onSelectSession: (sessionId: string) => void;
  onDeleteSession: (sessionId: string) => void;
}

export function WritingHistoryPanel({
  sessions,
  activeSessionId,
  loading,
  onSelectSession,
  onDeleteSession,
}: WritingHistoryPanelProps) {
  return (
    <aside className="rounded-3xl border border-ink-200/80 dark:border-slate-800 bg-white dark:bg-slate-900 p-5 shadow-card space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-sm font-extrabold text-ink-900 dark:text-slate-100">
          <Clock className="h-4 w-4 text-brand-600 dark:text-emerald-400" aria-hidden="true" />
          <h2>Lịch sử luyện viết</h2>
        </div>
        <span className="rounded-full bg-ink-100 dark:bg-slate-800 px-2.5 py-0.5 text-xs font-bold text-ink-600 dark:text-slate-300">
          {sessions.length}
        </span>
      </div>

      {loading ? (
        <div className="space-y-2.5">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="h-20 rounded-2xl bg-ink-100/70 dark:bg-slate-800 animate-pulse"
            />
          ))}
        </div>
      ) : sessions.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-ink-200 dark:border-slate-700 p-6 text-center space-y-2">
          <FileText className="h-7 w-7 text-ink-300 dark:text-slate-500 mx-auto" aria-hidden="true" />
          <p className="text-xs font-semibold text-ink-500 dark:text-slate-400">
            Chưa có phiên luyện viết nào. Hãy nhập ý tưởng đầu tiên của bạn!
          </p>
        </div>
      ) : (
        <div className="space-y-2.5 max-h-[540px] overflow-y-auto pr-1">
          {sessions.map((item) => {
            const isActive = item.id === activeSessionId;
            return (
              <div
                key={item.id}
                className={`group relative rounded-2xl border p-3.5 transition-all ${
                  isActive
                    ? "border-brand-500 bg-brand-50/70 dark:bg-emerald-950/40 shadow-2xs"
                    : "border-ink-100 dark:border-slate-800 bg-ink-50/40 dark:bg-slate-800/50 hover:border-brand-300 dark:hover:border-emerald-700 hover:bg-white dark:hover:bg-slate-800"
                }`}
              >
                <button
                  type="button"
                  onClick={() => onSelectSession(item.id)}
                  className="w-full text-left space-y-1.5 pr-6 cursor-pointer"
                >
                  <div className="flex items-center gap-1.5 flex-wrap">
                    <span className="rounded-md bg-brand-100 dark:bg-emerald-950/80 px-1.5 py-0.5 text-[10px] font-extrabold text-brand-800 dark:text-emerald-300">
                      {item.scaleType} {item.targetLevel}
                    </span>
                    <span className="rounded-md bg-ink-100 dark:bg-slate-700 px-1.5 py-0.5 text-[10px] font-bold text-ink-600 dark:text-slate-200">
                      {item.mode === "SUGGESTION" ? "Suggestion" : "Guidance"}
                    </span>
                    {item.hasUserDraft && (
                      <span className="inline-flex items-center gap-0.5 text-[10px] font-bold text-emerald-700 dark:text-emerald-300">
                        <CheckCircle2 className="h-3 w-3" aria-hidden="true" />
                        <span>Đã viết nháp</span>
                      </span>
                    )}
                  </div>

                  <p className="text-xs font-bold text-ink-900 dark:text-slate-100 line-clamp-2 leading-snug">
                    {item.inputText}
                  </p>
                </button>

                <button
                  type="button"
                  onClick={() => onDeleteSession(item.id)}
                  aria-label="Xóa phiên luyện viết"
                  className="absolute top-3 right-3 rounded-lg p-1 text-ink-400 dark:text-slate-400 opacity-70 hover:bg-red-50 dark:hover:bg-rose-950/60 hover:text-red-600 dark:hover:text-rose-300 group-hover:opacity-100 transition-all cursor-pointer"
                >
                  <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                </button>
              </div>
            );
          })}
        </div>
      )}
    </aside>
  );
}

