import type { Metadata } from "next";
import "./globals.css";

import { AuthProvider } from "@/features/auth/hooks/useAuth";

export const metadata: Metadata = {
  title: "ParroTalk - Master English through Dictation",
  description: "A fun and effective way to improve your English listening skills.",
  icons: {
    icon: "/logo.png",
    apple: "/logo.png",
  },
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

