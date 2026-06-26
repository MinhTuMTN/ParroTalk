"use client";

import { useCallback, useEffect, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useDebounce } from "use-debounce";
import { adminUserService } from "@/features/users/services/adminUserService";
import type {
  AdminUser,
  AdminUserRole,
  AdminUserStatus,
} from "@/features/users/types/user";

const DEFAULT_PAGE_SIZE = 10;

const readPage = (value: string | null) => {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0;
};

const readPageSize = (value: string | null) => {
  const parsed = Number(value);
  return [8, 10, 20].includes(parsed) ? parsed : DEFAULT_PAGE_SIZE;
};

const readRole = (value: string | null): AdminUserRole | "all" =>
  value === "ADMIN" || value === "PRO_USER" || value === "USER" ? value : "all";

const readStatus = (value: string | null): AdminUserStatus | "all" =>
  value === "ACTIVE" || value === "INACTIVE" ? value : "all";

export function useUsers(enabled = true) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const [users, setUsers] = useState<AdminUser[]>([]);
  const [searchQuery, setSearchQuery] = useState(searchParams.get("search") ?? "");
  const [debouncedSearch] = useDebounce(searchQuery, 400);
  const [roleFilter, setRoleFilter] = useState<AdminUserRole | "all">(
    readRole(searchParams.get("role")),
  );
  const [statusFilter, setStatusFilter] = useState<AdminUserStatus | "all">(
    readStatus(searchParams.get("status")),
  );
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [page, setPage] = useState(readPage(searchParams.get("page")));
  const [pageSize, setPageSize] = useState(readPageSize(searchParams.get("size")));
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchUsers = useCallback(async () => {
    if (!enabled) return;

    setLoading(true);
    setError(null);

    try {
      const result = await adminUserService.getUsers({
        search: debouncedSearch,
        role: roleFilter,
        status: statusFilter,
        page,
        size: pageSize,
      });

      setUsers(result.users);
      setPage(result.page);
      setPageSize(result.size);
      setTotalItems(result.totalItems);
      setTotalPages(result.totalPages);
      setSelectedIds((current) =>
        current.filter((selectedId) => result.users.some((user) => user.id === selectedId)),
      );
    } catch {
      setError("Unable to load users. Please try again.");
    } finally {
      setLoading(false);
    }
  }, [debouncedSearch, enabled, page, pageSize, roleFilter, statusFilter]);

  useEffect(() => {
    void fetchUsers();
  }, [fetchUsers]);

  useEffect(() => {
    const params = new URLSearchParams(searchParams.toString());

    if (debouncedSearch) params.set("search", debouncedSearch);
    else params.delete("search");

    if (roleFilter !== "all") params.set("role", roleFilter);
    else params.delete("role");

    if (statusFilter !== "all") params.set("status", statusFilter);
    else params.delete("status");

    if (page > 0) params.set("page", page.toString());
    else params.delete("page");

    if (pageSize !== DEFAULT_PAGE_SIZE) params.set("size", pageSize.toString());
    else params.delete("size");

    const nextQuery = params.toString();
    const currentQuery = searchParams.toString();
    if (nextQuery !== currentQuery) {
      router.replace(`${pathname}${nextQuery ? `?${nextQuery}` : ""}`, { scroll: false });
    }
  }, [debouncedSearch, page, pageSize, pathname, roleFilter, router, searchParams, statusFilter]);

  const updateSearchQuery = (value: string) => {
    setPage(0);
    setSearchQuery(value);
  };

  const updateRoleFilter = (value: AdminUserRole | "all") => {
    setPage(0);
    setRoleFilter(value);
  };

  const updateStatusFilter = (value: AdminUserStatus | "all") => {
    setPage(0);
    setStatusFilter(value);
  };

  const updatePageSize = (value: number) => {
    setPage(0);
    setPageSize(value);
  };

  const selectUser = (id: string, checked: boolean) => {
    setSelectedIds((current) =>
      checked ? [...current, id] : current.filter((selectedId) => selectedId !== id),
    );
  };

  const selectAllVisible = (checked: boolean) => {
    const visibleIds = users.map((user) => user.id);

    setSelectedIds((current) => {
      if (!checked) {
        return current.filter((id) => !visibleIds.includes(id));
      }

      return Array.from(new Set([...current, ...visibleIds]));
    });
  };

  return {
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
    refresh: fetchUsers,
    setPage,
    setSearchQuery: updateSearchQuery,
    setRoleFilter: updateRoleFilter,
    setStatusFilter: updateStatusFilter,
    setPageSize: updatePageSize,
    selectUser,
    selectAllVisible,
  };
}
