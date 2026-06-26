import { Suspense } from "react";
import UsersManagementPage from "@/features/users/components/UsersManagementPage";

export default function AdminUsersRoute() {
  return (
    <Suspense fallback={null}>
      <UsersManagementPage />
    </Suspense>
  );
}
