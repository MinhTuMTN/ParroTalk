import { Metadata } from 'next';
import CategoryList from '@/features/cms/components/CategoryList';

export const metadata: Metadata = {
  title: 'Category Management | Admin',
  description: 'Manage lesson categories',
};

export default function CategoriesPage() {
  return (
    <div className="p-6 max-w-7xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Lesson Categories</h1>
          <p className="text-gray-500 text-sm mt-1">Manage hierarchical lesson categories</p>
        </div>
      </div>
      <CategoryList />
    </div>
  );
}
