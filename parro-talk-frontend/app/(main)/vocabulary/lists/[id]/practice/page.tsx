"use client";

import { useEffect, useState } from "react";
import { useParams, useSearchParams } from "next/navigation";
import Link from "next/link";
import {
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  RotateCcw,
  Sparkles,
  XCircle,
} from "lucide-react";
import type { CustomListMcqResponse } from "@/features/vocabulary/types/customList";
import {
  fetchCustomListPracticeQuestions,
  submitCustomListAnswer,
} from "@/features/vocabulary/services/customVocabularyListService";
import {
  formatGenerationSourceBadge,
  formatQuestionCategoryLabel,
} from "@/features/vocabulary/components/custom-lists/customListUtils";

export default function CustomListMcqPracticePage() {
  const params = useParams<{ id: string }>();
  const searchParams = useSearchParams();
  const listId = params.id;
  const shareToken = searchParams.get("shareToken") || undefined;

  const [questions, setQuestions] = useState<CustomListMcqResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [index, setIndex] = useState(0);
  const [selectedOption, setSelectedOption] = useState<string | null>(null);
  const [score, setScore] = useState(0);
  const [completed, setCompleted] = useState(false);

  useEffect(() => {
    fetchCustomListPracticeQuestions(listId, shareToken)
      .then((res) => setQuestions(res))
      .finally(() => setLoading(false));
  }, [listId, shareToken]);

  if (loading) {
    return (
      <div className="mx-auto max-w-3xl p-8 text-center text-sm text-ink-500">
        Đang tải bộ câu hỏi Multiple Choice 8 dạng...
      </div>
    );
  }

  if (questions.length === 0) {
    return (
      <div className="mx-auto max-w-2xl space-y-4 p-8 text-center">
        <p className="text-base font-bold text-ink-900">
          Chưa có câu hỏi nào cho danh sách này.
        </p>
        <p className="text-xs text-ink-500">
          Hãy quay lại trang chi tiết danh sách và bấm &ldquo;Chốt từ vựng &amp;
          Tạo câu hỏi AI (Batch)&rdquo;.
        </p>
        <Link
          href={`/vocabulary/lists/${listId}`}
          className="inline-flex items-center gap-1.5 rounded-xl bg-brand-600 px-4 py-2 text-xs font-bold text-white"
        >
          <ArrowLeft className="h-4 w-4" /> Quay lại danh sách
        </Link>
      </div>
    );
  }

  if (completed) {
    return (
      <div className="mx-auto max-w-xl py-12 px-4">
        <div className="rounded-2xl border border-ink-200 bg-white p-8 text-center shadow-card">
          <h1 className="text-2xl font-extrabold text-ink-900">
            Hoàn thành phiên luyện tập!
          </h1>
          <p className="mt-2 text-sm text-ink-600">
            Bạn trả lời đúng{" "}
            <strong className="text-brand-600">
              {score} / {questions.length}
            </strong>{" "}
            câu hỏi.
          </p>
          <div className="mt-6 flex justify-center gap-3">
            <button
              type="button"
              onClick={() => {
                setIndex(0);
                setScore(0);
                setSelectedOption(null);
                setCompleted(false);
              }}
              className="inline-flex items-center gap-1.5 rounded-xl border border-ink-200 px-4 py-2 text-xs font-bold text-ink-700 hover:bg-ink-50"
            >
              <RotateCcw className="h-4 w-4" /> Làm lại
            </button>
            <Link
              href={`/vocabulary/lists/${listId}`}
              className="inline-flex items-center gap-1.5 rounded-xl bg-brand-600 px-4 py-2 text-xs font-bold text-white hover:bg-brand-700"
            >
              Về chi tiết danh sách
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const current = questions[index];
  const isAnswered = selectedOption !== null;
  const isCorrect = selectedOption === current.correctAnswer;

  const handleSelect = (opt: string) => {
    if (isAnswered) return;
    setSelectedOption(opt);
    const correct = opt === current.correctAnswer;
    if (correct) {
      setScore((s) => s + 1);
    }
    submitCustomListAnswer(listId, {
      questionId: current.id,
      isCorrect: correct,
    }).catch(() => {
      // background sync
    });
  };

  const handleNext = () => {
    if (index + 1 >= questions.length) {
      setCompleted(true);
    } else {
      setIndex((i) => i + 1);
      setSelectedOption(null);
    }
  };

  return (
    <div className="min-h-full bg-ink-50 py-8 px-4">
      <div className="mx-auto max-w-3xl space-y-6">
        <div className="flex items-center justify-between">
          <Link
            href={`/vocabulary/lists/${listId}`}
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-ink-500 hover:text-brand-600"
          >
            <ArrowLeft className="h-4 w-4" /> Thoát luyện tập
          </Link>
          <span className="text-xs font-bold text-ink-600">
            Câu {index + 1} / {questions.length} • Đúng: {score}
          </span>
        </div>

        <div className="rounded-2xl border border-ink-200 bg-white p-6 shadow-card">
          {/* Category & Source Badges */}
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded-full bg-brand-100 px-3 py-0.5 text-xs font-bold text-brand-700">
                {formatQuestionCategoryLabel(current.questionCategory)}
              </span>
              {current.cefrLevel && (
                <span className="rounded-md bg-ink-100 px-2 py-0.5 text-xs font-bold text-ink-700">
                  {current.cefrLevel}
                </span>
              )}
              <span className="text-xs text-ink-500">
                Từ mục tiêu: <strong>{current.targetWord}</strong>
              </span>
            </div>

            <span className="inline-flex items-center gap-1 rounded-full bg-ocean-50 px-2.5 py-0.5 text-[11px] font-semibold text-ocean-700 border border-ocean-200">
              <Sparkles className="h-3 w-3" />
              Nguồn câu hỏi:{" "}
              {formatGenerationSourceBadge(current.generationSource)}
            </span>
          </div>

          <h2 className="mt-4 text-lg font-bold leading-relaxed text-ink-900">
            {current.questionText}
          </h2>

          <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2">
            {current.options.map((opt) => {
              let style =
                "border-ink-200 bg-white text-ink-800 hover:border-brand-400";
              if (isAnswered) {
                if (opt === current.correctAnswer) {
                  style =
                    "border-emerald-500 bg-emerald-50 text-emerald-900 font-bold";
                } else if (opt === selectedOption) {
                  style = "border-red-400 bg-red-50 text-red-800";
                } else {
                  style = "border-ink-100 bg-ink-50/40 text-ink-400";
                }
              }

              return (
                <button
                  key={opt}
                  type="button"
                  disabled={isAnswered}
                  onClick={() => handleSelect(opt)}
                  className={`flex items-center justify-between rounded-xl border p-4 text-left text-sm transition-all ${style}`}
                >
                  <span>{opt}</span>
                  {isAnswered && opt === current.correctAnswer && (
                    <CheckCircle2 className="h-4.5 w-4.5 shrink-0 text-emerald-600" />
                  )}
                  {isAnswered &&
                    opt === selectedOption &&
                    opt !== current.correctAnswer && (
                      <XCircle className="h-4.5 w-4.5 shrink-0 text-red-600" />
                    )}
                </button>
              );
            })}
          </div>

          {isAnswered && (
            <div className="mt-6 space-y-2 rounded-xl border border-ink-100 bg-ink-50 p-4 text-xs">
              <div
                className={`font-bold ${
                  isCorrect ? "text-emerald-700" : "text-red-700"
                }`}
              >
                {isCorrect
                  ? "Chính xác!"
                  : `Chưa đúng. Đáp án đúng là: ${current.correctAnswer}`}
              </div>
              {current.completedSentence && (
                <p className="italic text-ink-700">
                  Câu hoàn chỉnh: &ldquo;{current.completedSentence}&rdquo;
                </p>
              )}
              {current.explanationVn && (
                <p className="text-ink-800">
                  <strong>Giải thích (VN):</strong> {current.explanationVn}
                </p>
              )}
              {current.explanationEn && (
                <p className="text-ink-500">
                  <strong>Explanation (EN):</strong> {current.explanationEn}
                </p>
              )}

              <div className="pt-2 text-right">
                <button
                  type="button"
                  onClick={handleNext}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-brand-600 px-4 py-2 text-xs font-bold text-white hover:bg-brand-700"
                >
                  {index + 1 >= questions.length
                    ? "Xem kết quả"
                    : "Câu tiếp theo"}{" "}
                  <ArrowRight className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
