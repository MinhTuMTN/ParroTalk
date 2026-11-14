import { Pencil, Trash2 } from "lucide-react";
import { LessonStatus, type Lesson } from "@/features/lesson/types/lesson";

type LessonActionsProps = {
  lesson: Lesson;
  disabled?: boolean;
  onToggleStatus: (lesson: Lesson, next: boolean) => void;
  onEdit: (lesson: Lesson) => void;
  onDelete: (lesson: Lesson) => void;
};

export default function LessonActions({
  lesson,
  disabled = false,
  onToggleStatus,
  onEdit,
  onDelete,
}: LessonActionsProps) {
  const isPublished = lesson.status === LessonStatus.PUBLISHED;

  return (
    <div className="inline-flex items-center justify-end gap-2">
      <button
        type="button"
        role="switch"
        aria-checked={isPublished}
        aria-label={`Toggle visibility for "${lesson.title}" (currently ${isPublished ? "Published" : "Hidden"})`}
        title={isPublished ? "Click to hide lesson" : "Click to publish lesson"}
        disabled={disabled}
        onClick={() => onToggleStatus(lesson, !isPublished)}
        className={`relative inline-flex h-7 w-12 shrink-0 cursor-pointer items-center rounded-full transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60 ${
          isPublished ? "bg-emerald-600" : "bg-slate-300"
        }`}
      >
        <span
          className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-xs transition-transform ${
            isPublished ? "translate-x-6" : "translate-x-1"
          }`}
        />
      </button>

      <button
        type="button"
        disabled={disabled}
        onClick={() => onEdit(lesson)}
        className="inline-flex min-h-[36px] min-w-[36px] cursor-pointer items-center justify-center rounded-xl text-slate-500 transition hover:bg-indigo-50 hover:text-indigo-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 disabled:cursor-not-allowed disabled:opacity-50"
        aria-label={`Edit "${lesson.title}"`}
        title={`Edit "${lesson.title}"`}
      >
        <Pencil className="h-4 w-4" aria-hidden="true" />
      </button>

      <button
        type="button"
        disabled={disabled}
        onClick={() => onDelete(lesson)}
        className="inline-flex min-h-[36px] min-w-[36px] cursor-pointer items-center justify-center rounded-xl text-slate-500 transition hover:bg-rose-50 hover:text-rose-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 disabled:cursor-not-allowed disabled:opacity-50"
        aria-label={`Delete "${lesson.title}"`}
        title={`Delete "${lesson.title}"`}
      >
        <Trash2 className="h-4 w-4" aria-hidden="true" />
      </button>
    </div>
  );
}
