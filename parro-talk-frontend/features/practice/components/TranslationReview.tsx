"use client";

import { ArrowRight } from "lucide-react";

type TranslationReviewProps = {
  sentence: string;
  translatedText?: string | null;
  onContinue: () => void;
};

export default function TranslationReview({
  sentence,
  translatedText,
  onContinue,
}: TranslationReviewProps) {
  return (
    <section className="w-full border-t border-gray-100 bg-white px-4 py-4 shadow-[0_-8px_30px_rgba(0,0,0,0.04)] sm:px-6 sm:py-6">
      <div className="mx-auto flex max-w-4xl flex-col gap-4">
        <div>
          <p className="text-sm font-semibold text-gray-400">Completed sentence</p>
          <p className="mt-1 text-base font-bold text-gray-900 sm:text-lg">{sentence}</p>
        </div>

        <div className="rounded-md border border-emerald-100 bg-emerald-50 px-4 py-3">
          <p className="text-sm font-semibold text-emerald-700">Vietnamese translation</p>
          <p className="mt-1 text-base font-medium text-emerald-950">
            {translatedText || "Translation is not available yet."}
          </p>
        </div>

        <button
          type="button"
          onClick={onContinue}
          className="inline-flex min-h-11 items-center justify-center gap-2 rounded-md bg-emerald-500 px-4 py-2 font-semibold text-white transition hover:bg-emerald-600"
        >
          Continue
          <ArrowRight className="h-4 w-4" />
        </button>
      </div>
    </section>
  );
}
