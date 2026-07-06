"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import {
  BarChart3,
  BookOpen,
  CheckCircle2,
  ChevronRight,
  Clock3,
  Headphones,
  Languages,
  LogOut,
  Menu,
  Mic2,
  Play,
  Shield,
  Smartphone,
  Sparkles,
  Target,
  User as UserIcon,
  X,
  Zap,
} from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";

const navItems = [
  { label: "Courses", href: "/library" },
  { label: "Dictionary", href: "/dictionary" },
  { label: "Progress", href: "/profile" },
];

const stats = [
  { value: "10k+", label: "learners" },
  { value: "500+", label: "lessons" },
  { value: "Mobile", label: "friendly practice" },
];

const steps = [
  {
    icon: BookOpen,
    title: "Choose a lesson",
    description: "Pick bite-sized English clips matched to your listening level.",
  },
  {
    icon: Headphones,
    title: "Listen & type",
    description: "Replay tricky moments and write exactly what you hear.",
  },
  {
    icon: CheckCircle2,
    title: "Check & improve",
    description: "See instant corrections, translation, and words to review next.",
  },
];

const previews = [
  {
    icon: Mic2,
    title: "Dictation Practice",
    description: "Focused typing practice with audio controls and highlighted words.",
  },
  {
    icon: Languages,
    title: "Vietnamese Translation",
    description: "Understand meaning quickly with Vietnamese support after checking.",
  },
  {
    icon: BarChart3,
    title: "Progress Tracking",
    description: "Track daily streaks, completed lessons, and listening accuracy.",
  },
];

const benefits = [
  "Real listening practice",
  "Word-by-word audio accuracy",
  "Vietnamese translation support",
  "Mobile-first learning",
  "Build vocabulary naturally",
  "Track daily progress",
];

const pwaFeatures = [
  "Add to Home Screen",
  "Fast loading",
  "Resume recent lesson",
  "Full-screen mobile experience",
  "Daily streak reminder",
];

export default function HomePage() {
  return (
    <main className="min-h-screen overflow-hidden bg-[#fffaf0] text-slate-900 selection:bg-emerald-200/70">
      <div className="absolute inset-x-0 top-0 -z-10 h-[760px] bg-[radial-gradient(circle_at_top_left,rgba(16,185,129,0.22),transparent_34%),radial-gradient(circle_at_top_right,rgba(217,249,157,0.55),transparent_30%),linear-gradient(180deg,#fff7e7_0%,#f7fffb_58%,#fffaf0_100%)]" />
      <Header />
      <HeroSection />
      <StatsSection />
      <HowItWorksSection />
      <PracticePreviewSection />
      <BenefitsSection />
      <PwaSection />
      <FinalCtaSection />
      <Footer />
    </main>
  );
}

function Header() {
  const { user, isAuthenticated, logout } = useAuth();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target as Node)) {
        setIsUserMenuOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <header className="sticky top-0 z-50 border-b border-white/60 bg-[#fffaf0]/85 backdrop-blur-xl">
      <nav className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-4 sm:px-6 lg:px-8" aria-label="Main navigation">
        <Link href="/" className="flex items-center gap-3" aria-label="ParroTalk home">
          <Image src="/logo.png" alt="ParroTalk logo" width={44} height={44} className="rounded-2xl" priority />
          <span className="text-xl font-black tracking-tight text-slate-900">ParroTalk</span>
        </Link>

        <div className="hidden items-center gap-8 md:flex">
          {navItems.map((item) => (
            <Link key={item.label} href={item.href} className="text-sm font-bold text-slate-600 transition-colors hover:text-emerald-600">
              {item.label}
            </Link>
          ))}
        </div>

        <div className="hidden items-center gap-3 md:flex">
          <AuthAction user={user} isAuthenticated={isAuthenticated} logout={logout} isUserMenuOpen={isUserMenuOpen} setIsUserMenuOpen={setIsUserMenuOpen} userMenuRef={userMenuRef} />
        </div>

        <button
          type="button"
          className="inline-flex h-12 w-12 items-center justify-center rounded-2xl border border-emerald-100 bg-white text-slate-700 shadow-sm md:hidden"
          aria-label="Toggle navigation menu"
          aria-expanded={isMobileMenuOpen}
          onClick={() => setIsMobileMenuOpen((value) => !value)}
        >
          {isMobileMenuOpen ? <X size={22} /> : <Menu size={22} />}
        </button>
      </nav>

      {isMobileMenuOpen && (
        <div className="mx-4 mb-4 rounded-3xl border border-emerald-100 bg-white p-3 shadow-xl shadow-emerald-900/5 md:hidden">
          <div className="grid gap-1">
            {navItems.map((item) => (
              <Link key={item.label} href={item.href} onClick={() => setIsMobileMenuOpen(false)} className="rounded-2xl px-4 py-3 text-base font-bold text-slate-700 hover:bg-emerald-50">
                {item.label}
              </Link>
            ))}
          </div>
          <div className="mt-3 border-t border-slate-100 pt-3">
            <AuthAction user={user} isAuthenticated={isAuthenticated} logout={logout} compact />
          </div>
        </div>
      )}
    </header>
  );
}

type AuthUser = {
  fullName?: string | null;
  role?: string | null;
};

type AuthActionProps = {
  user?: AuthUser | null;
  isAuthenticated: boolean;
  logout: () => void;
  isUserMenuOpen?: boolean;
  setIsUserMenuOpen?: React.Dispatch<React.SetStateAction<boolean>>;
  userMenuRef?: React.RefObject<HTMLDivElement | null>;
  compact?: boolean;
};

function AuthAction({ user, isAuthenticated, logout, isUserMenuOpen, setIsUserMenuOpen, userMenuRef, compact = false }: AuthActionProps) {
  if (!isAuthenticated) {
    return (
      <Link href="/login" className={`${compact ? "flex w-full justify-center" : "inline-flex"} rounded-2xl bg-slate-900 px-5 py-3 text-sm font-black text-white shadow-lg shadow-slate-900/10 transition hover:bg-emerald-600`}>
        Sign In
      </Link>
    );
  }

  if (compact) {
    return (
      <div className="grid gap-2">
        {user?.role === "ADMIN" && <Link href="/admin" className="rounded-2xl bg-emerald-50 px-4 py-3 text-sm font-bold text-emerald-700">Admin</Link>}
        <button type="button" onClick={logout} className="rounded-2xl bg-red-50 px-4 py-3 text-left text-sm font-bold text-red-600">Log out</button>
      </div>
    );
  }

  return (
    <div className="relative" ref={userMenuRef}>
      <button type="button" onClick={() => setIsUserMenuOpen?.((prev) => !prev)} className="flex items-center gap-2 rounded-2xl border border-emerald-100 bg-white px-3 py-2 shadow-sm transition hover:border-emerald-200">
        <span className="flex h-9 w-9 items-center justify-center rounded-full bg-emerald-100 text-sm font-black uppercase text-emerald-700">{user?.fullName?.charAt(0) || <UserIcon size={16} />}</span>
        <span className="max-w-32 truncate text-sm font-bold text-slate-700">{user?.fullName}</span>
      </button>
      {isUserMenuOpen && (
        <div className="absolute right-0 mt-2 w-56 rounded-3xl border border-slate-100 bg-white p-2 shadow-2xl shadow-slate-900/10">
          {user?.role === "ADMIN" && <Link href="/admin" className="flex items-center gap-3 rounded-2xl px-3 py-3 text-sm font-bold text-emerald-700 hover:bg-emerald-50"><Shield size={16} /> Admin</Link>}
          <button type="button" onClick={logout} className="flex w-full items-center gap-3 rounded-2xl px-3 py-3 text-left text-sm font-bold text-red-600 hover:bg-red-50"><LogOut size={16} /> Log out</button>
        </div>
      )}
    </div>
  );
}

function HeroSection() {
  return (
    <section className="mx-auto grid max-w-7xl items-center gap-12 px-4 pb-16 pt-12 sm:px-6 lg:grid-cols-[1fr_0.92fr] lg:px-8 lg:pb-24 lg:pt-20">
      <div className="max-w-3xl">
        <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-emerald-200 bg-white/80 px-4 py-2 text-xs font-black uppercase tracking-[0.22em] text-emerald-700 shadow-sm">
          <Sparkles size={15} /> English dictation for Vietnamese learners
        </div>
        <h1 className="text-5xl font-black leading-[0.98] tracking-tight text-slate-950 sm:text-6xl lg:text-7xl">
          Master English Listening, One Dictation at a Time.
        </h1>
        <p className="mt-6 max-w-2xl text-lg leading-8 text-slate-600 sm:text-xl">
          Practice with real English videos. Listen, type what you hear, check instantly, and build vocabulary naturally.
        </p>
        <div className="mt-8 flex flex-col gap-3 sm:flex-row">
          <Link href="/library" className="inline-flex min-h-14 items-center justify-center gap-2 rounded-2xl bg-emerald-500 px-7 py-4 text-base font-black text-white shadow-xl shadow-emerald-500/25 transition hover:-translate-y-0.5 hover:bg-emerald-600">
            Start Practicing Free <ChevronRight size={20} />
          </Link>
          <Link href="#demo" className="inline-flex min-h-14 items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white px-7 py-4 text-base font-black text-slate-800 shadow-sm transition hover:-translate-y-0.5 hover:border-emerald-200 hover:text-emerald-700">
            <Play size={18} /> Watch Demo
          </Link>
        </div>
      </div>
      <DictationMockup />
    </section>
  );
}

function DictationMockup() {
  return (
    <div id="demo" className="relative mx-auto w-full max-w-xl scroll-mt-24">
      <div className="absolute -left-8 top-12 h-32 w-32 rounded-full bg-lime-200/70 blur-3xl" />
      <div className="absolute -right-8 bottom-8 h-40 w-40 rounded-full bg-emerald-200/70 blur-3xl" />
      <div className="relative rounded-[2rem] border border-white bg-white/90 p-4 shadow-2xl shadow-emerald-900/12 sm:rounded-[2.5rem] sm:p-6">
        <div className="rounded-[1.5rem] bg-slate-950 p-4 text-white sm:p-5">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.22em] text-emerald-300">Lesson 12</p>
              <h2 className="mt-1 text-lg font-black">Daily Conversation</h2>
            </div>
            <div className="rounded-2xl bg-lime-300 px-3 py-2 text-sm font-black text-slate-950">🔥 7 day</div>
          </div>
          <div className="mt-5 rounded-2xl bg-white/10 p-4">
            <div className="flex items-center gap-3">
              <button type="button" aria-label="Play sample audio" className="flex h-12 w-12 items-center justify-center rounded-full bg-emerald-400 text-slate-950"><Play size={20} fill="currentColor" /></button>
              <div className="flex-1">
                <div className="mb-2 flex justify-between text-xs font-bold text-slate-300"><span>00:14</span><span>00:32</span></div>
                <div className="h-2 rounded-full bg-white/15"><div className="h-2 w-2/5 rounded-full bg-emerald-300" /></div>
              </div>
            </div>
            <div className="mt-4 flex items-end gap-1" aria-label="Audio waveform">
              {[30, 52, 40, 70, 46, 84, 62, 38, 76, 50, 65, 42, 58, 34].map((height, index) => (
                <span key={index} className="flex-1 rounded-full bg-emerald-300/80" style={{ height: `${height}px` }} />
              ))}
            </div>
          </div>
        </div>
        <div className="mt-4 rounded-[1.5rem] border border-emerald-100 bg-emerald-50/70 p-4">
          <p className="text-sm font-bold text-slate-500">Current sentence</p>
          <p className="mt-2 text-xl font-black leading-snug text-slate-900">
            I <span className="rounded-lg bg-lime-200 px-1.5">usually</span> practice English before work.
          </p>
          <label htmlFor="dictation-preview" className="sr-only">Dictation answer preview</label>
          <input id="dictation-preview" readOnly value="I usually practice English before..." className="mt-4 w-full rounded-2xl border border-white bg-white px-4 py-4 text-base font-semibold text-slate-700 shadow-inner outline-none" />
          <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <button type="button" className="min-h-12 rounded-2xl bg-slate-900 px-5 py-3 text-sm font-black text-white">Check Answer</button>
            <div className="flex items-center gap-2 text-sm font-bold text-emerald-700"><Target size={18} /> 86% accuracy</div>
          </div>
        </div>
      </div>
    </div>
  );
}

function StatsSection() {
  return <section className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8"><div className="grid gap-3 rounded-[2rem] border border-emerald-100 bg-white/80 p-3 shadow-xl shadow-emerald-900/5 sm:grid-cols-3">{stats.map((stat) => <div key={stat.label} className="rounded-3xl bg-gradient-to-br from-emerald-50 to-lime-50 px-6 py-5 text-center"><p className="text-3xl font-black text-slate-950">{stat.value}</p><p className="mt-1 text-sm font-bold text-slate-500">{stat.label}</p></div>)}</div></section>;
}

function SectionHeader({ eyebrow, title, description }: { eyebrow: string; title: string; description: string }) {
  return <div className="mx-auto max-w-2xl text-center"><p className="text-sm font-black uppercase tracking-[0.22em] text-emerald-600">{eyebrow}</p><h2 className="mt-3 text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">{title}</h2><p className="mt-4 text-base leading-7 text-slate-600">{description}</p></div>;
}

function HowItWorksSection() {
  return <section className="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8"><SectionHeader eyebrow="How it works" title="Understand the flow in seconds" description="ParroTalk turns every lesson into a simple loop: listen carefully, type confidently, and improve with feedback." /><div className="mt-10 grid gap-5 md:grid-cols-3">{steps.map((step, index) => <Card key={step.title} icon={step.icon} title={step.title} description={step.description} badge={`0${index + 1}`} />)}</div></section>;
}

function PracticePreviewSection() {
  return <section className="mx-auto max-w-7xl px-4 pb-20 sm:px-6 lg:px-8"><SectionHeader eyebrow="Practice preview" title="Everything you need for focused listening" description="Short lessons, helpful translations, and clear progress signals keep practice lightweight and motivating." /><div className="mt-10 grid gap-5 md:grid-cols-3">{previews.map((preview) => <Card key={preview.title} icon={preview.icon} title={preview.title} description={preview.description} />)}</div></section>;
}

function BenefitsSection() {
  return <section className="bg-white/65 py-20"><div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8"><SectionHeader eyebrow="Why ParroTalk" title="Built for Vietnamese learners who want real listening gains" description="Clear feedback, friendly pacing, and mobile-first lessons make consistent English listening practice easier." /><div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{benefits.map((benefit) => <div key={benefit} className="flex items-center gap-3 rounded-3xl border border-emerald-100 bg-white p-5 shadow-sm"><CheckCircle2 className="shrink-0 text-emerald-500" size={22} /><span className="font-black text-slate-800">{benefit}</span></div>)}</div></div></section>;
}

function PwaSection() {
  return <section className="mx-auto grid max-w-7xl items-center gap-10 px-4 py-20 sm:px-6 lg:grid-cols-2 lg:px-8"><div><p className="text-sm font-black uppercase tracking-[0.22em] text-emerald-600">Mobile / PWA ready</p><h2 className="mt-3 text-4xl font-black tracking-tight text-slate-950 sm:text-5xl">Practice anywhere, like a mobile app.</h2><p className="mt-5 max-w-xl text-lg leading-8 text-slate-600">Open ParroTalk on your phone, jump back into a recent lesson, and keep your listening streak alive wherever you are.</p><div className="mt-8 grid gap-3 sm:grid-cols-2">{pwaFeatures.map((feature) => <div key={feature} className="flex items-center gap-3 rounded-2xl bg-white px-4 py-3 font-bold text-slate-700 shadow-sm"><Zap size={18} className="text-lime-500" />{feature}</div>)}</div></div><div className="mx-auto w-full max-w-sm rounded-[3rem] border-[10px] border-slate-900 bg-slate-900 p-3 shadow-2xl shadow-slate-900/20"><div className="rounded-[2.2rem] bg-gradient-to-br from-emerald-50 to-lime-50 p-5"><div className="mx-auto mb-5 h-1.5 w-20 rounded-full bg-slate-300" /><div className="rounded-3xl bg-white p-5 shadow-sm"><div className="flex items-center justify-between"><Smartphone className="text-emerald-600" /><span className="rounded-full bg-lime-200 px-3 py-1 text-xs font-black text-slate-800">PWA</span></div><h3 className="mt-6 text-2xl font-black text-slate-950">Resume Lesson</h3><p className="mt-2 text-sm font-semibold text-slate-500">Business English · 8 min left</p><div className="mt-6 h-3 rounded-full bg-slate-100"><div className="h-3 w-3/5 rounded-full bg-emerald-500" /></div><button type="button" className="mt-6 w-full rounded-2xl bg-emerald-500 px-5 py-4 font-black text-white">Continue Practice</button></div><div className="mt-4 grid grid-cols-2 gap-3"><div className="rounded-2xl bg-white p-4 text-center"><Clock3 className="mx-auto text-emerald-500" /><p className="mt-2 text-sm font-black">12 min</p></div><div className="rounded-2xl bg-white p-4 text-center"><BarChart3 className="mx-auto text-lime-500" /><p className="mt-2 text-sm font-black">+18 words</p></div></div></div></div></section>;
}

function FinalCtaSection() {
  return <section className="mx-auto max-w-7xl px-4 pb-20 sm:px-6 lg:px-8"><div className="rounded-[2rem] bg-slate-950 px-6 py-12 text-center text-white shadow-2xl shadow-slate-900/20 sm:px-10 sm:py-16"><h2 className="text-4xl font-black tracking-tight sm:text-5xl">Ready to train your ears?</h2><p className="mx-auto mt-5 max-w-2xl text-lg leading-8 text-slate-300">Start your first dictation lesson today and improve your English listening with focused practice.</p><Link href="/library" className="mt-8 inline-flex min-h-14 items-center justify-center gap-2 rounded-2xl bg-emerald-400 px-7 py-4 font-black text-slate-950 transition hover:bg-lime-300">Start Practicing Free <ChevronRight size={20} /></Link></div></section>;
}

function Footer() {
  return <footer className="border-t border-emerald-100 bg-white/70"><div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:px-6 md:grid-cols-[1.2fr_1fr] lg:px-8"><div><Link href="/" className="inline-flex items-center gap-3"><Image src="/logo.png" alt="ParroTalk logo" width={40} height={40} className="rounded-2xl" /><span className="text-xl font-black text-slate-950">ParroTalk</span></Link><p className="mt-4 max-w-md text-sm leading-6 text-slate-600">English dictation practice for Vietnamese learners. Listen, type, check, translate, and grow your vocabulary naturally.</p><p className="mt-5 text-sm font-semibold text-slate-400">© 2026 ParroTalk. All rights reserved.</p></div><div className="flex flex-wrap items-start gap-4 md:justify-end">{[...navItems, { label: "Sign In", href: "/login" }].map((item) => <Link key={item.label} href={item.href} className="rounded-full px-3 py-2 text-sm font-bold text-slate-600 transition hover:bg-emerald-50 hover:text-emerald-700">{item.label}</Link>)}</div></div></footer>;
}

function Card({ icon: Icon, title, description, badge }: { icon: React.ElementType; title: string; description: string; badge?: string }) {
  return <article className="group rounded-[1.75rem] border border-emerald-100 bg-white p-6 shadow-sm transition hover:-translate-y-1 hover:shadow-xl hover:shadow-emerald-900/8"><div className="flex items-center justify-between"><div className="flex h-13 w-13 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700"><Icon size={24} /></div>{badge && <span className="text-sm font-black text-lime-600">{badge}</span>}</div><h3 className="mt-6 text-xl font-black text-slate-950">{title}</h3><p className="mt-3 text-sm leading-6 text-slate-600">{description}</p></article>;
}
