"use client";

import React from "react";
import { Link2, MessageSquare, Compass, Sparkles } from "lucide-react";
import type { WordSenseItem } from "./types";

interface Props {
  senses: WordSenseItem[];
  hasPhrasalVerbs?: boolean;
  hasIdioms?: boolean;
}

export function WordDetailSidebar({
  senses,
  hasPhrasalVerbs = false,
  hasIdioms = false,
}: Props) {
  const scrollTo = (id: string) => {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  };

  return (
    <aside className="space-y-4 sticky top-6">
      <div className="rounded-3xl border border-ink-200/80 bg-white p-5 sm:p-6 shadow-card space-y-4">
        <h3 className="text-xs font-bold text-ink-700 uppercase tracking-wider">
          In this word
        </h3>

        <nav className="space-y-1.5 text-xs">
          {/* Sense links */}
          {senses.map((s) => (
            <button
              key={s.id}
              type="button"
              onClick={() => scrollTo(`sense-${s.orderNumber}`)}
              className="w-full text-left flex items-start gap-2.5 p-2 rounded-xl text-ink-600 hover:text-brand-800 hover:bg-brand-50/70 transition-all font-medium group"
            >
              <span className="font-bold text-brand-700 text-[11px] shrink-0 pt-0.5">
                {s.orderNumber}
              </span>
              <span className="line-clamp-1 leading-snug group-hover:underline">
                {s.title}
              </span>
            </button>
          ))}

          {/* Phrasal verbs */}
          {hasPhrasalVerbs && (
            <button
              type="button"
              onClick={() => scrollTo("phrasal-verbs")}
              className="w-full text-left flex items-center gap-2.5 p-2 rounded-xl text-ink-600 hover:text-brand-800 hover:bg-brand-50/70 transition-all font-medium group"
            >
              <Link2 className="w-3.5 h-3.5 text-brand-600 shrink-0" />
              <span className="group-hover:underline">Phrasal verbs</span>
            </button>
          )}

          {/* Idioms */}
          {hasIdioms && (
            <button
              type="button"
              onClick={() => scrollTo("idioms-expressions")}
              className="w-full text-left flex items-center gap-2.5 p-2 rounded-xl text-ink-600 hover:text-brand-800 hover:bg-brand-50/70 transition-all font-medium group"
            >
              <MessageSquare className="w-3.5 h-3.5 text-brand-600 shrink-0" />
              <span className="group-hover:underline">Idioms &amp; expressions</span>
            </button>
          )}
        </nav>

        {/* Callout box */}
        <div className="rounded-2xl border border-brand-200/60 bg-brand-50/50 p-4 space-y-1.5 text-xs pt-3">
          <div className="font-bold text-brand-800">
            Same word. Different worlds.
          </div>
          <p className="text-[11px] text-ink-500 leading-relaxed">
            Start with the meaning that fits your conversation. Open its usage to see words that belong together.
          </p>
        </div>
      </div>
    </aside>
  );
}
