"use client";

import { useState, useEffect, useCallback, Suspense } from "react";
import LessonCard from "@/features/lesson/components/LessonCard";
import FeaturedLesson from "@/features/lesson/components/FeaturedLesson";
import { Search, Loader2, ChevronLeft, ChevronRight } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useRouter, useSearchParams, usePathname } from "next/navigation";
import { useDebounce } from "use-debounce";
import { useUI } from "@/hooks/useUI";
import { Menu } from "lucide-react";
import { lessonService, Lesson, Category } from "@/features/lesson/services/lessonService";
import UserMenu from "@/components/common/UserMenu";

function LibraryContent() {
    const { user, isAuthenticated, isLoading: isAuthLoading, logout } = useAuth();
    const { openMobileMenu } = useUI();
    const router = useRouter();
    const searchParams = useSearchParams();
    const pathname = usePathname();

    const [lessons, setLessons] = useState<Lesson[]>([]);
    const [categories, setCategories] = useState<Category[]>([]);
    const [activeCategory, setActiveCategory] = useState<string>(searchParams.get("category") || "");
    const [activeTab, setActiveTab] = useState<"library" | "my-lessons">(
        searchParams.get("tab") === "my-lessons" ? "my-lessons" : "library",
    );
    const [searchQuery, setSearchQuery] = useState(searchParams.get("q") || "");
    const [debouncedSearch] = useDebounce(searchQuery, 500);
    const [page, setPage] = useState(Number(searchParams.get("page")) || 0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);

    // Reset page when filters change
    useEffect(() => {
        setPage(0);
    }, [debouncedSearch, activeCategory, activeTab]);

    // Sync state to URL
    useEffect(() => {
        const params = new URLSearchParams(searchParams.toString());
        if (debouncedSearch) params.set("q", debouncedSearch); else params.delete("q");
        if (activeCategory && activeTab === "library") params.set("category", activeCategory); else params.delete("category");
        if (activeTab === "my-lessons") params.set("tab", "my-lessons"); else params.delete("tab");
        if (page > 0) params.set("page", page.toString()); else params.delete("page");

        const newQueryString = params.toString();
        const currentQueryString = searchParams.toString();

        if (newQueryString !== currentQueryString) {
            router.replace(`${pathname}${newQueryString ? `?${newQueryString}` : ""}`, { scroll: false });
        }
    }, [debouncedSearch, activeCategory, activeTab, page, pathname, router, searchParams]);

    const fetchLessons = useCallback(async () => {
        setLoading(true);
        try {
            const data = activeTab === "my-lessons"
                ? await lessonService.getMyLessons(page, 9, debouncedSearch)
                : await lessonService.getAllLessons(page, 9, debouncedSearch, activeCategory);
            setLessons(data.content || []);
            setTotalPages(data.totalPages || 0);
        } catch (err) {
            console.error("Error fetching lessons:", err);
        } finally {
            setLoading(false);
        }
    }, [page, debouncedSearch, activeCategory, activeTab]);

    useEffect(() => {
        if (!isAuthLoading && !isAuthenticated) {
            router.push("/login");
            return;
        }
    }, [isAuthenticated, isAuthLoading, router]);

    useEffect(() => {
        if (!isAuthLoading && user?.role !== "PRO_USER" && activeTab === "my-lessons") {
            setActiveTab("library");
        }
    }, [activeTab, isAuthLoading, user?.role]);



    useEffect(() => {
        if (isAuthenticated) {
            fetchLessons();
        }
    }, [fetchLessons, isAuthenticated]);

    if (isAuthLoading) {
        return (
            <div className="min-h-screen bg-white flex items-center justify-center">
                <Loader2 className="w-10 h-10 animate-spin text-green-500" />
            </div>
        );
    }

    const featuredJob = activeTab === "library" && page === 0 && lessons.length > 0 && !debouncedSearch && !activeCategory ? lessons[0] : null;
    // const gridJobs = jobs.filter(job => job.id !== featuredJob?.id);
    const gridJobs = lessons;

    return (
        <div className="bg-white flex flex-col flex-1 min-w-0">
            {/* Top Header */}
            <header className="px-4 md:px-8 py-4 md:py-5 flex items-center justify-between border-b border-gray-100 bg-white/80 backdrop-blur-md sticky top-0 z-50">
                <div className="flex items-center gap-1">
                    <button
                        onClick={openMobileMenu}
                        className="lg:hidden p-2 hover:bg-gray-100 rounded-xl text-gray-400 transition-all active:scale-95"
                    >
                        <Menu size={24} />
                    </button>
 
                    <div className="hidden lg:flex gap-10 text-sm font-bold">
                        <button
                            onClick={() => setActiveTab("library")}
                            className={`${activeTab === "library" ? "text-green-500 border-b-2 border-green-500" : "text-gray-400 hover:text-gray-800"} pb-1 shrink-0 transition-colors`}
                        >
                            Lesson Library
                        </button>
                        {user?.role === "PRO_USER" || user?.role === "ADMIN" && (
                            <button
                                onClick={() => {
                                    setActiveTab("my-lessons");
                                    setActiveCategory("");
                                }}
                                className={`${activeTab === "my-lessons" ? "text-green-500 border-b-2 border-green-500" : "text-gray-400 hover:text-gray-800"} pb-1 shrink-0 transition-colors`}
                            >
                                My Lessons
                            </button>
                        )}
                        <Link href="/profile" className="text-gray-400 hover:text-gray-800 transition-colors cursor-pointer shrink-0">User Streak</Link>
                    </div>
 
                    <div className="lg:hidden relative w-24 h-7 sm:w-28 sm:h-8 flex items-center">
                        <Image src="/logo_long.png" alt="ParroTalk" width={75} height={30} className="object-contain" />
                    </div>
                </div>
 
                <div className="relative flex-1 md:flex-none">
                    <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <input
                        type="text"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        placeholder="Search lessons..."
                        className="bg-gray-50 border border-gray-100 rounded-full pl-10 pr-4 py-2 w-full md:w-64 lg:w-96 text-sm focus:outline-none focus:ring-2 focus:ring-green-100 focus:border-green-500 text-gray-800 transition-all"
                    />
                </div>
 
                <div className="flex items-center gap-3 sm:gap-6">
                    <UserMenu />
                </div>
            </header>

            <main className="px-4 md:px-8 py-8 md:py-12 max-w-7xl w-full mx-auto flex flex-col gap-6 md:gap-10">
                {user?.role === "PRO_USER" || user?.role === "ADMIN" && (
                    <div className="flex lg:hidden rounded-2xl bg-gray-100 p-1">
                        <button
                            onClick={() => setActiveTab("library")}
                            className={`flex-1 rounded-xl px-4 py-2.5 text-sm font-bold transition-all ${activeTab === "library"
                                ? "bg-white text-green-600 shadow-sm"
                                : "text-gray-500"
                                }`}
                        >
                            Lesson Library
                        </button>
                        <button
                            onClick={() => {
                                setActiveTab("my-lessons");
                                setActiveCategory("");
                            }}
                            className={`flex-1 rounded-xl px-4 py-2.5 text-sm font-bold transition-all ${activeTab === "my-lessons"
                                ? "bg-white text-green-600 shadow-sm"
                                : "text-gray-500"
                                }`}
                        >
                            My Lessons
                        </button>
                    </div>
                )}

                <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
                    <div className="flex flex-col gap-3 max-w-xl">
                        <h1 className="text-3xl md:text-5xl font-black text-gray-900 tracking-tight leading-tight">
                            {activeTab === "my-lessons" ? "My Lessons" : "Lesson Library"}
                        </h1>
                        <p className="text-gray-500 text-base md:text-lg">
                            {activeTab === "my-lessons"
                                ? "Practice with the private lessons you created for yourself."
                                : "Hone your dictation and pronunciation skills with our curated collection of pristine audio and video lessons."}
                        </p>
                    </div>

                    {activeTab === "library" && (
                        <div className="flex gap-2 overflow-x-auto pb-2 -mx-4 px-4 sm:mx-0 sm:px-0 scrollbar-hide">
                            <button
                                onClick={() => { setActiveCategory(""); setPage(0); }}
                                className={`px-6 py-2.5 rounded-full text-sm font-bold transition-all active:scale-95 whitespace-nowrap ${activeCategory === ""
                                    ? "bg-gray-900 text-white shadow-lg shadow-gray-200"
                                    : "bg-white border border-gray-100 text-gray-600 hover:bg-gray-50 hover:border-gray-200"
                                    }`}
                            >
                                All
                            </button>
                            {categories.map(cat => (
                                <button
                                    key={cat.id}
                                    onClick={() => { setActiveCategory(cat.id); setPage(0); }}
                                    className={`px-6 py-2.5 rounded-full text-sm font-bold transition-all active:scale-95 whitespace-nowrap ${activeCategory === cat.id
                                        ? "bg-gray-900 text-white shadow-lg shadow-gray-200"
                                        : "bg-white border border-gray-100 text-gray-600 hover:bg-gray-50 hover:border-gray-200"
                                        }`}
                                >
                                    {cat.name}
                                </button>
                            ))}
                        </div>
                    )}
                </div>

                {loading ? (
                    <div className="w-full flex flex-col gap-8 py-10">
                        <div className="w-full h-[400px] bg-gray-100 rounded-[2rem] animate-pulse"></div>
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                            {[1, 2, 3].map(i => (
                                <div key={i} className="bg-gray-50 h-[300px] rounded-[2rem] animate-pulse"></div>
                            ))}
                        </div>
                    </div>
                ) : (
                    <>
                        {featuredJob && <FeaturedLesson job={featuredJob} />}
                        {lessons.length > 0 ? (
                            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-8">
                                {gridJobs.map(lesson => (
                                    <LessonCard key={lesson.id} lesson={lesson} />
                                ))}
                            </div>
                        ) : (
                            <div className="text-center py-32 bg-gray-50 rounded-[2rem] border border-gray-100 flex flex-col items-center justify-center gap-4">
                                <div className="w-16 h-16 bg-white rounded-full flex items-center justify-center shadow-sm text-gray-400">
                                    <Search size={24} />
                                </div>
                                <h3 className="text-xl font-bold text-gray-900">No lessons found</h3>
                                <p className="text-gray-500 max-w-sm">Try tweaking your filters or searching for something else.</p>
                            </div>
                        )}

                        {totalPages > 1 && (
                            <div className="flex items-center justify-center gap-4 py-8">
                                <button
                                    onClick={() => setPage(p => Math.max(0, p - 1))}
                                    disabled={page === 0}
                                    className="p-3 bg-white border border-gray-100 rounded-full text-gray-600 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-sm"
                                >
                                    <ChevronLeft size={20} />
                                </button>
                                <span className="text-sm font-bold text-gray-600">Page {page + 1} of {totalPages}</span>
                                <button
                                    onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                                    disabled={page === totalPages - 1}
                                    className="p-3 bg-white border border-gray-100 rounded-full text-gray-600 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-sm"
                                >
                                    <ChevronRight size={20} />
                                </button>
                            </div>
                        )}
                    </>
                )}
            </main>
        </div>
    );
}

export default function LibraryPage() {
    return (
        <Suspense fallback={
            <div className="min-h-screen bg-white flex items-center justify-center">
                <Loader2 className="w-10 h-10 animate-spin text-green-500" />
            </div>
        }>
            <LibraryContent />
        </Suspense>
    );
}

