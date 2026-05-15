"use client";

import type { InputHTMLAttributes } from "react";
import { Search } from "lucide-react";

type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  withSearchIcon?: boolean;
};

export default function Input({
  withSearchIcon = false,
  className = "",
  ...props
}: InputProps) {
  return (
    <div className="relative w-full">
      {withSearchIcon && (
        <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
      )}
      <input
        className={`w-full rounded-2xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-800 outline-none transition focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100 ${withSearchIcon ? "pl-10" : ""} ${className}`}
        {...props}
      />
    </div>
  );
}

