import Link from "next/link";
import { ArrowRight, CheckCheck, ChevronRight } from "lucide-react";
import { topicHref, type TopicWithProgress } from "../../lib/topicMeta";
import { ProgressBar, SectionHeader, TopicIcon } from "../shared";

interface Props {
  topics: TopicWithProgress[];
  loading: boolean;
}

function MyTopicRow({ topic }: { topic: TopicWithProgress }) {
  const done = topic.status === "completed";
  return (
    <Link
      href={topicHref(topic.id)}
      className="group flex items-center gap-4 rounded-2xl border border-ink-200/70 bg-white px-4 py-3.5 shadow-card transition-all duration-200 hover:-translate-y-0.5 hover:border-brand-200 hover:shadow-card-hover"
    >
      <TopicIcon Icon={topic.Icon} tint={topic.tint} />
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <span className="truncate text-sm font-semibold text-ink-900">{topic.name}</span>
          {topic.isCurrent && (
            <span className="shrink-0 rounded-md bg-brand-100 px-1.5 py-px text-[10px] font-semibold text-brand-700">
              Current
            </span>
          )}
        </div>
        <div className="mt-0.5 text-[11px] text-ink-400">
          {topic.learned} of {topic.wordCount} words learned
          {done && " · Topic complete"}
        </div>
        <ProgressBar pct={topic.progress} className="mt-2" />
      </div>
      <div className="w-10 text-right text-xs font-bold text-brand-600">
        {done ? <CheckCheck className="ml-auto h-4 w-4" /> : `${topic.progress}%`}
      </div>
      <ChevronRight className="h-4 w-4 text-ink-300 transition-all group-hover:translate-x-0.5 group-hover:text-brand-600" />
    </Link>
  );
}

export function MyTopicsList({ topics, loading }: Props) {
  return (
    <section className="animate-fade-up [animation-delay:240ms]">
      <SectionHeader
        title="My Topics"
        right={
          <Link
            href="/vocabulary/topics"
            className="group inline-flex items-center gap-1 font-semibold text-brand-600 hover:text-brand-700"
          >
            View all topics
            <ArrowRight className="h-3.5 w-3.5 transition-transform group-hover:translate-x-0.5" />
          </Link>
        }
      />
      <div className="space-y-2.5">
        {loading ? (
          Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-[76px] animate-pulse rounded-2xl border border-ink-200/70 bg-white" />
          ))
        ) : topics.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-ink-200 bg-white p-8 text-center text-sm text-ink-400">
            No topics yet. Start learning to see them here.
          </div>
        ) : (
          topics.map((t) => <MyTopicRow key={t.id} topic={t} />)
        )}
      </div>
    </section>
  );
}
