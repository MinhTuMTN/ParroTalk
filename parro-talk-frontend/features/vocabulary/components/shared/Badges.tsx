import { Check } from "lucide-react";
import { CEFR_LABELS, type CefrLevel, type TopicStatus } from "../../lib/topicMeta";

const STATUS_STYLES: Record<TopicStatus, { label: string; className: string }> = {
  "not-started": { label: "Not started", className: "bg-ink-100 text-ink-500" },
  "in-progress": { label: "In progress", className: "bg-honey-50 text-honey-600 ring-1 ring-inset ring-honey-100" },
  completed: { label: "Completed", className: "bg-brand-50 text-brand-700 ring-1 ring-inset ring-brand-200" },
};

export function StatusBadge({ status }: { status: TopicStatus }) {
  const s = STATUS_STYLES[status];
  return (
    <span className={`inline-flex items-center gap-1 rounded-md px-2 py-1 text-[10px] font-semibold ${s.className}`}>
      {status === "completed" && <Check className="w-3 h-3" strokeWidth={2.5} />}
      {status === "in-progress" && <span className="w-1.5 h-1.5 rounded-full bg-honey-500 animate-pulse" />}
      {s.label}
    </span>
  );
}

export function LevelBadge({ level }: { level: CefrLevel }) {
  return (
    <span className="rounded-md bg-brand-50 px-2 py-0.5 text-[10px] font-semibold text-brand-700">
      {level} · {CEFR_LABELS[level]}
    </span>
  );
}
