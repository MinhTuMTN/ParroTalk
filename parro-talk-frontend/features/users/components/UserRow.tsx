"use client";

import { Eye, MoreVertical, Pencil } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import Badge from "@/components/ui/Badge";
import type {
  AdminUser,
  AdminUserRole,
} from "@/features/users/types/user";

type UserRowProps = {
  user: AdminUser;
  selected: boolean;
  isCurrentUser: boolean;
  onSelect: (id: string, checked: boolean) => void;
  onView: (user: AdminUser) => void;
  onEdit: (user: AdminUser) => void;
  onToggleStatus: (user: AdminUser) => void;
  onResetPassword: (user: AdminUser) => void;
  onDelete: (user: AdminUser) => void;
};

const roleLabelMap: Record<AdminUserRole, string> = {
  ADMIN: "Admin",
  PRO_USER: "Pro User",
  USER: "User",
};

const roleToneMap: Record<AdminUserRole, "success" | "info" | "warning"> = {
  ADMIN: "success",
  PRO_USER: "info",
  USER: "warning",
};

const getInitials = (value: string) =>
  value
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");

const formatDate = (value: string) =>
  new Intl.DateTimeFormat("en-US", {
    month: "numeric",
    day: "numeric",
    year: "numeric",
  }).format(new Date(value));

const formatRelativeTime = (value?: string | null) => {
  if (!value) return "No activity yet";

  const differenceMs = Math.max(0, Date.now() - new Date(value).getTime());
  const minute = 60_000;
  const hour = 60 * minute;
  const day = 24 * hour;

  if (differenceMs < hour) {
    const minutes = Math.max(1, Math.round(differenceMs / minute));
    return `${minutes} minute${minutes === 1 ? "" : "s"} ago`;
  }

  if (differenceMs < day) {
    const hours = Math.max(1, Math.round(differenceMs / hour));
    return `${hours} hour${hours === 1 ? "" : "s"} ago`;
  }

  const days = Math.max(1, Math.round(differenceMs / day));
  return `${days} day${days === 1 ? "" : "s"} ago`;
};

const hasRecentActivity = (user: AdminUser) => {
  if (!user.lastActiveAt || user.status !== "ACTIVE") return false;

  const threshold = 2 * 24 * 60 * 60 * 1000;
  return Date.now() - new Date(user.lastActiveAt).getTime() <= threshold;
};

export default function UserRow({
  user,
  selected,
  isCurrentUser,
  onSelect,
  onView,
  onEdit,
  onToggleStatus,
  onResetPassword,
  onDelete,
}: UserRowProps) {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleOutsideClick = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsMenuOpen(false);
      }
    };

    document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, []);

  return (
    <tr className="border-t border-slate-200 text-sm text-slate-700">
      <td className="px-4 py-4">
        <input
          type="checkbox"
          checked={selected}
          onChange={(event) => onSelect(user.id, event.target.checked)}
          aria-label={`Select ${user.fullName}`}
        />
      </td>
      <td className="px-4 py-4">
        <div className="flex items-center gap-3">
          {user.avatarUrl ? (
            <img
              src={user.avatarUrl}
              alt={user.fullName}
              className="h-10 w-10 rounded-full object-cover"
            />
          ) : (
            <span className="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-100 text-xs font-bold text-emerald-700">
              {getInitials(user.fullName)}
            </span>
          )}
          <span>
            <span className="block font-semibold text-slate-900">{user.fullName}</span>
            <span className="block text-xs text-slate-500">@{user.username}</span>
          </span>
        </div>
      </td>
      <td className="px-4 py-4 text-slate-600">{user.email}</td>
      <td className="px-4 py-4">
        <Badge tone={roleToneMap[user.role]}>{roleLabelMap[user.role]}</Badge>
      </td>
      <td className="px-4 py-4">
        <Badge tone={user.status === "ACTIVE" ? "success" : "neutral"}>
          {user.status === "ACTIVE" ? "Active" : "Inactive"}
        </Badge>
      </td>
      <td className="px-4 py-4">{formatDate(user.createdAt)}</td>
      <td className="px-4 py-4">
        <span className="inline-flex items-center gap-2 whitespace-nowrap">
          <span
            className={`h-2 w-2 rounded-full ${
              hasRecentActivity(user) ? "bg-emerald-500" : "bg-slate-300"
            }`}
          />
          {formatRelativeTime(user.lastActiveAt)}
        </span>
      </td>
      <td className="px-4 py-4">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => onView(user)}
            className="rounded-xl border border-slate-200 p-2 text-slate-500 transition hover:border-emerald-200 hover:bg-emerald-50 hover:text-emerald-700"
            aria-label={`View ${user.fullName}`}
          >
            <Eye className="h-4 w-4" />
          </button>
          <button
            type="button"
            onClick={() => onEdit(user)}
            className="rounded-xl border border-slate-200 p-2 text-slate-500 transition hover:border-emerald-200 hover:bg-emerald-50 hover:text-emerald-700"
            aria-label={`Edit ${user.fullName}`}
          >
            <Pencil className="h-4 w-4" />
          </button>
          <div className="relative" ref={menuRef}>
            <button
              type="button"
              onClick={() => setIsMenuOpen((value) => !value)}
              className="rounded-xl border border-slate-200 p-2 text-slate-500 transition hover:border-emerald-200 hover:bg-emerald-50 hover:text-emerald-700"
              aria-label={`More actions for ${user.fullName}`}
            >
              <MoreVertical className="h-4 w-4" />
            </button>

            {isMenuOpen ? (
              <div className="absolute right-0 top-11 z-20 w-52 rounded-2xl border border-slate-200 bg-white p-2 shadow-xl">
                <button
                  type="button"
                  onClick={() => {
                    setIsMenuOpen(false);
                    onToggleStatus(user);
                  }}
                  disabled={isCurrentUser && user.status === "ACTIVE"}
                  className="block w-full rounded-xl px-3 py-2 text-left text-sm text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-300"
                >
                  {user.status === "ACTIVE" ? "Deactivate user" : "Activate user"}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setIsMenuOpen(false);
                    onResetPassword(user);
                  }}
                  className="block w-full rounded-xl px-3 py-2 text-left text-sm text-slate-700 transition hover:bg-slate-50"
                >
                  Reset password
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setIsMenuOpen(false);
                    onDelete(user);
                  }}
                  disabled={isCurrentUser}
                  className="block w-full rounded-xl px-3 py-2 text-left text-sm text-rose-600 transition hover:bg-rose-50 disabled:cursor-not-allowed disabled:text-slate-300"
                >
                  Delete user
                </button>
              </div>
            ) : null}
          </div>
        </div>
      </td>
    </tr>
  );
}
