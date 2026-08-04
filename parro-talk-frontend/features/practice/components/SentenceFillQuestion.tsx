import React, { useState } from 'react';
import { PracticeQuestion } from '../types';
import { Button } from '@/components/ui/Button';

interface SentenceFillQuestionProps {
  question: PracticeQuestion;
  onAnswer: (answer: string) => void;
}

export const SentenceFillQuestion: React.FC<SentenceFillQuestionProps> = ({ question, onAnswer }) => {
  return (
    <div className="flex flex-col items-center justify-center space-y-8 animate-in fade-in zoom-in duration-300 w-full max-w-2xl mx-auto">
      <div className="text-center space-y-4">
        <h3 className="text-xl text-slate-500 font-medium">Fill in the blank</h3>
        <p className="text-3xl font-bold text-slate-800 dark:text-white max-w-lg leading-relaxed">
          {question.sentenceTemplate}
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 w-full pt-8">
        {question.options?.map((option, index) => (
          <Button 
            key={index}
            variant="outline"
            onClick={() => onAnswer(option)}
            className="py-12 text-xl font-medium rounded-2xl hover:border-indigo-500 hover:bg-indigo-50 dark:hover:bg-indigo-950/30 transition-all shadow-sm"
          >
            {option}
          </Button>
        ))}
      </div>
    </div>
  );
};
