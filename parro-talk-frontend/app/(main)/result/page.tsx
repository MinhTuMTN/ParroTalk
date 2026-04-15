"use client";

import Link from "next/link";
import { CheckCircle, XCircle, Award, ArrowRight, RotateCcw } from "lucide-react";

export default function ResultPage() {
  const searchParams = {
    score: "8.5",
    passed: "true",
    lessonId: "1"
  }
  const scoreRaw = searchParams?.score;
  const passedRaw = searchParams?.passed;
  const lessonId = searchParams?.lessonId;

  const score = scoreRaw ? parseFloat(scoreRaw) : 0;
  const isPassed = passedRaw === "true";

  return (
    <div className="flex flex-col items-center justify-center min-h-[calc(100vh-80px)] bg-gray-50 p-6">
      <div className="bg-white p-10 rounded-3xl shadow-xl w-full max-w-md text-center border top-effect transition-all flex flex-col items-center">
        {isPassed ? (
          <div className="w-24 h-24 bg-green-100 text-green-500 rounded-full flex items-center justify-center mb-6 shadow-green-100 shadow-inner">
            <CheckCircle size={48} />
          </div>
        ) : (
          <div className="w-24 h-24 bg-red-100 text-red-500 rounded-full flex items-center justify-center mb-6 shadow-red-100 shadow-inner">
            <XCircle size={48} />
          </div>
        )}

        <h1 className="text-3xl font-black text-gray-800 mb-2">
          {isPassed ? "Great Job!" : "Keep Trying!"}
        </h1>
        <p className="text-gray-500 font-medium mb-8">
          {isPassed ? "You've passed the lesson criteria." : "You can review and try again."}
        </p>

        <div className="bg-gray-50 w-full p-6 rounded-2xl flex flex-col items-center gap-2 mb-8 border border-gray-100">
          <Award className="text-yellow-500 mb-2" size={32} />
          <span className="text-sm font-bold text-gray-400 tracking-widest uppercase">Your Score</span>
          <span className={`text-5xl font-black ${isPassed ? 'text-green-500' : 'text-red-500'}`}>
            {score.toFixed(1)} <span className="text-2xl text-gray-400">/10</span>
          </span>
        </div>

        <div className="flex flex-col gap-3 w-full">
          {lessonId && !isPassed && (
            <Link href={`/practice/${lessonId}`} className="w-full py-4 rounded-xl bg-orange-100 text-orange-600 font-bold hover:bg-orange-200 transition-colors flex items-center justify-center gap-2">
              <RotateCcw size={18} />
              Try Again
            </Link>
          )}
          <Link href="/library" className="w-full py-4 rounded-xl bg-gray-900 text-white font-bold shadow-lg hover:shadow-xl hover:-translate-y-0.5 transition-all flex items-center justify-center gap-2">
            Continue Learning
            <ArrowRight size={18} />
          </Link>
        </div>
      </div>
    </div>
  );
}
