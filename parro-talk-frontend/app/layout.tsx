import type { Metadata, Viewport } from "next";
import "./globals.css";

import { AuthProvider } from "@/features/auth/hooks/useAuth";

export const metadata: Metadata = {
  title: "ParroTalk - English Dictation Practice",
  description:
    "Practice English listening with real videos, focused dictation, instant checking, Vietnamese translations, and mobile-friendly progress tracking.",
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
  themeColor: "#10b981",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="antialiased">
      <body>
        <AuthProvider>
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
