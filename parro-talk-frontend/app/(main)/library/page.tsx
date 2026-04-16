"use client";

import { useState, useEffect, useCallback, useRef, Suspense } from "react";
import LessonCard from "@/components/library/LessonCard";
import FeaturedLesson from "@/components/library/FeaturedLesson";
import { Search, Bell, LogOut, Loader2, ChevronLeft, ChevronRight, Settings, User as UserIcon, LogOut as LogOutIcon } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { useRouter, useSearchParams, usePathname } from "next/navigation";
import { useDebounce } from "use-debounce";
import { useUI } from "@/context/UIContext";
import { Menu } from "lucide-react";
import { lessonService, Lesson, Category } from "@/lib/services/lessonService";

function LibraryContent() {
    const { user, isAuthenticated, isLoading: isAuthLoading, logout } = useAuth();
    const { openMobileMenu } = useUI();
    const router = useRouter();
    const searchParams = useSearchParams();
    const pathname = usePathname();

    const [jobs, setJobs] = useState<Lesson[]>([]);
    const [categories, setCategories] = useState<Category[]>([]);
    const [activeCategory, setActiveCategory] = useState<string>(searchParams.get("category") || "");
    const [searchQuery, setSearchQuery] = useState(searchParams.get("q") || "");
    const [debouncedSearch] = useDebounce(searchQuery, 500);
    const [page, setPage] = useState(Number(searchParams.get("page")) || 0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);
    const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
    const userMenuRef = useRef<HTMLDivElement>(null);

    // Sync state to URL
    useEffect(() => {
        const params = new URLSearchParams(searchParams.toString());
        if (debouncedSearch) params.set("q", debouncedSearch); else params.delete("q");
        if (activeCategory) params.set("category", activeCategory); else params.delete("category");
        if (page > 0) params.set("page", page.toString()); else params.delete("page");

        const newQueryString = params.toString();
        const currentQueryString = searchParams.toString();

        if (newQueryString !== currentQueryString) {
            router.replace(`${pathname}${newQueryString ? `?${newQueryString}` : ""}`, { scroll: false });
        }
    }, [debouncedSearch, activeCategory, page, pathname, router, searchParams]);

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
    }, [isAuthenticated, isAuthLoading, router]);

    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (userMenuRef.current && !userMenuRef.current.contains(event.target as Node)) {
                setIsUserMenuOpen(false);
            }
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

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
        <div className="min-h-screen bg-white flex flex-col">
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
                        <span className="text-green-500 border-b-2 border-green-500 pb-1 shrink-0">Lesson Library</span>
                        <Link href="/profile" className="text-gray-400 hover:text-gray-800 transition-colors cursor-pointer shrink-0">My Progress</Link>
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
                    <div className="relative" ref={userMenuRef}>
                        <div
                            onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                            className="flex items-center gap-2 sm:gap-3 pl-2 sm:pl-4 border-l border-gray-100 cursor-pointer group"
                        >
                            <div className="hidden sm:flex flex-col items-end">
                                <span className="text-sm font-black text-gray-800 leading-none group-hover:text-green-600 transition-colors uppercase">{user?.fullName}</span>
                                <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">{user?.role}</span>
                            </div>
                            <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-green-100 text-green-600 flex items-center justify-center font-bold outline outline-offset-2 outline-white group-hover:bg-green-200 group-hover:outline-green-50 transition-all shadow-sm text-sm sm:text-base">
                                {user?.fullName?.charAt(0) || "U"}
                            </div>
                        </div>

                        {isUserMenuOpen && (
                            <div className="absolute right-0 mt-3 w-56 bg-white rounded-2xl shadow-2xl border border-gray-100 py-2 z-[60] animate-in fade-in zoom-in duration-200 origin-top-right">
                                <div className="px-4 py-3 border-b border-gray-50 flex flex-col gap-1 sm:hidden">
                                    <p className="text-sm font-black text-gray-900 leading-none">{user?.fullName}</p>
                                    <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">{user?.role}</p>
                                </div>

                                <div className="p-1.5 flex flex-col gap-0.5">
                                    <Link
                                        href="/profile"
                                        className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-bold text-gray-600 hover:bg-gray-50 hover:text-gray-900 transition-all group"
                                        onClick={() => setIsUserMenuOpen(false)}
                                    >
                                        <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-500 flex items-center justify-center group-hover:scale-110 transition-transform">
                                            <UserIcon size={16} />
                                        </div>
                                        Profile
                                    </Link>
                                    <Link
                                        href="/settings"
                                        className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-bold text-gray-600 hover:bg-gray-50 hover:text-gray-900 transition-all group"
                                        onClick={() => setIsUserMenuOpen(false)}
                                    >
                                        <div className="w-8 h-8 rounded-lg bg-gray-100 text-gray-500 flex items-center justify-center group-hover:scale-110 transition-transform">
                                            <Settings size={16} />
                                        </div>
                                        Settings
                                    </Link>
                                    <div className="h-px bg-gray-50 my-1 mx-2" />
                                    <button
                                        onClick={() => {
                                            logout();
                                            setIsUserMenuOpen(false);
                                        }}
                                        className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-bold text-red-500 hover:bg-red-50 transition-all group w-full text-left"
                                    >
                                        <div className="w-8 h-8 rounded-lg bg-red-50 text-red-500 flex items-center justify-center group-hover:scale-110 transition-transform">
                                            <LogOutIcon size={16} />
                                        </div>
                                        Log out
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </header>

            <main className="px-4 md:px-8 py-8 md:py-12 max-w-7xl w-full mx-auto flex flex-col gap-6 md:gap-10">
                <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
                    <div className="flex flex-col gap-3 max-w-xl">
                        <h1 className="text-3xl md:text-5xl font-black text-gray-900 tracking-tight leading-tight">Lesson Library</h1>
                        <p className="text-gray-500 text-base md:text-lg">Hone your dictation and pronunciation skills with our curated collection of pristine audio and video lessons.</p>
                    </div>

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
