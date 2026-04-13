"use client";

import { useState, useEffect, useCallback } from "react";
import LessonCard from "@/components/library/LessonCard";
import FeaturedLesson from "@/components/library/FeaturedLesson";
import { Search, Bell, LogOut, Loader2, ChevronLeft, ChevronRight } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";
import { useDebounce } from "use-debounce";

import { lessonService, Lesson, Category } from "@/lib/services/lessonService";

export default function LibraryPage() {
    const { user, isAuthenticated, isLoading: isAuthLoading, logout } = useAuth();
    const router = useRouter();

    const [jobs, setJobs] = useState<Lesson[]>([]);
    const [categories, setCategories] = useState<Category[]>([]);
    const [activeCategory, setActiveCategory] = useState<string>("");
    const [searchQuery, setSearchQuery] = useState("");
    const [debouncedSearch] = useDebounce(searchQuery, 500);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);

    const fetchCategories = useCallback(async () => {
        try {
            const data = await lessonService.getCategories();
            if (data) setCategories(data);
        } catch (e) {
            console.error("Failed to load categories", e);
        }
    }, []);

    const fetchLessons = useCallback(async () => {
        setLoading(true);
        try {
            const data = await lessonService.getAllLessons(page, 9, debouncedSearch, activeCategory);
            setJobs(data.content || []);
            setTotalPages(data.totalPages || 0);
        } catch (err) {
            console.error("Error fetching lessons:", err);
        } finally {
            setLoading(false);
        }
    }, [page, debouncedSearch, activeCategory]);

    useEffect(() => {
        if (!isAuthLoading && !isAuthenticated) {
            router.push("/login");
            return;
        }
        if (isAuthenticated) {
            fetchCategories();
        }
    }, [isAuthenticated, isAuthLoading, router, fetchCategories]);

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

    const featuredJob = page === 0 && jobs.length > 0 && !debouncedSearch && !activeCategory ? jobs[0] : null;
    const gridJobs = jobs.filter(job => job.id !== featuredJob?.id);

    return (
        <>
            {/* Top Header */}
            <header className="px-8 py-5 flex items-center justify-between border-b border-gray-100 bg-white/80 backdrop-blur-md sticky top-0 z-50">
                <div className="hidden md:flex gap-10 text-sm font-bold">
                    <span className="text-green-500 border-b-2 border-green-500 pb-1">Lesson Library</span>
                    <Link href="/profile" className="text-gray-400 hover:text-gray-800 transition-colors cursor-pointer">My Progress</Link>
                </div>

                {/* Mobile title */}
                <div className="md:hidden relative w-28 h-8">
                    <Image src="/logo_long.png" alt="ParroTalk" fill className="object-contain" />
                </div>

                <div className="flex items-center gap-6 text-gray-400">
                    <div className="relative hidden sm:block">
                        <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4" />
                        <input
                            type="text"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            placeholder="Search lessons..."
                            className="bg-gray-50 border border-gray-100 rounded-full pl-10 pr-4 py-2 w-64 text-sm focus:outline-none focus:ring-2 focus:ring-green-100 focus:border-green-500 text-gray-800 transition-all"
                        />
                    </div>
                    <Bell className="w-5 h-5 cursor-pointer hover:text-gray-800 transition-colors" />

                    <div className="flex items-center gap-3 pl-4 border-l border-gray-100">
                        <div className="hidden sm:flex flex-col items-end">
                            <span className="text-sm font-black text-gray-800 leading-none">{user?.fullName}</span>
                            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">{user?.role}</span>
                        </div>
                        <div className="w-10 h-10 rounded-full bg-green-100 text-green-600 flex items-center justify-center font-bold outline outline-offset-2 outline-white cursor-pointer hover:bg-green-200 transition-colors shadow-sm">
                            {user?.fullName?.charAt(0) || "U"}
                        </div>
                        <button
                            onClick={logout}
                            className="w-10 h-10 rounded-full bg-gray-50 text-gray-400 flex items-center justify-center hover:bg-red-50 hover:text-red-500 transition-all border border-transparent hover:border-red-100"
                            title="Logout"
                        >
                            <LogOut size={18} />
                        </button>
                    </div>
                </div>
            </header>

            {/* Page Content */}
            <div className="px-6 md:px-10 py-12 max-w-7xl w-full mx-auto flex flex-col gap-10">
                {/* Page Headers & Filters */}
                <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-8">
                    <div className="flex flex-col gap-3 max-w-xl">
                        <h1 className="text-4xl md:text-5xl font-black text-gray-900 tracking-tight leading-none">Lesson Library</h1>
                        <p className="text-gray-500 text-lg">Hone your dictation and pronunciation skills with our curated collection of pristine audio and video lessons.</p>
                    </div>

                    <div className="flex flex-wrap items-center gap-2">
                        <button
                            onClick={() => { setActiveCategory(""); setPage(0); }}
                            className={`px-6 py-2.5 rounded-full text-sm font-bold transition-all active:scale-95 ${
                                activeCategory === ""
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
                                className={`px-6 py-2.5 rounded-full text-sm font-bold transition-all active:scale-95 ${
                                    activeCategory === cat.id
                                        ? "bg-gray-900 text-white shadow-lg shadow-gray-200"
                                        : "bg-white border border-gray-100 text-gray-600 hover:bg-gray-50 hover:border-gray-200"
                                }`}
                            >
                                {cat.name}
                            </button>
                        ))}
                    </div>
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
                        {featuredJob && (
                            <FeaturedLesson job={featuredJob} />
                        )}

                        {jobs.length > 0 ? (
                            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-8">
                                {gridJobs.map(job => (
                                    <LessonCard key={job.id} job={job} />
                                ))}
                            </div>
                        ) : (
                            <div className="text-center py-32 bg-gray-50 rounded-[2rem] border border-gray-100 flex flex-col items-center justify-center gap-4">
                                <div className="w-16 h-16 bg-white rounded-full flex items-center justify-center shadow-sm text-gray-400">
                                    <Search size={24} />
                                </div>
                                <h3 className="text-xl font-bold text-gray-900">No lessons found</h3>
                                <p className="text-gray-500 max-w-sm">We couldn&apos;t find any lessons matching your filter. Try tweaking your filters or uploading more media.</p>
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
            </div>
        </>
    );
}
