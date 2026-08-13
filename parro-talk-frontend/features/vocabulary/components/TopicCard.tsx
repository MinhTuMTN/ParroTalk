import React from 'react';
import Link from 'next/link';
import { VocabularyTopicDto } from '../types';

interface Props {
  topic: VocabularyTopicDto;
}

export const TopicCard: React.FC<Props> = ({ topic }) => {
  return (
    <Link href={`/vocabulary?topic=${encodeURIComponent(topic.name)}`}>
      <div className="group relative p-6 rounded-2xl bg-white dark:bg-gray-800/80 border border-gray-100 dark:border-gray-700/50 shadow-md hover:shadow-2xl hover:-translate-y-1 transition-all duration-300 backdrop-blur-md overflow-hidden cursor-pointer">
        <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-to-bl from-blue-500/10 to-indigo-500/0 rounded-bl-full group-hover:scale-110 transition-transform" />
        <div className="text-4xl mb-4 transform group-hover:scale-110 transition-transform duration-300">
          📚
        </div>
        <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-2 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
          {topic.name}
        </h3>
        <div className="flex justify-between items-center text-xs font-semibold text-gray-500 dark:text-gray-400 mt-4 pt-3 border-t border-gray-100 dark:border-gray-700/50">
          <span>{topic.wordCount} Từ vựng</span>
          <span className="text-blue-500 group-hover:translate-x-1 transition-transform">Khám phá →</span>
        </div>
      </div>
    </Link>
  );
};