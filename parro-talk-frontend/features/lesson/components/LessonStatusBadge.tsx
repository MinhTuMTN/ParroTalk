import Badge from "@/components/ui/Badge";
import { LessonStatus } from "@/features/lesson/types/lesson";

type LessonStatusBadgeProps = {
  status: LessonStatus;
};

export default function LessonStatusBadge({ status }: LessonStatusBadgeProps) {
  return status === LessonStatus.PUBLISHED ? (
    <Badge tone="success">Published</Badge>
  ) : (
    <Badge tone="neutral">Hidden</Badge>
  );
}

