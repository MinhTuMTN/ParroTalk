"use client";

import Image from "next/image";
import Link from "next/link";
import { Headphones, GraduationCap, ChevronRight, User as UserIcon, LogOut } from "lucide-react";
import { useAuth } from "@/context/AuthContext";

export default function Home() {
    const { user, isAuthenticated, logout } = useAuth();

    return (
        <main className="min-h-screen bg-[#FDFDFD] selection:bg-green-100">
            {/* Navigation */}
            <nav className="max-w-7xl mx-auto px-6 py-8 flex items-center justify-between">
                <div className="flex items-center gap-2">
                    <div className="relative w-80 h-16">
                        <Image
                            src="/logo_long.png"
                            alt="ParroTalk"
                            fill
                            sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
                            className="object-contain"
                            priority
                        />
                    </div>
                </div>
                <div className="hidden md:flex items-center gap-8 text-sm font-bold text-gray-500">
                    <Link href="/library" className="hover:text-green-500 transition-colors">Courses</Link>
                    <Link href="#" className="hover:text-green-500 transition-colors">Dictionary</Link>
                    <Link href="#" className="hover:text-green-500 transition-colors">Progress</Link>
                </div>

                {isAuthenticated ? (
                    <div className="flex items-center gap-4">
                        <div className="flex items-center gap-2 px-4 py-2 bg-gray-50 rounded-2xl border border-gray-100">
                            <div className="w-8 h-8 rounded-full bg-green-100 text-green-600 flex items-center justify-center font-bold text-xs uppercase">
                                {user?.fullName?.charAt(0) || "U"}
                            </div>
                            <span className="text-sm font-bold text-gray-700 hidden sm:block">{user?.fullName}</span>
                        </div>
                        <button
                            onClick={logout}
                            className="p-3 rounded-2xl bg-white border-2 border-gray-100 text-gray-400 hover:text-red-500 hover:border-red-100 transition-all shadow-sm"
                            title="Logout"
                        >
                            <LogOut size={20} />
                        </button>
                    </div>
                ) : (
                    <Link
                        href="/login"
                        className="px-6 py-3 rounded-2xl bg-white border-2 border-gray-100 text-gray-800 font-bold hover:bg-gray-50 transition-all text-sm shadow-sm"
                    >
                        Sign In
                    </Link>
                )}
            </nav>

            {/* Hero Section */}
            <section className="max-w-7xl mx-auto px-6 py-12 md:py-24 grid md:grid-cols-2 gap-16 items-center">
                <div className="flex flex-col gap-8">
                    <div className="inline-flex items-center gap-2 px-4 py-2 bg-green-50 rounded-full text-green-600 text-xs font-black uppercase tracking-widest">
                        <GraduationCap size={14} />
                        Master English Listening
                    </div>

                    <h1 className="text-6xl md:text-7xl font-black text-gray-900 leading-[1.1]">
                        Master English <br />
                        <span className="text-green-500 underline decoration-green-100 underline-offset-8">through Dictation</span>.
                    </h1>

                    <p className="text-xl text-gray-500 leading-relaxed max-w-lg">
                        Improve your listening skills and vocabulary with our bite-sized daily dictation exercises. Follow along, type what you hear, and level up your English.
                    </p>

                    <div className="flex flex-col sm:flex-row gap-4">
                        <Link
                            href="/library"
                            className="group bg-green-500 text-white px-8 py-5 rounded-2xl text-lg font-bold shadow-xl shadow-green-100 hover:bg-green-600 transition-all flex items-center justify-center gap-2 active:scale-95"
                        >
                            Start Practicing
                            <ChevronRight className="group-hover:translate-x-1 transition-transform" />
                        </Link>
                        <button className="bg-white border-2 border-gray-100 text-gray-800 px-8 py-5 rounded-2xl text-lg font-bold hover:bg-gray-50 transition-all active:scale-95">
                            View Demo
                        </button>
                    </div>

                    <div className="flex items-center gap-6 pt-4">
                        <div className="flex -space-x-3">
                            {[1, 2, 3, 4].map(i => (
                                <div key={i} className={`w-10 h-10 rounded-full border-4 border-white bg-gray-200 overflow-hidden`}>
                                    <img src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${i}`} alt="user" />
                                </div>
                            ))}
                        </div>
                        <div className="text-sm">
                            <span className="font-bold text-gray-800">10k+ learners</span>
                            <p className="text-gray-400">Mastering English everyday</p>
                        </div>
                    </div>
                </div>

                <div className="relative">
                    {/* Abstract UI Elements to "Wow" the user */}
                    <div className="absolute -top-12 -left-12 w-64 h-64 bg-green-100 rounded-full blur-[100px] opacity-60 animate-pulse" />
                    <div className="absolute -bottom-12 -right-12 w-64 h-64 bg-blue-100 rounded-full blur-[100px] opacity-60 animate-pulse delay-700" />

                    <div className="relative bg-white p-8 rounded-[3rem] shadow-2xl border border-gray-100 transform rotate-2 hover:rotate-0 transition-all duration-700 hover:scale-105">
                        <div className="flex flex-col gap-6">
                            <div className="flex items-center justify-between">
                                <div className="h-4 w-24 bg-gray-100 rounded-full" />
                                <div className="h-4 w-4 bg-green-400 rounded-full" />
                            </div>

                            <div className="flex flex-col gap-3">
                                <div className="h-3 w-full bg-gray-50 rounded-full" />
                                <div className="h-3 w-4/5 bg-gray-50 rounded-full" />
                                <div className="h-3 w-3/4 bg-gray-50 rounded-full" />
                            </div>

                            <div className="aspect-video bg-green-50 rounded-2xl flex items-center justify-center text-green-500">
                                <Headphones size={64} strokeWidth={1} />
                            </div>

                            <div className="py-4 px-6 bg-green-500 rounded-2xl text-white font-bold flex items-center justify-between">
                                <span>Check Answer</span>
                                <ChevronRight />
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* Footer / Features mini section */}
            <footer className="max-w-7xl mx-auto px-6 py-12 border-t border-gray-100 mt-24">
                <div className="flex flex-col md:flex-row justify-between items-center gap-8 text-gray-400 text-sm">
                    <p>© 2026 ParroTalk. All rights reserved.</p>
                    <div className="flex gap-8 font-medium">
                        <Link href="#" className="hover:text-green-500 transition-colors">Privacy Policy</Link>
                        <Link href="#" className="hover:text-green-500 transition-colors">Terms of Service</Link>
                        <Link href="#" className="hover:text-green-500 transition-colors">Contact Us</Link>
                    </div>
                </div>
            </footer>
        </main>
    );
}
