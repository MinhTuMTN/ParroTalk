import os

base_dir = r"D:\Projects\english-dictation\parro-talk-frontend"

files = {
    r"features\vocabulary\types\index.ts": """
export interface VocabularyTopicDto {
  id: string;
  name: string;
  icon: string;
  colorTheme: string;
  wordCount: number;
  progress: number;
}

export interface VocabularySummaryDto {
  id: string;
  word: string;
  ipa: string;
  cefrLevel: string;
  pos: string;
  vietnameseMeaning: string;
  isBookmarked: boolean;
  audioUrlUS: string;
  audioUrlUK: string;
}

export interface VocabularyDetailDto extends VocabularySummaryDto {
  definitions: string[];
  exampleSentences: string[];
  idioms: string[];
  collocations: string[];
  phrasalVerbs: string[];
  synonyms: string[];
  antonyms: string[];
}

export interface SaveVocabularyRequest {
  vocabularyId: string;
}

export interface VocabularyReportRequestDto {
  vocabularyId: string;
  issueType: string;
  description: string;
}

export interface PracticeQuestionDto {
  id: string;
  type: string;
  questionText: string;
  options?: string[];
  correctAnswer?: string;
  explanation?: string;
}

export interface PracticeSubmissionDto {
  questionId: string;
  selectedAnswer: string;
}
""",
    r"features\vocabulary\services\api.ts": """
import axios from 'axios';
import { 
  VocabularyTopicDto, 
  VocabularySummaryDto, 
  VocabularyDetailDto, 
  SaveVocabularyRequest, 
  VocabularyReportRequestDto,
  PracticeQuestionDto,
  PracticeSubmissionDto
} from '../types';

const api = axios.create({
  baseURL: '/api'
});

export const getVocabularyTopics = async (): Promise<VocabularyTopicDto[]> => {
  const { data } = await api.get('/vocabulary/topics');
  return data;
};

export const searchVocabulary = async (query: string): Promise<VocabularySummaryDto[]> => {
  const { data } = await api.get('/vocabulary/search', { params: { query } });
  return data;
};

export const getVocabularyDetail = async (id: string): Promise<VocabularyDetailDto> => {
  const { data } = await api.get(`/vocabulary/${id}`);
  return data;
};

export const saveVocabulary = async (req: SaveVocabularyRequest) => {
  const { data } = await api.post('/me/vocabulary/save', req);
  return data;
};

export const reportVocabulary = async (req: VocabularyReportRequestDto) => {
  const { data } = await api.post('/vocabulary/report', req);
  return data;
};

export const getPracticeQuestions = async (): Promise<PracticeQuestionDto[]> => {
  const { data } = await api.get('/vocabulary/practice');
  return data;
};

export const submitPractice = async (req: PracticeSubmissionDto) => {
  const { data } = await api.post('/vocabulary/practice/submit', req);
  return data;
};
""",
    r"features\vocabulary\hooks\index.ts": """
import { useState, useEffect } from 'react';
import * as api from '../services/api';
import { VocabularyTopicDto, VocabularySummaryDto, VocabularyDetailDto, PracticeQuestionDto } from '../types';

export const useVocabularyTopics = () => {
  const [topics, setTopics] = useState<VocabularyTopicDto[]>([]);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    api.getVocabularyTopics().then(setTopics).finally(() => setLoading(false));
  }, []);
  
  return { topics, loading };
};

export const useVocabularySearch = () => {
  const [results, setResults] = useState<VocabularySummaryDto[]>([]);
  const [loading, setLoading] = useState(false);
  
  const search = async (query: string) => {
    setLoading(true);
    const data = await api.searchVocabulary(query);
    setResults(data);
    setLoading(false);
  };
  
  return { search, results, loading };
};

export const useVocabularyDetail = (id: string) => {
  const [detail, setDetail] = useState<VocabularyDetailDto | null>(null);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    if(id) {
       api.getVocabularyDetail(id).then(setDetail).finally(() => setLoading(false));
    }
  }, [id]);
  
  return { detail, loading };
};

export const useBookmarkVocabulary = () => {
  const bookmark = async (id: string) => {
    await api.saveVocabulary({ vocabularyId: id });
  };
  return { bookmark };
};

export const useVocabularyPractice = () => {
  const [questions, setQuestions] = useState<PracticeQuestionDto[]>([]);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    api.getPracticeQuestions().then(setQuestions).finally(() => setLoading(false));
  }, []);
  
  return { questions, loading };
};
""",
    r"features\vocabulary\components\TopicCard.tsx": """
import React from 'react';
import { VocabularyTopicDto } from '../types';

interface Props {
  topic: VocabularyTopicDto;
}

export const TopicCard: React.FC<Props> = ({ topic }) => {
  return (
    <div className={`p-4 rounded-xl shadow-md cursor-pointer transition-transform hover:scale-105 backdrop-blur-md bg-opacity-70 dark:bg-gray-800 ${topic.colorTheme}`}>
      <div className="text-3xl mb-2">{topic.icon}</div>
      <h3 className="text-lg font-semibold text-gray-900 dark:text-white">{topic.name}</h3>
      <p className="text-sm text-gray-500 dark:text-gray-400">{topic.wordCount} words</p>
      <div className="mt-4 w-full bg-gray-200 rounded-full h-2 dark:bg-gray-700">
        <div className="bg-blue-600 h-2 rounded-full" style={{ width: `${topic.progress}%` }}></div>
      </div>
    </div>
  );
};
""",
    r"features\vocabulary\components\TopicGrid.tsx": """
import React from 'react';
import { VocabularyTopicDto } from '../types';
import { TopicCard } from './TopicCard';

interface Props {
  topics: VocabularyTopicDto[];
}

export const TopicGrid: React.FC<Props> = ({ topics }) => {
  return (
    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-6">
      {topics.map(topic => <TopicCard key={topic.id} topic={topic} />)}
    </div>
  );
};
""",
    r"features\vocabulary\components\VocabularySearchBar.tsx": """
import React, { useState } from 'react';

export const VocabularySearchBar: React.FC<{ onSearch: (q: string) => void }> = ({ onSearch }) => {
  const [query, setQuery] = useState('');
  
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    onSearch(query);
  };
  
  return (
    <form onSubmit={handleSearch} className="flex gap-2 mb-8">
      <input 
        type="text" 
        value={query}
        onChange={e => setQuery(e.target.value)}
        placeholder="Search vocabulary..."
        className="flex-1 p-3 rounded-lg border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white focus:ring-2 focus:ring-blue-500 transition-shadow glassmorphism"
      />
      <button type="submit" className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
        Search
      </button>
    </form>
  );
};
""",
    r"features\vocabulary\components\VocabularyCard.tsx": """
import React from 'react';
import { VocabularySummaryDto } from '../types';
import Link from 'next/link';

interface Props {
  vocab: VocabularySummaryDto;
  onBookmark: (id: string) => void;
}

export const VocabularyCard: React.FC<Props> = ({ vocab, onBookmark }) => {
  return (
    <div className="p-5 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-800 dark:bg-gray-900 glassmorphism relative group">
      <div className="flex justify-between items-start">
        <div>
          <Link href={`/vocabulary/${vocab.id}`}>
            <h3 className="text-xl font-bold text-gray-900 dark:text-white hover:text-blue-500 cursor-pointer">{vocab.word}</h3>
          </Link>
          <p className="text-gray-500 dark:text-gray-400 font-mono text-sm">{vocab.ipa}</p>
        </div>
        <button onClick={() => onBookmark(vocab.id)} className="text-gray-400 hover:text-yellow-500 transition-colors">
          {vocab.isBookmarked ? '★' : '☆'}
        </button>
      </div>
      <div className="mt-3 flex gap-2">
        <span className="px-2 py-1 text-xs rounded bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200">{vocab.cefrLevel}</span>
        <span className="px-2 py-1 text-xs rounded bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200">{vocab.pos}</span>
      </div>
      <p className="mt-4 text-gray-700 dark:text-gray-300">{vocab.vietnameseMeaning}</p>
    </div>
  );
};
""",
    r"features\vocabulary\components\VocabularyDetailView.tsx": """
import React, { useState } from 'react';
import { VocabularyDetailDto } from '../types';

export const VocabularyDetailView: React.FC<{ vocab: VocabularyDetailDto, onSave: () => void, onReport: () => void }> = ({ vocab, onSave, onReport }) => {
  const [showVn, setShowVn] = useState(false);
  
  return (
    <div className="max-w-4xl mx-auto p-6 bg-white dark:bg-gray-900 rounded-2xl shadow-lg">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-4xl font-extrabold text-gray-900 dark:text-white">{vocab.word}</h1>
          <p className="text-lg text-gray-500 dark:text-gray-400 font-mono mt-1">{vocab.ipa}</p>
        </div>
        <div className="flex gap-4">
          <button onClick={() => new Audio(vocab.audioUrlUS).play()} className="p-2 rounded-full bg-gray-100 hover:bg-gray-200 dark:bg-gray-800">🇺🇸 🔊</button>
          <button onClick={() => new Audio(vocab.audioUrlUK).play()} className="p-2 rounded-full bg-gray-100 hover:bg-gray-200 dark:bg-gray-800">🇬🇧 🔊</button>
        </div>
      </div>
      
      <div className="flex gap-4 mb-8">
        <button onClick={onSave} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">Save to My List</button>
        <button onClick={() => setShowVn(!showVn)} className="px-4 py-2 bg-gray-200 dark:bg-gray-800 rounded-lg">Toggle VN Translation</button>
        <button onClick={onReport} className="px-4 py-2 text-red-500 border border-red-500 rounded-lg hover:bg-red-50">Report Issue</button>
      </div>
      
      {showVn && <div className="mb-6 p-4 bg-yellow-50 dark:bg-yellow-900 rounded-lg text-lg">{vocab.vietnameseMeaning}</div>}
      
      <div className="space-y-6">
        <Section title="Definitions" items={vocab.definitions} />
        <Section title="Examples" items={vocab.exampleSentences} />
        <Section title="Idioms" items={vocab.idioms} />
        <Section title="Collocations" items={vocab.collocations} />
        <Section title="Synonyms & Antonyms" items={[...vocab.synonyms, ...vocab.antonyms]} />
      </div>
    </div>
  );
};

const Section: React.FC<{title: string, items: string[]}> = ({ title, items }) => {
  if(!items || items.length === 0) return null;
  return (
    <div>
      <h3 className="text-xl font-bold mb-3 dark:text-white">{title}</h3>
      <ul className="list-disc pl-5 space-y-2 dark:text-gray-300">
        {items.map((item, i) => <li key={i}>{item}</li>)}
      </ul>
    </div>
  );
};
""",
    r"features\vocabulary\components\ReportModal.tsx": """
import React, { useState } from 'react';

export const ReportModal: React.FC<{ isOpen: boolean, onClose: () => void, onSubmit: (type: string, desc: string) => void }> = ({ isOpen, onClose, onSubmit }) => {
  const [type, setType] = useState('TYPO');
  const [desc, setDesc] = useState('');
  
  if(!isOpen) return null;
  
  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white dark:bg-gray-900 p-6 rounded-xl max-w-md w-full">
        <h2 className="text-2xl font-bold mb-4 dark:text-white">Report Issue</h2>
        <select value={type} onChange={e => setType(e.target.value)} className="w-full mb-4 p-2 rounded border dark:bg-gray-800 dark:text-white">
          <option value="TYPO">Typo</option>
          <option value="WRONG_MEANING">Wrong Meaning</option>
          <option value="AUDIO_ERROR">Audio Error</option>
        </select>
        <textarea value={desc} onChange={e => setDesc(e.target.value)} placeholder="Description..." className="w-full mb-4 p-2 border rounded h-32 dark:bg-gray-800 dark:text-white"></textarea>
        <div className="flex justify-end gap-2">
          <button onClick={onClose} className="px-4 py-2 rounded bg-gray-200 dark:bg-gray-800">Cancel</button>
          <button onClick={() => { onSubmit(type, desc); onClose(); }} className="px-4 py-2 rounded bg-blue-600 text-white">Submit</button>
        </div>
      </div>
    </div>
  );
};
""",
    r"features\vocabulary\components\MultipleChoiceQuiz.tsx": """
import React from 'react';
import { PracticeQuestionDto } from '../types';

export const MultipleChoiceQuiz: React.FC<{ question: PracticeQuestionDto, onAnswer: (ans: string) => void }> = ({ question, onAnswer }) => {
  return (
    <div className="p-6 bg-white dark:bg-gray-800 rounded-xl shadow-lg text-center max-w-2xl mx-auto">
      <h3 className="text-2xl font-bold mb-8 dark:text-white">{question.questionText}</h3>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {question.options?.map(opt => (
          <button key={opt} onClick={() => onAnswer(opt)} className="p-4 rounded-lg border-2 border-blue-500 text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900 transition-colors font-medium text-lg">
            {opt}
          </button>
        ))}
      </div>
    </div>
  );
};
""",
    r"features\vocabulary\components\FlashcardDeck.tsx": """
import React, { useState } from 'react';
import { PracticeQuestionDto } from '../types';

export const FlashcardDeck: React.FC<{ card: PracticeQuestionDto, onRate: (rate: string) => void }> = ({ card, onRate }) => {
  const [flipped, setFlipped] = useState(false);
  
  return (
    <div className="max-w-md mx-auto">
      <div 
        className="w-full aspect-[3/4] perspective-1000 cursor-pointer group"
        onClick={() => setFlipped(!flipped)}
      >
        <div className={`relative w-full h-full transition-transform duration-700 preserve-3d ${flipped ? 'rotate-y-180' : ''}`}>
          {/* Front */}
          <div className="absolute w-full h-full backface-hidden bg-gradient-to-br from-blue-500 to-purple-600 rounded-2xl shadow-xl flex items-center justify-center p-8 text-center text-white">
            <h2 className="text-4xl font-bold">{card.questionText}</h2>
          </div>
          {/* Back */}
          <div className="absolute w-full h-full backface-hidden bg-white dark:bg-gray-800 rounded-2xl shadow-xl flex flex-col items-center justify-center p-8 text-center rotate-y-180">
            <h2 className="text-3xl font-bold mb-4 dark:text-white">{card.explanation}</h2>
          </div>
        </div>
      </div>
      
      {flipped && (
        <div className="flex justify-center gap-4 mt-8">
          <button onClick={() => { onRate('HARD'); setFlipped(false); }} className="px-6 py-2 rounded-full bg-red-100 text-red-600 font-bold hover:bg-red-200 transition-colors">Hard</button>
          <button onClick={() => { onRate('MEDIUM'); setFlipped(false); }} className="px-6 py-2 rounded-full bg-yellow-100 text-yellow-600 font-bold hover:bg-yellow-200 transition-colors">Medium</button>
          <button onClick={() => { onRate('EASY'); setFlipped(false); }} className="px-6 py-2 rounded-full bg-green-100 text-green-600 font-bold hover:bg-green-200 transition-colors">Easy</button>
        </div>
      )}
    </div>
  );
};
""",
    r"features\vocabulary\components\ClozeTestPractice.tsx": """
import React, { useState } from 'react';
import { PracticeQuestionDto } from '../types';

export const ClozeTestPractice: React.FC<{ question: PracticeQuestionDto, onCorrect: () => void }> = ({ question, onCorrect }) => {
  const [val, setVal] = useState('');
  
  const check = () => {
    if(val.toLowerCase() === question.correctAnswer?.toLowerCase()) {
      onCorrect();
      setVal('');
    }
  };
  
  return (
    <div className="p-6 bg-white dark:bg-gray-800 rounded-xl shadow-lg max-w-2xl mx-auto text-center">
      <h3 className="text-xl mb-6 dark:text-white leading-relaxed">{question.questionText}</h3>
      <div className="flex justify-center gap-4">
        <input 
          type="text" 
          value={val}
          onChange={e => setVal(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && check()}
          className="px-4 py-2 border-b-2 border-gray-400 focus:border-blue-500 bg-transparent text-center text-2xl outline-none dark:text-white w-48 font-mono tracking-widest"
          placeholder="_ _ _ _ _"
        />
        <button onClick={check} className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">Check</button>
      </div>
    </div>
  );
};
""",
    r"app\(main)\vocabulary\page.tsx": """
"use client";
import React from 'react';
import { TopicGrid } from '../../../features/vocabulary/components/TopicGrid';
import { VocabularySearchBar } from '../../../features/vocabulary/components/VocabularySearchBar';
import { VocabularyCard } from '../../../features/vocabulary/components/VocabularyCard';
import { useVocabularyTopics, useVocabularySearch, useBookmarkVocabulary } from '../../../features/vocabulary/hooks';

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
""",
    r"app\(main)\vocabulary\[id]\page.tsx": """
"use client";
import React, { useState } from 'react';
import { useParams } from 'next/navigation';
import { VocabularyDetailView } from '../../../../features/vocabulary/components/VocabularyDetailView';
import { ReportModal } from '../../../../features/vocabulary/components/ReportModal';
import { useVocabularyDetail, useBookmarkVocabulary } from '../../../../features/vocabulary/hooks';
import * as api from '../../../../features/vocabulary/services/api';

export default function VocabularyDetailPage() {
  const params = useParams();
  const id = Array.isArray(params.id) ? params.id[0] : params.id;
  const { detail, loading } = useVocabularyDetail(id || '');
  const { bookmark } = useBookmarkVocabulary();
  const [reportOpen, setReportOpen] = useState(false);
  
  if(loading || !detail) return <div className="p-8 text-center">Loading...</div>;
  
  return (
    <div className="p-8">
      <VocabularyDetailView 
        vocab={detail} 
        onSave={() => bookmark(detail.id)} 
        onReport={() => setReportOpen(true)} 
      />
      <ReportModal 
        isOpen={reportOpen} 
        onClose={() => setReportOpen(false)} 
        onSubmit={(type, desc) => api.reportVocabulary({ vocabularyId: detail.id, issueType: type, description: desc })}
      />
    </div>
  );
}
""",
    r"app\(main)\vocabulary\practice\page.tsx": """
"use client";
import React, { useState } from 'react';
import { useVocabularyPractice } from '../../../../features/vocabulary/hooks';
import { MultipleChoiceQuiz } from '../../../../features/vocabulary/components/MultipleChoiceQuiz';
import { FlashcardDeck } from '../../../../features/vocabulary/components/FlashcardDeck';
import { ClozeTestPractice } from '../../../../features/vocabulary/components/ClozeTestPractice';

export default function PracticePage() {
  const { questions, loading } = useVocabularyPractice();
  const [idx, setIdx] = useState(0);
  
  if(loading) return <div className="p-8 text-center">Loading...</div>;
  if(!questions || questions.length === 0) return <div className="p-8 text-center">No practice questions available.</div>;
  
  const q = questions[idx];
  
  const next = () => setIdx((i) => (i + 1) % questions.length);
  
  return (
    <div className="p-8 max-w-4xl mx-auto h-screen flex flex-col justify-center">
      <h1 className="text-3xl font-bold mb-12 text-center dark:text-white">Practice Area</h1>
      
      <div className="flex-1">
        {q.type === 'QUIZ' && <MultipleChoiceQuiz question={q} onAnswer={next} />}
        {q.type === 'FLASHCARD' && <FlashcardDeck card={q} onRate={next} />}
        {q.type === 'CLOZE' && <ClozeTestPractice question={q} onCorrect={next} />}
      </div>
      
      <div className="mt-8 text-center text-gray-500">
        Question {idx + 1} of {questions.length}
      </div>
    </div>
  );
}
"""
}

for rel_path, content in files.items():
    full_path = os.path.join(base_dir, rel_path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, 'w', encoding='utf-8') as f:
        f.write(content.strip() + "\\n")

print("Files generated successfully!")
