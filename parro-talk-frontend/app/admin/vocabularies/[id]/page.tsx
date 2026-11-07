"use client";

import React, { useEffect, useState, useMemo } from "react";
import { useParams, useRouter } from "next/navigation";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import FloatingToast from "@/components/ui/FloatingToast";
import { PlusCircle, Save, ArrowLeft, Volume2, Edit2, Globe, Book, Quote, Trash2, Wand2, Loader2, SparklesIcon } from "lucide-react";
import { adminVocabularyApi } from "@/features/admin/services/api";
import EditSenseModal from "./EditSenseModal";
import VocabularyDetailSkeleton from "./VocabularyDetailSkeleton";
import { playAudioOrTTS, playTTS, stopTTS } from "@/lib/tts";

export default function AdminVocabularyDetailPage() {
  const params = useParams();
  const router = useRouter();
  const id = params.id as string;
  const isCreate = id === "create";

  const [loading, setLoading] = useState(!isCreate);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState<{ message: string; variant: "success" | "error" | "info" } | null>(null);

  const [word, setWord] = useState("");
  const [commonMeaningVn, setCommonMeaningVn] = useState("");
  const [pronunciations, setPronunciations] = useState<Record<string, unknown>[]>([
    { region: "UK", ipa: "", audioUrl: "" },
    { region: "US", ipa: "", audioUrl: "" },
  ]);
  const [senses, setSenses] = useState<Record<string, unknown>[]>([]);
  const [idioms, setIdioms] = useState<Record<string, unknown>[]>([]);
  const [phrasalVerbs, setPhrasalVerbs] = useState<Record<string, unknown>[]>([]);
  
  const [editingSenseIndex, setEditingSenseIndex] = useState<number | null>(null);
  const [playingAudioKey, setPlayingAudioKey] = useState<string | null>(null);
  const [playingExampleIndex, setPlayingExampleIndex] = useState<number | null>(null);
  const [isRefiningAll, setIsRefiningAll] = useState(false);

  const handleRefineAll = async () => {
    if (senses.length === 0) return;
    setIsRefiningAll(true);
    try {
      const refinedList = await adminVocabularyApi.aiRefineSenses(word, senses);
      if (refinedList && refinedList.length === senses.length) {
        const updatedSenses = senses.map((sense, i) => {
          const refined = refinedList.find((r: any) => r.index === i) || refinedList[i];
          return {
            ...sense,
            cefrLevel: refined.cefrLevel || sense.cefrLevel,
            definitionEn: refined.definitionEn || sense.definitionEn,
            definitionVn: refined.definitionVn || sense.definitionVn,
            exampleEn: refined.exampleEn || sense.exampleEn,
            exampleVn: refined.exampleVn || sense.exampleVn
          };
        });
        setSenses(updatedSenses);
        setToast({ message: "All senses refined successfully", variant: "success" });
      }
    } catch (error) {
      console.error("Failed to refine all senses", error);
      setToast({ message: "Failed to refine senses", variant: "error" });
    } finally {
      setIsRefiningAll(false);
    }
  };

  useEffect(() => {
    if (!isCreate) {
      fetchVocabulary();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, isCreate]);

  const fetchVocabulary = async () => {
    try {
      const data = await adminVocabularyApi.getVocabulary(id) as Record<string, unknown>;
      setWord((data.word as string) || "");
      setCommonMeaningVn((data.commonMeaningVn as string) || "");

      // Ensure standard UK & US pronunciations are always present and not lost
      let prons = (data.pronunciations as Record<string, unknown>[]) || [];
      if (prons.length === 0) {
        const ukIpa = (data.ipaUk as string) || "";
        const usIpa = (data.ipaUs as string) || "";
        prons = [
          { region: "UK", ipa: ukIpa, audioUrl: (data.audioUk as string) || "" },
          { region: "US", ipa: usIpa, audioUrl: (data.audioUs as string) || "" },
        ];
      } else {
        const hasUk = prons.some(p => ((p.region as string) || "").toUpperCase() === "UK");
        const hasUs = prons.some(p => ((p.region as string) || "").toUpperCase() === "US");
        if (!hasUk) prons.unshift({ region: "UK", ipa: (data.ipaUk as string) || "", audioUrl: (data.audioUk as string) || "" });
        if (!hasUs) prons.push({ region: "US", ipa: (data.ipaUs as string) || "", audioUrl: (data.audioUs as string) || "" });
      }
      setPronunciations(prons);
      setSenses((data.senses as Record<string, unknown>[]) || []);
      setIdioms((data.idioms as Record<string, unknown>[]) || []);
      setPhrasalVerbs((data.phrasalVerbs as Record<string, unknown>[]) || []);
    } catch {
      setToast({ message: "Error loading vocabulary", variant: "error" });
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const payload: Record<string, unknown> = {
        word,
        commonMeaningVn,
        pronunciations,
        senses,
        idioms,
        phrasalVerbs,
        categoryIds: [],
        tags: [],
        definitions: [],
        examples: [],
        relations: []
      };

      if (isCreate) {
        await adminVocabularyApi.createVocabulary(payload);
      } else {
        await adminVocabularyApi.updateVocabulary(id, payload);
      }
      
      setToast({ message: "Vocabulary saved successfully", variant: "success" });
      if (isCreate) {
        setTimeout(() => router.push("/admin/vocabularies"), 1000);
      }
    } catch {
      setToast({ message: "Error saving vocabulary", variant: "error" });
    } finally {
      setSaving(false);
    }
  };

  const replaceSense = (index: number, updatedSense: Record<string, unknown>) => {
    const updated = [...senses];
    updated[index] = updatedSense;
    setSenses(updated);
  };

  const handleAiGenerateCommonMeaning = async () => {
    if (!word.trim()) {
      setToast({ message: "Please enter a word first", variant: "error" });
      return;
    }
    try {
      setToast({ message: "Generating common meaning...", variant: "info" });
      const res = await adminVocabularyApi.aiGenerateCommonMeaning(word);
      if (res && res.commonMeaningVn) {
        setCommonMeaningVn(res.commonMeaningVn);
        setToast({ message: "Generated successfully!", variant: "success" });
      } else {
        setToast({ message: "AI returned empty result", variant: "error" });
      }
    } catch (err: any) {
      console.error(err);
      setToast({ message: err?.response?.data?.message || "Failed to generate common meaning", variant: "error" });
    }
  };

  const addSense = () => {
    const newSense = { 
      partOfSpeech: "noun", 
      cefrLevel: "A1", 
      definitionEn: "", 
      definitionVn: "",
      exampleEn: "",
      exampleVn: "",
      synonyms: [], 
      antonyms: [],
      collocations: [],
      topics: []
    };
    setSenses([...senses, newSense]);
    setEditingSenseIndex(senses.length);
  };
  
  const removeSense = (index: number) => {
    setSenses(senses.filter((_, i) => i !== index));
  };

  const handlePlayPronunciation = (p: Record<string, unknown>) => {
    const region = ((p.region as string) || "UK").toUpperCase();
    if (playingAudioKey === region) {
      stopTTS();
      setPlayingAudioKey(null);
      return;
    }
    setPlayingAudioKey(region);
    const lang = region === "UK" ? "en-GB" : "en-US";
    playAudioOrTTS(word, p.audioUrl as string, lang, {
      onStart: () => setPlayingAudioKey(region),
      onEnd: () => setPlayingAudioKey(null),
    });
  };

  const handlePlayExampleTTS = (idx: number, sentence: string) => {
    if (playingExampleIndex === idx) {
      stopTTS();
      setPlayingExampleIndex(null);
      return;
    }
    setPlayingExampleIndex(idx);
    playTTS(sentence, {
      lang: "en-US",
      onStart: () => setPlayingExampleIndex(idx),
      onEnd: () => setPlayingExampleIndex(null),
      onError: () => setPlayingExampleIndex(null),
    });
  };

  const cefrSummary = useMemo(() => {
    const grouped: Record<string, string[]> = {};
    senses.forEach((s) => {
      const pos = (s.partOfSpeech as string) || "Other";
      const lvl = (s.cefrLevel as string) || "A1";
      if (!grouped[pos]) grouped[pos] = [];
      if (!grouped[pos].includes(lvl)) grouped[pos].push(lvl);
    });
    return Object.entries(grouped).map(([pos, levels]) => ({
      pos,
      levels: levels.sort(),
    }));
  }, [senses]);

  const completeness = useMemo(() => {
    const hasWord = Boolean(word.trim());
    const hasMeaning = Boolean(commonMeaningVn.trim());
    const hasPron = pronunciations.some(p => Boolean(((p.ipa as string) || "").trim()));
    const hasSenses = senses.length > 0;
    let score = 0;
    if (hasWord) score += 25;
    if (hasMeaning) score += 25;
    if (hasPron) score += 25;
    if (hasSenses) score += 25;
    return score;
  }, [word, commonMeaningVn, pronunciations, senses]);

  if (loading) return <VocabularyDetailSkeleton />;

  return (
    <div className="p-6 max-w-6xl mx-auto space-y-6 pb-24 relative">
      {toast && <FloatingToast message={toast.message} variant={toast.variant} onClose={() => setToast(null)} />}
      
      <div className="flex items-center gap-4 mb-2">
        <Button variant="ghost" size="icon" onClick={() => router.back()}>
          <ArrowLeft className="w-5 h-5" />
        </Button>
        <div className="text-sm font-semibold text-slate-500 uppercase tracking-widest">
          {isCreate ? "Create Vocabulary" : "Vocabulary Editor"}
        </div>
      </div>

      {/* Header Section */}
      <div className="bg-slate-50 rounded-[2.5rem] p-8 shadow-sm">
        <div className="flex flex-col lg:flex-row justify-between items-start gap-8">
          <div className="flex-1">
            <div className="flex flex-wrap items-end gap-4 mb-4">
              {isCreate ? (
                <Input 
                  className="text-4xl font-black text-slate-800 bg-transparent border-none p-0 focus-visible:ring-0 w-80 h-auto" 
                  value={word} 
                  onChange={(e) => setWord(e.target.value)} 
                  placeholder="Enter word..." 
                />
              ) : (
                <h1 className="text-5xl font-black text-slate-800 tracking-tight">{word}</h1>
              )}
              <span className="text-2xl text-orange-600 font-medium pb-1 flex items-center">
                / <Input className="text-2xl font-medium text-orange-600 bg-transparent border-none p-0 focus-visible:ring-0 w-48 h-auto mx-1" value={commonMeaningVn} onChange={(e) => setCommonMeaningVn(e.target.value)} placeholder="Meaning..." /> /
              </span>
              <div className="flex items-center gap-2 mb-2">
                <button 
                  onClick={handleAiGenerateCommonMeaning}
                  className="flex items-center gap-1.5 bg-gradient-to-r from-orange-100 to-amber-100 hover:from-orange-200 hover:to-amber-200 text-orange-700 text-xs font-bold tracking-wider uppercase px-3 py-1.5 rounded-md transition-all shadow-sm border border-orange-200/50"
                  title="Generate Meaning with AI"
                >
                  <SparklesIcon className="w-3.5 h-3.5" />
                  Auto Generate
                </button>
              </div>
            </div>
            
            {/* Pronunciations Badges (UK & US with Free TTS & Inline IPA Edit) */}
            <div className="flex flex-wrap items-center gap-3.5 mb-6">
              {pronunciations.map((p, i) => {
                const region = ((p.region as string) || "UK").toUpperCase();
                const isPlaying = playingAudioKey === region;
                return (
                  <div 
                    key={i} 
                    className="flex items-center bg-slate-200/70 hover:bg-slate-200/90 rounded-2xl px-3.5 py-1.5 gap-2 shadow-sm border border-slate-300/40 transition-all group"
                  >
                    <span className="text-slate-600 font-extrabold text-xs uppercase tracking-wider">
                      {region}
                    </span>
                    <span className="text-slate-400 font-semibold text-sm">/</span>
                    <input
                      type="text"
                      value={(p.ipa as string) || ""}
                      onChange={(e) => {
                        const updated = [...pronunciations];
                        updated[i] = { ...updated[i], ipa: e.target.value };
                        setPronunciations(updated);
                      }}
                      placeholder={`IPA (${region})...`}
                      className="bg-transparent text-slate-800 font-semibold text-[15px] w-28 focus:outline-none focus:bg-white/90 focus:ring-2 focus:ring-emerald-400 rounded-md px-1.5 py-0.5 transition-all"
                    />
                    <span className="text-slate-400 font-semibold text-sm">/</span>
                    <button
                      type="button"
                      onClick={() => handlePlayPronunciation(p)}
                      disabled={!word.trim()}
                      title={`Listen ${region} pronunciation (Free TTS)`}
                      className={`p-1.5 rounded-full transition-all ${
                        isPlaying 
                          ? "bg-emerald-600 text-white shadow-md shadow-emerald-600/30 scale-110 ring-2 ring-emerald-300" 
                          : "text-emerald-700 hover:bg-emerald-100 hover:scale-110"
                      }`}
                    >
                      <Volume2 className={`w-4 h-4 ${isPlaying ? "animate-bounce" : ""}`} />
                    </button>
                  </div>
                );
              })}
              <Button 
                variant="ghost" 
                size="sm" 
                className="text-slate-500 rounded-xl hover:bg-slate-200/50 text-xs font-bold"
                onClick={() => setPronunciations([...pronunciations, { region: "AU", ipa: "", audioUrl: "" }])}
              >
                + Add Accent
              </Button>
            </div>
            
            {/* CEFR Tiers */}
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-slate-500 text-xs font-bold uppercase tracking-wider mr-1">CEFR Tiers:</span>
              {cefrSummary.length > 0 ? (
                cefrSummary.map((item, i) => (
                  <div 
                    key={i} 
                    className={`font-bold px-3 py-1 rounded-lg text-xs shadow-sm capitalize ${
                      i === 0 
                        ? "bg-emerald-700 text-white shadow-emerald-700/20" 
                        : i === 1 
                        ? "bg-indigo-100 text-indigo-700" 
                        : "bg-slate-200 text-slate-700"
                    }`}
                  >
                    {item.pos}: {item.levels.join("-")}
                  </div>
                ))
              ) : (
                <div className="bg-slate-200 text-slate-500 font-bold px-3 py-1 rounded-lg text-xs">A1-C2</div>
              )}
            </div>
          </div>
          
          {/* Completeness Card */}
          <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 w-full lg:w-80 shrink-0">
            <div className="flex justify-between items-center mb-2">
              <span className="text-sm font-semibold text-slate-600">Vocabulary Completeness</span>
              <span className="text-sm font-bold text-emerald-600">{completeness}%</span>
            </div>
            <div className="h-2.5 bg-slate-100 rounded-full mb-6 overflow-hidden">
              <div 
                className="h-full bg-emerald-600 rounded-full transition-all duration-1000 ease-out" 
                style={{ width: `${completeness}%` }}
              />
            </div>
            <div className="grid grid-cols-3 gap-2">
              <div className="flex flex-col items-center justify-center bg-slate-50 rounded-2xl p-2.5 border border-slate-100">
                <div className="text-[10px] uppercase font-bold tracking-wider text-slate-500 mb-1 text-center">Senses</div>
                <div className="font-bold text-slate-800 leading-tight text-center">{senses.length} <span className="font-normal text-xs block text-slate-500">total</span></div>
              </div>
              <div className="flex flex-col items-center justify-center bg-slate-50 rounded-2xl p-2.5 border border-slate-100">
                <div className="text-[10px] uppercase font-bold tracking-wider text-slate-500 mb-1 text-center">Dual<br/>Examples</div>
                <div className="font-bold text-slate-800 leading-tight text-center">{senses.length * 2} <span className="font-normal text-xs block text-slate-500">pairs</span></div>
              </div>
              <div className="flex flex-col items-center justify-center bg-emerald-50 rounded-2xl p-2.5 border border-emerald-100">
                <div className="text-[10px] uppercase font-bold tracking-wider text-slate-500 mb-1 text-center">Audio<br/>Status</div>
                <div className="font-bold text-emerald-700 text-sm leading-tight text-center">Ready</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Senses List */}
      <div className="mt-8 space-y-6">
        <div className="flex justify-between items-center px-2">
          <h2 className="text-xl font-bold text-slate-800">Senses ({senses.length})</h2>
          <div className="flex items-center gap-2">
            {senses.length > 0 && (
              <Button 
                variant="secondary" 
                size="sm" 
                onClick={handleRefineAll} 
                disabled={isRefiningAll}
                className="rounded-xl font-bold bg-indigo-50 text-indigo-700 border-indigo-100 hover:bg-indigo-100 shadow-sm"
              >
                {isRefiningAll ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Wand2 className="w-4 h-4 mr-2" />}
                {isRefiningAll ? "Refining..." : "AI Refine All"}
              </Button>
            )}
            <Button variant="secondary" size="sm" onClick={addSense} leftIcon={<PlusCircle className="w-4 h-4" />} className="rounded-xl font-bold bg-white border-slate-200 shadow-sm">
              Add New Sense
            </Button>
          </div>
        </div>
        
        {senses.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-[2rem] border border-dashed border-slate-300 text-slate-500">
            No senses defined yet. Click &quot;Add New Sense&quot; to create one.
          </div>
        ) : (
          senses.map((sense, index) => (
            <div key={index} className="bg-white rounded-[2rem] p-6 shadow-sm border border-slate-100 relative group hover:shadow-md transition-shadow">
              <div className="flex justify-between items-center mb-6">
                <div className="flex items-center gap-3">
                  <div className="bg-indigo-100 text-indigo-700 font-black px-3 py-1 rounded-lg text-sm shadow-sm flex items-center justify-center">
                    #{index + 1}
                  </div>
                  <div className="bg-orange-100 text-orange-700 font-bold px-3 py-1 rounded-lg text-sm shadow-sm uppercase">
                    CEFR {(sense.cefrLevel as string) || "Unknown"}
                  </div>
                  <div className="text-slate-300 mx-1">|</div>
                  <div className="text-slate-500 text-sm font-medium capitalize">
                    Part of Speech: {(sense.partOfSpeech as string) || "General"}
                  </div>
                </div>
                <div className="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <Button variant="secondary" size="sm" onClick={() => setEditingSenseIndex(index)} className="rounded-xl px-4 flex items-center gap-2 font-bold text-slate-600 border-slate-200 bg-slate-50 hover:bg-slate-100">
                    <Edit2 className="w-4 h-4" /> Edit
                  </Button>
                  <Button variant="ghost" size="icon" onClick={() => removeSense(index)} className="rounded-xl text-slate-400 hover:text-rose-500 hover:bg-rose-50">
                    <Trash2 className="w-4 h-4" />
                  </Button>
                </div>
              </div>
              
              <div className="space-y-3 mb-6">
                <div className="flex bg-indigo-50/50 rounded-2xl p-4 gap-4 items-start border border-indigo-50/50">
                  <div className="bg-emerald-100 text-emerald-800 text-[11px] font-black px-2 py-1 rounded-md flex gap-1 items-center whitespace-nowrap mt-0.5 tracking-wider shadow-sm">
                    <Globe className="w-3 h-3" /> EN DEF
                  </div>
                  <p className="text-slate-700 font-medium leading-relaxed pt-0.5 text-[15px]">
                    {(sense.definitionEn as string) || <span className="text-slate-400 italic">No English definition provided.</span>}
                  </p>
                </div>
                <div className="flex bg-rose-50/30 rounded-2xl p-4 gap-4 items-start border border-rose-50/50">
                  <div className="bg-rose-100 text-rose-800 text-[11px] font-black px-2 py-1 rounded-md flex gap-1 items-center whitespace-nowrap mt-0.5 tracking-wider shadow-sm">
                    <Book className="w-3 h-3" /> VI GLOSS
                  </div>
                  <p className="text-slate-700 font-medium leading-relaxed pt-0.5 text-[15px]">
                    {(sense.definitionVn as string) || <span className="text-slate-400 italic">No Vietnamese gloss provided.</span>}
                  </p>
                </div>
              </div>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                {/* Dual Curated Example with Free TTS */}
                <div className="bg-slate-50 rounded-2xl p-5 border border-slate-100 relative overflow-hidden group/example">
                  <div className="absolute top-0 left-0 w-1 h-full bg-indigo-200"></div>
                  <div className="flex justify-between items-center mb-4 pl-2">
                    <div className="text-[11px] font-black text-slate-500 tracking-widest flex items-center gap-1.5 uppercase">
                      <Quote className="w-3.5 h-3.5" fill="currentColor" opacity={0.3} /> DUAL CURATED EXAMPLE
                    </div>
                    {Boolean((sense.exampleEn as string)?.trim()) && (
                      <button 
                        type="button"
                        onClick={() => handlePlayExampleTTS(index, sense.exampleEn as string)}
                        className={`px-3 py-1 rounded-xl flex items-center gap-1.5 text-xs font-bold transition-all shadow-sm ${
                          playingExampleIndex === index 
                            ? "bg-emerald-600 text-white shadow-emerald-600/30 scale-105" 
                            : "text-emerald-700 bg-emerald-100/70 hover:bg-emerald-200/80 hover:scale-105"
                        }`}
                      >
                        <Volume2 className={`w-3.5 h-3.5 ${playingExampleIndex === index ? "animate-pulse" : ""}`} /> 
                        {playingExampleIndex === index ? "Playing Audio..." : "Listen (US GenAm)"}
                      </button>
                    )}
                  </div>
                  <div className="pl-2 space-y-2">
                    <p className="text-slate-800 font-semibold text-[15px] leading-relaxed">
                      &quot;{(sense.exampleEn as string) || "No example sentence."}&quot;
                    </p>
                    <p className="text-slate-500 text-sm font-medium">
                      &quot;{(sense.exampleVn as string) || "Chưa có ví dụ."}&quot;
                    </p>
                  </div>
                </div>
                
                {/* Associations and Topics */}
                <div className="bg-slate-50/50 rounded-2xl p-5 border border-slate-100 flex flex-col justify-center space-y-4">
                  <div className="flex gap-4 items-center">
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-400 w-24 shrink-0">Topics:</span>
                    <div className="flex flex-wrap gap-2">
                      {Array.isArray(sense.topics) && sense.topics.length > 0 ? sense.topics.map((s: unknown, i: number) => (
                        <div key={i} className="bg-emerald-50 text-emerald-700 font-semibold text-sm px-2.5 py-1 rounded-md border border-emerald-100 shadow-sm">{s as string}</div>
                      )) : <span className="text-slate-300 text-sm italic">None</span>}
                    </div>
                  </div>
                  <div className="flex gap-4 items-center">
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-400 w-24 shrink-0">Synonyms:</span>
                    <div className="flex flex-wrap gap-2">
                      {Array.isArray(sense.synonyms) && sense.synonyms.length > 0 ? sense.synonyms.map((s: unknown, i: number) => (
                        <div key={i} className="bg-white text-slate-600 font-semibold text-sm px-2.5 py-1 rounded-md border border-slate-200 shadow-sm">{s as string}</div>
                      )) : <span className="text-slate-300 text-sm italic">None</span>}
                    </div>
                  </div>
                  <div className="flex gap-4 items-center">
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-400 w-24 shrink-0">Antonyms:</span>
                    <div className="flex flex-wrap gap-2">
                      {Array.isArray(sense.antonyms) && sense.antonyms.length > 0 ? sense.antonyms.map((s: unknown, i: number) => (
                        <div key={i} className="bg-rose-50 text-rose-700 font-semibold text-sm px-2.5 py-1 rounded-md border border-rose-100 shadow-sm">{s as string}</div>
                      )) : <span className="text-slate-300 text-sm italic">None</span>}
                    </div>
                  </div>
                  <div className="flex gap-4 items-center">
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-400 w-24 shrink-0">Collocations:</span>
                    <div className="flex flex-wrap gap-2">
                      {Array.isArray(sense.collocations) && sense.collocations.length > 0 ? sense.collocations.map((s: unknown, i: number) => (
                        <div key={i} className="bg-indigo-50 text-indigo-700 font-semibold text-sm px-2.5 py-1 rounded-md border border-indigo-100 shadow-sm">{s as string}</div>
                      )) : <span className="text-slate-300 text-sm italic">None</span>}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Floating Save Button */}
      <div className="fixed bottom-0 left-0 right-0 p-5 bg-white/80 backdrop-blur-md border-t border-slate-200/60 flex justify-end px-10 z-40">
        <div className="max-w-6xl w-full flex justify-end gap-4">
          <Button variant="secondary" onClick={() => router.back()} className="rounded-xl px-6 font-bold border-slate-200 text-slate-600">Cancel</Button>
          <Button 
            leftIcon={<Save className="w-5 h-5" />} 
            onClick={handleSave} 
            disabled={saving}
            className="rounded-xl px-8 font-bold bg-slate-900 text-white hover:bg-slate-800 shadow-lg shadow-slate-900/20"
          >
            {saving ? "Saving..." : "Save Vocabulary"}
          </Button>
        </div>
      </div>

      {/* Edit Sense Modal Component */}
      <EditSenseModal 
        isOpen={editingSenseIndex !== null}
        onClose={() => setEditingSenseIndex(null)}
        sense={editingSenseIndex !== null ? senses[editingSenseIndex] : null}
        index={editingSenseIndex || 0}
        word={word}
        onSave={(updatedSense) => {
          if (editingSenseIndex !== null) {
            replaceSense(editingSenseIndex, updatedSense);
          }
          setEditingSenseIndex(null);
        }}
      />
    </div>
  );
}
