import { formatCompact, formatFull } from "./format";

/** Compact number (K/M) with the full value on hover/long-press and for screen readers. */
export default function Num({ value }: { value: number | null | undefined }) {
  return (
    <span title={formatFull(value)} aria-label={formatFull(value)}>
      {formatCompact(value)}
    </span>
  );
}
