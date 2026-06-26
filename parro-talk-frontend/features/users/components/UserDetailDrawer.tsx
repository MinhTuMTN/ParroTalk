"use client";

import { X } from "lucide-react";
import Badge from "@/components/ui/Badge";
import type { AdminUser } from "@/features/users/types/user";

type UserDetailDrawerProps = {
  user: AdminUser | null;
  isOpen: boolean;
  loading: boolean;
  onClose: () => void;
};

const getInitials = (value: string) =>
  value
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");

const formatDateTime = (value?: string | null) =>
  value
    ? new Intl.DateTimeFormat("en-US", {
        month: "short",
        day: "numeric",
        year: "numeric",
        hour: "numeric",
        minute: "2-digit",
      }).format(new Date(value))
    : "No activity yet";

export default function UserDetailDrawer({
  user,
  isOpen,
  loading,
  onClose,
}: UserDetailDrawerProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[90]">
      <button
        type="button"
        className="absolute inset-0 bg-slate-950/35 backdrop-blur-sm"
        onClick={onClose}
        aria-label="Close user detail"
      />
      <aside className="absolute right-0 top-0 h-full w-full max-w-md overflow-y-auto border-l border-slate-200 bg-white p-6 shadow-2xl">
        <button
          type="button"
          onClick={onClose}
          className="absolute right-5 top-5 rounded-full p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
          aria-label="Close"
        >
          <X className="h-5 w-5" />
        </button>

        {loading || !user ? (
          <div className="mt-16 text-sm text-slate-500">Loading user details...</div>
        ) : (
          <>
            <div className="mt-10 flex items-center gap-4">
              {user.avatarUrl ? (
                <img src={user.avatarUrl} alt={user.fullName} className="h-16 w-16 rounded-full object-cover" />
              ) : (
                <span className="flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100 text-lg font-bold text-emerald-700">
                  {getInitials(user.fullName)}
                </span>
              )}
              <div>
                <h2 className="text-xl font-bold text-slate-950">{user.fullName}</h2>
                <p className="text-sm text-slate-500">@{user.username}</p>
              </div>
            </div>

            <dl className="mt-8 space-y-4 text-sm">
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 pb-4">
                <dt className="text-slate-500">Email</dt>
                <dd className="font-medium text-slate-800">{user.email}</dd>
              </div>
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 pb-4">
                <dt className="text-slate-500">Role</dt>
                <dd>
                  <Badge tone={user.role === "ADMIN" ? "success" : user.role === "PRO_USER" ? "info" : "warning"}>
                    {user.role === "ADMIN" ? "Admin" : user.role === "PRO_USER" ? "Pro User" : "User"}
                  </Badge>
                </dd>
              </div>
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 pb-4">
                <dt className="text-slate-500">Status</dt>
                <dd>
                  <Badge tone={user.status === "ACTIVE" ? "success" : "neutral"}>
                    {user.status === "ACTIVE" ? "Active" : "Inactive"}
                  </Badge>
                </dd>
              </div>
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 pb-4">
                <dt className="text-slate-500">Joined date</dt>
                <dd className="font-medium text-slate-800">{formatDateTime(user.createdAt)}</dd>
              </div>
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 pb-4">
                <dt className="text-slate-500">Last active</dt>
                <dd className="font-medium text-slate-800">{formatDateTime(user.lastActiveAt)}</dd>
              </div>
            </dl>

            <div className="mt-8 grid grid-cols-2 gap-3">
              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <p className="text-xs font-semibold text-slate-500">Lessons learned</p>
                <p className="mt-2 text-2xl font-bold text-slate-950">{user.totalLessonsCompleted}</p>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <p className="text-xs font-semibold text-slate-500">Current streak</p>
                <p className="mt-2 text-2xl font-bold text-slate-950">{user.currentStreak}</p>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <p className="text-xs font-semibold text-slate-500">Average score</p>
                <p className="mt-2 text-2xl font-bold text-slate-950">{user.avgScore.toFixed(1)}</p>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <p className="text-xs font-semibold text-slate-500">Longest streak</p>
                <p className="mt-2 text-2xl font-bold text-slate-950">{user.longestStreak}</p>
              </div>
            </div>
          </>
        )}
      </aside>
    </div>
  );
}
