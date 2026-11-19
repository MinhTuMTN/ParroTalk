"use client";

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { CheckCircle2, HelpCircle, AlertCircle, Send } from "lucide-react";
import { tokenizeWithWordIndex } from "@/lib/tokenizer";
import type {
  BlankAnswerItem,
  BlankResultItem,
  BlankSlot,
  DictationMode,
  DraftSegmentResponse,
} from "@/features/lesson/services/lessonService";
import WordDictation from "./WordDictation";

export interface FillInBlanksDraftState {
  answers: Record<number, string>;
  results: Record<number, BlankResultItem>;
  revealedWordIndices: number[];
  summaryFeedback?: string | null;
}

interface FillInBlanksDictationProps {
  sentence: string;
  mode: Exclude<DictationMode, "FULL">;
  blanks?: BlankSlot[];
  suitableForBlanks?: boolean;
  isCompleted?: boolean;
  draftState?: FillInBlanksDraftState;
  fallbackInput?: string;
  onFallbackInputChange?: (val: string) => void;
  onDraftChange?: (next: FillInBlanksDraftState) => void;
  onSubmitBlanks: (payload: {
    blankAnswers: BlankAnswerItem[];
    userAnswer: string;
  }) => Promise<DraftSegmentResponse>;
  onSegmentComplete: (response?: DraftSegmentResponse) => void;
  onHintUsed?: () => void;
}

const EMPTY_BLANKS: BlankSlot[] = [];

export default function FillInBlanksDictation({
  sentence,
  mode,
  blanks = EMPTY_BLANKS,
  suitableForBlanks = true,
  isCompleted = false,
  draftState,
  fallbackInput = "",
  onFallbackInputChange,
  onDraftChange,
  onSubmitBlanks,
  onSegmentComplete,
  onHintUsed,
}: FillInBlanksDictationProps) {
  const tokens = useMemo(() => tokenizeWithWordIndex(sentence), [sentence]);

  const sortedBlanks = useMemo(() => {
    const seen = new Set<number>();
    const list: BlankSlot[] = [];
    for (const slot of blanks) {
      if (!seen.has(slot.wordIndex)) {
        seen.add(slot.wordIndex);
        list.push(slot);
      }
    }
    return list.sort((a, b) => a.wordIndex - b.wordIndex);
  }, [blanks]);

  const blankWordIndexSet = useMemo(
    () => new Set(sortedBlanks.map((b) => b.wordIndex)),
    [sortedBlanks]
  );

  const wordTextByIndex = useMemo(() => {
    const map = new Map<number, string>();
    for (const tok of tokens) {
      if (tok.isWord) {
        map.set(tok.wordIndex, tok.text);
      }
    }
    return map;
  }, [tokens]);

  const [localAnswers, setLocalAnswers] = useState<Record<number, string>>(
    () => draftState?.answers ?? {}
  );
  const [localResults, setLocalResults] = useState<Record<number, BlankResultItem>>(
    () => draftState?.results ?? {}
  );
  const [localRevealed, setLocalRevealed] = useState<Set<number>>(
    () => new Set(draftState?.revealedWordIndices ?? [])
  );
  const [summaryFeedback, setSummaryFeedback] = useState<string | null>(
    () => draftState?.summaryFeedback ?? null
  );
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isChecking, setIsChecking] = useState(false);
  const [focusedWordIndex, setFocusedWordIndex] = useState<number | null>(null);

  const inputRefs = useRef<Map<number, HTMLInputElement>>(new Map());

  const answers = draftState ? draftState.answers : localAnswers;
  const results = draftState ? draftState.results : localResults;
  const revealedSet = useMemo(
    () =>
      draftState
        ? new Set(draftState.revealedWordIndices)
        : localRevealed,
    [draftState, localRevealed]
  );
  const currentSummary = draftState
    ? draftState.summaryFeedback ?? null
    : summaryFeedback;

  const syncState = useCallback(
    (
      nextAnswers: Record<number, string>,
      nextResults: Record<number, BlankResultItem>,
      nextRevealed: Set<number>,
      nextSummary: string | null
    ) => {
      if (onDraftChange) {
        onDraftChange({
          answers: nextAnswers,
          results: nextResults,
          revealedWordIndices: Array.from(nextRevealed),
          summaryFeedback: nextSummary,
        });
      } else {
        setLocalAnswers(nextAnswers);
        setLocalResults(nextResults);
        setLocalRevealed(nextRevealed);
        setSummaryFeedback(nextSummary);
      }
    },
    [onDraftChange]
  );

  // Focus first editable blank on mount or sentence/mode change
  useEffect(() => {
    if (sortedBlanks.length === 0 || isCompleted) return;
    const firstEditable = sortedBlanks.find(
      (b) => !results[b.wordIndex]?.correct && !revealedSet.has(b.wordIndex)
    );
    if (firstEditable) {
      const el = inputRefs.current.get(firstEditable.wordIndex);
      if (el) {
        el.focus();
      }
    }
    // Only auto-focus when sentence/mode changes
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sentence, mode]);

  const isBlankLocked = useCallback(
    (wordIndex: number) =>
      Boolean(isCompleted || results[wordIndex]?.correct || revealedSet.has(wordIndex)),
    [isCompleted, results, revealedSet]
  );

  const handleBlankChange = useCallback(
    (wordIndex: number, value: string) => {
      if (isBlankLocked(wordIndex)) return;
      const nextAnswers = { ...answers, [wordIndex]: value };
      const nextResults = { ...results };
      // Clear error state on edit so learner can retry cleanly
      if (nextResults[wordIndex] && !nextResults[wordIndex].correct) {
        delete nextResults[wordIndex];
      }
      setErrorMessage(null);
      syncState(nextAnswers, nextResults, revealedSet, null);
    },
    [answers, isBlankLocked, results, revealedSet, syncState]
  );

  const buildReconstructedSentence = useCallback(
    (currentAnswers: Record<number, string>) => {
      return tokens
        .map((tok) => {
          if (tok.isWord && blankWordIndexSet.has(tok.wordIndex)) {
            return (currentAnswers[tok.wordIndex] ?? "").trim();
          }
          return tok.text;
        })
        .join("")
        .replace(/\s+/g, " ")
        .trim();
    },
    [blankWordIndexSet, tokens]
  );

  const handleCheck = useCallback(async () => {
    if (isChecking || isCompleted || sortedBlanks.length === 0) return;

    const blankAnswers: BlankAnswerItem[] = sortedBlanks.map((slot) => ({
      wordIndex: slot.wordIndex,
      answer: (answers[slot.wordIndex] ?? "").trim(),
    }));

    setIsChecking(true);
    setErrorMessage(null);

    try {
      const response = await onSubmitBlanks({
        blankAnswers,
        userAnswer: buildReconstructedSentence(answers),
      });

      const nextResults: Record<number, BlankResultItem> = { ...results };
      const nextAnswers = { ...answers };

      if (Array.isArray(response.blankResults)) {
        for (const item of response.blankResults) {
          nextResults[item.wordIndex] = item;
          if (item.correct && item.expectedAnswer) {
            nextAnswers[item.wordIndex] = item.expectedAnswer;
          }
        }
      } else if (response.correct) {
        for (const slot of sortedBlanks) {
          const expected = wordTextByIndex.get(slot.wordIndex) ?? "";
          nextResults[slot.wordIndex] = {
            wordIndex: slot.wordIndex,
            correct: true,
            expectedAnswer: expected,
          };
        }
      }

      const correctCount = sortedBlanks.filter(
        (slot) => nextResults[slot.wordIndex]?.correct
      ).length;
      const totalCount = sortedBlanks.length;
      const nextSummary = response.correct
        ? `${totalCount} / ${totalCount} blanks correct`
        : `${correctCount} / ${totalCount} blanks correct`;

      syncState(nextAnswers, nextResults, revealedSet, nextSummary);

      if (response.correct) {
        onSegmentComplete(response);
      } else {
        const firstWrong = sortedBlanks.find(
          (slot) => !nextResults[slot.wordIndex]?.correct
        );
        if (firstWrong) {
          inputRefs.current.get(firstWrong.wordIndex)?.focus();
        }
      }
    } catch {
      setErrorMessage("Could not verify your answers. Please try again.");
    } finally {
      setIsChecking(false);
    }
  }, [
    answers,
    buildReconstructedSentence,
    isChecking,
    isCompleted,
    onSegmentComplete,
    onSubmitBlanks,
    results,
    revealedSet,
    sortedBlanks,
    syncState,
    wordTextByIndex,
  ]);

  const handleHint = useCallback(() => {
    if (isCompleted || sortedBlanks.length === 0) return;

    // Prefer the currently focused blank if editable/unfilled; otherwise first unfilled/incorrect blank
    let targetSlot: BlankSlot | undefined;
    if (
      focusedWordIndex !== null &&
      blankWordIndexSet.has(focusedWordIndex) &&
      !isBlankLocked(focusedWordIndex)
    ) {
      targetSlot = sortedBlanks.find((b) => b.wordIndex === focusedWordIndex);
    }
    if (!targetSlot) {
      targetSlot =
        sortedBlanks.find(
          (b) =>
            !isBlankLocked(b.wordIndex) && !(answers[b.wordIndex] ?? "").trim()
        ) ?? sortedBlanks.find((b) => !isBlankLocked(b.wordIndex));
    }

    if (!targetSlot) return;

    const expectedWord = wordTextByIndex.get(targetSlot.wordIndex) ?? "";
    const nextAnswers = { ...answers, [targetSlot.wordIndex]: expectedWord };
    const nextResults: Record<number, BlankResultItem> = {
      ...results,
      [targetSlot.wordIndex]: {
        wordIndex: targetSlot.wordIndex,
        correct: true,
        expectedAnswer: expectedWord,
      },
    };
    const nextRevealed = new Set(revealedSet);
    nextRevealed.add(targetSlot.wordIndex);

    const correctCount = sortedBlanks.filter(
      (slot) => nextResults[slot.wordIndex]?.correct
    ).length;
    const nextSummary = `${correctCount} / ${sortedBlanks.length} blanks correct`;

    syncState(nextAnswers, nextResults, nextRevealed, nextSummary);
    onHintUsed?.();

    const nextEditable = sortedBlanks.find(
      (b) =>
        b.wordIndex !== targetSlot!.wordIndex &&
        !nextResults[b.wordIndex]?.correct &&
        !nextRevealed.has(b.wordIndex)
    );
    if (nextEditable) {
      inputRefs.current.get(nextEditable.wordIndex)?.focus();
    }
  }, [
    answers,
    blankWordIndexSet,
    focusedWordIndex,
    isBlankLocked,
    isCompleted,
    onHintUsed,
    results,
    revealedSet,
    sortedBlanks,
    syncState,
    wordTextByIndex,
  ]);

  const handleKeyDown = useCallback(
    (wordIndex: number, e: React.KeyboardEvent<HTMLInputElement>) => {
      const idx = sortedBlanks.findIndex((b) => b.wordIndex === wordIndex);
      if (idx === -1) return;

      if (e.key === "Enter") {
        e.preventDefault();
        const nextUnfilled = sortedBlanks
          .slice(idx + 1)
          .find(
            (b) =>
              !isBlankLocked(b.wordIndex) && !(answers[b.wordIndex] ?? "").trim()
          );
        const nextEditable =
          nextUnfilled ??
          sortedBlanks.slice(idx + 1).find((b) => !isBlankLocked(b.wordIndex));

        if (nextEditable) {
          inputRefs.current.get(nextEditable.wordIndex)?.focus();
        } else {
          void handleCheck();
        }
        return;
      }

      if (e.key === "Tab" && !e.shiftKey) {
        const nextEditable = sortedBlanks
          .slice(idx + 1)
          .find((b) => !isBlankLocked(b.wordIndex));
        if (nextEditable) {
          e.preventDefault();
          inputRefs.current.get(nextEditable.wordIndex)?.focus();
        }
        return;
      }

      if (e.key === "Tab" && e.shiftKey) {
        const prevEditable = [...sortedBlanks.slice(0, idx)]
          .reverse()
          .find((b) => !isBlankLocked(b.wordIndex));
        if (prevEditable) {
          e.preventDefault();
          inputRefs.current.get(prevEditable.wordIndex)?.focus();
        }
        return;
      }

      if (
        e.key === "Backspace" &&
        (answers[wordIndex] ?? "").length === 0
      ) {
        const prevEditable = [...sortedBlanks.slice(0, idx)]
          .reverse()
          .find((b) => !isBlankLocked(b.wordIndex));
        if (prevEditable) {
          e.preventDefault();
          inputRefs.current.get(prevEditable.wordIndex)?.focus();
        }
      }
    },
    [answers, handleCheck, isBlankLocked, sortedBlanks]
  );

  // Fallback when segment is unsuitable or has no blanks for this mode
  if (!suitableForBlanks || sortedBlanks.length === 0) {
    return (
      <div
        className="w-full max-w-3xl mx-auto flex flex-col gap-3"
        data-testid="fill-blanks-fallback"
      >
        <div className="rounded-xl border border-honey-100 dark:border-amber-800/70 bg-honey-50/70 dark:bg-amber-950/30 px-3.5 py-2 text-xs font-semibold text-honey-600 dark:text-amber-200 flex items-center justify-between gap-2">
          <span>
            Short or non-blank segment — type the full phrase below to continue.
          </span>
        </div>
        <WordDictation
          sentence={sentence}
          fullInput={fallbackInput}
          onInputChange={onFallbackInputChange ?? (() => {})}
          onSentenceComplete={() => onSegmentComplete()}
          onHintUsed={onHintUsed}
          isCompleted={isCompleted}
        />
      </div>
    );
  }

  const allEditableLocked = sortedBlanks.every((b) => isBlankLocked(b.wordIndex));
  const correctBlankCount = sortedBlanks.filter(
    (b) => results[b.wordIndex]?.correct
  ).length;

  return (
    <div
      className="w-full max-w-4xl mx-auto flex flex-col gap-3 [padding-bottom:env(safe-area-inset-bottom)]"
      data-testid="fill-in-blanks-dictation"
    >
      {/* Inline sentence with uniform-width blank inputs */}
      <div className="rounded-2xl border border-ink-200/80 dark:border-slate-700 bg-ink-50/60 dark:bg-slate-900/90 px-4 py-3.5 sm:px-5 sm:py-4 leading-[2.75rem] text-base sm:text-lg font-medium text-ink-900 dark:text-slate-100 flex flex-wrap items-baseline gap-y-2">
        {tokens.map((tok) => {
          if (tok.isWord && blankWordIndexSet.has(tok.wordIndex)) {
            const blankOrder =
              sortedBlanks.findIndex((b) => b.wordIndex === tok.wordIndex) + 1;
            const result = results[tok.wordIndex];
            const isCorrect = Boolean(result?.correct);
            const isIncorrect = Boolean(result && !result.correct);
            const locked = isBlankLocked(tok.wordIndex);
            const value = answers[tok.wordIndex] ?? "";

            return (
              <span
                key={`blank-${tok.tokenIndex}-${tok.wordIndex}`}
                className="inline-flex items-center mx-1 align-baseline"
              >
                <input
                  ref={(el) => {
                    if (el) {
                      inputRefs.current.set(tok.wordIndex, el);
                    } else {
                      inputRefs.current.delete(tok.wordIndex);
                    }
                  }}
                  type="text"
                  value={value}
                  readOnly={locked}
                  aria-label={`Blank ${blankOrder}`}
                  aria-invalid={isIncorrect}
                  data-word-index={tok.wordIndex}
                  autoComplete="off"
                  autoCorrect="off"
                  autoCapitalize="none"
                  spellCheck={false}
                  onFocus={() => setFocusedWordIndex(tok.wordIndex)}
                  onChange={(e) => handleBlankChange(tok.wordIndex, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(tok.wordIndex, e)}
                  className={`w-[6.75rem] sm:w-[7.5rem] min-h-[44px] sm:min-h-[38px] px-2.5 py-1 text-center text-sm sm:text-base font-bold rounded-xl border-2 outline-none transition-all ${
                    isCorrect
                      ? "border-brand-500 bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-200 cursor-default"
                      : isIncorrect
                        ? "border-coral-500 bg-coral-50 dark:bg-rose-950/40 text-coral-700 dark:text-rose-200 focus:ring-4 focus:ring-coral-100 dark:focus:ring-rose-950/40"
                        : "border-ink-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-ink-900 dark:text-slate-100 focus:border-brand-500 focus:ring-4 focus:ring-brand-100/70 dark:focus:ring-brand-900/40"
                  }`}
                />
              </span>
            );
          }

          return (
            <span
              key={`tok-${tok.tokenIndex}`}
              className="whitespace-pre-wrap"
            >
              {tok.text}
            </span>
          );
        })}
      </div>

      {/* Controls & Feedback row */}
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2 text-xs font-semibold">
          {currentSummary ? (
            <span
              role="status"
              className={`inline-flex items-center gap-1.5 rounded-xl px-3 py-1.5 border ${
                correctBlankCount === sortedBlanks.length
                  ? "bg-brand-50 dark:bg-brand-900/40 border-brand-200 dark:border-brand-700 text-brand-700 dark:text-brand-200"
                  : "bg-coral-50 dark:bg-rose-950/40 border-coral-200 dark:border-rose-800 text-coral-700 dark:text-rose-200"
              }`}
            >
              {correctBlankCount === sortedBlanks.length ? (
                <CheckCircle2 size={14} aria-hidden="true" />
              ) : (
                <AlertCircle size={14} aria-hidden="true" />
              )}
              <span>{currentSummary}</span>
            </span>
          ) : (
            <span className="text-ink-500 dark:text-slate-400">
              {sortedBlanks.length}{" "}
              {sortedBlanks.length === 1 ? "blank" : "blanks"} in this sentence
            </span>
          )}

          {errorMessage && (
            <span
              role="alert"
              className="text-coral-600 dark:text-rose-300 font-bold"
            >
              {errorMessage}
            </span>
          )}
        </div>

        <div className="flex items-center gap-2 ml-auto">
          <button
            type="button"
            onClick={handleHint}
            disabled={isCompleted || allEditableLocked}
            className="inline-flex items-center justify-center gap-1.5 rounded-xl border border-honey-100 dark:border-amber-800 bg-honey-50 dark:bg-amber-950/50 text-honey-600 dark:text-amber-200 hover:bg-honey-100 dark:hover:bg-amber-900/50 px-3.5 py-2 min-h-[44px] sm:min-h-[38px] text-xs font-extrabold uppercase tracking-wider transition-all disabled:opacity-40 cursor-pointer active:scale-95"
          >
            <HelpCircle size={15} aria-hidden="true" />
            <span>Hint</span>
          </button>

          <button
            type="button"
            onClick={() => void handleCheck()}
            disabled={isChecking || isCompleted}
            className="inline-flex items-center justify-center gap-1.5 rounded-xl bg-brand-500 hover:bg-brand-600 text-white px-4 py-2 min-h-[44px] sm:min-h-[38px] text-xs font-extrabold uppercase tracking-wider shadow-sm transition-all disabled:opacity-50 cursor-pointer active:scale-95"
          >
            <Send size={14} aria-hidden="true" />
            <span>{isChecking ? "Checking..." : "Check"}</span>
          </button>
        </div>
      </div>
    </div>
  );
}
