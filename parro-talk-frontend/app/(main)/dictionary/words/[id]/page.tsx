export default function WordDetailPage() {
  return (
    <main className="min-h-full bg-[#f7fbf8] px-4 py-5 text-slate-900 sm:px-6 lg:px-8 lg:py-8">
      <section className="mx-auto max-w-5xl rounded-[2rem] border border-emerald-100 bg-white p-6 shadow-sm">
        <p className="text-sm font-black uppercase tracking-wide text-emerald-700">Word detail</p>
        <h1 className="mt-3 text-5xl font-black tracking-tight text-slate-950">journey</h1>
        <p className="mt-2 text-lg font-bold text-slate-400">/journey/</p>
        <div className="mt-6 rounded-3xl bg-emerald-50 p-5">
          <h2 className="text-2xl font-black text-emerald-900">chuyen di</h2>
          <p className="mt-3 font-semibold leading-7 text-slate-700">An act of travelling from one place to another.</p>
        </div>
      </section>
    </main>
  );
}
