"use client";

import ReactPlayer from "react-player/lazy";
import { Play, Pause, RotateCcw, Zap, ChevronLeft, ChevronRight, Repeat } from "lucide-react";
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
  const playerRef = useRef<ReactPlayer>(null);

  const [progressPercent, setProgressPercent] = useState(0);
  const isYoutube = src?.includes('youtube.com') || src?.includes('youtu.be');

  useEffect(() => {
    localStorage.setItem("parrotalk_video_loop", JSON.stringify(isLooping));
  }, [isLooping]);

  const handleReplay = useCallback(() => {
    if (activeSegment) {
      if (isYoutube) {
        if (playerRef.current) {
          playerRef.current.seekTo(activeSegment.start, 'seconds');
          setIsPlaying(true);
        }
      } else if (videoRef.current) {
        videoRef.current.currentTime = activeSegment.start;
        videoRef.current.play().catch(e => console.log("Playback error", e));
        setIsPlaying(true);
      }
      if (onReplay) onReplay();
    }
  }, [activeSegment, onReplay, isYoutube]);

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
    if (isYoutube || !activeSegment) return;
    const v = videoRef.current;
    if (!v) return;

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
  }, [activeSegment, isLooping, isPlaying, isYoutube]);

  // Handle active Segment change payload
  useEffect(() => {
    if (activeSegment) {
      if (isYoutube) {
        if (playerRef.current) {
          playerRef.current.seekTo(activeSegment.start, 'seconds');
          setIsPlaying(true);
        }
      } else if (videoRef.current) {
        videoRef.current.currentTime = activeSegment.start;
        videoRef.current.play()
          .then(() => setIsPlaying(true))
          .catch(e => {
            console.log("Playback blocked or failed:", e);
            setIsPlaying(false);
          });
      }
    }
  }, [activeSegment, isYoutube]);

  useEffect(() => {
    if (!isYoutube && videoRef.current) {
      videoRef.current.playbackRate = speed;
    }
  }, [speed, isYoutube]);

  const togglePlay = () => {
    if (isYoutube) {
      setIsPlaying(!isPlaying);
    } else if (videoRef.current) {
      if (videoRef.current.paused) {
        videoRef.current.play().catch(e => console.log(e));
      } else {
        videoRef.current.pause();
      }
      setIsPlaying(!videoRef.current.paused);
    }
  };

  const onYoutubeProgress = (state: { playedSeconds: number }) => {
    if (isYoutube && activeSegment) {
      const { start, end } = activeSegment;
      const ct = state.playedSeconds;

      if (ct >= end) {
        if (isLooping) {
          playerRef.current?.seekTo(start, 'seconds');
        } else {
          setIsPlaying(false);
        }
      }

      const mappedProgress = Math.max(0, Math.min(100, ((ct - start) / (end - start)) * 100));
      setProgressPercent(mappedProgress);
    }
  };

  return (
    <div className="flex flex-col gap-3 w-full max-w-[360px] shrink-0">
      <div className="relative aspect-[4/3] bg-gray-950 rounded-3xl overflow-hidden group shadow-xl border border-gray-100">
        {src ? (
          isYoutube ? (
            <div className="absolute inset-0 w-full h-full">
              <ReactPlayer
                ref={playerRef}
                url={src}
                playing={isPlaying}
                playbackRate={speed}
                width="100%"
                height="100%"
                onProgress={onYoutubeProgress}
                playsinline
                config={{
                  youtube: {
                    playerVars: {
                      autoplay: 1,
                      controls: 0,
                      modestbranding: 1,
                      rel: 0,
                      iv_load_policy: 3,
                      disablekb: 1
                    }
                  }
                }}
              />
            </div>
          ) : (
            <video
              ref={videoRef}
              src={src}
              className="w-full h-full object-cover opacity-90 transition-opacity"
              playsInline
            />
          )
        ) : (
          <div className="w-full h-full flex items-center justify-center text-gray-500 font-bold select-none bg-gray-900 animate-pulse">Loading Source...</div>
        )}

        {/* Overlay Controller View */}
        <div className={`absolute inset-0 flex flex-col items-center justify-center gap-4 transition-all duration-300 bg-black/40 ${isPlaying ? 'opacity-0 hover:opacity-100 pointer-events-none' : 'opacity-100'}`}>
          <button
            onClick={togglePlay}
            className="w-14 h-14 bg-green-500 text-white rounded-full flex items-center justify-center shadow-lg hover:scale-110 hover:bg-green-400 active:scale-95 transition-all outline-none"
          >
            {isPlaying ? <Pause size={24} fill="currentColor" /> : <Play size={24} fill="currentColor" className="ml-1" />}
          </button>

          <div className="flex items-center gap-3">
            <button
              onClick={handleReplay}
              className="px-4 py-2 bg-white/10 backdrop-blur-md border border-white/20 text-white rounded-xl text-[10px] font-black uppercase tracking-wider hover:bg-white/20 transition-all flex items-center gap-2 shadow-sm"
            >
              <RotateCcw size={14} />
              Replay [ESC]
            </button>
            <button
              onClick={() => setIsLooping(!isLooping)}
              className={`px-4 py-2 backdrop-blur-md border rounded-xl text-[10px] font-black uppercase tracking-wider transition-all flex items-center gap-2 shadow-sm ${isLooping ? 'bg-green-500/80 border-green-400 text-white' : 'bg-white/10 border-white/20 text-white hover:bg-white/20'}`}
            >
              <Repeat size={14} />
              {isLooping ? 'Loop ON' : 'Loop OFF'}
            </button>
          </div>
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
            onClick={handleReplay}
            className="flex-1 py-3 bg-green-50 text-green-600 rounded-xl font-black text-[10px] uppercase tracking-widest border border-green-100 flex items-center justify-center gap-2 active:bg-green-100 transition-all shadow-sm"
          >
            <RotateCcw size={16} />
            Replay
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
