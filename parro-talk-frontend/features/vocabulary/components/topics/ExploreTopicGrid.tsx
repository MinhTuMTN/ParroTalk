import { SearchX } from "lucide-react";
import type { TopicWithProgress } from "../../lib/topicMeta";
import { ExploreTopicCard } from "./ExploreTopicCard";

interface Props {
  topics: TopicWithProgress[];
  loading: boolean;
  onReset: () => void;
}

export function ExploreTopicGrid({ topics, loading, onReset }: Props) {
  if (loading) {
    return (
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className="h-[230px] animate-pulse rounded-2xl border border-ink-200/70 bg-white" />
        ))}
      </div>
    );
  }

  if (topics.length === 0) {
    return (
      <div className="flex flex-col items-center rounded-2xl border border-dashed border-ink-200 bg-white px-6 py-14 text-center">
        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-50 text-brand-600">
          <SearchX className="h-5 w-5" />
        </div>
        <h3 className="mt-4 text-sm font-bold text-ink-900">No topics match your filters</h3>
        <p className="mt-1 text-xs text-ink-500">Try a different keyword or clear the filters.</p>
        <button
          type="button"
          onClick={onReset}
          className="mt-4 rounded-xl border border-brand-200 bg-brand-50 px-4 py-2 text-xs font-semibold text-brand-700 transition-colors hover:bg-brand-100"
        >
          Clear filters
        </button>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {topics.map((t, i) => (
        <ExploreTopicCard key={t.id} topic={t} index={i} />
      ))}
    </div>
  );
}
