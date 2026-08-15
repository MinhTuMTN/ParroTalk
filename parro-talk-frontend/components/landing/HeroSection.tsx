import { ArrowRight, Play } from "lucide-react";
import Link from "next/link";
import { AudioPlaygroundMockup } from "./AudioPlaygroundMockup";

export function HeroSection() {
  return (
    <section className="relative overflow-hidden px-4 pb-16 pt-16 sm:px-6 lg:px-8 lg:pb-24 lg:pt-20">
      <div
        className="pointer-events-none absolute inset-0 -z-10 opacity-35"
        style={{
          backgroundImage:
            "radial-gradient(circle at 50% 0%, #85f8c4 0%, transparent 55%), radial-gradient(circle at 80% 30%, #fed7aa 0%, transparent 40%)",
        }}
      />
      <div className="mx-auto max-w-7xl text-center">
        <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-slate-200/80 bg-white/90 px-4 py-1.5 text-xs font-bold text-slate-700 shadow-sm backdrop-blur-md">
          <span className="text-amber-500">🔥</span> Khám phá phương pháp luyện
          nghe tương tác mới
        </div>

        <h1 className="mx-auto max-w-4xl text-4xl font-extrabold tracking-tight text-slate-950 sm:text-5xl lg:text-6xl leading-[1.15]">
          Luyện Nghe Tương Tác <br className="hidden md:block" />
          <span className="text-emerald-700">
            Phản Xạ Tiếng Anh Tự Nhiên
          </span>
        </h1>

        <p className="mx-auto mt-6 max-w-2xl text-base leading-relaxed text-slate-600 sm:text-lg">
          Biến việc học thành trải nghiệm thú vị. Điều chỉnh tốc độ linh hoạt,
          lặp lại thông minh và nhận gợi ý độ chính xác (heat map) ngay khi bạn
          đang gõ.
        </p>

        <div className="mb-14 mt-8 flex flex-col items-center justify-center gap-3.5 sm:flex-row">
          <Link
            href="/library"
            className="btn-press inline-flex w-full min-h-13 items-center justify-center gap-2 rounded-xl border-b-[3px] border-amber-600 bg-amber-500 px-8 py-3.5 text-sm font-bold text-white shadow-lg shadow-amber-500/20 transition hover:brightness-105 sm:w-auto"
          >
            Bắt đầu luyện tập <ArrowRight size={18} />
          </Link>
          <a
            href="#demo-widget"
            className="inline-flex w-full min-h-13 items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-8 py-3.5 text-sm font-bold text-slate-700 shadow-sm transition hover:bg-slate-50 hover:text-emerald-700 sm:w-auto"
          >
            <Play size={16} className="fill-current text-emerald-600" />
            Xem Demo
          </a>
        </div>

        <AudioPlaygroundMockup />
      </div>
    </section>
  );
}
