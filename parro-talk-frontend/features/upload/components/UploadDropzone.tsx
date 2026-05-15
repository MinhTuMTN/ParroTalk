import React, { useCallback, useState } from 'react';
import { CloudUpload } from 'lucide-react';

interface UploadDropzoneProps {
  onFileSelect: (file: File) => void;
  isLoading?: boolean;
}

const SUPPORTED_FORMATS = ['audio/mpeg', 'audio/wav', 'audio/x-m4a', 'audio/mp4'];
const FORMAT_LABELS = ['MP3', 'WAV', 'M4A'];

export default function UploadDropzone({ onFileSelect, isLoading }: UploadDropzoneProps) {
  const [isDragOver, setIsDragOver] = useState(false);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
  }, []);

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setIsDragOver(false);
      
      if (isLoading) return;

      const file = e.dataTransfer.files[0];
      if (file && SUPPORTED_FORMATS.includes(file.type)) {
        onFileSelect(file);
      } else {
        alert('Please upload a supported audio format (MP3, WAV, M4A).');
      }
    },
    [onFileSelect, isLoading]
  );

  const handleFileInput = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      if (isLoading) return;
      const file = e.target.files?.[0];
      if (file) {
        onFileSelect(file);
      }
    },
    [onFileSelect, isLoading]
  );

  return (
    <div
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      className={`
        relative w-full rounded-3xl border-2 border-dashed p-12 text-center transition-all
        ${isDragOver ? 'border-green-500 bg-green-50/50' : 'border-gray-200 bg-white hover:border-green-300 hover:bg-gray-50/50'}
        ${isLoading ? 'opacity-50 pointer-events-none' : 'cursor-pointer'}
      `}
    >
      <input
        type="file"
        accept=".mp3,.wav,.m4a,audio/*"
        onChange={handleFileInput}
        className="absolute inset-0 z-10 w-full h-full opacity-0 cursor-pointer"
        disabled={isLoading}
      />
      
      <div className="flex flex-col items-center justify-center gap-4">
        <div className="w-16 h-16 rounded-full bg-green-50 flex items-center justify-center mb-2">
          <CloudUpload className="w-8 h-8 text-green-600" />
        </div>
        
        <h3 className="text-xl font-bold text-gray-900">Drag & drop audio files here</h3>
        <p className="text-gray-500 font-medium">
          or <span className="text-green-600 font-bold underline decoration-2 underline-offset-4 cursor-pointer">click to browse</span> your computer
        </p>

        <div className="flex gap-2 mt-4">
          {FORMAT_LABELS.map(label => (
            <span key={label} className="px-4 py-1.5 rounded-full bg-gray-100 text-gray-500 text-xs font-bold tracking-wide">
              {label}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}

