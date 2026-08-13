"use client";

import Switch from "@/components/ui/Switch";
import { Pause, Play, Zap } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import ReactPlayer from "react-player/lazy";

interface Segment {
  start: number;
  end: number;
  text: string;
}

const formatDuration = (seconds: number) => {
  const m = Math.floor(seconds / 60);
  const s = Math.round(seconds % 60);
  if (m === 0) return `${s}s`;
  return `${m}m ${s}s`;
};

export default function VideoPlayer({
  src,
  activeSegment,
  onReplay,
  isPlaying: isPlayingProp,
  setIsPlaying: setIsPlayingProp,
  replayTrigger = 0,
  speed: speedProp,
  setSpeed: setSpeedProp,
  lessonTitle,
  lessonDuration,
  totalSegments
}: {
  src?: string,
  activeSegment?: Segment,
  onReplay?: () => void,
  isPlaying?: boolean,
  setIsPlaying?: React.Dispatch<React.SetStateAction<boolean>> | ((val: boolean) => void),
  replayTrigger?: number,
  speed?: number,
  setSpeed?: React.Dispatch<React.SetStateAction<number>>,
  lessonTitle?: string,
  lessonDuration?: number,
  totalSegments?: number
}) {
  const [localIsPlaying, localSetIsPlaying] = useState(false);
  const isPlaying = isPlayingProp !== undefined ? isPlayingProp : localIsPlaying;
  const setIsPlaying = setIsPlayingProp !== undefined ? setIsPlayingProp : localSetIsPlaying;

  const [localSpeed, localSetSpeed] = useState(1);
  const speed = speedProp !== undefined ? speedProp : localSpeed;
  const setSpeed = setSpeedProp !== undefined ? setSpeedProp : localSetSpeed;
  const [isLooping, setIsLooping] = useState(() => {
    if (typeof window !== "undefined") {
      const saved = localStorage.getItem("parrotalk_video_loop");
      return saved !== null ? JSON.parse(saved) : false;
    }
    return false;
  });

  const hasEndedRef = useRef(false);

  // Reset segment end state when activeSegment changes
  useEffect(() => {
    hasEndedRef.current = false;
  }, [activeSegment]);

  // Sync replay metric when user starts playing again after the video segment finishes
  useEffect(() => {
    if (isPlaying && hasEndedRef.current) {
      if (onReplay) onReplay();
      hasEndedRef.current = false;
    }
  }, [isPlaying, onReplay]);

  const playerRef = useRef<ReactPlayer | HTMLVideoElement | null>(null);
  const [progressPercent, setProgressPercent] = useState(0);
  const isYoutube = src?.includes('youtube.com') || src?.includes('youtu.be');

  useEffect(() => {
    localStorage.setItem("parrotalk_video_loop", JSON.stringify(isLooping));
  }, [isLooping]);

  const handleReplay = useCallback(() => {
    if (activeSegment && playerRef.current) {
      if (isYoutube) {
        (playerRef.current as ReactPlayer).seekTo(activeSegment.start, 'seconds');
        setIsPlaying(true);
      } else {
        const video = playerRef.current as HTMLVideoElement;
        video.currentTime = activeSegment.start;
        video.play().catch(e => console.log("Playback error", e));
        setIsPlaying(true);
      }
      if (onReplay) onReplay();
    }
  }, [activeSegment, onReplay, isYoutube, setIsPlaying]);

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

  // Use ref to hold latest handleReplay closure and avoid infinite rendering loops
  const handleReplayRef = useRef(handleReplay);
  useEffect(() => {
    handleReplayRef.current = handleReplay;
  }, [handleReplay]);

  // Handle external replay trigger
  useEffect(() => {
    if (replayTrigger > 0) {
      handleReplayRef.current();
    }
  }, [replayTrigger]);

  // Handle loop clipping bounding
  useEffect(() => {
    if (isYoutube || !activeSegment) return;
    const v = playerRef.current as HTMLVideoElement;
    if (!v) return;

    const handleTimeUpdate = () => {
      const ct = v.currentTime;
      const { start, end } = activeSegment;

      if (ct >= end) {
        v.currentTime = start;
        if (isLooping) {
          v.play().catch(e => console.log(e));
          if (onReplay) onReplay();
        } else {
          v.pause();
          setIsPlaying(false);
          hasEndedRef.current = true;
        }
      }

      const mappedProgress = Math.max(0, Math.min(100, ((ct - start) / (end - start)) * 100));
      setProgressPercent(mappedProgress);
      if (v.paused !== !isPlaying) setIsPlaying(!v.paused);
    };

    v.addEventListener('timeupdate', handleTimeUpdate);
    return () => v.removeEventListener('timeupdate', handleTimeUpdate);
  }, [activeSegment, isLooping, isPlaying, isYoutube, onReplay, setIsPlaying]);

  // Handle active Segment change payload
  useEffect(() => {
    if (activeSegment && playerRef.current) {
      if (isYoutube) {
        (playerRef.current as ReactPlayer).seekTo(activeSegment.start, 'seconds');
        setIsPlaying(true);
      } else {
        const video = playerRef.current as HTMLVideoElement;
        video.currentTime = activeSegment.start;
        video.play()
          .then(() => setIsPlaying(true))
          .catch(e => {
            console.log("Playback blocked or failed:", e);
            setIsPlaying(false);
          });
      }
    }
  }, [activeSegment, isYoutube, setIsPlaying]);

  useEffect(() => {
    if (!isYoutube && playerRef.current) {
      (playerRef.current as HTMLVideoElement).playbackRate = speed;
    }
  }, [speed, isYoutube]);

  // Sync HTML5 video play/pause status with isPlaying prop
  useEffect(() => {
    if (isYoutube) return;
    const video = playerRef.current as HTMLVideoElement;
    if (!video) return;

    if (isPlaying) {
      if (video.paused) {
        video.play().catch(e => console.log("Playback error", e));
      }
    } else {
      if (!video.paused) {
        video.pause();
      }
    }
  }, [isPlaying, isYoutube]);

  const togglePlay = () => {
    if (isYoutube) {
      setIsPlaying(!isPlaying);
    } else if (playerRef.current) {
      const video = playerRef.current as HTMLVideoElement;
      if (video.paused) {
        video.play().catch(e => console.log(e));
      } else {
        video.pause();
      }
      setIsPlaying(!video.paused);
    }
  };

  const toggleSpeed = useCallback(() => {
    setSpeed(prev => prev === 1.5 ? 0.5 : prev + 0.25);
  }, [setSpeed]);


  const onYoutubeProgress = (state: { playedSeconds: number }) => {
    if (isYoutube && activeSegment && playerRef.current) {
      const { start, end } = activeSegment;
      const ct = state.playedSeconds;

      if (ct >= end) {
        if (isLooping) {
          (playerRef.current as ReactPlayer).seekTo(start, 'seconds');
          if (onReplay) onReplay();
        } else {
          setIsPlaying(false);
          (playerRef.current as ReactPlayer).seekTo(start, 'seconds');
          hasEndedRef.current = true;
        }
      }

      const mappedProgress = Math.max(0, Math.min(100, ((ct - start) / (end - start)) * 100));
      setProgressPercent(mappedProgress);
    }
  };

  const handleProgressClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!activeSegment || !playerRef.current) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const clickPercent = Math.max(0, Math.min(1, clickX / rect.width));
    const seekTime = activeSegment.start + clickPercent * (activeSegment.end - activeSegment.start);

    if (isYoutube) {
      (playerRef.current as ReactPlayer).seekTo(seekTime, 'seconds');
    } else {
      (playerRef.current as HTMLVideoElement).currentTime = seekTime;
    }
    setProgressPercent(clickPercent * 100);
  };

  return (
    <div className="flex flex-col gap-3 w-full max-w-[480px] shrink-0">
      <div className="relative aspect-[4/3] bg-gray-950 rounded-3xl overflow-hidden group shadow-xl border border-gray-100">
        {src ? (
          isYoutube ? (
            <div className="absolute inset-0 w-full h-full">
              <ReactPlayer
                ref={playerRef as React.RefObject<ReactPlayer | null>}
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
              ref={playerRef as React.RefObject<HTMLVideoElement | null>}
              src={src}
              className="w-full h-full object-cover opacity-90 transition-opacity"
              playsInline
            />
          )
        ) : (
          <div className="w-full h-full flex items-center justify-center text-gray-500 font-bold select-none bg-gray-900 animate-pulse">Loading Source...</div>
        )}

        {/* Overlay Play/Pause Control for Non-YouTube Media */}
        {!isYoutube && src && (
          <div
            onClick={togglePlay}
            className={`
              absolute inset-0 flex items-center justify-center cursor-pointer z-20 select-none transition-all duration-300
              ${isPlaying ? "bg-transparent hover:bg-black/15" : "bg-black/25"}
            `}
          >
            <div
              className={`
                flex items-center justify-center w-14 h-14 rounded-full
                bg-white/20 backdrop-blur-md border border-white/30 text-white
                shadow-xl transition-all duration-300 transform
                hover:scale-110 hover:bg-white/30 active:scale-95
                ${isPlaying ? "opacity-0 group-hover:opacity-100" : "opacity-100 bg-white/30 scale-105"}
              `}
            >
              {isPlaying ? (
                <Pause size={24} className="text-white" fill="currentColor" />
              ) : (
                <Play size={24} className="text-white translate-x-[1px]" fill="currentColor" />
              )}
            </div>
          </div>
        )}

        {/* Speed indicator on video */}
        <div className="absolute top-4 right-4 bg-black/40 backdrop-blur-md px-2 py-1 rounded-lg border border-white/10 text-[9px] font-black text-white uppercase tracking-widest flex items-center gap-1 shadow-sm">
          <Zap size={10} className="text-yellow-400" fill="currentColor" />
          {speed}x
        </div>

        {/* Progress Bar Overlaid on Bottom */}
        <div 
          className="absolute bottom-0 left-0 right-0 h-2 bg-white/20 flex z-30 overflow-hidden cursor-pointer hover:h-3 transition-all"
          onClick={handleProgressClick}
        >
          <div className="h-full bg-green-500 transition-all duration-100 ease-linear shadow-[0_0_10px_rgba(34,197,94,0.5)] relative" style={{ width: `${progressPercent}%` }}>
            <div className="absolute top-0 right-0 bottom-0 w-2 bg-white/50 blur-sm rounded-full animate-pulse" />
          </div>
        </div>
      </div>
      {/* Compact Controls Card */}
      <div className="bg-white rounded-2xl p-4 border border-gray-100 shadow-sm flex flex-col gap-4">
        {/* Lesson Info Section */}
        {lessonTitle && (
          <div className="flex flex-col gap-1 pb-3 border-b border-gray-100">
            <h3 className="text-sm font-black text-gray-800 leading-snug tracking-tight">
              {lessonTitle}
            </h3>
            <div className="flex items-center gap-2 text-[9px] font-black text-gray-400 uppercase tracking-widest">
              {lessonDuration !== undefined && lessonDuration > 0 && (
                <>
                  <span>{formatDuration(lessonDuration)}</span>
                  <span className="text-gray-200">•</span>
                </>
              )}
              <span>{totalSegments} segments</span>
            </div>
          </div>
        )}

        <div className="flex items-center justify-between">
          {/* Loop Setting */}
          <div className="flex flex-row items-center gap-4">
            <span className="text-[9px] font-black text-gray-400 uppercase tracking-widest">
              Auto replay
            </span>

            <Switch
              checked={isLooping}
              onChange={() => setIsLooping(!isLooping)}
            />
          </div>

          {/* Speed Setting */}
          <div className="flex flex-row items-center gap-4">
            <span className="text-[9px] font-black text-gray-400 uppercase tracking-widest">
              Speed
            </span>

            <div className="flex bg-gray-50 p-0.5 rounded-xl gap-0.5 border border-gray-100">
              <button
                onClick={toggleSpeed}
                className="px-3 py-1.5 rounded-lg text-[10px] font-black transition-all active:scale-95 bg-white text-green-600 shadow-sm border border-gray-100"
              >
                {speed}x
              </button>
            </div>
          </div>
        </div>
      </div>

    </div>
  );
}
