import type { Metadata, Viewport } from "next";
import { Plus_Jakarta_Sans } from "next/font/google";
import "./globals.css";

import { AuthProvider } from "@/features/auth/hooks/useAuth";

import { THEME_COLOR } from "@/lib/constants/colors";

const plusJakartaSans = Plus_Jakarta_Sans({
  subsets: ["latin", "vietnamese"],
  weight: ["400", "500", "600", "700", "800"],
  variable: "--font-plus-jakarta-sans",
  display: "swap",
});

export const metadata: Metadata = {
  title: "ParroTalk - Nền Tảng Luyện Nghe & Chép Chính Tả Tiếng Anh",
  description:
    "Rèn luyện phản xạ nghe chép chính tả tiếng Anh với video thực tế. Điều chỉnh tốc độ, lặp lại thông minh và nhận gợi ý độ chính xác tức thì.",
  applicationName: "ParroTalk",
  appleWebApp: {
    capable: true,
    title: "ParroTalk",
    statusBarStyle: "default",
  },
  formatDetection: {
    telephone: false,
  },
  icons: {
    icon: "/logo.png",
    apple: "/logo.png",
  },
};

export const viewport: Viewport = {
  themeColor: THEME_COLOR,
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi" className={`antialiased ${plusJakartaSans.variable}`}>
      <body className="font-sans min-h-screen bg-slate-50 text-slate-900">
        <AuthProvider>
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
