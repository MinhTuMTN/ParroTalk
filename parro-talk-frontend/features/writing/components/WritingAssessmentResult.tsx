"use client";

import React from "react";
import {
  AlertTriangle,
  ArrowRight,
  Check,
  CheckCircle2,
  Copy,
  Info,
  PenLine,
  PlusCircle,
  Sparkles,
} from "lucide-react";
import type {
  GrammarIssue,
  GrammarValidationResult,
  WritingAssessmentCriterion,
  WritingAssessmentCriterionKey,
} from "../types";

const CRITERION_LABELS: Record<
  WritingAssessmentCriterionKey,
  { nameEn: string; labelVn: string }
> = {
  TASK_FULFILLMENT: {
    nameEn: "Task Fulfillment & Idea Development",
    labelVn: "Hoàn thành yêu cầu và phát triển ý",
  },
  COHERENCE_COHESION: {
    nameEn: "Coherence & Cohesion",
    labelVn: "Tính mạch lạc và liên kết",
  },
  LEXICAL_RESOURCE: {
    nameEn: "Lexical Resource",
    labelVn: "Vốn từ vựng",
  },
  GRAMMATICAL_RANGE_ACCURACY: {
    nameEn: "Grammatical Range & Accuracy",
    labelVn: "Độ đa dạng và chính xác ngữ pháp",
  },
};

function getScoreTone(score: number): {
  badge: string;
  bar: string;
} {
  if (score >= 80) {
    return {
      badge:
        "border-emerald-300 dark:border-emerald-700/60 bg-emerald-100/90 dark:bg-emerald-950/70 text-emerald-800 dark:text-emerald-200",
      bar: "bg-emerald-500 dark:bg-emerald-400",
    };
  }
  if (score >= 60) {
    return {
      badge:
        "border-amber-300 dark:border-amber-700/60 bg-amber-100/90 dark:bg-amber-950/70 text-amber-800 dark:text-amber-200",
      bar: "bg-amber-500 dark:bg-amber-400",
    };
  }
  return {
    badge:
      "border-rose-300 dark:border-rose-700/60 bg-rose-100/90 dark:bg-rose-950/70 text-rose-800 dark:text-rose-200",
    bar: "bg-rose-500 dark:bg-rose-400",
  };
}

function getIssueTypeMeta(
  issue: GrammarIssue,
  isEn: boolean
): {
  label: string;
  className: string;
} {
  const normalized = (issue.type || issue.ruleId || "").toUpperCase();
  switch (normalized) {
    case "GRAMMAR":
      return {
        label: isEn ? "Grammar" : "Ngữ pháp",
        className:
          "bg-violet-100/80 dark:bg-violet-950/60 text-violet-800 dark:text-violet-200 border-violet-200 dark:border-violet-700/60",
      };
    case "SPELLING":
      return {
        label: isEn ? "Spelling" : "Chính tả",
        className:
          "bg-rose-100/80 dark:bg-rose-950/60 text-rose-800 dark:text-rose-200 border-rose-200 dark:border-rose-700/60",
      };
    case "WORD_CHOICE":
    case "REPEATED_WORD":
      return {
        label: isEn ? "Word choice" : "Dùng từ",
        className:
          "bg-sky-100/80 dark:bg-sky-950/60 text-sky-800 dark:text-sky-200 border-sky-200 dark:border-sky-700/60",
      };
    case "PUNCTUATION":
    case "TERMINAL_PUNCTUATION":
    case "UPPERCASE_START":
    case "UNBALANCED_BRACKETS":
    case "MULTIPLE_SPACES":
      return {
        label: isEn ? "Punctuation" : "Dấu câu",
        className:
          "bg-amber-100/80 dark:bg-amber-950/60 text-amber-800 dark:text-amber-200 border-amber-200 dark:border-amber-700/60",
      };
    case "TOO_SHORT":
      return {
        label: isEn ? "Length" : "Độ dài",
        className:
          "bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 border-slate-200 dark:border-slate-700",
      };
    default:
      return {
        label: normalized
          ? normalized.replace(/_/g, " ")
          : isEn
            ? "Issue"
            : "Lưu ý",
        className:
          "bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 border-slate-200 dark:border-slate-700",
      };
  }
}

export interface WritingAssessmentResultProps {
  validation: GrammarValidationResult;
  userDraft?: string;
  isEn: boolean;
  copiedCorrected: boolean;
  onCopyCorrected: (text: string) => void;
  onApplyCorrected: (text: string) => void;
  onInsertTerm: (term: string) => void;
}

export function WritingAssessmentResult({
  validation,
  userDraft,
  isEn,
  copiedCorrected,
  onCopyCorrected,
  onApplyCorrected,
  onInsertTerm,
}: WritingAssessmentResultProps) {
  const issues = Array.isArray(validation.issues) ? validation.issues : [];
  const usedTerms = Array.isArray(validation.usedTerms)
    ? validation.usedTerms.filter((t) => Boolean(t?.trim()))
    : [];
  const missingTerms = Array.isArray(validation.missingTerms)
    ? validation.missingTerms.filter((t) => Boolean(t?.trim()))
    : [];
  const correctedSentence = validation.correctedSentence?.trim() || "";
  const criteria: WritingAssessmentCriterion[] = Array.isArray(
    validation.assessmentCriteria
  )
    ? validation.assessmentCriteria.filter(
        (c) =>
          c &&
          typeof c.score === "number" &&
          Number.isFinite(c.score)
      )
    : [];

  const score =
    typeof validation.score === "number" && Number.isFinite(validation.score)
      ? Math.max(0, Math.min(100, Math.round(validation.score)))
      : null;

  const hasAiSignals =
    score !== null || Boolean(correctedSentence) || criteria.length > 0;
  const isRuleOnlyFallback =
    validation.aiReviewed === false ||
    validation.reviewSource?.toUpperCase() === "RULE_ONLY" ||
    (!hasAiSignals && validation.aiReviewed !== true);
  const isLegacyAiResult =
    !isRuleOnlyFallback && criteria.length === 0 && hasAiSignals;

  const overallTone = score !== null ? getScoreTone(score) : null;

  return (
    <div
      className={`rounded-xl border p-3.5 sm:p-4 space-y-3.5 ${
        validation.valid
          ? "border-brand-200 dark:border-brand-500/40 bg-brand-50/70 dark:bg-emerald-950/40 text-brand-900 dark:text-emerald-200"
          : "border-honey-200 dark:border-amber-700/60 bg-honey-50/70 dark:bg-amber-950/35 text-ink-900 dark:text-amber-200"
      }`}
    >
      {/* Overall Score & Summary Header */}
      <div className="space-y-2">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <div className="flex items-start gap-2 text-xs sm:text-sm font-bold flex-1 min-w-0">
            {validation.valid ? (
              <CheckCircle2
                className="h-4 w-4 text-brand-600 dark:text-emerald-400 shrink-0 mt-0.5"
                aria-hidden="true"
              />
            ) : (
              <AlertTriangle
                className="h-4 w-4 text-honey-600 dark:text-amber-300 shrink-0 mt-0.5"
                aria-hidden="true"
              />
            )}
            <span className="leading-snug">{validation.summaryVn}</span>
          </div>

          {score !== null && overallTone && (
            <span
              data-testid="overall-score-badge"
              className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-extrabold tabular-nums shrink-0 ${overallTone.badge}`}
            >
              <span>{isEn ? "Overall Score" : "Tổng điểm"}:</span>
              <span>{`${score}/100`}</span>
            </span>
          )}
        </div>

        {score !== null && overallTone && (
          <div className="h-1.5 w-full rounded-full bg-ink-200/70 dark:bg-slate-800 overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-300 ${overallTone.bar}`}
              style={{ width: `${score}%` }}
            />
          </div>
        )}
      </div>

      {/* Rule-only fallback notice */}
      {isRuleOnlyFallback && (
        <div className="flex items-center gap-2 rounded-lg border border-sky-200/80 dark:border-sky-800/60 bg-sky-50/80 dark:bg-sky-950/40 px-2.5 py-1.5 text-[11px] font-medium text-sky-800 dark:text-sky-200">
          <Info
            className="h-3.5 w-3.5 shrink-0 text-sky-600 dark:text-sky-300"
            aria-hidden="true"
          />
          <span>
            {isEn
              ? "Basic rule-based check only (AI assessment was unavailable for this result)."
              : "Đang kiểm tra theo quy tắc cơ bản (đánh giá AI tạm thời chưa khả dụng)."}
          </span>
        </div>
      )}

      {/* Legacy AI result fallback notice (historical records without 4-criteria breakdown) */}
      {isLegacyAiResult && (
        <div className="flex items-center gap-2 rounded-lg border border-ink-200/80 dark:border-slate-700 bg-white/80 dark:bg-slate-900/70 px-2.5 py-1.5 text-[11px] font-medium text-ink-600 dark:text-slate-300">
          <Info
            className="h-3.5 w-3.5 shrink-0 text-ink-500 dark:text-slate-400"
            aria-hidden="true"
          />
          <span>
            {isEn
              ? "Legacy assessment format (created before 4-criteria scoring). Resubmit your draft to view detailed criterion scores."
              : "Kết quả đánh giá theo phiên bản cũ (trước khi có 4 tiêu chí). Hãy kiểm tra lại bài viết để xem điểm chi tiết theo 4 tiêu chí."}
          </span>
        </div>
      )}

      {/* Four Assessment Criteria Grid */}
      {criteria.length > 0 && (
        <div className="space-y-2">
          <div className="text-[11px] font-extrabold uppercase tracking-wider text-ink-600 dark:text-slate-300">
            {isEn ? "Four-Criteria Assessment" : "Đánh giá theo 4 tiêu chí"}
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2.5">
            {criteria.map((item) => {
              const clamped = Math.max(0, Math.min(100, Math.round(item.score)));
              const tone = getScoreTone(clamped);
              const fallbackLabel = CRITERION_LABELS[item.criterion];
              const nameEn = item.nameEn?.trim() || fallbackLabel?.nameEn || item.criterion;
              const labelVn = item.labelVn?.trim() || fallbackLabel?.labelVn || "";

              return (
                <div
                  key={item.criterion}
                  className="rounded-xl border border-ink-200/80 dark:border-slate-700/80 bg-white/95 dark:bg-slate-900/85 p-3 space-y-2 shadow-2xs"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <div className="text-xs font-extrabold text-ink-900 dark:text-slate-100 leading-tight">
                        {nameEn}
                      </div>
                      {labelVn && (
                        <div className="text-[11px] font-medium text-ink-500 dark:text-slate-400 leading-snug mt-0.5">
                          {labelVn}
                        </div>
                      )}
                    </div>
                    <span
                      className={`inline-flex items-center rounded-full border px-2 py-0.5 text-[11px] font-extrabold tabular-nums shrink-0 ${tone.badge}`}
                    >
                      {`${clamped}/100`}
                    </span>
                  </div>

                  <div
                    role="progressbar"
                    aria-label={nameEn}
                    aria-valuenow={clamped}
                    aria-valuemin={0}
                    aria-valuemax={100}
                    className="h-1.5 w-full rounded-full bg-ink-100 dark:bg-slate-800 overflow-hidden"
                  >
                    <div
                      className={`h-full rounded-full transition-all duration-300 ${tone.bar}`}
                      style={{ width: `${clamped}%` }}
                    />
                  </div>

                  {item.feedbackVn && (
                    <p className="text-xs text-ink-700 dark:text-slate-300 leading-relaxed">
                      {item.feedbackVn}
                    </p>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Suggested Corrected Sentence / Paragraph Card */}
      {correctedSentence && (
        <div className="rounded-xl border border-brand-200/90 dark:border-emerald-700/50 bg-white/95 dark:bg-slate-900/90 p-3 space-y-2 shadow-2xs">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <span className="inline-flex items-center gap-1.5 text-[11px] font-extrabold uppercase tracking-wider text-brand-700 dark:text-emerald-300">
              <Sparkles className="h-3.5 w-3.5" aria-hidden="true" />
              <span>
                {isEn ? "Suggested correction" : "Gợi ý bản sửa"}
              </span>
            </span>
            <div className="flex items-center gap-1.5">
              <button
                type="button"
                onClick={() => onCopyCorrected(correctedSentence)}
                className="inline-flex items-center gap-1 rounded-lg border border-ink-200 dark:border-slate-700 bg-ink-50/80 dark:bg-slate-800 px-2 py-1 text-[11px] font-bold text-ink-700 dark:text-slate-200 hover:bg-ink-100 dark:hover:bg-slate-700 transition-colors cursor-pointer"
              >
                {copiedCorrected ? (
                  <>
                    <Check
                      className="h-3 w-3 text-brand-600 dark:text-emerald-400"
                      aria-hidden="true"
                    />
                    <span>{isEn ? "Copied" : "Đã chép"}</span>
                  </>
                ) : (
                  <>
                    <Copy className="h-3 w-3" aria-hidden="true" />
                    <span>{isEn ? "Copy" : "Sao chép"}</span>
                  </>
                )}
              </button>
              <button
                type="button"
                onClick={() => onApplyCorrected(correctedSentence)}
                className="inline-flex items-center gap-1 rounded-lg bg-brand-600 hover:bg-brand-700 px-2.5 py-1 text-[11px] font-bold text-white transition-colors cursor-pointer"
              >
                <PenLine className="h-3 w-3" aria-hidden="true" />
                <span>{isEn ? "Apply to draft" : "Áp dụng vào nháp"}</span>
              </button>
            </div>
          </div>
          <p className="text-xs sm:text-sm font-bold text-ink-900 dark:text-slate-100 leading-relaxed whitespace-pre-wrap">
            &ldquo;{correctedSentence}&rdquo;
          </p>
        </div>
      )}

      {/* Per-issue cards */}
      {issues.length > 0 && (
        <div className="space-y-2">
          {issues.map((issue, idx) => {
            const typeMeta = getIssueTypeMeta(issue, isEn);
            const baseDraft = userDraft ?? "";
            const isWholeDraftFallback =
              issue.ruleId === "TOO_SHORT" ||
              (issue.offset === 0 &&
                issue.length === baseDraft.length &&
                baseDraft.length > 20);
            const slicedOriginal =
              !isWholeDraftFallback &&
              typeof issue.offset === "number" &&
              typeof issue.length === "number" &&
              issue.offset >= 0 &&
              issue.length > 0 &&
              issue.offset + issue.length <= baseDraft.length
                ? baseDraft
                    .slice(issue.offset, issue.offset + issue.length)
                    .trim()
                : "";
            const isAiIssue = Boolean(issue.ruleId?.startsWith("AI_"));
            const aiMessageOriginal =
              isAiIssue &&
              issue.message?.trim() &&
              issue.message.trim() !== issue.messageVn?.trim()
                ? issue.message.trim()
                : "";
            const originalText =
              issue.original?.trim() || slicedOriginal || aiMessageOriginal;
            const suggestionText =
              issue.suggestion?.trim() ||
              issue.replacements?.[0]?.trim() ||
              "";
            const extraReplacements = (issue.replacements ?? []).filter(
              (rep) => Boolean(rep?.trim()) && rep.trim() !== suggestionText
            );
            const explanationText = isAiIssue
              ? issue.messageVn?.trim() ||
                (!aiMessageOriginal ? issue.message?.trim() : "") ||
                ""
              : isEn
                ? issue.message?.trim() || issue.messageVn?.trim() || ""
                : issue.messageVn?.trim() || issue.message?.trim() || "";

            return (
              <div
                key={idx}
                className="rounded-lg border border-ink-200/70 dark:border-slate-700/80 bg-white/90 dark:bg-slate-900/85 p-2.5 space-y-1.5 text-xs"
              >
                <div className="flex flex-wrap items-center gap-1.5">
                  <span
                    className={`inline-flex items-center rounded-md border px-1.5 py-0.5 text-[10px] font-extrabold uppercase tracking-wide ${typeMeta.className}`}
                  >
                    {typeMeta.label}
                  </span>

                  {(originalText || suggestionText) && (
                    <div className="inline-flex flex-wrap items-center gap-1.5 font-mono text-[11px]">
                      {originalText && (
                        <span className="rounded bg-rose-100/90 dark:bg-rose-950/60 px-1.5 py-0.5 text-rose-800 dark:text-rose-200 line-through">
                          {originalText}
                        </span>
                      )}
                      {originalText && suggestionText && (
                        <ArrowRight
                          className="h-3 w-3 text-ink-400 dark:text-slate-500 shrink-0"
                          aria-hidden="true"
                        />
                      )}
                      {suggestionText && (
                        <span className="rounded bg-emerald-100/90 dark:bg-emerald-950/60 px-1.5 py-0.5 font-bold text-emerald-800 dark:text-emerald-200">
                          {suggestionText}
                        </span>
                      )}
                    </div>
                  )}
                </div>

                {explanationText && (
                  <p className="text-ink-700 dark:text-slate-300 leading-relaxed">
                    {explanationText}
                    {extraReplacements.length > 0 && (
                      <span className="text-brand-700 dark:text-emerald-300 font-semibold">
                        {" "}
                        ({isEn ? "Also" : "Gợi ý khác"}:{" "}
                        {extraReplacements.join(", ")})
                      </span>
                    )}
                  </p>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Used / Missing Suggested Term Chips */}
      {(usedTerms.length > 0 || missingTerms.length > 0) && (
        <div className="space-y-2 pt-1 border-t border-ink-200/60 dark:border-slate-700/60">
          {usedTerms.length > 0 && (
            <div className="flex flex-wrap items-center gap-1.5">
              <span className="text-[11px] font-bold text-emerald-800 dark:text-emerald-300 mr-1">
                {isEn ? "Used terms:" : "Từ đã dùng:"}
              </span>
              {usedTerms.map((term) => (
                <span
                  key={term}
                  className="inline-flex items-center gap-1 rounded-full border border-emerald-300/80 dark:border-emerald-700/60 bg-emerald-100/80 dark:bg-emerald-950/60 px-2 py-0.5 text-[11px] font-semibold text-emerald-800 dark:text-emerald-200"
                >
                  <CheckCircle2
                    className="h-3 w-3 shrink-0"
                    aria-hidden="true"
                  />
                  <span>{term}</span>
                </span>
              ))}
            </div>
          )}

          {missingTerms.length > 0 && (
            <div className="flex flex-wrap items-center gap-1.5">
              <span className="text-[11px] font-bold text-amber-800 dark:text-amber-300 mr-1">
                {isEn ? "Try adding:" : "Gợi ý bổ sung:"}
              </span>
              {missingTerms.map((term) => (
                <button
                  key={term}
                  type="button"
                  onClick={() => onInsertTerm(term)}
                  title={
                    isEn
                      ? `Insert "${term}" into draft`
                      : `Chèn "${term}" vào câu nháp`
                  }
                  className="inline-flex items-center gap-1 rounded-full border border-amber-300/80 dark:border-amber-700/60 bg-amber-100/80 dark:bg-amber-950/60 px-2 py-0.5 text-[11px] font-semibold text-amber-900 dark:text-amber-200 hover:bg-amber-200/80 dark:hover:bg-amber-900/70 transition-colors cursor-pointer"
                >
                  <PlusCircle
                    className="h-3 w-3 shrink-0"
                    aria-hidden="true"
                  />
                  <span>{term}</span>
                </button>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
