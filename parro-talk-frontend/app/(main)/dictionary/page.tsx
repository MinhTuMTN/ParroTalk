"use client";

import Image from "next/image";
import Link from "next/link";
import { useMemo, useState } from "react";
import {
  ArrowRight,
  BookOpen,
  BriefcaseBusiness,
  ChevronDown,
  FileText,
  Grid2X2,
  GraduationCap,
  Heart,
  List,
  Plane,
  Play,
  RefreshCw,
  Search,
  Sparkles,
  Star,
  Trophy,
  Upload,
  Volume2,
  MessageCircle,
} from "lucide-react";

type CefrLevel = "A1" | "A2" | "B1" | "B2" | "C1" | "C2";
type WordSetCategory = "Oxford" | "Exams" | "Topics" | "From Lessons" | "Saved" | "AI Generated";
type IconTone = "emerald" | "violet" | "blue" | "orange" | "cyan" | "amber" | "rose";

type WordSet = {
  id: string;
  title: string;
  description: string;
  levels: CefrLevel[];
  totalWords: number;
  learnedWords: number;
  dueWords: number;
  category: WordSetCategory;
  icon: typeof GraduationCap;
  tone: IconTone;
};

type ContinueLearningSet = {
  id: string;
  title: string;
  progressText: string;
  percentage: number;
  dueWords: number;
  icon: typeof GraduationCap;
  tone: IconTone;
};

type RecommendedWord = {
  id: string;
  word: string;
  phonetic: string;
  partOfSpeech: string;
  meaningVi: string;
  level: CefrLevel;
};

const WORD_SETS: WordSet[] = [
  {
    id: "oxford-3000",
    title: "Oxford 3000",
    description: "Essential English words every learner should know.",
    levels: ["A1", "A2", "B1", "B2"],
    totalWords: 3000,
    learnedWords: 124,
    dueWords: 18,
    category: "Oxford",
    icon: GraduationCap,
    tone: "emerald",
  },
  {
    id: "oxford-5000",
    title: "Oxford 5000",
    description: "Upper-intermediate and advanced words.",
    levels: ["B2", "C1"],
    totalWords: 5000,
    learnedWords: 42,
    dueWords: 5,
    category: "Oxford",
    icon: Star,
    tone: "violet",
  },
  {
    id: "ielts-academic",
    title: "IELTS Academic",
    description: "Words you need for IELTS success.",
    levels: ["B1", "B2", "C1"],
    totalWords: 800,
    learnedWords: 32,
    dueWords: 7,
    category: "Exams",
    icon: Trophy,
    tone: "blue",
  },
  {
    id: "toeic-600",
    title: "TOEIC 600",
    description: "High-frequency words for TOEIC tests.",
    levels: ["A2", "B1", "B2"],
    totalWords: 600,
    learnedWords: 28,
    dueWords: 6,
    category: "Exams",
    icon: BriefcaseBusiness,
    tone: "orange",
  },
  {
    id: "business-english",
    title: "Business English",
    description: "Essential words for work and career.",
    levels: ["A2", "B1", "B2"],
    totalWords: 500,
    learnedWords: 56,
    dueWords: 9,
    category: "Topics",
    icon: BriefcaseBusiness,
    tone: "blue",
  },
  {
    id: "travel-english",
    title: "Travel English",
    description: "Useful words for travel and tourism.",
    levels: ["A1", "A2", "B1"],
    totalWords: 400,
    learnedWords: 34,
    dueWords: 4,
    category: "Topics",
    icon: Plane,
    tone: "cyan",
  },
  {
    id: "daily-conversation",
    title: "Daily Conversation",
    description: "Common words in everyday conversations.",
    levels: ["A1", "A2", "B1"],
    totalWords: 1200,
    learnedWords: 120,
    dueWords: 15,
    category: "Topics",
    icon: MessageCircle,
    tone: "amber",
  },
  {
    id: "words-from-lessons",
    title: "Words from Lessons",
    description: "Words extracted from your uploaded lessons.",
    levels: ["B1", "B2", "C1"],
    totalWords: 320,
    learnedWords: 48,
    dueWords: 6,
    category: "From Lessons",
    icon: FileText,
    tone: "rose",
  },
  {
    id: "saved-words",
    title: "Saved Words",
    description: "Your bookmarked words from lessons and practice.",
    levels: ["A1", "A2", "B1", "B2", "C1"],
    totalWords: 214,
    learnedWords: 89,
    dueWords: 12,
    category: "Saved",
    icon: Heart,
    tone: "rose",
  },
  {
    id: "ai-generated",
    title: "AI Generated Words",
    description: "Smart word sets generated from your learning gaps.",
    levels: ["A2", "B1", "B2"],
    totalWords: 180,
    learnedWords: 21,
    dueWords: 3,
    category: "AI Generated",
    icon: Sparkles,
    tone: "violet",
  },
];

const CONTINUE_LEARNING_SETS: ContinueLearningSet[] = [
  {
    id: "oxford-3000",
    title: "Oxford 3000",
    progressText: "124 / 3000 learned",
    percentage: 41,
    dueWords: 18,
    icon: GraduationCap,
    tone: "emerald",
  },
  {
    id: "ielts-academic",
    title: "IELTS Academic",
    progressText: "32 / 800 learned",
    percentage: 40,
    dueWords: 7,
    icon: Star,
    tone: "violet",
  },
  {
    id: "business-english",
    title: "Business English",
    progressText: "56 / 500 learned",
    percentage: 56,
    dueWords: 9,
    icon: BriefcaseBusiness,
    tone: "orange",
  },
  {
    id: "saved-words",
    title: "Saved Words",
    progressText: "89 learned",
    percentage: 72,
    dueWords: 12,
    icon: Heart,
    tone: "rose",
  },
];

const RECOMMENDED_WORDS: RecommendedWord[] = [
  { id: "journey", word: "journey", phonetic: "/ˈdʒɜːrni/", partOfSpeech: "n.", meaningVi: "chuyến đi", level: "A2" },
  { id: "improve", word: "improve", phonetic: "/ɪmˈpruːv/", partOfSpeech: "v.", meaningVi: "cải thiện", level: "A2" },
  { id: "accurate", word: "accurate", phonetic: "/ˈækjərət/", partOfSpeech: "adj.", meaningVi: "chính xác", level: "B1" },
  { id: "context", word: "context", phonetic: "/ˈkɑːntekst/", partOfSpeech: "n.", meaningVi: "ngữ cảnh", level: "B1" },
  { id: "essential", word: "essential", phonetic: "/ɪˈsenʃəl/", partOfSpeech: "adj.", meaningVi: "thiết yếu", level: "B2" },
  { id: "contribute", word: "contribute", phonetic: "/kənˈtrɪbjuːt/", partOfSpeech: "v.", meaningVi: "đóng góp", level: "B2" },
];

const TABS: Array<"All" | WordSetCategory> = ["All", "Oxford", "Exams", "Topics", "From Lessons", "Saved", "AI Generated"];

const toneClasses: Record<IconTone, { icon: string; soft: string }> = {
  emerald: { icon: "bg-emerald-100 text-emerald-700", soft: "bg-emerald-500" },
  violet: { icon: "bg-violet-100 text-violet-700", soft: "bg-violet-500" },
  blue: { icon: "bg-blue-100 text-blue-700", soft: "bg-blue-500" },
  orange: { icon: "bg-orange-100 text-orange-700", soft: "bg-orange-500" },
  cyan: { icon: "bg-cyan-100 text-cyan-700", soft: "bg-cyan-500" },
  amber: { icon: "bg-amber-100 text-amber-700", soft: "bg-amber-500" },
  rose: { icon: "bg-rose-100 text-rose-700", soft: "bg-rose-500" },
};

function formatNumber(value: number) {
  return new Intl.NumberFormat("en-US").format(value);
}

function CefrBadge({ level }: { level: CefrLevel }) {
  return (
    <span className="inline-flex h-6 min-w-8 items-center justify-center rounded-full bg-emerald-50 px-2 text-[11px] font-black text-emerald-700 ring-1 ring-emerald-100">
      {level}
    </span>
  );
}

function SectionHeader({ title, subtitle, action }: { title: string; subtitle?: string; action?: string }) {
  return (
    <div className="mb-4 flex items-end justify-between gap-4">
      <div>
        <h2 className="text-lg font-black text-slate-950 sm:text-xl">{title}</h2>
        {subtitle ? <p className="mt-1 text-sm font-semibold text-slate-500">{subtitle}</p> : null}
      </div>
      {action ? (
        <button className="inline-flex shrink-0 items-center gap-2 text-sm font-black text-slate-600 transition hover:text-emerald-600">
          {action}
          <ArrowRight size={16} />
        </button>
      ) : null}
    </div>
  );
}

export default function VocabularyPage() {
  const [query, setQuery] = useState("");
  const [activeTab, setActiveTab] = useState<(typeof TABS)[number]>("All");

  const filteredWordSets = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    return WORD_SETS.filter((wordSet) => {
      const matchesTab = activeTab === "All" || wordSet.category === activeTab;
      const matchesQuery =
        !normalizedQuery ||
        [wordSet.title, wordSet.description, wordSet.category].some((value) => value.toLowerCase().includes(normalizedQuery));

      return matchesTab && matchesQuery;
    });
  }, [activeTab, query]);

  return (
    <main className="min-h-full bg-[#fbfefc] px-4 py-4 text-slate-900 sm:px-6 lg:px-8 lg:py-8">
      <div className="mx-auto flex max-w-7xl flex-col gap-8">
        <VocabularyHero query={query} onQueryChange={setQuery} />
        <ContinueLearningSection />
        <WordSetLibrarySection activeTab={activeTab} onTabChange={setActiveTab} wordSets={filteredWordSets} />
        <RecommendedTodaySection />
      </div>
    </main>
  );
}

function VocabularyHero({ query, onQueryChange }: { query: string; onQueryChange: (value: string) => void }) {
  return (
    <section className="grid gap-8 overflow-hidden rounded-[2rem] bg-white px-5 py-7 shadow-[0_20px_70px_rgba(15,23,42,0.06)] ring-1 ring-slate-100 sm:px-8 lg:grid-cols-[1.05fr_0.95fr] lg:items-center lg:px-10 lg:py-10">
      <div>
        <div className="mb-5 inline-flex items-center gap-2 rounded-full bg-emerald-50 px-3 py-1.5 text-xs font-black text-emerald-700 ring-1 ring-emerald-100">
          <BookOpen size={15} />
          Vocabulary Library
        </div>
        <h1 className="max-w-2xl text-3xl font-black leading-tight text-slate-950 sm:text-4xl lg:text-5xl">
          Expand your vocabulary through <span className="text-emerald-600">real listening.</span>
        </h1>
        <p className="mt-4 max-w-xl text-base font-semibold leading-7 text-slate-500">
          Discover word sets, learn in context, and remember words that matter.
        </p>

        <div className="mt-7 flex flex-col gap-3 rounded-3xl bg-white p-2 shadow-[0_14px_44px_rgba(15,23,42,0.08)] ring-1 ring-slate-100 sm:flex-row sm:items-center">
          <div className="flex min-h-12 flex-1 items-center gap-3 px-3">
            <Search className="shrink-0 text-slate-400" size={22} />
            <input
              value={query}
              onChange={(event) => onQueryChange(event.target.value)}
              placeholder="Search any English word or word set..."
              className="w-full bg-transparent text-sm font-bold text-slate-700 outline-none placeholder:text-slate-400 sm:text-base"
            />
          </div>
          <button className="h-12 rounded-2xl bg-emerald-600 px-7 text-sm font-black text-white shadow-lg shadow-emerald-100 transition hover:bg-emerald-700 active:scale-95">
            Search
          </button>
        </div>

        <div className="mt-7 flex flex-col gap-3 sm:flex-row">
          <Link
            href="/library"
            className="inline-flex h-12 items-center justify-center gap-2 rounded-full bg-emerald-600 px-7 text-sm font-black text-white shadow-xl shadow-emerald-100 transition hover:bg-emerald-700 active:scale-95"
          >
            <Play size={17} fill="currentColor" />
            Start Practice
          </Link>
          <Link
            href="/upload"
            className="inline-flex h-12 items-center justify-center gap-2 rounded-full bg-white px-7 text-sm font-black text-slate-700 shadow-sm ring-1 ring-slate-200 transition hover:text-emerald-700 hover:ring-emerald-200 active:scale-95"
          >
            <Upload size={17} />
            Import From Lesson
          </Link>
        </div>
      </div>

      <div className="relative min-h-[250px] lg:min-h-[320px]">
        <FloatingWordCard className="left-2 top-4" word="journey" phonetic="n. /ˈdʒɜːrni/" />
        <FloatingWordCard className="right-4 top-12" word="improve" phonetic="v. /ɪmˈpruːv/" />
        <FloatingWordCard className="bottom-8 right-6" word="accurate" phonetic="adj. /ˈækjərət/" />
        <div className="absolute inset-x-8 bottom-3 top-8 rounded-full bg-emerald-50/80 blur-2xl" />
        <div className="absolute left-1/2 top-1/2 h-52 w-52 -translate-x-1/2 -translate-y-1/2 rounded-full bg-emerald-100/70 sm:h-64 sm:w-64" />
        <div className="absolute left-1/2 top-1/2 h-44 w-44 -translate-x-1/2 -translate-y-1/2 sm:h-56 sm:w-56">
          <Image src="/logo.png" alt="ParroTalk mascot" fill priority sizes="(max-width: 1024px) 176px, 224px" className="object-contain drop-shadow-2xl" />
        </div>
        <Sparkles className="absolute right-14 top-3 text-amber-300" size={22} fill="currentColor" />
        <Sparkles className="absolute bottom-20 left-8 text-emerald-200" size={18} fill="currentColor" />
      </div>
    </section>
  );
}

function FloatingWordCard({ word, phonetic, className }: { word: string; phonetic: string; className: string }) {
  return (
    <div className={`absolute z-10 rounded-2xl bg-white/90 px-4 py-3 shadow-lg shadow-slate-200/60 ring-1 ring-slate-100 backdrop-blur ${className}`}>
      <p className="text-sm font-black text-slate-950">{word}</p>
      <p className="mt-0.5 text-xs font-bold text-slate-500">{phonetic}</p>
    </div>
  );
}

function ContinueLearningSection() {
  return (
    <section>
      <SectionHeader title="Continue Learning" action="View all" />
      <div className="-mx-4 flex gap-4 overflow-x-auto px-4 pb-2 sm:mx-0 sm:grid sm:grid-cols-2 sm:overflow-visible sm:px-0 lg:grid-cols-4">
        {CONTINUE_LEARNING_SETS.map((item) => (
          <ContinueLearningCard key={item.id} item={item} />
        ))}
      </div>
    </section>
  );
}

function ContinueLearningCard({ item }: { item: ContinueLearningSet }) {
  const Icon = item.icon;

  return (
    <article className="min-w-[260px] rounded-3xl bg-white p-5 shadow-[0_16px_48px_rgba(15,23,42,0.06)] ring-1 ring-slate-100">
      <div className="flex items-start gap-4">
        <div className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-full ${toneClasses[item.tone].icon}`}>
          <Icon size={25} fill="currentColor" strokeWidth={1.8} />
        </div>
        <div className="min-w-0">
          <h3 className="truncate text-sm font-black text-slate-950">{item.title}</h3>
          <p className="mt-2 text-xs font-bold text-slate-500">{item.progressText}</p>
        </div>
      </div>

      <div className="mt-6 flex items-center gap-3">
        <div className="h-1.5 flex-1 rounded-full bg-slate-100">
          <div className={`h-full rounded-full ${toneClasses[item.tone].soft}`} style={{ width: `${item.percentage}%` }} />
        </div>
        <span className="w-9 text-right text-xs font-black text-slate-500">{item.percentage}%</span>
      </div>

      <div className="mt-6 flex items-center justify-between gap-3">
        <span className="text-xs font-black text-orange-600">{item.dueWords} due</span>
        <button className="h-9 rounded-xl px-4 text-xs font-black text-slate-700 ring-1 ring-slate-200 transition hover:text-emerald-700 hover:ring-emerald-200">
          Continue
        </button>
      </div>
    </article>
  );
}

function WordSetLibrarySection({
  activeTab,
  onTabChange,
  wordSets,
}: {
  activeTab: (typeof TABS)[number];
  onTabChange: (tab: (typeof TABS)[number]) => void;
  wordSets: WordSet[];
}) {
  return (
    <section>
      <SectionHeader title="Word Set Library" action="View all" />
      <div className="rounded-[2rem] bg-white p-4 shadow-[0_20px_70px_rgba(15,23,42,0.05)] ring-1 ring-slate-100 sm:p-5">
        <WordSetTabs activeTab={activeTab} onTabChange={onTabChange} />
        <WordSetFilters />
        <WordSetGrid wordSets={wordSets} />
      </div>
    </section>
  );
}

function WordSetTabs({ activeTab, onTabChange }: { activeTab: (typeof TABS)[number]; onTabChange: (tab: (typeof TABS)[number]) => void }) {
  return (
    <div className="-mx-4 overflow-x-auto px-4 sm:mx-0 sm:px-0">
      <div className="flex min-w-max rounded-2xl border border-slate-100 bg-white p-1">
        {TABS.map((tab) => (
          <button
            key={tab}
            onClick={() => onTabChange(tab)}
            className={`h-9 rounded-xl px-4 text-xs font-black transition ${
              activeTab === tab ? "bg-emerald-600 text-white shadow-md shadow-emerald-100" : "text-slate-600 hover:bg-emerald-50 hover:text-emerald-700"
            }`}
          >
            {tab}
          </button>
        ))}
      </div>
    </div>
  );
}

function WordSetFilters() {
  return (
    <div className="mt-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
      <div className="-mx-4 flex gap-3 overflow-x-auto px-4 sm:mx-0 sm:px-0">
        <FilterButton label="CEFR Level" />
        <FilterButton label="Purpose" />
        <FilterButton label="Status" />
      </div>
      <div className="flex items-center gap-3">
        <FilterButton label="Sort by: Recently added" className="flex-1 sm:flex-none" />
        <div className="grid h-11 w-[94px] shrink-0 grid-cols-2 rounded-2xl border border-slate-200 bg-white p-1">
          <button className="flex items-center justify-center rounded-xl bg-emerald-600 text-white" aria-label="Grid view">
            <Grid2X2 size={17} />
          </button>
          <button className="flex items-center justify-center rounded-xl text-slate-500 hover:bg-slate-50" aria-label="List view">
            <List size={18} />
          </button>
        </div>
      </div>
    </div>
  );
}

function FilterButton({ label, className = "" }: { label: string; className?: string }) {
  return (
    <button className={`flex h-11 shrink-0 items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white px-4 text-xs font-black text-slate-700 transition hover:border-emerald-200 hover:text-emerald-700 ${className}`}>
      {label}
      <ChevronDown size={15} />
    </button>
  );
}

function WordSetGrid({ wordSets }: { wordSets: WordSet[] }) {
  if (wordSets.length === 0) {
    return (
      <div className="mt-5 rounded-3xl border border-dashed border-slate-200 bg-slate-50 p-8 text-center">
        <Sparkles className="mx-auto text-slate-300" />
        <p className="mt-3 text-sm font-bold text-slate-500">No word sets match this search yet.</p>
      </div>
    );
  }

  return (
    <div className="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
      {wordSets.map((wordSet) => (
        <WordSetCard key={wordSet.id} wordSet={wordSet} />
      ))}
    </div>
  );
}

function WordSetCard({ wordSet }: { wordSet: WordSet }) {
  const Icon = wordSet.icon;

  return (
    <article className="flex min-h-[286px] flex-col rounded-3xl border border-slate-100 bg-white p-5 shadow-[0_14px_42px_rgba(15,23,42,0.04)] transition hover:-translate-y-0.5 hover:border-emerald-100 hover:shadow-[0_18px_54px_rgba(15,23,42,0.07)]">
      <div className="flex items-start gap-4">
        <div className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-full ${toneClasses[wordSet.tone].icon}`}>
          <Icon size={24} fill="currentColor" strokeWidth={1.8} />
        </div>
        <div>
          <h3 className="text-base font-black text-slate-950">{wordSet.title}</h3>
          <p className="mt-2 line-clamp-2 text-xs font-semibold leading-5 text-slate-500">{wordSet.description}</p>
        </div>
      </div>

      <div className="mt-5 flex flex-wrap gap-2">
        {wordSet.levels.map((level) => (
          <CefrBadge key={level} level={level} />
        ))}
      </div>

      <div className="mt-auto pt-6">
        <div className="grid grid-cols-3 divide-x divide-slate-100 border-y border-slate-100 py-3">
          <StatBlock value={formatNumber(wordSet.totalWords)} label="Words" />
          <StatBlock value={formatNumber(wordSet.learnedWords)} label="Learned" />
          <StatBlock value={formatNumber(wordSet.dueWords)} label="Due" highlight />
        </div>

        <div className="mt-4 grid grid-cols-2 gap-3">
          <button className="h-10 rounded-xl bg-emerald-600 text-xs font-black text-white shadow-lg shadow-emerald-100 transition hover:bg-emerald-700 active:scale-95">
            Practice
          </button>
          <button className="h-10 rounded-xl bg-white text-xs font-black text-slate-700 ring-1 ring-slate-200 transition hover:text-emerald-700 hover:ring-emerald-200 active:scale-95">
            View
          </button>
        </div>
      </div>
    </article>
  );
}

function StatBlock({ value, label, highlight = false }: { value: string; label: string; highlight?: boolean }) {
  return (
    <div className="px-2 text-center first:pl-0 last:pr-0">
      <p className={`text-sm font-black ${highlight ? "text-orange-600" : "text-slate-950"}`}>{value}</p>
      <p className="mt-1 text-[10px] font-bold text-slate-400">{label}</p>
    </div>
  );
}

function RecommendedTodaySection() {
  return (
    <section>
      <div className="mb-4 flex items-end justify-between gap-4">
        <div className="flex items-start gap-3">
          <Sparkles className="mt-1 text-amber-400" fill="currentColor" size={22} />
          <div>
            <h2 className="text-lg font-black text-slate-950 sm:text-xl">Recommended Today</h2>
            <p className="mt-1 text-sm font-semibold text-slate-500">Focus on these words to level up faster!</p>
          </div>
        </div>
        <button className="inline-flex shrink-0 items-center gap-2 text-sm font-black text-slate-600 transition hover:text-emerald-600">
          Refresh
          <RefreshCw size={15} />
        </button>
      </div>

      <div className="-mx-4 flex gap-4 overflow-x-auto px-4 pb-2 sm:mx-0 sm:px-0">
        {RECOMMENDED_WORDS.map((word) => (
          <WordCard key={word.id} word={word} />
        ))}
      </div>
    </section>
  );
}

function WordCard({ word }: { word: RecommendedWord }) {
  return (
    <Link
      href={`/dictionary/words/${word.id}`}
      className="min-w-[170px] rounded-3xl bg-white p-4 shadow-[0_14px_42px_rgba(15,23,42,0.05)] ring-1 ring-slate-100 transition hover:-translate-y-0.5 hover:ring-emerald-100"
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="text-base font-black text-slate-950">{word.word}</h3>
          <p className="mt-1 text-xs font-bold text-slate-500">{word.phonetic}</p>
        </div>
        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-emerald-50 text-emerald-600" aria-label={`Play ${word.word}`}>
          <Volume2 size={16} />
        </span>
      </div>
      <p className="mt-4 min-h-5 text-sm font-black text-slate-700">
        {word.partOfSpeech} {word.meaningVi}
      </p>
      <div className="mt-4 flex items-center justify-between">
        <CefrBadge level={word.level} />
        <Star size={17} className="text-slate-400" fill="currentColor" />
      </div>
    </Link>
  );
}
