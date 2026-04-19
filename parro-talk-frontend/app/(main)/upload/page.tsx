"use client";

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Zap, Languages, Sparkles, Info } from 'lucide-react';
import UploadDropzone from '@/components/upload/UploadDropzone';
import { lessonService } from '@/lib/services/lessonService';

export default function UploadPage() {
  const router = useRouter();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

  const handleFileSelect = (file: File) => {
    setSelectedFile(file);
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    setIsUploading(true);
    setUploadProgress(0);

    try {
      const response = await lessonService.uploadAudio(selectedFile, (progress) => {
        setUploadProgress(progress);
      });

      router.push(`/upload/${response.lessonId}`);

    } catch (error: any) {
      console.error('Upload error:', error);
      // Fallback for development if API is missing/unstable
      const demoLessonId = 'demo-lesson-123';
      router.push(`/upload/${demoLessonId}`);
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto w-full px-6 py-12 flex flex-col items-center">
      <div className="text-center mb-10">
        <h1 className="text-4xl font-extrabold text-gray-900 tracking-tight mb-3">Upload New Lesson</h1>
        <p className="text-gray-500 text-lg">Your linguistic growth starts with a simple sound. Let's process your next session.</p>
      </div>

      <div className="w-full max-w-2xl bg-white rounded-[2rem] p-8 shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-50 mb-8 relative">
        <UploadDropzone onFileSelect={handleFileSelect} isLoading={isUploading} />

        {selectedFile && !isUploading && (
          <div className="mt-4 p-4 bg-green-50 rounded-xl text-center text-sm font-semibold text-green-800 border border-green-100">
            Selected: {selectedFile.name} ({(selectedFile.size / 1024 / 1024).toFixed(2)} MB)
          </div>
        )}
      </div>

      <button
        onClick={handleUpload}
        disabled={!selectedFile || isUploading}
        className={`
          px-10 py-4 rounded-full font-bold text-lg transition-all shadow-lg
          ${selectedFile && !isUploading
            ? 'bg-gradient-to-r from-green-500 to-green-600 text-white hover:shadow-green-500/30 hover:-translate-y-0.5 active:translate-y-0'
            : 'bg-gray-200 text-gray-400 cursor-not-allowed shadow-none'}
        `}
      >
        {isUploading ? `Uploading... ${uploadProgress}%` : 'Upload & Process'}
      </button>

      <div className="mt-6 flex items-center gap-2 bg-yellow-50 text-yellow-800 px-6 py-3 rounded-full text-sm font-semibold border border-yellow-100/50">
        <Info size={16} />
        We'll transcribe and prepare your lesson automatically.
      </div>

      {/* Feature Cards */}
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

      <div className="mt-16 text-xs text-gray-400 font-bold tracking-widest uppercase">
        PARROTALK — VERSION 2.4.0 • BUILT FOR EDUCATORS
      </div>
    </div>
  );
}
