"use client";

import { useState, useEffect } from "react";
import LessonCard from "@/components/library/LessonCard";
import FeaturedLesson from "@/components/library/FeaturedLesson";
import { Mic, Search, Bell, Settings } from "lucide-react";
import Link from "next/link";

interface Job {
  id: string;
  status: string;
  progress: number;
  currentStep: string;
  fileUrl: string;
  createdAt: string;
}

export default function LibraryPage() {
    const [jobs, setJobs] = useState<Job[]>([]);
    const [filter, setFilter] = useState("All");
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetch("http://localhost:8080/api/jobs")
           .then(res => res.json())
           .then((data: Job[]) => {
               setJobs(data);
               setLoading(false);
           })
           .catch(err => {
               console.error("Error fetching jobs:", err);
               setLoading(false);
           });
    }, []);

    // Derived filtered jobs
    const filteredJobs = jobs.filter(job => {
        if (filter === "All") return true;
        if (filter === "In Progress") return job.status === "PROCESSING" || job.status === "PENDING";
        if (filter === "Completed") return job.status === "DONE";
        // for Audio/Video, since we rely on fileUrl suffix
        if (filter === "Audio") return job.fileUrl.match(/\.(mp3|wav|m4a|aac)$/i);
        if (filter === "Video") return job.fileUrl.match(/\.(mp4|mkv|mov|avi)$/i);
        return true;
    });

    const featuredJob = jobs.length > 0 ? jobs[0] : null;

    // Remaining jobs for the grid, excluding the featured job
    const gridJobs = filteredJobs.filter(job => job.id !== featuredJob?.id);

    return (
        <main className="min-h-screen bg-[#FDFDFD] flex font-sans">
            {/* Sidebar Mapping */}
            <aside className="w-72 border-r border-gray-100 hidden lg:flex flex-col py-8 px-6 gap-10 shrink-0 bg-white">
                <div className="flex items-center gap-3 px-2">
                    <div className="bg-green-500 p-2.5 rounded-xl text-white shadow-lg shadow-green-100">
                        <Mic size={22} />
                    </div>
                    <span className="text-2xl font-black tracking-tight text-gray-800">ParroTalk</span>
                </div>
                <div className="flex flex-col gap-2">
                    <div className="text-xs font-black text-gray-400 tracking-widest uppercase mb-2 px-4">Menu</div>
                    <Link href="/library" className="px-4 py-3.5 bg-green-50 text-green-600 font-bold rounded-2xl flex items-center gap-3">
                        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" /></svg>
                        Library
                    </Link>
                    <div className="px-4 py-3.5 text-gray-500 font-bold hover:bg-gray-50 hover:text-gray-800 rounded-2xl cursor-pointer transition-colors flex items-center gap-3">
                        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" /></svg>
                        My Progress
                    </div>
                    <div className="px-4 py-3.5 text-gray-500 font-bold hover:bg-gray-50 hover:text-gray-800 rounded-2xl cursor-pointer transition-colors flex items-center gap-3">
                        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" /></svg>
                        Downloads
                    </div>
                    <div className="px-4 py-3.5 text-gray-500 font-bold hover:bg-gray-50 hover:text-gray-800 rounded-2xl cursor-pointer transition-colors flex items-center gap-3">
                        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" /></svg>
                        Favorites
                    </div>
                </div>
            </aside>

            {/* Main Content Area */}
            <div className="flex-1 flex flex-col h-screen overflow-y-auto">
                <header className="px-8 py-5 flex items-center justify-between border-b border-gray-100 bg-white/80 backdrop-blur-md sticky top-0 z-50">
                    <div className="hidden md:flex gap-10 text-sm font-bold">
                        <span className="text-green-500 border-b-2 border-green-500 pb-1">Lesson Library</span>
                        <span className="text-gray-400 hover:text-gray-800 transition-colors cursor-pointer">My Progress</span>
                        <span className="text-gray-400 hover:text-gray-800 transition-colors cursor-pointer">Downloads</span>
                    </div>
                    {/* Mobile title */}
                    <div className="md:hidden font-black text-gray-800">ParroTalk</div>

                    <div className="flex items-center gap-6 text-gray-400">
                        <div className="relative hidden sm:block">
                            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4" />
                            <input 
                                type="text" 
                                placeholder="Search lessons..." 
                                className="bg-gray-50 border border-gray-100 rounded-full pl-10 pr-4 py-2 w-64 text-sm focus:outline-none focus:ring-2 focus:ring-green-100 focus:border-green-500 text-gray-800 transition-all" 
                            />
                        </div>
                        <Bell className="w-5 h-5 cursor-pointer hover:text-gray-800 transition-colors" />
                        <Settings className="w-5 h-5 cursor-pointer hover:text-gray-800 transition-colors" />
                        <div className="w-9 h-9 rounded-full bg-green-100 text-green-600 flex items-center justify-center font-bold outline outline-offset-2 outline-white cursor-pointer hover:bg-green-200 transition-colors">
                            U
                        </div>
                    </div>
                </header>

                <div className="px-6 md:px-10 py-12 max-w-7xl w-full mx-auto flex flex-col gap-10">
                    {/* Page Headers & Filters */}
                    <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-8">
                        <div className="flex flex-col gap-3 max-w-xl">
                            <h1 className="text-4xl md:text-5xl font-black text-gray-900 tracking-tight leading-none">Lesson Library</h1>
                            <p className="text-gray-500 text-lg">Hone your dictation and pronunciation skills with our curated collection of pristine audio and video lessons.</p>
                        </div>

                        <div className="flex flex-wrap items-center gap-2">
                            {["All", "Audio", "Video", "In Progress", "Completed"].map(f => (
                                <button 
                                    key={f}
                                    onClick={() => setFilter(f)}
                                    className={`px-6 py-2.5 rounded-full text-sm font-bold transition-all active:scale-95 ${
                                        filter === f 
                                        ? 'bg-gray-900 text-white shadow-lg shadow-gray-200' 
                                        : 'bg-white border border-gray-100 text-gray-600 hover:bg-gray-50 hover:border-gray-200'
                                    }`}
                                >
                                    {f}
                                </button>
                            ))}
                        </div>
                    </div>

                    {loading ? (
                        <div className="w-full flex flex-col gap-8 py-10">
                            <div className="w-full h-[400px] bg-gray-100 rounded-[2rem] animate-pulse"></div>
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                {[1,2,3].map(i => (
                                    <div key={i} className="bg-gray-50 h-[300px] rounded-[2rem] animate-pulse"></div>
                                ))}
                            </div>
                        </div>
                    ) : (
                        <>
                            {featuredJob && filter === "All" && (
                                <FeaturedLesson job={featuredJob} />
                            )}

                            {filteredJobs.length > 0 ? (
                                <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-8">
                                    {(filter === "All" ? gridJobs : filteredJobs).map(job => (
                                        <LessonCard key={job.id} job={job} />
                                    ))}
                                </div>
                            ) : (
                                <div className="text-center py-32 bg-gray-50 rounded-[2rem] border border-gray-100 flex flex-col items-center justify-center gap-4">
                                    <div className="w-16 h-16 bg-white rounded-full flex items-center justify-center shadow-sm text-gray-400">
                                        <Search size={24} />
                                    </div>
                                    <h3 className="text-xl font-bold text-gray-900">No lessons found</h3>
                                    <p className="text-gray-500 max-w-sm">We couldn't find any lessons matching your specific filter. Try tweaking your filters or uploading more media.</p>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>
        </main>
    )
}
