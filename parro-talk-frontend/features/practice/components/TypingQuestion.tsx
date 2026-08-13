import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import React, { useState } from 'react';
import { PracticeQuestion } from '../types';

interface TypingQuestionProps {
  question: PracticeQuestion;
  onAnswer: (answer: string) => void;
}

export const TypingQuestion: React.FC<TypingQuestionProps> = ({ question, onAnswer }) => {
  const [value, setValue] = useState('');
  const [prevQuestion, setPrevQuestion] = useState(question);

  if (question !== prevQuestion) {
    setPrevQuestion(question);
    setValue('');
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (value.trim()) {
      onAnswer(value.trim());
    }
  };

  return (
    <div className="flex flex-col items-center justify-center space-y-8 animate-in fade-in zoom-in duration-300 w-full max-w-xl mx-auto">
      <div className="text-center space-y-4">
        <h3 className="text-xl text-slate-500 font-medium">Type the English word for:</h3>
        <p className="text-3xl font-bold text-slate-800 dark:text-white leading-relaxed">
          {question.definition}
        </p>
      </div>

      <form onSubmit={handleSubmit} className="w-full pt-8 space-y-6">
        <Input 
          autoFocus
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder="Type your answer here..."
          className="text-2xl py-8 text-center rounded-2xl border-2 focus-visible:ring-offset-0 focus-visible:ring-indigo-500 shadow-sm"
        />
        <Button 
          type="submit" 
          disabled={!value.trim()}
          className="w-full py-6 text-lg rounded-xl shadow-md hover:shadow-lg transition-all"
        >
          Check Answer
        </Button>
      </form>
    </div>
  );
};
