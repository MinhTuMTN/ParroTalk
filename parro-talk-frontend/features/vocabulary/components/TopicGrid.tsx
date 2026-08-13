import React from 'react';
import { VocabularyTopicDto } from '../types';
import { TopicCard } from './TopicCard';

interface Props {
  topics: VocabularyTopicDto[];
}

export const TopicGrid: React.FC<Props> = ({ topics }) => {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-6">
      {topics.map((topic, i) => (
        <TopicCard key={topic.name || i} topic={topic} />
      ))}
    </div>
  );
};