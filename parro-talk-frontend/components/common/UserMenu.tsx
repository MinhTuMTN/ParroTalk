"use client";

import { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { Settings, User as UserIcon, LogOut as LogOutIcon } from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useUserSettings } from "@/features/settings/hooks/useUserSettings";
import { getSettingsCopy } from "@/features/settings/types";

export default function UserMenu() {
  const { user, logout } = useAuth();
  const { settings } = useUserSettings();
  const navCopy = getSettingsCopy(settings.uiLanguage).nav;
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [failedAvatarUrl, setFailedAvatarUrl] = useState<string | null>(null);
  const userMenuRef = useRef<HTMLDivElement>(null);

  const avatarSrc = user?.avatarUrl?.trim() || "";
  const showAvatarImage = Boolean(avatarSrc) && failedAvatarUrl !== avatarSrc;

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target as Node)) {
        setIsUserMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <div className="relative" ref={userMenuRef}>
      <div
        onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
        className="flex items-center gap-2 sm:gap-3 pl-2 sm:pl-4 border-l border-gray-100 dark:border-slate-800 cursor-pointer group"
      >
        <div className="hidden sm:flex flex-col items-end">
          <span className="text-sm font-black text-gray-800 dark:text-slate-100 leading-none group-hover:text-green-600 dark:group-hover:text-emerald-400 transition-colors uppercase">
            {user?.fullName ?? "User"}
          </span>
          <span className="text-[10px] font-bold text-gray-400 dark:text-slate-400 uppercase tracking-widest">
            {user?.role ?? "USER"}
          </span>
        </div>
        <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-green-100 dark:bg-emerald-950 text-green-600 dark:text-emerald-300 flex items-center justify-center font-bold outline outline-offset-2 outline-white dark:outline-slate-800 group-hover:bg-green-200 dark:group-hover:bg-emerald-900 group-hover:outline-green-50 transition-all shadow-sm text-sm sm:text-base overflow-hidden">
          {showAvatarImage ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={avatarSrc}
              alt={user?.fullName || "User avatar"}
              className="w-full h-full object-cover"
              onError={() => setFailedAvatarUrl(avatarSrc)}
            />
          ) : (
            user?.fullName?.charAt(0) || "U"
          )}
        </div>
      </div>

      {isUserMenuOpen && (
        <div className="absolute right-0 mt-3 w-56 bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-gray-100 dark:border-slate-700 py-2 z-[60] animate-in fade-in zoom-in duration-200 origin-top-right">
          <div className="px-4 py-3 border-b border-gray-50 dark:border-slate-800 flex flex-col gap-1 sm:hidden">
            <p className="text-sm font-black text-gray-900 dark:text-slate-100 leading-none">{user?.fullName ?? "User"}</p>
            <p className="text-[10px] font-bold text-gray-400 dark:text-slate-400 uppercase tracking-widest">{user?.role ?? "USER"}</p>
          </div>

          <div className="p-1.5 flex flex-col gap-0.5">
            <Link
              href="/profile"
              prefetch={false}
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-bold text-gray-600 dark:text-slate-200 hover:bg-gray-50 dark:hover:bg-slate-800 hover:text-gray-900 dark:hover:text-white transition-all group"
              onClick={() => setIsUserMenuOpen(false)}
            >
              <div className="w-8 h-8 rounded-lg bg-blue-50 dark:bg-blue-950/60 text-blue-500 dark:text-blue-300 flex items-center justify-center group-hover:scale-110 transition-transform">
                <UserIcon size={16} />
              </div>
              {navCopy.profile}
            </Link>
            <Link
              href="/settings"
              prefetch={false}
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-bold text-gray-600 dark:text-slate-200 hover:bg-gray-50 dark:hover:bg-slate-800 hover:text-gray-900 dark:hover:text-white transition-all group"
              onClick={() => setIsUserMenuOpen(false)}
            >
              <div className="w-8 h-8 rounded-lg bg-gray-100 dark:bg-slate-800 text-gray-500 dark:text-slate-300 flex items-center justify-center group-hover:scale-110 transition-transform">
                <Settings size={16} />
              </div>
              {navCopy.settings}
            </Link>
            <div className="h-px bg-gray-50 dark:bg-slate-800 my-1 mx-2" />
            <button
              onClick={() => {
                logout();
                setIsUserMenuOpen(false);
              }}
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-bold text-red-500 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/40 transition-all group w-full text-left"
            >
              <div className="w-8 h-8 rounded-lg bg-red-50 dark:bg-red-950/50 text-red-500 dark:text-red-400 flex items-center justify-center group-hover:scale-110 transition-transform">
                <LogOutIcon size={16} />
              </div>
              {navCopy.logout}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
