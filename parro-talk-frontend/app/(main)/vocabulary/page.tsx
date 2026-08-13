"use client";

import { TopicGrid } from '@/features/vocabulary/components/TopicGrid';
import { VocabularyCard } from '@/features/vocabulary/components/VocabularyCard';
import { VocabularySearchBar } from '@/features/vocabulary/components/VocabularySearchBar';
import { useBookmarkVocabulary, useVocabularySearch, useVocabularyTopics } from '@/features/vocabulary/hooks';

export default function VocabularyPage() {
  const { topics } = useVocabularyTopics();
  const { search, results } = useVocabularySearch();
  const { bookmark } = useBookmarkVocabulary();
  
  return (
    <div className="p-8 max-w-7xl mx-auto">
      <h1 className="text-4xl font-extrabold mb-8 text-gray-900 dark:text-white">Vocabulary Center</h1>
      <VocabularySearchBar onSearch={search} />
      
      {results.length > 0 ? (
        <div className="mb-12">
          <h2 className="text-2xl font-bold mb-6 dark:text-white">Search Results</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
             {results.map(r => <VocabularyCard key={r.id} vocab={r} onBookmark={bookmark} />)}
          </div>
        </div>
      ) : null}

      <div>
        <h2 className="text-2xl font-bold mb-6 dark:text-white">Topics & Categories</h2>
        <TopicGrid topics={topics} />
      </div>
    </div>
  );
}