import UserRow from "@/features/users/components/UserRow";
import type { AdminUser } from "@/features/users/types/user";

type UsersTableProps = {
  users: AdminUser[];
  selectedIds: string[];
  currentUserId?: string;
  onSelectAll: (checked: boolean) => void;
  onSelect: (id: string, checked: boolean) => void;
  onView: (user: AdminUser) => void;
  onEdit: (user: AdminUser) => void;
  onToggleStatus: (user: AdminUser) => void;
  onResetPassword: (user: AdminUser) => void;
  onDelete: (user: AdminUser) => void;
};

export default function UsersTable({
  users,
  selectedIds,
  currentUserId,
  onSelectAll,
  onSelect,
  onView,
  onEdit,
  onToggleStatus,
  onResetPassword,
  onDelete,
}: UsersTableProps) {
  const allSelected = users.length > 0 && users.every((user) => selectedIds.includes(user.id));

  return (
    <div className="overflow-x-auto rounded-[24px] border border-slate-200 bg-white shadow-sm">
      <table className="w-full min-w-[1040px]">
        <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-[0.08em] text-slate-500">
          <tr>
            <th className="px-4 py-4">
              <input
                type="checkbox"
                checked={allSelected}
                onChange={(event) => onSelectAll(event.target.checked)}
                aria-label="Select all visible users"
              />
            </th>
            <th className="px-4 py-4">User</th>
            <th className="px-4 py-4">Email</th>
            <th className="px-4 py-4">Role</th>
            <th className="px-4 py-4">Status</th>
            <th className="px-4 py-4">Joined Date</th>
            <th className="px-4 py-4">Last Active</th>
            <th className="px-4 py-4">Actions</th>
          </tr>
        </thead>
        <tbody>
          {users.map((user) => (
            <UserRow
              key={user.id}
              user={user}
              selected={selectedIds.includes(user.id)}
              isCurrentUser={user.id === currentUserId}
              onSelect={onSelect}
              onView={onView}
              onEdit={onEdit}
              onToggleStatus={onToggleStatus}
              onResetPassword={onResetPassword}
              onDelete={onDelete}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
}
