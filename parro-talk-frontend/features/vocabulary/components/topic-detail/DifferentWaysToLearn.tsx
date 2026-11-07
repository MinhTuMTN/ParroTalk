"use client";

import React from "react";
import Link from "next/link";
import {
  Layers,
  Target,
  Keyboard,
  Puzzle,
  Headphones,
  Mic,
  Check,
  ArrowRight,
  RotateCcw,
  BookOpen,
  Clock,
} from "lucide-react";
import type { TopicWithProgress } from "../../lib/topicMeta";
import {
  useTopicModeProgress,
  type ModeProgressItem,
} from "../../hooks/useTopicModeProgress";

interface Props {
  topic: TopicWithProgress;
}

function ModeActionButton({
  item,
  topicName,
  isComingSoon = false,
}: {
  item: ModeProgressItem;
  topicName: string;
  isComingSoon?: boolean;
}) {
  if (isComingSoon) {
    return (
      <button
        type="button"
        disabled
        className="inline-flex items-center gap-1 rounded-xl border border-ink-200 bg-ink-50 px-3.5 py-1.5 text-xs font-semibold text-ink-400 cursor-not-allowed select-none"
      >
        <span>Coming Soon</span>
      </button>
    );
  }

  const href = `/vocabulary/practice?topic=${topicName}&mode=${item.id}`;

  if (item.status === "in-progress") {
    return (
      <Link
        href={href}
        className="group inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-b from-brand-500 to-brand-600 hover:from-brand-600 hover:to-brand-700 active:scale-[0.98] px-4 py-1.5 text-xs font-bold text-white shadow-sm shadow-brand/20 transition-all"
      >
        <span>Continue</span>
        <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-1" />
      </Link>
    );
  }

  if (item.status === "completed") {
    return (
      <Link
        href={href}
        className="group inline-flex items-center gap-1.5 rounded-xl border border-brand-200 bg-brand-50 hover:bg-brand-100 hover:border-brand-300 px-4 py-1.5 text-xs font-bold text-brand-700 transition-all shadow-sm"
      >
        <RotateCcw className="w-3 h-3 text-brand-600 transition-transform group-hover:-rotate-45" />
        <span>Review</span>
      </Link>
    );
  }

  // Not started
  return (
    <Link
      href={href}
      className="group inline-flex items-center gap-1.5 rounded-xl border border-ink-200 bg-white hover:bg-brand-50 hover:border-brand-300 hover:text-brand-700 px-4 py-1.5 text-xs font-bold text-ink-800 transition-all shadow-sm"
    >
      <span>Start</span>
      <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-1" />
    </Link>
  );
}

function ModeStatusBadge({
  item,
  isComingSoon = false,
}: {
  item: ModeProgressItem;
  isComingSoon?: boolean;
}) {
  if (isComingSoon) {
    return (
      <span className="text-[10px] font-semibold text-honey-700 bg-honey-50 border border-honey-200 rounded-md px-2 py-0.5">
        Coming soon
      </span>
    );
  }

  if (item.status === "completed") {
    return (
      <span className="text-[10px] font-semibold text-brand-700 bg-brand-50 border border-brand-200 rounded-md px-2 py-0.5 inline-flex items-center gap-1">
        <Check className="w-3 h-3 text-brand-600" strokeWidth={2.5} />
        Completed
      </span>
    );
  }

  if (item.status === "in-progress") {
    return (
      <span className="text-[10px] font-semibold text-brand-700 bg-brand-50 border border-brand-200 rounded-md px-2 py-0.5 inline-flex items-center gap-1.5">
        <span className="w-1.5 h-1.5 rounded-full bg-brand-500 animate-pulse" />
        In progress ({item.completed}/{item.total})
      </span>
    );
  }

  return (
    <span className="text-[10px] font-semibold text-ink-500 bg-ink-100 rounded-md px-2 py-0.5">
      Not started
    </span>
  );
}

function ModeMiniProgress({ item }: { item: ModeProgressItem }) {
  if (item.status === "not-started") return null;

  return (
    <div className="space-y-1 pt-1">
      <div className="flex items-center justify-between text-[10px] font-medium text-ink-400">
        <span>Progress</span>
        <span className="font-semibold text-brand-700">{item.percent}%</span>
      </div>
      <div className="h-1.5 w-full bg-ink-100 rounded-full overflow-hidden">
        <div
          className="h-full bg-brand-500 rounded-full transition-all duration-500 ease-out"
          style={{ width: `${item.percent}%` }}
        />
      </div>
    </div>
  );
}

export function DifferentWaysToLearn({ topic }: Props) {
  const topicName = encodeURIComponent(topic.name);
  const { progressMap } = useTopicModeProgress(topic.name || topic.id, topic.wordCount);

  const flashcard = progressMap["flashcard"];
  const multipleChoice = progressMap["multiple-choice"];
  const typeWord = progressMap["type-word"];
  const fillBlank = progressMap["fill-blank"];
  const listenType = progressMap["listen-type"];
  const pronunciation = progressMap["pronunciation"];

  return (
    <section className="space-y-6 animate-fade-up">
      {/* Section Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-xl sm:text-2xl font-extrabold text-ink-900 tracking-tight">
            Different ways. The same words.
          </h2>
          <p className="text-xs sm:text-sm text-ink-500 mt-0.5">
            Find a rhythm that works for you — you don&apos;t have to follow an order.
          </p>
        </div>

        <div className="self-start sm:self-auto inline-flex items-center gap-1.5 rounded-full border border-ink-200 bg-white px-3.5 py-1 text-xs font-semibold text-ink-600 shadow-sm">
          <span>Learn</span>
          <span className="text-ink-300">→</span>
          <span>Practice</span>
          <span className="text-ink-300">→</span>
          <span>Apply</span>
        </div>
      </div>

      <div className="space-y-6">
        {/* ── 01 Learn ── */}
        <div className="grid grid-cols-1 md:grid-cols-12 gap-4 items-start">
          <div className="md:col-span-3 space-y-1">
            <div className="inline-flex items-center gap-2">
              <span className="text-[11px] font-bold text-brand-700 bg-brand-50 border border-brand-100 rounded-md px-1.5 py-0.5">
                01
              </span>
              <span className="text-sm font-bold text-ink-900">Learn</span>
            </div>
            <p className="text-xs text-ink-500">Get to know your new words.</p>
          </div>

          <div className="md:col-span-9">
            <div className="group rounded-2xl border border-ink-200/80 bg-white p-5 shadow-card hover:border-brand-300 hover:shadow-card-hover transition-all">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-start gap-4">
                  <div className="w-11 h-11 rounded-xl bg-brand-50 text-brand-600 border border-brand-100 flex items-center justify-center shrink-0">
                    <Layers className="w-5 h-5" />
                  </div>
                  <div className="space-y-1.5">
                    <div className="flex items-center gap-2">
                      <h3 className="font-bold text-base text-ink-900 group-hover:text-brand-700 transition-colors">
                        Learn Words
                      </h3>
                    </div>
                    <p className="text-xs text-ink-500 max-w-lg leading-relaxed">
                      Discover each word, its meaning, pronunciation, and an example — revealed one step at a time.
                    </p>
                    <div className="flex flex-wrap items-center gap-3 text-[11px] font-medium text-ink-500 pt-1">
                      <span className="inline-flex items-center gap-1">
                        <BookOpen className="w-3.5 h-3.5 text-brand-600" />
                        <span>{flashcard.total} words</span>
                      </span>
                      <span className="text-ink-300">•</span>
                      <span className="inline-flex items-center gap-1">
                        <Clock className="w-3.5 h-3.5 text-brand-600" />
                        <span>~10 min</span>
                      </span>
                      <span className="text-ink-300">•</span>
                      <span className="font-semibold text-brand-700">Recognition &amp; understanding</span>
                    </div>

                    {/* Progress indicator for Learn Words */}
                    <div className="max-w-xs pt-1">
                      <ModeMiniProgress item={flashcard} />
                    </div>
                  </div>
                </div>

                <div className="flex sm:flex-col items-center sm:items-end justify-between sm:justify-center gap-3 shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-ink-100">
                  <ModeStatusBadge item={flashcard} />
                  <ModeActionButton item={flashcard} topicName={topicName} />
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* ── 02 Practice ── */}
        <div className="grid grid-cols-1 md:grid-cols-12 gap-4 items-start pt-2">
          <div className="md:col-span-3 space-y-1">
            <div className="inline-flex items-center gap-2">
              <span className="text-[11px] font-bold text-brand-700 bg-brand-50 border border-brand-100 rounded-md px-1.5 py-0.5">
                02
              </span>
              <span className="text-sm font-bold text-ink-900">Practice</span>
            </div>
            <p className="text-xs text-ink-500">Make familiar words stick.</p>
          </div>

          <div className="md:col-span-9 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {/* Multiple Choice */}
            <div className="group rounded-2xl border border-ink-200/80 bg-white p-5 shadow-card hover:border-brand-300 hover:shadow-card-hover transition-all flex flex-col justify-between space-y-4">
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="w-10 h-10 rounded-xl bg-brand-50 text-brand-600 border border-brand-100 flex items-center justify-center">
                    <Target className="w-5 h-5" />
                  </div>
                  <ModeStatusBadge item={multipleChoice} />
                </div>
                <div>
                  <h3 className="font-bold text-sm text-ink-900 group-hover:text-brand-700 transition-colors">
                    Multiple Choice
                  </h3>
                  <p className="text-xs text-ink-500 mt-1 leading-relaxed">
                    Choose the correct meaning or word.
                  </p>
                </div>
                <div className="space-y-1 text-[11px] text-ink-500">
                  <div className="flex items-center gap-2">
                    <span className="inline-flex items-center gap-1">
                      <BookOpen className="w-3 h-3 text-brand-600" />
                      {multipleChoice.total} questions
                    </span>
                    <span className="text-ink-300">•</span>
                    <span className="inline-flex items-center gap-1">
                      <Clock className="w-3 h-3 text-brand-600" />
                      ~5 min
                    </span>
                  </div>
                  <div className="font-semibold text-brand-700">Vocabulary recall</div>
                </div>

                <ModeMiniProgress item={multipleChoice} />
              </div>

              <div className="flex items-center justify-between pt-2 border-t border-ink-100">
                <span className="text-[11px] text-ink-400">
                  {multipleChoice.status === "completed"
                    ? "Completed"
                    : multipleChoice.status === "in-progress"
                    ? `${multipleChoice.completed}/${multipleChoice.total} done`
                    : "Quick practice"}
                </span>
                <ModeActionButton item={multipleChoice} topicName={topicName} />
              </div>
            </div>

            {/* Type the Word */}
            <div className="group rounded-2xl border border-ink-200/80 bg-white p-5 shadow-card hover:border-brand-300 hover:shadow-card-hover transition-all flex flex-col justify-between space-y-4">
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="w-10 h-10 rounded-xl bg-honey-50 text-honey-600 border border-honey-100 flex items-center justify-center">
                    <Keyboard className="w-5 h-5" />
                  </div>
                  <ModeStatusBadge item={typeWord} />
                </div>
                <div>
                  <h3 className="font-bold text-sm text-ink-900 group-hover:text-brand-700 transition-colors">
                    Type the Word
                  </h3>
                  <p className="text-xs text-ink-500 mt-1 leading-relaxed">
                    Remember and type the target word from its definition alone.
                  </p>
                </div>
                <div className="space-y-1 text-[11px] text-ink-500">
                  <div className="flex items-center gap-2">
                    <span className="inline-flex items-center gap-1">
                      <BookOpen className="w-3 h-3 text-honey-600" />
                      {typeWord.total} words
                    </span>
                    <span className="text-ink-300">•</span>
                    <span className="inline-flex items-center gap-1">
                      <Clock className="w-3 h-3 text-honey-600" />
                      ~5 min
                    </span>
                  </div>
                  <div className="font-semibold text-honey-700">Active word recall</div>
                </div>

                <ModeMiniProgress item={typeWord} />
              </div>

              <div className="flex items-center justify-between pt-2 border-t border-ink-100">
                <span className="text-[11px] text-ink-400">
                  {typeWord.status === "completed"
                    ? "Completed"
                    : typeWord.status === "in-progress"
                    ? `${typeWord.completed}/${typeWord.total} done`
                    : "Definition recall"}
                </span>
                <ModeActionButton item={typeWord} topicName={topicName} />
              </div>
            </div>

            {/* Fill in the Blank */}
            <div className="group rounded-2xl border border-ink-200/80 bg-white p-5 shadow-card hover:border-brand-300 hover:shadow-card-hover transition-all flex flex-col justify-between space-y-4">
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="w-10 h-10 rounded-xl bg-ocean-50 text-ocean-600 border border-ocean-100 flex items-center justify-center">
                    <Puzzle className="w-5 h-5" />
                  </div>
                  <ModeStatusBadge item={fillBlank} />
                </div>
                <div>
                  <h3 className="font-bold text-sm text-ink-900 group-hover:text-brand-700 transition-colors">
                    Fill in the Blank
                  </h3>
                  <p className="text-xs text-ink-500 mt-1 leading-relaxed">
                    Analyze sentence context to determine and apply the right word form.
                  </p>
                </div>
                <div className="space-y-1 text-[11px] text-ink-500">
                  <div className="flex items-center gap-2">
                    <span className="inline-flex items-center gap-1">
                      <BookOpen className="w-3 h-3 text-ocean-600" />
                      {fillBlank.total} questions
                    </span>
                    <span className="text-ink-300">•</span>
                    <span className="inline-flex items-center gap-1">
                      <Clock className="w-3 h-3 text-ocean-600" />
                      ~5 min
                    </span>
                  </div>
                  <div className="font-semibold text-brand-700">Context &amp; usage</div>
                </div>

                <ModeMiniProgress item={fillBlank} />
              </div>

              <div className="flex items-center justify-between pt-2 border-t border-ink-100">
                <span className="text-[11px] text-ink-400">
                  {fillBlank.status === "completed"
                    ? "Completed"
                    : fillBlank.status === "in-progress"
                    ? `${fillBlank.completed}/${fillBlank.total} done`
                    : "Context practice"}
                </span>
                <ModeActionButton item={fillBlank} topicName={topicName} />
              </div>
            </div>
          </div>
        </div>

        {/* ── 03 Apply ── */}
        <div className="grid grid-cols-1 md:grid-cols-12 gap-4 items-start pt-2">
          <div className="md:col-span-3 space-y-1">
            <div className="inline-flex items-center gap-2">
              <span className="text-[11px] font-bold text-brand-700 bg-brand-50 border border-brand-100 rounded-md px-1.5 py-0.5">
                03
              </span>
              <span className="text-sm font-bold text-ink-900">Apply</span>
            </div>
            <p className="text-xs text-ink-500">Bring your words into real English.</p>
          </div>

          <div className="md:col-span-9 grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Listen & Type */}
            <div className="group rounded-2xl border border-ink-200/80 bg-white p-5 shadow-card hover:border-brand-300 hover:shadow-card-hover transition-all flex flex-col justify-between space-y-4">
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="w-10 h-10 rounded-xl bg-brand-50 text-brand-600 border border-brand-100 flex items-center justify-center">
                    <Headphones className="w-5 h-5" />
                  </div>
                  <ModeStatusBadge item={listenType} />
                </div>
                <div>
                  <h3 className="font-bold text-sm text-ink-900 group-hover:text-brand-700 transition-colors">
                    Listen &amp; Type
                  </h3>
                  <p className="text-xs text-ink-500 mt-1 leading-relaxed">
                    Listen to an English sentence and type what you hear.
                  </p>
                </div>
                <div className="space-y-1 text-[11px] text-ink-500">
                  <div className="flex items-center gap-2">
                    <span className="inline-flex items-center gap-1">
                      <BookOpen className="w-3 h-3 text-brand-600" />
                      {listenType.total} sentences
                    </span>
                    <span className="text-ink-300">•</span>
                    <span className="inline-flex items-center gap-1">
                      <Clock className="w-3 h-3 text-brand-600" />
                      ~10 min
                    </span>
                  </div>
                  <div className="font-semibold text-brand-700">Listening &amp; spelling</div>
                </div>

                <ModeMiniProgress item={listenType} />
              </div>

              <div className="flex items-center justify-between pt-2 border-t border-ink-100">
                <span className="text-[11px] text-ink-400">
                  {listenType.status === "completed"
                    ? "Completed"
                    : listenType.status === "in-progress"
                    ? `${listenType.completed}/${listenType.total} done`
                    : "Use words in context"}
                </span>
                <ModeActionButton item={listenType} topicName={topicName} />
              </div>
            </div>

            {/* Pronunciation (Speaking) - Coming Soon */}
            <div className="group rounded-2xl border border-ink-200/80 bg-white p-5 shadow-card flex flex-col justify-between space-y-4 opacity-95">
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="w-10 h-10 rounded-xl bg-ink-100 text-ink-600 border border-ink-200 flex items-center justify-center">
                    <Mic className="w-5 h-5" />
                  </div>
                  <ModeStatusBadge item={pronunciation} isComingSoon />
                </div>
                <div>
                  <h3 className="font-bold text-sm text-ink-900">
                    Pronunciation
                  </h3>
                  <p className="text-xs text-ink-500 mt-1 leading-relaxed">
                    Listen to the word, then practice saying it aloud.
                  </p>
                </div>
                <div className="space-y-1 text-[11px] text-ink-500">
                  <div className="flex items-center gap-2">
                    <span className="inline-flex items-center gap-1">
                      <BookOpen className="w-3 h-3 text-ink-400" />
                      {pronunciation.total} words
                    </span>
                    <span className="text-ink-300">•</span>
                    <span className="inline-flex items-center gap-1">
                      <Clock className="w-3 h-3 text-ink-400" />
                      ~5 min
                    </span>
                  </div>
                  <div className="font-semibold text-ink-600">Speaking &amp; pronunciation</div>
                </div>
              </div>

              <div className="flex items-center justify-between pt-2 border-t border-ink-100">
                <span className="text-[11px] text-ink-400">Microphone needed in a real session</span>
                <ModeActionButton item={pronunciation} topicName={topicName} isComingSoon />
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
