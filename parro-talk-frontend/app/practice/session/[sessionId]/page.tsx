'use client';

import React, { useEffect, useState, use } from 'react';
import { useRouter } from 'next/navigation';
import { PracticeSession, PracticeQuestion, Sm2Rating, AnswerResult } from '@/features/practice/types';
import { practiceApi } from '@/features/practice/services/api';
import { FlashcardQuestion } from '@/features/practice/components/FlashcardQuestion';
import { MultipleChoiceQuestion } from '@/features/practice/components/MultipleChoiceQuestion';
import { TypingQuestion } from '@/features/practice/components/TypingQuestion';
import { ListeningQuestion } from '@/features/practice/components/ListeningQuestion';
import { SentenceFillQuestion } from '@/features/practice/components/SentenceFillQuestion';
import { MatchQuestion } from '@/features/practice/components/MatchQuestion';
import { Button } from '@/components/ui/Button';
import { CheckCircle2, XCircle } from 'lucide-react';
import confetti from 'canvas-confetti';

export default function PracticeRunner({ params }: { params: Promise<{ sessionId: string }> }) {
  const router = useRouter();
  const { sessionId } = use(params);

  const [session, setSession] = useState<PracticeSession | null>(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [result, setResult] = useState<AnswerResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [sessionFinished, setSessionFinished] = useState(false);
  const [startTime, setStartTime] = useState(Date.now());

  useEffect(() => {
    const stored = sessionStorage.getItem(`practice_session_${sessionId}`);
    if (stored) {
      setSession(JSON.parse(stored));
      setStartTime(Date.now());
    } else {
      router.push('/practice');
    }
  }, [sessionId, router]);

  const currentQuestion = session?.questions[currentIndex];

  const handleAnswer = async (answer: string, rating?: Sm2Rating) => {
    if (!session || !currentQuestion) return;
    setLoading(true);

    try {
      const res = await practiceApi.submitAnswer({
        sessionId: session.sessionId,
        userVocabularyId: currentQuestion.userVocabularyId,
        answer,
        rating,
        timeSpentMs: Date.now() - startTime
      });
      setResult(res);
      if (res.correct) {
        confetti({ particleCount: 50, spread: 60, origin: { y: 0.8 }, colors: ['#4f46e5', '#818cf8'] });
      }
    } catch (error) {
      console.error('Failed to submit answer', error);
      alert('Failed to submit answer');
    } finally {
      setLoading(false);
    }
  };

  const nextQuestion = () => {
    setResult(null);
    setStartTime(Date.now());
    if (session && currentIndex < session.questions.length - 1) {
      setCurrentIndex(curr => curr + 1);
    } else {
      setSessionFinished(true);
      confetti({ particleCount: 200, spread: 100, origin: { y: 0.6 } });
    }
  };

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (result && e.key === 'Enter') {
        e.preventDefault();
        nextQuestion();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [result]);

  if (!session) {
    return <div className="h-screen flex items-center justify-center animate-pulse text-indigo-500">Loading...</div>;
  }

  if (sessionFinished) {
    return (
      <div className="h-screen flex flex-col items-center justify-center space-y-8 bg-slate-50 dark:bg-slate-950 p-4">
        <h1 className="text-4xl md:text-6xl font-bold text-slate-800 dark:text-white">Session Complete! 🎉</h1>
        <div className="p-8 bg-white dark:bg-slate-900 rounded-3xl shadow-xl space-y-4 text-center">
          <p className="text-xl text-slate-600 dark:text-slate-300">Great job! You&apos;ve reviewed {session.questions.length} words.</p>
          <Button 
            size="lg" 
            onClick={() => router.push('/practice')}
            className="w-full mt-6 py-6 text-xl rounded-xl shadow-md hover:shadow-lg transition-all"
          >
            Back to Dashboard
          </Button>
        </div>
      </div>
    );
  }

  if (!currentQuestion) return null;

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-slate-950">
      <div className="p-4 md:p-8">
        <div className="max-w-4xl mx-auto flex items-center space-x-4">
          <div className="w-full h-3 bg-slate-200 dark:bg-slate-800 rounded-full overflow-hidden">
            <div 
              className="h-full bg-indigo-500 transition-all duration-300"
              style={{ width: `${(currentIndex / session.questions.length) * 100}%` }}
            />
          </div>
          <span className="text-sm font-bold text-slate-500 w-16 text-right">
            {currentIndex + 1} / {session.questions.length}
          </span>
        </div>
      </div>

      <div className="flex-1 flex flex-col items-center justify-center p-4">
        <div className="w-full max-w-4xl min-h-[400px] flex items-center justify-center">
          {!result ? (
            <>
              {currentQuestion.questionType === 'FLASHCARD' && <FlashcardQuestion question={currentQuestion} onAnswer={(a, r) => handleAnswer(a, r)} />}
              {currentQuestion.questionType === 'MULTIPLE_CHOICE' && <MultipleChoiceQuestion question={currentQuestion} onAnswer={(a) => handleAnswer(a)} />}
              {currentQuestion.questionType === 'TYPING' && <TypingQuestion question={currentQuestion} onAnswer={(a) => handleAnswer(a)} />}
              {currentQuestion.questionType === 'LISTENING' && <ListeningQuestion question={currentQuestion} onAnswer={(a) => handleAnswer(a)} />}
              {currentQuestion.questionType === 'SENTENCE_FILL' && <SentenceFillQuestion question={currentQuestion} onAnswer={(a) => handleAnswer(a)} />}
              {currentQuestion.questionType === 'MATCH' && <MatchQuestion question={currentQuestion} onAnswer={(a) => handleAnswer(a)} />}
            </>
          ) : (
            <div className="flex flex-col items-center justify-center space-y-8 animate-in fade-in zoom-in duration-300">
              {result.correct ? (
                <div className="flex flex-col items-center space-y-4 text-emerald-500">
                  <CheckCircle2 className="w-24 h-24" />
                  <h2 className="text-3xl font-bold">Correct!</h2>
                  <p className="text-emerald-600/80 font-medium">+{result.xpEarned} XP</p>
                </div>
              ) : (
                <div className="flex flex-col items-center space-y-4 text-red-500">
                  <XCircle className="w-24 h-24" />
                  <h2 className="text-3xl font-bold">Incorrect</h2>
                  <div className="p-4 bg-red-50 dark:bg-red-950/30 rounded-xl text-center">
                    <p className="text-sm text-red-400 font-medium mb-1">Correct answer:</p>
                    <p className="text-xl font-bold">{result.correctAnswer}</p>
                  </div>
                </div>
              )}
              
              <div className="mt-8 text-center max-w-lg">
                <p className="text-slate-600 dark:text-slate-300">{result.explanation}</p>
              </div>

              <div className="pt-8 w-full">
                <Button 
                  onClick={nextQuestion} 
                  className={`w-full py-8 text-xl font-bold rounded-2xl shadow-lg hover:shadow-xl transition-all ${
                    result.correct ? 'bg-emerald-500 hover:bg-emerald-600' : 'bg-red-500 hover:bg-red-600'
                  }`}
                >
                  Continue
                </Button>
                <p className="text-center text-xs text-slate-400 mt-4">Press Enter to continue</p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
