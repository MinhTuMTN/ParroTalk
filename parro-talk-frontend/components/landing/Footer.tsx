import { Mail, Share2, Video } from "lucide-react";
import Image from "next/image";
import Link from "next/link";

export function Footer() {
  return (
    <footer
      id="footer"
      className="border-t border-slate-200/80 bg-slate-900 text-slate-300 py-14 px-4 sm:px-6 lg:px-8"
    >
      <div className="mx-auto max-w-7xl">
        <div className="grid gap-10 sm:grid-cols-2 md:grid-cols-4 mb-12">
          {/* Brand Info */}
          <div className="flex flex-col gap-4">
            <Link href="/" className="flex items-center gap-2.5">
              <Image
                src="/logo.png"
                alt="ParroTalk Logo"
                width={36}
                height={36}
                className="rounded-xl brightness-110"
              />
              <span className="text-xl font-black text-white tracking-tight">
                ParroTalk
              </span>
            </Link>
            <p className="text-sm leading-relaxed text-slate-400">
              Nâng tầm kỹ năng nghe chép chính tả với AI. Rèn luyện đôi tai phản
              xạ tự nhiên qua từng câu video.
            </p>
            <div className="flex items-center gap-3 text-slate-400">
              <Link
                href="/library"
                className="transition hover:text-emerald-400"
                aria-label="Thư viện bài học"
              >
                <Video size={18} />
              </Link>
              <a
                href="mailto:info@parrotalk.fun"
                className="transition hover:text-emerald-400"
                aria-label="Gửi email liên hệ"
              >
                <Mail size={18} />
              </a>
              <a
                href="#pwa-guide"
                className="transition hover:text-emerald-400"
                aria-label="Hướng dẫn PWA"
              >
                <Share2 size={18} />
              </a>
            </div>
          </div>

          {/* Column 2: Sản phẩm */}
          <div className="flex flex-col gap-3">
            <h4 className="text-xs font-black uppercase tracking-wider text-white">
              Sản phẩm
            </h4>
            <nav className="flex flex-col gap-2.5 text-sm">
              <Link href="/library" className="transition hover:text-white">
                Kho bài học
              </Link>
              <a href="#how-it-works" className="transition hover:text-white">
                Phương pháp Dictation
              </a>
              <a href="#demo-widget" className="transition hover:text-white">
                Luyện nghe tương tác
              </a>
              <a href="#pwa-guide" className="transition hover:text-white">
                Cài đặt ứng dụng PWA
              </a>
            </nav>
          </div>

          {/* Column 3: Công ty */}
          <div className="flex flex-col gap-3">
            <h4 className="text-xs font-black uppercase tracking-wider text-white">
              Công ty
            </h4>
            <nav className="flex flex-col gap-2.5 text-sm">
              <a href="#footer" className="transition hover:text-white">
                Về chúng tôi
              </a>
              <Link href="/library" className="transition hover:text-white">
                Lộ trình học tập
              </Link>
              <a
                href="mailto:info@parrotalk.fun"
                className="transition hover:text-white"
              >
                Tuyển dụng
              </a>
            </nav>
          </div>

          {/* Column 4: Hỗ trợ */}
          <div className="flex flex-col gap-3">
            <h4 className="text-xs font-black uppercase tracking-wider text-white">
              Hỗ trợ
            </h4>
            <nav className="flex flex-col gap-2.5 text-sm">
              <Link href="/login" className="transition hover:text-white">
                Đăng nhập tài khoản
              </Link>
              <a
                href="mailto:info@parrotalk.fun"
                className="transition hover:text-white"
              >
                Báo cáo lỗi
              </a>
              <a
                href="mailto:info@parrotalk.fun"
                className="flex items-center gap-2 transition hover:text-white"
              >
                <Mail size={16} /> info@parrotalk.fun
              </a>
            </nav>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 border-t border-slate-800 pt-8 text-xs text-slate-500">
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
