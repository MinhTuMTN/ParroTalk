"use client";

import { useEffect, useState } from "react";
import { Flame } from "lucide-react";
import { Eyebrow } from "../shared";

function useGreeting() {
  const [greeting, setGreeting] = useState("Hello");
  useEffect(() => {
    const h = new Date().getHours();
    setGreeting(h < 12 ? "Good morning" : h < 18 ? "Good afternoon" : "Good evening");
  }, []);
  return greeting;
}

export function StreakBadge({ days }: { days: number }) {
  return (
    <div className="flex items-center gap-3 self-start rounded-2xl border border-coral-100 bg-gradient-to-br from-coral-50 to-white px-4 py-2.5 shadow-card">
      <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-coral-500 to-coral-600 text-white shadow-[0_6px_14px_-6px_rgba(216,90,43,0.7)]">
        <Flame className="h-4.5 w-4.5" />
      </div>
      <div>
        <div className="text-sm font-bold leading-tight text-ink-900">{days}-day streak</div>
        <div className="text-[11px] text-coral-600">You&apos;re building a lovely habit.</div>
      </div>
    </div>
  );
}

export function DashboardHeader({ name, streak }: { name?: string; streak: number }) {
  const greeting = useGreeting();
  return (
    <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start animate-fade-up">
      <div>
        <Eyebrow>Your vocabulary journey</Eyebrow>
        <h1 className="mt-2 text-[28px] font-extrabold leading-tight tracking-tight text-ink-900 sm:text-[34px]">
          {greeting}
          {name && <span className="text-brand-600">, {name}</span>}.
        </h1>
        <p className="mt-1 text-sm text-ink-500">
          A few new words. A little more confidence. Let&apos;s keep going.
        </p>
      </div>
      <StreakBadge days={streak} />
    </header>
  );
}
