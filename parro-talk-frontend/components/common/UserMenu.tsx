"use client";

import { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { Settings, User as UserIcon, LogOut as LogOutIcon } from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";

export default function UserMenu() {
  const { user, logout } = useAuth();
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);

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
        className="flex items-center gap-2 sm:gap-3 pl-2 sm:pl-4 border-l border-gray-100 cursor-pointer group"
      >
        <div className="hidden sm:flex flex-col items-end">
          <span className="text-sm font-black text-gray-800 leading-none group-hover:text-green-600 transition-colors uppercase">
            {user?.fullName ?? "User"}
          </span>
          <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">
            {user?.role ?? "USER"}
          </span>
        </div>
        <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-green-100 text-green-600 flex items-center justify-center font-bold outline outline-offset-2 outline-white group-hover:bg-green-200 group-hover:outline-green-50 transition-all shadow-sm text-sm sm:text-base">
          {user?.fullName?.charAt(0) || "U"}
        </div>
      </div>

      {isUserMenuOpen && (
        <div className="absolute right-0 mt-3 w-56 bg-white rounded-2xl shadow-2xl border border-gray-100 py-2 z-[60] animate-in fade-in zoom-in duration-200 origin-top-right">
          <div className="px-4 py-3 border-b border-gray-50 flex flex-col gap-1 sm:hidden">
            <p className="text-sm font-black text-gray-900 leading-none">{user?.fullName ?? "User"}</p>
            <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">{user?.role ?? "USER"}</p>
          </div>

          <div className="p-1.5 flex flex-col gap-0.5">
            <Link
              href="/profile"
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-bold text-gray-600 hover:bg-gray-50 hover:text-gray-900 transition-all group"
              onClick={() => setIsUserMenuOpen(false)}
            >
              <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-500 flex items-center justify-center group-hover:scale-110 transition-transform">
                <UserIcon size={16} />
              </div>
              Profile
            </Link>
            <Link
              href="/settings"
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-bold text-gray-600 hover:bg-gray-50 hover:text-gray-900 transition-all group"
              onClick={() => setIsUserMenuOpen(false)}
            >
              <div className="w-8 h-8 rounded-lg bg-gray-100 text-gray-500 flex items-center justify-center group-hover:scale-110 transition-transform">
                <Settings size={16} />
              </div>
              Settings
            </Link>
            <div className="h-px bg-gray-50 my-1 mx-2" />
            <button
              onClick={() => {
                logout();
                setIsUserMenuOpen(false);
              }}
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-bold text-red-500 hover:bg-red-50 transition-all group w-full text-left"
            >
              <div className="w-8 h-8 rounded-lg bg-red-50 text-red-500 flex items-center justify-center group-hover:scale-110 transition-transform">
                <LogOutIcon size={16} />
              </div>
              Log out
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
