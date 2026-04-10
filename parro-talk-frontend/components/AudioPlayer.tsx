"use client";

import { useEffect, useRef, useState } from "react";
import { Play, Pause, RotateCcw, Volume2 } from "lucide-react";

interface AudioPlayerProps {
  url: string;
  onPlay?: () => void;
  autoPlay?: boolean;
}

export default function AudioPlayer({ url, onPlay, autoPlay = false }: AudioPlayerProps) {
  const [isPlaying, setIsPlaying] = useState(false);
  const [progress, setProgress] = useState(0);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  useEffect(() => {
    if (autoPlay && audioRef.current) {
      audioRef.current.play().catch(() => {
        // Autoplay might be blocked by browser
      });
    }
  }, [autoPlay]);

  const togglePlay = () => {
    if (!audioRef.current) return;
    if (isPlaying) {
      audioRef.current.pause();
    } else {
      audioRef.current.play();
      if (onPlay) onPlay();
    }
    setIsPlaying(!isPlaying);
  };

  const restart = () => {
    if (!audioRef.current) return;
    audioRef.current.currentTime = 0;
    audioRef.current.play();
    setIsPlaying(true);
    if (onPlay) onPlay();
  };

  const handleTimeUpdate = () => {
    if (!audioRef.current) return;
    const current = audioRef.current.currentTime;
    const duration = audioRef.current.duration;
    setProgress((current / duration) * 100);
  };

  const handleEnded = () => {
    setIsPlaying(false);
    setProgress(100);
  };

  return (
    <div className="w-full max-w-md bg-white rounded-3xl p-6 shadow-sm border border-green-100 transition-all hover:shadow-md">
      <audio
        ref={audioRef}
        src={url}
        onTimeUpdate={handleTimeUpdate}
        onEnded={handleEnded}
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
      />
      
      <div className="flex flex-col gap-4">
        <div className="flex items-center justify-between">
            <div className="p-3 bg-green-50 rounded-2xl text-green-600">
                <Volume2 size={24} />
            </div>
            <div className="flex-1 px-4">
                <div className="h-2 w-full bg-gray-100 rounded-full overflow-hidden">
                    <div 
                        className="h-full bg-green-500 transition-all duration-300 ease-linear" 
                        style={{ width: `${progress}%` }}
                    />
                </div>
            </div>
        </div>

        <div className="flex items-center justify-center gap-6">
          <button
            onClick={restart}
            className="p-4 text-green-600 hover:bg-green-50 rounded-full transition-colors active:scale-95"
            title="Replay"
          >
            <RotateCcw size={28} />
          </button>
          
          <button
            onClick={togglePlay}
            className="p-6 bg-green-500 text-white rounded-full shadow-lg shadow-green-200 hover:bg-green-600 transition-all active:scale-95"
          >
            {isPlaying ? <Pause size={32} /> : <Play size={32} fill="currentColor" />}
          </button>

          <div className="w-[60px]" /> {/* Spacer to center the play button */}
        </div>
      </div>
    </div>
  );
}
