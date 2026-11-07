"use client";

import React, { useEffect, useState } from "react";
import { getVocabularyByWord } from "@/features/vocabulary/services/api";

interface VocabularyDetailPopupProps {
  word: string;
  onClose: () => void;
}

interface VocabularyData {
  word: string;
  commonMeaningVn?: string;
  pronunciations?: { accent: string; ipa: string; audioUrl?: string }[];
  senses?: {
    partOfSpeech?: string;
    cefrLevel?: string;
    definitionEn: string;
    definitionVn?: string;
    exampleEn?: string;
    exampleVn?: string;
  }[];
  idioms?: { phrase: string; definitionEn: string }[];
}

export default function VocabularyDetailPopup({ word, onClose }: VocabularyDetailPopupProps) {
  const [data, setData] = useState<VocabularyData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    getVocabularyByWord(word)
      .then(resData => {
        if (resData.code === 1000) {
          setData(resData.result);
        } else {
          setData(null);
        }
      })
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, [word]);

  if (!word) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl shadow-xl border border-slate-200/80 w-full max-w-2xl max-h-[90vh] overflow-y-auto relative">
        <button 
          onClick={onClose}
          className="absolute top-4 right-4 text-slate-400 hover:text-slate-600 p-2"
        >
          ✕
        </button>
        
        {loading ? (
          <div className="p-12 flex justify-center text-emerald-600">
            <span className="animate-pulse font-semibold">Đang tải từ vựng...</span>
          </div>
        ) : !data ? (
          <div className="p-12 text-center text-slate-500">
            <p>Không tìm thấy thông tin từ vựng.</p>
          </div>
        ) : (
          <div className="p-8">
            <div className="mb-6">
              <h2 className="text-3xl font-bold text-slate-900 mb-2">{data.word}</h2>
              {data.commonMeaningVn && (
                <p className="text-lg text-emerald-700 font-medium">{data.commonMeaningVn}</p>
              )}
              
              {(data.pronunciations?.length ?? 0) > 0 && (
                <div className="flex gap-4 mt-3">
                  {data.pronunciations?.map((p: { accent: string, ipa: string, audioUrl?: string }, i: number) => (
                    <div key={i} className="flex items-center gap-2 text-sm text-slate-600 bg-slate-50 px-3 py-1 rounded-full border border-slate-200">
                      <span className="font-semibold text-emerald-700">{p.accent}</span>
                      <span>{p.ipa}</span>
                      {p.audioUrl && (
                        <button className="text-emerald-500 hover:text-emerald-700" onClick={() => new Audio(p.audioUrl!).play()}>
                          ▶
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>

            {(data.senses?.length ?? 0) > 0 && (
              <div className="space-y-6">
                <h3 className="font-bold text-slate-800 text-lg border-b pb-2">Định nghĩa</h3>
                {data.senses?.map((sense: { partOfSpeech?: string, cefrLevel?: string, definitionEn: string, definitionVn?: string, exampleEn?: string, exampleVn?: string }, i: number) => (
                  <div key={i} className="bg-emerald-50/50 p-4 rounded-xl border border-emerald-100">
                    <div className="flex items-center gap-2 mb-2">
                      {sense.partOfSpeech && (
                        <span className="px-2 py-0.5 bg-emerald-100 text-emerald-800 text-xs font-bold rounded uppercase">
                          {sense.partOfSpeech}
                        </span>
                      )}
                      {sense.cefrLevel && (
                        <span className="px-2 py-0.5 bg-amber-100 text-amber-800 text-xs font-bold rounded uppercase">
                          {sense.cefrLevel}
                        </span>
                      )}
                    </div>
                    
                    <p className="text-slate-800 font-medium">{sense.definitionEn}</p>
                    {sense.definitionVn && <p className="text-slate-600 mt-1">{sense.definitionVn}</p>}
                    
                    {sense.exampleEn && (
                      <div className="mt-3 pl-3 border-l-2 border-emerald-300 italic text-slate-600 text-sm">
                        <p>&quot;{sense.exampleEn}&quot;</p>
                        {sense.exampleVn && <p className="text-slate-500 mt-0.5">{sense.exampleVn}</p>}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
            
            {(data.idioms?.length ?? 0) > 0 && (
              <div className="mt-6">
                 <h3 className="font-bold text-slate-800 text-lg border-b pb-2 mb-3">Thành ngữ (Idioms)</h3>
                 <ul className="space-y-2">
                   {data.idioms?.map((idm: { phrase: string, definitionEn: string }, i: number) => (
                     <li key={i} className="text-sm">
                       <span className="font-bold text-slate-800">{idm.phrase}: </span>
                       <span className="text-slate-600">{idm.definitionEn}</span>
                     </li>
                   ))}
                 </ul>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
