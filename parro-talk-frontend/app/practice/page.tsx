'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { practiceApi } from '@/features/practice/services/api';
import { PracticeStatistics } from '@/features/practice/types';
import { Brain, Flame, Target, Trophy, Clock } from 'lucide-react';

export default function PracticeDashboard() {
  const router = useRouter();
  const [stats, setStats] = useState<PracticeStatistics | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    practiceApi.getStatistics().then(setStats).catch(console.error);
  }, []);

  const startSession = async () => {
    setLoading(true);
    try {
      const session = await practiceApi.startSession();
      sessionStorage.setItem(`practice_session_${session.sessionId}`, JSON.stringify(session));
      router.push(`/practice/session/${session.sessionId}`);
    } catch (error) {
      console.error('Failed to start session', error);
      alert('Failed to start session. Maybe no due words?');
    } finally {
      setLoading(false);
    }
  };

  if (!stats) return <div className="p-8 text-center animate-pulse">Loading dashboard...</div>;

  return (
    <div className="container max-w-5xl mx-auto p-4 md:p-8 space-y-12">
      
      <div className="flex flex-col md:flex-row items-center justify-between gap-6 p-8 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-3xl text-white shadow-xl">
        <div className="space-y-2 text-center md:text-left">
          <h1 className="text-4xl font-bold tracking-tight">Vocabulary Practice</h1>
          <p className="text-indigo-100 text-lg">You have {stats.reviewDue} words to review today.</p>
        </div>
        <Button 
          size="lg"
          onClick={startSession}
          disabled={loading}
          className="bg-white text-indigo-600 hover:bg-indigo-50 py-8 px-12 text-2xl font-bold rounded-2xl shadow-lg transition-transform hover:scale-105 active:scale-95"
        >
          {loading ? 'Starting...' : 'Start Review'}
        </Button>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 md:gap-6">
        <StatCard icon={<Clock />} label="Due Review" value={stats.reviewDue.toString()} color="text-amber-500" bg="bg-amber-100 dark:bg-amber-900/30" />
        <StatCard icon={<Brain />} label="Today Learned" value={stats.todayLearned.toString()} color="text-blue-500" bg="bg-blue-100 dark:bg-blue-900/30" />
        <StatCard icon={<Trophy />} label="Total Mastered" value={stats.totalMastered.toString()} color="text-emerald-500" bg="bg-emerald-100 dark:bg-emerald-900/30" />
        <StatCard icon={<Flame />} label="Current Streak" value={`${stats.currentStreak} Days`} color="text-orange-500" bg="bg-orange-100 dark:bg-orange-900/30" />
      </div>

      <div className="p-8 bg-white dark:bg-slate-900 rounded-3xl shadow-sm border border-slate-100 dark:border-slate-800 space-y-6">
        <h2 className="text-2xl font-bold text-slate-800 dark:text-white flex items-center gap-2">
          <Target className="text-indigo-500" /> Daily Goal
        </h2>
        
        <div className="space-y-2">
          <div className="flex justify-between text-sm font-medium text-slate-600 dark:text-slate-400">
            <span>Progress</span>
            <span>{Math.min(100, Math.round((stats.todayLearned / 20) * 100))}%</span>
          </div>
          <div className="h-4 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
            <div 
              className="h-full bg-gradient-to-r from-indigo-500 to-purple-500 rounded-full transition-all duration-1000 ease-out"
              style={{ width: `${Math.min(100, (stats.todayLearned / 20) * 100)}%` }}
            />
          </div>
          <p className="text-xs text-slate-500 text-right">Goal: 20 words / day</p>
        </div>
      </div>
    </div>
  );
}

function StatCard({ icon, label, value, color, bg }: { icon: React.ReactNode, label: string, value: string, color: string, bg: string }) {
  return (
    <div className="p-6 bg-white dark:bg-slate-900 rounded-3xl shadow-sm border border-slate-100 dark:border-slate-800 flex flex-col items-center justify-center text-center space-y-3 hover:shadow-md transition-shadow">
      <div className={`p-4 rounded-2xl ${bg} ${color}`}>
        {React.cloneElement(icon as React.ReactElement<{ className?: string }>, { className: 'w-8 h-8' })}
      </div>
      <div>
        <p className="text-sm text-slate-500 font-medium">{label}</p>
        <p className="text-2xl font-bold text-slate-800 dark:text-white mt-1">{value}</p>
      </div>
    </div>
  );
}
