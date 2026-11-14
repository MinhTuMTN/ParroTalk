"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import {
  Users,
  BookOpen,
  FolderTree,
  ShieldAlert,
  ArrowRight,
  Plus,
  RotateCcw,
  Sparkles,
  ExternalLink,
  Layers,
  Tag as TagIcon,
  BookA,
  CheckCircle2,
  Clock,
} from "lucide-react";
import { dashboardService, AdminDashboardStats } from "../services/dashboardService";
import Button from "@/components/ui/Button";
import Badge from "@/components/ui/Badge";

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<AdminDashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const fetchStats = async () => {
    setLoading(true);
    setError("");
    try {
      const data = await dashboardService.getStats();
      setStats(data);
    } catch (err: unknown) {
      console.error("Failed to load admin stats", err);
      setError("Unable to load dashboard metrics. Please refresh.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStats();
  }, []);

  const totalPendingReports = (stats?.pendingLessonReports || 0) + (stats?.pendingFeedbacks || 0);

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
              <Sparkles className="w-3.5 h-3.5" /> ParroTalk Platform
            </span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-gray-900 dark:text-white mt-1">
            Admin Overview
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
            Real-time analytics, content health, and pending moderation workflows.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <Button
            variant="outline"
            onClick={fetchStats}
            disabled={loading}
            className="flex items-center gap-1.5"
            aria-label="Refresh dashboard metrics"
          >
            <RotateCcw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </Button>

          <Link href="/admin/lessons">
            <Button variant="primary" className="flex items-center gap-1.5 shadow-sm">
              <Plus className="w-4 h-4" /> Create Lesson
            </Button>
          </Link>
        </div>
      </div>

      {/* Error alert */}
      {error && (
        <div className="p-4 rounded-xl bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400 text-sm">
          {error}
        </div>
      )}

      {/* Pending Moderation Attention Alert */}
      {!loading && totalPendingReports > 0 && (
        <div className="rounded-2xl border border-rose-200 dark:border-rose-900 bg-linear-to-r from-rose-50 to-orange-50 dark:from-rose-950/30 dark:to-orange-950/30 p-5 shadow-xs">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-start gap-3.5">
              <div className="p-2.5 rounded-xl bg-rose-500/10 text-rose-600 dark:text-rose-400">
                <ShieldAlert className="w-6 h-6" />
              </div>
              <div>
                <h2 className="text-base font-bold text-gray-900 dark:text-white">
                  Attention Required: {totalPendingReports} Open Item{totalPendingReports > 1 ? "s" : ""}
                </h2>
                <p className="text-xs text-gray-600 dark:text-gray-300 mt-0.5">
                  You have{" "}
                  <strong>{stats?.pendingLessonReports || 0} lesson issue reports</strong> and{" "}
                  <strong>{stats?.pendingFeedbacks || 0} app feedbacks</strong> awaiting triage.
                </p>
              </div>
            </div>
            <Link href="/admin/reports">
              <Button
                variant="primary"
                size="sm"
                className="bg-rose-600 hover:bg-rose-700 text-white shrink-0 flex items-center gap-1.5"
              >
                Review Reports <ArrowRight className="w-4 h-4" />
              </Button>
            </Link>
          </div>
        </div>
      )}

      {/* Primary KPI Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Users Card */}
        <div className="p-5 rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-xs relative overflow-hidden group">
          <div className="flex items-center justify-between text-gray-500 dark:text-gray-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Registered Users</span>
            <div className="p-2 rounded-xl bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400">
              <Users className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 text-3xl font-extrabold text-gray-900 dark:text-white">
            {loading ? "..." : stats?.totalUsers.toLocaleString() ?? 0}
          </div>
          <div className="mt-2 flex items-center gap-1.5 text-xs text-gray-500 dark:text-gray-400">
            <span className="inline-block w-2 h-2 rounded-full bg-emerald-500" />
            <span>{stats?.activeUsers ?? 0} Active Learners</span>
          </div>
        </div>

        {/* Lessons Card */}
        <div className="p-5 rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-xs relative overflow-hidden group">
          <div className="flex items-center justify-between text-gray-500 dark:text-gray-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Dictation Lessons</span>
            <div className="p-2 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400">
              <BookOpen className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 text-3xl font-extrabold text-gray-900 dark:text-white">
            {loading ? "..." : stats?.totalLessons.toLocaleString() ?? 0}
          </div>
          <div className="mt-2 flex items-center gap-1.5 text-xs text-gray-500 dark:text-gray-400">
            <span className="font-semibold text-emerald-600 dark:text-emerald-400">
              {stats?.publishedLessons ?? 0} Published
            </span>
            <span>•</span>
            <span>{stats?.hiddenLessons ?? 0} Hidden</span>
          </div>
        </div>

        {/* Taxonomies & Content */}
        <div className="p-5 rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-xs relative overflow-hidden group">
          <div className="flex items-center justify-between text-gray-500 dark:text-gray-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Categories & Tags</span>
            <div className="p-2 rounded-xl bg-purple-50 dark:bg-purple-950/40 text-purple-600 dark:text-purple-400">
              <FolderTree className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 text-3xl font-extrabold text-gray-900 dark:text-white">
            {loading ? "..." : (stats ? stats.totalCategories + stats.totalTags : 0)}
          </div>
          <div className="mt-2 flex items-center gap-1.5 text-xs text-gray-500 dark:text-gray-400">
            <span>{stats?.totalCategories ?? 0} Categories</span>
            <span>•</span>
            <span>{stats?.totalTags ?? 0} Tags</span>
          </div>
        </div>

        {/* Vocabulary & Moderation */}
        <div className="p-5 rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-xs relative overflow-hidden group">
          <div className="flex items-center justify-between text-gray-500 dark:text-gray-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Vocabulary Terms</span>
            <div className="p-2 rounded-xl bg-amber-50 dark:bg-amber-950/40 text-amber-600 dark:text-amber-400">
              <BookA className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 text-3xl font-extrabold text-gray-900 dark:text-white">
            {loading ? "..." : stats?.totalVocabularies.toLocaleString() ?? 0}
          </div>
          <div className="mt-2 flex items-center gap-1.5 text-xs text-gray-500 dark:text-gray-400">
            <span className="text-amber-600 dark:text-amber-400 font-medium">Global Dictionary</span>
          </div>
        </div>
      </div>

      {/* Quick Launch Command Center */}
      <div className="rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 p-5 shadow-xs">
        <h2 className="text-sm font-bold text-gray-900 dark:text-white uppercase tracking-wider mb-4 flex items-center gap-2">
          <Layers className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
          Administrative Tools
        </h2>

        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
          <Link
            href="/admin/lessons"
            className="flex flex-col items-center justify-center p-3.5 rounded-xl border border-gray-100 dark:border-gray-800 hover:border-emerald-300 dark:hover:border-emerald-700 bg-gray-50/50 dark:bg-gray-800/30 hover:bg-emerald-50/40 dark:hover:bg-emerald-950/20 transition-all text-center group"
          >
            <BookOpen className="w-5 h-5 text-emerald-600 dark:text-emerald-400 mb-1.5 group-hover:scale-110 transition-transform" />
            <span className="text-xs font-semibold text-gray-900 dark:text-white">Lessons</span>
            <span className="text-[10px] text-gray-500">Create & manage</span>
          </Link>

          <Link
            href="/admin/categories"
            className="flex flex-col items-center justify-center p-3.5 rounded-xl border border-gray-100 dark:border-gray-800 hover:border-emerald-300 dark:hover:border-emerald-700 bg-gray-50/50 dark:bg-gray-800/30 hover:bg-emerald-50/40 dark:hover:bg-emerald-950/20 transition-all text-center group"
          >
            <FolderTree className="w-5 h-5 text-indigo-600 dark:text-indigo-400 mb-1.5 group-hover:scale-110 transition-transform" />
            <span className="text-xs font-semibold text-gray-900 dark:text-white">Categories</span>
            <span className="text-[10px] text-gray-500">Tree taxonomy</span>
          </Link>

          <Link
            href="/admin/tags"
            className="flex flex-col items-center justify-center p-3.5 rounded-xl border border-gray-100 dark:border-gray-800 hover:border-emerald-300 dark:hover:border-emerald-700 bg-gray-50/50 dark:bg-gray-800/30 hover:bg-emerald-50/40 dark:hover:bg-emerald-950/20 transition-all text-center group"
          >
            <TagIcon className="w-5 h-5 text-purple-600 dark:text-purple-400 mb-1.5 group-hover:scale-110 transition-transform" />
            <span className="text-xs font-semibold text-gray-900 dark:text-white">Tags</span>
            <span className="text-[10px] text-gray-500">Color labels</span>
          </Link>

          <Link
            href="/admin/reports"
            className="flex flex-col items-center justify-center p-3.5 rounded-xl border border-gray-100 dark:border-gray-800 hover:border-rose-300 dark:hover:border-rose-700 bg-gray-50/50 dark:bg-gray-800/30 hover:bg-rose-50/40 dark:hover:bg-rose-950/20 transition-all text-center group relative"
          >
            {totalPendingReports > 0 && (
              <span className="absolute top-2 right-2 w-2 h-2 rounded-full bg-rose-500 animate-pulse" />
            )}
            <ShieldAlert className="w-5 h-5 text-rose-600 dark:text-rose-400 mb-1.5 group-hover:scale-110 transition-transform" />
            <span className="text-xs font-semibold text-gray-900 dark:text-white">Reports</span>
            <span className="text-[10px] text-rose-500 font-medium">
              {totalPendingReports > 0 ? `${totalPendingReports} open` : "Clean queue"}
            </span>
          </Link>

          <Link
            href="/admin/users"
            className="flex flex-col items-center justify-center p-3.5 rounded-xl border border-gray-100 dark:border-gray-800 hover:border-emerald-300 dark:hover:border-emerald-700 bg-gray-50/50 dark:bg-gray-800/30 hover:bg-emerald-50/40 dark:hover:bg-emerald-950/20 transition-all text-center group"
          >
            <Users className="w-5 h-5 text-blue-600 dark:text-blue-400 mb-1.5 group-hover:scale-110 transition-transform" />
            <span className="text-xs font-semibold text-gray-900 dark:text-white">Users</span>
            <span className="text-[10px] text-gray-500">Roles & accounts</span>
          </Link>

          <Link
            href="/admin/vocabularies"
            className="flex flex-col items-center justify-center p-3.5 rounded-xl border border-gray-100 dark:border-gray-800 hover:border-emerald-300 dark:hover:border-emerald-700 bg-gray-50/50 dark:bg-gray-800/30 hover:bg-emerald-50/40 dark:hover:bg-emerald-950/20 transition-all text-center group"
          >
            <BookA className="w-5 h-5 text-amber-600 dark:text-amber-400 mb-1.5 group-hover:scale-110 transition-transform" />
            <span className="text-xs font-semibold text-gray-900 dark:text-white">Dictionary</span>
            <span className="text-[10px] text-gray-500">Vocabulary senses</span>
          </Link>
        </div>
      </div>

      {/* Recent Content Activity & Status Distribution */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent Lessons (2 Cols) */}
        <div className="lg:col-span-2 rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-xs overflow-hidden">
          <div className="p-4 border-b border-gray-200 dark:border-gray-800 flex items-center justify-between bg-gray-50/60 dark:bg-gray-800/40">
            <h2 className="text-sm font-bold text-gray-900 dark:text-white flex items-center gap-2">
              <Clock className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
              Recently Created Lessons
            </h2>
            <Link
              href="/admin/lessons"
              className="text-xs font-semibold text-emerald-600 dark:text-emerald-400 hover:underline flex items-center gap-1"
            >
              View all <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="overflow-x-auto" tabIndex={0} role="region" aria-label="Recent Lessons">
            <table className="w-full text-left text-sm text-gray-700 dark:text-gray-300">
              <caption className="sr-only">Recent dictation lessons</caption>
              <thead className="bg-gray-50 dark:bg-gray-800/70 text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400 border-b border-gray-200 dark:border-gray-800">
                <tr>
                  <th scope="col" className="py-3 px-4">Lesson</th>
                  <th scope="col" className="py-3 px-4 text-center">Segments</th>
                  <th scope="col" className="py-3 px-4 text-center">Status</th>
                  <th scope="col" className="py-3 px-4">Date</th>
                  <th scope="col" className="py-3 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-800 text-xs">
                {loading ? (
                  <tr>
                    <td colSpan={5} className="py-8 text-center text-gray-400">
                      Loading recent lessons...
                    </td>
                  </tr>
                ) : !stats?.recentLessons?.length ? (
                  <tr>
                    <td colSpan={5} className="py-8 text-center text-gray-400">
                      No lessons found.
                    </td>
                  </tr>
                ) : (
                  stats.recentLessons.map((item) => (
                    <tr
                      key={item.id}
                      className="hover:bg-gray-50/80 dark:hover:bg-gray-800/50 transition-colors"
                    >
                      <td className="py-3 px-4 font-semibold text-gray-900 dark:text-white max-w-xs truncate">
                        {item.title}
                      </td>
                      <td className="py-3 px-4 text-center text-gray-600 dark:text-gray-300 font-medium">
                        {item.totalSegments}
                      </td>
                      <td className="py-3 px-4 text-center">
                        <Badge variant={item.status === "PUBLISHED" ? "success" : "default"}>
                          {item.status}
                        </Badge>
                      </td>
                      <td className="py-3 px-4 text-gray-500 whitespace-nowrap">
                        {new Date(item.createdAt).toLocaleDateString()}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <Link
                          href={`/admin/lessons/${item.id}`}
                          className="text-emerald-600 dark:text-emerald-400 hover:underline font-medium inline-flex items-center gap-1"
                        >
                          Edit <ExternalLink className="w-3 h-3" />
                        </Link>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Content Pipeline Health Card (1 Col) */}
        <div className="rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 p-5 shadow-xs flex flex-col justify-between">
          <div>
            <h2 className="text-sm font-bold text-gray-900 dark:text-white flex items-center gap-2 mb-4">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
              Content Pipeline
            </h2>

            {/* Published vs Hidden ratio */}
            <div className="space-y-2 mb-6">
              <div className="flex justify-between text-xs font-semibold text-gray-700 dark:text-gray-300">
                <span>Published Lessons</span>
                <span>
                  {stats && stats.totalLessons > 0
                    ? Math.round((stats.publishedLessons / stats.totalLessons) * 100)
                    : 0}
                  %
                </span>
              </div>
              <div className="w-full bg-gray-100 dark:bg-gray-800 h-2.5 rounded-full overflow-hidden flex">
                <div
                  className="bg-emerald-500 h-full rounded-full transition-all duration-500"
                  style={{
                    width: `${
                      stats && stats.totalLessons > 0
                        ? (stats.publishedLessons / stats.totalLessons) * 100
                        : 0
                    }%`,
                  }}
                />
              </div>
              <div className="flex justify-between text-[11px] text-gray-500">
                <span>{stats?.publishedLessons ?? 0} published</span>
                <span>{stats?.hiddenLessons ?? 0} draft / hidden</span>
              </div>
            </div>

            {/* Platform summary stats */}
            <div className="space-y-3 border-t border-gray-100 dark:border-gray-800 pt-4 text-xs text-gray-600 dark:text-gray-300">
              <div className="flex items-center justify-between">
                <span>Lesson Categories</span>
                <strong className="text-gray-900 dark:text-white">{stats?.totalCategories ?? 0}</strong>
              </div>
              <div className="flex items-center justify-between">
                <span>Content Tags</span>
                <strong className="text-gray-900 dark:text-white">{stats?.totalTags ?? 0}</strong>
              </div>
              <div className="flex items-center justify-between">
                <span>Vocabulary Words</span>
                <strong className="text-gray-900 dark:text-white">
                  {stats?.totalVocabularies.toLocaleString() ?? 0}
                </strong>
              </div>
              <div className="flex items-center justify-between">
                <span>Pending Moderation Queue</span>
                <strong className={totalPendingReports > 0 ? "text-rose-600 dark:text-rose-400" : "text-emerald-600"}>
                  {totalPendingReports}
                </strong>
              </div>
            </div>
          </div>

          <div className="pt-5 border-t border-gray-100 dark:border-gray-800 mt-4">
            <Link href="/admin/lessons">
              <Button variant="outline" size="sm" className="w-full justify-center text-xs">
                Manage All Lessons
              </Button>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
