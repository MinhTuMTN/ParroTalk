import { stats } from "./constants";

export function SocialProofSection() {
  return (
    <section className="border-y border-emerald-900/5 bg-white py-12">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 gap-4 text-center sm:gap-6 md:grid-cols-4">
          {stats.map((stat) => (
            <div
              key={stat.label}
              className="soft-card flex flex-col items-center justify-center rounded-2xl p-5 border border-emerald-100/60 bg-emerald-50/20 transition hover:border-emerald-300"
            >
              <div className="text-3xl sm:text-4xl font-black text-emerald-700 tracking-tight">
                {stat.value}
              </div>
              <div className="mt-2 text-xs sm:text-sm font-bold tracking-normal text-slate-600">
                {stat.label}
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
