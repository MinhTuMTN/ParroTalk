import { Metadata } from 'next';
import ReportsManagementPage from '@/features/reports/components/ReportsManagementPage';

export const metadata: Metadata = {
  title: 'Reports & Feedback | Admin',
  description: 'Moderate lesson issue reports and user feedback',
};

export default function AdminReportsPage() {
  return (
    <div className="p-6 max-w-7xl mx-auto">
      <ReportsManagementPage />
    </div>
  );
}
