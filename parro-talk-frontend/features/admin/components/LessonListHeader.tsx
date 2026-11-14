import { BookOpen, Plus, RefreshCw } from "lucide-react";
import Link from "next/link";
import Badge from "@/components/ui/Badge";
import Button from "@/components/ui/Button";

type LessonListHeaderProps = {
  totalCount?: number;
  loading?: boolean;
  onRefresh?: () => void;
};

export default function LessonListHeader({
  totalCount,
  loading = false,
  onRefresh,
}: LessonListHeaderProps) {
  return (
    <div className="flex flex-col justify-between gap-4 rounded-3xl border border-slate-100 bg-white p-6 shadow-sm sm:flex-row sm:items-center sm:p-8">
      <div className="flex items-start gap-4">
        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-tr from-emerald-600 to-teal-500 text-white shadow-lg shadow-emerald-500/20">
          <BookOpen className="h-6 w-6" aria-hidden="true" />
        </div>
        <div>
          <span className="text-[11px] font-bold uppercase tracking-widest text-emerald-700">
            Content Curriculum
          </span>
          <div className="mt-0.5 flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-black tracking-tight text-slate-900">
              Lesson Management
            </h1>
            {typeof totalCount === "number" ? (
              <Badge tone="success" className="tabular-nums">
                {totalCount} {totalCount === 1 ? "Lesson" : "Lessons"}
              </Badge>
            ) : null}
          </div>
          <p className="mt-1 max-w-2xl text-sm text-slate-500">
            Organize, review media &amp; transcript segments, assign categories, and publish your English dictation curriculum.
          </p>
        </div>
      </div>

      <div className="flex shrink-0 flex-wrap items-center gap-2.5">
        {onRefresh ? (
          <Button
            type="button"
            variant="secondary"
            size="md"
            disabled={loading}
            onClick={onRefresh}
            leftIcon={
              <RefreshCw
                className={`h-4 w-4 ${loading ? "animate-spin" : ""}`}
                aria-hidden="true"
              />
            }
          >
            Refresh
          </Button>
        ) : null}

        <Link
          href="/upload"
          className="inline-flex items-center justify-center gap-2 rounded-2xl bg-emerald-600 px-5 py-2.5 text-sm font-semibold text-white shadow-md shadow-emerald-600/20 transition-all hover:bg-emerald-700 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2"
        >
          <Plus className="h-4 w-4" aria-hidden="true" />
          <span>Create Lesson</span>
        </Link>
      </div>
    </div>
  );
}
