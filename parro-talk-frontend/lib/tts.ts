/**
 * Text-To-Speech (TTS) helper using standard Web Speech API with fallback.
 * 100% free, zero external keys required, works seamlessly across modern browsers.
 */

import { getStoredUserSettings } from '@/features/settings/hooks/useUserSettings';

export interface PlayTTSOptions {
  lang?: 'en-US' | 'en-GB';
  rate?: number;
  onStart?: () => void;
  onEnd?: () => void;
  onError?: (err: unknown) => void;
}

export function playTTS(
  text: string, 
  options?: PlayTTSOptions
): boolean {
  if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
    console.warn('SpeechSynthesis is not supported in this browser environment.');
    options?.onEnd?.();
    return false;
  }

  try {
    window.speechSynthesis.cancel();
    const cleanText = text.replace(/<[^>]*>/g, '').trim();
    if (!cleanText) {
      options?.onEnd?.();
      return false;
    }

    const storedSettings = getStoredUserSettings();
    const defaultLang: 'en-US' | 'en-GB' =
      storedSettings.preferredAccent === 'UK' ? 'en-GB' : 'en-US';
    const utterance = new SpeechSynthesisUtterance(cleanText);
    const lang = options?.lang || defaultLang;
    const rate =
      typeof options?.rate === 'number' && Number.isFinite(options.rate)
        ? options.rate
        : storedSettings.playbackSpeed || 0.92;
    utterance.lang = lang;
    utterance.rate = rate;
    utterance.pitch = 1.0;

    const voices = window.speechSynthesis.getVoices();
    const preferredVoice = voices.find(v => 
      v.lang === lang || 
      v.lang.replace('_', '-') === lang || 
      (lang === 'en-US' && (v.name.includes('Google US English') || v.name.includes('Natural') || v.name.includes('David') || v.name.includes('Zira'))) ||
      (lang === 'en-GB' && (v.name.includes('Google UK English') || v.name.includes('George') || v.name.includes('Hazel')))
    );
    if (preferredVoice) {
      utterance.voice = preferredVoice;
    }

    if (options?.onStart) utterance.onstart = options.onStart;
    utterance.onend = () => options?.onEnd?.();
    utterance.onerror = (e) => {
      console.warn('TTS playback error:', e);
      options?.onEnd?.();
      options?.onError?.(e);
    };

    window.speechSynthesis.speak(utterance);
    return true;
  } catch (err) {
    console.error('Failed to execute TTS:', err);
    options?.onEnd?.();
    options?.onError?.(err);
    return false;
  }
}

export function stopTTS(): void {
  if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
    window.speechSynthesis.cancel();
  }
}

export function playAudioOrTTS(
  text: string, 
  audioUrl?: string, 
  lang?: 'en-US' | 'en-GB',
  callbacks?: {
    onStart?: () => void;
    onEnd?: () => void;
  }
): void {
  const storedSettings = getStoredUserSettings();
  const effectiveLang: 'en-US' | 'en-GB' =
    lang || (storedSettings.preferredAccent === 'UK' ? 'en-GB' : 'en-US');
  const playbackRate = storedSettings.playbackSpeed || 1.0;

  if (audioUrl && audioUrl.trim()) {
    callbacks?.onStart?.();
    const audio = new Audio(audioUrl);
    audio.playbackRate = playbackRate;
    audio.onended = () => callbacks?.onEnd?.();
    audio.onerror = () => {
      // Fallback to TTS if the custom audio fails to load or play
      playTTS(text, { lang: effectiveLang, rate: playbackRate, onStart: callbacks?.onStart, onEnd: callbacks?.onEnd });
    };
    audio.play().catch(() => {
      playTTS(text, { lang: effectiveLang, rate: playbackRate, onStart: callbacks?.onStart, onEnd: callbacks?.onEnd });
    });
  } else {
    playTTS(text, { lang: effectiveLang, rate: playbackRate, onStart: callbacks?.onStart, onEnd: callbacks?.onEnd });
  }
}
