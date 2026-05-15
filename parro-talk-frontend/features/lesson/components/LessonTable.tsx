import LessonRow from "@/features/lesson/components/LessonRow";
import type { Lesson } from "@/features/lesson/types/lesson";

type LessonTableProps = {
  lessons: Lesson[];
  selectedIds: string[];
  onSelectAll: (checked: boolean) => void;
  onSelect: (id: string, checked: boolean) => void;
  onToggleStatus: (lesson: Lesson, next: boolean) => void;
  onEdit: (lesson: Lesson) => void;
  onDelete: (lesson: Lesson) => void;
};

export default function LessonTable({
  lessons,
  selectedIds,
  onSelectAll,
  onSelect,
  onToggleStatus,
  onEdit,
  onDelete,
}: LessonTableProps) {
  const allSelected =
    lessons.length > 0 && lessons.every((lesson) => selectedIds.includes(lesson.id));

  return (
    <div className="overflow-x-auto rounded-[26px] border border-slate-200 bg-white shadow-sm">
      <table className="w-full min-w-[900px]">
        <thead className="bg-slate-50 text-left text-sm text-slate-600">
          <tr>
            <th className="px-4 py-4">
              <input
                type="checkbox"
                checked={allSelected}
                onChange={(event) => onSelectAll(event.target.checked)}
              />
            </th>
            <th className="px-4 py-4">Title</th>
            <th className="px-4 py-4">Source</th>
            <th className="px-4 py-4">Duration</th>
            <th className="px-4 py-4">Status</th>
            <th className="px-4 py-4">Created Date</th>
            <th className="px-4 py-4">Actions</th>
          </tr>
        </thead>
        <tbody>
          {lessons.map((lesson) => (
            <LessonRow
              key={lesson.id}
              lesson={lesson}
              selected={selectedIds.includes(lesson.id)}
              onSelect={onSelect}
              onToggleStatus={onToggleStatus}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
}

