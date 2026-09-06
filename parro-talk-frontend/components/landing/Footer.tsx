import { Mail, Share2, Video } from "lucide-react";
import Image from "next/image";
import Link from "next/link";

export function Footer() {
  return (
    <footer
      id="footer"
      className="border-t border-slate-800 bg-slate-950 text-slate-300 py-16 px-4 sm:px-6 lg:px-8"
    >
      <div className="mx-auto max-w-7xl">
        <div className="grid gap-10 sm:grid-cols-2 md:grid-cols-4 mb-14">
          {/* Brand Info */}
          <div className="flex flex-col gap-4">
            <Link href="/" className="flex items-center gap-2.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-900/60 p-1 border border-emerald-500/30">
                <Image
                  src="/logo.png"
                  alt="ParroTalk Logo"
                  width={28}
                  height={28}
                  className="rounded-lg object-contain"
                />
              </div>
              <span className="text-xl font-black text-white tracking-tight">
                Parro<span className="text-emerald-400">Talk</span>
              </span>
            </Link>
            <p className="text-sm leading-relaxed text-slate-400">
              Nâng tầm kỹ năng nghe chép chính tả với AI. Rèn luyện đôi tai phản
              xạ tự nhiên qua từng câu video.
            </p>
            <div className="flex items-center gap-3 text-slate-400 mt-1">
              <Link
                href="/library"
                className="flex h-8 w-8 items-center justify-center rounded-lg bg-slate-900 transition hover:bg-emerald-950 hover:text-emerald-400"
                aria-label="Thư viện bài học"
              >
                <Video size={16} />
              </Link>
              <a
                href="mailto:info@parrotalk.fun"
                className="flex h-8 w-8 items-center justify-center rounded-lg bg-slate-900 transition hover:bg-emerald-950 hover:text-emerald-400"
                aria-label="Gửi email liên hệ"
              >
                <Mail size={16} />
              </a>
              <a
                href="#pwa-guide"
                className="flex h-8 w-8 items-center justify-center rounded-lg bg-slate-900 transition hover:bg-emerald-950 hover:text-emerald-400"
                aria-label="Hướng dẫn PWA"
              >
                <Share2 size={16} />
              </a>
            </div>
          </div>

          {/* Column 2: Sản phẩm */}
          <div className="flex flex-col gap-3.5">
            <h4 className="text-xs font-black uppercase tracking-wider text-white">
              Sản phẩm
            </h4>
            <nav className="flex flex-col gap-2.5 text-sm">
              <Link href="/library" className="text-slate-400 transition hover:text-white">
                Kho bài học
              </Link>
              <a href="#how-it-works" className="text-slate-400 transition hover:text-white">
                Phương pháp Dictation
              </a>
              <a href="#demo-widget" className="text-slate-400 transition hover:text-white">
                Luyện nghe tương tác
              </a>
              <a href="#pwa-guide" className="text-slate-400 transition hover:text-white">
                Cài đặt ứng dụng PWA
              </a>
            </nav>
          </div>

          {/* Column 3: Công ty */}
          <div className="flex flex-col gap-3.5">
            <h4 className="text-xs font-black uppercase tracking-wider text-white">
              Công ty
            </h4>
            <nav className="flex flex-col gap-2.5 text-sm">
              <a href="#footer" className="text-slate-400 transition hover:text-white">
                Về chúng tôi
              </a>
              <Link href="/library" className="text-slate-400 transition hover:text-white">
                Lộ trình học tập
              </Link>
              <a
                href="mailto:info@parrotalk.fun"
                className="text-slate-400 transition hover:text-white"
              >
                Tuyển dụng
              </a>
            </nav>
          </div>

          {/* Column 4: Hỗ trợ */}
          <div className="flex flex-col gap-3.5">
            <h4 className="text-xs font-black uppercase tracking-wider text-white">
              Hỗ trợ
            </h4>
            <nav className="flex flex-col gap-2.5 text-sm">
              <Link href="/login" className="text-slate-400 transition hover:text-white">
                Đăng nhập tài khoản
              </Link>
              <a
                href="mailto:info@parrotalk.fun"
                className="text-slate-400 transition hover:text-white"
              >
                Báo cáo lỗi
              </a>
              <a
                href="mailto:info@parrotalk.fun"
                className="flex items-center gap-2 text-slate-400 transition hover:text-white"
              >
                <Mail size={15} className="text-emerald-400" /> info@parrotalk.fun
              </a>
            </nav>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 border-t border-slate-900 pt-8 text-xs text-slate-500">
          <p>© 2026 ParroTalk. All rights reserved.</p>
          <div className="flex items-center gap-6">
            <a href="#footer" className="transition hover:text-slate-400">
              Privacy Policy
            </a>
            <a href="#footer" className="transition hover:text-slate-400">
              Terms of Service
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
