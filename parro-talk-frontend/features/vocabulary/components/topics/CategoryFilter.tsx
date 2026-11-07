import { LayoutGrid } from "lucide-react";
import { CATEGORIES } from "../../lib/topicMeta";
import type { CategoryFilter as CategoryValue } from "../../hooks/useTopicFilters";

interface Props {
  value: CategoryValue;
  onChange: (value: CategoryValue) => void;
}

const BASE =
  "inline-flex shrink-0 items-center gap-1.5 rounded-xl border px-3 py-2 text-xs font-semibold transition-all";
const ACTIVE = "border-brand-600 bg-brand-600 text-white shadow-brand";
const IDLE = "border-ink-200/80 bg-white text-ink-600 hover:border-brand-300 hover:text-brand-700";

export function CategoryFilter({ value, onChange }: Props) {
  return (
    <div role="group" aria-label="Topic categories" className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1 no-scrollbar">
      <button
        type="button"
        aria-pressed={value === "All"}
        onClick={() => onChange("All")}
        className={`${BASE} ${value === "All" ? ACTIVE : IDLE}`}
      >
        <LayoutGrid className="h-3.5 w-3.5" />
        All categories
      </button>
      {CATEGORIES.map(({ label, icon: Icon }) => (
        <button
          key={label}
          type="button"
          aria-pressed={value === label}
          onClick={() => onChange(label)}
          className={`${BASE} ${value === label ? ACTIVE : IDLE}`}
        >
          <Icon className="h-3.5 w-3.5" />
          {label}
        </button>
      ))}
    </div>
  );
}
