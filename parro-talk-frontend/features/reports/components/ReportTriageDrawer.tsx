"use client";

import React, { useEffect, useState, useId } from "react";
import {
  X,
  AlertCircle,
  Clock,
  User as UserIcon,
  CheckCircle2,
  History,
  Send,
  ExternalLink,
} from "lucide-react";
import {
  LessonReportResponse,
  AppFeedbackResponse,
  ModerationStatus,
  ModerationPriority,
  ModerationUpdateRequest,
  ModerationEventResponse,
} from "../types/report";
import { reportService } from "../services/reportService";
import Button from "@/components/ui/Button";
import Label from "@/components/ui/Label";
import Textarea from "@/components/ui/Textarea";
import Badge from "@/components/ui/Badge";

interface ReportTriageDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  report: LessonReportResponse | null;
  feedback: AppFeedbackResponse | null;
  onSuccess: () => void;
}

export const STATUS_COLORS: Record<ModerationStatus, { bg: string; text: string; border: string }> = {
  OPEN: { bg: "bg-blue-50 dark:bg-blue-950/40", text: "text-blue-700 dark:text-blue-300", border: "border-blue-200 dark:border-blue-800" },
  IN_REVIEW: { bg: "bg-amber-50 dark:bg-amber-950/40", text: "text-amber-700 dark:text-amber-300", border: "border-amber-200 dark:border-amber-800" },
  RESOLVED: { bg: "bg-emerald-50 dark:bg-emerald-950/40", text: "text-emerald-700 dark:text-emerald-300", border: "border-emerald-200 dark:border-emerald-800" },
  REJECTED: { bg: "bg-gray-100 dark:bg-gray-800", text: "text-gray-700 dark:text-gray-300", border: "border-gray-200 dark:border-gray-700" },
  DUPLICATE: { bg: "bg-purple-50 dark:bg-purple-950/40", text: "text-purple-700 dark:text-purple-300", border: "border-purple-200 dark:border-purple-800" },
};

export const PRIORITY_COLORS: Record<ModerationPriority, { bg: string; text: string }> = {
  LOW: { bg: "bg-gray-100 dark:bg-gray-800", text: "text-gray-600 dark:text-gray-400" },
  MEDIUM: { bg: "bg-blue-100 dark:bg-blue-950/60", text: "text-blue-700 dark:text-blue-300" },
  HIGH: { bg: "bg-orange-100 dark:bg-orange-950/60", text: "text-orange-700 dark:text-orange-300" },
  URGENT: { bg: "bg-red-100 dark:bg-red-950/60", text: "text-red-700 dark:text-red-300" },
};

export default function ReportTriageDrawer({
  isOpen,
  onClose,
  report,
  feedback,
  onSuccess,
}: ReportTriageDrawerProps) {
  const titleId = useId();
  const currentItem = report || feedback;
  const isLessonReport = !!report;

  const [status, setStatus] = useState<ModerationStatus>("OPEN");
  const [priority, setPriority] = useState<ModerationPriority>("MEDIUM");
  const [resolutionNote, setResolutionNote] = useState("");
  const [internalNote, setInternalNote] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [events, setEvents] = useState<ModerationEventResponse[]>([]);
  const [loadingEvents, setLoadingEvents] = useState(false);

  useEffect(() => {
    if (!isOpen || !currentItem) return;

    setStatus(currentItem.moderation.status || "OPEN");
    setPriority(currentItem.moderation.priority || "MEDIUM");
    setResolutionNote(currentItem.moderation.resolutionNote || "");
    setInternalNote("");
    setError("");

    // Fetch audit events
    const loadEvents = async () => {
      setLoadingEvents(true);
      try {
        const evts = isLessonReport
          ? await reportService.getLessonReportEvents(currentItem.id)
          : await reportService.getAppFeedbackEvents(currentItem.id);
        setEvents(evts || []);
      } catch (e) {
        console.error("Failed to load audit events", e);
        setEvents([]);
      } finally {
        setLoadingEvents(false);
      }
    };

    loadEvents();
  }, [isOpen, currentItem, isLessonReport]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && isOpen) {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !currentItem) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const updatePayload: ModerationUpdateRequest = {
        status,
        priority,
        resolutionNote: resolutionNote.trim() || undefined,
        note: internalNote.trim() || undefined,
      };

      if (isLessonReport) {
        await reportService.updateLessonReport(currentItem.id, updatePayload);
      } else {
        await reportService.updateAppFeedback(currentItem.id, updatePayload);
      }

      onSuccess();
      onClose();
    } catch (err: unknown) {
      let msg = "Failed to update moderation state.";
      if (err instanceof Error) msg = err.message;
      if (err && typeof err === "object" && "response" in err) {
        const res = (err as { response?: { data?: { message?: string } } }).response;
        if (res?.data?.message) msg = res.data.message;
      }
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const reporter = isLessonReport ? report.reporter : feedback?.submitter;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      className="fixed inset-0 z-50 flex items-center justify-end bg-black/50 backdrop-blur-xs transition-opacity"
    >
      <div className="h-full w-full max-w-xl bg-white dark:bg-gray-900 shadow-2xl flex flex-col border-l border-gray-200 dark:border-gray-800 animate-in slide-in-from-right duration-200">
        {/* Header */}
        <div className="p-5 border-b border-gray-200 dark:border-gray-800 flex items-center justify-between bg-gray-50/70 dark:bg-gray-800/50">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-emerald-600 dark:text-emerald-400">
                {isLessonReport ? "Lesson Report Triage" : "App Feedback Triage"}
              </span>
              <span
                className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                  PRIORITY_COLORS[currentItem.moderation.priority]?.bg || ""
                } ${PRIORITY_COLORS[currentItem.moderation.priority]?.text || ""}`}
              >
                {currentItem.moderation.priority}
              </span>
            </div>
            <h2 id={titleId} className="text-lg font-bold text-gray-900 dark:text-white mt-0.5 truncate max-w-md">
              {isLessonReport
                ? `Report: ${report.type} (${report.reason})`
                : feedback?.title || "App Feedback"}
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
            aria-label="Close drawer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {/* Metadata Cards */}
          <div className="rounded-xl border border-gray-200 dark:border-gray-800 p-4 bg-gray-50/40 dark:bg-gray-800/30 space-y-3">
            <div className="flex items-center justify-between text-xs text-gray-500 dark:text-gray-400">
              <span className="inline-flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5" />
                Submitted: {new Date(currentItem.createdAt).toLocaleString()}
              </span>
              <span className="inline-flex items-center gap-1.5">
                <UserIcon className="w-3.5 h-3.5" />
                {reporter?.fullName || reporter?.email || "Anonymous"}
              </span>
            </div>

            {isLessonReport && (
              <div className="pt-2 border-t border-gray-200 dark:border-gray-800 text-xs flex flex-wrap items-center gap-2">
                <span className="text-gray-500">Lesson ID:</span>
                <code className="bg-gray-100 dark:bg-gray-800 px-1.5 py-0.5 rounded text-gray-800 dark:text-gray-200 font-mono">
                  {report.lessonId}
                </code>
                {report.segmentId && (
                  <>
                    <span className="text-gray-400">•</span>
                    <span className="text-gray-500">Segment ID:</span>
                    <code className="bg-gray-100 dark:bg-gray-800 px-1.5 py-0.5 rounded text-gray-800 dark:text-gray-200 font-mono">
                      {report.segmentId}
                    </code>
                  </>
                )}
                <a
                  href={`/admin/lessons/${report.lessonId}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1 text-emerald-600 dark:text-emerald-400 hover:underline ml-auto font-medium"
                >
                  View Lesson <ExternalLink className="w-3 h-3" />
                </a>
              </div>
            )}

            {!isLessonReport && feedback && (
              <div className="pt-2 border-t border-gray-200 dark:border-gray-800 text-xs flex items-center gap-2">
                <span className="text-gray-500">Category:</span>
                <Badge variant="outline">{feedback.category}</Badge>
              </div>
            )}
          </div>

          {/* Description Block */}
          <div className="space-y-1.5">
            <Label>User Description</Label>
            <div className="p-4 rounded-xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 text-sm text-gray-800 dark:text-gray-200 whitespace-pre-wrap leading-relaxed shadow-xs">
              {currentItem.description || <span className="text-gray-400 italic">No description provided</span>}
            </div>
          </div>

          {/* Moderation Form */}
          <form id="triage-form" onSubmit={handleSubmit} className="space-y-4 pt-2">
            <h3 className="text-sm font-semibold text-gray-900 dark:text-white uppercase tracking-wider flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-500" /> Triage Decision
            </h3>

            {error && (
              <div className="p-3 rounded-xl bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                {error}
              </div>
            )}

            <div className="grid grid-cols-2 gap-3">
              {/* Status Select */}
              <div className="space-y-1.5">
                <Label htmlFor="mod-status">Status</Label>
                <select
                  id="mod-status"
                  value={status}
                  onChange={(e) => setStatus(e.target.value as ModerationStatus)}
                  className="w-full text-sm rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-950 p-2 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                >
                  <option value="OPEN">OPEN</option>
                  <option value="IN_REVIEW">IN_REVIEW</option>
                  <option value="RESOLVED">RESOLVED</option>
                  <option value="REJECTED">REJECTED</option>
                  <option value="DUPLICATE">DUPLICATE</option>
                </select>
              </div>

              {/* Priority Select */}
              <div className="space-y-1.5">
                <Label htmlFor="mod-priority">Priority</Label>
                <select
                  id="mod-priority"
                  value={priority}
                  onChange={(e) => setPriority(e.target.value as ModerationPriority)}
                  className="w-full text-sm rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-950 p-2 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                >
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                  <option value="URGENT">URGENT</option>
                </select>
              </div>
            </div>

            {/* Resolution Note */}
            <div className="space-y-1.5">
              <Label htmlFor="res-note">
                Resolution Note <span className="text-xs text-gray-400">(visible upon resolution)</span>
              </Label>
              <Textarea
                id="res-note"
                rows={2}
                placeholder="Details of fix or why this report was resolved/rejected..."
                value={resolutionNote}
                onChange={(e) => setResolutionNote(e.target.value)}
                maxLength={1000}
              />
            </div>

            {/* Internal Audit Note */}
            <div className="space-y-1.5">
              <Label htmlFor="audit-note">
                Internal Audit Note <span className="text-xs text-gray-400">(logged in history)</span>
              </Label>
              <Textarea
                id="audit-note"
                rows={2}
                placeholder="Internal comment for admin logs..."
                value={internalNote}
                onChange={(e) => setInternalNote(e.target.value)}
                maxLength={1000}
              />
            </div>
          </form>

          {/* Audit History Timeline */}
          <div className="pt-4 border-t border-gray-200 dark:border-gray-800 space-y-3">
            <h3 className="text-sm font-semibold text-gray-900 dark:text-white uppercase tracking-wider flex items-center gap-1.5">
              <History className="w-4 h-4 text-indigo-500" /> Moderation History
            </h3>

            {loadingEvents ? (
              <div className="text-xs text-gray-400 py-3 text-center">Loading audit log...</div>
            ) : events.length === 0 ? (
              <div className="text-xs text-gray-400 italic py-2">No previous moderation events recorded.</div>
            ) : (
              <div className="space-y-3 relative before:absolute before:left-2 before:top-2 before:bottom-2 before:w-0.5 before:bg-gray-200 dark:before:bg-gray-800 pl-6">
                {events.map((evt) => (
                  <div key={evt.id} className="relative text-xs space-y-1">
                    <span className="absolute -left-6 top-1 w-2.5 h-2.5 rounded-full bg-indigo-500 ring-4 ring-white dark:ring-gray-900" />
                    <div className="flex items-center justify-between text-gray-500 dark:text-gray-400">
                      <span className="font-medium text-gray-900 dark:text-white">
                        {evt.actor?.fullName || evt.actor?.email || "System"} changed {evt.field}
                      </span>
                      <span>{new Date(evt.createdAt).toLocaleString()}</span>
                    </div>
                    <div className="text-gray-600 dark:text-gray-300">
                      <span className="line-through text-gray-400">{evt.oldValue || "none"}</span>
                      {" → "}
                      <span className="font-semibold text-emerald-600 dark:text-emerald-400">
                        {evt.newValue || "none"}
                      </span>
                    </div>
                    {evt.note && (
                      <p className="bg-gray-50 dark:bg-gray-800 p-2 rounded-lg text-gray-700 dark:text-gray-300 mt-1 italic">
                        &ldquo;{evt.note}&rdquo;
                      </p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Footer Actions */}
        <div className="p-4 border-t border-gray-200 dark:border-gray-800 flex items-center justify-end gap-3 bg-gray-50/70 dark:bg-gray-800/50">
          <Button variant="outline" onClick={onClose} disabled={loading}>
            Cancel
          </Button>
          <Button
            type="submit"
            form="triage-form"
            variant="primary"
            disabled={loading}
            className="flex items-center gap-1.5"
          >
            <Send className="w-4 h-4" />
            {loading ? "Saving..." : "Apply Triage Update"}
          </Button>
        </div>
      </div>
    </div>
  );
}
