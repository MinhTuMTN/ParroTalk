"use client";

import React, { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  AlertCircle,
  BookOpenCheck,
  Lightbulb,
  Menu,
  PenTool,
  Sparkles,
} from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useUI } from "@/hooks/useUI";
import UserMenu from "@/components/common/UserMenu";
import VocabularyDetailPopup from "@/components/vocabulary/VocabularyDetailPopup";
import { writingService } from "@/features/writing/services/writingService";
import { WritingPromptComposer } from "@/features/writing/components/WritingPromptComposer";
import { SuggestionModeView } from "@/features/writing/components/SuggestionModeView";
import { GuidanceModeView } from "@/features/writing/components/GuidanceModeView";
import { WritingHistoryPanel } from "@/features/writing/components/WritingHistoryPanel";
import type {
  DifficultyScale,
  WritingMode,
  WritingSessionResponse,
  WritingSessionSummary,
} from "@/features/writing/types";

export default function WritingAssistantPage() {
  const { isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const { openMobileMenu } = useUI();
  const router = useRouter();

  const [inputText, setInputText] = useState("");
  const [mode, setMode] = useState<WritingMode>("SUGGESTION");
  const [scaleType, setScaleType] = useState<DifficultyScale>("CEFR");
  const [targetLevel, setTargetLevel] = useState("B2");

  const [activeSession, setActiveSession] =
    useState<WritingSessionResponse | null>(null);
  const [sessions, setSessions] = useState<WritingSessionSummary[]>([]);
  const [analyzing, setAnalyzing] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(true);
  const [savingDraft, setSavingDraft] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [dictionaryPopupWord, setDictionaryPopupWord] = useState<string | null>(
    null
  );

  useEffect(() => {
    if (!isAuthLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isAuthenticated, isAuthLoading, router]);

  const fetchHistory = useCallback(async () => {
    if (!isAuthenticated) return;
    setLoadingHistory(true);
    try {
      const pageData = await writingService.getSessions(0, 15);
      setSessions(pageData?.content ?? []);
    } catch {
      // Non-blocking history load
    } finally {
      setLoadingHistory(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);

  const upsertSessionSummary = useCallback((detail: WritingSessionResponse) => {
    const summary: WritingSessionSummary = {
      id: detail.id,
      inputText: detail.inputText,
      inputLanguage: detail.inputLanguage,
      mode: detail.mode,
      scaleType: detail.scaleType,
      targetLevel: detail.targetLevel,
      coreIdeaSummaryVn: detail.coreIdeaSummaryVn,
      hasUserDraft: Boolean(detail.userDraft && detail.userDraft.trim().length > 0),
      createdAt: detail.createdAt,
    };
    setSessions((prev) => {
      const filtered = prev.filter((s) => s.id !== summary.id);
      return [summary, ...filtered].slice(0, 15);
    });
  }, []);

  const runAnalysis = async (
    targetInput: string,
    targetMode: WritingMode,
    targetScale: DifficultyScale,
    level: string
  ) => {
    const trimmed = targetInput.trim();
    if (trimmed.length < 3) {
      setErrorMessage("Vui lòng nhập ý tưởng tối thiểu 3 ký tự.");
      return;
    }

    setAnalyzing(true);
    setErrorMessage(null);
    try {
      const result = await writingService.analyzeIdea({
        inputText: trimmed,
        mode: targetMode,
        scaleType: targetScale,
        targetLevel: level,
      });
      setActiveSession(result);
      upsertSessionSummary(result);
    } catch (err: unknown) {
      const apiMsg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message ||
        "Không thể phân tích ý tưởng lúc này. Vui lòng kiểm tra kết nối và thử lại.";
      setErrorMessage(apiMsg);
    } finally {
      setAnalyzing(false);
    }
  };

  const handleAnalyze = () => {
    void runAnalysis(inputText, mode, scaleType, targetLevel);
  };

  const handleModeChange = (nextMode: WritingMode) => {
    setMode(nextMode);
    // If switching an active session to GUIDANCE mode and it doesn't have completeSentences yet, fetch GUIDANCE analysis
    if (
      nextMode === "GUIDANCE" &&
      activeSession &&
      activeSession.completeSentences.length === 0 &&
      !analyzing
    ) {
      void runAnalysis(
        activeSession.inputText,
        "GUIDANCE",
        activeSession.scaleType,
        activeSession.targetLevel
      );
    }
  };

  const handleSelectSession = async (sessionId: string) => {
    setErrorMessage(null);
    try {
      const detail = await writingService.getSessionDetail(sessionId);
      setActiveSession(detail);
      setInputText(detail.inputText);
      setMode(detail.mode);
      setScaleType(detail.scaleType);
      setTargetLevel(detail.targetLevel);
    } catch {
      setErrorMessage("Không thể tải chi tiết phiên luyện viết đã chọn.");
    }
  };

  const handleDeleteSession = async (sessionId: string) => {
    try {
      await writingService.deleteSession(sessionId);
      setSessions((prev) => prev.filter((s) => s.id !== sessionId));
      if (activeSession?.id === sessionId) {
        setActiveSession(null);
      }
    } catch {
      setErrorMessage("Không thể xóa phiên luyện viết.");
    }
  };

  const handleSaveDraft = async (draft: string) => {
    if (!activeSession?.id) return;
    setSavingDraft(true);
    setErrorMessage(null);
    try {
      const updated = await writingService.updateDraft(activeSession.id, draft);
      setActiveSession(updated);
      upsertSessionSummary(updated);
    } catch {
      setErrorMessage("Không thể lưu câu nháp. Vui lòng thử lại.");
    } finally {
      setSavingDraft(false);
    }
  };

  const handleClear = () => {
    setInputText("");
    setActiveSession(null);
    setErrorMessage(null);
  };

  return (
    <div className="min-h-screen bg-ink-50 text-ink-900 flex flex-col">
      {/* ── Top Sticky Header ── */}
      <header className="sticky top-0 z-40 border-b border-brand-100/80 bg-white/90 backdrop-blur-md px-4 md:px-8 py-3 flex items-center justify-between shadow-2xs">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={openMobileMenu}
            aria-label="Mở menu điều hướng"
            className="lg:hidden p-2 hover:bg-brand-50 rounded-xl text-ink-600 transition-all cursor-pointer"
          >
            <Menu size={22} />
          </button>

          <div className="flex items-center gap-2.5">
            <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-brand-100 text-brand-700">
              <PenTool className="h-4 w-4" aria-hidden="true" />
            </span>
            <div>
              <h1 className="text-base font-extrabold text-ink-900 leading-tight">
                Writing Assistant
              </h1>
              <p className="text-[11px] text-ink-500 hidden sm:block">
                Diễn đạt ý tưởng sang tiếng Anh tự nhiên theo chuẩn CEFR &amp; IELTS
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <UserMenu />
        </div>
      </header>

      {/* ── Main Content Grid ── */}
      <main className="mx-auto w-full max-w-[1260px] flex-1 px-4 py-4 sm:px-6 sm:py-6">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-start">
          {/* Left / Main Column (8 cols on desktop) */}
          <div className="lg:col-span-8 space-y-5">
            <WritingPromptComposer
              inputText={inputText}
              onInputTextChange={setInputText}
              mode={mode}
              onModeChange={handleModeChange}
              scaleType={scaleType}
              onScaleTypeChange={setScaleType}
              targetLevel={targetLevel}
              onTargetLevelChange={setTargetLevel}
              loading={analyzing}
              onSubmit={handleAnalyze}
              onClear={handleClear}
            />

            {/* Error Alert */}
            {errorMessage && (
              <div
                role="alert"
                className="flex items-center gap-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-xs sm:text-sm font-semibold text-red-700"
              >
                <AlertCircle className="h-4 w-4 shrink-0 text-red-600" aria-hidden="true" />
                <span>{errorMessage}</span>
              </div>
            )}

            {/* Loading Skeleton */}
            {analyzing && (
              <div
                aria-busy="true"
                aria-label="Đang phân tích ý tưởng"
                className="rounded-2xl border border-ink-200/80 bg-white p-5 shadow-card space-y-3.5 animate-pulse"
              >
                <div className="h-5 w-1/3 rounded-lg bg-brand-100" />
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div className="h-24 rounded-xl bg-ink-100/80" />
                  <div className="h-24 rounded-xl bg-ink-100/80" />
                  <div className="h-24 rounded-xl bg-ink-100/80" />
                </div>
                <div className="h-20 rounded-xl bg-ink-100/60" />
              </div>
            )}

            {/* Active Session Result */}
            {!analyzing && activeSession && (
              <div className="space-y-4">
                {/* Compact Mode Switcher Bar for Current Result */}
                <div className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-ink-200/80 bg-white px-3.5 py-2 shadow-2xs">
                  <div className="flex items-center gap-1.5 text-xs font-bold text-ink-600">
                    <Sparkles className="h-3.5 w-3.5 text-brand-600" aria-hidden="true" />
                    <span>
                      Kết quả{" "}
                      <strong className="text-brand-700">
                        {activeSession.scaleType} {activeSession.targetLevel}
                      </strong>
                    </span>
                  </div>

                  <div className="inline-flex rounded-lg bg-ink-100/80 p-0.5">
                    <button
                      type="button"
                      onClick={() => handleModeChange("SUGGESTION")}
                      className={`inline-flex items-center gap-1 rounded-md px-2.5 py-1 text-xs font-bold transition-all cursor-pointer ${
                        mode === "SUGGESTION"
                          ? "bg-white text-brand-700 shadow-2xs"
                          : "text-ink-600 hover:text-ink-900"
                      }`}
                    >
                      <Lightbulb className="h-3 w-3" aria-hidden="true" />
                      <span>Gợi ý (Suggestion)</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => handleModeChange("GUIDANCE")}
                      className={`inline-flex items-center gap-1 rounded-md px-2.5 py-1 text-xs font-bold transition-all cursor-pointer ${
                        mode === "GUIDANCE"
                          ? "bg-white text-brand-700 shadow-2xs"
                          : "text-ink-600 hover:text-ink-900"
                      }`}
                    >
                      <BookOpenCheck className="h-3 w-3" aria-hidden="true" />
                      <span>Hướng dẫn (Guidance)</span>
                    </button>
                  </div>
                </div>

                {mode === "SUGGESTION" ? (
                  <SuggestionModeView
                    key={activeSession.id}
                    session={activeSession}
                    onOpenDictionary={(w) => setDictionaryPopupWord(w)}
                    onSaveDraft={handleSaveDraft}
                    savingDraft={savingDraft}
                    onSwitchToGuidance={() => handleModeChange("GUIDANCE")}
                  />
                ) : (
                  <GuidanceModeView
                    session={activeSession}
                    onOpenDictionary={(w) => setDictionaryPopupWord(w)}
                  />
                )}
              </div>
            )}

            {/* Empty State when no session is active */}
            {!analyzing && !activeSession && (
              <div className="rounded-2xl border border-dashed border-brand-200 bg-white/70 p-6 text-center space-y-2">
                <div className="mx-auto flex h-10 w-10 items-center justify-center rounded-xl bg-brand-100 text-brand-700">
                  <PenTool className="h-5 w-5" aria-hidden="true" />
                </div>
                <h2 className="text-sm sm:text-base font-extrabold text-ink-900">
                  Sẵn sàng hỗ trợ bạn viết tiếng Anh tự nhiên
                </h2>
                <p className="mx-auto max-w-lg text-xs text-ink-500 leading-relaxed">
                  Nhập ý tưởng bằng tiếng Việt hoặc tiếng Anh vào khung phía trên, chọn chế độ{" "}
                  <strong className="text-brand-700">Gợi ý</strong> (nhanh, tự luyện viết) hoặc{" "}
                  <strong className="text-brand-700">Hướng dẫn</strong> (chi tiết kèm câu mẫu) và nhấn{" "}
                  <strong className="text-brand-700">Phân tích</strong>.
                </p>
              </div>
            )}
          </div>

          {/* Right Column: Session History (4 cols on desktop) */}
          <div className="lg:col-span-4">
            <WritingHistoryPanel
              sessions={sessions}
              activeSessionId={activeSession?.id}
              loading={loadingHistory}
              onSelectSession={handleSelectSession}
              onDeleteSession={handleDeleteSession}
            />
          </div>
        </div>
      </main>

      {/* Reused Dictionary Popup for matched words */}
      {dictionaryPopupWord && (
        <VocabularyDetailPopup
          word={dictionaryPopupWord}
          onClose={() => setDictionaryPopupWord(null)}
        />
      )}
    </div>
  );
}
