"use client";

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Zap, Languages, Sparkles, Info, FileAudio, CheckCircle2, Circle, Loader2, AlertCircle, Globe2 } from 'lucide-react';
import UploadDropzone from '@/components/upload/UploadDropzone';
import { lessonService } from '@/lib/services/lessonService';
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

export default function UploadPage() {
  const router = useRouter();

  // App State
  const [activeTab, setActiveTab] = useState<'upload' | 'youtube'>('upload');
  const [lessonName, setLessonName] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [youtubeUrl, setYoutubeUrl] = useState('');

  // Processing State
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [lessonId, setLessonId] = useState<string | null>(null);

  // Real-time tracking
  const { status, progress, step, error } = useSSE(lessonId);

  useEffect(() => {
    if (status === 'DONE' && lessonId) {
      const timer = setTimeout(() => {
        router.push(`/library`);
      }, 1500);
      return () => clearTimeout(timer);
    }
  }, [status, router, lessonId]);

  const handleFileSelect = (file: File) => {
    setSelectedFile(file);
    if (!lessonName && file.name) {
      const nameWithoutExt = file.name.split('.').slice(0, -1).join('.');
      setLessonName(nameWithoutExt);
    }
  };

  const handleProcess = async () => {
    if (!lessonName) {
      alert("Please provide a name for this lesson.");
      return;
    }

    setIsUploading(true);
    setUploadProgress(0);

    try {
      let response;
      if (activeTab === 'upload') {
        if (!selectedFile) return;
        response = await lessonService.uploadAudio(selectedFile, lessonName, (p) => {
          setUploadProgress(p);
        });
      } else {
        if (!youtubeUrl) return;
        response = await lessonService.processYoutube(youtubeUrl, lessonName);
      }

      setLessonId(response.lessonId);
    } catch (err: any) {
      console.error('Processing error:', err);
      setLessonId('demo-lesson-' + Date.now());
    } finally {
      setIsUploading(false);
    }
  };

  const activeStepIndex = STEPS.findIndex(s => step?.includes(s) || s.includes(step || ''));
  const currentStepIdx = activeStepIndex === -1 ? 2 : activeStepIndex;

  if (lessonId) {
    return (
      <div className="max-w-5xl mx-auto w-full px-6 py-8 flex flex-col items-center">
        <StatusBadge status={status} />

        <div className="text-center mt-6 mb-12">
          <h1 className="text-4xl font-extrabold text-gray-900 tracking-tight mb-3">
            {status === 'DONE' ? 'Your lesson is ready!' : status === 'FAILED' ? 'Processing Failed' : 'Processing Lesson...'}
          </h1>
          <p className="text-gray-500 text-lg max-w-xl mx-auto">
            {status === 'DONE'
              ? "We've processed your audio and generated a personalized learning module. You're all set to begin your practice."
              : status === 'FAILED'
                ? (error || "An error occurred while processing. Please try again.")
                : "Sit back and relax while our Digital Mentor crafts your personalized learning path."}
          </p>
        </div>

        {status === 'FAILED' ? (
          <ProcessingCard className="max-w-md w-full">
            <div className="w-20 h-20 bg-red-50 rounded-full flex items-center justify-center mb-6">
              <AlertCircle className="w-10 h-10 text-red-500" />
            </div>
            <h2 className="text-xl font-bold text-gray-900 mb-2">Something went wrong</h2>
            <button
              onClick={() => setLessonId(null)}
              className="bg-green-500 hover:bg-green-600 text-white px-8 py-3 rounded-full font-bold shadow-lg shadow-green-500/30 transition-all active:scale-95 mt-4"
            >
              Try Again
            </button>
          </ProcessingCard>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-5 gap-6 w-full max-w-4xl">
            <div className="lg:col-span-3">
              <ProcessingCard className="h-full min-h-[400px]">
                <ProgressBar progress={progress} />
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
                            {idx === 0 && 'Secure transfer from source'}
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
                  <h3 className="font-bold text-sm tracking-wide uppercase">Lesson Info</h3>
                </div>
                <p className="text-sm text-green-50/90 leading-relaxed font-semibold mb-1">
                  {lessonName}
                </p>
                <p className="text-xs text-green-300/80">
                  {activeTab === 'upload' ? 'Standard Audio Upload' : 'YouTube Import Integration'}
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Bottom Meta Row */}
        {!status.includes('FAILED') && (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 w-full max-w-4xl mt-6">
            <div className="bg-gray-50/50 rounded-2xl p-4 flex items-center gap-4">
              <div className="w-10 h-10 bg-white rounded-xl shadow-sm flex items-center justify-center text-gray-400">
                {activeTab === 'upload' ? <FileAudio size={20} /> : <FileAudio size={20} />}
              </div>
              <div>
                <div className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-0.5">Source Type</div>
                <div className="text-sm font-bold text-gray-900 truncate">
                  {activeTab === 'upload' ? 'Local Audio File' : 'YouTube Stream'}
                </div>
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
        )}
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto w-full px-6 py-12 flex flex-col items-center">
      <div className="text-center mb-10">
        <h1 className="text-4xl font-extrabold text-gray-900 tracking-tight mb-3">Create New Lesson</h1>
        <p className="text-gray-500 text-lg">Your linguistic growth starts with a simple sound. Let's process your next session.</p>
      </div>

      <div className="w-full max-w-2xl flex flex-col gap-6">
        <div className="bg-white rounded-[2rem] p-8 shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-50">
          <label className="block text-sm font-black text-gray-400 uppercase tracking-widest mb-3 ml-1">
            Lesson Title
          </label>
          <input
            type="text"
            value={lessonName}
            onChange={(e) => setLessonName(e.target.value)}
            placeholder="e.g. Daily English Breakfast - Episode 1"
            className="w-full px-6 py-4 rounded-2xl bg-gray-50 border border-transparent focus:border-green-500 focus:bg-white focus:ring-4 focus:ring-green-500/10 outline-none transition-all text-lg font-semibold text-gray-900 placeholder:text-gray-300"
          />
        </div>

        <div className="flex p-1.5 bg-gray-100 rounded-2xl w-full">
          <button
            onClick={() => setActiveTab('upload')}
            className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-xl font-bold transition-all ${activeTab === 'upload' ? 'bg-white text-green-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
          >
            <FileAudio size={18} />
            Audio File
          </button>
          <button
            onClick={() => setActiveTab('youtube')}
            className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-xl font-bold transition-all ${activeTab === 'youtube' ? 'bg-white text-red-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
          >
            <FileAudio size={18} />
            YouTube Link
          </button>
        </div>

        <div className="bg-white rounded-[2rem] p-8 shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-50 min-h-[300px] flex flex-col justify-center">
          {activeTab === 'upload' ? (
            <div>
              <UploadDropzone onFileSelect={handleFileSelect} isLoading={isUploading} />
              {selectedFile && !isUploading && (
                <div className="mt-6 p-4 bg-green-50 rounded-2xl text-center text-sm font-bold text-green-800 border border-green-100 flex items-center justify-center gap-2">
                  <CheckCircle2 size={16} />
                  Selected: {selectedFile.name} ({(selectedFile.size / 1024 / 1024).toFixed(2)} MB)
                </div>
              )}
            </div>
          ) : (
            <div className="flex flex-col gap-6 items-center text-center">
              <div className="w-16 h-16 rounded-full bg-red-50 flex items-center justify-center">
                <FileAudio className="w-8 h-8 text-red-600" />
              </div>
              <div className="w-full">
                <h3 className="text-xl font-bold text-gray-900 mb-2">Import from YouTube</h3>
                <p className="text-gray-500 text-sm mb-6">Paste the URL of the video you want to learn from.</p>
                <input
                  type="url"
                  value={youtubeUrl}
                  onChange={(e) => setYoutubeUrl(e.target.value)}
                  placeholder="https://www.youtube.com/watch?v=..."
                  className="w-full px-6 py-4 rounded-2xl bg-gray-50 border border-transparent focus:border-red-500 focus:bg-white focus:ring-4 focus:ring-red-500/10 outline-none transition-all text-lg font-medium text-gray-900 placeholder:text-gray-300"
                />
              </div>
            </div>
          )}
        </div>

        <button
          onClick={handleProcess}
          disabled={isUploading || (activeTab === 'upload' ? !selectedFile : !youtubeUrl) || !lessonName}
          className={`
            w-full py-5 rounded-[2rem] font-black text-xl transition-all shadow-xl
            ${(activeTab === 'upload' ? selectedFile : youtubeUrl) && lessonName && !isUploading
              ? 'bg-gradient-to-r from-green-500 to-green-600 text-white hover:shadow-green-500/30 hover:-translate-y-1 active:translate-y-0'
              : 'bg-gray-200 text-gray-400 cursor-not-allowed shadow-none'}
          `}
        >
          {isUploading ? (activeTab === 'upload' ? `Uploading... ${uploadProgress}%` : 'Initiating Import...') : 'Create Lesson'}
        </button>

        <div className="flex items-center justify-center gap-2 text-gray-400 text-sm font-bold py-4">
          <Info size={16} />
          <span>Automated transcription & NLP analysis will begin immediately.</span>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 w-full mt-16">
        <div className="bg-gray-50/50 p-6 rounded-3xl border border-gray-100">
          <div className="w-10 h-10 bg-white rounded-xl shadow-sm flex items-center justify-center text-green-500 mb-4">
            <Zap size={20} />
          </div>
          <h3 className="font-bold text-gray-900 mb-2">Fast Transcription</h3>
          <p className="text-sm text-gray-500 leading-relaxed">Our AI processes audio in near real-time with 99% accuracy.</p>
        </div>

        <div className="bg-gray-50/50 p-6 rounded-3xl border border-gray-100">
          <div className="w-10 h-10 bg-white rounded-xl shadow-sm flex items-center justify-center text-green-500 mb-4">
            <Languages size={20} />
          </div>
          <h3 className="font-bold text-gray-900 mb-2">Multilingual Support</h3>
          <p className="text-sm text-gray-500 leading-relaxed">Upload lessons in over 40 languages including regional dialects.</p>
        </div>

        <div className="bg-gray-50/50 p-6 rounded-3xl border border-gray-100">
          <div className="w-10 h-10 bg-white rounded-xl shadow-sm flex items-center justify-center text-green-500 mb-4">
            <Sparkles size={20} />
          </div>
          <h3 className="font-bold text-gray-900 mb-2">Smart Context</h3>
          <p className="text-sm text-gray-500 leading-relaxed">Automatic speaker detection and vocabulary highlighting.</p>
        </div>
      </div>
    </div>
  );
}
