"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Pause, Play, Trash2 } from "lucide-react";
import ReactPlayer from "react-player";
import Sidebar from "@/src/components/layout/Sidebar";
import Header from "@/src/components/layout/Header";
import Button from "@/src/components/ui/Button";
import Input from "@/src/components/ui/Input";
import Switch from "@/src/components/ui/Switch";
import SegmentEditor from "@/src/components/lesson/SegmentEditor";
import { useLessonDetail } from "@/src/hooks/useLessonDetail";
import ConfirmationModal from "@/components/ui/ConfirmationModal";

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
  const [searchValue, setSearchValue] = useState("");

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
    saveChanges,
    deleteLesson,
    clearMessages,
  } = useLessonDetail(id);

  const safeDuration = form?.duration ?? lesson?.duration ?? 1;

  const handleSeek = (time: number) => {
    const nextTime = Math.max(0, Math.min(time, safeDuration));
    playerRef.current?.seekTo(nextTime, "seconds");
    setPlayedSeconds(nextTime);
  };

  const activeSegmentId = useMemo(() => {
    const active = segments.find(
      (segment) => playedSeconds >= segment.startTime && playedSeconds <= segment.endTime,
    );
    return active?.id ?? null;
  }, [segments, playedSeconds]);

  useEffect(() => {
    if (!activeSegmentId) return;
    const target = document.querySelector(`[data-segment-id="${activeSegmentId}"]`);
    if (!target) return;
    target.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }, [activeSegmentId]);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const targetTag = (event.target as HTMLElement).tagName;
      const isTyping = ["INPUT", "TEXTAREA"].includes(targetTag);

      if (event.code === "Space" && !isTyping) {
        event.preventDefault();
        setIsPlaying((prev) => !prev);
      }

      if (event.key === "Enter" && !isTyping && activeSegmentId) {
        event.preventDefault();
        splitSegment(activeSegmentId);
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [activeSegmentId, splitSegment]);

  const timelineSegments = useMemo(() => {
    const duration = safeDuration || 1;
    return segments.map((segment) => ({
      ...segment,
      left: `${(segment.startTime / duration) * 100}%`,
      width: `${Math.max(2, ((segment.endTime - segment.startTime) / duration) * 100)}%`,
    }));
  }, [segments, safeDuration]);

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
      <div className="mx-auto flex max-w-[1800px]">
        <Sidebar
          collapsed={isSidebarCollapsed}
          onToggle={() => setIsSidebarCollapsed((v) => !v)}
        />

        <main className="w-full p-4 md:p-8">
          <Header
            search={searchValue}
            onSearchChange={setSearchValue}
            onMenuClick={() => setIsSidebarCollapsed(false)}
          />

          <div className="mt-6 grid gap-6 xl:grid-cols-[380px_1fr]">
            <section className="rounded-[30px] bg-[#f4f7fa] p-6 shadow-inner">
              <h1 className="text-3xl font-black tracking-tight text-slate-900">Lesson Details</h1>

              <div className="mt-6 space-y-4">
                <label className="block">
                  <span className="mb-2 block text-sm font-semibold text-slate-600">Lesson Title</span>
                  <Input
                    value={form.title}
                    onChange={(event) => updateForm("title", event.target.value)}
                  />
                </label>

                <label className="block">
                  <span className="mb-2 block text-sm font-semibold text-slate-600">
                    YouTube / Audio URL
                  </span>
                  <Input
                    value={form.source}
                    onChange={(event) => updateForm("source", event.target.value)}
                  />
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
                        {form.status === "published" ? "Published" : "Hidden"}
                      </span>
                      <Switch
                        checked={form.status === "published"}
                        onChange={(next) =>
                          updateForm("status", next ? "published" : "hidden")
                        }
                      />
                    </div>
                  </div>
                </div>
              </div>

              <div className="mt-10 space-y-3">
                {error && (
                  <div className="flex items-center justify-between rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
                    <span>{error}</span>
                    <button onClick={clearMessages} className="font-semibold">
                      Dismiss
                    </button>
                  </div>
                )}

                {success && (
                  <div className="flex items-center justify-between rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                    <span>{success}</span>
                    <button onClick={clearMessages} className="font-semibold">
                      Dismiss
                    </button>
                  </div>
                )}

                <Button
                  className="w-full py-3 text-base"
                  disabled={saving}
                  onClick={saveChanges}
                >
                  {saving ? "Saving..." : "Save Changes"}
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
              <div className="rounded-[30px] bg-white p-5 shadow-sm">
                <div className="relative overflow-hidden rounded-[24px] bg-gradient-to-r from-slate-900 to-slate-700 p-5 text-white">
                  <div className="absolute -right-10 -top-10 h-40 w-40 rounded-full bg-emerald-500/20 blur-2xl" />
                  <div className="relative z-10">
                    <div className="mb-3 flex items-center justify-between gap-4">
                      <button
                        type="button"
                        onClick={() => setIsPlaying((prev) => !prev)}
                        className="inline-flex h-12 w-12 items-center justify-center rounded-full bg-emerald-500 text-white shadow-[0_10px_24px_rgba(16,185,129,0.4)] transition hover:bg-emerald-400"
                      >
                        {isPlaying ? <Pause className="h-5 w-5" /> : <Play className="h-5 w-5" />}
                      </button>

                      <p className="text-sm font-semibold">
                        {formatClock(playedSeconds)} / {formatClock(safeDuration)}
                      </p>
                    </div>

                    <input
                      type="range"
                      min={0}
                      max={safeDuration}
                      step={0.1}
                      value={Math.min(playedSeconds, safeDuration)}
                      onChange={(event) => handleSeek(Number(event.target.value))}
                      className="h-2 w-full cursor-pointer appearance-none rounded-full bg-white/30"
                    />

                    <div className="relative mt-4 h-6 rounded-full bg-white/15">
                      {timelineSegments.map((segment) => (
                        <button
                          key={segment.id}
                          type="button"
                          onClick={() => handleSeek(segment.startTime)}
                          className={`absolute top-1 h-4 rounded-full transition ${
                            segment.id === activeSegmentId ? "bg-emerald-400" : "bg-white/70"
                          }`}
                          style={{ left: segment.left, width: segment.width }}
                          title={segment.text}
                        />
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
                    onProgress={(state) => setPlayedSeconds(state.playedSeconds)}
                    onEnded={() => setIsPlaying(false)}
                  />
                </div>
              </div>

              <SegmentEditor
                segments={segments}
                activeSegmentId={activeSegmentId}
                onSeek={(time) => {
                  handleSeek(time);
                  setIsPlaying(true);
                }}
                onAdd={addSegment}
                onDelete={removeSegment}
                onSplit={splitSegment}
                onChange={updateSegment}
              />
            </section>
          </div>
        </main>
      </div>

      <ConfirmationModal
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={async () => {
          const deleted = await deleteLesson();
          setShowDeleteConfirm(false);
          if (deleted) {
            router.push("/admin/lessons");
          }
        }}
        title="Delete lesson?"
        message={`This will permanently delete "${form.title}".`}
        confirmText="Delete"
      />
    </div>
  );
}
