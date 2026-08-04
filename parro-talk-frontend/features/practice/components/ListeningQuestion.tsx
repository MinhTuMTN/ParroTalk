import React, { useState, useEffect } from 'react';
import { PracticeQuestion } from '../types';
import { Button } from '@/components/ui/Button';
import { Volume2 } from 'lucide-react';
import { Input } from '@/components/ui/Input';

interface ListeningQuestionProps {
  question: PracticeQuestion;
  onAnswer: (answer: string) => void;
}

export const ListeningQuestion: React.FC<ListeningQuestionProps> = ({ question, onAnswer }) => {
  const [value, setValue] = useState('');

  const playAudio = () => {
    if (question.audioUrl) {
      const audio = new Audio(question.audioUrl);
      audio.play().catch(console.error);
    }
  };

  useEffect(() => {
    setValue('');
    // Auto play audio when question loads
    playAudio();
  }, [question]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (value.trim()) {
      onAnswer(value.trim());
    }
  };

  return (
    <div className="flex flex-col items-center justify-center space-y-8 animate-in fade-in zoom-in duration-300 w-full max-w-xl mx-auto">
      <div className="text-center space-y-8">
        <h3 className="text-xl text-slate-500 font-medium">Type what you hear</h3>
        
        <button 
          onClick={playAudio}
          className="p-8 rounded-full bg-indigo-100 hover:bg-indigo-200 dark:bg-indigo-900 dark:hover:bg-indigo-800 transition-colors shadow-lg animate-pulse"
        >
          <Volume2 className="w-16 h-16 text-indigo-600 dark:text-indigo-300" />
        </button>
      </div>

      <form onSubmit={handleSubmit} className="w-full pt-8 space-y-6">
        <Input 
          autoFocus
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder="???????"
          className="text-2xl py-8 text-center rounded-2xl border-2 focus-visible:ring-offset-0 focus-visible:ring-indigo-500 shadow-sm font-mono tracking-widest"
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
