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
import {
  type BlankLiveState,
  getBlankLiveState,
  validateSegmentBlanks,
} from "../utils/blankAnswerValidator";
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
  const [composingSet, setComposingSet] = useState<Set<number>>(() => new Set());

  const inputRefs = useRef<Map<number, HTMLInputElement>>(new Map());
  const composingRef = useRef<Set<number>>(new Set());
  const preCompositionStateRef = useRef<Map<number, BlankLiveState>>(new Map());
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
    composingRef.current = new Set();
    preCompositionStateRef.current = new Map();
    setComposingSet(new Set());
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

  const findNextEditableBlank = useCallback(
    (fromSlotIndex: number) =>
      sortedBlanks
        .slice(fromSlotIndex + 1)
        .find((b) => !isBlankLocked(b.wordIndex)),
    [isBlankLocked, sortedBlanks]
  );

  const findPrevEditableBlank = useCallback(
    (fromSlotIndex: number) =>
      [...sortedBlanks.slice(0, fromSlotIndex)]
        .reverse()
        .find((b) => !isBlankLocked(b.wordIndex)),
    [isBlankLocked, sortedBlanks]
  );

  const handleCompositionStart = useCallback(
    (wordIndex: number) => {
      const slot = sortedBlanks.find((b) => b.wordIndex === wordIndex);
      const expected = wordTextByIndex.get(wordIndex) ?? "";
      const preState = getBlankLiveState(
        answers[wordIndex] ?? "",
        expected,
        slot?.acceptableAnswers
      );
      preCompositionStateRef.current.set(wordIndex, preState);
      composingRef.current.add(wordIndex);
      setComposingSet((prev) => {
        if (prev.has(wordIndex)) return prev;
        const next = new Set(prev);
        next.add(wordIndex);
        return next;
      });
    },
    [answers, sortedBlanks, wordTextByIndex]
  );

  const handleCompositionEnd = useCallback(
    (wordIndex: number, e: React.CompositionEvent<HTMLInputElement>) => {
      composingRef.current.delete(wordIndex);
      setComposingSet((prev) => {
        if (!prev.has(wordIndex)) return prev;
        const next = new Set(prev);
        next.delete(wordIndex);
        return next;
      });

      const preCompState = preCompositionStateRef.current.get(wordIndex);
      preCompositionStateRef.current.delete(wordIndex);

      if (isBlankLocked(wordIndex)) return;

      const finalValue = e.currentTarget.value;
      const currentAnswer = answers[wordIndex] ?? "";
      let nextAnswers = answers;
      if (finalValue !== currentAnswer) {
        nextAnswers = { ...answers, [wordIndex]: finalValue };
        const nextResults = { ...results };
        if (nextResults[wordIndex] && !nextResults[wordIndex].correct) {
          delete nextResults[wordIndex];
        }
        setErrorMessage(null);
        syncState(nextAnswers, nextResults, revealedSet, null);
      }

      const slotIndex = sortedBlanks.findIndex((b) => b.wordIndex === wordIndex);
      if (slotIndex === -1) return;

      const slot = sortedBlanks[slotIndex];
      const expected = wordTextByIndex.get(wordIndex) ?? "";
      const nextLiveState = getBlankLiveState(
        finalValue,
        expected,
        slot.acceptableAnswers
      );
      const nextValidation = validateSegmentBlanks(
        sortedBlanks,
        wordTextByIndex,
        nextAnswers
      );

      if (
        document.activeElement === e.currentTarget &&
        preCompState !== "correct" &&
        nextLiveState === "correct" &&
        (!suitableForBlanks || !nextValidation.allCorrect)
      ) {
        const nextEditable = findNextEditableBlank(slotIndex);
        if (nextEditable) {
          inputRefs.current.get(nextEditable.wordIndex)?.focus();
        }
      }
    },
    [
      answers,
      findNextEditableBlank,
      isBlankLocked,
      results,
      revealedSet,
      sortedBlanks,
      suitableForBlanks,
      syncState,
      wordTextByIndex,
    ]
  );

  const handleBlankChange = useCallback(
    (wordIndex: number, value: string, isNativeComposing = false) => {
      if (isBlankLocked(wordIndex)) return;

      const slotIndex = sortedBlanks.findIndex((b) => b.wordIndex === wordIndex);
      const slot = slotIndex !== -1 ? sortedBlanks[slotIndex] : undefined;
      const expected = wordTextByIndex.get(wordIndex) ?? "";
      const prevLiveState = getBlankLiveState(
        answers[wordIndex] ?? "",
        expected,
        slot?.acceptableAnswers
      );
      const nextLiveState = getBlankLiveState(
        value,
        expected,
        slot?.acceptableAnswers
      );

      const nextAnswers = { ...answers, [wordIndex]: value };
      const nextResults = { ...results };
      // Clear error state on edit so learner can retry cleanly
      if (nextResults[wordIndex] && !nextResults[wordIndex].correct) {
        delete nextResults[wordIndex];
      }
      setErrorMessage(null);
      syncState(nextAnswers, nextResults, revealedSet, null);

      const isComposing =
        composingRef.current.has(wordIndex) || isNativeComposing;
      if (
        !isComposing &&
        prevLiveState !== "correct" &&
        nextLiveState === "correct" &&
        slotIndex !== -1
      ) {
        const nextValidation = validateSegmentBlanks(
          sortedBlanks,
          wordTextByIndex,
          nextAnswers
        );
        if (!suitableForBlanks || !nextValidation.allCorrect) {
          const nextEditable = findNextEditableBlank(slotIndex);
          if (nextEditable) {
            inputRefs.current.get(nextEditable.wordIndex)?.focus();
          }
        }
      }
    },
    [
      answers,
      findNextEditableBlank,
      isBlankLocked,
      results,
      revealedSet,
      sortedBlanks,
      suitableForBlanks,
      syncState,
      wordTextByIndex,
    ]
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
      composingSet.size > 0 ||
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
    composingSet.size,
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
        const nextEditable = findNextEditableBlank(idx);

        if (nextEditable) {
          inputRefs.current.get(nextEditable.wordIndex)?.focus();
        } else {
          handleCommitLastBlank();
        }
        return;
      }

      if (e.key === "Tab" && !e.shiftKey) {
        const nextEditable = findNextEditableBlank(idx);
        if (nextEditable) {
          e.preventDefault();
          inputRefs.current.get(nextEditable.wordIndex)?.focus();
        }
        return;
      }

      if (e.key === "Tab" && e.shiftKey) {
        const prevEditable = findPrevEditableBlank(idx);
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
        const prevEditable = findPrevEditableBlank(idx);
        if (prevEditable) {
          e.preventDefault();
          inputRefs.current.get(prevEditable.wordIndex)?.focus();
        }
      }
    },
    [
      answers,
      findNextEditableBlank,
      findPrevEditableBlank,
      handleCommitLastBlank,
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
      {/* Inline sentence with length-masked blank inputs */}
      <div className="rounded-xl border border-ink-200/80 dark:border-slate-700 bg-ink-50/60 dark:bg-slate-900/90 px-3 py-1.5 sm:px-4 sm:py-2 leading-8 sm:leading-9 text-sm sm:text-[15px] font-medium text-ink-900 dark:text-slate-100 flex flex-wrap items-baseline gap-y-1">
        {tokens.map((tok) => {
          if (tok.isWord && blankWordIndexSet.has(tok.wordIndex)) {
            const slotIndex = sortedBlanks.findIndex(
              (b) => b.wordIndex === tok.wordIndex
            );
            const blankOrder = slotIndex + 1;
            const slot = slotIndex !== -1 ? sortedBlanks[slotIndex] : undefined;
            const expected = wordTextByIndex.get(tok.wordIndex) ?? "";
            const mask = expected.replace(/[a-zA-Z0-9]/g, "•");
            const result = results[tok.wordIndex];
            const isAllMatched = validation.allCorrect && !isCompleted;
            const isCommitCorrect = Boolean(result?.correct);
            const isCommitIncorrect =
              !isAllMatched && Boolean(result && !result.correct);
            const locked = isBlankLocked(tok.wordIndex);
            const isLastEditable = lastEditableWordIndex === tok.wordIndex;
            const rawAnswer = answers[tok.wordIndex];
            const value =
              rawAnswer && rawAnswer.length > 0
                ? rawAnswer
                : isCompleted
                  ? expected
                  : "";
            const liveState: BlankLiveState = locked
              ? "correct"
              : composingSet.has(tok.wordIndex)
                ? "typing"
                : getBlankLiveState(value, expected, slot?.acceptableAnswers);
            const isLiveWrong =
              !locked &&
              !isCommitIncorrect &&
              !isAllMatched &&
              liveState === "wrong";
            const isLiveCorrect =
              !locked &&
              !isCommitIncorrect &&
              !isAllMatched &&
              liveState === "correct";
            const isInvalid = isCommitIncorrect || isLiveWrong;

            const wrapperBgClass = isAllMatched || isCommitCorrect || isLiveCorrect
              ? "bg-brand-50 dark:bg-brand-900/40"
              : isCommitIncorrect
                ? "bg-coral-50 dark:bg-rose-950/40"
                : isLiveWrong
                  ? "bg-red-50/40 dark:bg-rose-950/30"
                  : "bg-white dark:bg-slate-800";

            const inputToneClass = isAllMatched
              ? "border-brand-500 text-brand-700 dark:text-brand-200 ring-4 ring-brand-100/80 dark:ring-brand-900/40 shadow-sm"
              : isCommitCorrect
                ? "border-brand-500 text-brand-700 dark:text-brand-200 cursor-default"
                : isCommitIncorrect
                  ? "border-coral-500 text-coral-700 dark:text-rose-200 focus:ring-4 focus:ring-coral-100 dark:focus:ring-rose-950/40"
                  : isLiveWrong
                    ? "border-red-400 dark:border-rose-500 text-red-600 dark:text-rose-300 focus:ring-4 focus:ring-red-50 dark:focus:ring-rose-950/40 shadow-lg shadow-red-100/50 dark:shadow-none"
                    : isLiveCorrect
                      ? "border-brand-500 text-brand-700 dark:text-brand-200 focus:ring-4 focus:ring-brand-100/70 dark:focus:ring-brand-900/40"
                      : "border-ink-200 dark:border-slate-700 text-ink-900 dark:text-slate-100 focus:border-brand-500 focus:ring-4 focus:ring-brand-100/70 dark:focus:ring-brand-900/40";

            return (
              <span
                key={`blank-${tok.tokenIndex}-${tok.wordIndex}`}
                className={`relative inline-block mx-1 align-baseline max-w-[calc(100%-0.5rem)] min-w-0 rounded-lg ${wrapperBgClass}`}
              >
                <span
                  aria-hidden="true"
                  className={`pointer-events-none absolute inset-0 flex items-center overflow-hidden whitespace-pre font-mono text-base font-bold border-2 border-transparent rounded-lg py-0.5 text-ink-300 dark:text-slate-600 ${
                    locked ? "px-2 justify-center" : "pl-2.5 pr-7 justify-start text-left"
                  }`}
                >
                  <span className="invisible">{value}</span>
                  {locked || isAllMatched || isLiveCorrect ? "" : mask.slice(value.length)}
                </span>
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
                  aria-invalid={isInvalid ? "true" : "false"}
                  aria-describedby={
                    isCommitIncorrect && errorMessage ? errorRegionId : undefined
                  }
                  data-word-index={tok.wordIndex}
                  autoComplete="off"
                  autoCorrect="off"
                  autoCapitalize="none"
                  spellCheck={false}
                  style={{
                    width: `calc(${Math.max(expected.length, 2)}ch + 2.75rem)`,
                  }}
                  onCompositionStart={() => handleCompositionStart(tok.wordIndex)}
                  onCompositionEnd={(e) => handleCompositionEnd(tok.wordIndex, e)}
                  onChange={(e) =>
                    handleBlankChange(
                      tok.wordIndex,
                      e.target.value,
                      Boolean(
                        (e.nativeEvent as { isComposing?: boolean } | undefined)
                          ?.isComposing
                      )
                    )
                  }
                  onKeyDown={(e) => handleKeyDown(tok.wordIndex, e)}
                  className={`relative max-w-full h-9 ${
                    locked ? "px-2 text-center" : "pl-2.5 pr-7 text-left"
                  } py-0.5 font-mono text-base font-bold bg-transparent rounded-lg border-2 outline-none transition-all ${inputToneClass}`}
                />
                {!locked && (
                  <button
                    type="button"
                    tabIndex={-1}
                    aria-label={`Reveal hint for blank ${blankOrder}`}
                    title="Reveal hint (Alt+H)"
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() => revealBlankHint(tok.wordIndex)}
                    className="absolute right-1 top-1/2 -translate-y-1/2 z-10 w-5 h-5 rounded-md inline-flex items-center justify-center text-[11px] font-extrabold text-ink-400 dark:text-slate-400 hover:text-honey-600 dark:hover:text-amber-300 hover:bg-honey-50 dark:hover:bg-amber-950/60 focus-visible:outline-2 focus-visible:outline-brand-500 transition-colors cursor-pointer"
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
