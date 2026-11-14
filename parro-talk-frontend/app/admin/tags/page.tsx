import { Metadata } from 'next';
import TagManagementPage from '@/features/cms/components/tags/TagManagementPage';

export const metadata: Metadata = {
  title: 'Tag Management | Admin',
  description: 'Manage lesson tags and taxonomy',
};

export default function TagsPage() {
  return (
    <div className="p-6 max-w-7xl mx-auto">
      <TagManagementPage />
    </div>
  );
}
