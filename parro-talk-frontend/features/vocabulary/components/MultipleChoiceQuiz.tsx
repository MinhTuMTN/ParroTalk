import React, { useState } from 'react';
import { PracticeQuestionDto } from '../types';

interface Props {
  question: PracticeQuestionDto;
  onAnswer: (correct: boolean) => void;
}

export const MultipleChoiceQuiz: React.FC<Props> = ({ question, onAnswer }) => {
  const [selected, setSelected] = useState<string | null>(null);

  const handleSelect = (option: string) => {
    setSelected(option);
    setTimeout(() => {
      onAnswer(option === question.correctAnswer);
      setSelected(null);
    }, 1200);
  };

  return (
    <div className="p-8 bg-white dark:bg-gray-900 rounded-3xl shadow-xl border border-gray-100 dark:border-gray-800">
      <span className="px-3 py-1 text-xs font-bold rounded-full bg-blue-100 dark:bg-blue-900/50 text-blue-600 dark:text-blue-300">
        Trắc nghiệm Vocabulary
      </span>
      <h2 className="text-2xl font-bold mt-4 mb-6 text-gray-900 dark:text-white">{question.prompt}</h2>

      <div className="space-y-4">
        {question.options?.map((opt, i) => {
          let stateStyle = 'bg-gray-50 dark:bg-gray-800 border-gray-200 dark:border-gray-700 hover:border-blue-500';
          if (selected) {
            if (opt === question.correctAnswer) {
              stateStyle = 'bg-emerald-500 text-white border-emerald-500';
            } else if (opt === selected) {
              stateStyle = 'bg-rose-500 text-white border-rose-500';
            }
          }

          return (
            <button
              key={i}
              disabled={!!selected}
              onClick={() => handleSelect(opt)}
              className={`w-full p-4 text-left font-semibold rounded-2xl border transition-all ${stateStyle}`}
            >
              {opt}
            </button>
          );
        })}
      </div>
    </div>
  );
};