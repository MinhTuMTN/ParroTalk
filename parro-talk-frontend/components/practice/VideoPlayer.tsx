"use client";

import { Play, Pause, RotateCcw, Subtitles, MoreHorizontal, Zap, Save, ChevronLeft, ChevronRight } from "lucide-react";

import { useState, useEffect, useCallback, useRef } from "react";

interface Segment {
  start: number;
  end: number;
  text: string;
}

export default function VideoPlayer({ src, activeSegment, onReplay, onSave, onNext, onPrevious, hasNext, hasPrevious }: { 
  src?: string, 
  activeSegment?: Segment, 
  onReplay?: () => void, 
  onSave?: () => void,
  onNext?: () => void,
  onPrevious?: () => void,
  hasNext?: boolean,
  hasPrevious?: boolean
}) {
  const [isPlaying, setIsPlaying] = useState(false);


  const [speed, setSpeed] = useState(1);
  const [isLooping, setIsLooping] = useState(() => {
    if (typeof window !== "undefined") {
      const saved = localStorage.getItem("parrotalk_video_loop");
      return saved !== null ? JSON.parse(saved) : true;
    }
    return false;
  });
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    localStorage.setItem("parrotalk_video_loop", JSON.stringify(isLooping));
  }, [isLooping]);
  const [progressPercent, setProgressPercent] = useState(0);

  const handleReplay = useCallback(() => {
    if (videoRef.current && activeSegment) {
      videoRef.current.currentTime = activeSegment.start;
      videoRef.current.play().catch(e => console.log("Playback error", e));
      setIsPlaying(true);
      if (onReplay) onReplay();
    }
  }, [activeSegment, onReplay]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        e.preventDefault();
        handleReplay();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [handleReplay]);

  // Handle loop clipping bounding
  useEffect(() => {
    const v = videoRef.current;
    if (!v || !activeSegment) return;

    const handleTimeUpdate = () => {
      const ct = v.currentTime;
      const { start, end } = activeSegment;

      if (ct >= end) {
        v.currentTime = start;
        if (isLooping) {
          v.play().catch(e => console.log(e));
        } else {
          v.pause();
          setIsPlaying(false);
        }
      }

      const mappedProgress = Math.max(0, Math.min(100, ((ct - start) / (end - start)) * 100));
      setProgressPercent(mappedProgress);
      if (v.paused !== !isPlaying) setIsPlaying(!v.paused);
    };

    v.addEventListener('timeupdate', handleTimeUpdate);
    return () => v.removeEventListener('timeupdate', handleTimeUpdate);
  }, [activeSegment, isLooping, isPlaying]);

  // Handle active Segment change payload
  useEffect(() => {
    if (activeSegment && videoRef.current) {
      videoRef.current.currentTime = activeSegment.start;
      videoRef.current.play().catch(e => console.log(e));
      setIsPlaying(true);
    }
  }, [activeSegment]);

  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.playbackRate = speed;
    }
  }, [speed]);

  const togglePlay = () => {
    if (videoRef.current) {
      if (videoRef.current.paused) {
        videoRef.current.play().catch(e => console.log(e));
      } else {
        videoRef.current.pause();
      }
      setIsPlaying(!videoRef.current.paused);
    }
  };

  return (
    <div className="flex flex-col gap-3 w-full max-w-[360px] shrink-0">
      <div className="relative aspect-[4/3] bg-gray-950 rounded-3xl overflow-hidden group shadow-xl border border-gray-100">
        {src ? (
          <video
            ref={videoRef}
            src={src}
            className="w-full h-full object-cover opacity-90 transition-opacity"
            playsInline
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-gray-500 font-bold select-none bg-gray-900 animate-pulse">Loading Source...</div>
        )}

        {/* Overlay Controller View */}
        <div className={`absolute inset-0 flex flex-col items-center justify-center gap-4 transition-all duration-300 bg-black/40 ${isPlaying ? 'opacity-0 hover:opacity-100' : 'opacity-100'}`}>
          <button
            onClick={togglePlay}
            className="w-14 h-14 bg-green-500 text-white rounded-full flex items-center justify-center shadow-lg hover:scale-110 hover:bg-green-400 active:scale-95 transition-all outline-none"
          >
            {isPlaying ? <Pause size={24} fill="currentColor" /> : <Play size={24} fill="currentColor" className="ml-1" />}
          </button>
          <button
            onClick={handleReplay}
            className="px-4 py-2 bg-white/10 backdrop-blur-md border border-white/20 text-white rounded-xl text-[10px] font-black uppercase tracking-wider hover:bg-white/20 transition-all flex items-center gap-2 shadow-sm"
          >
            <RotateCcw size={14} />
            Replay [ESC]
          </button>
        </div>

        {/* Speed indicator on video */}
        <div className="absolute top-4 right-4 bg-black/40 backdrop-blur-md px-2 py-1 rounded-lg border border-white/10 text-[9px] font-black text-white uppercase tracking-widest flex items-center gap-1 shadow-sm">
          <Zap size={10} className="text-yellow-400" fill="currentColor" />
          {speed}x
        </div>

        {/* Progress Bar Overlaid on Bottom */}
        <div className="absolute bottom-0 left-0 right-0 h-1.5 bg-white/20 flex z-10 overflow-hidden">
          <div className="h-full bg-green-500 transition-all duration-100 ease-linear shadow-[0_0_10px_rgba(34,197,94,0.5)] relative" style={{ width: `${progressPercent}%` }}>
            <div className="absolute top-0 right-0 bottom-0 w-2 bg-white/50 blur-sm rounded-full animate-pulse" />
          </div>
        </div>
      </div>

      {/* Compact Controls Card */}
      <div className="bg-white rounded-2xl p-3 border border-gray-100 shadow-sm flex flex-col gap-3">
        <div className="flex items-center justify-between px-1">
          <span className="text-[9px] font-black text-gray-400 uppercase tracking-widest">Speed</span>
          <div className="flex bg-gray-50 p-0.5 rounded-xl gap-0.5 border border-gray-100">
            {[0.5, 0.75, 1, 1.25, 1.5].map(s => (
              <button
                key={s}
                onClick={() => setSpeed(s)}
                className={`px-3 py-1.5 rounded-lg text-[10px] font-black transition-all active:scale-95 ${speed === s ? "bg-white text-green-600 shadow-sm border-gray-100 border" : "text-gray-400 hover:text-gray-600"}`}
              >
                {s}x
              </button>
            ))}
          </div>
        </div>

        <div className="flex items-center justify-between pt-3 border-t border-gray-50">
          <div className="flex items-center gap-4">
            <button className="flex items-center gap-1.5 text-gray-400 hover:text-green-500 transition-colors font-black text-[9px] uppercase tracking-widest px-1">
              <Subtitles size={12} />
              Subs
            </button>
            <button onClick={() => setIsLooping(!isLooping)} className={`flex items-center gap-1.5 transition-colors font-black text-[9px] uppercase tracking-widest px-1 ${isLooping ? 'text-green-500' : 'text-gray-400 hover:text-green-500'}`}>
              <RotateCcw size={12} />
              Loop
            </button>
          </div>
          <button
            onClick={onSave}
            className="flex items-center gap-1.5 px-3 py-1.5 bg-gray-900 text-white rounded-xl text-[10px] font-black uppercase tracking-widest hover:bg-gray-800 transition-all active:scale-95 shadow-sm"
          >
            <Save size={12} />
            Save Progress
          </button>
        </div>

        {/* Mobile Navigation Buttons */}
        <div className="md:hidden flex gap-2 mt-1">
          <button
            onClick={onPrevious}
            disabled={!hasPrevious}
            className="flex-1 py-3 bg-gray-50 text-gray-600 rounded-xl font-black text-[10px] uppercase tracking-widest border border-gray-100 flex items-center justify-center gap-2 disabled:opacity-30 active:bg-gray-100 transition-all"
          >
            <ChevronLeft size={16} />
            Prev
          </button>
          <button
            onClick={onNext}
            disabled={!hasNext}
            className="flex-1 py-3 bg-gray-50 text-gray-600 rounded-xl font-black text-[10px] uppercase tracking-widest border border-gray-100 flex items-center justify-center gap-2 disabled:opacity-30 active:bg-gray-100 transition-all"
          >
            Next
            <ChevronRight size={16} />
          </button>
        </div>
      </div>

    </div>
  );
}
