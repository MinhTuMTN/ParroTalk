const compact = new Intl.NumberFormat("en", {
  notation: "compact",
  maximumFractionDigits: 1,
});
const full = new Intl.NumberFormat("en");

export const formatCompact = (n: number | null | undefined) =>
  compact.format(n ?? 0);
export const formatFull = (n: number | null | undefined) =>
  full.format(n ?? 0);

export function formatDateTime(iso: string | null): string {
  if (!iso) return "—";
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString("vi-VN");
}

export function formatShortDate(iso: string): string {
  const d = new Date(`${iso}T00:00:00`);
  return Number.isNaN(d.getTime())
    ? iso
    : d.toLocaleDateString("en", { month: "short", day: "numeric" });
}

/** Colorblind-safe pair (blue / orange), also differentiated by label order. */
export const INPUT_COLOR = "#2563EB";
export const OUTPUT_COLOR = "#D97706";
