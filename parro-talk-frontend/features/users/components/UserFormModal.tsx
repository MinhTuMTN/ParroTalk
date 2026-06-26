"use client";

import { X } from "lucide-react";
import { useMemo, useState } from "react";
import Button from "@/components/ui/Button";
import Input from "@/components/ui/Input";
import type {
  AdminUser,
  AdminUserRole,
  AdminUserStatus,
  CreateAdminUserInput,
  UpdateAdminUserInput,
} from "@/features/users/types/user";

type UserFormModalProps = {
  mode: "create" | "edit";
  user?: AdminUser | null;
  submitting: boolean;
  onClose: () => void;
  onSubmit: (payload: CreateAdminUserInput | UpdateAdminUserInput) => Promise<void>;
};

type UserFormState = {
  fullName: string;
  username: string;
  email: string;
  role: AdminUserRole;
  status: AdminUserStatus;
  password: string;
};

type UserFormErrors = Partial<Record<keyof UserFormState, string>>;

const initialForm: UserFormState = {
  fullName: "",
  username: "",
  email: "",
  role: "USER",
  status: "ACTIVE",
  password: "",
};

const validateForm = (form: UserFormState, mode: UserFormModalProps["mode"]) => {
  const errors: UserFormErrors = {};

  if (!form.fullName.trim()) errors.fullName = "Full name is required.";
  if (!form.username.trim()) errors.username = "Username is required.";
  else if (/\s/.test(form.username)) errors.username = "Username must not contain spaces.";
  if (!form.email.trim()) errors.email = "Email is required.";
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errors.email = "Email is invalid.";
  if (mode === "create" && !form.password) errors.password = "Password is required.";
  else if (mode === "create" && form.password.length < 6) {
    errors.password = "Password must be at least 6 characters.";
  }

  return errors;
};

export default function UserFormModal({
  mode,
  user,
  submitting,
  onClose,
  onSubmit,
}: UserFormModalProps) {
  const [form, setForm] = useState<UserFormState>(() =>
    mode === "edit" && user
      ? {
          fullName: user.fullName,
          username: user.username,
          email: user.email,
          role: user.role,
          status: user.status,
          password: "",
        }
      : initialForm,
  );
  const [errors, setErrors] = useState<UserFormErrors>({});

  const title = mode === "create" ? "Add New User" : "Edit User";
  const submitLabel = mode === "create" ? "Create User" : "Save Changes";
  const payload = useMemo(
    () => ({
      fullName: form.fullName.trim(),
      username: form.username.trim(),
      email: form.email.trim(),
      role: form.role,
      status: form.status,
      ...(mode === "create" ? { password: form.password } : {}),
    }),
    [form, mode],
  );

  return (
    <div className="fixed inset-0 z-[90] flex items-center justify-center p-4">
      <button
        type="button"
        className="absolute inset-0 bg-slate-950/40 backdrop-blur-sm"
        onClick={onClose}
        aria-label="Close user form"
      />

      <form
        className="relative w-full max-w-xl rounded-[28px] border border-slate-200 bg-white p-6 shadow-2xl"
        onSubmit={async (event) => {
          event.preventDefault();
          const nextErrors = validateForm(form, mode);
          setErrors(nextErrors);
          if (Object.keys(nextErrors).length > 0) return;
          await onSubmit(payload);
        }}
      >
        <button
          type="button"
          onClick={onClose}
          className="absolute right-5 top-5 rounded-full p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
          aria-label="Close"
        >
          <X className="h-5 w-5" />
        </button>

        <div className="pr-10">
          <h2 className="text-xl font-bold text-slate-950">{title}</h2>
          <p className="mt-1 text-sm text-slate-500">
            {mode === "create"
              ? "Create a new account for the admin workspace."
              : "Update user profile and access settings."}
          </p>
        </div>

        <div className="mt-6 grid gap-4 sm:grid-cols-2">
          <label className="block sm:col-span-2">
            <span className="mb-2 block text-sm font-semibold text-slate-600">Full name</span>
            <Input
              value={form.fullName}
              onChange={(event) => setForm((current) => ({ ...current, fullName: event.target.value }))}
            />
            {errors.fullName ? <span className="mt-1 block text-xs text-rose-600">{errors.fullName}</span> : null}
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-semibold text-slate-600">Username</span>
            <Input
              value={form.username}
              onChange={(event) => setForm((current) => ({ ...current, username: event.target.value }))}
            />
            {errors.username ? <span className="mt-1 block text-xs text-rose-600">{errors.username}</span> : null}
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-semibold text-slate-600">Email</span>
            <Input
              type="email"
              value={form.email}
              onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
            />
            {errors.email ? <span className="mt-1 block text-xs text-rose-600">{errors.email}</span> : null}
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-semibold text-slate-600">Role</span>
            <select
              value={form.role}
              onChange={(event) =>
                setForm((current) => ({ ...current, role: event.target.value as AdminUserRole }))
              }
              className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-700 outline-none transition focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
            >
              <option value="USER">User</option>
              <option value="PRO_USER">Pro User</option>
              <option value="ADMIN">Admin</option>
            </select>
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-semibold text-slate-600">Status</span>
            <select
              value={form.status}
              onChange={(event) =>
                setForm((current) => ({ ...current, status: event.target.value as AdminUserStatus }))
              }
              className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-700 outline-none transition focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
            >
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </label>

          {mode === "create" ? (
            <label className="block sm:col-span-2">
              <span className="mb-2 block text-sm font-semibold text-slate-600">Password</span>
              <Input
                type="password"
                value={form.password}
                onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
              />
              {errors.password ? <span className="mt-1 block text-xs text-rose-600">{errors.password}</span> : null}
            </label>
          ) : null}
        </div>

        <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={submitting}>
            {submitting ? "Saving..." : submitLabel}
          </Button>
        </div>
      </form>
    </div>
  );
}
