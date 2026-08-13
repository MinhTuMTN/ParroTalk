import React, { useState } from 'react';
import { VocabularyDetailDto } from '../types';

interface Props {
  vocab: VocabularyDetailDto;
  onSave: () => void;
  onReport: () => void;
}

export const VocabularyDetailView: React.FC<Props> = ({ vocab, onSave, onReport }) => {
  const [showVn, setShowVn] = useState(true);

  const playAudio = (url?: string) => {
    if (url) {
      new Audio(url).play().catch((e) => console.error('Audio playback failed', e));
    }
  };

  return (
    <div className="max-w-4xl mx-auto p-8 bg-white dark:bg-gray-900 rounded-3xl shadow-2xl border border-gray-100 dark:border-gray-800">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 mb-8 pb-6 border-b border-gray-100 dark:border-gray-800">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-4xl font-extrabold text-gray-900 dark:text-white tracking-tight">{vocab.word}</h1>
            {vocab.cefrLevel && (
              <span className="px-3.5 py-1 text-xs font-bold rounded-full bg-gradient-to-r from-blue-600 to-indigo-600 text-white shadow-md">
                {vocab.cefrLevel}
              </span>
            )}
          </div>
          <p className="text-xl text-blue-600 dark:text-blue-400 font-mono font-medium mt-1">{vocab.phonetic}</p>
        </div>

        <div className="flex flex-wrap gap-3">
          {vocab.audioUsUrl && (
            <button
              onClick={() => playAudio(vocab.audioUsUrl)}
              className="px-4 py-2.5 rounded-xl bg-gray-100 hover:bg-blue-50 dark:bg-gray-800 dark:hover:bg-gray-700 text-gray-800 dark:text-gray-200 font-medium transition-all flex items-center gap-2"
            >
              🇺🇸 US 🔊
            </button>
          )}
          {vocab.audioUkUrl && (
            <button
              onClick={() => playAudio(vocab.audioUkUrl)}
              className="px-4 py-2.5 rounded-xl bg-gray-100 hover:bg-blue-50 dark:bg-gray-800 dark:hover:bg-gray-700 text-gray-800 dark:text-gray-200 font-medium transition-all flex items-center gap-2"
            >
              🇬🇧 UK 🔊
            </button>
          )}
        </div>
      </div>

      <div className="flex flex-wrap gap-4 mb-8">
        <button
          onClick={onSave}
          className="px-6 py-3 bg-gradient-to-r from-blue-600 to-indigo-600 text-white font-semibold rounded-xl shadow-lg hover:shadow-xl hover:scale-105 transition-all"
        >
          ⭐ Save to My List
        </button>
        <button
          onClick={() => setShowVn(!showVn)}
          className="px-6 py-3 bg-gray-100 dark:bg-gray-800 text-gray-800 dark:text-gray-200 font-semibold rounded-xl hover:bg-gray-200 dark:hover:bg-gray-700 transition-all"
        >
          {showVn ? '👁️ Hide VN Translation' : '👁️ Show VN Translation'}
        </button>
        <button
          onClick={onReport}
          className="px-6 py-3 text-red-600 dark:text-red-400 border border-red-200 dark:border-red-900/50 rounded-xl hover:bg-red-50 dark:hover:bg-red-950/40 font-semibold transition-all"
        >
          🚩 Report Issue
        </button>
      </div>

      {showVn && vocab.commonMeaningVi && (
        <div className="mb-8 p-5 bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-900/40 rounded-2xl text-amber-900 dark:text-amber-200 text-lg font-medium">
          🇻🇳 Nghĩa tiếng Việt: <span className="font-bold">{vocab.commonMeaningVi}</span>
        </div>
      )}

      <div className="space-y-8">
        <DetailSection title="📖 Definitions" items={vocab.definitions} />
        <DetailSection title="💡 Example Sentences" items={vocab.examples} />
        <DetailSection title="🔥 Idioms" items={vocab.idioms} />
        <DetailSection title="🔗 Collocations" items={vocab.collocations} />
        <DetailSection title="⚡ Phrasal Verbs" items={vocab.phrasalVerbs} />
        <DetailSection title="🔄 Synonyms" items={vocab.synonyms} />
        <DetailSection title="🚫 Antonyms" items={vocab.antonyms} />
      </div>
    </div>
  );
};

const DetailSection: React.FC<{ title: string; items?: string[] }> = ({ title, items }) => {
  if (!items || items.length === 0) return null;
  return (
    <div className="p-6 bg-gray-50 dark:bg-gray-800/50 rounded-2xl border border-gray-100 dark:border-gray-700/50">
      <h3 className="text-xl font-bold mb-4 text-gray-900 dark:text-white">{title}</h3>
      <ul className="space-y-2.5">
        {items.map((item, i) => (
          <li key={i} className="text-gray-700 dark:text-gray-300 leading-relaxed flex items-start gap-2">
            <span className="text-blue-500 font-bold">•</span>
            <span>{item}</span>
          </li>
        ))}
      </ul>
    </div>
  );
};