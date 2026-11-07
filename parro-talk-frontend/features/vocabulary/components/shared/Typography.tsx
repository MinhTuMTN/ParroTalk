import { Sprout } from "lucide-react";
import type { ReactNode } from "react";

export function FooterNote({ children }: { children?: ReactNode }) {
  return (
    <p className="flex items-center justify-center gap-1.5 py-2 text-xs text-ink-500">
      <Sprout className="w-3.5 h-3.5 text-brand-500" />
      {children ?? "You don't need to learn it all today. Just keep growing."}
    </p>
  );
}

/** Small uppercase label above page titles. */
export function Eyebrow({ children }: { children: ReactNode }) {
  return (
    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-brand-600">{children}</p>
  );
}
