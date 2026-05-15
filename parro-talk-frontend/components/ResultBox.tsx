"use client";

import { PracticeResult } from "@/types";
import { CheckCircle2, XCircle, RotateCcw, ArrowRight } from "lucide-react";
import confetti from "canvas-confetti";
import { useEffect } from "react";

interface ResultBoxProps {
  result: PracticeResult;
  onRetry: () => void;
  onNext?: () => void;
}

export default function ResultBox({ result, onRetry, onNext }: ResultBoxProps) {
  useEffect(() => {
    if (result.score >= 80) {
      confetti({
        particleCount: 150,
        spread: 70,
        origin: { y: 0.6 },
        colors: ["#22c55e", "#86efac", "#ffffff"]
      });
    }
  }, [result.score]);

  const getFeedbackMessage = () => {
    if (result.score === 100) return "Perfect! You have a great ear.";
    if (result.score >= 80) return "Excellent! Almost perfect.";
    if (result.score >= 50) return "Good job! Keep practicing.";
    return "Don't give up! Try listening again.";
  };

  return (
    <div className="w-full max-w-2xl bg-white rounded-[2rem] p-8 shadow-xl border border-gray-100 animate-in fade-in zoom-in duration-300">
      <div className="flex flex-col items-center text-center gap-6">
        <div className={`p-4 rounded-full ${result.score >= 80 ? "bg-green-100 text-green-600" : "bg-orange-100 text-orange-600"}`}>
          {result.score >= 80 ? <CheckCircle2 size={48} /> : <XCircle size={48} />}
        </div>

        <div>
           <h2 className="text-4xl font-black text-gray-800 mb-2">{result.score}%</h2>
           <p className="text-xl text-gray-600 font-medium">{getFeedbackMessage()}</p>
        </div>

        <div className="w-full bg-gray-50 rounded-2xl p-6 text-left border border-gray-100">
           <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-4">Feedback</h3>
           <div className="flex flex-wrap gap-x-2 gap-y-3">
             {result.tokens.map((token, idx) => (
               <div key={idx} className="flex flex-col items-center">
                 <span className={`text-lg font-bold ${token.isCorrect ? "text-green-600" : "text-red-500 underline decoration-2 underline-offset-4"}`}>
                   {token.text}
                 </span>
                 {!token.isCorrect && token.expected && (
                   <span className="text-xs font-bold text-gray-400">
                     {token.expected}
                   </span>
                 )}
               </div>
             ))}
           </div>
        </div>

        <div className="flex w-full gap-4 mt-4">
          <button
            onClick={onRetry}
            className="flex-1 flex items-center justify-center gap-2 py-4 rounded-2xl border-2 border-gray-200 text-gray-600 font-bold hover:bg-gray-50 transition-all"
          >
            <RotateCcw size={20} />
            Try Again
          </button>
          
          <button
            onClick={onNext}
            className="flex-1 flex items-center justify-center gap-2 py-4 rounded-2xl bg-green-500 text-white font-bold hover:bg-green-600 shadow-lg shadow-green-100 transition-all"
          >
            {onNext ? "Continue" : "Next Sentence"}
            <ArrowRight size={20} />
          </button>
        </div>
      </div>
    </div>
  );
}

