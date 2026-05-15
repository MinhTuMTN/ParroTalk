"use client";

import { Plus, Scissors, Trash2 } from "lucide-react";
import Button from "@/components/ui/Button";

import type { Segment } from "@/features/lesson/types/lesson";

type SegmentEditorProps = {
  segments: Segment[];
  activeSegmentId: string | null;
    onSeek: (segmentId: string) => void;

  onAdd: (afterId?: string) => void;
  onDelete: (id: string) => void;
  onSplit: (id: string, cursorPosition?: number) => void;
    onChange: (id: string, patch: Partial<Segment>) => void;
    onSaveChanges: () => void;
  onNoChangesSaveAttempt: () => void;
  isSaving: boolean;
  hasSegmentChanges: boolean;
};



const toClock = (value: number) => {
  const mins = Math.floor(value / 60);
  const secs = Math.floor(value % 60);
  const ms = Math.round((value - Math.floor(value)) * 100);
  return `${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}.${String(ms).padStart(2, "0")}`;
};

export default function SegmentEditor({
  segments,
  activeSegmentId,
  onSeek,
  onAdd,
  onDelete,
  onSplit,
    onChange,
    onSaveChanges,
  onNoChangesSaveAttempt,
  isSaving,
  hasSegmentChanges,
}: SegmentEditorProps) {


  return (
    <section className="rounded-[28px] bg-[#f8fafc] p-5 shadow-inner">
            <div className="mb-4 flex items-center justify-between gap-2">
        <div>
                    <div className="flex items-center gap-2">
            <h2 className="text-2xl font-bold text-slate-900">Segment List</h2>
            {hasSegmentChanges ? (
              <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-700">
                Unsaved segment changes
              </span>
            ) : null}
          </div>

          <p className="text-sm text-slate-500">Editing {segments.length} audio segments</p>
        </div>
        <div className="flex items-center gap-2">
                    <Button
            onClick={hasSegmentChanges ? onSaveChanges : onNoChangesSaveAttempt}
            disabled={isSaving}
            className="px-4 py-2 text-sm"
          >
            {isSaving ? "Saving..." : "Save Changes"}
          </Button>

          <button
            type="button"
            onClick={() => onAdd()}
            className="inline-flex items-center gap-2 rounded-2xl bg-slate-700 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-800"
          >
            <Plus className="h-4 w-4" /> New Segment
          </button>
        </div>
      </div>


      <div className="max-h-[58vh] space-y-3 overflow-y-auto pr-2">
        {segments.map((segment) => {
          const active = segment.id === activeSegmentId;

          return (
            <article
              key={segment.id}
              data-segment-id={segment.id}
              className={`rounded-3xl border bg-white p-4 shadow-sm transition-all duration-200 ${
                active
                  ? "border-emerald-400 shadow-[0_12px_30px_rgba(16,185,129,0.2)] ring-2 ring-emerald-100"
                  : "border-slate-200"
              }`}
            >
              <div className="grid gap-3 md:grid-cols-[180px_1fr_40px]">
                <button
                  type="button"
                  onClick={() => onSeek(segment.id)}
                  className="rounded-2xl bg-slate-100 p-3 text-left font-mono text-xs text-slate-600 transition hover:bg-emerald-50 hover:text-emerald-700"
                >
                  <p className="text-[10px] font-bold tracking-wide text-emerald-700">START</p>
                  <p className="mt-1 text-sm font-semibold">{toClock(segment.startTime)}</p>
                  <p className="mt-3 text-[10px] font-bold tracking-wide text-slate-500">END</p>
                  <p className="mt-1 text-sm font-semibold">{toClock(segment.endTime)}</p>
                </button>

                <div>
                  <textarea
                    value={segment.text}
                    onChange={(event) => onChange(segment.id, { text: event.target.value })}
                    onClick={() => onSeek(segment.id)}
                    onKeyDown={(event) => {
                      if (event.key === "Enter" && !event.shiftKey) {
                        event.preventDefault();
                        const target = event.currentTarget;
                        onSplit(segment.id, target.selectionStart);
                      }
                    }}
                    className="h-24 w-full resize-none rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700 outline-none transition focus:border-emerald-500 focus:bg-white"
                    placeholder="Type transcript..."
                  />

                  <div className="mt-2 grid grid-cols-2 gap-2">
                    <label className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-500">
                      Start
                      <input
                        type="number"
                        value={segment.startTime}
                        min={0}
                        step={0.1}
                        onChange={(event) =>
                          onChange(segment.id, {
                            startTime: Number(event.target.value),
                          })
                        }
                        className="mt-1 w-full bg-transparent text-sm font-medium text-slate-700 outline-none"
                      />
                    </label>
                    <label className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-500">
                      End
                      <input
                        type="number"
                        value={segment.endTime}
                        min={0}
                        step={0.1}
                        onChange={(event) =>
                          onChange(segment.id, {
                            endTime: Number(event.target.value),
                          })
                        }
                        className="mt-1 w-full bg-transparent text-sm font-medium text-slate-700 outline-none"
                      />
                    </label>
                  </div>
                </div>

                                <div className="flex flex-col items-center gap-2 pt-1 text-slate-500">

                  <button
                    type="button"
                    onClick={() => onSplit(segment.id)}
                    className="rounded-lg p-2 transition hover:bg-slate-100 hover:text-emerald-700"
                    title="Split segment"
                  >
                    <Scissors className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => onAdd(segment.id)}
                    className="rounded-lg p-2 transition hover:bg-slate-100 hover:text-emerald-700"
                    title="Add after"
                  >
                    <Plus className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => onDelete(segment.id)}
                    className="rounded-lg p-2 transition hover:bg-rose-50 hover:text-rose-600"
                    title="Delete"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}

