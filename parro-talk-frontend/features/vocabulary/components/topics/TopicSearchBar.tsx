import { Search, X } from "lucide-react";

interface Props {
  value: string;
  onChange: (value: string) => void;
}

export function TopicSearchBar({ value, onChange }: Props) {
  return (
    <div className="group flex items-center gap-3 rounded-2xl border border-ink-200/80 bg-white px-4 py-3 shadow-card transition-all focus-within:border-brand-400 focus-within:ring-4 focus-within:ring-brand-100">
      <Search className="h-4.5 w-4.5 shrink-0 text-ink-400 transition-colors group-focus-within:text-brand-600" />
      <input
        id="topic-search"
        type="search"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Search topics…"
        aria-label="Search topics"
        className="min-w-0 flex-1 bg-transparent text-sm text-ink-900 placeholder:text-ink-400 outline-none [&::-webkit-search-cancel-button]:hidden"
      />
      {value ? (
        <button
          type="button"
          onClick={() => onChange("")}
          aria-label="Clear search"
          className="rounded-full p-1 text-ink-400 transition-colors hover:bg-ink-100 hover:text-ink-700"
        >
          <X className="h-3.5 w-3.5" />
        </button>
      ) : (
        <span className="hidden text-[11px] text-ink-400 sm:block">Find your next useful words</span>
      )}
    </div>
  );
}
