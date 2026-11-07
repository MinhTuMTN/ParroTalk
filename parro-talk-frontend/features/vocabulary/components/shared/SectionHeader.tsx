import type { ReactNode } from "react";

interface SectionHeaderProps {
  title: ReactNode;
  badge?: ReactNode;
  right?: ReactNode;
  className?: string;
}

export function SectionHeader({ title, badge, right, className = "mb-4" }: SectionHeaderProps) {
  return (
    <div className={`flex items-center justify-between gap-4 ${className}`}>
      <div className="flex items-center gap-2.5">
        <h2 className="text-lg font-bold tracking-tight text-ink-900">{title}</h2>
        {badge && (
          <span className="rounded-full bg-brand-100 px-2 py-0.5 text-[11px] font-semibold text-brand-700">
            {badge}
          </span>
        )}
      </div>
      {right && <div className="text-xs text-ink-500">{right}</div>}
    </div>
  );
}
