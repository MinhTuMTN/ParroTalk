import { Metadata } from 'next';
import AdminDashboardPage from '@/features/admin/components/AdminDashboardPage';

export const metadata: Metadata = {
  title: 'Admin Dashboard | ParroTalk',
  description: 'Overview of system metrics, moderation and content operations',
};

export default function AdminPage() {
  return (
    <div className="p-6 max-w-7xl mx-auto">
      <AdminDashboardPage />
    </div>
  );
}
