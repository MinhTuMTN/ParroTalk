import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { CEFR_LABELS, topicHref, type TopicWithProgress } from "../../lib/topicMeta";
import { ProgressBar, SectionHeader, TopicIcon } from "../shared";

function ContinueTopicCard({ topic }: { topic: TopicWithProgress }) {
  return (
    <div className="group relative overflow-hidden rounded-2xl border border-brand-200/70 bg-gradient-to-br from-brand-50 to-brand-100/60 p-5 shadow-card transition-all duration-200 hover:-translate-y-0.5 hover:shadow-card-hover">
      <div className="pointer-events-none absolute -right-12 -top-12 h-40 w-40 rounded-full bg-white/60 blur-2xl" />

      <div className="relative flex items-start gap-3">
        <div className="rounded-xl bg-white shadow-sm">
          <TopicIcon Icon={topic.Icon} tint={topic.tint} size="sm" />
        </div>
        <div className="min-w-0">
          <h3 className="truncate text-[15px] font-bold text-ink-900">{topic.name}</h3>
          <p className="truncate text-xs text-ink-500">{topic.description || "Keep the momentum going."}</p>
        </div>
      </div>

      <div className="relative mt-5 flex items-end gap-4">
        <div className="min-w-0 flex-1">
          <div className="mb-1.5 flex justify-between text-[11px]">
            <span className="text-ink-600">
              {topic.learned} of {topic.wordCount} words learned
            </span>
            <span className="font-bold text-brand-700">{topic.progress}%</span>
          </div>
          <ProgressBar pct={topic.progress} variant="soft" />
          <div className="mt-1.5 text-[11px] text-ink-400">
            {topic.remaining} words remaining · {topic.level} {CEFR_LABELS[topic.level]}
          </div>
        </div>
        <Link
          href={topicHref(topic.id)}
          className="inline-flex shrink-0 items-center gap-1.5 rounded-xl bg-gradient-to-b from-brand-500 to-brand-600 px-4 py-2 text-xs font-semibold text-white shadow-brand transition-all hover:to-brand-700 active:scale-[0.97]"
        >
          Continue
          <ArrowRight className="h-3.5 w-3.5 transition-transform group-hover:translate-x-0.5" />
        </Link>
      </div>
    </div>
  );
}

export function ContinueLearningStrip({ topics }: { topics: TopicWithProgress[] }) {
  if (topics.length === 0) return null;
  return (
    <section className="animate-fade-up [animation-delay:120ms]">
      <SectionHeader
        title="Continue Learning"
        badge={`${topics.length} topic${topics.length > 1 ? "s" : ""}`}
        right="A little more familiar every day"
      />
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {topics.map((t) => (
          <ContinueTopicCard key={t.id} topic={t} />
        ))}
      </div>
    </section>
  );
}
