"use client";

import { Info, Menu, Search } from "lucide-react";
import Link from "next/link";
import { useUI } from "@/hooks/useUI";
import { useAuth } from "@/features/auth/hooks/useAuth";

export default function StreakHeader() {
  const { openMobileMenu } = useUI();
  const { user } = useAuth();

  return (
    <>
      <header className="sticky top-0 z-40 border-b border-gray-100 bg-white/90 backdrop-blur-md">
        <div className="mx-auto flex items-center justify-between gap-4 px-4 py-4 md:px-8">
          <div className="flex min-w-0 items-center gap-3 md:gap-8">
            <button
              type="button"
              onClick={openMobileMenu}
              className="rounded-xl p-2 text-gray-400 transition-all hover:bg-gray-100 active:scale-95 lg:hidden"
              aria-label="Open menu"
            >
              <Menu size={22} />
            </button>

            <nav className="hidden items-center gap-8 text-sm font-bold lg:flex">
              <Link href="/library" className="text-green-600 transition-colors hover:text-green-700">
                Lesson Library
              </Link>
              <Link href="/profile" className="border-b-2 border-green-500 pb-1 text-gray-900">
                User Streak
              </Link>
            </nav>
          </div>

          <div className="hidden flex-1 justify-center md:flex">
            <label className="relative block w-full max-w-md">
              <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="Search lessons..."
                className="w-full rounded-full border border-gray-100 bg-gray-50 py-2.5 pl-11 pr-4 text-sm text-gray-700 outline-none transition focus:border-green-400 focus:bg-white focus:ring-2 focus:ring-green-100"
              />
            </label>
          </div>

          <div className="flex items-center gap-3 border-l border-gray-100 pl-4">
            <div className="hidden text-right sm:block">
              <p className="text-sm font-black uppercase leading-none text-gray-900">{user?.fullName ?? "User"}</p>
              <p className="mt-1 text-[10px] font-bold uppercase tracking-widest text-gray-400">{user?.role ?? "USER"}</p>
            </div>
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-green-50 font-black text-green-600">
              {user?.fullName?.charAt(0) ?? "U"}
            </div>
          </div>
        </div>
      </header>

      <section className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-4 py-8 md:gap-10 md:px-8 md:py-12">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-3xl font-black tracking-tight text-slate-900 md:text-4xl">User Streak</h1>
            <div className="group relative">
              <button
                type="button"
                className="flex h-6 w-6 items-center justify-center rounded-full border border-green-100 bg-green-50 text-green-700 transition hover:bg-green-100 focus:outline-none focus:ring-2 focus:ring-green-200"
                aria-label="How streaks work"
              >
                <Info size={14} />
              </button>
              <div className="pointer-events-none absolute left-1/2 top-full z-20 mt-3 hidden w-72 -translate-x-1/2 rounded-2xl border border-gray-100 bg-white p-4 text-sm leading-6 text-slate-600 shadow-xl group-hover:block group-focus-within:block">
                A study day counts after you complete at least 2 segments. Study on consecutive days to keep your streak alive.
              </div>
            </div>
          </div>
          <p className="mt-2 text-sm text-slate-500 md:text-base">Keep learning every day and build your streak!</p>
        </div>
      </section>
    </>
  );
}
