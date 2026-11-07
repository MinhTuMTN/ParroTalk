import type { ReactNode } from "react";
import type { LucideIcon } from "lucide-react";
import { BookOpen, CheckCheck, Sprout, TrendingUp } from "lucide-react";
import { ProgressBar, SectionHeader } from "../shared";

interface Props {
  totalLearned: number;
  learning: number;
  mastered: number;
  weeklyGain: number;
  wordGoal: number;
}

function Stat({
  icon: Icon,
  chip,
  label,
  children,
}: {
  icon: LucideIcon;
  chip: string;
  label: string;
  children: ReactNode;
}) {
  return (
    <div className="p-5">
      <div className="flex items-center gap-2 text-xs font-medium text-ink-500">
        <span className={`flex h-6 w-6 items-center justify-center rounded-lg ${chip}`}>
          <Icon className="h-3.5 w-3.5" />
        </span>
        {label}
      </div>
      {children}
    </div>
  );
}

export function ProgressOverview({ totalLearned, learning, mastered, weeklyGain, wordGoal }: Props) {
  const pct = Math.round((totalLearned / wordGoal) * 100);
  return (
    <section className="animate-fade-up [animation-delay:180ms]">
      <SectionHeader title="Your progress, at a glance" right="Every word is a step forward." />
      <div className="grid grid-cols-2 divide-ink-100 overflow-hidden rounded-2xl border border-ink-200/70 bg-white shadow-card lg:grid-cols-4 lg:divide-x">
        <Stat icon={BookOpen} chip="bg-brand-100 text-brand-600" label="Total words learned">
          <div className="mt-3 text-[28px] font-extrabold leading-none text-ink-900">{totalLearned}</div>
          <div className="mt-2 inline-flex rounded-full bg-brand-50 px-2 py-0.5 text-[11px] font-semibold text-brand-700">
            +{weeklyGain} this week
          </div>
        </Stat>
        <Stat icon={Sprout} chip="bg-honey-100 text-honey-600" label="Learning">
          <div className="mt-3 text-[28px] font-extrabold leading-none text-ink-900">{learning}</div>
          <div className="mt-2 text-[11px] text-ink-400">Growing more familiar</div>
        </Stat>
        <Stat icon={CheckCheck} chip="bg-ocean-100 text-ocean-600" label="Mastered">
          <div className="mt-3 text-[28px] font-extrabold leading-none text-ink-900">{mastered}</div>
          <div className="mt-2 text-[11px] text-ink-400">Words you know by heart</div>
        </Stat>
        <Stat icon={TrendingUp} chip="bg-brand-600 text-white" label="Overall progress">
          <div className="mt-3 bg-gradient-to-r from-brand-500 to-brand-700 bg-clip-text text-[28px] font-extrabold leading-none text-transparent">
            {pct}%
          </div>
          <ProgressBar pct={pct} className="mt-3" />
          <div className="mt-2 text-[11px] text-ink-400">
            {totalLearned} of your {wordGoal}-word goal
          </div>
        </Stat>
      </div>
    </section>
  );
}
