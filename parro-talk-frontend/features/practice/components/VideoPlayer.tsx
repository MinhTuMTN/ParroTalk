"use client";

import Switch from "@/components/ui/Switch";
import { useUserSettings } from "@/features/settings/hooks/useUserSettings";
import { Pause, Play, Zap } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import ReactPlayer from "react-player/lazy";

interface Segment {
  start: number;
  end: number;
  text: string;
}

function isOutOfSegmentBounds(
  currentTime: number,
  seg: Segment,
  startTolerance: number
): boolean {
  if (seg.end <= seg.start) return false;
  const pastEnd =
    seg.end - seg.start > 0.1
      ? currentTime >= seg.end - 0.05
      : currentTime >= seg.end;
  return currentTime < seg.start - startTolerance || pastEnd;
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
  const { settings, updateSettings } = useUserSettings();
  const isLooping = settings.dictationAutoReplay;

  const hasEndedRef = useRef(false);
  const isPlayingRef = useRef(isPlaying);
  useEffect(() => {
    isPlayingRef.current = isPlaying;
  }, [isPlaying]);

  const onReplayRef = useRef(onReplay);
  useEffect(() => {
    onReplayRef.current = onReplay;
  }, [onReplay]);

  const activeSegmentRef = useRef(activeSegment);
  useEffect(() => {
    activeSegmentRef.current = activeSegment;
  }, [activeSegment]);

  const settingsRef = useRef(settings);
  useEffect(() => {
    settingsRef.current = settings;
  }, [settings]);

  // Reset segment end state when activeSegment changes
  useEffect(() => {
    hasEndedRef.current = false;
  }, [activeSegment]);

  // Sync replay metric when user starts playing again after the video segment finishes
  useEffect(() => {
    if (isPlaying && hasEndedRef.current) {
      onReplayRef.current?.();
      hasEndedRef.current = false;
    }
  }, [isPlaying]);

  const playerRef = useRef<ReactPlayer | HTMLVideoElement | null>(null);
  const [progressPercent, setProgressPercent] = useState(0);
  const isYoutube = src?.includes('youtube.com') || src?.includes('youtu.be');

  const isYoutubeReadyRef = useRef(false);
  const lastAutoReplayAtRef = useRef(0);
  const ignorePauseUntilRef = useRef(0);

  useEffect(() => {
    isYoutubeReadyRef.current = false;
  }, [src]);

  const handleReplay = useCallback(() => {
    if (activeSegment && playerRef.current) {
      hasEndedRef.current = false;
      if (isYoutube) {
        ignorePauseUntilRef.current = Date.now() + 300;
        (playerRef.current as ReactPlayer).seekTo(activeSegment.start, 'seconds');
        setIsPlaying(true);
      } else {
        const video = playerRef.current as HTMLVideoElement;
        video.currentTime = activeSegment.start;
        video.play().catch((err) => {
          if (err?.name !== "AbortError") {
            console.log("Playback error", err);
            setIsPlaying(false);
          }
        });
        setIsPlaying(true);
      }
      onReplayRef.current?.();
    }
  }, [activeSegment, isYoutube, setIsPlaying]);

  useEffect(() => {
    const replayKey = settings.dictationReplayKey || "Escape";

    if (replayKey === "Control") {
      let ctrlAlone = false;
      const handleCtrlDown = (e: KeyboardEvent) => {
        if (e.key === "Control" && !e.repeat && !e.altKey && !e.metaKey && !e.shiftKey) {
          ctrlAlone = true;
        } else if (ctrlAlone) {
          ctrlAlone = false;
        }
      };
      const handleCtrlUp = (e: KeyboardEvent) => {
        if (e.key === "Control" && ctrlAlone) {
          ctrlAlone = false;
          e.preventDefault();
          handleReplay();
        }
      };
      window.addEventListener("keydown", handleCtrlDown);
      window.addEventListener("keyup", handleCtrlUp);
      return () => {
        window.removeEventListener("keydown", handleCtrlDown);
        window.removeEventListener("keyup", handleCtrlUp);
      };
    }

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.repeat || e.defaultPrevented) {
        return;
      }
      if (
        e.key === "Escape" &&
        typeof document !== "undefined" &&
        (document.querySelector('[data-quick-settings-open="true"]') ||
          document.querySelector('[data-shortcuts-open="true"]'))
      ) {
        return;
      }
      if (e.key === replayKey && !e.ctrlKey && !e.metaKey && !e.altKey) {
        e.preventDefault();
        handleReplay();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [handleReplay, settings.dictationReplayKey]);

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

  const playHtml5Safely = useCallback((video: HTMLVideoElement) => {
    video.play().catch((err) => {
      if (err?.name !== "AbortError") {
        console.log("Playback error", err);
        setIsPlaying(false);
      }
    });
  }, [setIsPlaying]);

  const replayHtml5FromStart = useCallback((video: HTMLVideoElement, start: number) => {
    const now = Date.now();
    if (now - lastAutoReplayAtRef.current < 350) {
      return false;
    }
    lastAutoReplayAtRef.current = now;
    video.currentTime = start;
    playHtml5Safely(video);
    onReplayRef.current?.();
    return true;
  }, [playHtml5Safely]);

  const replayYoutubeFromStart = useCallback((player: ReactPlayer, start: number) => {
    const now = Date.now();
    if (now - lastAutoReplayAtRef.current < 350) {
      return false;
    }
    lastAutoReplayAtRef.current = now;
    ignorePauseUntilRef.current = now + 300;
    player.seekTo(start, 'seconds');
    onReplayRef.current?.();
    return true;
  }, []);

  const enforceHtml5SegmentBoundary = useCallback((v: HTMLVideoElement) => {
    const seg = activeSegmentRef.current;
    if (!seg || seg.end <= seg.start) return false;
    if (v.seeking) return false;
    const { start, end } = seg;
    const ct = v.currentTime;

    if (!v.paused && ct >= end) {
      if (settingsRef.current.dictationAutoReplay) {
        replayHtml5FromStart(v, start);
      } else {
        v.pause();
        v.currentTime = start;
        setIsPlaying(false);
        hasEndedRef.current = true;
      }
      setProgressPercent(0);
      return true;
    }
    return false;
  }, [replayHtml5FromStart, setIsPlaying]);

  // Handle loop clipping bounding
  useEffect(() => {
    if (isYoutube || !src) return;
    const v = playerRef.current as HTMLVideoElement | null;
    if (!v) return;

    const handleTimeUpdate = () => {
      if (enforceHtml5SegmentBoundary(v)) return;
      const seg = activeSegmentRef.current;
      if (!seg || seg.end <= seg.start) return;
      const { start, end } = seg;
      const ct = v.currentTime;
      const mappedProgress = Math.max(0, Math.min(100, ((ct - start) / (end - start)) * 100));
      setProgressPercent(mappedProgress);
    };

    v.addEventListener('timeupdate', handleTimeUpdate);
    return () => v.removeEventListener('timeupdate', handleTimeUpdate);
  }, [isYoutube, src, enforceHtml5SegmentBoundary]);

  const hasActiveSegment = Boolean(activeSegment);

  // 100ms interval guard for HTML5 video (iOS Safari timeupdate can lag 250-500ms)
  useEffect(() => {
    if (!isPlaying || isYoutube || !hasActiveSegment) return;
    const interval = window.setInterval(() => {
      const v = playerRef.current as HTMLVideoElement | null;
      if (!v) return;
      enforceHtml5SegmentBoundary(v);
    }, 100);
    return () => window.clearInterval(interval);
  }, [isPlaying, isYoutube, hasActiveSegment, enforceHtml5SegmentBoundary]);

  // Handle active Segment change payload
  useEffect(() => {
    if (activeSegment && playerRef.current) {
      hasEndedRef.current = false;
      if (isYoutube) {
        if (isYoutubeReadyRef.current) {
          ignorePauseUntilRef.current = Date.now() + 300;
          (playerRef.current as ReactPlayer).seekTo(activeSegment.start, 'seconds');
        }
        setIsPlaying(true);
      } else {
        const video = playerRef.current as HTMLVideoElement;
        if (video.readyState >= 1) {
          video.currentTime = activeSegment.start;
        }
        if (isPlayingRef.current) {
          playHtml5Safely(video);
        } else {
          setIsPlaying(true);
        }
      }
    }
  }, [activeSegment, isYoutube, playHtml5Safely, setIsPlaying]);

  useEffect(() => {
    if (!isYoutube && playerRef.current) {
      (playerRef.current as HTMLVideoElement).playbackRate = speed;
    }
  }, [speed, isYoutube]);

  // Sync play/pause status and clamp playback position to activeSegment bounds
  useEffect(() => {
    if (isYoutube) {
      const seg = activeSegmentRef.current;
      const player = playerRef.current as ReactPlayer | null;
      if (isPlaying && isYoutubeReadyRef.current && seg && player) {
        const ct = player.getCurrentTime?.();
        if (typeof ct === 'number' && isOutOfSegmentBounds(ct, seg, 0.25)) {
          ignorePauseUntilRef.current = Date.now() + 300;
          player.seekTo(seg.start, 'seconds');
        }
      }
      return;
    }

    const video = playerRef.current as HTMLVideoElement | null;
    if (!video) return;

    if (isPlaying) {
      const seg = activeSegmentRef.current;
      if (seg && video.readyState >= 1 && isOutOfSegmentBounds(video.currentTime, seg, 0.15)) {
        video.currentTime = seg.start;
      }
      if (video.paused) {
        playHtml5Safely(video);
      }
    } else {
      if (!video.paused) {
        video.pause();
      }
    }
  }, [isPlaying, isYoutube, playHtml5Safely]);

  const handleLoadedMetadata = () => {
    const video = playerRef.current as HTMLVideoElement | null;
    const seg = activeSegmentRef.current;
    if (video && seg) {
      video.currentTime = seg.start;
      if (isPlayingRef.current && video.paused) {
        playHtml5Safely(video);
      }
    }
  };

  const handleVideoPlay = () => {
    hasEndedRef.current = false;
    const video = playerRef.current as HTMLVideoElement | null;
    const seg = activeSegmentRef.current;
    if (video && seg && isOutOfSegmentBounds(video.currentTime, seg, 0.15)) {
      video.currentTime = seg.start;
    }
  };

  const handleVideoEnded = () => {
    const video = playerRef.current as HTMLVideoElement | null;
    const seg = activeSegmentRef.current;
    if (!video || !seg || seg.end <= seg.start) return;

    if (settingsRef.current.dictationAutoReplay) {
      replayHtml5FromStart(video, seg.start);
    } else {
      video.currentTime = seg.start;
      hasEndedRef.current = true;
      setIsPlaying(false);
    }
    setProgressPercent(0);
  };

  const handleYoutubeReady = () => {
    isYoutubeReadyRef.current = true;
    const seg = activeSegmentRef.current;
    if (seg && playerRef.current) {
      (playerRef.current as ReactPlayer).seekTo(seg.start, 'seconds');
    }
  };

  const handleYoutubePlay = () => {
    hasEndedRef.current = false;
    if (!isPlayingRef.current) {
      setIsPlaying(true);
    }
    const seg = activeSegmentRef.current;
    const player = playerRef.current as ReactPlayer | null;
    if (seg && player) {
      const ct = player.getCurrentTime?.();
      if (typeof ct === 'number' && isOutOfSegmentBounds(ct, seg, 0.25)) {
        ignorePauseUntilRef.current = Date.now() + 300;
        player.seekTo(seg.start, 'seconds');
      }
    }
  };

  const handleYoutubePause = () => {
    if (!isPlayingRef.current || Date.now() < ignorePauseUntilRef.current) {
      return;
    }
    setIsPlaying(false);
  };

  const handleYoutubeEnded = () => {
    const player = playerRef.current as ReactPlayer | null;
    const seg = activeSegmentRef.current;
    if (!player || !seg || seg.end <= seg.start) return;

    if (settingsRef.current.dictationAutoReplay) {
      replayYoutubeFromStart(player, seg.start);
    } else {
      hasEndedRef.current = true;
      setIsPlaying(false);
    }
    setProgressPercent(0);
  };

  const togglePlay = () => {
    if (isYoutube) {
      if (!isPlaying) {
        hasEndedRef.current = false;
      }
      setIsPlaying(!isPlaying);
    } else if (playerRef.current) {
      const video = playerRef.current as HTMLVideoElement;
      if (video.paused) {
        hasEndedRef.current = false;
        const seg = activeSegmentRef.current;
        if (seg && video.readyState >= 1 && isOutOfSegmentBounds(video.currentTime, seg, 0.15)) {
          video.currentTime = seg.start;
        }
        playHtml5Safely(video);
      } else {
        video.pause();
      }
      setIsPlaying(!video.paused);
    }
  };

  const toggleSpeed = useCallback(() => {
    setSpeed((prev) =>
      prev >= 1.5 ? 0.5 : Math.min(1.5, Math.round((prev + 0.25) * 100) / 100)
    );
  }, [setSpeed]);


  const onYoutubeProgress = (state: { playedSeconds: number }) => {
    const seg = activeSegmentRef.current;
    const player = playerRef.current as ReactPlayer | null;
    if (!isYoutube || !seg || seg.end <= seg.start || !player) return;

    const { start, end } = seg;
    const ct = state.playedSeconds;

    if (ct >= end) {
      if (settingsRef.current.dictationAutoReplay) {
        replayYoutubeFromStart(player, start);
      } else {
        const internal = (
          playerRef.current as unknown as {
            getInternalPlayer?: () => { pauseVideo?: () => void };
          }
        )?.getInternalPlayer?.();
        internal?.pauseVideo?.();
        setIsPlaying(false);
        hasEndedRef.current = true;
      }
      setProgressPercent(0);
      return;
    }

    const mappedProgress = Math.max(0, Math.min(100, ((ct - start) / (end - start)) * 100));
    setProgressPercent(mappedProgress);
  };

  const handleProgressClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!activeSegment || activeSegment.end <= activeSegment.start || !playerRef.current) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const clickPercent = Math.max(0, Math.min(1, clickX / rect.width));
    const seekTime = activeSegment.start + clickPercent * (activeSegment.end - activeSegment.start);

    if (isYoutube) {
      if (isPlaying) {
        ignorePauseUntilRef.current = Date.now() + 300;
      }
      (playerRef.current as ReactPlayer).seekTo(seekTime, 'seconds');
    } else {
      (playerRef.current as HTMLVideoElement).currentTime = seekTime;
    }
    setProgressPercent(clickPercent * 100);
  };

  return (
    <div className="flex flex-col gap-3 w-full max-w-[480px] shrink-0">
      <div className="relative aspect-[4/3] bg-gray-950 rounded-3xl overflow-hidden group shadow-xl border border-gray-100 dark:border-slate-800">
        {src ? (
          isYoutube ? (
            <div className="absolute inset-0 w-full h-full">
              <ReactPlayer
                ref={playerRef as React.RefObject<ReactPlayer | null>}
                url={src}
                playing={isPlaying}
                playbackRate={speed}
                progressInterval={100}
                width="100%"
                height="100%"
                onReady={handleYoutubeReady}
                onPlay={handleYoutubePlay}
                onPause={handleYoutubePause}
                onEnded={handleYoutubeEnded}
                onProgress={onYoutubeProgress}
                playsinline
                config={{
                  youtube: {
                    playerVars: {
                      autoplay: 0,
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
              onLoadedMetadata={handleLoadedMetadata}
              onPlay={handleVideoPlay}
              onEnded={handleVideoEnded}
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
      <div className="bg-white dark:bg-slate-900 rounded-2xl p-4 border border-gray-100 dark:border-slate-800 shadow-sm flex flex-col gap-4">
        {/* Lesson Info Section */}
        {lessonTitle && (
          <div className="flex flex-col gap-1 pb-3 border-b border-gray-100 dark:border-slate-800">
            <h3 className="text-sm font-black text-gray-800 dark:text-slate-100 leading-snug tracking-tight">
              {lessonTitle}
            </h3>
            <div className="flex items-center gap-2 text-[9px] font-black text-gray-400 dark:text-slate-400 uppercase tracking-widest">
              {lessonDuration !== undefined && lessonDuration > 0 && (
                <>
                  <span>{formatDuration(lessonDuration)}</span>
                  <span className="text-gray-200 dark:text-slate-700">•</span>
                </>
              )}
              <span>{totalSegments} segments</span>
            </div>
          </div>
        )}

        <div className="flex items-center justify-between">
          {/* Loop Setting */}
          <div className="flex flex-row items-center gap-4">
            <span className="text-[9px] font-black text-gray-400 dark:text-slate-400 uppercase tracking-widest">
              Auto replay
            </span>

            <Switch
              checked={isLooping}
              onChange={() =>
                void updateSettings({ dictationAutoReplay: !isLooping })
              }
            />
          </div>

          {/* Speed Setting */}
          <div className="flex flex-row items-center gap-4">
            <span className="text-[9px] font-black text-gray-400 dark:text-slate-400 uppercase tracking-widest">
              Speed
            </span>

            <div className="flex bg-gray-50 dark:bg-slate-800 p-0.5 rounded-xl gap-0.5 border border-gray-100 dark:border-slate-700">
              <button
                onClick={toggleSpeed}
                className="px-3 py-1.5 rounded-lg text-[10px] font-black transition-all active:scale-95 bg-white dark:bg-slate-700 text-green-600 dark:text-emerald-300 shadow-sm border border-gray-100 dark:border-slate-600"
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
