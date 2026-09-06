"use client";

import { useAuth } from "@/features/auth/hooks/useAuth";
import { BookOpen, Headphones, Home, User } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";

export function MobileBottomNav() {
  const pathname = usePathname();
  const { isAuthenticated } = useAuth();

  const navLinks = [
    { label: "Trang chủ", href: "/", icon: Home },
    { label: "Kho bài", href: "/library", icon: BookOpen },
    { label: "Luyện tập", href: "/vocabulary/practice", icon: Headphones },
    { label: "Tài khoản", href: isAuthenticated ? "/profile" : "/login", icon: User },
  ];

  return (
    <div className="fixed bottom-4 left-0 right-0 z-40 px-4 md:hidden pointer-events-none">
      <nav
        aria-label="Mobile bottom navigation"
        className="pointer-events-auto mx-auto max-w-sm rounded-full border border-emerald-100 bg-white/95 px-3 py-2 shadow-xl shadow-slate-900/10 backdrop-blur-lg flex items-center justify-around"
      >
        {navLinks.map((item) => {
          const Icon = item.icon;
          const isActive = pathname === item.href;

          return (
            <Link
              key={item.label}
              href={item.href}
              className={`relative flex flex-col items-center justify-center rounded-full px-3.5 py-1.5 transition-all ${
                isActive
                  ? "bg-emerald-600 text-white shadow-md shadow-emerald-600/30 font-bold"
                  : "text-slate-500 hover:text-emerald-700 font-medium"
              }`}
            >
              <Icon size={18} />
              <span className="text-[10px] mt-0.5">{item.label}</span>
              {isActive && (
                <span className="absolute -bottom-1 h-1 w-1 rounded-full bg-white" />
              )}
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
