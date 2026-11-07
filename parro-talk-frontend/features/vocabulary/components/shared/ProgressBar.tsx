interface ProgressBarProps {
  pct: number;
  /** "soft" sits on tinted backgrounds, "default" on white. */
  variant?: "default" | "soft";
  className?: string;
}

export function ProgressBar({ pct, variant = "default", className = "" }: ProgressBarProps) {
  const track = variant === "soft" ? "bg-white/70" : "bg-ink-100";
  const value = Math.min(100, Math.max(0, pct));
  return (
    <div
      role="progressbar"
      aria-valuenow={value}
      aria-valuemin={0}
      aria-valuemax={100}
      className={`h-1.5 w-full overflow-hidden rounded-full ${track} ${className}`}
    >
      <div
        className="h-full rounded-full bg-gradient-to-r from-brand-400 to-brand-600 transition-[width] duration-700 ease-out"
        style={{ width: `${value}%` }}
      />
    </div>
  );
}
