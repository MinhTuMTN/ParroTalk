import type { ReactNode } from "react";

type BadgeProps = {
  children: ReactNode;
  tone?: "success" | "neutral";
  className?: string;
};

export default function Badge({
  children,
  tone = "neutral",
  className = "",
}: BadgeProps) {
  const toneClass =
    tone === "success"
      ? "bg-emerald-100 text-emerald-800"
      : "bg-slate-200 text-slate-600";

  return (
    <span
      className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${toneClass} ${className}`}
    >
      {children}
    </span>
  );
}
