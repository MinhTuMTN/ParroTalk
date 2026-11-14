import LessonRow from "@/features/lesson/components/LessonRow";
import type { Lesson } from "@/features/lesson/types/lesson";

type LessonTableProps = {
  lessons: Lesson[];
  selectedIds: string[];
  disabled?: boolean;
  onSelectAll: (checked: boolean) => void;
  onSelect: (id: string, checked: boolean) => void;
  onToggleStatus: (lesson: Lesson, next: boolean) => void;
  onEdit: (lesson: Lesson) => void;
  onDelete: (lesson: Lesson) => void;
};

export default function LessonTable({
  lessons,
  selectedIds,
  disabled = false,
  onSelectAll,
  onSelect,
  onToggleStatus,
  onEdit,
  onDelete,
}: LessonTableProps) {
  const allSelected =
    lessons.length > 0 &&
    lessons.every((lesson) => selectedIds.includes(lesson.id));

  return (
    <div
      tabIndex={0}
      role="region"
      aria-labelledby="lessons-table-caption"
      className="overflow-x-auto outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
    >
      <table className="w-full min-w-[960px] border-collapse text-left">
        <caption id="lessons-table-caption" className="sr-only">
          Admin lessons directory showing thumbnail, title, categories, media source, duration, visibility status, creation date, and row actions
        </caption>
        <thead>
          <tr className="border-b border-slate-100 bg-slate-50/80 text-xs font-bold uppercase tracking-wider text-slate-500">
            <th scope="col" className="w-12 py-3.5 pl-4 pr-2">
              <input
                type="checkbox"
                checked={allSelected}
                disabled={disabled || lessons.length === 0}
                aria-label="Select all lessons on current page"
                onChange={(event) => onSelectAll(event.target.checked)}
                className="h-4 w-4 cursor-pointer rounded border-slate-300 accent-emerald-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
              />
            </th>
            <th scope="col" className="py-3.5 px-3">
              Lesson
            </th>
            <th scope="col" className="py-3.5 px-3">
              Media &amp; Source
            </th>
            <th scope="col" className="py-3.5 px-3">
              Duration
            </th>
            <th scope="col" className="py-3.5 px-3">
              Status
            </th>
            <th scope="col" className="py-3.5 px-3">
              Created
            </th>
            <th scope="col" className="py-3.5 pl-3 pr-4 text-right">
              Actions
            </th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100 text-sm text-slate-700">
          {lessons.map((lesson) => (
            <LessonRow
              key={lesson.id}
              lesson={lesson}
              selected={selectedIds.includes(lesson.id)}
              disabled={disabled}
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
