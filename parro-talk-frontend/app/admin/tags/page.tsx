import { Metadata } from 'next';
import TagList from '@/features/cms/components/TagList';

export const metadata: Metadata = {
  title: 'Tag Management | Admin',
  description: 'Manage lesson tags',
};

export default function TagsPage() {
  return (
    <div className="p-6 max-w-7xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Lesson Tags</h1>
          <p className="text-gray-500 text-sm mt-1">Manage tags for lessons</p>
        </div>
      </div>
      <TagList />
    </div>
  );
}
