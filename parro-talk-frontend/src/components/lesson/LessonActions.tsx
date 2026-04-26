import { Pencil, Trash2 } from "lucide-react";
import Switch from "@/src/components/ui/Switch";
import type { Lesson } from "@/src/types/lesson";

type LessonActionsProps = {
  lesson: Lesson;
  onToggleStatus: (lesson: Lesson, next: boolean) => void;
  onEdit: (lesson: Lesson) => void;
  onDelete: (lesson: Lesson) => void;
};

export default function LessonActions({
  lesson,
  onToggleStatus,
  onEdit,
  onDelete,
}: LessonActionsProps) {
  return (
    <div className="flex items-center gap-3">
      <Switch
        checked={lesson.status === "published"}
        onChange={(next) => onToggleStatus(lesson, next)}
      />
      <button
        onClick={() => onEdit(lesson)}
        className="text-slate-600 hover:text-slate-900"
        aria-label="Edit lesson"
      >
        <Pencil className="h-4 w-4" />
      </button>
      <button
        onClick={() => onDelete(lesson)}
        className="text-slate-600 hover:text-rose-600"
        aria-label="Delete lesson"
      >
        <Trash2 className="h-4 w-4" />
      </button>
    </div>
  );
}
