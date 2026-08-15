"use client";

import { useAuth } from "@/features/auth/hooks/useAuth";
import { Menu, X } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useState } from "react";
import { AuthAction } from "./AuthAction";
import { navItems } from "./constants";

export function Header() {
  const { user, isAuthenticated, logout } = useAuth();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  return (
    <header className="sticky top-0 z-50 border-b border-slate-200/80 bg-white/85 backdrop-blur-xl">
      <nav
        className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-3.5 sm:px-6 lg:px-8"
        aria-label="Main navigation"
      >
        <div className="flex items-center gap-8">
          <Link
            href="/"
            className="flex items-center gap-2.5 transition hover:opacity-90"
            aria-label="ParroTalk home"
          >
            <Image
              src="/logo.png"
              alt="ParroTalk logo"
              width={38}
              height={38}
              className="rounded-xl shadow-sm"
              priority
            />
            <span className="text-xl font-extrabold tracking-tight text-emerald-800">
              ParroTalk
            </span>
          </Link>

          <div className="hidden items-center gap-7 md:flex">
            {navItems.map((item) => (
              <Link
                key={item.label}
                href={item.href}
                className="text-sm font-semibold text-slate-600 transition-colors hover:text-emerald-700"
              >
                {item.label}
              </Link>
            ))}
          </div>
        </div>

        <div className="hidden items-center gap-3 md:flex">
          <AuthAction
            user={user}
            isAuthenticated={isAuthenticated}
            logout={logout}
          />
        </div>

        <button
          type="button"
          className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-700 shadow-sm transition hover:bg-slate-50 md:hidden"
          aria-label="Toggle navigation menu"
          aria-expanded={isMobileMenuOpen}
          onClick={() => setIsMobileMenuOpen((value) => !value)}
        >
          {isMobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
        </button>
      </nav>

      {isMobileMenuOpen && (
        <div className="mx-4 mb-4 rounded-2xl border border-slate-200 bg-white p-3 shadow-xl shadow-slate-900/5 md:hidden">
          <div className="grid gap-1">
            {navItems.map((item) => (
              <Link
                key={item.label}
                href={item.href}
                onClick={() => setIsMobileMenuOpen(false)}
                className="rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 transition hover:bg-emerald-50 hover:text-emerald-700"
              >
                {item.label}
              </Link>
            ))}
          </div>
          <div className="mt-3 border-t border-slate-100 pt-3">
            <AuthAction
              user={user}
              isAuthenticated={isAuthenticated}
              logout={logout}
              compact
              onItemClick={() => setIsMobileMenuOpen(false)}
            />
          </div>
        </div>
      )}
    </header>
  );
}
