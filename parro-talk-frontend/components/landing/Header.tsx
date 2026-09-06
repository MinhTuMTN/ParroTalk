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
    <header className="sticky top-0 z-50 border-b border-emerald-900/5 bg-white/90 backdrop-blur-xl transition-all">
      <nav
        className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-3 sm:px-6 lg:px-8"
        aria-label="Main navigation"
      >
        <div className="flex items-center gap-8">
          <Link
            href="/"
            className="group flex items-center gap-2.5 transition"
            aria-label="ParroTalk home"
          >
            <div className="relative flex h-10 w-10 items-center justify-center rounded-2xl bg-emerald-50 border border-emerald-200/60 p-1 shadow-sm transition-transform group-hover:scale-105">
              <Image
                src="/logo.png"
                alt="ParroTalk logo"
                width={32}
                height={32}
                className="rounded-xl object-contain"
                priority
              />
            </div>
            <div className="flex flex-col">
              <span className="text-xl font-extrabold tracking-tight text-slate-900 group-hover:text-emerald-700 transition-colors">
                Parro<span className="text-emerald-600">Talk</span>
              </span>
            </div>
          </Link>

          <div className="hidden items-center gap-1.5 md:flex bg-slate-50/80 p-1 rounded-full border border-slate-200/60">
            {navItems.map((item) => (
              <Link
                key={item.label}
                href={item.href}
                className="rounded-full px-4 py-1.5 text-xs font-bold text-slate-600 transition-all hover:bg-white hover:text-emerald-700 hover:shadow-xs"
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
          className="inline-flex h-10 w-10 items-center justify-center rounded-2xl border border-slate-200/80 bg-white text-slate-700 shadow-sm transition hover:bg-slate-50 active:scale-95 md:hidden"
          aria-label="Toggle navigation menu"
          aria-expanded={isMobileMenuOpen}
          onClick={() => setIsMobileMenuOpen((value) => !value)}
        >
          {isMobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
        </button>
      </nav>

      {isMobileMenuOpen && (
        <div className="mx-4 mb-4 rounded-3xl border border-emerald-100 bg-white p-4 shadow-xl shadow-slate-900/5 md:hidden animate-in fade-in slide-in-from-top-2 duration-200">
          <div className="grid gap-1.5">
            {navItems.map((item) => (
              <Link
                key={item.label}
                href={item.href}
                onClick={() => setIsMobileMenuOpen(false)}
                className="rounded-2xl px-4 py-3 text-sm font-bold text-slate-700 transition hover:bg-emerald-50 hover:text-emerald-700"
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
