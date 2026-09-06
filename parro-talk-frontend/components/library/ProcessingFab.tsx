import React from "react";
import { Hourglass } from "lucide-react";

interface ProcessingFabProps {
  count?: number;
  onClick?: () => void;
  className?: string;
}

export default function ProcessingFab({
  count = 0,
  onClick,
  className = "",
}: ProcessingFabProps) {
  if (count <= 0) return null;

  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={`Xem tiến trình xử lý (${count})`}
      className={`fixed bottom-6 right-6 md:bottom-8 md:right-8 z-40 bg-white/95 backdrop-blur-md text-gray-800 font-semibold py-3 px-5 rounded-full shadow-lg hover:shadow-xl border border-amber-200/80 flex items-center gap-3 hover:scale-105 active:scale-95 transition-all group cursor-pointer ${className}`}
    >
      <div className="relative flex items-center justify-center">
        <Hourglass
          className="w-5 h-5 text-amber-500 animate-spin"
          style={{ animationDuration: "4s" }}
        />
        <span className="absolute -top-1 -right-1 flex h-2.5 w-2.5">
          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
          <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-amber-500"></span>
        </span>
      </div>
      <span className="text-sm font-bold text-gray-900">
        Processing ({count})
      </span>
    </button>
  );
}
