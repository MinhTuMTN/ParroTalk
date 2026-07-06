import Link from "next/link";
import { ArrowLeft, BookOpen, Headphones, Plus, Volume2 } from "lucide-react";
import { getWordById } from "@/features/dictionary/mockData";

export default async function WordDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const word = getWordById(id);
  const mainSense = word.senses[0];

  return (
    <main className="min-h-full bg-[#f7fbf8] px-4 py-5 text-slate-900 sm:px-6 lg:px-8 lg:py-8">
      <section className="mx-auto max-w-6xl space-y-6">
        <Link href="/dictionary" className="inline-flex items-center gap-2 text-sm font-black text-slate-500 hover:text-emerald-600">
          <ArrowLeft size={18} /> Back to Dictionary
        </Link>

        <section className="rounded-[2rem] border border-emerald-100 bg-white p-6 shadow-sm sm:p-8">
          <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <div className="flex flex-wrap gap-2">
                <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-black text-emerald-700">Oxford word</span>
                <span className="rounded-full bg-slate-50 px-3 py-1 text-xs font-black text-slate-600">{word.cefrLevel}</span>
                <span className="rounded-full bg-slate-50 px-3 py-1 text-xs font-black text-slate-600">{word.partOfSpeech}</span>
                <span className="rounded-full bg-slate-50 px-3 py-1 text-xs font-black text-slate-600">{word.category}</span>
              </div>
              <h1 className="mt-5 text-5xl font-black tracking-tight text-slate-950 sm:text-6xl">{word.lemma}</h1>
              <p className="mt-2 text-lg font-bold text-slate-400">{word.phonetic}</p>
            </div>
            <div className="flex gap-3">
              <button className="rounded-2xl bg-emerald-50 p-3 text-emerald-700"><Volume2 size={20} /></button>
              <button className="inline-flex items-center gap-2 rounded-2xl bg-slate-950 px-4 py-3 text-sm font-black text-white"><Plus size={18} /> Save</button>
            </div>
          </div>

          <div className="mt-7 rounded-3xl bg-emerald-50 p-5 ring-1 ring-emerald-100">
            <p className="text-xs font-black uppercase tracking-wide text-emerald-700">Main meaning</p>
            <h2 className="mt-2 text-2xl font-black text-emerald-900">{word.translationVi}</h2>
            <p className="mt-3 font-semibold leading-7 text-slate-700">{word.definitionEn}</p>
            <div className="mt-4 rounded-2xl bg-white p-4">
              <p className="font-bold text-slate-900">{mainSense.exampleEn}</p>
              <p className="mt-1 text-sm font-medium text-slate-500">{mainSense.exampleVi}</p>
            </div>
          </div>
        </section>

        <section className="grid gap-6 lg:grid-cols-[1fr_300px]">
          <div className="space-y-6">
            <div className="rounded-[2rem] border border-slate-100 bg-white p-6 shadow-sm">
              <div className="flex items-center gap-2"><BookOpen className="text-emerald-600" size={22} /><h2 className="text-xl font-black text-slate-950">Meanings</h2></div>
              <div className="mt-4 space-y-3">
                {word.senses.map((sense, index) => (
                  <article key={sense.definitionEn} className="rounded-3xl border border-slate-100 p-4">
                    <span className="rounded-full bg-slate-50 px-3 py-1 text-xs font-black text-slate-500">Sense {index + 1}</span>
                    <h3 className="mt-3 text-lg font-black">{sense.translationVi}</h3>
                    <p className="mt-2 text-sm font-semibold leading-6 text-slate-600">{sense.definitionEn}</p>
                    <p className="mt-3 rounded-2xl bg-slate-50 p-4 text-sm font-bold text-slate-700">{sense.exampleEn}</p>
                  </article>
                ))}
              </div>
            </div>

            <div className="rounded-[2rem] border border-slate-100 bg-white p-6 shadow-sm">
              <div className="flex items-center gap-2"><Headphones className="text-emerald-600" size={22} /><h2 className="text-xl font-black text-slate-950">From ParroTalk lessons</h2></div>
              <div className="mt-5 rounded-3xl border border-dashed border-emerald-200 bg-emerald-50/60 p-4">
                <p className="text-xs font-black uppercase tracking-wide text-emerald-700">Dictation segment</p>
                <p className="mt-2 text-sm font-bold leading-6 text-slate-700">{mainSense.exampleEn}</p>
                <button className="mt-4 inline-flex items-center gap-2 rounded-2xl bg-white px-4 py-2 text-sm font-black text-emerald-700"><Headphones size={16} /> Practice with sentence</button>
              </div>
            </div>
          </div>

          <aside className="space-y-6 lg:sticky lg:top-6 lg:self-start">
            <div className="rounded-[2rem] border border-slate-100 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-black">Synonyms</h2>
              <div className="mt-3 flex flex-wrap gap-2">
                {word.synonyms.map((item) => <span key={item} className="rounded-full bg-slate-50 px-3 py-1 text-xs font-black text-slate-600">{item}</span>)}
              </div>
              <h2 className="mt-5 text-lg font-black">Antonyms</h2>
              <div className="mt-3 flex flex-wrap gap-2">
                {word.antonyms.length ? word.antonyms.map((item) => <span key={item} className="rounded-full bg-slate-50 px-3 py-1 text-xs font-black text-slate-600">{item}</span>) : <p className="text-sm font-bold text-slate-400">No common antonyms.</p>}
              </div>
            </div>

            <div className="rounded-[2rem] border border-slate-100 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-black">Review status</h2>
              <div className="mt-4 grid gap-3">
                <div className="flex justify-between rounded-2xl bg-slate-50 p-3"><span className="text-sm font-bold text-slate-500">Status</span><span className="text-sm font-black text-emerald-700">{word.status}</span></div>
                <div className="flex justify-between rounded-2xl bg-slate-50 p-3"><span className="text-sm font-bold text-slate-500">Due</span><span className="text-sm font-black text-orange-600">Today</span></div>
              </div>
            </div>

            <Link href="/dictionary/practice" className="inline-flex w-full justify-center rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white shadow-sm">Practice this word</Link>
          </aside>
        </section>
      </section>
    </main>
  );
}
