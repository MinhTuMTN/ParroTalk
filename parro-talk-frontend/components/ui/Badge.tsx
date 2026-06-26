import type { ReactNode } from "react";

type BadgeProps = {
  children: ReactNode;
  tone?: "success" | "neutral" | "info" | "warning";
  className?: string;
};

export default function Badge({
  children,
  tone = "neutral",
  className = "",
}: BadgeProps) {
  const toneClassMap = {
    success: "bg-emerald-100 text-emerald-800",
    neutral: "bg-slate-200 text-slate-600",
    info: "bg-sky-100 text-sky-700",
    warning: "bg-amber-100 text-amber-700",
  } as const;

  return (
    <span
      className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${toneClassMap[tone]} ${className}`}
    >
      {children}
    </span>
  );
}

