"use client";

import React, { useEffect, useState, useMemo, useCallback } from "react";
import { useParams } from "next/navigation";
import {
  TopicDetailHero,
  TopicProgressCard,
  RecommendedLearningCard,
  DifferentWaysToLearn,
  SmartReviewCard,
  TopicWordList,
  TopicSidebar,
  type WordItemWithStatus,
} from "@/features/vocabulary/components/topic-detail";
import { FooterNote } from "@/features/vocabulary/components/shared";
import * as api from "@/features/vocabulary/services/api";

export default function TopicDetailPage() {
  const params = useParams();
  const rawId = Array.isArray(params.id) ? params.id[0] : params.id;
  const topicId = decodeURIComponent(rawId || "");

  const [topicData, setTopicData] = useState<any>(null);
  const [topicLoading, setTopicLoading] = useState(true);
  const [topicError, setTopicError] = useState<any>(null);

  const fetchTopic = useCallback(() => {
    let active = true;
    setTopicLoading(true);
    setTopicError(null);
    api.getTopicSummary(topicId)
       .then(res => { if (active) setTopicData(res); })
       .catch(err => { if (active) setTopicError(err?.response?.status === 404 ? 'not-found' : 'error'); })
       .finally(() => { if (active) setTopicLoading(false); });
    return () => { active = false; };
  }, [topicId]);

  useEffect(() => fetchTopic(), [fetchTopic]);

  const [page, setPage] = useState(0);
  const size = 30;
  const [wordsData, setWordsData] = useState<any>(null);
  const [wordsLoading, setWordsLoading] = useState(true);
  const [wordsError, setWordsError] = useState<any>(null);

  const fetchWords = useCallback(() => {
    let active = true;
    setWordsLoading(true);
    setWordsError(null);
    api.getTopicWords(topicId, { page, size })
       .then(res => { if (active) setWordsData(res); })
       .catch(err => { if (active) setWordsError(true); })
       .finally(() => { if (active) setWordsLoading(false); });
    return () => { active = false; };
  }, [topicId, page, size]);

  useEffect(() => fetchWords(), [fetchWords]);

  if (topicLoading) {
    return (
      <div className="min-h-screen bg-ink-50 p-8 flex items-center justify-center">
        <div className="text-center space-y-3">
          <div className="w-10 h-10 border-3 border-brand-500 border-t-transparent rounded-full animate-spin mx-auto" />
          <p className="text-sm font-semibold text-ink-500">Loading topic details…</p>
        </div>
      </div>
    );
  }

  if (topicError === 'not-found') {
    return (
      <div className="min-h-screen bg-ink-50 p-8 flex items-center justify-center">
        <div className="text-center space-y-4 max-w-md">
          <h1 className="text-3xl font-bold text-ink-900">Topic Not Found</h1>
          <p className="text-ink-600">The topic you are looking for does not exist or has been removed.</p>
          <button onClick={() => window.history.back()} className="px-4 py-2 bg-brand-500 text-white rounded-lg hover:bg-brand-600 transition-colors">Go Back</button>
        </div>
      </div>
    );
  }

  if (topicError) {
    return (
      <div className="min-h-screen bg-ink-50 p-8 flex items-center justify-center">
        <div className="text-center space-y-4 max-w-md">
          <h1 className="text-3xl font-bold text-red-600">Error Loading Topic</h1>
          <p className="text-ink-600">We encountered an error while loading the topic details.</p>
          <button onClick={() => fetchTopic()} className="px-4 py-2 bg-brand-500 text-white rounded-lg hover:bg-brand-600 transition-colors">Retry</button>
        </div>
      </div>
    );
  }

  const apiWords: WordItemWithStatus[] = (wordsData?.content || []).map((w: any) => ({
    ...w,
    status: w.status ? w.status.toLowerCase() : null
  }));

  const nextWordToLearn =
    apiWords.find((w) => w.status === "learning")?.word ||
    apiWords.find((w) => w.status === "new")?.word ||
    apiWords[0]?.word || "Itinerary";

  return (
    <div className="relative min-h-full bg-ink-50 text-ink-800">
      <div className="pointer-events-none absolute inset-x-0 top-0 h-80 bg-gradient-to-b from-brand-50 to-transparent" />

      <div className="relative mx-auto max-w-[1100px] space-y-8 px-4 py-8 sm:px-8 sm:py-10">
        <TopicDetailHero topic={topicData} />
        <TopicProgressCard topic={topicData} />
        <RecommendedLearningCard topic={topicData} nextWord={nextWordToLearn} />
        <DifferentWaysToLearn topic={topicData} />
        <SmartReviewCard topic={topicData} />

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start pt-2">
          <div className="lg:col-span-2">
            {wordsError && (
               <div className="mb-4 p-4 bg-red-50 text-red-600 rounded-lg flex justify-between items-center shadow-sm">
                 <span>Failed to load words.</span>
                 <button onClick={() => fetchWords()} className="text-sm font-semibold underline">Retry</button>
               </div>
            )}
            <div className="flex justify-between items-center mb-4 bg-white p-3 rounded-xl shadow-sm border border-ink-100">
              <div className="font-semibold text-ink-900 flex items-center">
                Words <span className="text-ink-500 font-normal ml-2">({topicData?.wordCount || 0})</span>
              </div>
              <div className="flex items-center gap-3">
                <button 
                  onClick={() => setPage(p => Math.max(0, p - 1))} 
                  disabled={page === 0 || wordsLoading} 
                  className="px-3 py-1 rounded bg-ink-50 text-ink-700 disabled:opacity-40 hover:bg-ink-100 transition-colors text-sm font-medium border border-ink-200"
                >
                  Prev
                </button>
                <span className="text-sm text-ink-600 min-w-[5rem] text-center font-medium">
                  {wordsData?.totalPages ? `Page ${page + 1} / ${wordsData.totalPages}` : '...'}
                </span>
                <button 
                  onClick={() => setPage(p => p + 1)} 
                  disabled={page >= (wordsData?.totalPages || 1) - 1 || wordsLoading} 
                  className="px-3 py-1 rounded bg-ink-50 text-ink-700 disabled:opacity-40 hover:bg-ink-100 transition-colors text-sm font-medium border border-ink-200"
                >
                  Next
                </button>
              </div>
            </div>
            <TopicWordList
              words={apiWords}
              totalWords={topicData?.wordCount || 0}
              loading={wordsLoading}
            />
          </div>

          <div className="lg:col-span-1">
            <TopicSidebar topic={topicData} nextWord={nextWordToLearn} />
          </div>
        </div>

        <FooterNote />
      </div>
    </div>
  );
}
