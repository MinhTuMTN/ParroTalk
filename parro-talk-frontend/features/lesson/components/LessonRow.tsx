"use client";

import { useState } from "react";
import {
  Clock,
  ExternalLink,
  Headphones,
  ListChecks,
  Video,
  PlaySquare,
  Cloud,
} from "lucide-react";
import type { Lesson } from "@/features/lesson/types/lesson";
import LessonActions from "@/features/lesson/components/LessonActions";
import LessonStatusBadge from "@/features/lesson/components/LessonStatusBadge";

type LessonRowProps = {
  lesson: Lesson;
  selected: boolean;
  disabled?: boolean;
  onSelect: (id: string, checked: boolean) => void;
  onToggleStatus: (lesson: Lesson, next: boolean) => void;
  onEdit: (lesson: Lesson) => void;
  onDelete: (lesson: Lesson) => void;
};

const formatDuration = (totalSeconds: number) => {
  const safe = Math.max(0, Math.floor(totalSeconds || 0));
  const hrs = Math.floor(safe / 3600);
  const mins = Math.floor((safe % 3600) / 60);
  const secs = safe % 60;
  if (hrs > 0) {
    return `${hrs}:${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
  }
  return `${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
};

const formatCreatedDate = (value: string) => {
  if (!value) return "—";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return parsed.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
};

export default function LessonRow({
  lesson,
  selected,
  disabled = false,
  onSelect,
  onToggleStatus,
  onEdit,
  onDelete,
}: LessonRowProps) {
  const [failedThumbUrl, setFailedThumbUrl] = useState<string | null>(null);
  const isVideo = lesson.mediaType?.toUpperCase() === "VIDEO";
  const isYoutube = lesson.sourceType?.toUpperCase() === "YOUTUBE";
  const hasValidThumbnail =
    Boolean(lesson.thumbnailUrl) && lesson.thumbnailUrl !== failedThumbUrl;
  const visibleCategories = lesson.categories.slice(0, 2);
  const remainingCategoryCount = Math.max(0, lesson.categories.length - 2);
  const hasExternalSource =
    Boolean(lesson.source) && /^https?:\/\//i.test(lesson.source ?? "");

  return (
    <tr
      className={`group transition-colors ${
        selected ? "bg-emerald-50/50" : "hover:bg-slate-50/80"
      }`}
    >
      <td className="py-4 pl-4 pr-2 align-middle">
        <input
          type="checkbox"
          checked={selected}
          disabled={disabled}
          aria-label={`Select "${lesson.title}"`}
          onChange={(event) => onSelect(lesson.id, event.target.checked)}
          className="h-4 w-4 cursor-pointer rounded border-slate-300 accent-emerald-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
        />
      </td>

      {/* Lesson Thumbnail, Title & Categories */}
      <td className="py-4 px-3 align-middle">
        <div className="flex items-start gap-3.5">
          <button
            type="button"
            onClick={() => onEdit(lesson)}
            aria-label={`Open editor for "${lesson.title}"`}
            className="relative flex h-11 w-16 shrink-0 cursor-pointer items-center justify-center overflow-hidden rounded-xl border border-slate-200/80 bg-slate-100 text-slate-400 transition group-hover:border-emerald-300 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
          >
            {hasValidThumbnail ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={lesson.thumbnailUrl ?? undefined}
                alt=""
                onError={() => setFailedThumbUrl(lesson.thumbnailUrl ?? null)}
                className="h-full w-full object-cover"
              />
            ) : isVideo ? (
              <Video className="h-5 w-5 text-slate-400" aria-hidden="true" />
            ) : (
              <Headphones className="h-5 w-5 text-slate-400" aria-hidden="true" />
            )}
          </button>

          <div className="min-w-0 flex-1">
            <button
              type="button"
              onClick={() => onEdit(lesson)}
              className="line-clamp-2 cursor-pointer text-left text-sm font-bold text-slate-900 transition-colors hover:text-emerald-700 focus-visible:outline-none focus-visible:underline"
            >
              {lesson.title || "Untitled Lesson"}
            </button>

            <div className="mt-1.5 flex flex-wrap items-center gap-1.5">
              {visibleCategories.length > 0 ? (
                <>
                  {visibleCategories.map((category) => (
                    <span
                      key={category.id}
                      className="inline-flex items-center rounded-md border border-slate-200 bg-slate-50 px-2 py-0.5 text-[11px] font-semibold text-slate-600"
                    >
                      {category.name}
                    </span>
                  ))}
                  {remainingCategoryCount > 0 ? (
                    <span className="inline-flex items-center rounded-md bg-emerald-50 px-1.5 py-0.5 text-[11px] font-bold text-emerald-700">
                      +{remainingCategoryCount}
                    </span>
                  ) : null}
                </>
              ) : (
                <span className="text-[11px] font-medium italic text-slate-400">
                  Uncategorized
                </span>
              )}
            </div>
          </div>
        </div>
      </td>

      {/* Media & Source */}
      <td className="py-4 px-3 align-middle">
        <div className="flex flex-col gap-1.5">
          <div className="flex flex-wrap items-center gap-1.5">
            <span
              className={`inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-[11px] font-bold ${
                isVideo
                  ? "bg-indigo-50 text-indigo-700"
                  : "bg-teal-50 text-teal-700"
              }`}
            >
              {isVideo ? (
                <Video className="h-3 w-3" aria-hidden="true" />
              ) : (
                <Headphones className="h-3 w-3" aria-hidden="true" />
              )}
              {lesson.mediaType || "AUDIO"}
            </span>

            <span className="inline-flex items-center gap-1 rounded-md bg-slate-100 px-2 py-0.5 text-[11px] font-semibold text-slate-600">
              {isYoutube ? (
                <PlaySquare className="h-3 w-3 text-rose-500" aria-hidden="true" />
              ) : (
                <Cloud className="h-3 w-3 text-sky-500" aria-hidden="true" />
              )}
              {isYoutube ? "YouTube" : "Cloudinary"}
            </span>

            {typeof lesson.totalSegments === "number" ? (
              <span className="inline-flex items-center gap-1 rounded-md bg-slate-100 px-2 py-0.5 text-[11px] font-semibold tabular-nums text-slate-600">
                <ListChecks className="h-3 w-3 text-slate-400" aria-hidden="true" />
                {lesson.totalSegments} segs
              </span>
            ) : null}
          </div>

          {lesson.source ? (
            hasExternalSource ? (
              <a
                href={lesson.source}
                target="_blank"
                rel="noopener noreferrer"
                aria-label={`Open source media for ${lesson.title}`}
                className="inline-flex max-w-[210px] items-center gap-1 truncate text-xs text-slate-500 transition hover:text-emerald-700"
              >
                <span className="truncate">{lesson.source}</span>
                <ExternalLink className="h-3 w-3 shrink-0" aria-hidden="true" />
              </a>
            ) : (
              <span className="max-w-[210px] truncate text-xs text-slate-500">
                {lesson.source}
              </span>
            )
          ) : (
            <span className="text-xs italic text-slate-400">No source URL</span>
          )}
        </div>
      </td>

      {/* Duration */}
      <td className="py-4 px-3 align-middle">
        <span className="inline-flex items-center gap-1.5 text-xs font-semibold tabular-nums text-slate-700">
          <Clock className="h-3.5 w-3.5 text-slate-400" aria-hidden="true" />
          {formatDuration(lesson.duration)}
        </span>
      </td>

      {/* Status */}
      <td className="py-4 px-3 align-middle">
        <LessonStatusBadge
          status={lesson.status}
          processingStatus={lesson.processingStatus}
          progress={lesson.progress}
        />
      </td>

      {/* Created Date */}
      <td className="py-4 px-3 align-middle text-xs font-medium tabular-nums text-slate-500">
        {formatCreatedDate(lesson.createdAt)}
      </td>

      {/* Actions */}
      <td className="py-4 pl-3 pr-4 text-right align-middle">
        <LessonActions
          lesson={lesson}
          disabled={disabled}
          onToggleStatus={onToggleStatus}
          onEdit={onEdit}
          onDelete={onDelete}
        />
      </td>
    </tr>
  );
}
