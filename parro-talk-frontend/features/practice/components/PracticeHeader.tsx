"use client";

import { useEffect, useRef, useState } from "react";
import { Settings, Menu, SlidersHorizontal, ArrowUpRight } from "lucide-react";
import Link from "next/link";
import { useAuth } from "@/features/auth/hooks/useAuth";
import Switch from "@/components/ui/Switch";
import { useUserSettings } from "@/features/settings/hooks/useUserSettings";
import {
  PLAYBACK_SPEED_OPTIONS,
  REPLAY_KEY_OPTIONS,
} from "@/features/settings/types";
import { useUI } from "@/hooks/useUI";

interface PracticeHeaderProps {
  currentSentence: number;
  totalSentences: number;
  percent: number;
  onFinish?: () => void;
  isAllCompleted?: boolean;
  canSubmit?: boolean;
}

export default function PracticeHeader({ currentSentence, totalSentences, percent, onFinish, isAllCompleted, canSubmit }: PracticeHeaderProps) {
  const { user } = useAuth();
  const { openMobileMenu } = useUI();
  const { settings, updateSettings } = useUserSettings();
  const [isQuickSettingsOpen, setIsQuickSettingsOpen] = useState(false);
  const [failedAvatarUrl, setFailedAvatarUrl] = useState<string | null>(null);
  const popoverRef = useRef<HTMLDivElement>(null);

  const avatarSrc = user?.avatarUrl?.trim() || "";
  const showAvatarImage = Boolean(avatarSrc) && failedAvatarUrl !== avatarSrc;
  const isEn = settings.uiLanguage === "en";

  useEffect(() => {
    if (!isQuickSettingsOpen) {
      return;
    }
    const handleOutsideClick = (event: MouseEvent) => {
      if (
        popoverRef.current &&
        !popoverRef.current.contains(event.target as Node)
      ) {
        setIsQuickSettingsOpen(false);
      }
    };
    const handleEscapeCapture = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        event.stopPropagation();
        event.stopImmediatePropagation();
        setIsQuickSettingsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleOutsideClick);
    window.addEventListener("keydown", handleEscapeCapture, true);
    return () => {
      document.removeEventListener("mousedown", handleOutsideClick);
      window.removeEventListener("keydown", handleEscapeCapture, true);
    };
  }, [isQuickSettingsOpen]);

  return (
    <header className="h-16 bg-white/90 dark:bg-slate-900/95 border-b border-gray-100 dark:border-slate-800 px-4 md:px-6 flex items-center justify-between sticky top-0 z-50 backdrop-blur-md">
      <div className="flex items-center gap-4">
        <button
          onClick={openMobileMenu}
          className="lg:hidden p-2 hover:bg-gray-100 dark:hover:bg-slate-800 rounded-xl text-gray-400 dark:text-slate-400 transition-all active:scale-95"
        >
          <Menu size={24} />
        </button>

        <Link href="/library" className="hidden sm:flex items-center gap-2 group">
          <div className="w-8 h-8 bg-green-500 rounded-lg flex items-center justify-center text-white shadow-lg shadow-green-100 dark:shadow-none group-hover:scale-110 transition-transform">
            <span className="font-black text-xs">P</span>
          </div>
          <span className="font-black text-sm text-gray-800 dark:text-slate-100 tracking-tight">ParroTalk</span>
        </Link>
      </div>

      <div className="flex-1 max-w-md px-4 sm:px-10">
        <div className="flex items-center justify-between mb-1.5 px-0.5">
          <span className="text-[9px] font-black text-gray-400 dark:text-slate-400 uppercase tracking-widest flex items-center gap-2">
            <span className="hidden xs:inline">{isEn ? "Lesson Progress" : "Tiến độ bài học"}</span>
            <span className="text-gray-300 dark:text-slate-600">•</span>
            <span className="text-gray-600 dark:text-slate-300">
              {isEn ? `Sentence ${currentSentence}/${totalSentences}` : `Câu ${currentSentence}/${totalSentences}`}
            </span>
          </span>
          <span className="text-[9px] font-black text-green-500 dark:text-emerald-300 uppercase tracking-widest bg-green-50 dark:bg-emerald-950/60 px-1.5 py-0.5 rounded">{percent}%</span>
        </div>
        <div className="h-1.5 w-full bg-gray-100 dark:bg-slate-800 rounded-full overflow-hidden shadow-inner">
          <div className="h-full bg-green-500 transition-all duration-1000 ease-out shadow-[0_0_8px_rgba(34,197,94,0.4)]" style={{ width: `${percent}%` }} />
        </div>
      </div>

      <div className="flex items-center gap-1 sm:gap-3">
        {(canSubmit || isAllCompleted) && (
          <button
            type="button"
            aria-label="Submit lesson"
            onClick={onFinish}
            className="flex items-center gap-1.5 px-3 py-1.5 sm:px-4 sm:py-2 bg-green-500 text-white rounded-xl text-xs font-black uppercase tracking-widest hover:bg-green-600 transition-all shadow-lg shadow-green-100 dark:shadow-none animate-bounce-subtle active:scale-95 cursor-pointer"
          >
            {isEn ? "Submit" : "Nộp bài"}
          </button>
        )}
        <div
          className="relative"
          ref={popoverRef}
          data-quick-settings-open={isQuickSettingsOpen ? "true" : undefined}
        >
          <button
            type="button"
            aria-label={isEn ? "Quick settings" : "Cài đặt nhanh"}
            aria-expanded={isQuickSettingsOpen}
            onClick={() => setIsQuickSettingsOpen((prev) => !prev)}
            className={`flex p-2 rounded-xl transition-all cursor-pointer ${
              isQuickSettingsOpen
                ? "bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-300"
                : "text-gray-400 dark:text-slate-400 hover:text-gray-800 dark:hover:text-slate-100 hover:bg-gray-50 dark:hover:bg-slate-800"
            }`}
          >
            <Settings size={18} />
          </button>

          {isQuickSettingsOpen && (
            <div className="absolute right-0 mt-2 w-80 rounded-2xl bg-white dark:bg-slate-900 border border-gray-100 dark:border-slate-700 shadow-2xl p-4 z-50 space-y-3.5 text-slate-900 dark:text-slate-100">
              <div className="flex items-center justify-between border-b border-gray-100 dark:border-slate-800 pb-2.5">
                <div className="flex items-center gap-2 text-xs font-extrabold text-gray-800 dark:text-slate-100">
                  <SlidersHorizontal size={14} className="text-emerald-600 dark:text-emerald-400" />
                  <span>
                    {isEn ? "Quick Practice Settings" : "Cài đặt luyện tập nhanh"}
                  </span>
                </div>
                <Link
                  href="/settings"
                  onClick={() => setIsQuickSettingsOpen(false)}
                  className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-600 dark:text-emerald-400 hover:text-emerald-700 dark:hover:text-emerald-300"
                >
                  <span>{isEn ? "All settings" : "Tất cả cài đặt"}</span>
                  <ArrowUpRight size={12} />
                </Link>
              </div>

              <div className="space-y-3 text-xs">
                <div className="flex items-center justify-between gap-3">
                  <span className="font-semibold text-gray-700 dark:text-slate-200">
                    {isEn ? "Auto-advance sentence" : "Tự động chuyển câu"}
                  </span>
                  <Switch
                    checked={settings.dictationAutoAdvance}
                    onChange={(checked) =>
                      void updateSettings({ dictationAutoAdvance: checked })
                    }
                  />
                </div>

                <div className="flex items-center justify-between gap-3">
                  <span className="font-semibold text-gray-700 dark:text-slate-200">
                    {isEn ? "Show translation" : "Hiện bản dịch ngay"}
                  </span>
                  <Switch
                    checked={settings.dictationShowTranslation}
                    onChange={(checked) =>
                      void updateSettings({ dictationShowTranslation: checked })
                    }
                  />
                </div>

                <div className="flex items-center justify-between gap-3">
                  <span className="font-semibold text-gray-700 dark:text-slate-200">
                    {isEn ? "Auto replay" : "Tự động phát lại"}
                  </span>
                  <Switch
                    checked={settings.dictationAutoReplay}
                    onChange={(checked) =>
                      void updateSettings({ dictationAutoReplay: checked })
                    }
                  />
                </div>

                <div className="space-y-1.5 pt-1 border-t border-gray-100 dark:border-slate-800">
                  <p className="font-semibold text-gray-700 dark:text-slate-200">
                    {isEn ? "Preferred voice accent" : "Giọng đọc ưu tiên"}
                  </p>
                  <div className="grid grid-cols-2 gap-1.5">
                    {(["US", "UK"] as const).map((accent) => {
                      const active = settings.preferredAccent === accent;
                      return (
                        <button
                          key={accent}
                          type="button"
                          onClick={() =>
                            void updateSettings({ preferredAccent: accent })
                          }
                          className={`rounded-lg py-1.5 text-[11px] font-bold border transition-all cursor-pointer ${
                            active
                              ? "border-emerald-500 bg-emerald-500 text-white"
                              : "border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-gray-600 dark:text-slate-300 hover:border-emerald-300"
                          }`}
                        >
                          {accent === "US"
                            ? isEn
                              ? "US (American)"
                              : "US (Giọng Mỹ)"
                            : isEn
                              ? "UK (British)"
                              : "UK (Giọng Anh)"}
                        </button>
                      );
                    })}
                  </div>
                </div>

                <div className="space-y-1.5 pt-1 border-t border-gray-100 dark:border-slate-800">
                  <p className="font-semibold text-gray-700 dark:text-slate-200">
                    {isEn ? "Replay shortcut key" : "Phím phát lại"}
                  </p>
                  <div className="grid grid-cols-3 gap-1.5">
                    {REPLAY_KEY_OPTIONS.map((keyOpt) => {
                      const active = settings.dictationReplayKey === keyOpt.value;
                      return (
                        <button
                          key={keyOpt.value}
                          type="button"
                          onClick={() =>
                            void updateSettings({
                              dictationReplayKey: keyOpt.value,
                            })
                          }
                          className={`rounded-lg py-1.5 text-[11px] font-bold border transition-all cursor-pointer ${
                            active
                              ? "border-emerald-500 bg-emerald-500 text-white"
                              : "border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-gray-600 dark:text-slate-300 hover:border-emerald-300"
                          }`}
                        >
                          {keyOpt.shortLabel}
                        </button>
                      );
                    })}
                  </div>
                </div>

                <div className="space-y-1.5 pt-1 border-t border-gray-100 dark:border-slate-800">
                  <p className="font-semibold text-gray-700 dark:text-slate-200">
                    {isEn ? "Playback speed" : "Tốc độ phát"}
                  </p>
                  <div className="flex flex-wrap gap-1">
                    {PLAYBACK_SPEED_OPTIONS.map((rate) => {
                      const active = settings.playbackSpeed === rate;
                      return (
                        <button
                          key={rate}
                          type="button"
                          onClick={() =>
                            void updateSettings({ playbackSpeed: rate })
                          }
                          className={`rounded-lg px-2 py-1 text-[11px] font-bold border transition-all cursor-pointer ${
                            active
                              ? "border-emerald-500 bg-emerald-500 text-white"
                              : "border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-gray-600 dark:text-slate-300 hover:border-emerald-300"
                          }`}
                        >
                          {rate}x
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
        <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-green-100 dark:bg-emerald-950 text-green-600 dark:text-emerald-300 flex items-center justify-center font-black text-xs border-2 border-white dark:border-slate-800 shadow-sm ml-1 uppercase cursor-pointer hover:bg-green-200 dark:hover:bg-emerald-900 transition-colors overflow-hidden">
          {showAvatarImage ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={avatarSrc}
              alt={user?.fullName || "User avatar"}
              className="w-full h-full object-cover"
              onError={() => setFailedAvatarUrl(avatarSrc)}
            />
          ) : (
            user?.fullName?.charAt(0) || "U"
          )}
        </div>
      </div>
    </header>
  );
}

