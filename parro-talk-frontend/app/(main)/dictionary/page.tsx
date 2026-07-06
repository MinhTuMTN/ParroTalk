"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { ArrowRight, BookOpen, Brain, Clock3, Search, Sparkles, Volume2 } from "lucide-react";
import { categories, cefrLevels, dictionaryWords, wordSets } from "@/features/dictionary/mockData";

function CefrBadge({ level }: { level: string }) {
  return <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-700 ring-1 ring-emerald-100">{level}</span>;
}

export default function DictionaryPage() {
  const [query, setQuery] = useState("");
  const filteredWords = useMemo(() => {
    const value = query.trim().toLowerCase();
    if (!value) return dictionaryWords;
    return dictionaryWords.filter((word) =>
      [word.lemma, word.translationVi, word.definitionEn, word.category].some((field) => field.toLowerCase().includes(value)),
    );
  }, [query]);

  return (
    <main className="min-h-full bg-[#f7fbf8] px-4 py-5 text-slate-900 sm:px-6 lg:px-8 lg:py-8">
      <section className="mx-auto flex max-w-7xl flex-col gap-6">
        <div className="rounded-[2rem] border border-emerald-100 bg-white p-5 shadow-sm sm:p-7">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <div className="mb-3 inline-flex items-center gap-2 rounded-full bg-emerald-50 px-3 py-1 text-xs font-bold text-emerald-700">
                <BookOpen size={14} /> Dictionary
              </div>
              <h1 className="text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">Build vocabulary from real listening.</h1>
              <p className="mt-3 max-w-2xl text-sm font-medium leading-6 text-slate-500 sm:text-base">
                Search any English word, explore Oxford word sets, and practice vocabulary with listening-first exercises.
              </p>
            </div>
            <Link href="/dictionary/practice" className="inline-flex items-center justify-center gap-2 rounded-2xl bg-emerald-500 px-5 py-3 text-sm font-black text-white shadow-lg shadow-emerald-100 transition hover:bg-emerald-600 active:scale-95">
              Start Practice <ArrowRight size={18} />
            </Link>
          </div>

          <div className="mt-6 rounded-3xl border border-slate-100 bg-slate-50 p-2 sm:flex sm:items-center">
            <div className="flex flex-1 items-center gap-3 px-3 py-2">
              <Search className="text-slate-400" size={20} />
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Search any English word..."
                className="w-full bg-transparent text-base font-bold outline-none placeholder:text-slate-400"
              />
            </div>
            <button className="mt-2 w-full rounded-2xl bg-slate-950 px-5 py-3 text-sm font-black text-white sm:mt-0 sm:w-auto">Search</button>
          </div>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
          {wordSets.map((set) => (
            <Link key={set.id} href={`/dictionary/sets/${set.id}`} className="group rounded-[1.75rem] border border-slate-100 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:border-emerald-200 hover:shadow-md">
              <div className="flex items-center justify-between gap-3">
                <div className="rounded-2xl bg-emerald-50 p-3 text-emerald-600"><Sparkles size={22} /></div>
                <ArrowRight className="text-slate-300 transition group-hover:text-emerald-500" size={20} />
              </div>
              <h2 className="mt-4 text-xl font-black text-slate-950">{set.name}</h2>
              <p className="mt-2 min-h-10 text-sm font-medium leading-5 text-slate-500">{set.description}</p>
              <div className="mt-4 flex flex-wrap gap-2">
                {set.levels.map((level) => <CefrBadge key={level} level={level} />)}
              </div>
              <div className="mt-5 grid grid-cols-3 gap-2 text-center">
                <div className="rounded-2xl bg-slate-50 p-3"><p className="text-sm font-black">{set.totalWords}</p><p className="text-[11px] font-bold text-slate-400">words</p></div>
                <div className="rounded-2xl bg-slate-50 p-3"><p className="text-sm font-black">{set.learned}</p><p className="text-[11px] font-bold text-slate-400">learned</p></div>
                <div className="rounded-2xl bg-orange-50 p-3"><p className="text-sm font-black text-orange-600">{set.dueToday}</p><p className="text-[11px] font-bold text-orange-500">due</p></div>
              </div>
            </Link>
          ))}
        </div>

        <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
          <aside className="rounded-[1.75rem] border border-slate-100 bg-white p-5 shadow-sm lg:sticky lg:top-6 lg:self-start">
            <h3 className="text-sm font-black uppercase tracking-wide text-slate-400">Filters</h3>
            <div className="mt-4">
              <p className="mb-2 text-sm font-black text-slate-700">CEFR level</p>
              <div className="flex flex-wrap gap-2">{cefrLevels.map((level) => <button key={level} className="rounded-full border border-slate-100 px-3 py-1.5 text-xs font-black text-slate-600 hover:border-emerald-200 hover:text-emerald-600">{level}</button>)}</div>
            </div>
            <div className="mt-5">
              <p className="mb-2 text-sm font-black text-slate-700">Category</p>
              <div className="flex flex-wrap gap-2">{categories.slice(0, 6).map((category) => <button key={category} className="rounded-full bg-slate-50 px-3 py-1.5 text-xs font-bold text-slate-500 hover:bg-emerald-50 hover:text-emerald-700">{category}</button>)}</div>
            </div>
          </aside>

          <section>
            <div className="mb-4 flex items-center justify-between gap-3">
              <div>
                <h2 className="text-xl font-black text-slate-950">Recommended words</h2>
                <p className="text-sm font-medium text-slate-500">Mock data now, API-ready later.</p>
              </div>
              <Clock3 className="text-slate-300" />
            </div>
            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
              {filteredWords.map((word) => (
                <Link key={word.id} href={`/dictionary/words/${word.id}`} className="rounded-[1.5rem] border border-slate-100 bg-white p-5 shadow-sm transition hover:border-emerald-200 hover:shadow-md">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <h3 className="text-2xl font-black text-slate-950">{word.lemma}</h3>
                      <p className="mt-1 text-sm font-bold text-slate-400">{word.partOfSpeech} · {word.phonetic}</p>
                    </div>
                    <button className="rounded-2xl bg-emerald-50 p-2 text-emerald-600"><Volume2 size={18} /></button>
                  </div>
                  <p className="mt-4 text-lg font-black text-emerald-700">{word.translationVi}</p>
                  <p className="mt-2 line-clamp-2 text-sm font-medium leading-6 text-slate-500">{word.definitionEn}</p>
                  <div className="mt-4 flex items-center justify-between"><CefrBadge level={word.cefrLevel} /><span className="rounded-full bg-slate-50 px-3 py-1 text-xs font-bold text-slate-500">{word.category}</span></div>
                </Link>
              ))}
            </div>
            {filteredWords.length === 0 && <div className="rounded-[1.5rem] border border-dashed border-slate-200 bg-white p-8 text-center"><Brain className="mx-auto text-slate-300" /><p className="mt-3 font-bold text-slate-500">Search for another English word to see definitions, Vietnamese translation and examples.</p></div>}
          </section>
        </div>
      </section>
    </main>
  );
}
