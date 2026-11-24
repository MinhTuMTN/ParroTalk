"use client";

import React, { useCallback, useEffect, useId, useMemo, useRef, useState } from "react";
import { AlertCircle } from "lucide-react";
import { tokenizeWithWordIndex } from "@/lib/tokenizer";
import type {
  BlankAnswerItem,
  BlankResultItem,
  BlankSlot,
  DictationMode,
} from "@/features/lesson/services/lessonService";
import { validateSegmentBlanks } from "../utils/blankAnswerValidator";
import WordDictation from "./WordDictation";

export interface FillInBlanksDraftState {
  answers: Record<number, string>;
  results: Record<number, BlankResultItem>;
  revealedWordIndices: number[];
  summaryFeedback?: string | null;
}

export interface SegmentBlankCompletePayload {
  userAnswer: string;
  blankAnswers: BlankAnswerItem[];
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
  onSegmentComplete: (payload?: SegmentBlankCompletePayload) => void;
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
  onSegmentComplete,
  onHintUsed,
}: FillInBlanksDictationProps) {
  const errorRegionId = useId();
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
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const inputRefs = useRef<Map<number, HTMLInputElement>>(new Map());
  const hasCompletedRef = useRef(false);
  const onSegmentCompleteRef = useRef(onSegmentComplete);
  onSegmentCompleteRef.current = onSegmentComplete;

  const answers = draftState ? draftState.answers : localAnswers;
  const results = draftState ? draftState.results : localResults;
  const revealedSet = useMemo(
    () =>
      draftState
        ? new Set(draftState.revealedWordIndices)
        : localRevealed,
    [draftState, localRevealed]
  );

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
      }
    },
    [onDraftChange]
  );

  // Reset completion guard and focus first editable blank on mount or sentence/mode change
  useEffect(() => {
    hasCompletedRef.current = false;
    setErrorMessage(null);
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
    // Only reset/auto-focus when sentence/mode changes
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

  const validation = useMemo(
    () => validateSegmentBlanks(sortedBlanks, wordTextByIndex, answers),
    [answers, sortedBlanks, wordTextByIndex]
  );

  const completeSegment = useCallback(
    (currentAnswers: Record<number, string>) => {
      if (hasCompletedRef.current || isCompleted) return;
      hasCompletedRef.current = true;
      const blankAnswers: BlankAnswerItem[] = sortedBlanks.map((slot) => ({
        wordIndex: slot.wordIndex,
        answer: (currentAnswers[slot.wordIndex] ?? "").trim(),
      }));
      onSegmentCompleteRef.current({
        userAnswer: buildReconstructedSentence(currentAnswers),
        blankAnswers,
      });
    },
    [buildReconstructedSentence, isCompleted, sortedBlanks]
  );

  // Auto-complete after 40ms green glow when all blanks validate client-side
  useEffect(() => {
    if (
      !suitableForBlanks ||
      sortedBlanks.length === 0 ||
      !validation.allCorrect ||
      isCompleted ||
      hasCompletedRef.current
    ) {
      return;
    }

    const timer = window.setTimeout(() => {
      completeSegment(answers);
    }, 40);

    return () => window.clearTimeout(timer);
  }, [
    answers,
    completeSegment,
    isCompleted,
    sortedBlanks.length,
    suitableForBlanks,
    validation.allCorrect,
  ]);

  const handleCommitLastBlank = useCallback(() => {
    if (isCompleted || sortedBlanks.length === 0 || hasCompletedRef.current) return;

    const checkResult = validateSegmentBlanks(sortedBlanks, wordTextByIndex, answers);
    if (checkResult.allCorrect) {
      completeSegment(answers);
      return;
    }

    const nextResults: Record<number, BlankResultItem> = { ...results };
    let wrongCount = 0;

    for (const slot of sortedBlanks) {
      const slotCorrect = Boolean(checkResult.results[slot.wordIndex]);
      if (slotCorrect) {
        const expected = wordTextByIndex.get(slot.wordIndex) ?? "";
        nextResults[slot.wordIndex] = {
          wordIndex: slot.wordIndex,
          correct: true,
          expectedAnswer: expected,
        };
      } else {
        wrongCount += 1;
        nextResults[slot.wordIndex] = {
          wordIndex: slot.wordIndex,
          correct: false,
          expectedAnswer: null,
        };
      }
    }

    syncState(answers, nextResults, revealedSet, null);
    setErrorMessage(
      wrongCount === 1
        ? "1 blank is incorrect. Please check and try again."
        : `${wrongCount} blanks are incorrect. Please check and try again.`
    );

    const firstWrong = sortedBlanks.find(
      (slot) => !checkResult.results[slot.wordIndex]
    );
    if (firstWrong) {
      inputRefs.current.get(firstWrong.wordIndex)?.focus();
    }
  }, [
    answers,
    completeSegment,
    isCompleted,
    results,
    revealedSet,
    sortedBlanks,
    syncState,
    wordTextByIndex,
  ]);

  const revealBlankHint = useCallback(
    (wordIndex: number) => {
      if (isCompleted || isBlankLocked(wordIndex) || hasCompletedRef.current) return;

      const expectedWord = wordTextByIndex.get(wordIndex) ?? "";
      const nextAnswers = { ...answers, [wordIndex]: expectedWord };
      const nextResults: Record<number, BlankResultItem> = {
        ...results,
        [wordIndex]: {
          wordIndex,
          correct: true,
          expectedAnswer: expectedWord,
        },
      };
      const nextRevealed = new Set(revealedSet);
      nextRevealed.add(wordIndex);

      setErrorMessage(null);
      syncState(nextAnswers, nextResults, nextRevealed, null);
      onHintUsed?.();

      const idx = sortedBlanks.findIndex((b) => b.wordIndex === wordIndex);
      const nextEditable =
        sortedBlanks
          .slice(idx + 1)
          .find(
            (b) =>
              !nextResults[b.wordIndex]?.correct &&
              !nextRevealed.has(b.wordIndex)
          ) ??
        sortedBlanks.find(
          (b) =>
            b.wordIndex !== wordIndex &&
            !nextResults[b.wordIndex]?.correct &&
            !nextRevealed.has(b.wordIndex)
        );

      if (nextEditable) {
        inputRefs.current.get(nextEditable.wordIndex)?.focus();
      }
    },
    [
      answers,
      isBlankLocked,
      isCompleted,
      onHintUsed,
      results,
      revealedSet,
      sortedBlanks,
      syncState,
      wordTextByIndex,
    ]
  );

  const handleKeyDown = useCallback(
    (wordIndex: number, e: React.KeyboardEvent<HTMLInputElement>) => {
      const idx = sortedBlanks.findIndex((b) => b.wordIndex === wordIndex);
      if (idx === -1) return;

      if (e.altKey && (e.key === "h" || e.key === "H" || e.code === "KeyH")) {
        e.preventDefault();
        revealBlankHint(wordIndex);
        return;
      }

      if (e.key === "Enter") {
        e.preventDefault();
        if (e.nativeEvent.isComposing || e.keyCode === 229) {
          return;
        }
        const nextEditable = sortedBlanks
          .slice(idx + 1)
          .find((b) => !isBlankLocked(b.wordIndex));

        if (nextEditable) {
          inputRefs.current.get(nextEditable.wordIndex)?.focus();
        } else {
          handleCommitLastBlank();
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
    [
      answers,
      handleCommitLastBlank,
      isBlankLocked,
      revealBlankHint,
      sortedBlanks,
    ]
  );

  const handleFallbackComplete = useCallback(() => {
    onSegmentCompleteRef.current();
  }, []);

  const editableWordIndices = useMemo(
    () =>
      sortedBlanks
        .filter((b) => !isBlankLocked(b.wordIndex))
        .map((b) => b.wordIndex),
    [isBlankLocked, sortedBlanks]
  );
  const lastEditableWordIndex =
    editableWordIndices.length > 0
      ? editableWordIndices[editableWordIndices.length - 1]
      : null;

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
          onSentenceComplete={handleFallbackComplete}
          onHintUsed={onHintUsed}
          isCompleted={isCompleted}
        />
      </div>
    );
  }

  return (
    <div
      className="w-full max-w-3xl mx-auto flex flex-col gap-1.5 [padding-bottom:env(safe-area-inset-bottom)]"
      data-testid="fill-in-blanks-dictation"
    >
      {/* Inline sentence with uniform-width blank inputs */}
      <div className="rounded-xl border border-ink-200/80 dark:border-slate-700 bg-ink-50/60 dark:bg-slate-900/90 px-3 py-1.5 sm:px-4 sm:py-2 leading-8 sm:leading-9 text-sm sm:text-[15px] font-medium text-ink-900 dark:text-slate-100 flex flex-wrap items-baseline gap-y-1">
        {tokens.map((tok) => {
          if (tok.isWord && blankWordIndexSet.has(tok.wordIndex)) {
            const blankOrder =
              sortedBlanks.findIndex((b) => b.wordIndex === tok.wordIndex) + 1;
            const result = results[tok.wordIndex];
            const isAllMatched = validation.allCorrect && !isCompleted;
            const isCorrect = Boolean(result?.correct) || isAllMatched;
            const isIncorrect = !isAllMatched && Boolean(result && !result.correct);
            const locked = isBlankLocked(tok.wordIndex);
            const isLastEditable = lastEditableWordIndex === tok.wordIndex;
            const rawAnswer = answers[tok.wordIndex];
            const value =
              rawAnswer && rawAnswer.length > 0
                ? rawAnswer
                : isCompleted
                  ? (wordTextByIndex.get(tok.wordIndex) ?? "")
                  : "";

            return (
              <span
                key={`blank-${tok.tokenIndex}-${tok.wordIndex}`}
                className="relative inline-flex items-center mx-1 align-baseline"
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
                  inputMode="text"
                  enterKeyHint={isLastEditable ? "done" : "next"}
                  value={value}
                  readOnly={locked}
                  aria-label={`Blank ${blankOrder}`}
                  aria-invalid={isIncorrect ? "true" : "false"}
                  aria-describedby={
                    isIncorrect && errorMessage ? errorRegionId : undefined
                  }
                  data-word-index={tok.wordIndex}
                  autoComplete="off"
                  autoCorrect="off"
                  autoCapitalize="none"
                  spellCheck={false}
                  onChange={(e) => handleBlankChange(tok.wordIndex, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(tok.wordIndex, e)}
                  className={`w-[5.25rem] sm:w-[6rem] h-7 sm:h-8 ${
                    locked ? "px-2 text-center" : "pl-2.5 pr-6 text-left"
                  } py-0.5 text-sm font-bold rounded-lg border-2 outline-none transition-all ${
                    isAllMatched
                      ? "border-brand-500 bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-200 ring-4 ring-brand-100/80 dark:ring-brand-900/40 shadow-sm"
                      : isCorrect
                        ? "border-brand-500 bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-200 cursor-default"
                        : isIncorrect
                          ? "border-coral-500 bg-coral-50 dark:bg-rose-950/40 text-coral-700 dark:text-rose-200 focus:ring-4 focus:ring-coral-100 dark:focus:ring-rose-950/40"
                          : "border-ink-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-ink-900 dark:text-slate-100 focus:border-brand-500 focus:ring-4 focus:ring-brand-100/70 dark:focus:ring-brand-900/40"
                  }`}
                />
                {!locked && (
                  <button
                    type="button"
                    tabIndex={-1}
                    aria-label={`Reveal hint for blank ${blankOrder}`}
                    title="Reveal hint (Alt+H)"
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() => revealBlankHint(tok.wordIndex)}
                    className="absolute right-1 top-1/2 -translate-y-1/2 w-5 h-5 rounded-md inline-flex items-center justify-center text-[11px] font-extrabold text-ink-400 dark:text-slate-400 hover:text-honey-600 dark:hover:text-amber-300 hover:bg-honey-50 dark:hover:bg-amber-950/60 focus-visible:outline-2 focus-visible:outline-brand-500 transition-colors cursor-pointer"
                  >
                    ?
                  </button>
                )}
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

      {/* Single mounted polite live region for commit-time validation feedback */}
      <div
        id={errorRegionId}
        aria-live="polite"
        className={`${errorMessage ? "min-h-[1.25rem]" : "min-h-0"} flex items-center justify-center text-xs font-semibold`}
      >
        {errorMessage && (
          <span className="inline-flex items-center gap-1.5 text-coral-600 dark:text-rose-300">
            <AlertCircle size={14} aria-hidden="true" />
            <span>{errorMessage}</span>
          </span>
        )}
      </div>
    </div>
  );
}
