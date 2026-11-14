"use client";

import React, { useCallback, useEffect, useState, useMemo } from "react";
import {
  ShieldAlert,
  MessageSquare,
  FileWarning,
  Filter,
  RotateCcw,
  ChevronLeft,
  ChevronRight,
  Eye,
} from "lucide-react";
import {
  LessonReportResponse,
  AppFeedbackResponse,
  ModerationStatus,
  ModerationPriority,
  LessonReportType,
  FeedbackCategory,
} from "../types/report";
import { reportService } from "../services/reportService";
import ReportTriageDrawer, { STATUS_COLORS, PRIORITY_COLORS } from "./ReportTriageDrawer";
import Button from "@/components/ui/Button";
import FloatingToast from "@/components/ui/FloatingToast";

type TabMode = "LESSON_REPORTS" | "APP_FEEDBACK";

interface ToastState {
  message: string;
  variant: "success" | "error" | "info" | "warning";
}

export default function ReportsManagementPage() {
  const [tab, setTab] = useState<TabMode>("LESSON_REPORTS");

  // State for Lesson Reports
  const [lessonReports, setLessonReports] = useState<LessonReportResponse[]>([]);
  const [lrPage, setLrPage] = useState(0);
  const [lrTotalPages, setLrTotalPages] = useState(0);
  const [lrTotalElements, setLrTotalElements] = useState(0);

  // State for App Feedback
  const [feedbacks, setFeedbacks] = useState<AppFeedbackResponse[]>([]);
  const [fbPage, setFbPage] = useState(0);
  const [fbTotalPages, setFbTotalPages] = useState(0);
  const [fbTotalElements, setFbTotalElements] = useState(0);

  // Filters
  const [statusFilter, setStatusFilter] = useState<"ALL" | ModerationStatus>("ALL");
  const [priorityFilter, setPriorityFilter] = useState<"ALL" | ModerationPriority>("ALL");
  const [typeFilter, setTypeFilter] = useState<"ALL" | LessonReportType>("ALL");
  const [categoryFilter, setCategoryFilter] = useState<"ALL" | FeedbackCategory>("ALL");

  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState<ToastState | null>(null);

  // Selected item for drawer
  const [selectedReport, setSelectedReport] = useState<LessonReportResponse | null>(null);
  const [selectedFeedback, setSelectedFeedback] = useState<AppFeedbackResponse | null>(null);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);

  const fetchLessonReports = useCallback(async () => {
    setLoading(true);
    try {
      const response = await reportService.getLessonReports({
        status: statusFilter === "ALL" ? undefined : statusFilter,
        priority: priorityFilter === "ALL" ? undefined : priorityFilter,
        type: typeFilter === "ALL" ? undefined : typeFilter,
        page: lrPage,
        size: 10,
      });
      setLessonReports(response.content || []);
      setLrTotalPages(response.totalPages || 0);
      setLrTotalElements(response.totalElements || 0);
    } catch (err: unknown) {
      console.error("Failed to load lesson reports", err);
      setToast({
        message: "Failed to load lesson reports.",
        variant: "error",
      });
    } finally {
      setLoading(false);
    }
  }, [statusFilter, priorityFilter, typeFilter, lrPage]);

  const fetchAppFeedbacks = useCallback(async () => {
    setLoading(true);
    try {
      const response = await reportService.getAppFeedbacks({
        status: statusFilter === "ALL" ? undefined : statusFilter,
        priority: priorityFilter === "ALL" ? undefined : priorityFilter,
        category: categoryFilter === "ALL" ? undefined : categoryFilter,
        page: fbPage,
        size: 10,
      });
      setFeedbacks(response.content || []);
      setFbTotalPages(response.totalPages || 0);
      setFbTotalElements(response.totalElements || 0);
    } catch (err: unknown) {
      console.error("Failed to load app feedbacks", err);
      setToast({
        message: "Failed to load app feedbacks.",
        variant: "error",
      });
    } finally {
      setLoading(false);
    }
  }, [statusFilter, priorityFilter, categoryFilter, fbPage]);

  useEffect(() => {
    if (tab === "LESSON_REPORTS") {
      fetchLessonReports();
    } else {
      fetchAppFeedbacks();
    }
  }, [tab, fetchLessonReports, fetchAppFeedbacks]);

  // Reset pagination on filter or tab change
  const handleTabChange = (newTab: TabMode) => {
    setTab(newTab);
    setStatusFilter("ALL");
    setPriorityFilter("ALL");
    setTypeFilter("ALL");
    setCategoryFilter("ALL");
    setLrPage(0);
    setFbPage(0);
  };

  const handleRefresh = () => {
    if (tab === "LESSON_REPORTS") fetchLessonReports();
    else fetchAppFeedbacks();
  };

  const handleOpenTriage = (report?: LessonReportResponse, feedback?: AppFeedbackResponse) => {
    setSelectedReport(report || null);
    setSelectedFeedback(feedback || null);
    setIsDrawerOpen(true);
  };

  // Quick stats computed for active tab
  const activeStats = useMemo(() => {
    const list = tab === "LESSON_REPORTS" ? lessonReports : feedbacks;
    const total = tab === "LESSON_REPORTS" ? lrTotalElements : fbTotalElements;
    const openCount = list.filter((i) => i.moderation?.status === "OPEN").length;
    const inReviewCount = list.filter((i) => i.moderation?.status === "IN_REVIEW").length;
    const resolvedCount = list.filter((i) => i.moderation?.status === "RESOLVED").length;
    return { total, open: openCount, inReview: inReviewCount, resolved: resolvedCount };
  }, [tab, lessonReports, feedbacks, lrTotalElements, fbTotalElements]);

  return (
    <div className="space-y-6">
      {/* Toast */}
      {toast && (
        <FloatingToast
          message={toast.message}
          variant={toast.variant}
          onClose={() => setToast(null)}
        />
      )}

      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-800 dark:bg-rose-950/60 dark:text-rose-300">
              <ShieldAlert className="w-3.5 h-3.5" /> Moderation & Quality Control
            </span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-gray-900 dark:text-white mt-1">
            Reports & Feedback
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
            Triage user issue reports on lessons, audio segments, and application feedback.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <Button
            variant="outline"
            onClick={handleRefresh}
            disabled={loading}
            className="flex items-center gap-1.5"
            aria-label="Refresh reports list"
          >
            <RotateCcw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </Button>
        </div>
      </div>

      {/* Tab Switcher & KPIs */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-200 dark:border-gray-800 pb-4">
        {/* Segmented Tab */}
        <div className="inline-flex rounded-xl bg-gray-100 dark:bg-gray-800 p-1 text-sm font-medium">
          <button
            onClick={() => handleTabChange("LESSON_REPORTS")}
            className={`inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold transition-all ${
              tab === "LESSON_REPORTS"
                ? "bg-white dark:bg-gray-900 text-gray-900 dark:text-white shadow-xs"
                : "text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white"
            }`}
          >
            <FileWarning className="w-4 h-4 text-rose-500" />
            Lesson Reports
            <span className="ml-1 px-1.5 py-0.2 rounded-full text-xs bg-rose-100 text-rose-800 dark:bg-rose-950/80 dark:text-rose-300">
              {lrTotalElements}
            </span>
          </button>
          <button
            onClick={() => handleTabChange("APP_FEEDBACK")}
            className={`inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold transition-all ${
              tab === "APP_FEEDBACK"
                ? "bg-white dark:bg-gray-900 text-gray-900 dark:text-white shadow-xs"
                : "text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white"
            }`}
          >
            <MessageSquare className="w-4 h-4 text-indigo-500" />
            App Feedback
            <span className="ml-1 px-1.5 py-0.2 rounded-full text-xs bg-indigo-100 text-indigo-800 dark:bg-indigo-950/80 dark:text-indigo-300">
              {fbTotalElements}
            </span>
          </button>
        </div>

        {/* Quick Summary Pill */}
        <div className="flex items-center gap-2 text-xs font-medium text-gray-500 dark:text-gray-400">
          <span className="flex items-center gap-1">
            <span className="w-2 h-2 rounded-full bg-blue-500" />
            Open: <strong className="text-gray-900 dark:text-white">{activeStats.open}</strong>
          </span>
          <span className="text-gray-300 dark:text-gray-700">•</span>
          <span className="flex items-center gap-1">
            <span className="w-2 h-2 rounded-full bg-amber-500" />
            In Review: <strong className="text-gray-900 dark:text-white">{activeStats.inReview}</strong>
          </span>
          <span className="text-gray-300 dark:text-gray-700">•</span>
          <span className="flex items-center gap-1">
            <span className="w-2 h-2 rounded-full bg-emerald-500" />
            Resolved: <strong className="text-gray-900 dark:text-white">{activeStats.resolved}</strong>
          </span>
        </div>
      </div>

      {/* Main Content Card */}
      <div className="rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-sm overflow-hidden">
        {/* Filter Toolbar */}
        <div className="p-4 border-b border-gray-200 dark:border-gray-800 flex flex-wrap items-center justify-between gap-3 bg-gray-50/60 dark:bg-gray-800/40">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs font-medium text-gray-500 dark:text-gray-400 inline-flex items-center gap-1">
              <Filter className="w-3.5 h-3.5" /> Status:
            </span>
            {(["ALL", "OPEN", "IN_REVIEW", "RESOLVED", "REJECTED", "DUPLICATE"] as const).map((st) => (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                className={`px-2.5 py-1 text-xs font-semibold rounded-lg transition-colors ${
                  statusFilter === st
                    ? "bg-rose-600 text-white shadow-xs"
                    : "bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-700"
                }`}
              >
                {st}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-2">
            {/* Priority Filter */}
            <select
              value={priorityFilter}
              onChange={(e) => setPriorityFilter(e.target.value as "ALL" | ModerationPriority)}
              className="text-xs rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-950 px-2.5 py-1.5 text-gray-700 dark:text-gray-300 focus:outline-none focus:ring-1 focus:ring-rose-500"
              aria-label="Filter by priority"
            >
              <option value="ALL">All Priorities</option>
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="URGENT">URGENT</option>
            </select>

            {/* Type or Category Filter */}
            {tab === "LESSON_REPORTS" ? (
              <select
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value as "ALL" | LessonReportType)}
                className="text-xs rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-950 px-2.5 py-1.5 text-gray-700 dark:text-gray-300 focus:outline-none focus:ring-1 focus:ring-rose-500"
                aria-label="Filter by report type"
              >
                <option value="ALL">All Types</option>
                <option value="FILE">Audio/File</option>
                <option value="TRANSLATION">Translation</option>
              </select>
            ) : (
              <select
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value as "ALL" | FeedbackCategory)}
                className="text-xs rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-950 px-2.5 py-1.5 text-gray-700 dark:text-gray-300 focus:outline-none focus:ring-1 focus:ring-rose-500"
                aria-label="Filter by feedback category"
              >
                <option value="ALL">All Categories</option>
                <option value="BUG">Bug</option>
                <option value="FEATURE_REQUEST">Feature Request</option>
                <option value="UI_UX">UI / UX</option>
                <option value="PERFORMANCE">Performance</option>
                <option value="LOGIN">Login</option>
                <option value="PAYMENT">Payment</option>
                <option value="OTHER">Other</option>
              </select>
            )}
          </div>
        </div>

        {/* Table View */}
        <div className="overflow-x-auto" tabIndex={0} role="region" aria-label="Moderation Table">
          {tab === "LESSON_REPORTS" ? (
            <table className="w-full text-left text-sm text-gray-700 dark:text-gray-300">
              <caption className="sr-only">List of user lesson reports</caption>
              <thead className="bg-gray-50 dark:bg-gray-800/70 text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400 border-b border-gray-200 dark:border-gray-800">
                <tr>
                  <th scope="col" className="py-3.5 px-4">Status & Priority</th>
                  <th scope="col" className="py-3.5 px-4">Report Type / Reason</th>
                  <th scope="col" className="py-3.5 px-4">Description Snippet</th>
                  <th scope="col" className="py-3.5 px-4">Reporter</th>
                  <th scope="col" className="py-3.5 px-4">Date</th>
                  <th scope="col" className="py-3.5 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                {loading ? (
                  <tr>
                    <td colSpan={6} className="py-12 text-center text-gray-400">
                      Loading reports...
                    </td>
                  </tr>
                ) : lessonReports.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-12 text-center text-gray-400">
                      No lesson reports match the selected filters.
                    </td>
                  </tr>
                ) : (
                  lessonReports.map((item) => (
                    <tr
                      key={item.id}
                      className="hover:bg-gray-50/80 dark:hover:bg-gray-800/50 transition-colors"
                    >
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-1.5">
                          <span
                            className={`px-2 py-0.5 rounded-full text-xs font-semibold border ${
                              STATUS_COLORS[item.moderation.status]?.bg || ""
                            } ${STATUS_COLORS[item.moderation.status]?.text || ""} ${
                              STATUS_COLORS[item.moderation.status]?.border || ""
                            }`}
                          >
                            {item.moderation.status}
                          </span>
                          <span
                            className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                              PRIORITY_COLORS[item.moderation.priority]?.bg || ""
                            } ${PRIORITY_COLORS[item.moderation.priority]?.text || ""}`}
                          >
                            {item.moderation.priority}
                          </span>
                        </div>
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="font-semibold text-gray-900 dark:text-white text-xs">
                          {item.type}
                        </div>
                        <div className="text-xs text-gray-500 dark:text-gray-400">{item.reason}</div>
                      </td>
                      <td className="py-3.5 px-4 max-w-sm truncate text-xs text-gray-600 dark:text-gray-300">
                        {item.description || <span className="italic text-gray-400">No details</span>}
                      </td>
                      <td className="py-3.5 px-4 text-xs text-gray-600 dark:text-gray-300">
                        {item.reporter?.fullName || item.reporter?.email || "Anonymous"}
                      </td>
                      <td className="py-3.5 px-4 text-xs text-gray-500 whitespace-nowrap">
                        {new Date(item.createdAt).toLocaleDateString()}
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleOpenTriage(item, undefined)}
                          className="inline-flex items-center gap-1 text-xs"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          Triage
                        </Button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          ) : (
            <table className="w-full text-left text-sm text-gray-700 dark:text-gray-300">
              <caption className="sr-only">List of application feedback</caption>
              <thead className="bg-gray-50 dark:bg-gray-800/70 text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400 border-b border-gray-200 dark:border-gray-800">
                <tr>
                  <th scope="col" className="py-3.5 px-4">Status & Priority</th>
                  <th scope="col" className="py-3.5 px-4">Category & Title</th>
                  <th scope="col" className="py-3.5 px-4">Description Snippet</th>
                  <th scope="col" className="py-3.5 px-4">Submitter</th>
                  <th scope="col" className="py-3.5 px-4">Date</th>
                  <th scope="col" className="py-3.5 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                {loading ? (
                  <tr>
                    <td colSpan={6} className="py-12 text-center text-gray-400">
                      Loading feedbacks...
                    </td>
                  </tr>
                ) : feedbacks.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-12 text-center text-gray-400">
                      No app feedback matches the selected filters.
                    </td>
                  </tr>
                ) : (
                  feedbacks.map((item) => (
                    <tr
                      key={item.id}
                      className="hover:bg-gray-50/80 dark:hover:bg-gray-800/50 transition-colors"
                    >
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-1.5">
                          <span
                            className={`px-2 py-0.5 rounded-full text-xs font-semibold border ${
                              STATUS_COLORS[item.moderation.status]?.bg || ""
                            } ${STATUS_COLORS[item.moderation.status]?.text || ""} ${
                              STATUS_COLORS[item.moderation.status]?.border || ""
                            }`}
                          >
                            {item.moderation.status}
                          </span>
                          <span
                            className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                              PRIORITY_COLORS[item.moderation.priority]?.bg || ""
                            } ${PRIORITY_COLORS[item.moderation.priority]?.text || ""}`}
                          >
                            {item.moderation.priority}
                          </span>
                        </div>
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="font-semibold text-gray-900 dark:text-white text-xs">
                          {item.title}
                        </div>
                        <div className="text-xs text-gray-500 dark:text-gray-400">{item.category}</div>
                      </td>
                      <td className="py-3.5 px-4 max-w-sm truncate text-xs text-gray-600 dark:text-gray-300">
                        {item.description || <span className="italic text-gray-400">No details</span>}
                      </td>
                      <td className="py-3.5 px-4 text-xs text-gray-600 dark:text-gray-300">
                        {item.submitter?.fullName || item.submitter?.email || "Anonymous"}
                      </td>
                      <td className="py-3.5 px-4 text-xs text-gray-500 whitespace-nowrap">
                        {new Date(item.createdAt).toLocaleDateString()}
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleOpenTriage(undefined, item)}
                          className="inline-flex items-center gap-1 text-xs"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          Triage
                        </Button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}
        </div>

        {/* Pagination Footer */}
        {((tab === "LESSON_REPORTS" && lrTotalPages > 1) ||
          (tab === "APP_FEEDBACK" && fbTotalPages > 1)) && (
          <div className="p-4 border-t border-gray-200 dark:border-gray-800 flex flex-col sm:flex-row items-center justify-between gap-3 text-sm text-gray-500 dark:text-gray-400 bg-gray-50/50 dark:bg-gray-800/30">
            <div>
              Showing page{" "}
              <span className="font-medium text-gray-900 dark:text-white">
                {(tab === "LESSON_REPORTS" ? lrPage : fbPage) + 1}
              </span>{" "}
              of{" "}
              <span className="font-medium text-gray-900 dark:text-white">
                {tab === "LESSON_REPORTS" ? lrTotalPages : fbTotalPages}
              </span>{" "}
              ({tab === "LESSON_REPORTS" ? lrTotalElements : fbTotalElements} items)
            </div>

            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() =>
                  tab === "LESSON_REPORTS"
                    ? setLrPage((p) => Math.max(0, p - 1))
                    : setFbPage((p) => Math.max(0, p - 1))
                }
                disabled={
                  (tab === "LESSON_REPORTS" ? lrPage === 0 : fbPage === 0) || loading
                }
                className="flex items-center gap-1"
              >
                <ChevronLeft className="w-4 h-4" /> Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() =>
                  tab === "LESSON_REPORTS"
                    ? setLrPage((p) => Math.min(lrTotalPages - 1, p + 1))
                    : setFbPage((p) => Math.min(fbTotalPages - 1, p + 1))
                }
                disabled={
                  (tab === "LESSON_REPORTS"
                    ? lrPage >= lrTotalPages - 1
                    : fbPage >= fbTotalPages - 1) || loading
                }
                className="flex items-center gap-1"
              >
                Next <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Triage Drawer */}
      <ReportTriageDrawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        report={selectedReport}
        feedback={selectedFeedback}
        onSuccess={() => {
          setToast({
            message: "Triage update saved successfully.",
            variant: "success",
          });
          handleRefresh();
        }}
      />
    </div>
  );
}
