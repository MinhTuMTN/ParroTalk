import { ChevronDown } from "lucide-react";
import { CEFR_LABELS, CEFR_LEVELS } from "../../lib/topicMeta";
import type { LevelFilter, SortOption, StatusFilter } from "../../hooks/useTopicFilters";

const STATUS_TABS: { value: StatusFilter; label: string }[] = [
  { value: "all", label: "All topics" },
  { value: "not-started", label: "Not started" },
  { value: "in-progress", label: "In progress" },
  { value: "completed", label: "Completed" },
];

interface Props {
  status: StatusFilter;
  onStatusChange: (v: StatusFilter) => void;
  counts: Record<StatusFilter, number>;
  level: LevelFilter;
  onLevelChange: (v: LevelFilter) => void;
  sort: SortOption;
  onSortChange: (v: SortOption) => void;
}

function Select<T extends string>({
  id,
  value,
  onChange,
  options,
  label,
}: {
  id: string;
  value: T;
  onChange: (v: T) => void;
  options: { value: T; label: string }[];
  label: string;
}) {
  return (
    <div className="relative">
      <select
        id={id}
        aria-label={label}
        value={value}
        onChange={(e) => onChange(e.target.value as T)}
        className="cursor-pointer appearance-none rounded-xl border border-ink-200/80 bg-white py-2 pl-3 pr-8 text-xs font-medium text-ink-700 shadow-card outline-none transition-colors hover:border-brand-300 focus:border-brand-400 focus:ring-4 focus:ring-brand-100"
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      <ChevronDown className="pointer-events-none absolute right-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-ink-400" />
    </div>
  );
}

export function ExploreToolbar({ status, onStatusChange, counts, level, onLevelChange, sort, onSortChange }: Props) {
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div className="-mx-1 flex overflow-x-auto px-1 no-scrollbar">
        <div className="flex gap-1 rounded-xl bg-ink-100/80 p-1">
          {STATUS_TABS.map((tab) => {
            const active = status === tab.value;
            return (
              <button
                key={tab.value}
                type="button"
                aria-pressed={active}
                onClick={() => onStatusChange(tab.value)}
                className={`inline-flex shrink-0 items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition-all ${
                  active ? "bg-white text-brand-700 shadow-sm" : "text-ink-500 hover:text-ink-800"
                }`}
              >
                {tab.label}
                <span
                  className={`rounded-full px-1.5 text-[10px] ${
                    active ? "bg-brand-100 text-brand-700" : "bg-ink-200/70 text-ink-500"
                  }`}
                >
                  {counts[tab.value]}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      <div className="flex items-center gap-2">
        <Select<LevelFilter>
          id="topic-level"
          label="Filter by difficulty"
          value={level}
          onChange={onLevelChange}
          options={[
            { value: "all", label: "All difficulties" },
            ...CEFR_LEVELS.map((l) => ({ value: l, label: `${l} · ${CEFR_LABELS[l]}` })),
          ]}
        />
        <Select<SortOption>
          id="topic-sort"
          label="Sort topics"
          value={sort}
          onChange={onSortChange}
          options={[
            { value: "recommended", label: "Recommended" },
            { value: "az", label: "A – Z" },
            { value: "words", label: "Most words" },
          ]}
        />
      </div>
    </div>
  );
}
