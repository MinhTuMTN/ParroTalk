import type { Lesson } from "@/features/lesson/types/lesson";
import LessonActions from "@/features/lesson/components/LessonActions";
import LessonStatusBadge from "@/features/lesson/components/LessonStatusBadge";

type LessonRowProps = {
  lesson: Lesson;
  selected: boolean;
  onSelect: (id: string, checked: boolean) => void;
  onToggleStatus: (lesson: Lesson, next: boolean) => void;
  onEdit: (lesson: Lesson) => void;
  onDelete: (lesson: Lesson) => void;
};

const formatDuration = (seconds: number) => {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
};

export default function LessonRow({
  lesson,
  selected,
  onSelect,
  onToggleStatus,
  onEdit,
  onDelete,
}: LessonRowProps) {
  return (
    <tr className="border-t border-slate-200 text-sm text-slate-700">
      <td className="px-4 py-5">
        <input
          type="checkbox"
          checked={selected}
          onChange={(event) => onSelect(lesson.id, event.target.checked)}
        />
      </td>
      <td className="px-4 py-5 font-semibold text-slate-900">{lesson.title}</td>
      <td className="max-w-56 truncate px-4 py-5 text-slate-600">{lesson.source}</td>
      <td className="px-4 py-5">{formatDuration(lesson.duration)}</td>
      <td className="px-4 py-5">
        <LessonStatusBadge status={lesson.status} />
      </td>
      <td className="px-4 py-5">{new Date(lesson.createdAt).toLocaleDateString()}</td>
      <td className="px-4 py-5">
        <LessonActions
          lesson={lesson}
          onToggleStatus={onToggleStatus}
          onEdit={onEdit}
          onDelete={onDelete}
        />
      </td>
    </tr>
  );
}

