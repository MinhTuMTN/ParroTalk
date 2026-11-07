import Link from "next/link";
import { ArrowRight, BookOpen, Sparkles } from "lucide-react";
import { topicHref, type TopicWithProgress } from "../../lib/topicMeta";
import { LevelBadge, StatusBadge, TopicIcon } from "../shared";

const CTA: Record<TopicWithProgress["status"], string> = {
  "not-started": "Start learning",
  "in-progress": "Continue",
  completed: "Revisit",
};

function footerHint(t: TopicWithProgress) {
  if (t.status === "completed") return "All words learned";
  if (t.status === "in-progress") return `${t.progress}% · ${t.remaining} words to go`;
  return t.wordCount >= 40 ? "Popular with learners" : "A fresh place to start";
}

interface Props {
  topic: TopicWithProgress;
  index: number;
}

export function ExploreTopicCard({ topic, index }: Props) {
  const inProgress = topic.status === "in-progress";
  return (
    <Link
      href={topicHref(topic.id)}
      style={{ animationDelay: `${Math.min(index, 12) * 40}ms` }}
      className="group relative flex flex-col overflow-hidden rounded-2xl border border-ink-200/70 bg-white p-5 shadow-card transition-all duration-200 animate-fade-up hover:-translate-y-1 hover:border-brand-200 hover:shadow-card-hover"
    >
      {/* subtle progress line on top for in-progress topics */}
      {inProgress && (
        <span className="absolute inset-x-0 top-0 h-0.5 bg-ink-100">
          <span className="block h-full bg-gradient-to-r from-brand-400 to-brand-600" style={{ width: `${topic.progress}%` }} />
        </span>
      )}

      <div className="flex items-start justify-between">
        <TopicIcon Icon={topic.Icon} tint={topic.tint} />
        <StatusBadge status={topic.status} />
      </div>

      <div className="mt-4 text-[11px] font-semibold text-brand-600">{topic.category}</div>
      <h3 className="mt-0.5 text-[15px] font-bold leading-snug text-ink-900 transition-colors group-hover:text-brand-700">
        {topic.name}
      </h3>
      <p className="mt-1.5 line-clamp-2 text-xs leading-relaxed text-ink-500">
        {topic.description || "Learn practical words and phrases for this topic."}
      </p>

      <div className="mt-auto pt-4">
        <div className="flex items-center justify-between border-b border-ink-100 pb-3">
          <span className="inline-flex items-center gap-1.5 text-[11px] text-ink-500">
            <BookOpen className="h-3.5 w-3.5" />
            {topic.wordCount} words
          </span>
          <LevelBadge level={topic.level} />
        </div>
        <div className="flex items-center justify-between pt-3">
          <span className="inline-flex items-center gap-1 text-[11px] text-ink-400">
            {topic.status === "not-started" && topic.wordCount >= 40 && (
              <Sparkles className="h-3 w-3 text-coral-500" />
            )}
            {footerHint(topic)}
          </span>
          <span className="inline-flex items-center gap-1 text-xs font-bold text-brand-700">
            {CTA[topic.status]}
            <ArrowRight className="h-3.5 w-3.5 transition-transform group-hover:translate-x-1" />
          </span>
        </div>
      </div>
    </Link>
  );
}
