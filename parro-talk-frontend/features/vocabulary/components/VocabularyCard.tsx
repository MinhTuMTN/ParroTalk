import React from 'react';
import Link from 'next/link';
import { VocabularySummaryDto } from '../types';

interface Props {
  vocab: VocabularySummaryDto;
  onBookmark?: (id: string) => void;
}

export const VocabularyCard: React.FC<Props> = ({ vocab, onBookmark }) => {
  return (
    <div className="p-6 rounded-2xl bg-white dark:bg-gray-800 border border-gray-100 dark:border-gray-700/60 shadow-lg hover:shadow-xl transition-all duration-300 flex flex-col justify-between">
      <div>
        <div className="flex justify-between items-start mb-3">
          <div>
            <Link href={`/vocabulary/${vocab.id}`}>
              <h3 className="text-2xl font-bold text-gray-900 dark:text-white hover:text-blue-600 dark:hover:text-blue-400 transition-colors">
                {vocab.word}
              </h3>
            </Link>
            <p className="text-sm font-mono text-gray-500 dark:text-gray-400 mt-0.5">{vocab.phonetic}</p>
          </div>
          {vocab.cefrLevel && (
            <span className="px-3 py-1 text-xs font-bold rounded-full bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300">
              {vocab.cefrLevel}
            </span>
          )}
        </div>

        <div className="text-sm text-gray-600 dark:text-gray-300 mb-4">
          <span className="italic font-medium text-gray-400 mr-2">({vocab.partOfSpeech})</span>
          <span className="font-medium text-gray-800 dark:text-gray-200">{vocab.commonMeaningVi}</span>
        </div>
      </div>

      <div className="flex justify-between items-center pt-4 border-t border-gray-100 dark:border-gray-700/50">
        <Link
          href={`/vocabulary/${vocab.id}`}
          className="text-sm font-semibold text-blue-600 dark:text-blue-400 hover:underline"
        >
          Xem chi tiết →
        </Link>
        {onBookmark && (
          <button
            onClick={() => onBookmark(vocab.id)}
            className="p-2 text-gray-400 hover:text-yellow-500 dark:hover:text-yellow-400 transition-colors"
            title="Lưu từ vựng"
          >
            ⭐
          </button>
        )}
      </div>
    </div>
  );
};