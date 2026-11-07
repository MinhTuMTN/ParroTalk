import Link from "next/link";
import { Compass, Plus } from "lucide-react";
import { topicHref, type TopicWithProgress } from "../../lib/topicMeta";
import { SectionHeader, TopicIcon } from "../shared";

interface Props {
  topics: TopicWithProgress[];
  loading: boolean;
}

export function RecommendedTopics({ topics, loading }: Props) {
  return (
    <section className="animate-fade-up [animation-delay:300ms]">
      <SectionHeader
        title="Recommended Topics"
        right={
          <Link href="/vocabulary/topics" aria-label="Explore all topics" className="text-brand-600 hover:text-brand-700">
            <Compass className="h-4 w-4" />
          </Link>
        }
      />
      <div className="divide-y divide-ink-100 rounded-2xl border border-ink-200/70 bg-white px-4 shadow-card">
        {loading ? (
          Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="my-3 h-14 animate-pulse rounded-lg bg-ink-50" />
          ))
        ) : topics.length === 0 ? (
          <div className="py-10 text-center text-sm text-ink-400">You&apos;ve explored every topic. 🎉</div>
        ) : (
          topics.map((topic) => (
            <Link key={topic.id} href={topicHref(topic.id)} className="group flex items-center gap-4 py-4">
              <TopicIcon Icon={topic.Icon} tint={topic.tint} />
              <div className="min-w-0 flex-1">
                <div className="truncate text-sm font-semibold text-ink-900 transition-colors group-hover:text-brand-700">
                  {topic.name}
                </div>
                <div className="mt-0.5 truncate text-[11px] text-ink-500">
                  {topic.description || "Discover new words for everyday life."}
                </div>
                <div className="mt-1 text-[11px] font-semibold text-brand-600">
                  {topic.wordCount} words · {topic.level}
                </div>
              </div>
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-ink-200 text-brand-600 transition-all group-hover:rotate-90 group-hover:border-brand-600 group-hover:bg-brand-600 group-hover:text-white">
                <Plus className="h-4 w-4" />
              </span>
            </Link>
          ))
        )}
      </div>
    </section>
  );
}
