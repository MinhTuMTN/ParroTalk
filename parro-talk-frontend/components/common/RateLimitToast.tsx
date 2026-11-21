"use client";

import { useEffect, useState } from "react";
import { Clock, X } from "lucide-react";

import { RATE_LIMIT_EVENT, type RateLimitEventDetail } from "@/lib/axios";

interface RateLimitState {
  totalSeconds: number;
  expiresAt: number;
  remainingSeconds: number;
  message: string;
}

function formatCountdown(seconds: number): string {
  if (seconds < 60) {
    return `${seconds}s`;
  }
  const minutes = Math.floor(seconds / 60);
  const rem = seconds % 60;
  return rem > 0 ? `${minutes}m ${rem}s` : `${minutes}m`;
}

export function RateLimitToast() {
  const [state, setState] = useState<RateLimitState | null>(null);

  useEffect(() => {
    const handleRateLimit = (event: Event) => {
      const customEvent = event as CustomEvent<RateLimitEventDetail>;
      const rawSeconds = customEvent.detail?.retryAfterSeconds;
      const retryAfterSeconds =
        typeof rawSeconds === "number" && Number.isFinite(rawSeconds) && rawSeconds > 0
          ? Math.ceil(rawSeconds)
          : 60;
      const message =
        customEvent.detail?.message ??
        "Bạn đang gửi yêu cầu quá nhanh. Vui lòng chờ một lát rồi thử lại.";

      const now = Date.now();
      const incomingExpiresAt = now + retryAfterSeconds * 1000;

      setState((prev) => {
        const nextExpiresAt = prev
          ? Math.max(prev.expiresAt, incomingExpiresAt)
          : incomingExpiresAt;
        const nextRemaining = Math.max(1, Math.ceil((nextExpiresAt - now) / 1000));
        return {
          totalSeconds: Math.max(prev?.totalSeconds ?? 0, nextRemaining),
          expiresAt: nextExpiresAt,
          remainingSeconds: nextRemaining,
          message,
        };
      });
    };

    window.addEventListener(RATE_LIMIT_EVENT, handleRateLimit);
    return () => window.removeEventListener(RATE_LIMIT_EVENT, handleRateLimit);
  }, []);

  const isVisible = state !== null;

  useEffect(() => {
    if (!isVisible) {
      return;
    }

    const timer = window.setInterval(() => {
      const now = Date.now();
      setState((prev) => {
        if (!prev) {
          return null;
        }
        const nextRemaining = Math.ceil((prev.expiresAt - now) / 1000);
        if (nextRemaining <= 0) {
          return null;
        }
        return {
          ...prev,
          remainingSeconds: nextRemaining,
        };
      });
    }, 1000);

    return () => window.clearInterval(timer);
  }, [isVisible]);

  if (!state) {
    return null;
  }

  const progressPercent = Math.max(
    0,
    Math.min(100, (state.remainingSeconds / Math.max(1, state.totalSeconds)) * 100)
  );

  return (
    <div
      role="status"
      aria-live="polite"
      aria-atomic="false"
      className="fixed bottom-4 right-4 z-[100] w-[calc(100vw-2rem)] max-w-sm overflow-hidden rounded-xl border border-amber-300/80 bg-white/95 shadow-lg backdrop-blur-md dark:border-amber-500/40 dark:bg-slate-900/95"
    >
      <div className="flex items-start gap-3 p-4">
        <div className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-300">
          <Clock className="h-5 w-5" aria-hidden="true" />
        </div>

        <div className="min-w-0 flex-1">
          <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">
            Quá nhiều yêu cầu
          </p>
          <p className="mt-0.5 text-xs leading-relaxed text-slate-600 dark:text-slate-300">
            {state.message}
          </p>
          <p
            aria-live="off"
            className="mt-1.5 text-xs font-medium text-amber-700 dark:text-amber-300"
          >
            Vui lòng thử lại sau{" "}
            <span className="tabular-nums font-semibold">
              {formatCountdown(state.remainingSeconds)}
            </span>
          </p>
        </div>

        <button
          type="button"
          onClick={() => setState(null)}
          aria-label="Đóng thông báo giới hạn tần suất"
          className="rounded-md p-1 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-800 dark:hover:text-slate-200"
        >
          <X className="h-4 w-4" aria-hidden="true" />
        </button>
      </div>

      <div className="h-1 w-full bg-amber-100 dark:bg-amber-950/60">
        <div
          className="h-full bg-amber-500 transition-[width] duration-1000 ease-linear dark:bg-amber-400"
          style={{ width: `${progressPercent}%` }}
        />
      </div>
    </div>
  );
}
