"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useUserSettings } from "@/features/settings/hooks/useUserSettings";
import { playTTS, stopTTS } from "@/lib/tts";
import {
  resolveWritingAudioUrl,
  type WritingAccent,
} from "../services/writingService";

const CANPLAY_FALLBACK_TIMEOUT_MS = 2500;
const MAX_PRELOAD_CACHE_SIZE = 30;
const preloadedAudioCache = new Map<string, HTMLAudioElement>();

/**
 * Checks whether an audio URL targets the backend `/writing/tts` endpoint.
 */
export function isWritingTtsAudioUrl(url?: string): boolean {
  if (!url) return false;
  const [pathWithoutQuery] = url.split(/[?#]/, 1);
  return /\/writing\/tts$/i.test(pathWithoutQuery);
}

/**
 * Warms up an Edge-TTS audio clip into a bounded LRU cache before click.
 * Only preloads URLs targeting `/writing/tts` to avoid hammering external CDNs.
 */
export function preloadWritingAudio(url?: string): void {
  if (
    typeof window === "undefined" ||
    typeof Audio === "undefined" ||
    !url ||
    !isWritingTtsAudioUrl(url)
  ) {
    return;
  }

  const existing = preloadedAudioCache.get(url);
  if (existing) {
    if (!existing.error) {
      preloadedAudioCache.delete(url);
      preloadedAudioCache.set(url, existing);
      return;
    }
    preloadedAudioCache.delete(url);
  }

  if (preloadedAudioCache.size >= MAX_PRELOAD_CACHE_SIZE) {
    const oldestKey = preloadedAudioCache.keys().next().value;
    if (oldestKey !== undefined) {
      preloadedAudioCache.delete(oldestKey);
    }
  }

  const audio = new Audio(url);
  audio.preload = "auto";
  audio.onerror = () => {
    if (preloadedAudioCache.get(url) === audio) {
      preloadedAudioCache.delete(url);
    }
  };
  audio.load();
  preloadedAudioCache.set(url, audio);
}

function discardAudioElement(audio: HTMLAudioElement, url?: string): void {
  audio.oncanplay = null;
  audio.onended = null;
  audio.onerror = null;
  audio.pause();
  if (url) {
    preloadedAudioCache.delete(url);
  }
  if (audio.readyState < 3) {
    audio.removeAttribute("src");
    audio.load();
  }
}

/**
 * Manages single-track audio playback for Writing Assistant with preloading,
 * bounded `canplay` wait before Web Speech fallback, stale cancellation, and unmount cleanup.
 */
export function useWritingAudio() {
  const { settings } = useUserSettings();
  const playbackSpeed = settings.playbackSpeed || 1.0;
  const [playingAudioKey, setPlayingAudioKey] = useState<string | null>(null);
  const [loadingAudioKey, setLoadingAudioKey] = useState<string | null>(null);
  const activeAudioRef = useRef<HTMLAudioElement | null>(null);
  const activeAudioUrlRef = useRef<string | null>(null);
  const fallbackTimerRef = useRef<number | null>(null);
  const playbackIdRef = useRef(0);

  const clearFallbackTimer = useCallback(() => {
    if (fallbackTimerRef.current !== null) {
      window.clearTimeout(fallbackTimerRef.current);
      fallbackTimerRef.current = null;
    }
  }, []);

  const stopActiveMedia = useCallback(
    (resetState = true) => {
      playbackIdRef.current += 1;
      clearFallbackTimer();
      const currentAudio = activeAudioRef.current;
      const currentUrl = activeAudioUrlRef.current;
      if (currentAudio) {
        // Detach handlers before pausing so AbortError/onerror from the interrupted clip cannot trigger fallback.
        currentAudio.oncanplay = null;
        currentAudio.onended = null;
        currentAudio.onerror = null;
        currentAudio.pause();
        if (currentAudio.readyState > 0 && currentAudio.readyState >= 3) {
          currentAudio.currentTime = 0;
        } else if (currentAudio.readyState < 3) {
          if (currentUrl) {
            preloadedAudioCache.delete(currentUrl);
          }
          currentAudio.removeAttribute("src");
          currentAudio.load();
        }
        activeAudioRef.current = null;
        activeAudioUrlRef.current = null;
      }
      stopTTS();
      if (resetState) {
        setLoadingAudioKey(null);
        setPlayingAudioKey(null);
      }
    },
    [clearFallbackTimer]
  );

  useEffect(() => {
    return () => {
      stopActiveMedia(false);
    };
  }, [stopActiveMedia]);

  const playWritingAudio = useCallback(
    (key: string, text: string, accent: WritingAccent, audioUrl?: string) => {
      stopActiveMedia(false);
      const playbackId = ++playbackIdRef.current;
      const cleanedText = text.trim();

      if (!cleanedText) {
        setLoadingAudioKey(null);
        setPlayingAudioKey(null);
        return;
      }

      const fallbackToWebSpeech = () => {
        if (playbackIdRef.current !== playbackId) return;
        clearFallbackTimer();
        setLoadingAudioKey(null);
        setPlayingAudioKey(key);
        const lang = accent === "UK" ? "en-GB" : "en-US";
        const started = playTTS(cleanedText, {
          lang,
          rate: playbackSpeed,
          onEnd: () => {
            if (playbackIdRef.current === playbackId) {
              setPlayingAudioKey(null);
            }
          },
          onError: () => {
            if (playbackIdRef.current === playbackId) {
              setPlayingAudioKey(null);
            }
          },
        });

        if (!started && playbackIdRef.current === playbackId) {
          setPlayingAudioKey(null);
        }
      };

      const preferredVoice =
        accent === "UK" ? settings.preferredUkVoice : settings.preferredUsVoice;
      const resolvedUrl = resolveWritingAudioUrl(
        audioUrl,
        cleanedText,
        accent,
        preferredVoice
      );
      if (!resolvedUrl || typeof Audio === "undefined") {
        fallbackToWebSpeech();
        return;
      }

      const cachedAudio = preloadedAudioCache.get(resolvedUrl);
      const canReuseCached = Boolean(cachedAudio && !cachedAudio.error);
      const audio = canReuseCached ? cachedAudio! : new Audio(resolvedUrl);
      if (canReuseCached) {
        preloadedAudioCache.delete(resolvedUrl);
        preloadedAudioCache.set(resolvedUrl, audio);
      } else {
        audio.preload = "auto";
        if (isWritingTtsAudioUrl(resolvedUrl)) {
          if (preloadedAudioCache.size >= MAX_PRELOAD_CACHE_SIZE) {
            const oldestKey = preloadedAudioCache.keys().next().value;
            if (oldestKey !== undefined) {
              preloadedAudioCache.delete(oldestKey);
            }
          }
          preloadedAudioCache.set(resolvedUrl, audio);
        }
      }

      activeAudioRef.current = audio;
      activeAudioUrlRef.current = resolvedUrl;
      let settled = false;

      const handleFailure = () => {
        if (settled || playbackIdRef.current !== playbackId) return;
        settled = true;
        clearFallbackTimer();
        discardAudioElement(audio, resolvedUrl);
        if (activeAudioRef.current === audio) {
          activeAudioRef.current = null;
          activeAudioUrlRef.current = null;
        }
        fallbackToWebSpeech();
      };

      const startAudioElement = () => {
        if (settled || playbackIdRef.current !== playbackId) return;
        settled = true;
        clearFallbackTimer();
        audio.oncanplay = null;
        audio.playbackRate = playbackSpeed;
        if (audio.readyState > 0) {
          audio.currentTime = 0;
        }
        setLoadingAudioKey(null);
        setPlayingAudioKey(key);
        audio.play().catch(() => {
          if (playbackIdRef.current !== playbackId) return;
          discardAudioElement(audio, resolvedUrl);
          if (activeAudioRef.current === audio) {
            activeAudioRef.current = null;
            activeAudioUrlRef.current = null;
          }
          fallbackToWebSpeech();
        });
      };

      audio.onended = () => {
        if (activeAudioRef.current === audio) {
          activeAudioRef.current = null;
          activeAudioUrlRef.current = null;
        }
        if (playbackIdRef.current === playbackId) {
          setLoadingAudioKey(null);
          setPlayingAudioKey(null);
        }
      };
      audio.onerror = handleFailure;

      if (audio.readyState >= 3) {
        startAudioElement();
        return;
      }

      setPlayingAudioKey(null);
      setLoadingAudioKey(key);
      audio.oncanplay = startAudioElement;
      fallbackTimerRef.current = window.setTimeout(() => {
        handleFailure();
      }, CANPLAY_FALLBACK_TIMEOUT_MS);
    },
    [
      clearFallbackTimer,
      playbackSpeed,
      settings.preferredUkVoice,
      settings.preferredUsVoice,
      stopActiveMedia,
    ]
  );

  return {
    playingAudioKey,
    loadingAudioKey,
    playWritingAudio,
  };
}
