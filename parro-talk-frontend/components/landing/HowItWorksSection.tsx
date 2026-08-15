import { steps } from "./constants";

export function HowItWorksSection() {
  return (
    <section
      id="how-it-works"
      className="py-20 px-4 sm:px-6 lg:px-8 bg-slate-50/70 scroll-mt-20"
    >
      <div className="mx-auto max-w-7xl">
        <div className="mx-auto max-w-2xl text-center mb-16">
          <h2 className="text-3xl font-extrabold tracking-tight text-slate-950 sm:text-4xl">
            Quy trình 3 bước vàng
          </h2>
          <p className="mt-3 text-base leading-relaxed text-slate-600 sm:text-lg">
            Phương pháp Dictation được chứng minh giúp cải thiện kỹ năng nghe
            hiểu vượt bậc qua từng ngày.
          </p>
        </div>

        <div className="grid gap-8 md:grid-cols-3">
          {steps.map((item) => {
            const Icon = item.icon;
            return (
              <div
                key={item.title}
                className={`glass-card relative z-10 flex flex-col items-center rounded-3xl p-8 text-center transition-all duration-300 hover:-translate-y-1.5 ${item.rotation}`}
              >
                <div className="mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-800 shadow-sm">
                  <Icon size={32} />
                </div>
                <span className="mb-2 text-xs font-extrabold uppercase tracking-widest text-emerald-600">
                  {item.step}
                </span>
                <h3 className="mb-3 text-xl font-extrabold text-slate-900">
                  {item.title}
                </h3>
                <p className="text-sm leading-relaxed text-slate-600">
                  {item.description}
                </p>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
