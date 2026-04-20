"use client";

import React, { useEffect, use } from 'react';
import { useRouter } from 'next/navigation';
import { CheckCircle2, Circle, AlertCircle, FileAudio, Globe2, Sparkles, Loader2 } from 'lucide-react';
import { useSSE } from '@/hooks/useSSE';
import ProgressBar from '@/components/upload/ProgressBar';
import StatusBadge from '@/components/upload/StatusBadge';
import ProcessingCard from '@/components/upload/ProcessingCard';

const STEPS = [
  'Downloading file...',
  'Extracting audio...',
  'Transcribing audio...',
  'Synthesizing Notes',
  'Ready for Review'
];

export default function ProcessingPage({ params }: { params: Promise<{ lessonId: string }> }) {
  const router = useRouter();
  const { lessonId } = use(params);
  const { status, progress, step, error } = useSSE(lessonId);

  useEffect(() => {
    if (status === 'DONE') {
      // Small delay before redirecting for better UX
      const timer = setTimeout(() => {
        router.push(`/result?lessonId=${lessonId}`);
        // Redirecting to /result based on previous conversation patterns (lesson reset/results)
        // Adjust if needed to /lesson/{lessonId}
      }, 1500);
      return () => clearTimeout(timer);
    }
  }, [status, router, lessonId]);

  const activeStepIndex = STEPS.findIndex(s => step?.includes(s) || s.includes(step || ''));
  const currentStepIdx = activeStepIndex === -1 ? 2 : activeStepIndex; // Default to middle if no match

  if (status === 'FAILED') {
    return (
      <div className="max-w-5xl mx-auto w-full px-6 py-12 flex flex-col items-center">
        <h1 className="text-4xl font-extrabold text-gray-900 tracking-tight mb-3 mt-8">Processing Failed</h1>
        <p className="text-gray-500 text-lg mb-12 text-center max-w-xl">
          {error || "An error occurred while processing your audio. Please try again."}
        </p>

        <ProcessingCard className="max-w-md w-full">
          <div className="w-20 h-20 bg-red-50 rounded-full flex items-center justify-center mb-6">
            <AlertCircle className="w-10 h-10 text-red-500" />
          </div>
          <h2 className="text-xl font-bold text-gray-900 mb-2">Processing failed</h2>
          <p className="text-center text-gray-500 text-sm mb-8">
            The file format might be unsupported or corrupted. Please check your source and try uploading again.
          </p>
          <button
            onClick={() => router.push('/upload')}
            className="bg-green-500 hover:bg-green-600 text-white px-8 py-3 rounded-full font-bold shadow-lg shadow-green-500/30 transition-all active:scale-95"
          >
            Try Again
          </button>
        </ProcessingCard>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto w-full px-6 py-8 flex flex-col items-center">
      <StatusBadge status={status} />

      <div className="text-center mt-6 mb-12">
        <h1 className="text-4xl font-extrabold text-gray-900 tracking-tight mb-3">
          {status === 'DONE' ? 'Your lesson is ready!' : 'Processing Lesson...'}
        </h1>
        <p className="text-gray-500 text-lg max-w-xl mx-auto">
          {status === 'DONE'
            ? "We've processed your audio and generated a personalized learning module. You're all set to begin your practice."
            : "Sit back and relax while our Digital Mentor crafts your personalized learning path from the uploaded audio."}
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6 w-full max-w-4xl">
        {/* Left Column - Progress */}
        <div className="lg:col-span-3">
          <ProcessingCard className="h-full min-h-[400px]">
            <ProgressBar progress={progress} />

            {/* Fake Audio Waveform */}
            <div className="mt-12 flex items-baseline justify-center gap-1.5 h-12">
              {[...Array(20)].map((_, i) => (
                <div
                  key={i}
                  className="w-2 bg-green-500/40 rounded-full animate-pulse"
                  style={{
                    height: `${Math.max(20, Math.random() * 100)}%`,
                    animationDelay: `${i * 0.1}s`,
                    animationDuration: '1s'
                  }}
                />
              ))}
            </div>
            <div className="mt-8 text-sm text-gray-500 font-medium flex items-center gap-2">
              <Loader2 size={16} className="animate-spin text-green-500" />
              <span>{step || 'Processing...'}</span>
            </div>
          </ProcessingCard>
        </div>

        {/* Right Column - Steps & Info */}
        <div className="lg:col-span-2 flex flex-col gap-6">
          <div className="bg-white rounded-3xl p-6 shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-50">
            <h3 className="font-bold text-gray-900 mb-6 text-lg">Lesson Construction</h3>
            <div className="flex flex-col gap-4">
              {STEPS.map((s, idx) => {
                const isCompleted = status === 'DONE' || idx < currentStepIdx;
                const isActive = status !== 'DONE' && idx === currentStepIdx;

                return (
                  <div key={idx} className="flex gap-4">
                    <div className="mt-0.5 relative">
                      {isCompleted ? (
                        <CheckCircle2 className="w-5 h-5 text-green-500" />
                      ) : isActive ? (
                        <div className="w-5 h-5 rounded-full border-4 border-green-100 flex items-center justify-center">
                          <div className="w-2.5 h-2.5 bg-green-500 rounded-full" />
                        </div>
                      ) : (
                        <Circle className="w-5 h-5 text-gray-200" />
                      )}
                      {idx !== STEPS.length - 1 && (
                        <div className={`absolute top-6 left-2.5 w-0.5 h-6 -ml-px ${isCompleted ? 'bg-green-500' : 'bg-gray-100'}`} />
                      )}
                    </div>
                    <div>
                      <h4 className={`font-bold text-sm ${isCompleted || isActive ? 'text-gray-900' : 'text-gray-400'}`}>{s}</h4>
                      <p className={`text-xs mt-0.5 ${isCompleted || isActive ? 'text-gray-500' : 'text-gray-300'}`}>
                        {idx === 0 && 'Secure transfer from storage'}
                        {idx === 1 && 'Preparing high-fidelity stream'}
                        {idx === 2 && 'Generating bilingual mapping'}
                        {idx === 3 && 'Extracting key vocabulary'}
                        {idx === 4 && 'Finalizing dashboard'}
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="bg-green-900 rounded-3xl p-6 text-white shadow-xl shadow-green-900/20">
            <div className="flex items-center gap-2 mb-3 text-green-300">
              <Sparkles size={18} />
              <h3 className="font-bold text-sm tracking-wide uppercase">Did you know?</h3>
            </div>
            <p className="text-sm text-green-50/90 leading-relaxed">
              Our AI detects nuances in 42 languages, ensuring your lesson captures colloquialisms and natural speech patterns.
            </p>
          </div>
        </div>
      </div>

      {/* Bottom Meta Row */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 w-full max-w-4xl mt-6">
        <div className="bg-gray-50/50 rounded-2xl p-4 flex items-center gap-4">
          <div className="w-10 h-10 bg-white rounded-xl shadow-sm flex items-center justify-center text-gray-400">
            <FileAudio size={20} />
          </div>
          <div>
            <div className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-0.5">Source File</div>
            <div className="text-sm font-bold text-gray-900 truncate">lesson_audio.mp3</div>
          </div>
        </div>

        <div className="bg-gray-50/50 rounded-2xl p-4 flex items-center gap-4">
          <div className="w-10 h-10 bg-white rounded-xl shadow-sm flex items-center justify-center text-gray-400">
            <Globe2 size={20} />
          </div>
          <div>
            <div className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-0.5">Target Language</div>
            <div className="text-sm font-bold text-gray-900">Auto-detect</div>
          </div>
        </div>

        <div className="bg-gray-50/50 rounded-2xl p-4 flex items-center gap-4">
          <div className="w-10 h-10 bg-white rounded-xl shadow-sm flex items-center justify-center text-gray-400">
            <Sparkles size={20} />
          </div>
          <div>
            <div className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-0.5">Processing Engine</div>
            <div className="text-sm font-bold text-gray-900">NeuralScribe Pro V2</div>
          </div>
        </div>
      </div>

    </div>
  );
}
