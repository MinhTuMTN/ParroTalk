"use client";

import React, { useState } from 'react';
import { useVocabularyPractice } from '../../../../features/vocabulary/hooks';
import { MultipleChoiceQuiz } from '../../../../features/vocabulary/components/MultipleChoiceQuiz';
import { FlashcardDeck } from '../../../../features/vocabulary/components/FlashcardDeck';
import { ClozeTestPractice } from '../../../../features/vocabulary/components/ClozeTestPractice';

export default function PracticePage() {
  const { questions, loading } = useVocabularyPractice();
  const [idx, setIdx] = useState(0);
  
  if (loading) return <div className="p-8 text-center text-gray-500 dark:text-gray-400">Loading practice questions...</div>;
  if (!questions || questions.length === 0) return <div className="p-8 text-center text-gray-500 dark:text-gray-400">No practice questions available.</div>;
  
  const q = questions[idx];
  const next = () => setIdx((i) => (i + 1) % questions.length);
  
  return (
    <div className="p-8 max-w-4xl mx-auto min-h-screen flex flex-col justify-center">
      <h1 className="text-3xl font-bold mb-12 text-center dark:text-white">Vocabulary Practice Area</h1>
      
      <div className="flex-1">
        {q.questionType === 'MULTIPLE_CHOICE' && <MultipleChoiceQuiz question={q} onAnswer={next} />}
        {q.questionType === 'FLASHCARD' && <FlashcardDeck card={q} onRate={next} />}
        {q.questionType === 'FILL_IN_BLANKS' && <ClozeTestPractice question={q} onCorrect={next} />}
      </div>
      
      <div className="mt-8 text-center text-gray-500 font-medium">
        Question {idx + 1} of {questions.length}
      </div>
    </div>
  );
}