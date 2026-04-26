import Badge from "@/src/components/ui/Badge";
import type { LessonStatus } from "@/src/types/lesson";

type LessonStatusBadgeProps = {
  status: LessonStatus;
};

export default function LessonStatusBadge({ status }: LessonStatusBadgeProps) {
  return status === "published" ? (
    <Badge tone="success">Published</Badge>
  ) : (
    <Badge tone="neutral">Hidden</Badge>
  );
}
