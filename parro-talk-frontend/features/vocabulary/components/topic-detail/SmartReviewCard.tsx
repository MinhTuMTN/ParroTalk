import React from "react";
import Link from "next/link";
import { Sparkles, Sliders, ArrowRight, Clock } from "lucide-react";
import type { TopicWithProgress } from "../../lib/topicMeta";

interface Props {
  topic: TopicWithProgress;
}

export function SmartReviewCard({ topic }: Props) {
  const topicName = encodeURIComponent(topic.name);
  const wordsNeedingPractice = Math.max(4, Math.round(topic.learned * 0.45));

  return (
    <section className="space-y-3 animate-fade-up">
      {/* Header */}
      <div className="flex items-center gap-2">
        <Sparkles className="w-4 h-4 text-brand-600" />
        <h2 className="text-lg font-bold text-ink-900 tracking-tight">Smart Practice</h2>
      </div>
      <p className="text-xs text-ink-500 -mt-1">
        Not sure what to choose? Let ParroTalk find the right mix for you.
      </p>

      {/* Card */}
      <div className="rounded-2xl border border-brand-200/80 bg-white p-5 sm:p-6 shadow-card hover:shadow-card-hover transition-all">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-5">
          <div className="flex items-start sm:items-center gap-4">
            <div className="w-12 h-12 rounded-2xl bg-brand-50 text-brand-600 border border-brand-100 flex items-center justify-center shrink-0">
              <Sliders className="w-6 h-6" />
            </div>

            <div className="space-y-1">
              <h3 className="font-bold text-base text-ink-900">Smart Review</h3>
              <p className="text-xs text-ink-600 max-w-md leading-relaxed">
                <span className="font-semibold text-brand-700">{wordsNeedingPractice} words</span> need more practice. Let&apos;s give them a little attention.
              </p>
              <div className="flex items-center gap-3 text-[11px] text-ink-400 pt-0.5">
                <span className="inline-flex items-center gap-1 font-medium">
                  <Clock className="w-3.5 h-3.5 text-brand-600" />
                  ~8 min
                </span>
                <span className="text-ink-300">•</span>
                <span>Automatically mixed exercises</span>
              </div>
            </div>
          </div>

          <div className="flex flex-col sm:items-end justify-center gap-1.5 shrink-0 pt-3 sm:pt-0 border-t sm:border-t-0 border-ink-100">
            <Link
              href={`/vocabulary/practice?topic=${topicName}&mode=smart-review`}
              className="group inline-flex items-center justify-center gap-2 rounded-xl border border-brand-200 bg-brand-50/60 hover:bg-brand-600 hover:text-white hover:border-brand-600 px-5 py-2.5 text-xs font-bold text-brand-700 transition-all shadow-sm"
            >
              <span>Start Smart Practice</span>
              <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-1" />
            </Link>
            <span className="text-[11px] text-ink-400 text-left sm:text-right">
              Available · Tailored to your words
            </span>
          </div>
        </div>
      </div>
    </section>
  );
}
