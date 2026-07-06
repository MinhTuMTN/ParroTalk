import Link from "next/link";
import { ArrowLeft, Plus, Volume2 } from "lucide-react";

export default function SampleWordPage() {
  return (
    <main className="min-h-full bg-[#f7fbf8] px-4 py-6 text-slate-900 sm:px-6 lg:px-8">
      <section className="mx-auto max-w-5xl space-y-6">
        <Link href="/dictionary" className="inline-flex items-center gap-2 text-sm font-black text-slate-500 hover:text-emerald-600">
          <ArrowLeft size={18} /> Back to Dictionary
        </Link>

        <section className="rounded-[2rem] border border-emerald-100 bg-white p-6 shadow-sm sm:p-8">
          <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <div className="flex flex-wrap gap-2">
                <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-black text-emerald-700">Oxford 3000</span>
                <span className="rounded-full bg-slate-50 px-3 py-1 text-xs font-black text-slate-600">A2</span>
                <span className="rounded-full bg-slate-50 px-3 py-1 text-xs font-black text-slate-600">noun</span>
              </div>
              <h1 className="mt-5 text-5xl font-black tracking-tight text-slate-950">journey</h1>
              <p className="mt-2 text-lg font-bold text-slate-400">/journey/</p>
            </div>
            <div className="flex gap-3">
              <button className="rounded-2xl bg-emerald-50 p-3 text-emerald-700"><Volume2 size={20} /></button>
              <button className="inline-flex items-center gap-2 rounded-2xl bg-slate-950 px-4 py-3 text-sm font-black text-white"><Plus size={18} /> Save</button>
            </div>
          </div>

          <div className="mt-7 rounded-3xl bg-emerald-50 p-5 ring-1 ring-emerald-100">
            <p className="text-xs font-black uppercase tracking-wide text-emerald-700">Main meaning</p>
            <h2 className="mt-2 text-2xl font-black text-emerald-900">chuyen di</h2>
            <p className="mt-3 font-semibold leading-7 text-slate-700">An act of travelling from one place to another.</p>
            <div className="mt-4 rounded-2xl bg-white p-4">
              <p className="font-bold text-slate-900">They went on a long journey across Europe.</p>
              <p className="mt-1 text-sm font-medium text-slate-500">Ho da co mot chuyen di dai xuyen chau Au.</p>
            </div>
          </div>
        </section>

        <section className="grid gap-6 lg:grid-cols-[1fr_300px]">
          <div className="rounded-[2rem] border border-slate-100 bg-white p-6 shadow-sm">
            <h2 className="text-xl font-black text-slate-950">Meanings</h2>
            <div className="mt-4 space-y-3">
              <article className="rounded-3xl border border-slate-100 p-4">
                <span className="rounded-full bg-slate-50 px-3 py-1 text-xs font-black text-slate-500">Sense 1</span>
                <h3 className="mt-3 text-lg font-black">chuyen di</h3>
                <p className="mt-2 text-sm font-semibold leading-6 text-slate-600">An act of travelling from one place to another.</p>
              </article>
              <article className="rounded-3xl border border-slate-100 p-4">
                <span className="rounded-full bg-slate-50 px-3 py-1 text-xs font-black text-slate-500">Sense 2</span>
                <h3 className="mt-3 text-lg font-black">hanh trinh phat trien</h3>
                <p className="mt-2 text-sm font-semibold leading-6 text-slate-600">A long process of personal change or development.</p>
              </article>
            </div>
          </div>

          <aside className="space-y-6">
            <div className="rounded-[2rem] border border-slate-100 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-black">Synonyms</h2>
              <div className="mt-3 flex flex-wrap gap-2">
                <span className="rounded-full bg-slate-50 px-3 py-1 text-xs font-black text-slate-600">trip</span>
                <span className="rounded-full bg-slate-50 px-3 py-1 text-xs font-black text-slate-600">travel</span>
                <span className="rounded-full bg-slate-50 px-3 py-1 text-xs font-black text-slate-600">voyage</span>
              </div>
            </div>
            <Link href="/dictionary/practice" className="inline-flex w-full justify-center rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white shadow-sm">Practice this word</Link>
          </aside>
        </section>
      </section>
    </main>
  );
}
