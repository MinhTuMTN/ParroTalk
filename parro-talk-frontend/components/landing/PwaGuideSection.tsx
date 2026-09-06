import { Zap } from "lucide-react";
import Image from "next/image";
import { pwaSteps } from "./constants";

export function PwaGuideSection() {
  return (
    <section
      id="pwa-guide"
      className="py-20 px-4 sm:px-6 lg:px-8 bg-white scroll-mt-20 border-b border-emerald-900/5"
    >
      <div className="mx-auto max-w-7xl">
        <div className="mx-auto max-w-2xl text-center mb-16">
          <div className="mb-3 inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-3.5 py-1 text-xs font-bold text-emerald-800 border border-emerald-200/60">
            <Zap size={14} className="text-amber-500 fill-amber-500" /> Tiện lợi
            & Mượt mà
          </div>
          <h2 className="text-3xl font-extrabold tracking-tight text-slate-950 sm:text-4xl">
            Trải nghiệm mượt mà hơn với ứng dụng ParroTalk
          </h2>
          <p className="mt-3 text-base leading-relaxed text-slate-600 sm:text-lg">
            Cài đặt ParroTalk vào màn hình chính để luyện nghe mọi lúc, mọi nơi
            mà không cần mở trình duyệt.
          </p>
        </div>

        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {pwaSteps.map((step) => (
            <div
              key={step.step}
              className="soft-card flex flex-col items-center rounded-3xl border border-slate-200/80 bg-slate-50/50 p-5 text-center transition-all duration-300 hover:border-emerald-300 hover:bg-white hover:shadow-xl hover:-translate-y-1"
            >
              <div className="mb-4 aspect-square w-full overflow-hidden rounded-2xl border border-slate-200/70 bg-white shadow-inner flex items-center justify-center p-3">
                <Image
                  src={step.image}
                  alt={step.alt}
                  width={240}
                  height={240}
                  className="h-full w-full object-contain rounded-xl"
                  loading="lazy"
                />
              </div>
              <span className="mb-1.5 rounded-full bg-emerald-100 px-3 py-0.5 text-xs font-extrabold text-emerald-800">
                {step.step}
              </span>
              <h4 className="mt-1 text-base font-extrabold text-slate-900">
                {step.title}
              </h4>
              <p className="mt-2 text-xs leading-relaxed text-slate-600">
                {step.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
