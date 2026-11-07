"use client";

import React, { useState, useEffect } from "react";
import { X, Upload, Volume2, Wand2, Quote, MonitorPlay, CheckCircle2, Loader2, BookOpen } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { playTTS, stopTTS } from "@/lib/tts";
import { Label } from "@/components/ui/Label";
import { Textarea } from "@/components/ui/Textarea";
import { adminVocabularyApi } from "@/features/admin/services/api";

/** 15 canonical IELTS topics — mirror of vocabulary_topics in the DB */
const CANONICAL_TOPICS = [
  { name: "People & Relationships",      slug: "people_relationships" },
  { name: "Emotions & Psychology",        slug: "emotions_psychology" },
  { name: "Communication & Language",     slug: "communication_language" },
  { name: "Education & Learning",         slug: "education_learning" },
  { name: "Work & Business",              slug: "work_business" },
  { name: "Money & Finance",              slug: "money_finance" },
  { name: "Society & Politics",           slug: "society_politics" },
  { name: "Science & Technology",         slug: "science_technology" },
  { name: "Health & Body",                slug: "health_body" },
  { name: "Food & Cooking",               slug: "food_cooking" },
  { name: "Home & Daily Life",            slug: "home_daily_life" },
  { name: "Travel & Transportation",      slug: "travel_transportation" },
  { name: "Nature & Environment",         slug: "nature_environment" },
  { name: "Arts, Media & Entertainment", slug: "arts_media_entertainment" },
  { name: "Sports & Recreation",          slug: "sports_recreation" },
] as const;

interface EditSenseModalProps {
  isOpen: boolean;
  onClose: () => void;
  sense: Record<string, unknown> | null;
  index: number;
  word: string;
  onSave: (updatedSense: Record<string, unknown>) => void;
}

export default function EditSenseModal({ isOpen, onClose, sense, index, word, onSave }: EditSenseModalProps) {
  if (!isOpen || !sense) return null;

  return (
    <EditSenseFormContent
      key={`${(sense.id as string) || index}`}
      sense={sense}
      index={index}
      word={word}
      onClose={onClose}
      onSave={onSave}
    />
  );
}

function EditSenseFormContent({
  sense,
  index,
  word,
  onClose,
  onSave,
}: {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  sense: Record<string, any>;
  index: number;
  word: string;
  onClose: () => void;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  onSave: (updatedSense: Record<string, any>) => void;
}) {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [localSense, setLocalSense] = useState<Record<string, any>>(() => ({ ...sense }));
  const [isTestingTts, setIsTestingTts] = useState(false);
  const [isRefining, setIsRefining] = useState(false);
  const [isPredictingCefr, setIsPredictingCefr] = useState(false);
  const [isGeneratingExample, setIsGeneratingExample] = useState(false);

  const handleAiRefine = async () => {
    try {
      setIsRefining(true);
      const refinedList = await adminVocabularyApi.aiRefineSenses(word, [localSense]);
      if (refinedList && refinedList.length > 0) {
        const refined = refinedList[0];
        setLocalSense(prev => ({
          ...prev,
          cefrLevel: refined.cefrLevel || prev.cefrLevel,
          definitionEn: refined.definitionEn || prev.definitionEn,
          definitionVn: refined.definitionVn || prev.definitionVn,
          exampleEn: refined.exampleEn || prev.exampleEn,
          exampleVn: refined.exampleVn || prev.exampleVn
        }));
      }
    } catch (error) {
      console.error("Failed to refine sense using AI", error);
      alert("Failed to refine sense. Please try again.");
    } finally {
      setIsRefining(false);
    }
  };

  const handleAiPredictCefr = async () => {
    try {
      setIsPredictingCefr(true);
      const refinedList = await adminVocabularyApi.aiRefineSenses(word, [localSense]);
      if (refinedList && refinedList.length > 0) {
        const refined = refinedList[0];
        setLocalSense(prev => ({
          ...prev,
          cefrLevel: refined.cefrLevel || prev.cefrLevel,
        }));
      }
    } catch (error) {
      console.error("Failed to predict CEFR using AI", error);
      alert("Failed to predict CEFR. Please try again.");
    } finally {
      setIsPredictingCefr(false);
    }
  };

  const handleAiGenerateExample = async () => {
    try {
      setIsGeneratingExample(true);
      const refinedList = await adminVocabularyApi.aiRefineSenses(word, [localSense]);
      if (refinedList && refinedList.length > 0) {
        const refined = refinedList[0];
        setLocalSense(prev => ({
          ...prev,
          exampleEn: refined.exampleEn || prev.exampleEn,
          exampleVn: refined.exampleVn || prev.exampleVn
        }));
      }
    } catch (error) {
      console.error("Failed to generate example using AI", error);
      alert("Failed to generate example. Please try again.");
    } finally {
      setIsGeneratingExample(false);
    }
  };

  const handleTestExampleTts = () => {
    if (isTestingTts) {
      stopTTS();
      setIsTestingTts(false);
      return;
    }
    const text = (localSense.exampleEn as string) || "";
    if (!text.trim()) return;
    setIsTestingTts(true);
    playTTS(text, {
      lang: "en-US",
      onEnd: () => setIsTestingTts(false),
      onError: () => setIsTestingTts(false),
    });
  };

  // Handle escape key
  useEffect(() => {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleEsc);
    return () => window.removeEventListener("keydown", handleEsc);
  }, [onClose]);

  const handleArrayAdd = (field: string, val: string) => {
    const trimmed = val.trim();
    if (!trimmed) return;
    const current = (localSense[field] as string[]) || [];
    if (!current.includes(trimmed)) {
      setLocalSense({ ...localSense, [field]: [...current, trimmed] });
    }
  };

  const handleArrayRemove = (field: string, i: number) => {
    const current = (localSense[field] as string[]) || [];
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    setLocalSense({ ...localSense, [field]: current.filter((_: any, idx: number) => idx !== i) });
  };

  return (
    <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-[100] flex items-center justify-center p-4">
      <div className="bg-white rounded-[2rem] w-full max-w-4xl shadow-2xl flex flex-col max-h-[90vh] animate-in fade-in zoom-in duration-200 overflow-hidden">
        {/* Header */}
        <div className="px-8 py-6 border-b border-slate-100 flex justify-between items-center bg-white z-10 shrink-0">
          <div className="flex gap-4 items-center">
            <div className="bg-emerald-700 text-white font-bold w-12 h-12 rounded-2xl flex items-center justify-center text-xl shadow-inner">
              #{index + 1}
            </div>
            <div>
              <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
                Edit Sense #{index + 1} <span className="text-slate-400 font-normal capitalize">({String(localSense.partOfSpeech || 'N/A')})</span>
                <span className="text-sm font-normal text-slate-400 ml-2">lemma: <span className="font-semibold text-slate-700">{word}</span></span>
              </h2>
              <p className="text-slate-500 text-sm">Update semantic definition, bilingual alignment, dual audio & associations</p>
            </div>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex gap-1 bg-slate-100 p-1 rounded-xl">
              {['A1', 'A2', 'B1', 'B2', 'C1', 'C2'].map(lvl => (
                <button
                  key={lvl}
                  className={`px-3 py-1.5 rounded-lg text-sm font-bold transition-all ${
                    localSense.cefrLevel === lvl 
                      ? 'bg-emerald-700 text-white shadow-md' 
                      : 'text-slate-500 hover:text-slate-700 hover:bg-slate-200/50'
                  }`}
                  onClick={() => setLocalSense({ ...localSense, cefrLevel: lvl })}
                >
                  {lvl}
                </button>
              ))}
            </div>
            <Button 
              variant="secondary" 
              size="sm" 
              className="text-orange-600 bg-orange-50 border-orange-100 hover:bg-orange-100 rounded-xl h-9 text-xs font-bold gap-1.5 shadow-sm ml-2"
              onClick={handleAiPredictCefr}
              disabled={isPredictingCefr}
            >
              {isPredictingCefr ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Wand2 className="w-3.5 h-3.5" />} 
              {isPredictingCefr ? "Predicting..." : "Auto CEFR"}
            </Button>
            <button onClick={onClose} className="p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600 rounded-full transition-colors">
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>
        
        {/* Body */}
        <div className="p-8 overflow-y-auto bg-slate-50/50 space-y-6 flex-1 custom-scrollbar">
          {/* EN Def */}
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm transition-all focus-within:border-emerald-200 focus-within:ring-4 focus-within:ring-emerald-50">
            <div className="flex justify-between items-center mb-3">
              <div className="flex items-center gap-2">
                <div className="bg-emerald-100 text-emerald-800 text-xs font-bold px-2.5 py-1 rounded-md tracking-wide">ENGLISH DEFINITION</div>
                <span className="text-sm text-slate-500 font-medium">(Oxford 3000 / Cambridge aligned)</span>
              </div>
              <span className="text-xs font-medium text-slate-400">Required • Max 280 chars</span>
            </div>
            <Textarea 
              className="border-none bg-slate-50/80 min-h-[80px] text-slate-700 resize-none focus-visible:ring-0 text-base" 
              value={localSense.definitionEn || ""}
              onChange={e => setLocalSense({ ...localSense, definitionEn: e.target.value })}
              placeholder="Enter English definition..."
            />
          </div>
          
          {/* VI Gloss */}
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm transition-all focus-within:border-rose-200 focus-within:ring-4 focus-within:ring-rose-50">
            <div className="flex justify-between items-center mb-3">
              <div className="flex items-center gap-2">
                <div className="bg-rose-100 text-rose-800 text-xs font-bold px-2.5 py-1 rounded-md tracking-wide">VIETNAMESE GLOSS</div>
                <span className="text-sm text-slate-500 font-medium">(Vietnamese Target Curriculum)</span>
              </div>
              <Button 
                variant="secondary" 
                size="sm" 
                className="text-rose-600 bg-rose-50 border-rose-100 hover:bg-rose-100 rounded-xl h-8 text-xs font-bold gap-1.5 shadow-sm"
                onClick={handleAiRefine}
                disabled={isRefining}
              >
                {isRefining ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Wand2 className="w-3.5 h-3.5" />} 
                {isRefining ? "Refining..." : "AI Refine"}
              </Button>
            </div>
            <Textarea 
              className="border-none bg-rose-50/30 min-h-[80px] text-slate-700 resize-none focus-visible:ring-0 text-base" 
              value={localSense.definitionVn || ""}
              onChange={e => setLocalSense({ ...localSense, definitionVn: e.target.value })}
              placeholder="Enter Vietnamese translation..."
            />
          </div>
          
          {/* Example */}
          <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
            <div className="flex justify-between items-center mb-5">
              <div className="flex items-center gap-2 text-slate-800 font-bold text-lg">
                <Quote className="w-5 h-5 text-emerald-600" fill="currentColor" opacity={0.2} /> 
                <span>Contextual Example Sentence <span className="text-slate-500 font-medium text-base ml-1">(Dual Corpus Pair)</span></span>
              </div>
              <div className="flex items-center gap-3">
                <Button 
                  variant="secondary" 
                  size="sm" 
                  className="text-emerald-600 bg-emerald-50 border-emerald-100 hover:bg-emerald-100 rounded-xl h-8 text-xs font-bold gap-1.5 shadow-sm"
                  onClick={handleAiGenerateExample}
                  disabled={isGeneratingExample}
                >
                  {isGeneratingExample ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Wand2 className="w-3.5 h-3.5" />} 
                  {isGeneratingExample ? "Generating..." : "AI Generate"}
                </Button>
                <div className="text-xs font-bold text-emerald-700 flex items-center gap-1.5 bg-emerald-50 px-3 py-1.5 rounded-full">
                  <div className="w-2 h-2 rounded-full bg-emerald-600 animate-pulse"></div> Primary Pedagogical Model
                </div>
              </div>
            </div>
            
            <div className="grid grid-cols-2 gap-5 mb-5">
              <div className="space-y-2">
                <Label className="text-xs font-bold text-slate-500 uppercase tracking-wider ml-1">Target English Sentence:</Label>
                <Input 
                  className="bg-slate-50 border-slate-200 focus-visible:ring-emerald-500 rounded-xl h-12 text-base shadow-inner" 
                  value={localSense.exampleEn || ""} 
                  onChange={e => setLocalSense({ ...localSense, exampleEn: e.target.value })} 
                />
              </div>
              <div className="space-y-2">
                <Label className="text-xs font-bold text-slate-500 uppercase tracking-wider ml-1">Vietnamese Translation:</Label>
                <Input 
                  className="bg-slate-50 border-slate-200 focus-visible:ring-emerald-500 rounded-xl h-12 text-base shadow-inner" 
                  value={localSense.exampleVn || ""} 
                  onChange={e => setLocalSense({ ...localSense, exampleVn: e.target.value })} 
                />
              </div>
            </div>
            
            <div className="flex flex-wrap items-center justify-between gap-4 bg-slate-50 p-4 rounded-xl border border-slate-100">
              <div className="flex items-center gap-3">
                <span className="text-sm font-semibold text-slate-700">Example Audio:</span>
                <span className="inline-flex items-center gap-1.5 text-xs font-semibold bg-emerald-100 text-emerald-800 px-2.5 py-1 rounded-full">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" /> Free Browser TTS (US GenAm)
                </span>
              </div>
              <div className="flex items-center gap-2">
                <Button 
                  type="button"
                  variant="secondary" 
                  size="sm" 
                  onClick={handleTestExampleTts}
                  disabled={!((localSense.exampleEn as string) || "").trim()}
                  className={`rounded-xl gap-2 font-semibold shadow-sm transition-all ${
                    isTestingTts 
                      ? 'bg-emerald-600 text-white shadow-emerald-600/30' 
                      : 'bg-white text-emerald-700 border-emerald-200 hover:bg-emerald-50'
                  }`}
                >
                  <Volume2 className={`w-4 h-4 ${isTestingTts ? 'animate-pulse' : ''}`} /> 
                  {isTestingTts ? "Playing TTS..." : "Listen Example"}
                </Button>
                <Button type="button" variant="secondary" size="sm" className="rounded-xl gap-2 h-9 px-3 font-semibold text-slate-500 shadow-sm">
                  <Upload className="w-4 h-4 text-slate-400" /> Custom Audio
                </Button>
              </div>
            </div>
          </div>
          
          {/* Synonyms, Antonyms, Collocations, Topics */}
          <div className="grid grid-cols-2 gap-5">
            {/* Topics — single canonical topic dropdown */}
            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col h-full">
              <div className="flex justify-between items-center mb-4">
                <div className="flex items-center gap-2 font-bold text-slate-700 text-sm tracking-widest">
                  <BookOpen className="w-4 h-4 text-emerald-500" />
                  TOPIC
                </div>
                {(localSense.topic as string | undefined) && (
                  <span className="text-xs font-semibold text-emerald-700 bg-emerald-50 border border-emerald-100 px-2 py-0.5 rounded-full">
                    1 assigned
                  </span>
                )}
              </div>
              {(localSense.topic as string | undefined) && (
                <div className="mb-3">
                  <div className="bg-emerald-50 text-emerald-700 px-3 py-1.5 rounded-lg text-sm font-medium border border-emerald-100 shadow-sm flex items-center justify-between">
                    <span>{localSense.topic as string}</span>
                    <X
                      className="w-3.5 h-3.5 text-emerald-400 cursor-pointer hover:text-emerald-600 transition-colors"
                      onClick={() => setLocalSense({ ...localSense, topic: undefined, topicId: undefined, topics: [] })}
                    />
                  </div>
                </div>
              )}
              <div className="mt-auto">
                <select
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-emerald-500 shadow-inner"
                  value={(localSense.topic as string | undefined) ?? ""}
                  onChange={e => {
                    const selected = CANONICAL_TOPICS.find(t => t.name === e.target.value);
                    if (selected) {
                      setLocalSense({ ...localSense, topic: selected.name, topics: [selected.name] });
                    } else {
                      setLocalSense({ ...localSense, topic: undefined, topicId: undefined, topics: [] });
                    }
                  }}
                >
                  <option value="">— Select a topic —</option>
                  {CANONICAL_TOPICS.map(t => (
                    <option key={t.slug} value={t.name}>{t.name}</option>
                  ))}
                </select>
              </div>
            </div>
            
            {/* Synonyms */}
            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col h-full">
              <div className="flex justify-between items-center mb-4">
                <div className="font-bold text-slate-700 text-sm tracking-widest">SYNONYMS</div>
                <div className="text-xs font-semibold text-slate-400 bg-slate-100 px-2 py-0.5 rounded-full">
                  {(localSense.synonyms?.length || 0)} active
                </div>
              </div>
              <div className="flex flex-wrap gap-2 mb-4">
                {localSense.synonyms?.map((s: string, i: number) => (
                  <div key={i} className="bg-white text-slate-700 px-3 py-1.5 rounded-lg text-sm font-medium border border-slate-200 shadow-sm flex items-center gap-1.5 group">
                    {s} <X className="w-3.5 h-3.5 text-slate-400 cursor-pointer group-hover:text-rose-500 transition-colors" onClick={() => handleArrayRemove("synonyms", i)} />
                  </div>
                ))}
              </div>
              <div className="mt-auto">
                <Input 
                  className="bg-slate-50 border-slate-200 focus-visible:ring-indigo-500 rounded-xl placeholder:text-slate-400 shadow-inner" 
                  placeholder="+ Add synonym" 
                  onKeyDown={e => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      handleArrayAdd("synonyms", e.currentTarget.value); 
                      e.currentTarget.value = '';
                    }
                  }} 
                />
              </div>
            </div>
            
            {/* Antonyms */}
            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col h-full">
              <div className="flex justify-between items-center mb-4">
                <div className="font-bold text-slate-700 text-sm tracking-widest">ANTONYMS</div>
                <div className="text-xs font-semibold text-slate-400 bg-slate-100 px-2 py-0.5 rounded-full">
                  {(localSense.antonyms?.length || 0)} active
                </div>
              </div>
              <div className="flex flex-wrap gap-2 mb-4">
                {localSense.antonyms?.map((s: string, i: number) => (
                  <div key={i} className="bg-rose-50 text-rose-700 px-3 py-1.5 rounded-lg text-sm font-medium border border-rose-100 shadow-sm flex items-center gap-1.5 group">
                    {s} <X className="w-3.5 h-3.5 text-rose-400 cursor-pointer group-hover:text-rose-600 transition-colors" onClick={() => handleArrayRemove("antonyms", i)} />
                  </div>
                ))}
              </div>
              <div className="mt-auto">
                <Input 
                  className="bg-slate-50 border-slate-200 focus-visible:ring-rose-500 rounded-xl placeholder:text-slate-400 shadow-inner" 
                  placeholder="+ Add antonym" 
                  onKeyDown={e => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      handleArrayAdd("antonyms", e.currentTarget.value); 
                      e.currentTarget.value = '';
                    }
                  }} 
                />
              </div>
            </div>
            
            {/* Collocations */}
            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col h-full">
              <div className="flex justify-between items-center mb-4">
                <div className="font-bold text-slate-700 text-sm tracking-widest">COLLOCATIONS</div>
                <div className="text-xs font-semibold text-slate-400 bg-slate-100 px-2 py-0.5 rounded-full">
                  {(localSense.collocations?.length || 0)} active
                </div>
              </div>
              <div className="flex flex-wrap gap-2 mb-4">
                {localSense.collocations?.map((s: string, i: number) => (
                  <div key={i} className="bg-indigo-50 text-indigo-700 px-3 py-1.5 rounded-lg text-sm font-medium border border-indigo-100 shadow-sm flex items-center gap-1.5 group">
                    {s} <X className="w-3.5 h-3.5 text-indigo-400 cursor-pointer group-hover:text-indigo-600 transition-colors" onClick={() => handleArrayRemove("collocations", i)} />
                  </div>
                ))}
              </div>
              <div className="mt-auto">
                <Input 
                  className="bg-slate-50 border-slate-200 focus-visible:ring-indigo-500 rounded-xl placeholder:text-slate-400 shadow-inner" 
                  placeholder="+ Add phrase" 
                  onKeyDown={e => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      handleArrayAdd("collocations", e.currentTarget.value); 
                      e.currentTarget.value = '';
                    }
                  }} 
                />
              </div>
            </div>
          </div>
        </div>
        
        {/* Footer */}
        <div className="p-6 border-t border-slate-100 bg-white flex justify-between items-center shrink-0">
          <button onClick={onClose} className="text-rose-600 font-bold hover:text-rose-800 transition-colors px-4 py-2 rounded-xl hover:bg-rose-50">
            Discard Changes
          </button>
          <div className="flex gap-3">
            <Button variant="secondary" className="rounded-xl gap-2 font-bold bg-slate-50 border-slate-200 text-slate-700 hover:bg-slate-100 px-5 shadow-sm">
              <MonitorPlay className="w-4.5 h-4.5" /> Preview Learner View
            </Button>
            <Button className="rounded-xl gap-2 font-bold bg-emerald-700 hover:bg-emerald-800 shadow-lg shadow-emerald-700/20 px-8 py-2.5" onClick={() => onSave(localSense)}>
              <CheckCircle2 className="w-5 h-5" /> Save Changes
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
