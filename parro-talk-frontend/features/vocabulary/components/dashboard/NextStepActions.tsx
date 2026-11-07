import Link from "next/link";
import type { LucideIcon } from "lucide-react";
import { ArrowRight, Layers, Plus, RefreshCw } from "lucide-react";
import { SectionHeader } from "../shared";

const ACTIONS: { href: string; icon: LucideIcon; chip: string; title: string; desc: string }[] = [
  { href: "/vocabulary/topics", icon: Plus, chip: "bg-brand-100 text-brand-600", title: "Learn New Words", desc: "Find a topic that speaks to you." },
  { href: "/vocabulary/review", icon: RefreshCw, chip: "bg-honey-100 text-honey-600", title: "Review Words", desc: "Make what you've learned stick." },
  { href: "/vocabulary/saved", icon: Layers, chip: "bg-ocean-100 text-ocean-600", title: "My Words", desc: "Your growing word collection." },
];

export function NextStepActions() {
  return (
    <section className="animate-fade-up [animation-delay:360ms]">
      <SectionHeader title="Make it your next step" />
      <div className="grid grid-cols-1 divide-y divide-ink-100 overflow-hidden rounded-2xl border border-ink-200/70 bg-white shadow-card sm:grid-cols-3 sm:divide-x sm:divide-y-0">
        {ACTIONS.map((a) => (
          <Link
            key={a.title}
            href={a.href}
            className="group flex items-center gap-3 px-5 py-4 transition-colors hover:bg-brand-50/60"
          >
            <span className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-xl ${a.chip} transition-transform group-hover:scale-105`}>
              <a.icon className="h-4 w-4" />
            </span>
            <div className="min-w-0 flex-1">
              <div className="text-sm font-semibold text-ink-900">{a.title}</div>
              <div className="text-[11px] text-ink-500">{a.desc}</div>
            </div>
            <ArrowRight className="h-4 w-4 text-ink-300 transition-all group-hover:translate-x-0.5 group-hover:text-brand-600" />
          </Link>
        ))}
      </div>
    </section>
  );
}
