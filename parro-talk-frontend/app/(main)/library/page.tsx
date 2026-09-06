"use client";

import React, {
  useState,
  useEffect,
  useCallback,
  useMemo,
  useRef,
  useTransition,
  Suspense,
} from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import {
  Menu,
  AlertCircle,
  RotateCcw,
  Flame,
} from "lucide-react";

import { useAuth } from "@/features/auth/hooks/useAuth";
import { useUI } from "@/hooks/useUI";
import {
  lessonService,
  Lesson,
  Category,
  LibrarySectionDTO,
  PageResponse,
} from "@/features/lesson/services/lessonService";
import { streakService } from "@/features/streak/services/streakService";
import type { UserStreakResponse } from "@/features/streak/types/streak";
import ConfirmationModal from "@/components/ui/ConfirmationModal";
import UserMenu from "@/components/common/UserMenu";

import {
  LibraryHero,
  StreakBanner,
  ContinueLearningSection,
  UploadedLessonsSection,
  CuratedShelfSection,
  ProcessingFab,
  ProcessingModal,
  LibrarySkeleton,
  LibraryEmptyState,
  type ContinueLearningItem,
  type UploadedLessonItem,
  type TopicLessonItem,
  type StreakInfo,
  type CategoryChipItem,
  type ProcessingJobItem,
  type TopicGradientTheme,
} from "@/components/library";

const THEME_PALETTE: TopicGradientTheme[] = [
  "emerald",
  "indigo",
  "coral",
  "amber",
  "blue",
  "purple",
];

function getGradientTheme(nameOrSlug?: string | null, index = 0): TopicGradientTheme {
  const lower = (nameOrSlug || "").toLowerCase();
  if (lower.includes("work") || lower.includes("career") || lower.includes("công việc")) {
    return "emerald";
  }
  if (lower.includes("tech") || lower.includes("công nghệ") || lower.includes("ai")) {
    return "indigo";
  }
  if (
    lower.includes("health") ||
    lower.includes("lifestyle") ||
    lower.includes("sức khỏe") ||
    lower.includes("lối sống")
  ) {
    return "coral";
  }
  if (lower.includes("daily") || lower.includes("hàng ngày") || lower.includes("cuộc sống")) {
    return "amber";
  }
  if (lower.includes("ted") || lower.includes("talk")) {
    return "purple";
  }
  if (lower.includes("6 min") || lower.includes("bbc") || lower.includes("podcast")) {
    return "emerald";
  }
  return THEME_PALETTE[index % THEME_PALETTE.length];
}

function getSourceFromTitle(title?: string | null): string {
  const lower = (title || "").toLowerCase();
  if (lower.includes("bbc") || lower.includes("6 min") || lower.includes("minute")) return "BBC";
  if (lower.includes("ted")) return "TED";
  if (lower.includes("voa")) return "VOA";
  return "ParroTalk";
}

// Discriminated union describing exactly what each shelf needs to render.
// Built once per `sections`/`isProOrAdmin` change via useMemo (see renderableSections
// below) instead of being recomputed inline in JSX on every render.
type RenderableSection =
  | { kind: "IN_PROGRESS"; key: string; title: string; items: ContinueLearningItem[] }
  | {
      kind: "USER_UPLOADED";
      key: string;
      title: string;
      seeAllHref: string;
      items: UploadedLessonItem[];
    }
  | {
      kind: "SHELF";
      key: string;
      shelfId: string;
      title: string;
      theme: TopicGradientTheme;
      seeAllHref?: string;
      items: TopicLessonItem[];
    };

// EARLY PREFETCH: Tối ưu ưu tiên tuyệt đối cho API library.
// Bắt đầu gọi API ngay khi trình duyệt vừa parse xong file JS, không cần chờ React hydration hay useEffect.
// Tránh được tình trạng kẹt xe (waterfall) do Next.js rsc prefetching.
let earlyLibraryPromise: Promise<any> | null = null;
let earlyFetchTime = 0;
if (typeof window !== "undefined") {
  const token = localStorage.getItem("token");
  const urlParams = new URLSearchParams(window.location.search);
  const isMyLessons = urlParams.get("tab") === "my-lessons";
  if (token && !isMyLessons) {
    if (process.env.NODE_ENV === "development") performance.mark("library-request-start");
    earlyFetchTime = Date.now();
    earlyLibraryPromise = lessonService.getLibrarySections().then(res => {
      if (process.env.NODE_ENV === "development") {
        performance.mark("library-response-received");
        performance.measure("library-fetch-duration", "library-request-start", "library-response-received");
      }
      return res;
    }).catch(e => {
      console.error("Early prefetch error:", e);
      return null;
    });
  }
}

function LibraryContent() {
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const { openMobileMenu } = useUI();
  const router = useRouter();
  const searchParams = useSearchParams();

  const initialTab = searchParams.get("tab") === "my-lessons" ? "my-lessons" : "library";
  const [activeTab, setActiveTab] = useState<"library" | "my-lessons">(initialTab);
  // Defers the (potentially heavy) content swap so the tab button itself still
  // responds instantly to the click; see handleTabChange below.
  const [isTabPending, startTabTransition] = useTransition();

  // Dynamic Data States
  const [sections, setSections] = useState<LibrarySectionDTO[]>([]);
  const [myLessons, setMyLessons] = useState<Lesson[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [streakData, setStreakData] = useState<UserStreakResponse | null>(null);

  // UI States
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [hasFetchError, setHasFetchError] = useState<boolean>(false);
  const [isProcessingModalOpen, setIsProcessingModalOpen] = useState<boolean>(false);
  const [processingJobs, setProcessingJobs] = useState<ProcessingJobItem[]>([]);

  // Per-tab response cache (stale-while-revalidate) + a monotonically increasing
  // request id so a slow/late response can never clobber a newer one — this is what
  // lets tab switches skip the skeleton and survive rapid back-and-forth clicking.
  const cacheRef = useRef<{ library?: LibrarySectionDTO[]; myLessons?: Lesson[] }>({});
  const requestIdRef = useRef(0);

  // Reset Progress Modal State
  const [resetModal, setResetModal] = useState<{
    isOpen: boolean;
    lessonId: string;
    lessonTitle: string;
    isResetting: boolean;
  }>({
    isOpen: false,
    lessonId: "",
    lessonTitle: "",
    isResetting: false,
  });

  const isProOrAdmin = Boolean(
    user?.role === "PRO_USER" || user?.role === "ADMIN"
  );

  // Redirect if unauthenticated
  useEffect(() => {
    if (!isAuthLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isAuthenticated, isAuthLoading, router]);

  // Adjust active tab if user is not Pro/Admin
  useEffect(() => {
    if (!isAuthLoading && !isProOrAdmin && activeTab === "my-lessons") {
      setActiveTab("library");
    }
  }, [activeTab, isAuthLoading, isProOrAdmin]);

  // Main dynamic sections fetcher.
  // `isBackground: true` is used for polling and post-mutation reconciliation: it
  // never toggles the loading/error UI, it just quietly syncs state with the server.
  const fetchLibraryData = useCallback(
    async (opts: { isBackground?: boolean } = {}) => {
      const { isBackground = false } = opts;
      const requestId = ++requestIdRef.current;
      const hasCachedData =
        activeTab === "my-lessons"
          ? cacheRef.current.myLessons !== undefined
          : cacheRef.current.library !== undefined;

      // Only show the full-page skeleton when there's truly nothing to show yet.
      if (!isBackground && !hasCachedData) {
        setIsLoading(true);
      }
      if (!isBackground) {
        setHasFetchError(false);
      }

      try {
        let tabPromise: Promise<any>;
        if (activeTab === "my-lessons") {
          tabPromise = lessonService.getMyLessons(0, 50);
        } else {
          // Reuse early promise if within 2 seconds. This allows React Strict Mode
          // (which mounts twice rapidly) to share the same promise, preventing a
          // duplicate network request.
          if (earlyLibraryPromise && Date.now() - earlyFetchTime < 2000) {
            tabPromise = earlyLibraryPromise;
            if (process.env.NODE_ENV !== "development") earlyLibraryPromise = null; // consume in prod
          } else {
            if (process.env.NODE_ENV === "development") performance.mark("library-request-start");
            tabPromise = lessonService.getLibrarySections().then(res => {
              if (process.env.NODE_ENV === "development") {
                performance.mark("library-response-received");
                performance.measure("library-fetch-duration", "library-request-start", "library-response-received");
              }
              return res;
            });
          }
        }

        const tabData = await tabPromise;

        // A newer request (tab switch, manual retry, or poll tick) has already
        // superseded this one — discard the stale result.
        if (requestId !== requestIdRef.current) return;

        if (activeTab === "my-lessons") {
          const myData = tabData as PageResponse<Lesson>;
          const content = myData.content || [];
          cacheRef.current.myLessons = content;
          setMyLessons(content);
          const activeUploadJobs: ProcessingJobItem[] = content
            .filter((l) => l.status === "PROCESSING" || l.status === "PENDING" || l.status === "FAILED")
            .map((l) => ({
              id: l.id,
              title: l.title || "Tệp âm thanh tải lên",
              progressPercent: l.progress || (l.status === "PROCESSING" ? 65 : 10),
              status: l.status,
              startedAt: l.createdAt,
              duration: l.duration,
            }));
          setProcessingJobs(activeUploadJobs);
        } else {
          const fetchedSections = (tabData as LibrarySectionDTO[]) || [];
          cacheRef.current.library = fetchedSections;
          setSections(fetchedSections);

          // Extract processing items if USER_PROCESSING section returned
          const processingSec = fetchedSections.find((s) => s.type === "USER_PROCESSING");
          if (processingSec && processingSec.items.length > 0) {
            const jobs: ProcessingJobItem[] = processingSec.items.map((item) => ({
              id: item.id,
              title: item.title || "Tệp âm thanh tải lên",
              progressPercent: item.progress || 50,
              status: "PROCESSING",
              duration: item.duration ?? undefined,
            }));
            setProcessingJobs(jobs);
          } else {
            setProcessingJobs([]); // Clear if none found
          }
        }
      } catch (err) {
        console.error("Error fetching library data:", err);
        if (requestId === requestIdRef.current && !hasCachedData) {
          setHasFetchError(true);
        }
      } finally {
        if (requestId === requestIdRef.current) {
          setIsLoading(false);
        }
      }
    },
    [activeTab]
  );

  useEffect(() => {
    if (!isAuthenticated) return;
    // Paint instantly from cache on tab switch (no skeleton flash), then revalidate.
    if (activeTab === "my-lessons" && cacheRef.current.myLessons) {
      setMyLessons(cacheRef.current.myLessons);
    } else if (activeTab === "library" && cacheRef.current.library) {
      setSections(cacheRef.current.library);
    }
    fetchLibraryData();
  }, [fetchLibraryData, isAuthenticated, activeTab]);

  // Secondary data (categories for the hero chips, streak for the banner) — fetched
  // AFTER the primary library/my-lessons request above. Two things make this a real
  // priority ordering rather than just "later in the file":
  //  1. Effects run in declaration order within the same commit, so the effect above
  //     has already called `fetchLibraryData()` — and therefore already dispatched
  //     its network request — before this effect body even runs.
  //  2. This effect further defers its own fetches to the next idle period via
  //     `requestIdleCallback` (falling back to a short `setTimeout` on Safari, which
  //     doesn't support it), so on a slow/throttled connection the browser finishes
  //     giving priority to the sections request instead of opening 2 more
  //     connections/streams at the same instant.
  // The `timeout` option guarantees these still run within ~1.5s even if the main
  // thread stays busy, so this never turns into an indefinite wait.
  useEffect(() => {
    if (!isAuthenticated) return;

    const runWhenIdle = (cb: () => void) => {
      if (typeof window !== "undefined" && "requestIdleCallback" in window) {
        const id = (window as any).requestIdleCallback(cb, { timeout: 1500 });
        return () => (window as any).cancelIdleCallback(id);
      }
      const t = setTimeout(cb, 200);
      return () => clearTimeout(t);
    };

    return runWhenIdle(() => {
      if (categories.length === 0) {
        lessonService
          .getCategories("", 0, 50)
          .then((res) => {
            if (res?.content) setCategories(res.content);
          })
          .catch(console.error);
      }
      if (!streakData) {
        streakService
          .getMyStreak()
          .then((res) => {
            if (res) setStreakData(res);
          })
          .catch(console.error);
      }
    });
  }, [isAuthenticated, categories.length, streakData]);

  // Performance measurement logging
  useEffect(() => {
    if (process.env.NODE_ENV === "development" && sections.length > 0 && typeof performance !== "undefined") {
      try {
        performance.mark("library-content-rendered");
        if (performance.getEntriesByName("library-request-start").length > 0) {
          performance.measure("navigation-to-render", "library-request-start", "library-content-rendered");
          const measures = performance.getEntriesByType("measure").filter(m => m.name.includes("library") || m.name.includes("navigation"));
          if (measures.length > 0) {
            console.log("⏱️ Performance Marks:", measures.map(m => ({ name: m.name, durationMs: Math.round(m.duration) })));
          }
        }
        performance.clearMeasures();
        performance.clearMarks();
      } catch (e) {
        // ignore performance API errors
      }
    }
  }, [sections]);

  // Periodic polling for active background processing jobs (every 10s if any job is
  // active). Pauses automatically while the tab isn't visible, and catches up with a
  // single immediate refresh when it becomes visible again — no point burning
  // network/battery polling a tab nobody is looking at.
  useEffect(() => {
    const hasActiveJobs = processingJobs.some(
      (j) => j.status === "PROCESSING" || j.status === "PENDING"
    );
    if (!hasActiveJobs || !isAuthenticated) return;

    let intervalId: ReturnType<typeof setInterval> | null = null;

    const startPolling = () => {
      if (intervalId) return;
      intervalId = setInterval(() => {
        fetchLibraryData({ isBackground: true });
      }, 10000);
    };
    const stopPolling = () => {
      if (intervalId) {
        clearInterval(intervalId);
        intervalId = null;
      }
    };
    const handleVisibilityChange = () => {
      if (document.visibilityState === "hidden") {
        stopPolling();
      } else {
        fetchLibraryData({ isBackground: true });
        startPolling();
      }
    };

    if (document.visibilityState !== "hidden") startPolling();
    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      stopPolling();
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [processingJobs, isAuthenticated, fetchLibraryData]);

  // Retry failed upload job — optimistic-ish: kick off the retry, then quietly
  // reconcile in the background instead of blocking on a full refetch.
  const handleRetryJob = useCallback(
    async (jobId: string) => {
      try {
        await lessonService.retryLesson(jobId);
        fetchLibraryData({ isBackground: true });
      } catch (err) {
        console.error("Failed to retry job:", err);
      }
    },
    [fetchLibraryData]
  );

  // Cancel/delete processing job or uploaded lesson — updates local state and the
  // cache immediately so the UI feels instant, then reconciles with the server
  // quietly in the background rather than blocking on (and skeleton-flashing from)
  // a full refetch.
  const handleDeleteJob = useCallback(
    async (jobId: string) => {
      try {
        await lessonService.deleteLesson(jobId);
        setProcessingJobs((prev) => prev.filter((j) => j.id !== jobId));
        setMyLessons((prev) => prev.filter((l) => l.id !== jobId));
        setSections((prev) =>
          prev.map((s) => ({ ...s, items: s.items.filter((it) => it.id !== jobId) }))
        );
        if (cacheRef.current.myLessons) {
          cacheRef.current.myLessons = cacheRef.current.myLessons.filter((l) => l.id !== jobId);
        }
        if (cacheRef.current.library) {
          cacheRef.current.library = cacheRef.current.library.map((s) => ({
            ...s,
            items: s.items.filter((it) => it.id !== jobId),
          }));
        }
        fetchLibraryData({ isBackground: true });
      } catch (err) {
        console.error("Failed to delete lesson/job:", err);
      }
    },
    [fetchLibraryData]
  );

  const handleClearCompletedJobs = useCallback(() => {
    setProcessingJobs((prev) =>
      prev.filter((j) => j.status === "PROCESSING" || j.status === "PENDING")
    );
  }, []);

  // Defers the tab content swap (React 18 transition) so the click itself stays
  // snappy even while the new tab's content is still being prepared/painted.
  const handleTabChange = useCallback((tab: "library" | "my-lessons") => {
    startTabTransition(() => setActiveTab(tab));
  }, []);

  // Build category chips list from API categories
  const categoryChips: CategoryChipItem[] = useMemo(() => {
    const chipList: CategoryChipItem[] = [{ id: "", label: "Tất cả" }];
    if (categories && categories.length > 0) {
      categories.slice(0, 8).forEach((cat) => {
        chipList.push({ id: cat.id, label: cat.name });
      });
    } else {
      chipList.push(
        { id: "work-career", label: "Work & Career" },
        { id: "technology", label: "Technology" },
        { id: "ted-talks", label: "TED Talks" },
        { id: "6-minute-english", label: "6 Minute English" },
        { id: "health-lifestyle", label: "Health & Lifestyle" }
      );
    }
    return chipList;
  }, [categories]);

  // Streak banner data
  const streakInfo: StreakInfo | null = useMemo(() => {
    if (!streakData) return null;
    return {
      currentStreak: streakData.currentStreak,
      longestStreak: streakData.longestStreak,
      hasStudiedToday: streakData.hasStudiedToday,
      weeklyProgress: streakData.weeklyProgress,
    };
  }, [streakData]);

  // Handle lesson navigation or reset modal
  const handleSelectLesson = useCallback(
    (lessonId: string, progress: number, title?: string) => {
      if (progress === 100) {
        setResetModal({
          isOpen: true,
          lessonId,
          lessonTitle: title || "Bài học này",
          isResetting: false,
        });
      } else {
        router.push(`/practice/${lessonId}`);
      }
    },
    [router]
  );

  const handleConfirmResetLesson = useCallback(async () => {
    if (!resetModal.lessonId) return;
    const id = resetModal.lessonId;
    setResetModal((prev) => ({ ...prev, isResetting: true }));
    try {
      await lessonService.resetProgress(id);
    } catch (err) {
      console.error("Failed to reset lesson progress:", err);
    } finally {
      setResetModal({
        isOpen: false,
        lessonId: "",
        lessonTitle: "",
        isResetting: false,
      });
      router.push(`/practice/${id}`);
    }
  }, [resetModal.lessonId, router]);

  // Stable per-child-type callbacks — keeps prop references stable across renders
  // so any memoized child components (ContinueLearningSection, CuratedShelfSection,
  // etc.) can actually skip re-rendering.
  const handleSelectContinueLesson = useCallback(
    (item: ContinueLearningItem) => handleSelectLesson(item.id, item.progressPercent, item.title),
    [handleSelectLesson]
  );
  const handleSelectShelfLesson = useCallback(
    (item: TopicLessonItem) => handleSelectLesson(item.id, 0, item.title),
    [handleSelectLesson]
  );
  const handlePracticeUploaded = useCallback(
    (item: UploadedLessonItem) => handleSelectLesson(item.id, item.progress ?? 0, item.title),
    [handleSelectLesson]
  );
  const handleDeleteUploaded = useCallback(
    (item: UploadedLessonItem) => handleDeleteJob(item.id),
    [handleDeleteJob]
  );

  const activeProcessingCount = processingJobs.filter(
    (j) => j.status === "PROCESSING" || j.status === "PENDING"
  ).length;

  const uploadedLessonItems: UploadedLessonItem[] = useMemo(() => {
    return myLessons.map((l) => ({
      id: l.id,
      title: l.title || "Bài học tải lên",
      status:
        l.status === "DONE"
          ? "READY"
          : l.status === "FAILED"
          ? "FAILED"
          : "PROCESSING",
      durationMinutes: l.duration ? Math.ceil(l.duration / 60) : 5,
      createdAt: l.createdAt,
      thumbnailUrl: l.thumbnail,
      progress: l.progress,
    }));
  }, [myLessons]);

  // Translate the raw `sections` DTOs into render-ready shelf data exactly once per
  // `sections`/`isProOrAdmin` change, instead of on every render (previously this ran
  // inline in JSX, so e.g. opening the processing modal — an unrelated state change —
  // re-built every array in every shelf on every re-render).
  const renderableSections: RenderableSection[] = useMemo(() => {
    const result: RenderableSection[] = [];

    sections.forEach((section, idx) => {
      if (!section.items || section.items.length === 0) return;

      switch (section.type) {
        case "IN_PROGRESS": {
          result.push({
            kind: "IN_PROGRESS",
            key: section.id || `in-progress-${idx}`,
            title: section.title || "Tiếp tục học",
            items: section.items.map((item) => ({
              id: item.id,
              title: item.title || "Bài học đang học",
              source: getSourceFromTitle(item.title),
              durationMinutes: item.duration ? Math.ceil(item.duration / 60) : 6,
              progressPercent: item.progress || 0,
              thumbnailUrl: item.thumbnailUrl || undefined,
            })),
          });
          break;
        }

        case "USER_UPLOADED": {
          if (!isProOrAdmin) return;
          result.push({
            kind: "USER_UPLOADED",
            key: section.id || `uploaded-${idx}`,
            title: section.title || "Bài học tải lên của bạn",
            seeAllHref: "?tab=my-lessons",
            items: section.items.map((item) => ({
              id: item.id,
              title: item.title || "Bài học tải lên",
              status: "READY",
              durationMinutes: item.duration ? Math.ceil(item.duration / 60) : 5,
              createdAt: new Date().toISOString(),
              thumbnailUrl: item.thumbnailUrl || undefined,
              progress: item.progress,
            })),
          });
          break;
        }

        case "CATEGORY": {
          const theme = getGradientTheme(section.categorySlug || section.title, idx);
          const seeAllHref = section.categorySlug
            ? `/library/categories/${section.categorySlug}`
            : section.categoryId
            ? `/library/categories/${section.categoryId}`
            : undefined;

          result.push({
            kind: "SHELF",
            key: section.id || `category-${idx}`,
            shelfId: `shelf-${section.id}`,
            title: section.title,
            theme,
            seeAllHref,
            items: section.items.map((item, itemIdx) => ({
              id: item.id,
              title: item.title || `Bài học ${itemIdx + 1}`,
              topicBadge: section.categoryName || "CATEGORY",
              lessonNumber: `Lesson ${String(itemIdx + 1).padStart(2, "0")}`,
              durationMinutes: item.duration ? Math.ceil(item.duration / 60) : 6,
              gradientTheme: theme,
              thumbnailUrl: item.thumbnailUrl || undefined,
              categoryName: section.categoryName || undefined,
            })),
          });
          break;
        }

        case "PROGRAM": {
          const theme = getGradientTheme(section.program || section.title, idx);
          result.push({
            kind: "SHELF",
            key: section.id || `program-${idx}`,
            shelfId: `shelf-${section.id}`,
            title: section.title,
            theme,
            seeAllHref: `/library/search?program=${encodeURIComponent(section.program || section.title)}`,
            items: section.items.map((item, itemIdx) => ({
              id: item.id,
              title: item.title || `Bài học ${itemIdx + 1}`,
              topicBadge: section.program || "PROGRAM",
              lessonNumber: `Ep. ${String(itemIdx + 1).padStart(2, "0")}`,
              durationMinutes: item.duration ? Math.ceil(item.duration / 60) : 6,
              gradientTheme: theme,
              thumbnailUrl: item.thumbnailUrl || undefined,
              source: section.program || undefined,
            })),
          });
          break;
        }

        default:
          break;
      }
    });

    return result;
  }, [sections, isProOrAdmin]);

  return (
    <div className="bg-[#F8FAF9] flex flex-col flex-1 min-w-0 min-h-screen text-slate-900">
      {/* Sticky Top Header Navigation */}
      <header className="px-4 md:px-8 py-3.5 sm:py-4 flex items-center justify-between border-b border-emerald-100/70 bg-white/90 backdrop-blur-md sticky top-0 z-40 shadow-2xs">
        <div className="flex items-center gap-3 sm:gap-8">
          <button
            type="button"
            onClick={openMobileMenu}
            aria-label="Mở menu điều hướng"
            className="lg:hidden p-2 hover:bg-emerald-50 rounded-xl text-slate-600 transition-all active:scale-95 cursor-pointer"
          >
            <Menu size={22} />
          </button>

          {/* Desktop Tab Navigation */}
          <nav className="hidden lg:flex items-center gap-8 text-sm font-bold">
            <button
              type="button"
              onClick={() => handleTabChange("library")}
              className={`pb-1 cursor-pointer transition-all border-b-2 ${
                activeTab === "library"
                  ? "text-emerald-700 border-emerald-600 font-extrabold"
                  : "text-slate-500 hover:text-slate-900 border-transparent"
              }`}
            >
              Thư viện bài học
            </button>

            {isProOrAdmin && (
              <button
                type="button"
                onClick={() => handleTabChange("my-lessons")}
                className={`pb-1 cursor-pointer transition-all border-b-2 ${
                  activeTab === "my-lessons"
                    ? "text-emerald-700 border-emerald-600 font-extrabold"
                    : "text-slate-500 hover:text-slate-900 border-transparent"
                }`}
              >
                Bài học của tôi
              </button>
            )}

            <Link
              href="/profile"
              prefetch={false}
              className="text-slate-500 hover:text-slate-900 transition-colors flex items-center gap-1.5"
            >
              <Flame className="w-4 h-4 text-orange-500" />
              <span>Tiến độ học tập</span>
            </Link>
          </nav>
        </div>

        {/* Right Nav Actions */}
        <div className="flex items-center gap-3 sm:gap-4">
          <UserMenu />
        </div>
      </header>

      {/* Hero Search Banner */}
      <LibraryHero
        title={activeTab === "my-lessons" ? "Bài học của bạn" : "Thư viện bài học"}
        subtitle={
          activeTab === "my-lessons"
            ? "Luyện tập với các bài học riêng do bạn tải lên và hệ thống AI tự động phân tích."
            : "Khám phá và luyện nghe tiếng Anh qua hàng trăm bài học thực tế từ BBC, TED, VOA."
        }
        categories={activeTab === "library" ? categoryChips : []}
        onSearchSubmit={(val) => {
          if (val.trim()) {
            router.push(`/library/search?q=${encodeURIComponent(val.trim())}`);
          }
        }}
        onSelectCategory={(catId) => {
          if (catId) {
            router.push(`/library/categories/${catId}`);
          }
        }}
      />

      {/* Main Content Area */}
      <main
        className={`flex-1 w-full mx-auto space-y-8 sm:space-y-10 py-6 sm:py-8 pb-24 transition-opacity duration-200 ${
          isTabPending ? "opacity-60" : "opacity-100"
        }`}
      >
        {/* Streak Banner */}
        <StreakBanner streak={streakInfo} href="/profile" />

        {/* Mobile Tab Switcher */}
        {isProOrAdmin && (
          <div className="max-w-6xl mx-auto px-4 md:px-8 lg:hidden">
            <div className="flex rounded-2xl bg-emerald-50/80 p-1 border border-emerald-200/50">
              <button
                type="button"
                onClick={() => handleTabChange("library")}
                className={`flex-1 rounded-xl py-2.5 text-xs font-bold transition-all cursor-pointer ${
                  activeTab === "library"
                    ? "bg-white text-emerald-800 shadow-xs border border-emerald-100"
                    : "text-slate-600 hover:text-slate-900"
                }`}
              >
                Thư viện bài học
              </button>
              <button
                type="button"
                onClick={() => handleTabChange("my-lessons")}
                className={`flex-1 rounded-xl py-2.5 text-xs font-bold transition-all cursor-pointer ${
                  activeTab === "my-lessons"
                    ? "bg-white text-emerald-800 shadow-xs border border-emerald-100"
                    : "text-slate-600 hover:text-slate-900"
                }`}
              >
                Bài học của tôi
              </button>
            </div>
          </div>
        )}

        {/* Content States */}
        {isLoading ? (
          <LibrarySkeleton />
        ) : hasFetchError ? (
          <div className="max-w-4xl mx-auto px-4 py-12 text-center">
            <div className="bg-white rounded-3xl border border-rose-100 p-8 shadow-xs flex flex-col items-center gap-3">
              <div className="w-12 h-12 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center">
                <AlertCircle className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-800">
                Không thể tải danh sách bài học
              </h3>
              <p className="text-sm text-slate-500 max-w-md">
                Đã xảy ra sự cố trong quá trình kết nối với máy chủ. Vui lòng kiểm tra lại đường truyền và thử lại.
              </p>
              <button
                type="button"
                onClick={() => fetchLibraryData()}
                className="mt-2 inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-bold transition-all active:scale-95 cursor-pointer shadow-xs"
              >
                <RotateCcw className="w-4 h-4" />
                <span>Thử tải lại</span>
              </button>
            </div>
          </div>
        ) : activeTab === "my-lessons" ? (
          /* "My Lessons" Tab View */
          <div className="space-y-8">
            <UploadedLessonsSection
              items={uploadedLessonItems}
              title="Bài học của bạn"
              uploadHref="/upload"
              onPractice={handlePracticeUploaded}
              onDelete={handleDeleteUploaded}
            />
          </div>
        ) : renderableSections.length === 0 ? (
          <LibraryEmptyState onResetFilters={() => fetchLibraryData()} />
        ) : (
          /* Dynamic Server-Configured Library Sections Layout */
          <div className="space-y-10 sm:space-y-12">
            {renderableSections.map((section) => {
              switch (section.kind) {
                case "IN_PROGRESS":
                  return (
                    <ContinueLearningSection
                      key={section.key}
                      items={section.items}
                      title={section.title}
                      onSelectLesson={handleSelectContinueLesson}
                    />
                  );

                case "USER_UPLOADED":
                  return (
                    <UploadedLessonsSection
                      key={section.key}
                      items={section.items}
                      title={section.title}
                      uploadHref="/upload"
                      seeAllHref={section.seeAllHref}
                      onPractice={handlePracticeUploaded}
                      onDelete={handleDeleteUploaded}
                    />
                  );

                case "SHELF":
                  return (
                    <CuratedShelfSection
                      key={section.key}
                      id={section.shelfId}
                      title={section.title}
                      gradientTheme={section.theme}
                      items={section.items}
                      seeAllHref={section.seeAllHref}
                      onSelectLesson={handleSelectShelfLesson}
                    />
                  );

                default:
                  return null;
              }
            })}
          </div>
        )}
      </main>

      {/* Floating Processing FAB Button (if active processing jobs exist) */}
      <ProcessingFab
        count={activeProcessingCount}
        onClick={() => setIsProcessingModalOpen(true)}
      />

      {/* Background Processing Details Modal */}
      <ProcessingModal
        isOpen={isProcessingModalOpen}
        onClose={() => setIsProcessingModalOpen(false)}
        jobs={processingJobs}
        onRetry={handleRetryJob}
        onCancel={handleDeleteJob}
        onClearCompleted={handleClearCompletedJobs}
        onPractice={(jobId) => router.push(`/practice/${jobId}`)}
      />

      {/* Reset 100% Completed Lesson Confirmation Modal */}
      <ConfirmationModal
        isOpen={resetModal.isOpen}
        onClose={() =>
          setResetModal({
            isOpen: false,
            lessonId: "",
            lessonTitle: "",
            isResetting: false,
          })
        }
        onConfirm={handleConfirmResetLesson}
        isLoading={resetModal.isResetting}
        title="Luyện tập lại bài học này?"
        message={`Bạn đã hoàn thành 100% bài học "${resetModal.lessonTitle}". Bạn có muốn bắt đầu lại từ đầu không? Tiến trình ghi nhớ hiện tại sẽ được đặt lại.`}
        confirmText="Học lại từ đầu"
        cancelText="Đóng"
      />
    </div>
  );
}

export default function LibraryPage() {
  return (
    <Suspense fallback={<LibrarySkeleton />}>
      <LibraryContent />
    </Suspense>
  );
}