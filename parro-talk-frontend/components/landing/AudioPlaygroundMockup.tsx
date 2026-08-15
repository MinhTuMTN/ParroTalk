"use client";

import { CheckCircle2, Play, Repeat, Sparkles, Volume2 } from "lucide-react";
import { useState } from "react";

export function AudioPlaygroundMockup() {
  const [isPlaying, setIsPlaying] = useState(true);
  const [speed, setSpeed] = useState("1x");
  const [isLooping, setIsLooping] = useState(true);

  return (
    <div
      id="demo-widget"
      className="relative mx-auto w-full max-w-3xl scroll-mt-24 text-left"
    >
      <div className="absolute -inset-4 -z-10 rounded-3xl bg-gradient-to-r from-emerald-200/40 via-teal-100/30 to-amber-100/30 blur-2xl" />

      <div className="glass-card flex flex-col gap-5 rounded-3xl p-5 sm:p-7">
        {/* Header */}
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200/80 pb-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-100 text-emerald-800 shadow-sm">
              <Sparkles size={20} />
            </div>
            <div>
              <div className="text-sm font-extrabold text-slate-900">
                Bài học mẫu: TED Talk
              </div>
              <div className="text-xs font-semibold text-slate-500">
                0:12 / 1:45 · Trình độ B2
              </div>
            </div>
          </div>

          <div className="flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-3.5 py-1 text-xs font-extrabold text-emerald-700 shadow-sm">
            <CheckCircle2 size={16} className="text-emerald-600" /> 94% Chính
            xác
          </div>
        </div>

        {/* Waveform & Play Action */}
        <div className="flex items-center gap-4 rounded-2xl bg-slate-50/80 p-3 border border-slate-100">
          <button
            type="button"
            onClick={() => setIsPlaying(!isPlaying)}
            aria-label={isPlaying ? "Tạm dừng audio" : "Phát audio"}
            className="btn-press flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-emerald-600 text-white shadow-md shadow-emerald-600/25 transition hover:bg-emerald-700"
          >
            {isPlaying ? (
              <Volume2 size={20} />
            ) : (
              <Play size={20} className="fill-current ml-0.5" />
            )}
          </button>

          <div className="flex h-10 flex-1 items-center justify-center gap-1 overflow-hidden px-2">
            {[40, 75, 95, 100, 65, 45, 80, 55, 90, 70, 85, 50, 60, 40, 30].map(
              (height, idx) => (
                <div
                  key={idx}
                  className={`w-1.5 rounded-full transition-all duration-300 ${
                    isPlaying
                      ? "bg-emerald-600 waveform-bar"
                      : "bg-slate-300 h-2"
                  }`}
                  style={{
                    height: isPlaying ? `${Math.max(15, height)}%` : "8px",
                    animationDelay: `${(idx % 6) * 0.12}s`,
                  }}
                />
              ),
            )}
            {[45, 30, 70, 50, 80, 35, 60, 40].map((height, idx) => (
              <div
                key={`inactive-${idx}`}
                className="w-1.5 rounded-full bg-slate-200"
                style={{ height: `${height}%` }}
              />
            ))}
          </div>
        </div>

        {/* Speed & Controls */}
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200/70 bg-white/70 p-2 text-xs">
          <div className="flex items-center gap-1">
            <span className="px-2 font-bold text-slate-500">Tốc độ:</span>
            {["0.5x", "1x", "1.5x", "2x"].map((val) => (
              <button
                key={val}
                type="button"
                onClick={() => setSpeed(val)}
                className={`rounded-lg px-3 py-1.5 font-bold transition-all ${
                  speed === val
                    ? "border border-emerald-200 bg-emerald-50 text-emerald-800 shadow-sm"
                    : "text-slate-600 hover:bg-slate-100"
                }`}
              >
                {val}
              </button>
            ))}
          </div>

          <button
            type="button"
            onClick={() => setIsLooping(!isLooping)}
            className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 font-bold transition-colors ${
              isLooping
                ? "border border-emerald-200 bg-emerald-100 text-emerald-800"
                : "border border-slate-200 bg-white text-slate-500 hover:bg-slate-50"
            }`}
          >
            <Repeat size={15} /> Lặp vòng câu {isLooping ? "(Bật)" : "(Tắt)"}
          </button>
        </div>

        {/* Interactive Typing Input with Heat Map */}
        <div className="relative mt-1">
          <div className="absolute -top-3 left-4 z-10 flex items-center gap-1.5 rounded-full border border-emerald-200 bg-white px-2.5 py-0.5 text-[10px] font-extrabold uppercase tracking-wider text-emerald-700 shadow-sm">
            <span className="h-2 w-2 animate-pulse rounded-full bg-emerald-500" />
            Typing Heat: Đang gõ đúng
          </div>

          <div className="relative overflow-hidden rounded-2xl border-2 border-emerald-500/40 bg-white p-5 pt-6 shadow-[0_0_20px_rgba(5,150,105,0.06)]">
            <div className="relative z-10 flex flex-wrap items-center gap-x-1.5 text-base sm:text-lg font-medium leading-relaxed text-slate-800">
              <span>Vulnerability is not</span>
              <span className="rounded-md border border-emerald-200 bg-emerald-100 px-1.5 py-0.5 font-bold text-emerald-800">
                wea
              </span>
              <span className="inline-block h-5 w-0.5 animate-pulse bg-emerald-600 align-middle" />
              <span className="ml-1 italic text-slate-400">
                ...and that truth is profoundly dangerous.
              </span>
            </div>
          </div>

          {/* Accuracy Progress Bar */}
          <div className="mt-3 flex h-2 w-full overflow-hidden rounded-full bg-slate-100 shadow-inner">
            <div
              className="h-full w-[65%] rounded-l-full bg-emerald-600 transition-all"
              title="Gõ đúng"
            />
            <div
              className="h-full w-[25%] bg-emerald-300 transition-all"
              title="Gợi ý"
            />
            <div
              className="h-full w-[10%] bg-amber-400 transition-all"
              title="Chờ kiểm tra"
            />
          </div>
        </div>
      </div>
    </div>
  );
}
