import { LessonStatus } from "@/features/lesson/types/lesson";

type LessonStatusBadgeProps = {
  status: LessonStatus;
  processingStatus?: "PENDING" | "PROCESSING" | "DONE" | "FAILED";
  progress?: number;
};

export default function LessonStatusBadge({
  status,
  processingStatus,
  progress,
}: LessonStatusBadgeProps) {
  const isPublished = status === LessonStatus.PUBLISHED;

  return (
    <div className="flex flex-col items-start gap-1.5">
      <span
        className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-bold ${
          isPublished
            ? "border-emerald-200 bg-emerald-50 text-emerald-700"
            : "border-slate-200 bg-slate-100 text-slate-700"
        }`}
      >
        <span
          className={`h-1.5 w-1.5 rounded-full ${
            isPublished ? "bg-emerald-500" : "bg-slate-400"
          }`}
          aria-hidden="true"
        />
        {isPublished ? "Published" : "Hidden"}
      </span>

      {processingStatus && processingStatus !== "DONE" ? (
        <span
          className={`inline-flex items-center gap-1 rounded-md border px-2 py-0.5 text-[11px] font-semibold ${
            processingStatus === "FAILED"
              ? "border-rose-200 bg-rose-50 text-rose-700"
              : processingStatus === "PROCESSING"
                ? "border-sky-200 bg-sky-50 text-sky-700"
                : "border-amber-200 bg-amber-50 text-amber-700"
          }`}
        >
          {processingStatus === "FAILED"
            ? "Processing failed"
            : processingStatus === "PROCESSING"
              ? `Processing ${typeof progress === "number" ? `${progress}%` : ""}`.trim()
              : "Pending queue"}
        </span>
      ) : null}
    </div>
  );
}
