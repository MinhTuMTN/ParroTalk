import { BookOpen } from "lucide-react";
import { Eyebrow } from "../shared";

export function TopicsHeader() {
  return (
    <header className="flex items-start justify-between gap-4 animate-fade-up">
      <div>
        <Eyebrow>Words for your world</Eyebrow>
        <h1 className="mt-2 text-[28px] font-extrabold leading-tight tracking-tight text-ink-900 sm:text-[34px]">
          Vocabulary Topics
        </h1>
        <p className="mt-1 text-sm text-ink-500">
          Explore what interests you. Learn the words you&apos;ll actually use.
        </p>
      </div>

      <div className="relative hidden sm:block">
        <div className="flex h-16 w-16 rotate-6 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-100 to-brand-200 text-brand-700 shadow-card transition-transform hover:rotate-0">
          <BookOpen className="h-7 w-7" strokeWidth={1.75} />
        </div>
        <span className="absolute -right-1 -top-1 h-3 w-3 rounded-full bg-coral-500 ring-4 ring-ink-50" />
      </div>
    </header>
  );
}
