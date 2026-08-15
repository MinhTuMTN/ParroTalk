"use client";

import { LogOut, Shield, User as UserIcon } from "lucide-react";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";

export type AuthUser = {
  fullName?: string | null;
  role?: string | null;
};

export interface AuthActionProps {
  user?: AuthUser | null;
  isAuthenticated: boolean;
  logout: () => void;
  /** When true, renders stacked full-width buttons suitable for mobile menus */
  compact?: boolean;
  /** Optional callback triggered when an item is clicked (e.g. closing mobile drawer) */
  onItemClick?: () => void;
}

export function AuthAction({
  user,
  isAuthenticated,
  logout,
  compact = false,
  onItemClick,
}: AuthActionProps) {
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);

  // Close dropdown on outside click
  useEffect(() => {
    if (!isOpen) return;

    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isOpen]);

  const handleLogout = () => {
    logout();
    setIsOpen(false);
    onItemClick?.();
  };

  const handleLinkClick = () => {
    setIsOpen(false);
    onItemClick?.();
  };

  // --- 1. GUEST STATE (NOT AUTHENTICATED) ---
  if (!isAuthenticated) {
    if (compact) {
      return (
        <div className="grid gap-2">
          <Link
            href="/login"
            onClick={onItemClick}
            className="flex w-full items-center justify-center rounded-xl border border-slate-200 bg-white py-2.5 text-sm font-bold text-slate-700 shadow-sm"
          >
            Đăng nhập
          </Link>
          <Link
            href="/login?mode=signup"
            onClick={onItemClick}
            className="flex w-full items-center justify-center rounded-xl bg-amber-500 py-2.5 text-sm font-bold text-white shadow-sm hover:brightness-105"
          >
            Đăng ký
          </Link>
        </div>
      );
    }

    return (
      <div className="flex items-center gap-3">
        <Link
          href="/login"
          className="px-3 py-2 text-sm font-bold text-slate-600 transition hover:text-emerald-700"
        >
          Đăng nhập
        </Link>
        <Link
          href="/login?mode=signup"
          className="btn-press inline-flex items-center justify-center rounded-xl border-b-2 border-amber-600 bg-amber-500 px-5 py-2 text-sm font-bold text-white shadow-sm transition hover:brightness-105"
        >
          Đăng ký
        </Link>
      </div>
    );
  }

  // --- 2. AUTHENTICATED STATE - MOBILE (COMPACT) ---
  if (compact) {
    return (
      <div className="grid gap-2">
        {user?.role === "ADMIN" && (
          <Link
            href="/admin"
            onClick={handleLinkClick}
            className="flex items-center gap-2 rounded-xl bg-emerald-50 px-4 py-2.5 text-sm font-bold text-emerald-800"
          >
            <Shield size={16} /> Trang Admin
          </Link>
        )}
        <button
          type="button"
          onClick={handleLogout}
          className="flex items-center gap-2 rounded-xl bg-rose-50 px-4 py-2.5 text-left text-sm font-bold text-rose-600"
        >
          <LogOut size={16} /> Đăng xuất
        </button>
      </div>
    );
  }

  // --- 3. AUTHENTICATED STATE - DESKTOP (DROPDOWN MENU) ---
  return (
    <div className="relative" ref={menuRef}>
      <button
        type="button"
        onClick={() => setIsOpen((prev) => !prev)}
        className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-1.5 shadow-sm transition hover:border-emerald-300"
      >
        <span className="flex h-8 w-8 items-center justify-center rounded-full bg-emerald-100 text-xs font-black uppercase text-emerald-800">
          {user?.fullName?.charAt(0) || <UserIcon size={14} />}
        </span>
        <span className="max-w-28 truncate text-sm font-bold text-slate-700">
          {user?.fullName || "Tài khoản"}
        </span>
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-52 rounded-2xl border border-slate-200 bg-white p-1.5 shadow-xl shadow-slate-900/10">
          {user?.role === "ADMIN" && (
            <Link
              href="/admin"
              onClick={handleLinkClick}
              className="flex items-center gap-2.5 rounded-xl px-3 py-2.5 text-sm font-bold text-emerald-800 hover:bg-emerald-50"
            >
              <Shield size={16} /> Trang Admin
            </Link>
          )}
          <button
            type="button"
            onClick={handleLogout}
            className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2.5 text-left text-sm font-bold text-rose-600 hover:bg-rose-50"
          >
            <LogOut size={16} /> Đăng xuất
          </button>
        </div>
      )}
    </div>
  );
}
