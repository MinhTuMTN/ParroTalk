import React, { useState } from 'react';
import { PracticeQuestion, Sm2Rating } from '../types';
import { Button } from '@/components/ui/Button';
import { Volume2 } from 'lucide-react';
import ReactPlayer from 'react-player';

interface FlashcardQuestionProps {
  question: PracticeQuestion;
  onAnswer: (answer: string, rating: Sm2Rating) => void;
}

export const FlashcardQuestion: React.FC<FlashcardQuestionProps> = ({ question, onAnswer }) => {
  const [showMeaning, setShowMeaning] = useState(false);

  const playAudio = () => {
    if (question.audioUrl) {
      const audio = new Audio(question.audioUrl);
      audio.play().catch(console.error);
    }
  };

  if (!showMeaning) {
    return (
      <div className="flex flex-col items-center justify-center space-y-8 animate-in fade-in zoom-in duration-300">
        <h2 className="text-5xl font-bold text-slate-800 dark:text-white tracking-tight">{question.displayWord}</h2>
        
        <div className="flex items-center space-x-4">
          {question.phonetic && (
            <span className="text-xl text-slate-500 font-mono">/{question.phonetic}/</span>
          )}
          {question.audioUrl && (
            <button 
              onClick={playAudio}
              className="p-2 rounded-full bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 transition-colors"
            >
              <Volume2 className="w-6 h-6 text-indigo-500" />
            </button>
          )}
        </div>

        {question.partOfSpeech && (
          <span className="px-3 py-1 rounded-full bg-indigo-100 text-indigo-700 text-sm font-medium">
            {question.partOfSpeech}
          </span>
        )}

        <div className="pt-12 w-full max-w-md">
          <Button 
            onClick={() => setShowMeaning(true)} 
            className="w-full py-6 text-lg rounded-xl shadow-lg hover:shadow-xl transition-all"
          >
            Show Meaning
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-300">
      <div className="text-center space-y-4">
        <h2 className="text-4xl font-bold text-slate-800 dark:text-white">{question.displayWord}</h2>
        <p className="text-2xl text-slate-600 dark:text-slate-300 max-w-lg">{question.definition}</p>
      </div>

      <div className="pt-8 w-full max-w-2xl space-y-4">
        <h3 className="text-center text-slate-500 font-medium mb-4">How well did you remember?</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <Button variant="outline" onClick={() => onAnswer('', 'AGAIN')} className="py-8 text-red-500 border-red-200 hover:bg-red-50 hover:border-red-500">
            <div className="flex flex-col items-center">
              <span className="font-bold text-lg">Again</span>
              <span className="text-xs opacity-70 mt-1">&lt; 1m</span>
            </div>
          </Button>
          <Button variant="outline" onClick={() => onAnswer('', 'HARD')} className="py-8 text-orange-500 border-orange-200 hover:bg-orange-50 hover:border-orange-500">
            <div className="flex flex-col items-center">
              <span className="font-bold text-lg">Hard</span>
              <span className="text-xs opacity-70 mt-1">~ 10m</span>
            </div>
          </Button>
          <Button variant="outline" onClick={() => onAnswer('', 'GOOD')} className="py-8 text-green-500 border-green-200 hover:bg-green-50 hover:border-green-500">
            <div className="flex flex-col items-center">
              <span className="font-bold text-lg">Good</span>
              <span className="text-xs opacity-70 mt-1">~ 1d</span>
            </div>
          </Button>
          <Button variant="outline" onClick={() => onAnswer('', 'EASY')} className="py-8 text-blue-500 border-blue-200 hover:bg-blue-50 hover:border-blue-500">
            <div className="flex flex-col items-center">
              <span className="font-bold text-lg">Easy</span>
              <span className="text-xs opacity-70 mt-1">~ 4d</span>
            </div>
          </Button>
        </div>
      </div>
    </div>
  );
};
