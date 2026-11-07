"use client";

import { Button } from "@/components/ui/Button";
import ConfirmationModal from "@/components/ui/ConfirmationModal";
import { Input } from "@/components/ui/Input";
import { adminVocabularyApi } from "@/features/admin/services/api";
import { 
  CheckCircle2, 
  Edit3, 
  FileDown, 
  LayoutList, 
  Plus, 
  Search, 
  Tag, 
  Trash2, 
  Volume2, 
  X 
} from "lucide-react";
import Link from "next/link";
import axios from 'axios';
import { useEffect, useRef, useState } from "react";

// Must match the 15 canonical topics seeded in vocabulary_topics table
const CANONICAL_TOPICS = [
  { name: "People & Relationships",      id: "" },
  { name: "Emotions & Psychology",        id: "" },
  { name: "Communication & Language",     id: "" },
  { name: "Education & Learning",         id: "" },
  { name: "Work & Business",              id: "" },
  { name: "Money & Finance",              id: "" },
  { name: "Society & Politics",           id: "" },
  { name: "Science & Technology",         id: "" },
  { name: "Health & Body",                id: "" },
  { name: "Food & Cooking",               id: "" },
  { name: "Home & Daily Life",            id: "" },
  { name: "Travel & Transportation",      id: "" },
  { name: "Nature & Environment",         id: "" },
  { name: "Arts, Media & Entertainment", id: "" },
  { name: "Sports & Recreation",          id: "" },
];

// Short display names for the filter chips in the toolbar
const TOPIC_FILTER_LABELS = CANONICAL_TOPICS.slice(0, 5).map(t => t.name);

interface PageData {
  content: any[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

const getPosBadgeClass = (pos: string) => {
  if (!pos) return 'text-slate-600 bg-slate-50 border-slate-200';
  const p = pos.toLowerCase();
  if (p.includes('adj')) return 'text-amber-600 bg-amber-50 border-amber-200';
  if (p.includes('verb') && !p.includes('adv')) return 'text-emerald-600 bg-emerald-50 border-emerald-200';
  if (p.includes('noun')) return 'text-blue-600 bg-blue-50 border-blue-200';
  if (p.includes('adv')) return 'text-purple-600 bg-purple-50 border-purple-200';
  if (p.includes('prep')) return 'text-indigo-600 bg-indigo-50 border-indigo-200';
  return 'text-slate-600 bg-slate-50 border-slate-200';
};

const getCefrBadgeClass = (level: string) => {
  if (!level) return 'text-slate-600 bg-slate-50 border-slate-200';
  const l = level.toUpperCase();
  if (l === 'A1' || l === 'A2') return 'text-slate-500 bg-slate-100 border-slate-200';
  if (l === 'B1' || l === 'B2') return 'text-emerald-700 bg-emerald-50 border-emerald-200 font-semibold';
  if (l === 'C1' || l === 'C2') return 'text-purple-700 bg-purple-50 border-purple-200 font-semibold';
  return 'text-slate-600 bg-slate-50 border-slate-200';
};

export default function AdminVocabularyListPage() {
  const [data, setData] = useState<PageData | null>(null);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [page, setPage] = useState(0);
  const [selectedTopicName, setSelectedTopicName] = useState<string | null>(null);
  const topicsMapRef = useRef<Record<string, string>>({}); // name -> uuid

  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [itemToDelete, setItemToDelete] = useState<string | null>(null);

  // Load topics from API once to build name→uuid map
  useEffect(() => {
    const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';
    axios.get(`${API_BASE}/vocabulary/topics`).then(res => {
      const topics = res.data?.result ?? res.data ?? [];
      const map: Record<string, string> = {};
      for (const t of topics) {
        if (t.id && t.name) map[t.name] = t.id;
      }
      topicsMapRef.current = map;
    }).catch(() => {});
  }, []);

  // Debounce search
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0); // Reset page on search change
    }, 500);
    return () => clearTimeout(handler);
  }, [search]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const topicId = selectedTopicName ? (topicsMapRef.current[selectedTopicName] ?? '') : '';
      const json = await adminVocabularyApi.getVocabularies(
        page, 
        20, 
        debouncedSearch,
        '', // level
        '', // partOfSpeech
        '', // status
        topicId
      );
      setData(json as any);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedSearch, page, selectedTopicName]);

  const handleDelete = async () => {
    if (!itemToDelete) return;
    try {
      await adminVocabularyApi.deleteVocabulary(itemToDelete);
      fetchData();
    } catch (error) {
      console.error(error);
    } finally {
      setDeleteModalOpen(false);
      setItemToDelete(null);
    }
  };

  const renderPageNumbers = () => {
    if (!data) return null;
    const total = data.totalPages || 1;
    const current = page + 1;
  
    const pages = [];
    if (total <= 7) {
      for (let i = 1; i <= total; i++) pages.push(i);
    } else {
      if (current <= 4) {
        pages.push(1, 2, 3, 4, 5, '...', total);
      } else if (current >= total - 3) {
        pages.push(1, '...', total - 4, total - 3, total - 2, total - 1, total);
      } else {
        pages.push(1, '...', current - 1, current, current + 1, '...', total);
      }
    }
  
    return (
      <div className="flex items-center gap-1">
        {pages.map((p, i) => (
          p === '...' ? (
            <span key={`dots-${i}`} className="text-slate-400 px-1">...</span>
          ) : (
            <button 
              key={p} 
              onClick={() => setPage((p as number) - 1)}
              className={`w-7 h-7 rounded flex items-center justify-center text-sm ${current === p ? 'bg-emerald-50 text-emerald-600 font-medium' : 'hover:bg-slate-100 text-slate-600'}`}
            >
              {p}
            </button>
          )
        ))}
      </div>
    );
  };

  const renderSensesIndicator = (count: number) => {
    const safeCount = typeof count === 'number' ? count : 0;
    const percentage = Math.min(100, Math.max(10, (safeCount / 15) * 100));
    return (
      <div className="w-12 h-1.5 bg-slate-100 rounded-full overflow-hidden">
        <div 
          className="h-full bg-emerald-500 rounded-full" 
          style={{ width: `${percentage}%` }} 
        />
      </div>
    );
  };

  const cefrLevels = ['All', 'A1', 'A2', 'B1', 'B2', 'C1', 'C2'];
  const activeCefr = 'B2';

  return (
    <div className="p-4 md:p-8 max-w-[1600px] mx-auto min-h-screen bg-slate-50/50">
      <div className="bg-white rounded-t-xl border border-slate-200">
        {/* Header Section */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center p-6 gap-4">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <h1 className="text-2xl font-bold text-slate-800 tracking-tight">Vocabulary Management</h1>
              <span className="text-xs text-indigo-600 bg-indigo-50 border border-indigo-100 px-2.5 py-1 rounded-full font-medium">Headwords & Lexical Units</span>
            </div>
            <p className="text-sm text-slate-500">Manage all vocabulary entries, grammatical categories, phonetics, and multi-sense semantics.</p>
          </div>
          <div className="flex gap-3">
            <Button variant="outline" className="text-slate-600 border-slate-200 shadow-sm" leftIcon={<FileDown className="w-4 h-4" />}>
              Export CSV
            </Button>
            <Link href="/admin/vocabularies/create">
              <Button className="bg-emerald-600 hover:bg-emerald-700 text-white shadow-sm" leftIcon={<Plus className="w-4 h-4" />}>
                Add Vocabulary
              </Button>
            </Link>
          </div>
        </div>

        {/* Search & Applied Filters Bar */}
        <div className="border-t border-slate-200 p-4 flex flex-col xl:flex-row xl:items-center justify-between gap-4">
          <div className="relative flex-1 max-w-3xl flex items-center">
            <Search className="absolute left-3.5 w-4 h-4 text-slate-400" />
            <Input 
              className="pl-10 pr-24 w-full border-slate-200 bg-slate-50/50 focus:bg-white transition-colors" 
              placeholder="Search words by headword, translation, topic, or phonetic pattern..." 
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <div className="absolute right-3 flex items-center gap-3 text-sm text-slate-500">
              <button className="hover:text-slate-700 font-medium">Exact</button>
              <button className="hover:text-slate-700 font-medium" onClick={() => setSearch('')}>Clear</button>
            </div>
          </div>
          <div className="flex items-center gap-2 text-sm">
            <span className="text-slate-400 font-medium">Applied:</span>
            <span className="flex items-center gap-1.5 bg-emerald-50 text-emerald-700 border border-emerald-100 px-2.5 py-1 rounded-md text-xs font-medium">
              CEFR: B2 <X className="w-3 h-3 cursor-pointer hover:text-emerald-900" />
            </span>
            <span className="flex items-center gap-1.5 bg-slate-100 text-slate-700 border border-slate-200 px-2.5 py-1 rounded-md text-xs font-medium">
              Status: Approved <X className="w-3 h-3 cursor-pointer hover:text-slate-900" />
            </span>
            <button className="text-emerald-600 font-medium hover:text-emerald-700 ml-2">Reset all</button>
          </div>
        </div>

        {/* Quick Filters Bar */}
        <div className="border-t border-slate-200 p-4 flex flex-col lg:flex-row lg:items-center justify-between text-sm gap-4">
          <div className="flex flex-col sm:flex-row sm:items-center gap-6 lg:gap-8 overflow-x-auto pb-2 sm:pb-0 hide-scrollbar">
            {/* CEFR */}
            <div className="flex items-center gap-3 shrink-0">
              <span className="text-slate-400 font-semibold text-[11px] uppercase tracking-wider">CEFR</span>
              <div className="flex gap-1.5">
                {cefrLevels.map(level => (
                  <button key={level} className={`px-2.5 py-1 rounded-md font-medium text-xs transition-colors ${level === activeCefr ? 'bg-emerald-600 text-white shadow-sm' : 'text-slate-600 hover:bg-slate-100'}`}>
                    {level}
                  </button>
                ))}
              </div>
            </div>
            
            {/* TOPICS */}
            <div className="flex items-center gap-3 shrink-0">
              <span className="text-slate-400 font-semibold text-[11px] uppercase tracking-wider">TOPICS</span>
              <div className="flex gap-2 flex-wrap">
                {CANONICAL_TOPICS.slice(0, 5).map(topic => (
                  <button 
                    key={topic.name} 
                    onClick={() => {
                      setSelectedTopicName(prev => prev === topic.name ? null : topic.name);
                      setPage(0);
                    }}
                    className={`px-3 py-1 rounded-full border text-xs font-medium transition-colors ${
                      selectedTopicName === topic.name
                        ? 'bg-emerald-600 text-white border-emerald-600 shadow-sm'
                        : 'bg-slate-50 text-slate-600 hover:bg-slate-100 border-slate-200'
                    }`}
                  >
                    {topic.name}
                  </button>
                ))}
                {selectedTopicName && !CANONICAL_TOPICS.slice(0, 5).find(t => t.name === selectedTopicName) && (
                  <span className="px-3 py-1 rounded-full bg-emerald-600 text-white border-emerald-600 text-xs font-medium">
                    {selectedTopicName} <X className="inline w-3 h-3 ml-1 cursor-pointer" onClick={() => setSelectedTopicName(null)} />
                  </span>
                )}
                <select
                  className="text-xs border border-slate-200 rounded-full px-2 py-1 bg-slate-50 text-slate-600 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                  value={selectedTopicName ?? ""}
                  onChange={e => { setSelectedTopicName(e.target.value || null); setPage(0); }}
                >
                  <option value="">+ More topics</option>
                  {CANONICAL_TOPICS.map(t => (
                    <option key={t.name} value={t.name}>{t.name}</option>
                  ))}
                </select>
              </div>
            </div>
          </div>
          
          <div className="flex items-center gap-4 shrink-0">
            <div className="flex items-center gap-2 border border-slate-200 rounded-lg px-3 py-1.5 bg-white shadow-sm">
              <span className="text-slate-500 text-xs font-medium">Rows:</span>
              <span className="font-semibold text-slate-700 text-xs">20</span>
            </div>
            <div className="flex items-center border border-slate-200 rounded-lg p-0.5 bg-slate-50/80 shadow-inner">
              <button className="px-3 py-1 bg-white shadow-sm rounded-md text-slate-800 font-medium text-xs">Comfortable</button>
              <button className="px-3 py-1 text-slate-500 hover:text-slate-700 text-xs font-medium">Compact</button>
              <button className="px-2 py-1 text-slate-500 hover:text-slate-700">
                <LayoutList className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>

        {/* Bulk Actions Bar */}
        <div className="bg-emerald-50/40 border-t border-slate-200 p-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3 pl-4">
            <input type="checkbox" className="w-4 h-4 rounded border-slate-300 text-emerald-600 focus:ring-emerald-600 cursor-pointer bg-white" />
            <span className="text-sm text-slate-700">
              Select all 20 entries on page (or <button className="text-emerald-600 font-medium hover:underline">select all {data?.totalElements || 7579} indexed items</button>)
            </span>
          </div>
          <div className="flex items-center gap-2 pr-4 overflow-x-auto hide-scrollbar">
            <Button variant="outline" size="sm" className="h-8 text-emerald-700 border-emerald-200 bg-white hover:bg-emerald-50 shadow-sm whitespace-nowrap" leftIcon={<Tag className="w-3 h-3" />}>Assign Topic</Button>
            <Button variant="outline" size="sm" className="h-8 text-emerald-700 border-emerald-200 bg-white hover:bg-emerald-50 shadow-sm whitespace-nowrap" leftIcon={<CheckCircle2 className="w-3 h-3" />}>Mark Reviewed</Button>
            <Button variant="outline" size="sm" className="h-8 text-rose-600 border-rose-200 bg-white hover:bg-rose-50 shadow-sm whitespace-nowrap" leftIcon={<Trash2 className="w-3 h-3" />}>Delete</Button>
          </div>
        </div>
      </div>

      {/* List Container */}
      <div className="bg-white border-x border-slate-200">
        {loading ? (
          <div className="py-12 text-center text-slate-500 flex flex-col items-center justify-center gap-3">
            <div className="w-6 h-6 border-2 border-emerald-500 border-t-transparent rounded-full animate-spin"></div>
            Loading vocabulary...
          </div>
        ) : data?.content.length === 0 ? (
          <div className="py-12 text-center text-slate-500">No vocabulary found.</div>
        ) : (
          data?.content.map((vocab) => {
            // Normalize data to handle both real API response and mock UI expectations
            const topics = (vocab.topics || (vocab.tags ? vocab.tags : [])).filter(Boolean);
            const posList = (Array.isArray(vocab.partsOfSpeech) ? vocab.partsOfSpeech : (vocab.partOfSpeech ? [vocab.partOfSpeech] : [])).filter(Boolean);
            const cefrList = (Array.isArray(vocab.cefrLevels) ? vocab.cefrLevels : (vocab.cefrLevel ? [vocab.cefrLevel] : [])).filter(Boolean);
            const senses = vocab.senseCount ?? vocab.definitionsCount ?? 0;
            
            return (
              <div key={vocab.id} className="flex flex-col sm:flex-row sm:items-center py-3.5 px-4 border-b border-slate-100 hover:bg-slate-50/80 group transition-colors gap-3 sm:gap-0">
                {/* Checkbox */}
                <div className="w-12 pl-4 shrink-0 hidden sm:block">
                  <input type="checkbox" className="w-4 h-4 rounded border-slate-300 text-emerald-600 cursor-pointer bg-white" />
                </div>
                
                {/* Word & Phonetics */}
                <div className="w-full sm:w-[28%] flex items-center gap-2.5">
                  <span className="font-semibold text-slate-800 text-[15px]">{vocab.word}</span>
                  {vocab.ipaUk && <span className="text-slate-400 text-sm font-normal font-mono bg-slate-50 px-1.5 rounded">/{vocab.ipaUk}/</span>}
                  <button className="text-slate-300 hover:text-emerald-600 transition-colors">
                    <Volume2 className="w-4 h-4" />
                  </button>
                </div>

                {/* Topics */}
                <div className="w-full sm:w-[22%] flex gap-1.5 flex-wrap">
                  {topics.slice(0, 2).map((t: string, i: number) => (
                    <span key={i} className="px-2.5 py-0.5 rounded-full bg-slate-50 border border-slate-100 text-slate-600 text-[11px] font-medium">{t}</span>
                  ))}
                  {topics.length > 2 && (
                    <span className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-500 text-[11px] font-medium">
                      +{topics.length - 2}
                    </span>
                  )}
                </div>

                {/* POS */}
                <div className="w-full sm:w-[20%] flex gap-1.5 flex-wrap">
                  {posList.map((pos: string, i: number) => (
                    <span key={i} className={`px-2 py-0.5 rounded text-[11px] font-semibold border ${getPosBadgeClass(pos)}`}>
                      {pos.toLowerCase()}
                    </span>
                  ))}
                </div>

                {/* CEFR */}
                <div className="w-full sm:w-[10%] flex gap-1 flex-wrap">
                  {cefrList.map((lvl: string, i: number) => (
                    <span key={i} className={`px-1.5 py-0.5 rounded text-[11px] border ${getCefrBadgeClass(lvl)}`}>
                      {lvl.toUpperCase()}
                    </span>
                  ))}
                </div>

                {/* Senses */}
                <div className="w-full sm:w-[12%] flex items-center gap-2.5">
                  <span className="text-xs text-slate-600 font-medium">{senses} senses</span>
                  {renderSensesIndicator(senses)}
                </div>

                {/* Actions */}
                <div className="flex-1 flex justify-end gap-1.5 pr-2 sm:opacity-0 sm:group-hover:opacity-100 transition-opacity">
                  <Link href={`/admin/vocabularies/${vocab.id}`}>
                    <button className="p-1.5 text-slate-400 hover:text-emerald-600 rounded-md hover:bg-emerald-50 transition-colors">
                      <Edit3 className="w-4 h-4" />
                    </button>
                  </Link>
                  <button 
                    className="p-1.5 text-slate-400 hover:text-rose-600 rounded-md hover:bg-rose-50 transition-colors" 
                    onClick={() => {
                      setItemToDelete(vocab.id);
                      setDeleteModalOpen(true);
                    }}
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Pagination Controls */}
      <div className="border border-t-0 border-slate-200 p-4 flex flex-col sm:flex-row sm:items-center justify-between text-sm text-slate-500 bg-white rounded-b-xl shadow-sm gap-4">
        <div className="flex items-center gap-4">
          <div>
            Showing <span className="font-medium text-slate-700">{data?.totalElements === 0 ? 0 : page * 20 + 1}</span> to <span className="font-medium text-slate-700">{Math.min((page + 1) * 20, data?.totalElements || 0)}</span> of <span className="font-medium text-slate-700">{data?.totalElements || 0}</span> entries
          </div>
          <div className="h-4 w-px bg-slate-200 hidden sm:block"></div>
          <div className="hidden sm:block">20 items per view</div>
        </div>
        
        <div className="flex items-center gap-2 sm:gap-3">
          <button 
            className="px-2 py-1 text-slate-500 hover:text-slate-800 disabled:opacity-40 disabled:hover:text-slate-500 font-medium transition-colors"
            disabled={page === 0}
            onClick={() => setPage(p => p - 1)}
          >
            Previous
          </button>
          
          {renderPageNumbers()}
          
          <button 
            className="px-2 py-1 text-slate-700 hover:text-slate-900 disabled:opacity-40 disabled:hover:text-slate-700 font-medium transition-colors"
            disabled={data ? page >= data.totalPages - 1 : true}
            onClick={() => setPage(p => p + 1)}
          >
            Next
          </button>
          
          <div className="flex items-center gap-2 ml-2 sm:ml-4">
            <span className="text-slate-400 hidden sm:inline">Go to:</span>
            <input 
              type="text" 
              className="w-10 h-7 border border-slate-200 rounded-md text-center text-slate-700 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 shadow-sm" 
              defaultValue="1" 
            />
          </div>
        </div>
      </div>

      <ConfirmationModal
        isOpen={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        onConfirm={handleDelete}
        title="Delete Vocabulary"
        message="Are you sure you want to delete this vocabulary? This will remove all associated senses, pronunciations, and relations. This action cannot be undone."
        confirmText="Delete"
        cancelText="Cancel"
      />
    </div>
  );
}

