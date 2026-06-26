"use client";

import { Bell, ChevronDown, Menu } from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";

type AdminHeaderProps = {
  onMenuClick: () => void;
};

const getInitials = (value: string) =>
  value
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");

export default function AdminHeader({ onMenuClick }: AdminHeaderProps) {
  const { user } = useAuth();
  const displayName = user?.fullName || "Admin";
  const roleLabel = user?.role === "ADMIN" ? "Super Admin" : user?.role || "Admin";
  const initials = getInitials(displayName) || "AD";

  return (
    <header className="flex items-center justify-between gap-4">
      <button
        type="button"
        onClick={onMenuClick}
        className="rounded-2xl border border-emerald-100 bg-white p-2.5 text-slate-600 shadow-sm transition hover:bg-emerald-50 lg:hidden"
        aria-label="Open sidebar"
      >
        <Menu className="h-5 w-5" />
      </button>

      <div className="ml-auto flex items-center gap-3 sm:gap-5">
        <button
          type="button"
          className="relative rounded-full border border-emerald-100 bg-white p-2.5 text-slate-600 shadow-sm transition hover:bg-emerald-50"
          aria-label="Notifications"
        >
          <Bell className="h-5 w-5" />
          <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] font-bold text-white">
            3
          </span>
        </button>

        <button
          type="button"
          className="flex items-center gap-3 rounded-2xl px-1 py-1 text-left transition hover:bg-white"
          aria-label="Open admin menu"
        >
          {user?.avatarUrl ? (
            <img
              src={user.avatarUrl}
              alt={displayName}
              className="h-11 w-11 rounded-full object-cover"
            />
          ) : (
            <span className="flex h-11 w-11 items-center justify-center rounded-full bg-emerald-100 text-sm font-bold text-emerald-700">
              {initials}
            </span>
          )}

          <span className="hidden sm:block">
            <span className="block text-sm font-semibold text-slate-900">{displayName}</span>
            <span className="block text-xs text-slate-500">{roleLabel}</span>
          </span>

          <ChevronDown className="hidden h-4 w-4 text-slate-500 sm:block" />
        </button>
      </div>
    </header>
  );
}
