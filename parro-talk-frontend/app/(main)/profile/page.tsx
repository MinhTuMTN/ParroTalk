"use client";

import { useEffect, useState } from "react";
import PracticeHeader from "@/components/practice/PracticeHeader";
import { lessonService, UserProfileResponse } from "@/lib/services/lessonService";
import { Trophy, Star, Target, Flame, Calendar as CalendarIcon, Loader2 } from "lucide-react";
import { format, subDays, parseISO, isSameDay } from "date-fns";

export default function ProfilePage() {
    const [profile, setProfile] = useState<UserProfileResponse | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchProfile = async () => {
            try {
                const data = await lessonService.getUserProfile();
                setProfile(data);
            } catch (err) {
                console.error("Failed to load profile", err);
            } finally {
                setLoading(false);
            }
        };
        fetchProfile();
    }, []);

    // Last 14 days activity visualizer
    const today = new Date();
    const last14Days = Array.from({ length: 14 }).map((_, i) => subDays(today, 13 - i));
    
    // Parse active days
    const activeDates = profile?.activeDays?.map(d => parseISO(d)) || [];

    return (
        <div className="flex flex-col min-h-screen bg-gray-50 border-r border-gray-100">
            {/* Minimal Header */}
            <header className="h-16 bg-white border-b border-gray-100 px-6 flex items-center shadow-sm">
                <h1 className="text-xl font-black text-gray-800">My Progress</h1>
            </header>

            <div className="flex-1 overflow-y-auto p-8 lg:p-12">
                {loading ? (
                    <div className="flex items-center justify-center h-full">
                        <Loader2 className="w-10 h-10 animate-spin text-green-500" />
                    </div>
                ) : !profile ? (
                    <div className="flex flex-col items-center justify-center h-full text-gray-500">
                        <p>No profile data available.</p>
                    </div>
                ) : (
                    <div className="max-w-4xl mx-auto flex flex-col gap-8">
                        {/* Profile Header */}
                        <div className="bg-white rounded-3xl p-8 border border-gray-100 shadow-sm flex items-center gap-6">
                            <div className="w-24 h-24 rounded-full bg-green-100 text-green-600 flex items-center justify-center font-black text-4xl shadow-inner border-4 border-white outline outline-1 outline-gray-100">
                                {profile.fullName.charAt(0) || "U"}
                            </div>
                            <div>
                                <h1 className="text-3xl font-black text-gray-900 mb-1">{profile.fullName}</h1>
                                <p className="text-gray-500">{profile.email}</p>
                            </div>
                        </div>

                        {/* Stats Grid */}
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                            <StatCard 
                                icon={<Target size={24} />} 
                                title="Lessons" 
                                value={profile.totalLessonsCompleted.toString()} 
                                color="bg-blue-100 text-blue-600" 
                            />
                            <StatCard 
                                icon={<Star size={24} />} 
                                title="Avg Score" 
                                value={profile.avgScore.toFixed(1)} 
                                color="bg-yellow-100 text-yellow-600" 
                            />
                            <StatCard 
                                icon={<Trophy size={24} />} 
                                title="Total Score" 
                                value={profile.totalScore.toFixed(1)} 
                                color="bg-purple-100 text-purple-600" 
                            />
                            <StatCard 
                                icon={<Flame size={24} />} 
                                title="Streak" 
                                value={`${profile.currentStreak} Days`} 
                                subValue={`Best: ${profile.longestStreak}`}
                                color="bg-orange-100 text-orange-600" 
                            />
                        </div>

                        {/* Activity Graph */}
                        <div className="bg-white rounded-3xl p-8 border border-gray-100 shadow-sm">
                            <div className="flex items-center gap-3 mb-6 border-b border-gray-50 pb-4">
                                <div className="p-2 bg-green-50 text-green-600 rounded-xl">
                                    <CalendarIcon size={20} />
                                </div>
                                <h2 className="text-xl font-bold text-gray-900">Activity (Last 14 Days)</h2>
                            </div>
                            
                            <div className="flex gap-2 justify-between max-w-2xl overflow-x-auto pb-4">
                                {last14Days.map((day, i) => {
                                    const isActive = activeDates.some(ad => isSameDay(ad, day));
                                    const isToday = isSameDay(day, today);
                                    
                                    return (
                                        <div key={i} className="flex flex-col items-center gap-2 flex-shrink-0">
                                            <div 
                                                className={`w-10 h-10 rounded-xl flex items-center justify-center font-bold text-sm transition-all
                                                    ${isActive ? 'bg-green-500 text-white shadow-md' : 'bg-gray-100 text-gray-400'}
                                                    ${isToday && !isActive ? 'ring-2 ring-gray-200' : ''}
                                                `}
                                            >
                                                {format(day, 'd')}
                                            </div>
                                            <span className="text-[10px] font-bold text-gray-400 uppercase">
                                                {format(day, 'E')}
                                            </span>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>

                    </div>
                )}
            </div>
        </div>
    );
}

function StatCard({ icon, title, value, subValue, color }: { icon: React.ReactNode, title: string, value: string, subValue?: string, color: string }) {
    return (
        <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm flex flex-col items-start gap-4 hover:-translate-y-1 hover:shadow-md transition-all">
            <div className={`p-3 rounded-2xl ${color}`}>
                {icon}
            </div>
            <div>
                <p className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-1">{title}</p>
                <div className="flex items-end gap-2">
                    <h3 className="text-2xl font-black text-gray-800 leading-none">{value}</h3>
                    {subValue && <span className="text-xs font-bold text-gray-400 mb-1">{subValue}</span>}
                </div>
            </div>
        </div>
    );
}
