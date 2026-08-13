import React, { useState } from 'react';
import { PracticeQuestionDto } from '../types';

interface Props {
  question: PracticeQuestionDto;
  onCorrect: () => void;
}

export const ClozeTestPractice: React.FC<Props> = ({ question, onCorrect }) => {
  const [input, setInput] = useState('');
  const [isError, setIsError] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (input.trim().toLowerCase() === question.correctAnswer.toLowerCase()) {
      setIsError(false);
      setInput('');
      onCorrect();
    } else {
      setIsError(true);
    }
  };

  return (
    <div className="p-8 bg-white dark:bg-gray-900 rounded-3xl shadow-xl border border-gray-100 dark:border-gray-800 max-w-xl mx-auto">
      <span className="px-3 py-1 text-xs font-bold rounded-full bg-indigo-100 dark:bg-indigo-900/50 text-indigo-600 dark:text-indigo-300">
        Điền từ Cloze Test
      </span>
      <h2 className="text-xl font-bold mt-4 mb-6 text-gray-900 dark:text-white leading-relaxed">{question.prompt}</h2>

      {question.wordHint && (
        <div className="mb-6 p-4 rounded-xl bg-gray-50 dark:bg-gray-800 text-center font-mono text-2xl tracking-widest text-blue-600 dark:text-blue-400">
          {question.wordHint}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <input
          type="text"
          value={input}
          onChange={(e) => { setInput(e.target.value); setIsError(false); }}
          placeholder="Nhập từ Tiếng Anh chính xác..."
          className={`w-full p-4 text-lg rounded-2xl border text-center font-semibold transition-all dark:bg-gray-800 dark:text-white ${isError ? 'border-rose-500 focus:ring-rose-500' : 'border-gray-200 dark:border-gray-700 focus:ring-blue-500'}`}
        />

        {isError && <p className="text-sm font-semibold text-rose-500 text-center">Chưa chính xác! Thử lại hoặc dùng gợi ý.</p>}

        <button
          type="submit"
          className="w-full py-4 bg-gradient-to-r from-blue-600 to-indigo-600 text-white font-bold text-lg rounded-2xl shadow-lg hover:shadow-xl transition-all"
        >
          Xác nhận câu trả lời
        </button>
      </form>
    </div>
  );
};