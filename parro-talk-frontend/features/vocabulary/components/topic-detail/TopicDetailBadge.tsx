import React from "react";
import { Plane, Compass, Sparkles } from "lucide-react";

export function TopicDetailBadge({ category = "Travel" }: { category?: string }) {
  return (
    <div className="relative w-48 h-40 flex items-center justify-center select-none" aria-hidden="true">
      {/* Ambient dotted orbits */}
      <svg className="absolute inset-0 w-full h-full" viewBox="0 0 200 160">
        <circle
          cx="105"
          cy="80"
          r="68"
          fill="#dcf1e5"
          opacity="0.6"
        />
        <circle
          cx="105"
          cy="80"
          r="60"
          fill="none"
          stroke="#2e9a68"
          strokeWidth="1.2"
          strokeDasharray="3 5"
          opacity="0.35"
        />
      </svg>

      {/* Tilted Sticker Card */}
      <div className="relative rotate-[-8deg] bg-white rounded-2xl border border-brand-200/80 shadow-[0_12px_28px_-8px_rgba(26,101,69,0.18)] p-3.5 px-4 flex flex-col items-center gap-2 transform transition-transform hover:rotate-0 duration-300">
        {/* Floating sparkle badge pinned top-right */}
        <div className="absolute -top-3.5 -right-3.5 w-8 h-8 rounded-full bg-brand-500 text-white flex items-center justify-center shadow-[0_4px_10px_rgba(46,154,104,0.4)] border-2 border-white animate-bounce-subtle">
          <Sparkles className="w-4 h-4 fill-white/20" />
        </div>

        {/* Icons row */}
        <div className="flex items-center gap-3 text-brand-600 mt-1">
          <div className="w-8 h-8 rounded-xl bg-brand-50 border border-brand-100 flex items-center justify-center">
            <Plane className="w-4.5 h-4.5 text-brand-600" />
          </div>
          <div className="w-8 h-8 rounded-xl bg-brand-50 border border-brand-100 flex items-center justify-center">
            <Compass className="w-4.5 h-4.5 text-brand-600" />
          </div>
        </div>

        {/* Sticker Text */}
        <div className="text-[10px] font-extrabold tracking-[0.16em] uppercase text-brand-700 bg-brand-50/80 px-2 py-0.5 rounded-md border border-brand-200/50">
          LET&apos;S GO PLACES
        </div>
      </div>
    </div>
  );
}
