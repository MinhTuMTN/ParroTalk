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
    <aside className="rounded-3xl border border-ink-200/80 bg-white p-5 shadow-card space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-sm font-extrabold text-ink-900">
          <Clock className="h-4 w-4 text-brand-600" aria-hidden="true" />
          <h2>Lịch sử luyện viết</h2>
        </div>
        <span className="rounded-full bg-ink-100 px-2.5 py-0.5 text-xs font-bold text-ink-600">
          {sessions.length}
        </span>
      </div>

      {loading ? (
        <div className="space-y-2.5">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="h-20 rounded-2xl bg-ink-100/70 animate-pulse"
            />
          ))}
        </div>
      ) : sessions.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-ink-200 p-6 text-center space-y-2">
          <FileText className="h-7 w-7 text-ink-300 mx-auto" aria-hidden="true" />
          <p className="text-xs font-semibold text-ink-500">
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
                    ? "border-brand-500 bg-brand-50/70 shadow-2xs"
                    : "border-ink-100 bg-ink-50/40 hover:border-brand-300 hover:bg-white"
                }`}
              >
                <button
                  type="button"
                  onClick={() => onSelectSession(item.id)}
                  className="w-full text-left space-y-1.5 pr-6 cursor-pointer"
                >
                  <div className="flex items-center gap-1.5 flex-wrap">
                    <span className="rounded-md bg-brand-100 px-1.5 py-0.5 text-[10px] font-extrabold text-brand-800">
                      {item.scaleType} {item.targetLevel}
                    </span>
                    <span className="rounded-md bg-ink-100 px-1.5 py-0.5 text-[10px] font-bold text-ink-600">
                      {item.mode === "SUGGESTION" ? "Suggestion" : "Guidance"}
                    </span>
                    {item.hasUserDraft && (
                      <span className="inline-flex items-center gap-0.5 text-[10px] font-bold text-emerald-700">
                        <CheckCircle2 className="h-3 w-3" aria-hidden="true" />
                        <span>Đã viết nháp</span>
                      </span>
                    )}
                  </div>

                  <p className="text-xs font-bold text-ink-900 line-clamp-2 leading-snug">
                    {item.inputText}
                  </p>
                </button>

                <button
                  type="button"
                  onClick={() => onDeleteSession(item.id)}
                  aria-label="Xóa phiên luyện viết"
                  className="absolute top-3 right-3 rounded-lg p-1 text-ink-400 opacity-70 hover:bg-red-50 hover:text-red-600 group-hover:opacity-100 transition-all cursor-pointer"
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

