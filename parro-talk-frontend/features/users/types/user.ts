export type AdminUserRole = "ADMIN" | "PRO_USER" | "USER";

export type AdminUserStatus = "ACTIVE" | "INACTIVE";

export type AdminUserSummary = {
  id: string;
  fullName: string;
  username: string;
  email: string;
  role: AdminUserRole;
  status: AdminUserStatus;
  avatarUrl?: string | null;
  emailVerified: boolean;
  createdAt: string;
  lastActiveAt?: string | null;
};

export type AdminUser = AdminUserSummary & {
  totalLessonsCompleted: number;
  totalScore: number;
  avgScore: number;
  currentStreak: number;
  longestStreak: number;
};

export type UserListParams = {
  search?: string;
  role?: AdminUserRole | "all";
  status?: AdminUserStatus | "all";
  page?: number;
  size?: number;
};

export type UserListResult = {
  users: AdminUserSummary[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

export type CreateAdminUserInput = {
  fullName: string;
  username: string;
  email: string;
  role: AdminUserRole;
  status: AdminUserStatus;
  password: string;
};

export type UpdateAdminUserInput = Omit<CreateAdminUserInput, "password">;

export type AdminResetPasswordResult = {
  temporaryPassword: string;
};
