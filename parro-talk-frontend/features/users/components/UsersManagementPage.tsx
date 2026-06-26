"use client";

import {
  ChevronLeft,
  ChevronRight,
  Download,
  Plus,
  SlidersHorizontal,
} from "lucide-react";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import axios from "axios";
import Button from "@/components/ui/Button";
import ConfirmationModal from "@/components/ui/ConfirmationModal";
import FloatingToast from "@/components/ui/FloatingToast";
import Input from "@/components/ui/Input";
import { useAuth } from "@/features/auth/hooks/useAuth";
import UserDetailDrawer from "@/features/users/components/UserDetailDrawer";
import UserFormModal from "@/features/users/components/UserFormModal";
import TemporaryPasswordModal from "@/features/users/components/TemporaryPasswordModal";
import UsersTable from "@/features/users/components/UsersTable";
import { useUsers } from "@/features/users/hooks/useUsers";
import { adminUserService } from "@/features/users/services/adminUserService";
import type {
  AdminUser,
  AdminUserRole,
  AdminUserStatus,
  CreateAdminUserInput,
  UpdateAdminUserInput,
} from "@/features/users/types/user";

const roleOptions: { value: AdminUserRole | "all"; label: string }[] = [
  { value: "all", label: "All Roles" },
  { value: "ADMIN", label: "Admin" },
  { value: "PRO_USER", label: "Pro User" },
  { value: "USER", label: "User" },
];

const statusOptions: { value: AdminUserStatus | "all"; label: string }[] = [
  { value: "all", label: "All Statuses" },
  { value: "ACTIVE", label: "Active" },
  { value: "INACTIVE", label: "Inactive" },
];

const pageSizeOptions = [8, 10, 20];

const getVisiblePages = (page: number, totalPages: number) => {
  if (totalPages <= 4) {
    return Array.from({ length: totalPages }, (_, index) => index);
  }

  if (page <= 1) {
    return [0, 1, 2, totalPages - 1];
  }

  if (page >= totalPages - 2) {
    return [0, totalPages - 3, totalPages - 2, totalPages - 1];
  }

  return [0, page, page + 1, totalPages - 1];
};

const getErrorMessage = (error: unknown, fallback: string) =>
  axios.isAxiosError<{ message?: string }>(error)
    ? error.response?.data?.message || fallback
    : fallback;

const escapeCsv = (value: string | number | null | undefined) => {
  const normalized = value == null ? "" : String(value);
  return `"${normalized.replace(/"/g, '""')}"`;
};

export default function UsersManagementPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const {
    users,
    searchQuery,
    roleFilter,
    statusFilter,
    selectedIds,
    page,
    pageSize,
    totalItems,
    totalPages,
    loading,
    error,
    refresh,
    setPage,
    setSearchQuery,
    setRoleFilter,
    setStatusFilter,
    setPageSize,
    selectUser,
    selectAllVisible,
  } = useUsers(user?.role === "ADMIN");

  const [formMode, setFormMode] = useState<"create" | "edit" | null>(null);
  const [editingUser, setEditingUser] = useState<AdminUser | null>(null);
  const [viewingUser, setViewingUser] = useState<AdminUser | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<AdminUser | null>(null);
  const [temporaryPassword, setTemporaryPassword] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [toast, setToast] = useState<{ message: string; variant: "success" | "error" | "info" } | null>(
    null,
  );

  useEffect(() => {
    if (!isAuthLoading && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isAuthenticated, isAuthLoading, router]);

  useEffect(() => {
    if (!toast) return;

    const timeout = window.setTimeout(() => setToast(null), 3500);
    return () => window.clearTimeout(timeout);
  }, [toast]);

  const visiblePages = getVisiblePages(page, totalPages);
  const firstVisibleUser = totalItems === 0 ? 0 : page * pageSize + 1;
  const lastVisibleUser = Math.min((page + 1) * pageSize, totalItems);

  const handleCreate = async (payload: CreateAdminUserInput | UpdateAdminUserInput) => {
    setSubmitting(true);
    try {
      await adminUserService.createUser(payload as CreateAdminUserInput);
      setFormMode(null);
      await refresh();
      setToast({ message: "User created successfully.", variant: "success" });
    } catch (submitError) {
      setToast({
        message: getErrorMessage(submitError, "Unable to create user."),
        variant: "error",
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpdate = async (payload: CreateAdminUserInput | UpdateAdminUserInput) => {
    if (!editingUser) return;

    setSubmitting(true);
    try {
      await adminUserService.updateUser(editingUser.id, payload as UpdateAdminUserInput);
      setFormMode(null);
      setEditingUser(null);
      await refresh();
      setToast({ message: "User updated successfully.", variant: "success" });
    } catch (submitError) {
      setToast({
        message: getErrorMessage(submitError, "Unable to update user."),
        variant: "error",
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleViewUser = async (target: AdminUser) => {
    setIsDetailOpen(true);
    setDetailLoading(true);
    try {
      setViewingUser(await adminUserService.getUser(target.id));
    } catch (detailError) {
      setToast({
        message: getErrorMessage(detailError, "Unable to load user details."),
        variant: "error",
      });
      setIsDetailOpen(false);
    } finally {
      setDetailLoading(false);
    }
  };

  const handleToggleStatus = async (target: AdminUser) => {
    const nextStatus = target.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    try {
      await adminUserService.updateStatus(target.id, nextStatus);
      await refresh();
      setToast({
        message: `User ${nextStatus === "ACTIVE" ? "activated" : "deactivated"} successfully.`,
        variant: "success",
      });
    } catch (statusError) {
      setToast({
        message: getErrorMessage(statusError, "Unable to update user status."),
        variant: "error",
      });
    }
  };

  const handleResetPassword = async (target: AdminUser) => {
    try {
      const result = await adminUserService.resetPassword(target.id);
      setTemporaryPassword(result.temporaryPassword);
      setToast({ message: "Temporary password generated.", variant: "success" });
    } catch (resetError) {
      setToast({
        message: getErrorMessage(resetError, "Unable to reset password."),
        variant: "error",
      });
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;

    setSubmitting(true);
    try {
      await adminUserService.deleteUser(deleteTarget.id);
      setDeleteTarget(null);
      await refresh();
      setToast({ message: "User deleted successfully.", variant: "success" });
    } catch (deleteError) {
      setToast({
        message: getErrorMessage(deleteError, "Unable to delete user."),
        variant: "error",
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleExport = () => {
    const headers = [
      "Full name",
      "Username",
      "Email",
      "Role",
      "Status",
      "Joined date",
      "Last active",
    ];
    const rows = users.map((item) => [
      item.fullName,
      item.username,
      item.email,
      item.role,
      item.status,
      item.createdAt,
      item.lastActiveAt ?? "",
    ]);
    const csv = [headers, ...rows].map((row) => row.map(escapeCsv).join(",")).join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "users-export.csv";
    anchor.click();
    URL.revokeObjectURL(url);
  };

  if (isAuthLoading || (!isAuthenticated && !user)) {
    return (
      <div className="rounded-[28px] border border-slate-200 bg-white p-10 text-center text-sm text-slate-500 shadow-sm">
        Loading users management...
      </div>
    );
  }

  if (user?.role !== "ADMIN") {
    return (
      <div className="rounded-[28px] border border-rose-100 bg-white p-10 text-center shadow-sm">
        <h1 className="text-2xl font-bold text-slate-950">Forbidden</h1>
        <p className="mt-2 text-sm text-slate-500">
          Only admin users can access this page.
        </p>
      </div>
    );
  }

  return (
    <>
      <section className="rounded-[28px] border border-emerald-50 bg-white p-4 shadow-sm md:p-6">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-950 md:text-[28px]">
              Users Management
            </h1>
            <p className="mt-2 text-sm text-slate-500">
              Manage and monitor user accounts, roles, and activity.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <Button leftIcon={<Plus className="h-4 w-4" />} onClick={() => setFormMode("create")}>
              Add New User
            </Button>
            <Button
              variant="secondary"
              leftIcon={<Download className="h-4 w-4" />}
              onClick={handleExport}
            >
              Export
            </Button>
          </div>
        </div>

        <div className="mt-6 grid gap-3 md:grid-cols-[minmax(260px,1.2fr)_180px_180px_auto]">
          <Input
            withSearchIcon
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            placeholder="Search users..."
          />

          <label className="sr-only" htmlFor="role-filter">
            Filter by role
          </label>
          <select
            id="role-filter"
            value={roleFilter}
            onChange={(event) => setRoleFilter(event.target.value as AdminUserRole | "all")}
            className="rounded-2xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-700 outline-none transition focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
          >
            {roleOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          <label className="sr-only" htmlFor="status-filter">
            Filter by status
          </label>
          <select
            id="status-filter"
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value as AdminUserStatus | "all")}
            className="rounded-2xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-700 outline-none transition focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
          >
            {statusOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          <Button
            variant="secondary"
            leftIcon={<SlidersHorizontal className="h-4 w-4" />}
            onClick={() => undefined}
            className="justify-self-start md:justify-self-auto"
          >
            More Filters
          </Button>
        </div>

        <div className="mt-6">
          {loading ? (
            <div className="rounded-[24px] border border-slate-200 bg-slate-50 p-10 text-center text-sm text-slate-500">
              Loading users...
            </div>
          ) : error ? (
            <div className="rounded-[24px] border border-rose-200 bg-rose-50 p-10 text-center text-sm text-rose-700">
              {error}
            </div>
          ) : users.length === 0 ? (
            <div className="rounded-[24px] border border-slate-200 bg-slate-50 p-10 text-center text-sm text-slate-500">
              No users found.
            </div>
          ) : (
            <UsersTable
              users={users}
              selectedIds={selectedIds}
              currentUserId={user.id}
              onSelectAll={selectAllVisible}
              onSelect={selectUser}
              onView={(target) => void handleViewUser(target)}
              onEdit={(target) => {
                setEditingUser(target);
                setFormMode("edit");
              }}
              onToggleStatus={(target) => void handleToggleStatus(target)}
              onResetPassword={(target) => void handleResetPassword(target)}
              onDelete={setDeleteTarget}
            />
          )}
        </div>

        <div className="mt-6 flex flex-col gap-4 text-sm text-slate-600 lg:flex-row lg:items-center lg:justify-between">
          <p>
            Showing {firstVisibleUser} to {lastVisibleUser} of {totalItems} users
          </p>

          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={page === 0}
              className="flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 text-slate-500 transition hover:border-emerald-200 hover:text-emerald-700 disabled:cursor-not-allowed disabled:opacity-40"
              aria-label="Previous page"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>

            {visiblePages.map((visiblePage, index) => {
              const previousPage = visiblePages[index - 1];
              const shouldShowEllipsis =
                previousPage !== undefined && visiblePage - previousPage > 1;

              return (
                <span key={visiblePage} className="contents">
                  {shouldShowEllipsis ? <span className="px-1 text-slate-400">...</span> : null}
                  <button
                    type="button"
                    onClick={() => setPage(visiblePage)}
                    className={`h-10 min-w-10 rounded-xl border px-3 font-semibold transition ${
                      visiblePage === page
                        ? "border-emerald-600 bg-emerald-600 text-white"
                        : "border-slate-200 text-slate-600 hover:border-emerald-200 hover:text-emerald-700"
                    }`}
                  >
                    {visiblePage + 1}
                  </button>
                </span>
              );
            })}

            <button
              type="button"
              onClick={() => setPage(Math.min(Math.max(totalPages - 1, 0), page + 1))}
              disabled={page + 1 >= totalPages}
              className="flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 text-slate-500 transition hover:border-emerald-200 hover:text-emerald-700 disabled:cursor-not-allowed disabled:opacity-40"
              aria-label="Next page"
            >
              <ChevronRight className="h-4 w-4" />
            </button>

            <label className="sr-only" htmlFor="page-size">
              Rows per page
            </label>
            <select
              id="page-size"
              value={pageSize}
              onChange={(event) => setPageSize(Number(event.target.value))}
              className="ml-1 rounded-2xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-700 outline-none transition focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
            >
              {pageSizeOptions.map((option) => (
                <option key={option} value={option}>
                  {option} / page
                </option>
              ))}
            </select>
          </div>
        </div>
      </section>

      {formMode ? (
        <UserFormModal
          key={`${formMode}-${editingUser?.id ?? "new"}`}
          mode={formMode}
          user={editingUser}
          submitting={submitting}
          onClose={() => {
            setFormMode(null);
            setEditingUser(null);
          }}
          onSubmit={formMode === "edit" ? handleUpdate : handleCreate}
        />
      ) : null}

      <UserDetailDrawer
        isOpen={isDetailOpen}
        user={viewingUser}
        loading={detailLoading}
        onClose={() => {
          setIsDetailOpen(false);
          setViewingUser(null);
        }}
      />

      <ConfirmationModal
        isOpen={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => void handleDelete()}
        title="Delete user?"
        message={
          deleteTarget
            ? `This will permanently delete "${deleteTarget.fullName}".`
            : "This action cannot be undone."
        }
        confirmText="Delete"
        isLoading={submitting}
      />

      <TemporaryPasswordModal
        password={temporaryPassword}
        onClose={() => setTemporaryPassword(null)}
      />

      {toast ? <FloatingToast message={toast.message} variant={toast.variant} /> : null}
    </>
  );
}
