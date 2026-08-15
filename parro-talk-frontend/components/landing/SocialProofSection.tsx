import { stats } from "./constants";

export function SocialProofSection() {
  return (
    <section className="border-y border-slate-200/80 bg-white py-10">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 gap-6 text-center divide-y sm:divide-y-0 sm:divide-x divide-slate-100 md:grid-cols-4">
          {stats.map((stat, index) => (
            <div key={stat.label} className={index > 1 ? "pt-4 sm:pt-0" : ""}>
              <div className="text-2xl sm:text-3xl font-extrabold text-emerald-700 tracking-tight">
                {stat.value}
              </div>
              <div className="mt-1 text-xs sm:text-sm font-bold uppercase tracking-wider text-slate-500">
                {stat.label}
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
