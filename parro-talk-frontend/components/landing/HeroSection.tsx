import { ArrowRight, Play, Sparkles } from "lucide-react";
import Link from "next/link";
import { AudioPlaygroundMockup } from "./AudioPlaygroundMockup";
import { FeaturedClassCards } from "./FeaturedClassCards";

export function HeroSection() {
  return (
    <section className="relative overflow-hidden px-4 pb-16 pt-12 sm:px-6 lg:px-8 lg:pb-24 lg:pt-16">
      {/* Background Soft Gradients */}
      <div
        className="pointer-events-none absolute inset-0 -z-10 opacity-40"
        style={{
          backgroundImage:
            "radial-gradient(circle at 50% 0%, #a7f3d0 0%, transparent 60%), radial-gradient(circle at 85% 25%, #fed7aa 0%, transparent 45%), radial-gradient(circle at 15% 35%, #bbf7d0 0%, transparent 40%)",
        }}
      />

      <div className="mx-auto max-w-7xl text-center space-y-12">
        {/* Top Header & Intro */}
        <div className="mx-auto max-w-4xl space-y-6">
          <div className="inline-flex items-center gap-2 rounded-full border border-emerald-200/80 bg-white/90 px-4 py-1.5 text-xs font-bold text-slate-800 shadow-xs backdrop-blur-md">
            <span className="text-amber-500">🔥</span> Khám phá phương pháp luyện
            nghe tương tác mới
          </div>

          <h1 className="text-4xl font-extrabold tracking-tight text-slate-950 sm:text-5xl lg:text-6xl leading-[1.12]">
            Luyện Nghe Tương Tác <br className="hidden md:block" />
            <span className="text-emerald-700">
              Phản Xạ Tiếng Anh Tự Nhiên
            </span>
          </h1>

          <p className="mx-auto max-w-2xl text-base leading-relaxed text-slate-600 sm:text-lg">
            Biến việc học thành trải nghiệm thú vị. Điều chỉnh tốc độ linh hoạt,
            lặp lại thông minh và nhận gợi ý độ chính xác (heat map) ngay khi bạn
            đang gõ.
          </p>

          <div className="flex flex-col items-center justify-center gap-3.5 sm:flex-row pt-2">
            <Link
              href="/library"
              className="btn-press-3d inline-flex w-full min-h-13 items-center justify-center gap-2 rounded-2xl border-b-[3px] border-amber-600 bg-amber-500 px-8 py-3.5 text-sm font-extrabold text-white shadow-lg shadow-amber-500/20 hover:brightness-105 sm:w-auto"
            >
              Bắt đầu luyện tập <ArrowRight size={18} />
            </Link>
            <a
              href="#demo-widget"
              className="btn-press inline-flex w-full min-h-13 items-center justify-center gap-2 rounded-2xl border border-slate-200/80 bg-white px-8 py-3.5 text-sm font-extrabold text-slate-700 shadow-xs transition hover:bg-emerald-50/50 hover:text-emerald-800 hover:border-emerald-200 sm:w-auto"
            >
              <Play size={16} className="fill-current text-emerald-600" />
              Xem Demo
            </a>
          </div>
        </div>

        {/* Featured Classes Banner Strip (Inspired by Mobile Design) */}
        <div className="mx-auto max-w-5xl">
          <div className="flex items-center justify-between mb-4 px-1">
            <h2 className="text-left text-lg font-extrabold tracking-tight text-slate-900 flex items-center gap-2">
              <Sparkles size={18} className="text-emerald-600" />
              Bài học thịnh hành hôm nay
            </h2>
            <Link
              href="/library"
              className="text-xs font-bold text-emerald-700 hover:underline"
            >
              Xem tất cả →
            </Link>
          </div>
          <FeaturedClassCards />
        </div>

        {/* Interactive Audio Playground Widget */}
        <div className="pt-4">
          <AudioPlaygroundMockup />
        </div>
      </div>
    </section>
  );
}
