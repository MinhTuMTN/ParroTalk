import Link from "next/link";
import { ArrowRight, BookOpen } from "lucide-react";
import { CEFR_LABELS, topicHref, type TopicWithProgress } from "../../lib/topicMeta";
import { ProgressBar } from "../shared";
import { TravelIllustration } from "./TravelIllustration";

interface Props {
  topic?: TopicWithProgress;
  loading: boolean;
}

export function ContinueLearningHero({ topic, loading }: Props) {
  return (
    <section className="relative flex flex-col overflow-hidden rounded-3xl border border-brand-200/70 bg-gradient-to-br from-brand-50 via-brand-100/60 to-brand-200/50 p-6 shadow-card sm:p-7 lg:col-span-2 animate-fade-up [animation-delay:60ms]">
      {/* ambient glow */}
      <div className="pointer-events-none absolute -right-16 -top-16 h-64 w-64 rounded-full bg-white/60 blur-3xl" />
      <div className="pointer-events-none absolute -bottom-20 left-1/3 h-48 w-48 rounded-full bg-brand-300/30 blur-3xl" />

      <div className="relative flex gap-4">
        <div className="min-w-0 flex-1">
          <div className="inline-flex items-center gap-1.5 rounded-full bg-white/70 px-2.5 py-1 text-xs font-semibold text-brand-700 ring-1 ring-brand-200/60 backdrop-blur">
            <BookOpen className="h-3.5 w-3.5" />
            Continue Learning
          </div>

          <h2 className="mt-4 text-2xl font-extrabold tracking-tight text-ink-900 sm:text-[26px]">
            {loading ? (
              <span className="inline-block h-7 w-56 animate-pulse rounded-md bg-white/70" />
            ) : (
              topic?.name ?? "Pick your first topic"
            )}
          </h2>
          <p className="mt-2 max-w-xs text-sm text-ink-600">
            {topic?.description || "Feel at home, wherever you go. Pick up where you left off."}
          </p>

          {topic && (
            <div className="mt-3 flex items-center gap-2 text-xs">
              <span className="font-semibold text-brand-700">
                {topic.level} · {CEFR_LABELS[topic.level]}
              </span>
              <span className="text-ink-300">•</span>
              <span className="text-ink-500">{topic.remaining} words to go</span>
            </div>
          )}

          <Link
            href={topic ? topicHref(topic.id) : "/vocabulary/topics"}
            className="group mt-5 inline-flex items-center gap-2 rounded-xl bg-gradient-to-b from-brand-500 to-brand-600 px-5 py-2.5 text-sm font-semibold text-white shadow-brand transition-all hover:-translate-y-0.5 hover:from-brand-500 hover:to-brand-700 active:translate-y-0"
          >
            Continue learning
            <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
          </Link>
        </div>

        <div className="-mr-2 -mt-2 hidden h-44 w-56 shrink-0 sm:block animate-float">
          <TravelIllustration />
        </div>
      </div>

      <div className="relative mt-6 border-t border-brand-200/70 pt-4">
        <div className="mb-2 flex items-center justify-between text-xs">
          <span className="text-ink-600">
            {topic?.learned ?? 0} of {topic?.wordCount ?? 0} words learned
          </span>
          <span className="font-bold text-brand-700">{topic?.progress ?? 0}% complete</span>
        </div>
        <ProgressBar pct={topic?.progress ?? 0} variant="soft" className="h-2" />
      </div>
    </section>
  );
}
