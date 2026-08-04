import { Metadata } from 'next';
import CmsDashboard from '@/features/cms/components/CmsDashboard';
import Link from 'next/link';

export const metadata: Metadata = {
  title: 'CMS Dashboard | Admin',
  description: 'Lesson categories and tags statistics',
};

export default function CmsDashboardPage() {
  return (
    <div className="p-6 max-w-7xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">CMS Dashboard</h1>
          <p className="text-gray-500 text-sm mt-1">Overview of lesson classification</p>
        </div>
        <div className="flex gap-3">
          <Link href="/admin/categories" className="px-4 py-2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors">
            Manage Categories
          </Link>
          <Link href="/admin/tags" className="px-4 py-2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors">
            Manage Tags
          </Link>
        </div>
      </div>
      <CmsDashboard />
    </div>
  );
}
