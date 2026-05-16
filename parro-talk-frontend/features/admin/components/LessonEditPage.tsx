"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Pause, Play, RotateCcw, Trash2, X } from "lucide-react";

import ReactPlayer from "react-player";
import Sidebar from "@/components/common/Sidebar";

import Button from "@/components/ui/Button";
import Input from "@/components/ui/Input";
import Switch from "@/components/ui/Switch";
import Badge from "@/components/ui/Badge";

import SegmentEditor from "@/features/lesson/components/SegmentEditor";
import { useLessonDetail } from "@/features/lesson/hooks/useLessonDetail";
import ConfirmationModal from "@/components/ui/ConfirmationModal";
import FloatingToast from "@/components/ui/FloatingToast";

import { lessonService } from "@/features/lesson/services/lessonService";
import type { Category } from "@/features/lesson/services/lessonService";
import { LessonStatus } from "@/features/lesson/types/lesson";



const formatClock = (value: number) => {
  const mins = Math.floor(value / 60);
  const secs = Math.floor(value % 60);
  return `${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
};

export default function LessonEditPage({ id }: { id: string }) {
  const router = useRouter();
  const playerRef = useRef<ReactPlayer>(null);
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [playedSeconds, setPlayedSeconds] = useState(0);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [selectedSegmentId, setSelectedSegmentId] = useState<string | null>(null);

  const [categoryInput, setCategoryInput] = useState("");
  const [availableCategories, setAvailableCategories] = useState<Category[]>([]);
  const [filteredCategoryOptions, setFilteredCategoryOptions] = useState<Category[]>([]);
  const [isCategoryLoading, setIsCategoryLoading] = useState(false);

  const mergeCategories = (current: Category[], incoming: Category[]) => {
    const map = new Map(current.map((item) => [item.id, item]));
    incoming.forEach((item) => map.set(item.id, item));
    return Array.from(map.values());
  };


  const [isCategoryDropdownOpen, setIsCategoryDropdownOpen] = useState(false);
  const [isCreatingCategory, setIsCreatingCategory] = useState(false);

  const [finishedSegmentId, setFinishedSegmentId] = useState<string | null>(null);
  const [dragging, setDragging] = useState<{ id: string; edge: "start" | "end" } | null>(null);
  const [timelineZoom, setTimelineZoom] = useState(2);
  const [hoveredSegmentId, setHoveredSegmentId] = useState<string | null>(null);
  const [segmentToast, setSegmentToast] = useState<string | null>(null);

  const trackRef = useRef<HTMLDivElement | null>(null);
  const trackScrollRef = useRef<HTMLDivElement | null>(null);
  const categoryBoxRef = useRef<HTMLLabelElement | null>(null);

  const {
    lesson,
    form,
    segments,
    loading,
    saving,
    deleting,
    error,
    success,
    updateForm,
    updateSegment,
    addSegment,
    removeSegment,
    splitSegment,
    hasSegmentChanges,
    saveChanges,
    deleteLesson,

    clearMessages,
  } = useLessonDetail(id);

  const safeDuration = form?.duration ?? lesson?.duration ?? 1;
  const effectiveDuration = useMemo(() => {
    if (safeDuration > 0) return safeDuration;
    const maxSegmentEnd = segments.reduce((max, segment) => Math.max(max, segment.endTime), 0);
    return Math.max(playedSeconds, maxSegmentEnd, 1);
  }, [playedSeconds, safeDuration, segments]);


  const selectedSegment = useMemo(
    () => segments.find((segment) => segment.id === selectedSegmentId) ?? null,
    [segments, selectedSegmentId],
  );

  const playbackStart = selectedSegment?.startTime ?? 0;
  const playbackEnd = selectedSegment?.endTime ?? safeDuration;
  const playbackDuration = Math.max(0.1, playbackEnd - playbackStart);

  const handleSeek = (time: number, bounds?: { start: number; end: number }) => {
    const start = bounds?.start ?? playbackStart;
    const end = bounds?.end ?? playbackEnd;
    const nextTime = Math.max(start, Math.min(time, end));
    playerRef.current?.seekTo(nextTime, "seconds");
    setPlayedSeconds(nextTime);
  };

  const selectAndPlaySegment = (segmentId: string) => {
    const segment = segments.find((item) => item.id === segmentId);
    if (!segment) return;
    setSelectedSegmentId(segment.id);
    setFinishedSegmentId(null);
    handleSeek(segment.startTime, { start: segment.startTime, end: segment.endTime });
    setIsPlaying(true);
  };


  const handleReplaySegment = () => {
    if (!selectedSegment) return;
    setFinishedSegmentId(null);
    playerRef.current?.seekTo(selectedSegment.startTime, "seconds");
    setPlayedSeconds(selectedSegment.startTime);
    setIsPlaying(true);
  };

  const setCurrentAsBoundary = (edge: "start" | "end") => {
    if (!selectedSegment) return;
    const current = Number(playedSeconds.toFixed(2));
    if (edge === "start") {
      updateSegment(selectedSegment.id, { startTime: Math.max(0, Math.min(current, selectedSegment.endTime - 0.05)) });
      return;
    }
    updateSegment(selectedSegment.id, { endTime: Math.max(selectedSegment.startTime + 0.05, Math.min(current, effectiveDuration)) });

  };

  const splitAtSelectedEnd = () => {
    if (!selectedSegment) return;
    addSegment(selectedSegment.id);
  };

  const activeSegmentId = useMemo(() => {
    const active = segments.find(
      (segment) => playedSeconds >= segment.startTime && playedSeconds <= segment.endTime,
    );
    return active?.id ?? null;
  }, [segments, playedSeconds]);

  useEffect(() => {
    if (!selectedSegmentId) return;
    const target = document.querySelector(`[data-segment-id="${selectedSegmentId}"]`);
    if (!target) return;
    target.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }, [selectedSegmentId]);

  useEffect(() => {
    if (!segmentToast) return;
    const timer = window.setTimeout(() => setSegmentToast(null), 2500);
    return () => window.clearTimeout(timer);
  }, [segmentToast]);

  useEffect(() => {
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      if (!hasSegmentChanges) return;
      event.preventDefault();
      event.returnValue = "";
    };

    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [hasSegmentChanges]);


  useEffect(() => {
    if (!form) return;
    setCategoryInput("");
  }, [form]);

  useEffect(() => {
    let mounted = true;

    const loadCategories = async () => {
      try {
        const response = await lessonService.getCategories("", 0, 100);
        if (!mounted) return;
        setAvailableCategories(response.content ?? []);
      } catch {
        if (!mounted) return;
        setAvailableCategories([]);
      }
    };

    void loadCategories();
    return () => {
      mounted = false;
    };
  }, []);


  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const targetTag = (event.target as HTMLElement).tagName;
      const isTyping = ["INPUT", "TEXTAREA"].includes(targetTag);

      if (event.code === "Space" && !isTyping) {
        event.preventDefault();
        setIsPlaying((prev) => !prev);
      }

      if (event.key === "Enter" && !isTyping && selectedSegmentId) {
        event.preventDefault();
        splitSegment(selectedSegmentId);
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [selectedSegmentId, splitSegment]);

  useEffect(() => {
    if (!form) return;

    const query = categoryInput.trim();
    let mounted = true;
    const debounce = window.setTimeout(async () => {
      setIsCategoryLoading(true);
      try {
        const response = await lessonService.getCategories(query, 0, 6);
        if (!mounted) return;
        const options = (response.content ?? []).filter((category) => !form.categoryIds.includes(category.id));
        setFilteredCategoryOptions(options);
        setAvailableCategories((current) => mergeCategories(current, response.content ?? []));
      } catch {
        if (!mounted) return;
        setFilteredCategoryOptions([]);
      } finally {
        if (!mounted) return;
        setIsCategoryLoading(false);
      }
    }, 250);

    return () => {
      mounted = false;
      window.clearTimeout(debounce);
    };
  }, [categoryInput, form]);


  const canCreateCategory = useMemo(() => {
    if (!form) return false;
    const value = categoryInput.trim();
    if (!value) return false;
    return !availableCategories.some((category) => category.name.toLowerCase() === value.toLowerCase());
  }, [availableCategories, categoryInput, form]);

  const addCategory = (category: Category) => {
    if (!form) return;

    const next = Array.from(new Set([...form.categoryIds, category.id]));
    updateForm("categoryIds", next);
    setCategoryInput("");
  };

  const createCategory = async () => {
    if (!form) return;
    const name = categoryInput.trim();
    if (!name || isCreatingCategory) return;

    setIsCreatingCategory(true);
    try {
      const created = await lessonService.createCategory(name);
      setAvailableCategories((current) => {
        if (current.some((item) => item.id === created.id)) return current;
        return [...current, created];
      });
      addCategory(created);
    } catch {
      // Keep existing page-level error handling unchanged.
    } finally {
      setIsCreatingCategory(false);
    }
  };

  const removeCategory = (categoryId: string) => {
    if (!form) return;
    updateForm(
      "categoryIds",
      form.categoryIds.filter((id) => id !== categoryId),
    );
  };

  useEffect(() => {
    const handleOutsideClick = (event: MouseEvent) => {
      if (!categoryBoxRef.current) return;
      if (!categoryBoxRef.current.contains(event.target as Node)) {
        setIsCategoryDropdownOpen(false);
      }
    };

    document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, []);

  const timelineSegments = useMemo(() => {


    const duration = Math.max(0.01, effectiveDuration);

    return segments.map((segment, index) => {

      const boundedStart = Math.max(0, Math.min(segment.startTime, duration));
      const boundedEnd = Math.max(boundedStart, Math.min(segment.endTime, duration));

      const rawLeft = (boundedStart / duration) * 100;
      const rawWidth = ((boundedEnd - boundedStart) / duration) * 100;

      const safeLeft = Math.max(0, Math.min(rawLeft, 100));
      const safeWidth = Math.max(0, Math.min(rawWidth, 100 - safeLeft));

      return {
        ...segment,
        lane: index % 2,
        left: Number(safeLeft.toFixed(2)),
        width: Number(safeWidth.toFixed(2)),
      };
    });
  }, [effectiveDuration, segments]);



  useEffect(() => {
    if (!dragging) return;

    const handleMove = (event: MouseEvent) => {
      const track = trackRef.current;
      if (!track) return;
      const rect = track.getBoundingClientRect();
      const ratio = Math.max(0, Math.min((event.clientX - rect.left) / rect.width, 1));
      const time = Number((ratio * effectiveDuration).toFixed(2));

      const seg = segments.find((item) => item.id === dragging.id);
      if (!seg) return;

      if (dragging.edge === "start") {
        updateSegment(dragging.id, { startTime: Math.min(time, seg.endTime - 0.05) });
      } else {
        updateSegment(dragging.id, { endTime: Math.max(time, seg.startTime + 0.05) });
      }
    };

    const handleUp = () => setDragging(null);

    window.addEventListener("mousemove", handleMove);
    window.addEventListener("mouseup", handleUp);
    return () => {
      window.removeEventListener("mousemove", handleMove);
      window.removeEventListener("mouseup", handleUp);
    };
  }, [dragging, effectiveDuration, segments, updateSegment]);


  if (loading || !form) {
    return (
      <div className="min-h-screen bg-[#e9edf1] p-8">
        <div className="mx-auto max-w-5xl rounded-[30px] bg-white p-10 text-center text-slate-500 shadow-sm">
          Loading lesson details...
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#e9edf1]">
      <div className="flex max-w-[1800px]">
        <Sidebar collapsed={isSidebarCollapsed} onToggle={() => setIsSidebarCollapsed((v) => !v)} />

        <main className="w-full p-4 md:p-8">
          <div className="grid gap-6 xl:grid-cols-[380px_1fr]">
            <section className="rounded-[30px] bg-[#f4f7fa] p-6 shadow-inner">
              <h1 className="text-3xl font-black tracking-tight text-slate-900">Lesson Details</h1>

              <div className="mt-6 space-y-4">
                <label className="block">
                  <span className="mb-2 block text-sm font-semibold text-slate-600">Lesson Title</span>
                  <Input value={form.title} onChange={(event) => updateForm("title", event.target.value)} />
                </label>

                <label className="block">
                  <span className="mb-2 block text-sm font-semibold text-slate-600">YouTube / Audio URL</span>
                  <Input value={form.source} onChange={(event) => updateForm("source", event.target.value)} />
                </label>

                <div className="grid grid-cols-2 gap-3">
                  <label className="rounded-2xl border border-slate-200 bg-white px-4 py-3">
                    <span className="text-xs font-semibold text-slate-500">Duration</span>
                    <p className="mt-1 text-lg font-semibold text-slate-800">{formatClock(safeDuration)}</p>
                  </label>

                  <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3">
                    <span className="text-xs font-semibold text-slate-500">Status</span>
                    <div className="mt-1 flex items-center gap-3">
                      <span className="text-sm font-semibold text-slate-700">
                        {form.status === LessonStatus.PUBLISHED ? "Published" : "Hidden"}
                      </span>
                      <Switch
                        checked={form.status === LessonStatus.PUBLISHED}
                        onChange={(next) => updateForm("status", next ? LessonStatus.PUBLISHED : LessonStatus.HIDDEN)}
                      />
                    </div>
                  </div>
                </div>

                <label className="block" ref={categoryBoxRef}>
                  <span className="mb-2 block text-sm font-semibold text-slate-600">Categories</span>

                  <Input
                    value={categoryInput}
                    onChange={(event) => {
                      setCategoryInput(event.target.value);
                      setIsCategoryDropdownOpen(true);
                    }}
                    onFocus={() => setIsCategoryDropdownOpen(true)}

                    onKeyDown={(event) => {
                      if (event.key === "Enter") {
                        event.preventDefault();
                        if (canCreateCategory) void createCategory();

                      }
                    }}
                    placeholder="Search or create category"
                  />

                  {form.categoryIds.length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-2">
                      {form.categoryIds.map((categoryId) => {
                        const categoryName = availableCategories.find((category) => category.id === categoryId)?.name ?? categoryId;
                        return (
                          <Badge key={categoryId} className="gap-1.5 bg-emerald-100 text-emerald-800">
                            {categoryName}
                            <button
                              type="button"
                              onClick={() => removeCategory(categoryId)}
                              className="inline-flex h-4 w-4 items-center justify-center rounded-full hover:bg-emerald-200"
                              aria-label={`Remove ${categoryName}`}
                            >
                              <X className="h-3 w-3" />
                            </button>
                          </Badge>
                        );
                      })}
                    </div>
                  )}

                  {isCategoryDropdownOpen && (filteredCategoryOptions.length > 0 || canCreateCategory || isCategoryLoading) && (

                    <div className="mt-2 rounded-xl border border-slate-200 bg-white p-2 shadow-sm">
                      {isCategoryLoading && (
                        <div className="px-3 py-2 text-sm text-slate-500">Loading categories...</div>
                      )}

                      {filteredCategoryOptions.map((option) => (
                        <button
                          key={option.id}
                          type="button"
                          onClick={() => {
                            addCategory(option);
                            setIsCategoryDropdownOpen(false);
                          }}
                          className="block w-full rounded-lg px-3 py-2 text-left text-sm text-slate-700 hover:bg-slate-100"
                        >
                          {option.name}
                        </button>
                      ))}
                      {canCreateCategory && (
                        <button
                          type="button"
                          onClick={() => void createCategory()}

                          className="block w-full rounded-lg px-3 py-2 text-left text-sm font-semibold text-emerald-700 hover:bg-emerald-50"
                        >
                          {isCreatingCategory ? "Creating..." : `Create "${categoryInput.trim()}"`}

                        </button>
                      )}
                    </div>
                  )}
                </label>
              </div>

              <div className="mt-10 space-y-3">
                {error && (
                  <div className="flex items-center justify-between rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
                    <span>{error}</span>
                    <button onClick={clearMessages} className="font-semibold">Dismiss</button>
                  </div>
                )}

                {success && (
                  <div className="flex items-center justify-between rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                    <span>{success}</span>
                    <button onClick={clearMessages} className="font-semibold">Dismiss</button>
                  </div>
                )}

                <Button className="w-full py-3 text-base" disabled={saving} onClick={saveChanges}>
                  {saving ? "Saving..." : "Save Lesson Info"}
                </Button>

                <Button
                  variant="secondary"
                  className="w-full border-rose-200 py-3 text-base text-rose-600 hover:bg-rose-50"
                  leftIcon={<Trash2 className="h-4 w-4" />}
                  disabled={deleting}
                  onClick={() => setShowDeleteConfirm(true)}
                >
                  {deleting ? "Deleting..." : "Delete Lesson"}
                </Button>
              </div>
            </section>

            <section className="space-y-4">
              <div className="rounded-[30px] border border-slate-200 bg-white p-5 shadow-sm">
                <div className="rounded-[24px] bg-slate-900 p-5 text-white">
                  <div className="mb-4 flex items-center justify-between gap-4">
                    <button
                      type="button"
                      onClick={() => {
                        if (finishedSegmentId && selectedSegmentId === finishedSegmentId) {
                          handleReplaySegment();
                          return;
                        }
                        setIsPlaying((prev) => !prev);
                      }}
                      className="inline-flex h-12 w-12 items-center justify-center rounded-full bg-emerald-500 transition hover:bg-emerald-400"
                    >
                      {isPlaying ? <Pause className="h-5 w-5" /> : finishedSegmentId && selectedSegmentId === finishedSegmentId ? <RotateCcw className="h-5 w-5" /> : <Play className="h-5 w-5" />}
                    </button>

                    <p className="text-sm font-semibold">
                      {formatClock(Math.max(0, playedSeconds - playbackStart))} / {formatClock(playbackDuration)}
                    </p>
                  </div>

                  <input
                    type="range"
                    min={playbackStart}
                    max={playbackEnd}
                    step={0.1}
                    value={Math.min(Math.max(playedSeconds, playbackStart), playbackEnd)}
                    onChange={(event) => handleSeek(Number(event.target.value))}
                    className="h-2 w-full cursor-pointer appearance-none rounded-full bg-white/30 accent-emerald-400"
                  />

                  <div className="mt-3 flex flex-wrap gap-2">
                    <button
                      type="button"
                      disabled={!selectedSegment}
                      onClick={() => setCurrentAsBoundary("start")}
                      className="rounded-lg bg-white/15 px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-40"
                    >
                      Set current as Start
                    </button>
                    <button
                      type="button"
                      disabled={!selectedSegment}
                      onClick={() => setCurrentAsBoundary("end")}
                      className="rounded-lg bg-white/15 px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-40"
                    >
                      Set current as End
                    </button>
                    <button
                      type="button"
                      disabled={!selectedSegment}
                      onClick={splitAtSelectedEnd}
                      className="rounded-lg bg-emerald-500/25 px-3 py-1.5 text-xs font-semibold text-emerald-100 disabled:opacity-40"
                    >
                      Split new segment from End
                    </button>
                  </div>

                  <div className="mt-4 flex items-center justify-between text-[11px] text-white/70">
                    <span>Segment track</span>
                    {selectedSegment ? <span>{selectedSegment.startTime.toFixed(2)}s - {selectedSegment.endTime.toFixed(2)}s</span> : <span>Click segment to edit</span>}
                  </div>

                  <div className="mt-2 flex items-center gap-2 text-xs">
                    {[1, 2, 4].map((zoom) => (
                      <button
                        key={zoom}
                        type="button"
                        onClick={() => setTimelineZoom(zoom)}
                        className={`rounded-md px-2 py-1 transition ${timelineZoom === zoom ? "bg-emerald-400 text-slate-900" : "bg-white/15 text-white hover:bg-white/25"}`}
                      >
                        {zoom}x
                      </button>
                    ))}
                    <span className="text-white/60">Zoom de de click segment ngan</span>
                  </div>

                  <div ref={trackScrollRef} className="mt-2 overflow-x-auto pb-1">
                    <div
                      ref={trackRef}
                      id="segment-track"
                      className="relative h-16 rounded-xl bg-white/15"
                      style={{ width: `${Math.max(timelineZoom * 100, 100)}%`, minWidth: "100%" }}
                    >
                      {timelineSegments.map((segment) => (
                        <div key={segment.id} className="absolute h-6" style={{ top: segment.lane === 0 ? "6px" : "34px", left: `${segment.left}%`, width: `${Math.max(segment.width, 0.8)}%` }}>


                          <button
                            type="button"
                            onClick={() => selectAndPlaySegment(segment.id)}
                            onMouseEnter={() => setHoveredSegmentId(segment.id)}
                            onMouseLeave={() => setHoveredSegmentId((current) => (current === segment.id ? null : current))}
                            className={`h-full w-full rounded-lg transition ${segment.id === selectedSegmentId ? "ring-2 ring-emerald-200/90" : ""} ${segment.id === activeSegmentId ? "bg-emerald-400" : "bg-white/70 hover:bg-white/90"}`}
                            title={segment.text}
                          />

                          <button
                            type="button"
                            className="absolute -left-1.5 top-0 z-10 h-6 w-3 cursor-ew-resize rounded bg-white/95"


                            onMouseDown={(event) => {
                              event.preventDefault();
                              event.stopPropagation();
                              setDragging({ id: segment.id, edge: "start" });
                            }}
                            aria-label="Drag segment start"
                          />
                          <button
                            type="button"
                            className="absolute -right-1.5 top-0 z-10 h-6 w-3 cursor-ew-resize rounded bg-white/95"


                            onMouseDown={(event) => {
                              event.preventDefault();
                              event.stopPropagation();
                              setDragging({ id: segment.id, edge: "end" });
                            }}
                            aria-label="Drag segment end"
                          />
                          {hoveredSegmentId === segment.id ? (
                            <div className="pointer-events-none absolute -top-6 left-0 rounded bg-black/80 px-1.5 py-0.5 text-[10px] text-white">
                              {segment.startTime.toFixed(2)}s - {segment.endTime.toFixed(2)}s
                            </div>
                          ) : null}
                        </div>
                      ))}
                    </div>
                  </div>

                </div>

                <div className="pointer-events-none h-0 w-0 opacity-0">
                  <ReactPlayer
                    ref={playerRef}
                    url={form.source}
                    playing={isPlaying}
                    controls={false}
                    width="0px"
                    height="0px"
                    onProgress={(state) => {
                      const current = state.playedSeconds;
                      setPlayedSeconds(current);
                      if (selectedSegment && current >= selectedSegment.endTime) {
                        setIsPlaying(false);
                        setFinishedSegmentId(selectedSegment.id);
                        playerRef.current?.seekTo(selectedSegment.endTime, "seconds");
                        setPlayedSeconds(selectedSegment.endTime);
                      }
                    }}
                    onEnded={() => setIsPlaying(false)}
                  />
                </div>
              </div>

              <SegmentEditor


                segments={segments}
                activeSegmentId={activeSegmentId}
                onSeek={selectAndPlaySegment}


                onAdd={addSegment}
                onDelete={removeSegment}
                onSplit={splitSegment}
                onChange={updateSegment}
                onSaveChanges={saveChanges}
                onNoChangesSaveAttempt={() => setSegmentToast("No segment changes to save.")}
                isSaving={saving}
                hasSegmentChanges={hasSegmentChanges}
              />


            </section>
          </div>
        </main>
      </div>

      {segmentToast ? <FloatingToast message={segmentToast} variant="warning" /> : null}

      <ConfirmationModal

        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={async () => {
          const deleted = await deleteLesson();
          setShowDeleteConfirm(false);
          if (deleted) router.push("/admin/lessons");
        }}
        title="Delete lesson?"
        message={`This will permanently delete "${form.title}".`}
        confirmText="Delete"
      />
    </div>
  );
}


