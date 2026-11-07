import type { LucideIcon } from "lucide-react";
import { TINT_CLASSES, type Tint } from "../../lib/topicMeta";

interface TopicIconProps {
  Icon: LucideIcon;
  tint: Tint;
  size?: "sm" | "md";
}

export function TopicIcon({ Icon, tint, size = "md" }: TopicIconProps) {
  const box = size === "sm" ? "w-10 h-10 rounded-xl" : "w-11 h-11 rounded-xl";
  return (
    <div className={`${box} ${TINT_CLASSES[tint]} ring-1 ring-inset flex items-center justify-center shrink-0`}>
      <Icon className="w-5 h-5" strokeWidth={1.75} />
    </div>
  );
}
